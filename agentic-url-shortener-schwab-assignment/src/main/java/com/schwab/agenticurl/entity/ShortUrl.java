package com.schwab.agenticurl.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "short_urls", indexes = {
        @Index(name = "idx_short_code", columnList = "short_code", unique = true)
})
public class ShortUrl {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "short_code", nullable = false, unique = true, length = 32)
    private String shortCode;
    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private long clickCount;
    @Column(length = 64)
    private String createdBy;

    protected ShortUrl() {}
    public ShortUrl(String shortCode, String originalUrl, String createdBy) {
        this.shortCode = shortCode; this.originalUrl = originalUrl; this.createdBy = createdBy;
        this.createdAt = Instant.now(); this.clickCount = 0;
    }
    public Long getId() { return id; }
    public String getShortCode() { return shortCode; }
    public String getOriginalUrl() { return originalUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public long getClickCount() { return clickCount; }
    public String getCreatedBy() { return createdBy; }
    public void incrementClickCount() { clickCount++; }
}
