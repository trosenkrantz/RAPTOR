package com.github.trosenkrantz.raptor.web.sse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class SseServerTest {
    @Test
    void streamsEventsToSubscribers() throws Exception {
        SseServer server = new SseServer(0, null);
        Thread.ofVirtual().start(server::run);
        LinkedBlockingQueue<byte[]> receivedEvents = new LinkedBlockingQueue<>();

        try {
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create("http://localhost:" + server.getPort() + "/events"))
                    .header("Accept", "text/event-stream")
                    .GET()
                    .build();
            HttpResponse.BodyHandler<Void> bodyHandler = HttpResponse.BodyHandlers.fromSubscriber(
                    new SseEventSubscriber(receivedEvents::add));
            CompletableFuture<HttpResponse<Void>> responseFuture = client.sendAsync(request, bodyHandler);

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (server.getSubscriberCount() == 0 && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            Assertions.assertEquals(1, server.getSubscriberCount(), "Client did not subscribe.");

            server.send(SseEvent.format("Hello, World!".getBytes(StandardCharsets.UTF_8)));
            server.send(SseEvent.format("a\nb".getBytes(StandardCharsets.UTF_8)));

            Assertions.assertArrayEquals(
                    "Hello, World!".getBytes(StandardCharsets.UTF_8),
                    receivedEvents.poll(5, TimeUnit.SECONDS));
            Assertions.assertArrayEquals(
                    "a\nb".getBytes(StandardCharsets.UTF_8),
                    receivedEvents.poll(5, TimeUnit.SECONDS));

            server.close();
            Assertions.assertEquals(200, responseFuture.join().statusCode());
        } finally {
            server.close();
        }
    }
}
