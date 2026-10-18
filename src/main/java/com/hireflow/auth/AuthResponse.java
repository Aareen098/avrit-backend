package com.hireflow.auth;

import com.hireflow.user.UserDto;

// What /auth/login sends back: { token, user: { id, name, email, role } }
public record AuthResponse(String token, UserDto user) {
}