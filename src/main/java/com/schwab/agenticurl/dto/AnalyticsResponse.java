package com.schwab.agenticurl.dto;

import java.time.Instant;

public record AnalyticsResponse(String code, String originalUrl, long clicks, Instant createdAt) {}
