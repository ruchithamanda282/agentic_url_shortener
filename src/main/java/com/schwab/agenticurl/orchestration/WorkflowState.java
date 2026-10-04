package com.schwab.agenticurl.orchestration;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WorkflowState {
    private final String workflowId;
    private final ScenarioType scenario;
    private final String requirement;
    private WorkflowStatus status = WorkflowStatus.CREATED;
    private final Instant createdAt = Instant.now();
    private Instant updatedAt = createdAt;
    private int retryCount;
    private int rollbackCount;
    private Instant startedAt;
    private Instant finishedAt;
    private String stopReason;
    private final Map<Stage, StageState> stages = new ConcurrentHashMap<>();
    private final List<String> audit = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Object> context = new ConcurrentHashMap<>();
    private final Set<Stage> approvals = ConcurrentHashMap.newKeySet();

    public WorkflowState(String workflowId, ScenarioType scenario, String requirement) {
        this.workflowId = workflowId; this.scenario = scenario; this.requirement = requirement;
    }
    public String getWorkflowId() { return workflowId; }
    public ScenarioType getScenario() { return scenario; }
    public String getRequirement() { return requirement; }
    public WorkflowStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public int getRetryCount() { return retryCount; }
    public int getRollbackCount() { return rollbackCount; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public String getStopReason() { return stopReason; }
    public Map<Stage, StageState> getStages() { return stages; }
    public List<String> getAudit() { return audit; }
    public Map<String, Object> getContext() { return context; }
    public Set<Stage> getApprovals() { return approvals; }
    public void setStatus(WorkflowStatus status) { this.status = status; touch(); }
    public void setStopReason(String stopReason) { this.stopReason = stopReason; touch(); }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; touch(); }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; touch(); }
    public void incRetry() { retryCount++; touch(); }
    public void incRollback() { rollbackCount++; touch(); }
    public void audit(String event) { audit.add(Instant.now() + " | " + event); touch(); }
    private void touch() { updatedAt = Instant.now(); }
}
