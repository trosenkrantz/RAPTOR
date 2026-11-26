package com.github.trosenkrantz.raptor.replay;

import com.github.trosenkrantz.raptor.logging.CaptureDirection;
import com.github.trosenkrantz.raptor.logging.CaptureLogger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

class CaptureLogParserTest {
    private static final String PREFIX = "2026-01-02T03:04:05.006 [INFO   ] ";

    @Test
    void parsesPayloadWithoutMetadata() {
        Optional<CapturedEvent> parsed = CaptureLogParser.parseLine(PREFIX + "Received \"Hello, World!\"");

        Assertions.assertTrue(parsed.isPresent());
        CapturedEvent event = parsed.get();
        Assertions.assertEquals(LocalDateTime.of(2026, 1, 2, 3, 4, 5, 6_000_000), event.timestamp());
        Assertions.assertEquals(CaptureDirection.RECEIVED, event.direction());
        Assertions.assertArrayEquals("Hello, World!".getBytes(StandardCharsets.UTF_8), event.data());
        Assertions.assertTrue(event.metadata().isEmpty());
    }

    @Test
    void parsesMetadataAndDirection() {
        Optional<CapturedEvent> parsed = CaptureLogParser.parseLine(
                PREFIX + "Sent [id=\"42\",event=\"update\"] \"abc\""
        );

        Assertions.assertTrue(parsed.isPresent());
        CapturedEvent event = parsed.get();
        Assertions.assertEquals(CaptureDirection.SENT, event.direction());
        Assertions.assertEquals(Map.of("id", "42", "event", "update"), event.metadata());
        Assertions.assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), event.data());
    }

    @Test
    void parsesBinaryPayload() {
        Optional<CapturedEvent> parsed = CaptureLogParser.parseLine(
                PREFIX + "Sent [frame=\"binary\"] \"\\\\x00\\\\xff\""
        );

        Assertions.assertTrue(parsed.isPresent());
        Assertions.assertEquals(Map.of("frame", "binary"), parsed.get().metadata());
        Assertions.assertArrayEquals(new byte[]{0, (byte) 0xff}, parsed.get().data());
    }

    @Test
    void decodesCommandSubstitutionLiterally() {
        Optional<CapturedEvent> parsed = CaptureLogParser.parseLine(PREFIX + "Received \"\\\\$(echo hi)\"");

        Assertions.assertTrue(parsed.isPresent());
        Assertions.assertArrayEquals("\\$(echo hi)".getBytes(StandardCharsets.UTF_8), parsed.get().data());
    }

    @Test
    void roundTripsFormattedLines() {
        String message = CaptureLogger.format(
                CaptureDirection.RECEIVED,
                "a,b\"c\n".getBytes(StandardCharsets.UTF_8),
                Map.of("id", "a,b\"c")
        );

        Optional<CapturedEvent> parsed = CaptureLogParser.parseLine(PREFIX + message);

        Assertions.assertTrue(parsed.isPresent());
        Assertions.assertEquals(Map.of("id", "a,b\"c"), parsed.get().metadata());
        Assertions.assertArrayEquals("a,b\"c\n".getBytes(StandardCharsets.UTF_8), parsed.get().data());
    }

    @Test
    void ignoresUnrelatedAndMalformedLines() {
        Assertions.assertTrue(CaptureLogParser.parseLine("Not a log line").isEmpty());
        Assertions.assertTrue(CaptureLogParser.parseLine("2026-01-02T03:04:05 INFO    Received \"abc\"").isEmpty());
        Assertions.assertTrue(CaptureLogParser.parseLine(PREFIX + "Connected to server.").isEmpty());
        Assertions.assertTrue(CaptureLogParser.parseLine(PREFIX + "Received \"unterminated").isEmpty());
    }
}
