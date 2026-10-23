package com.hireflow.auth;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordTokenRepository extends JpaRepository<PasswordToken, Long> {

    Optional<PasswordToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from PasswordToken t where t.user.id = :userId")
    void deleteForUser(@Param("userId") Long userId);

    @Modifying
    @Query("delete from PasswordToken t where t.expiresAt < :now")
    void deleteExpired(@Param("now") Instant now);
}