package com.schwab.agenticurl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShortenRequest(
        @NotBlank @Size(max = 2048) String url,
        @Size(max = 64) String createdBy
) {}
