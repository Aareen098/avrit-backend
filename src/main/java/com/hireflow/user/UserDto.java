package com.hireflow.user;

// What the frontend receives. The three company fields are null for candidates.
public record UserDto(Long id, String name, String email, String role,
                      Long companyId, String companyName, String companySlug, boolean emailVerified) {

    public static UserDto from(User u) {
        var c = u.getCompany();
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole().name(),
                c == null ? null : c.getId(), c == null ? null : c.getName(), c == null ? null : c.getSlug(),
                u.isEmailVerified());
    }
}