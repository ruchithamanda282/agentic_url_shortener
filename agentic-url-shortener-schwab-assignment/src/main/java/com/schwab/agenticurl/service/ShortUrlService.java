package com.schwab.agenticurl.service;

import com.schwab.agenticurl.dto.*;
import com.schwab.agenticurl.entity.ShortUrl;
import com.schwab.agenticurl.exception.NotFoundException;
import com.schwab.agenticurl.repository.ShortUrlRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.security.SecureRandom;
import java.util.regex.Pattern;

@Service
public class ShortUrlService {
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Pattern HTTP_URL = Pattern.compile("^https?://.+$", Pattern.CASE_INSENSITIVE);
    private final SecureRandom random = new SecureRandom();
    private final ShortUrlRepository repository;

    public ShortUrlService(ShortUrlRepository repository) { this.repository = repository; }

    @Transactional
    public ShortenResponse shorten(ShortenRequest request) {
        if (!HTTP_URL.matcher(request.url().trim()).matches()) {
            throw new IllegalArgumentException("Only http/https URLs are supported");
        }
        try { URI.create(request.url()); } catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Invalid URL"); }
        String code;
        do { code = randomCode(7); } while (repository.existsByShortCode(code));
        ShortUrl saved = repository.save(new ShortUrl(code, request.url().trim(), request.createdBy()));
        return toResponse(saved);
    }

    @Transactional
    public String resolve(String code) {
        ShortUrl url = repository.findByShortCode(code).orElseThrow(() -> new NotFoundException("Short URL not found: " + code));
        url.incrementClickCount();
        return url.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse analytics(String code) {
        ShortUrl url = repository.findByShortCode(code).orElseThrow(() -> new NotFoundException("Short URL not found: " + code));
        return new AnalyticsResponse(url.getShortCode(), url.getOriginalUrl(), url.getClickCount(), url.getCreatedAt());
    }

    private ShortenResponse toResponse(ShortUrl x) {
        return new ShortenResponse(x.getShortCode(), "/r/" + x.getShortCode(), x.getOriginalUrl(), x.getCreatedAt());
    }
    private String randomCode(int length) {
        StringBuilder b = new StringBuilder(length);
        for (int i = 0; i < length; i++) b.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        return b.toString();
    }
}
