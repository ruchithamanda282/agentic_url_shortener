package com.schwab.agenticurl.controller;

import com.schwab.agenticurl.service.ShortUrlService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class RedirectController {
    private final ShortUrlService service;
    public RedirectController(ShortUrlService service) { this.service = service; }

    @GetMapping("/r/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(java.net.URI.create(service.resolve(code)));
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }
}
