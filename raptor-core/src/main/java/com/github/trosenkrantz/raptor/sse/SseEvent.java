package com.github.trosenkrantz.raptor.sse;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** One Server-Sent Event with optional {@code id} and {@code event} fields and text-only data. */
final class SseEvent {
    private final String id;
    private final String event;
    private final byte[] data;

    SseEvent(String id, String event, byte[] data) {
        this.id = id;
        this.event = event;
        this.data = data;
    }

    SseEvent(byte[] data) {
        this(null, null, data);
    }

    String id() {
        return id;
    }

    String event() {
        return event;
    }

    byte[] data() {
        return data;
    }

    static String format(byte[] data) {
        return format(data, null, null);
    }

    /** Formats a complete SSE event, including the terminating blank line. */
    static String format(byte[] data, String id, String event) {
        String text = decodeUtf8(data);
        if (text.indexOf('\r') >= 0) throw new IllegalArgumentException("Carriage return cannot be sent in a Server-Sent Event.");

        StringBuilder formatted = new StringBuilder();
        if (id != null) formatted.append("id: ").append(validateField("id", id)).append('\n');
        if (event != null) formatted.append("event: ").append(validateField("event", event)).append('\n');
        for (String line : text.split("\n", -1)) {
            formatted.append("data: ").append(line).append('\n');
        }
        return formatted.append('\n').toString();
    }

    static String decodeField(String name, byte[] bytes) {
        return validateField(name, decodeUtf8(bytes));
    }

    static String validateField(String name, String value) {
        if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("SSE " + name + " cannot contain carriage return, line feed, or NUL.");
        }
        return value;
    }

    private static String decodeUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Server-Sent Events require valid UTF-8 data.", e);
        }
    }
}
