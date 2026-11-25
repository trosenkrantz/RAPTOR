package com.github.trosenkrantz.raptor.sse;

import com.github.trosenkrantz.raptor.tls.TlsVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;

class ServerSentEventsServiceTest {
    @Test
    void acceptsHttpUriWithoutTls() {
        Assertions.assertDoesNotThrow(() -> ServerSentEventsService.validateUriScheme(
                URI.create("http://localhost:50000/events"), TlsVersion.NONE));
    }

    @Test
    void acceptsHttpsUriWithTls() {
        Assertions.assertDoesNotThrow(() -> ServerSentEventsService.validateUriScheme(
                URI.create("https://localhost:50000/events"), TlsVersion.V1_2));
    }

    @Test
    void rejectsHttpUriWithTls() {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> ServerSentEventsService.validateUriScheme(
                        URI.create("http://localhost:50000/events"), TlsVersion.V1_2));
        Assertions.assertTrue(exception.getMessage().contains("https"));
    }

    @Test
    void rejectsHttpsUriWithoutTls() {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> ServerSentEventsService.validateUriScheme(
                        URI.create("https://localhost:50000/events"), TlsVersion.NONE));
        Assertions.assertTrue(exception.getMessage().contains("http"));
    }
}
