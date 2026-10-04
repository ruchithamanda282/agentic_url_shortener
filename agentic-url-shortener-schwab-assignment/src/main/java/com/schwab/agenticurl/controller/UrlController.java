package com.schwab.agenticurl.controller;

import com.schwab.agenticurl.dto.*;
import com.schwab.agenticurl.service.ShortUrlService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/urls")
public class UrlController {
    private final ShortUrlService service;
    public UrlController(ShortUrlService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        ShortenResponse response = service.shorten(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{code}").buildAndExpand(response.code()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{code}/analytics")
    public AnalyticsResponse analytics(@PathVariable String code) { return service.analytics(code); }

    @GetMapping("/{code}")
    public Map<String, String> resolve(@PathVariable String code) { return Map.of("url", service.resolve(code)); }
}
