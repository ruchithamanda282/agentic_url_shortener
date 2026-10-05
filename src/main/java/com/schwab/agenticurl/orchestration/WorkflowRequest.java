package com.schwab.agenticurl.orchestration;

import jakarta.validation.constraints.NotBlank;

public record WorkflowRequest(ScenarioType scenario, @NotBlank String requirement, Boolean autoApprove) {}
