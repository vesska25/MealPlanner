package de.mimosa_dev.MealPlanner.agent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One agent-loop execution (AI-30, AI-34). {@code systemPromptVersion} is a hash of the
 * external prompt resource file's content at run time (AI-33/AI-34), so a past run's behavior
 * can always be traced back to the exact instructions that produced it.
 */
@Entity
@Table(name = "agent_run")
public class AgentRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scenario", nullable = false)
    private AgentScenario scenario;

    @Column(name = "trigger", nullable = false)
    private String trigger;

    @Column(name = "system_prompt_version", nullable = false)
    private String systemPromptVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AgentRunStatus status;

    @Column(name = "iteration_count", nullable = false)
    private Integer iterationCount = 0;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected AgentRun() {
    }

    public AgentRun(Long userId, AgentScenario scenario, String trigger, String systemPromptVersion) {
        this.userId = userId;
        this.scenario = scenario;
        this.trigger = trigger;
        this.systemPromptVersion = systemPromptVersion;
        this.status = AgentRunStatus.RUNNING;
        this.iterationCount = 0;
        this.startedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public AgentScenario getScenario() {
        return scenario;
    }

    public String getTrigger() {
        return trigger;
    }

    public String getSystemPromptVersion() {
        return systemPromptVersion;
    }

    public AgentRunStatus getStatus() {
        return status;
    }

    public Integer getIterationCount() {
        return iterationCount;
    }

    public void incrementIterationCount() {
        this.iterationCount++;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void finish(AgentRunStatus status) {
        finish(status, null);
    }

    public void finish(AgentRunStatus status, String errorMessage) {
        this.status = status;
        this.errorMessage = errorMessage;
        this.finishedAt = Instant.now();
    }
}
