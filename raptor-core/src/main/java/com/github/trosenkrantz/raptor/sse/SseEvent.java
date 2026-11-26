package com.github.trosenkrantz.raptor.sse;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * One Server-Sent Event with optional {@code id} and {@code event} fields and text-only data.
 *
 * <p>SSE data fields must be valid UTF-8 and cannot contain a carriage return, as that would terminate the field.
 * Line feeds are preserved by splitting the payload into multiple {@code data:} fields, which SSE clients join back together with line feeds.</p>
 *
 * <p>ID and event fields must be valid UTF-8 and cannot contain carriage return, line feed, or NUL.</p>
 */
final class SseEvent {
    private final Optional<String> id;
    private final Optional<String> event;
    private final byte[] data;

    SseEvent(Optional<String> id, Optional<String> event, byte[] data) {
        this.id = id;
        this.event = event;
        this.data = data;
    }

    SseEvent(byte[] data) {
        this(Optional.empty(), Optional.empty(), data);
    }

    Optional<String> id() {
        return id;
    }

    Optional<String> event() {
        return event;
    }

    byte[] data() {
        return data;
    }

    static String format(byte[] data) {
        return format(data, Optional.empty(), Optional.empty());
    }

    /**
     * Formats a payload as a complete SSE event, including the terminating blank line.
     *
     * @param data  bytes to send
     * @param id    optional event id
     * @param event optional event type
     * @return the event encoded as UTF-8 text
     * @throws IllegalArgumentException if any field cannot be represented as an SSE field
     */
    static String format(byte[] data, Optional<String> id, Optional<String> event) {
        String text = decodeUtf8(data);
        if (text.indexOf('\r') >= 0) throw new IllegalArgumentException("Carriage return cannot be sent in a Server-Sent Event.");

        StringBuilder formatted = new StringBuilder();
        if (id.isPresent()) formatted.append("id: ").append(validateField("id", id.get())).append('\n');
        if (event.isPresent()) formatted.append("event: ").append(validateField("event", event.get())).append('\n');
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
