package com.hireflow.platform;

import com.hireflow.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Decides who the platform owner is: the one account whose email is MASTER_ADMIN_EMAIL.
// Nothing in the database can make someone a platform owner, and the app has no screen to promote anyone.
// The email must also be verified (signed in with Google, or confirmed by email link), so nobody can sign up
// with your address first and become the owner.
// The value is kept in a static field so User and the token code can read it without extra wiring.
@Component
public class PlatformAccess {

    public static final String ROLE = "PLATFORM_ADMIN";

    private static volatile String masterEmail = "";

    public PlatformAccess(@Value("${app.platform.master-email:}") String email) {
        masterEmail = email == null ? "" : email.trim().toLowerCase();
    }

    public static boolean isMasterEmail(String email) {
        return !masterEmail.isEmpty() && email != null && masterEmail.equals(email.trim().toLowerCase());
    }

    public static boolean isMaster(User user) {
        return isMasterEmail(user.getEmail()) && user.isEmailVerified();
    }

    public static String roleOf(User user) {
        return isMaster(user) ? ROLE : user.getRole().name();
    }
}