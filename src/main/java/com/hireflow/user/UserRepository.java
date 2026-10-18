package com.hireflow.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Everyone except candidates - the Team tab in Settings.
    List<User> findByCompany_IdOrderByIdAsc(Long companyId);
}