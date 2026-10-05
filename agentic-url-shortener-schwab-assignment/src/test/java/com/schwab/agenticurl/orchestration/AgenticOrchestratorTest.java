package com.schwab.agenticurl.orchestration;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AgenticOrchestratorTest {
    @Test void graphContainsParallelTestingAndDocumentation() {
        AgenticOrchestrator o = new AgenticOrchestrator();
        WorkflowState s = o.create(new WorkflowRequest(ScenarioType.GREENFIELD, "Create a URL shortener", true));
        assertEquals(java.util.List.of("IMPLEMENTATION"), s.getStages().get(Stage.TESTING).getDependencies());
        assertEquals(java.util.List.of("IMPLEMENTATION"), s.getStages().get(Stage.DOCUMENTATION).getDependencies());
        assertEquals(java.util.List.of("TESTING", "DOCUMENTATION"), s.getStages().get(Stage.RELEASE).getDependencies());
    }

    @Test void manualWorkflowStartsBehindHumanGate() {
        AgenticOrchestrator o = new AgenticOrchestrator();
        WorkflowState s = o.create(new WorkflowRequest(ScenarioType.AMBIGUOUS, "Need a shortener", false));
        assertEquals(WorkflowStatus.WAITING_APPROVAL, s.getStatus());
    }

    @Test void autoApprovedWorkflowCompletes() throws InterruptedException {
        AgenticOrchestrator orchestrator = new AgenticOrchestrator();
        WorkflowState state = orchestrator.create(new WorkflowRequest(
                ScenarioType.GREENFIELD, "Create a URL shortener", true));

        awaitStatus(state, WorkflowStatus.SUCCEEDED);

        assertTrue(state.getStages().values().stream()
                .allMatch(stage -> stage.getStatus() == StageStatus.SUCCEEDED));
    }

    @Test void timedOutStageIsAuditedAndEventuallySafeStops() throws InterruptedException {
        AgenticOrchestrator orchestrator = new AgenticOrchestrator(Duration.ofMillis(1)) {
            @Override
            String generate(Stage stage, WorkflowState state) {
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(1));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "late output";
            }
        };
        WorkflowState state = orchestrator.create(new WorkflowRequest(
                ScenarioType.GREENFIELD, "Timeout test", true));

        awaitStatus(state, WorkflowStatus.SAFE_STOPPED);

        assertEquals(StageStatus.FAILED, state.getStages().get(Stage.REQUIREMENTS).getStatus());
        assertEquals(3, state.getStages().get(Stage.REQUIREMENTS).getAttempts());
        assertTrue(state.getAudit().stream().anyMatch(event -> event.contains(
                "STAGE_TIMEOUT stage=REQUIREMENTS timeoutMs=1")));
    }

    private void awaitStatus(WorkflowState state, WorkflowStatus expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (state.getStatus() != expected && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertEquals(expected, state.getStatus());
    }

}
