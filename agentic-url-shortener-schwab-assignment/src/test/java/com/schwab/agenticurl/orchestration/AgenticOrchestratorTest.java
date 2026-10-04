package com.schwab.agenticurl.orchestration;

import org.junit.jupiter.api.Test;
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

    @Test void autoApproveRunsWorkflowToSucceeded() throws InterruptedException {
        AgenticOrchestrator o = new AgenticOrchestrator();
        WorkflowState created = o.create(new WorkflowRequest(ScenarioType.GREENFIELD, "Create a URL shortener", true));
        String id = created.getWorkflowId();

        // run() executes asynchronously; wait for terminal state
        WorkflowState s = null;
        long deadline = System.currentTimeMillis() + 10_000;
        do {
            Thread.sleep(100);
            s = o.get(id);
        } while (System.currentTimeMillis() < deadline
                && s.getStatus() != WorkflowStatus.SUCCEEDED
                && s.getStatus() != WorkflowStatus.SAFE_STOPPED);

        assertEquals(WorkflowStatus.SUCCEEDED, s.getStatus(),
                "autoApprove=true must drive the workflow to SUCCEEDED without human gates");
        for (Stage stage : Stage.values()) {
            assertEquals(StageStatus.SUCCEEDED, s.getStages().get(stage).getStatus(),
                    "stage " + stage + " should have succeeded");
        }
        assertTrue(s.getAudit().stream().anyMatch(e -> e.contains("AUTO_APPROVAL_ENABLED")),
                "audit trail must record AUTO_APPROVAL_ENABLED on the executed workflow");
    }

}
