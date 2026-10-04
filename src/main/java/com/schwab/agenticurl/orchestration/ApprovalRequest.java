package com.schwab.agenticurl.orchestration;

public record ApprovalRequest(Stage stage, boolean approved, String reason) {}
