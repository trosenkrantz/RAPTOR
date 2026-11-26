package com.github.trosenkrantz.raptor.sse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

class SseEventTest {
    @Test
    void formatsTextEvent() {
        Assertions.assertEquals(
                "data: Hello, World!\n\n",
                SseEvent.format("Hello, World!".getBytes(StandardCharsets.UTF_8))
        );
    }

    @Test
    void splitsLineFeedsIntoMultipleDataFields() {
        Assertions.assertEquals(
                "data: a\ndata: b\n\n",
                SseEvent.format("a\nb".getBytes(StandardCharsets.UTF_8))
        );
    }

    @Test
    void allowsNul() {
        Assertions.assertEquals("data: \0\n\n", SseEvent.format(new byte[]{0}));
    }

    @Test
    void preservesMultibyteUtf8() {
        Assertions.assertEquals(
                "data: héllo\n\n",
                SseEvent.format("héllo".getBytes(StandardCharsets.UTF_8))
        );
    }

    @Test
    void rejectsCarriageReturn() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SseEvent.format("a\rb".getBytes(StandardCharsets.UTF_8))
        );
    }

    @Test
    void rejectsInvalidUtf8() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SseEvent.format(new byte[]{(byte) 0xFF})
        );
    }

    @Test
    void formatsIdAndEventFields() {
        Assertions.assertEquals(
                "id: 42\nevent: update\ndata: Hello\n\n",
                SseEvent.format("Hello".getBytes(StandardCharsets.UTF_8), Optional.of("42"), Optional.of("update"))
        );
    }

    @Test
    void rejectsLineFeedInId() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SseEvent.format("Hello".getBytes(StandardCharsets.UTF_8), Optional.of("42\n43"), Optional.of("update"))
        );
    }

    @Test
    void rejectsNulInEvent() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SseEvent.format("Hello".getBytes(StandardCharsets.UTF_8), Optional.of("42"), Optional.of("update\0"))
        );
    }
}
