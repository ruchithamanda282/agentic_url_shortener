package com.schwab.agenticurl.service;

import com.schwab.agenticurl.dto.ShortenRequest;
import com.schwab.agenticurl.entity.ShortUrl;
import com.schwab.agenticurl.repository.ShortUrlRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;

class ShortUrlServiceTest {
    @Test void shortenCreatesSevenCharacterCode() {
        ShortUrlRepository repo = Mockito.mock(ShortUrlRepository.class);
        Mockito.when(repo.existsByShortCode(Mockito.anyString())).thenReturn(false);
        Mockito.when(repo.save(Mockito.any(ShortUrl.class))).thenAnswer(i -> i.getArgument(0));
        ShortUrlService service = new ShortUrlService(repo);
        var result = service.shorten(new ShortenRequest("https://example.com/a", "test"));
        assertEquals(7, result.code().length());
        assertEquals("https://example.com/a", result.originalUrl());
    }

    @Test void rejectsNonHttpUrl() {
        ShortUrlRepository repo = Mockito.mock(ShortUrlRepository.class);
        ShortUrlService service = new ShortUrlService(repo);
        assertThrows(IllegalArgumentException.class, () -> service.shorten(new ShortenRequest("ftp://bad", null)));
    }
}
