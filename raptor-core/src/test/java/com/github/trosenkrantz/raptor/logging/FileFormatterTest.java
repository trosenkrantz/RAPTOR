package com.github.trosenkrantz.raptor.logging;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class FileFormatterTest {
    @Test
    void timestampFormatterIncludesMilliseconds() {
        Assertions.assertEquals(
                "2026-01-02T03:04:05.006",
                LocalDateTime.of(2026, 1, 2, 3, 4, 5, 6_000_000).format(FileFormatter.TIMESTAMP_FORMATTER)
        );
    }
}
