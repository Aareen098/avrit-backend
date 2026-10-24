package com.hireflow.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.hireflow.common.ApiException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

// Checks the "credential" (a signed ID token) the browser gets from Google's sign-in button.
// The library checks Google's signature, that it was made for OUR client ID, the issuer and the expiry.
@Service
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);

    public record GoogleUser(String sub, String email, String name) {
    }

    private final GoogleIdTokenVerifier verifier; // null while GOOGLE_CLIENT_ID is not set

    public GoogleAuthService(@Value("${app.google.client-id:}") String clientId) {
        this.verifier = clientId.isBlank() ? null
                : new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(clientId.trim()))
                .build();
    }

    public GoogleUser verify(String credential) {
        if (verifier == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Google sign-in is not set up on this server");
        }
        GoogleIdToken token;
        try {
            token = verifier.verify(credential);
        } catch (GeneralSecurityException | IOException e) {
            log.warn("Google token could not be checked: {}", e.toString());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Google sign-in failed. Please try again.");
        }
        if (token == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Google sign-in failed. Please try again.");
        }
        GoogleIdToken.Payload payload = token.getPayload();
        String email = payload.getEmail();
        if (email == null || !Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Your Google email address is not verified");
        }
        Object name = payload.get("name");
        String displayName = name instanceof String s && !s.isBlank() ? s.trim() : email.substring(0, email.indexOf('@'));
        return new GoogleUser(payload.getSubject(), email.toLowerCase(), displayName);
    }
}