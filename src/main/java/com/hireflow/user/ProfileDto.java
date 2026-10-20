package com.hireflow.user;

// What the candidate profile page shows. completeness is 0-100.
public record ProfileDto(String name, String email, String phone, String location, String headline, String about,
                         String linkedin, String github, String portfolio, String resumeName, int completeness) {

    public static ProfileDto from(User u) {
        boolean[] done = {
                has(u.getPhone()), has(u.getLocation()), has(u.getHeadline()), has(u.getAbout()),
                has(u.getLinkedin()) || has(u.getGithub()) || has(u.getPortfolio()),
                has(u.getResumeStoredName())};
        int filled = 0;
        for (boolean d : done) if (d) filled++;
        return new ProfileDto(u.getName(), u.getEmail(), nz(u.getPhone()), nz(u.getLocation()), nz(u.getHeadline()),
                nz(u.getAbout()), nz(u.getLinkedin()), nz(u.getGithub()), nz(u.getPortfolio()),
                nz(u.getResumeFileName()), filled * 100 / done.length);
    }

    private static boolean has(String s) {
        return s != null && !s.isBlank();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}