package com.hireflow.interview;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    // This company's interviews, newest date first; the frontend splits them into Upcoming and Past.
    List<Interview> findByApplication_Job_Company_IdOrderByInterviewDateDescInterviewTimeDescIdDesc(Long companyId);
}