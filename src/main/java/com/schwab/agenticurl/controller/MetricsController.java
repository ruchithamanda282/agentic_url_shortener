package com.schwab.agenticurl.controller;

import com.schwab.agenticurl.orchestration.AgenticOrchestrator;
import com.schwab.agenticurl.orchestration.WorkflowState;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {
    private final AgenticOrchestrator orchestrator;
    public MetricsController(AgenticOrchestrator orchestrator) { this.orchestrator = orchestrator; }

    @GetMapping("/{workflowId}")
    public Map<String,Object> metrics(@PathVariable String workflowId) {
        WorkflowState s = orchestrator.get(workflowId);
        long latency = s.getStartedAt() == null ? 0 : Duration.between(s.getStartedAt(), s.getFinishedAt() == null ? Instant.now() : s.getFinishedAt()).toMillis();
        AtomicInteger done = new AtomicInteger();
        s.getStages().values().forEach(x -> { if (x.getStatus().name().equals("SUCCEEDED")) done.incrementAndGet(); });
        double successRate = done.get() / (double)s.getStages().size();
        return Map.of("workflowId", workflowId, "stageSuccessRate", successRate, "retryCount", s.getRetryCount(), "rollbackCount", s.getRollbackCount(), "endToEndLatencyMs", latency, "status", s.getStatus());
    }
}
