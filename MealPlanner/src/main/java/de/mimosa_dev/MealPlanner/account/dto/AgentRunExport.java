package de.mimosa_dev.MealPlanner.account.dto;

import java.time.Instant;

public record AgentRunExport(
        Long id, String scenario, String trigger, String status,
        Integer iterationCount, Instant startedAt, Instant finishedAt) {
}
