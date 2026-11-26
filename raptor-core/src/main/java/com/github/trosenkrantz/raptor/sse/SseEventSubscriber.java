package com.github.trosenkrantz.raptor.sse;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

/** HTTP response subscriber that dispatches each complete Server-Sent Event. */
class SseEventSubscriber implements HttpResponse.BodySubscriber<Void> {
    private final Consumer<SseEvent> onEvent;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final CompletableFuture<Void> result = new CompletableFuture<>();
    private Flow.Subscription subscription;

    SseEventSubscriber(Consumer<SseEvent> onEvent) {
        this.onEvent = onEvent;
    }

    @Override
    public CompletionStage<Void> getBody() {
        return result;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(List<ByteBuffer> item) {
        try {
            for (ByteBuffer bytes : item) {
                while (bytes.hasRemaining()) {
                    buffer.write(bytes.get());
                }
            }

            int delimiterIndex;
            while ((delimiterIndex = findDelimiter()) >= 0) {
                byte[] all = buffer.toByteArray();
                int delimiterLength = all[delimiterIndex] == '\r' ? 4 : 2;
                dispatchEvent(Arrays.copyOfRange(all, 0, delimiterIndex));

                byte[] remainder = Arrays.copyOfRange(all, delimiterIndex + delimiterLength, all.length);
                buffer.reset();
                buffer.write(remainder, 0, remainder.length);
            }
        } catch (Exception e) {
            onError(e);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        if (subscription != null) subscription.cancel();
        result.completeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        result.complete(null);
    }

    private int findDelimiter() {
        byte[] bytes = buffer.toByteArray();
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == '\n' && i + 1 < bytes.length && bytes[i + 1] == '\n') return i;
            if (bytes[i] == '\r' && i + 3 < bytes.length
                    && bytes[i + 1] == '\n' && bytes[i + 2] == '\r' && bytes[i + 3] == '\n') return i;
        }
        return -1;
    }

    private void dispatchEvent(byte[] eventBytes) {
        String text = new String(eventBytes, StandardCharsets.UTF_8).replace("\r\n", "\n");
        Optional<String> id = Optional.empty();
        Optional<String> event = Optional.empty();
        StringBuilder data = new StringBuilder();
        int dataLines = 0;
        for (String line : text.split("\n", -1)) {
            if (line.startsWith("data:")) {
                String value = fieldValue(line, "data");
                if (dataLines++ > 0) data.append('\n');
                data.append(value);
            } else if (line.startsWith("id:")) {
                id = Optional.of(fieldValue(line, "id"));
            } else if (line.startsWith("event:")) {
                event = Optional.of(fieldValue(line, "event"));
            }
        }
        if (dataLines > 0) {
            onEvent.accept(new SseEvent(id, event, data.toString().getBytes(StandardCharsets.UTF_8)));
        }
    }

    private static String fieldValue(String line, String field) {
        String value = line.substring(field.length() + 1);
        if (value.startsWith(" ")) value = value.substring(1);
        return value;
    }
}
