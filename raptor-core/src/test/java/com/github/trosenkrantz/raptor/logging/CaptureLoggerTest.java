package com.github.trosenkrantz.raptor.logging;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
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
    void formatsMetadataAlphabetically() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("event", "update");
        metadata.put("id", "42");

        Assertions.assertEquals(
                "Sent [event=\"update\",id=\"42\"] \"abc\"",
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
}
