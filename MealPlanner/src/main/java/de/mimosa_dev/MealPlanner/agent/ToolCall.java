package de.mimosa_dev.MealPlanner.agent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One tool invocation within an {@link AgentRun} (AI-31). {@code sequenceNumber} together with
 * the owning run is the idempotency key state-changing tools are keyed by (AI-15) — the
 * {@code uq_tool_call_run_sequence} constraint is what makes "was this call already applied" a
 * lookup rather than a convention.
 */
@Entity
@Table(name = "tool_call")
public class ToolCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_run_id", nullable = false)
    private AgentRun agentRun;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "tool_name", nullable = false)
    private String toolName;

    @Column(name = "arguments", nullable = false)
    private String arguments;

    @Column(name = "result")
    private String result;

    @Column(name = "is_error", nullable = false)
    private boolean error;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected ToolCall() {
    }

    public ToolCall(AgentRun agentRun, Integer sequenceNumber, String toolName, String arguments, String result, boolean error) {
        this.agentRun = agentRun;
        this.sequenceNumber = sequenceNumber;
        this.toolName = toolName;
        this.arguments = arguments;
        this.result = result;
        this.error = error;
    }

    public Long getId() {
        return id;
    }

    public AgentRun getAgentRun() {
        return agentRun;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public String getToolName() {
        return toolName;
    }

    public String getArguments() {
        return arguments;
    }

    public String getResult() {
        return result;
    }

    public boolean isError() {
        return error;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
