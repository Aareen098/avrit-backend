package com.hireflow.user;

import com.hireflow.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    private final UserRepository users;

    public MeController(UserRepository users) {
        this.users = users;
    }

    // GET /api/me  -> the logged-in user (needs a valid token)
    @GetMapping("/me")
    public UserDto me(Authentication auth) {
        User user = users.findByEmail(auth.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
        return UserDto.from(user);
    }
}