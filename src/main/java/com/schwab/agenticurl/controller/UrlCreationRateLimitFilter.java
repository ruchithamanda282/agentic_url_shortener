package com.schwab.agenticurl.controller;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class UrlCreationRateLimitFilter extends OncePerRequestFilter {
    private static final int REQUEST_LIMIT = 10;
    private static final long WINDOW_MILLIS = 60_000;

    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !"/api/urls".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long now = System.currentTimeMillis();
        AtomicBoolean allowed = new AtomicBoolean();
        Window window = windows.compute(request.getRemoteAddr(), (client, current) -> {
            if (current == null || now - current.startedAtMillis >= WINDOW_MILLIS) {
                allowed.set(true);
                return new Window(now, 1);
            }
            if (current.requestCount < REQUEST_LIMIT) {
                current.requestCount++;
                allowed.set(true);
            }
            return current;
        });

        if (!allowed.get()) {
            long retryAfterSeconds = Math.max(
                    1,
                    (window.startedAtMillis + WINDOW_MILLIS - now + 999) / 1000
            );
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
            response.getWriter().write("{\"error\":\"Rate limit exceeded. Try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static final class Window {
        private final long startedAtMillis;
        private int requestCount;

        private Window(long startedAtMillis, int requestCount) {
            this.startedAtMillis = startedAtMillis;
            this.requestCount = requestCount;
        }
    }
}
