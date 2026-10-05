package com.schwab.agenticurl.orchestration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class StageState {
    private Stage stage;
    private StageStatus status = StageStatus.PENDING;
    private int attempts;
    private Instant startedAt;
    private Instant finishedAt;
    private String output;
    private String error;
    private final List<String> dependencies = new ArrayList<>();
    public StageState() {}
    public StageState(Stage stage, List<Stage> deps) { this.stage = stage; deps.forEach(x -> dependencies.add(x.name())); }
    public Stage getStage() { return stage; }
    public StageStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public String getOutput() { return output; }
    public String getError() { return error; }
    public List<String> getDependencies() { return dependencies; }
    public void setStatus(StageStatus status) { this.status = status; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
    public void setOutput(String output) { this.output = output; }
    public void setError(String error) { this.error = error; }
}
