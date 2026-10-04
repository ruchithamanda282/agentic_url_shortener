package com.schwab.agenticurl.dto;

import java.time.Instant;

public record ShortenResponse(String code, String shortUrl, String originalUrl, Instant createdAt) {}
