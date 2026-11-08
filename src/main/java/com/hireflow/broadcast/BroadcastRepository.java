package com.hireflow.broadcast;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BroadcastRepository extends JpaRepository<Broadcast, Long> {

    List<Broadcast> findTop50ByOrderByIdDesc();
}