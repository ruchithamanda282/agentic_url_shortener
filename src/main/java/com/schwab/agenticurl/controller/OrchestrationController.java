package com.schwab.agenticurl.controller;

import com.schwab.agenticurl.orchestration.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/workflows")
public class OrchestrationController {
    private final AgenticOrchestrator orchestrator;
    public OrchestrationController(AgenticOrchestrator orchestrator) { this.orchestrator = orchestrator; }

    @PostMapping
    public ResponseEntity<WorkflowState> create(@Valid @RequestBody WorkflowRequest request) {
        return ResponseEntity.accepted().body(orchestrator.create(request));
    }
    @GetMapping("/{id}")
    public WorkflowState get(@PathVariable String id) { return orchestrator.get(id); }
    @PostMapping("/{id}/approve")
    public Map<String, String> approve(@PathVariable String id, @RequestBody ApprovalRequest request) {
        orchestrator.approve(id, request); return Map.of("workflowId", id, "message", "Approval processed");
    }
    @PostMapping("/{id}/stop")
    public Map<String, String> stop(@PathVariable String id, @RequestParam(defaultValue = "Stopped by operator") String reason) {
        orchestrator.safeStop(id, reason); return Map.of("workflowId", id, "message", "Workflow safe-stopped");
    }
    @PostMapping("/{id}/replan")
    public Map<String, String> replan(@PathVariable String id, @RequestBody Map<String, String> body) {
        orchestrator.replan(id, body.getOrDefault("requirement", "Updated requirement"));
        return Map.of("workflowId", id, "message", "Downstream stages invalidated; approval required to resume");
    }
}
