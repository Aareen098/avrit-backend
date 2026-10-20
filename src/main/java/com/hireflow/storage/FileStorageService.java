package com.hireflow.storage;

import com.hireflow.common.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// Saves uploaded resumes. Two modes, chosen only by configuration:
//   - SUPABASE_URL + SUPABASE_SECRET_KEY set  -> a private Supabase Storage bucket (use this when deployed,
//     because cloud hosts erase the local disk on every restart)
//   - not set                                  -> the local "uploads" folder (fine on your own computer)
// Files are never served directly: they are only read by the backend.
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of("pdf", "doc", "docx");
    // The only names we ever create: a random UUID plus a checked extension.
    private static final Pattern STORED_NAME = Pattern.compile("^[0-9a-f\\-]{36}\\.(pdf|doc|docx)$");
    private static final Map<String, String> TYPES = Map.of(
            "pdf", "application/pdf",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final Path root;
    private final String supabaseUrl;
    private final String supabaseKey;
    private final String bucket;

    public FileStorageService(@Value("${app.storage.dir}") String dir,
                              @Value("${app.storage.supabase-url}") String supabaseUrl,
                              @Value("${app.storage.supabase-key}") String supabaseKey,
                              @Value("${app.storage.bucket}") String bucket) {
        // Accept the address even if it was copied with /rest/v1 or /storage/v1 on the end
        this.supabaseUrl = supabaseUrl.trim().replaceAll("/+$", "").replaceAll("/(rest|storage)/v1$", "");
        this.supabaseKey = supabaseKey.trim();
        this.bucket = bucket.trim();
        this.root = Path.of(dir).toAbsolutePath().normalize();
        if (remote()) {
            log.info("Resumes are stored in the Supabase Storage bucket '{}'", this.bucket);
        } else {
            try {
                Files.createDirectories(root);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            log.info("Resumes are stored in the local folder {}", root);
        }
    }

    private boolean remote() {
        return !supabaseUrl.isEmpty() && !supabaseKey.isEmpty();
    }

    // Checks the file, saves it under a random name and returns that name.
    public String saveResume(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Upload your resume");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File must be under 5 MB");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED.contains(extension)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Use a PDF or Word file");
        }
        // The stored name is random + a checked extension, so the original name can never escape the folder.
        String storedName = UUID.randomUUID() + "." + extension;
        try {
            if (remote()) {
                upload(storedName, file.getBytes(), TYPES.get(extension));
            } else {
                try (InputStream in = file.getInputStream()) {
                    Files.copy(in, root.resolve(storedName));
                }
            }
        } catch (IOException e) {
            log.warn("Could not save resume: {}", e.getMessage());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the file");
        }
        return storedName;
    }

    // Opens a saved resume. Only names made by saveResume (UUID + extension) are accepted.
    public Resource load(String storedName) {
        if (storedName == null || !STORED_NAME.matcher(storedName).matches()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Resume file not found");
        }
        if (remote()) return download(storedName);

        Path file = root.resolve(storedName).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Resume file not found");
        }
        return new PathResource(file);
    }

    // Removes a saved resume. Never throws: a file that cannot be deleted is only logged.
    public void delete(String storedName) {
        if (storedName == null || !STORED_NAME.matcher(storedName).matches()) return;
        try {
            if (remote()) {
                HttpRequest request = authorized(objectUrl("object/" + bucket + "/" + storedName)).DELETE().build();
                HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() / 100 != 2 && response.statusCode() != 404) {
                    log.warn("Supabase Storage answered {} while deleting a resume", response.statusCode());
                }
            } else {
                Path file = root.resolve(storedName).normalize();
                if (file.startsWith(root)) Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            log.warn("Could not delete resume {}: {}", storedName, e.getMessage());
        }
    }

    // ---- Supabase Storage (REST) ----

    private void upload(String storedName, byte[] bytes, String contentType) throws IOException {
        HttpRequest request = authorized(objectUrl("object/" + bucket + "/" + storedName))
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .build();
        HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Supabase Storage answered " + response.statusCode() + ": " + response.body());
        }
    }

    private Resource download(String storedName) {
        HttpRequest request = authorized(objectUrl("object/authenticated/" + bucket + "/" + storedName)).GET().build();
        try {
            HttpResponse<byte[]> response = send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) return new ByteArrayResource(response.body());
            if (response.statusCode() == 400 || response.statusCode() == 404) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Resume file not found");
            }
            log.warn("Supabase Storage answered {} while reading a resume", response.statusCode());
        } catch (IOException e) {
            log.warn("Could not read resume from storage: {}", e.getMessage());
        }
        throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not read the resume file");
    }

    private <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws IOException {
        try {
            return http.send(request, handler);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Storage request was interrupted");
        }
    }

    private String objectUrl(String path) {
        return supabaseUrl + "/storage/v1/" + path;
    }

    // New "sb_secret_..." keys go in the apikey header only; old service_role keys are JWTs and also go in Authorization.
    private HttpRequest.Builder authorized(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("apikey", supabaseKey);
        if (supabaseKey.startsWith("eyJ")) builder.header("Authorization", "Bearer " + supabaseKey);
        return builder;
    }

    private static String extensionOf(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase();
    }
}