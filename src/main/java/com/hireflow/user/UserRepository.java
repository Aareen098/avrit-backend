package com.hireflow.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleSub(String googleSub);

    boolean existsByEmail(String email);

    // The Team tab in Settings: only people of the same company.
    List<User> findByCompany_IdOrderByIdAsc(Long companyId);

    // Platform owner: only how many seats each company uses, rows of [companyId, number of staff accounts].
    @Query("select u.company.id, count(u) from User u where u.company is not null group by u.company.id")
    List<Object[]> countStaffPerCompany();

    // Platform owner messages: everyone who works for a company, and every candidate account.
    List<User> findByCompanyIsNotNull();

    List<User> findByRole(Role role);
}