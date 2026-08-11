package de.mimosa_dev.MealPlanner.agent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ToolCallRepository extends JpaRepository<ToolCall, Long> {

    List<ToolCall> findByAgentRunIdOrderBySequenceNumberAsc(Long agentRunId);

    Optional<ToolCall> findByAgentRunIdAndSequenceNumber(Long agentRunId, Integer sequenceNumber);
}
