package com.github.trosenkrantz.raptor.logging;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

class CaptureLoggerTest {
    @Test
    void formatsTextWithoutMetadata() {
        Assertions.assertEquals(
                "Received \"Hello, World!\"",
                CaptureLogger.format(
                        CaptureDirection.RECEIVED,
                        "Hello, World!".getBytes(StandardCharsets.UTF_8),
                        Map.of()
                )
        );
    }

    @Test
    void formatsMetadataInDocumentedOrder() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("event", "update");
        metadata.put("id", "42");

        Assertions.assertEquals(
                "Sent [id=\"42\",event=\"update\"] \"abc\"",
                CaptureLogger.format(CaptureDirection.SENT, "abc".getBytes(StandardCharsets.UTF_8), metadata)
        );
    }

    @Test
    void formatsBinaryPayloadWithFrameMetadata() {
        Assertions.assertEquals(
                "Sent [frame=\"binary\"] \"\\\\x00\\\\xff\"",
                CaptureLogger.format(
                        CaptureDirection.SENT,
                        new byte[]{0, (byte) 0xff},
                        Map.of("frame", "binary")
                )
        );
    }

    @Test
    void escapesMetadataValues() {
        Assertions.assertEquals(
                "Received [id=\"a,b\\\"c\"] \"x\"",
                CaptureLogger.format(
                        CaptureDirection.RECEIVED,
                        "x".getBytes(StandardCharsets.UTF_8),
                        Map.of("id", "a,b\"c")
                )
        );
    }

    @Test
    void timestampFormatterIncludesMilliseconds() {
        Assertions.assertEquals(
                "2026-01-02T03:04:05.006",
                LocalDateTime.of(2026, 1, 2, 3, 4, 5, 6_000_000).format(CaptureLogger.TIMESTAMP_FORMATTER)
        );
    }
}
