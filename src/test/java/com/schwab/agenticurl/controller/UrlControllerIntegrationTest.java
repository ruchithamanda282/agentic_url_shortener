package com.schwab.agenticurl.controller;

import com.schwab.agenticurl.dto.ShortenRequest;
import com.schwab.agenticurl.repository.ShortUrlRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UrlControllerIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ShortUrlRepository repo;

    @Test void createAndAnalytics() throws Exception {
        repo.deleteAll();
        String body = "{\"url\":\"https://www.example.com/demo\",\"createdBy\":\"integration-test\"}";
        String response = mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").isString()).andReturn().getResponse().getContentAsString();
        String code = response.replaceAll("(?s).*\\\"code\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
        mvc.perform(get("/api/urls/" + code + "/analytics")).andExpect(status().isOk()).andExpect(jsonPath("$.clicks").value(0));
        mvc.perform(get("/r/" + code)).andExpect(status().isFound()).andExpect(header().string("Location", "https://www.example.com/demo"));
        mvc.perform(get("/api/urls/" + code + "/analytics")).andExpect(jsonPath("$.clicks").value(1));
    }

    @Test void limitsUrlCreationToTenRequestsPerMinutePerClient() throws Exception {
        String remoteAddress = "rate-limit-test-" + UUID.randomUUID();
        String body = "{\"url\":\"https://www.example.com/rate-limited\"}";

        for (int i = 0; i < 10; i++) {
            mvc.perform(post("/api/urls")
                            .with(request -> {
                                request.setRemoteAddr(remoteAddress);
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());
        }

        mvc.perform(post("/api/urls")
                        .with(request -> {
                            request.setRemoteAddr(remoteAddress);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error").value("Rate limit exceeded. Try again later."));
    }
}
