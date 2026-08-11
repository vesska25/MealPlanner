package de.mimosa_dev.MealPlanner.agent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentRunRepository extends JpaRepository<AgentRun, Long> {

    // FR-04 (full data export).
    List<AgentRun> findByUserId(Long userId);
}
