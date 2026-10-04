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

}
