package com.github.trosenkrantz.raptor.sse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class SseServerTest {
    @Test
    void streamsEventsToSubscribers() throws Exception {
        SseServer server = new SseServer(0, null);
        Thread.ofVirtual().start(server::run);
        LinkedBlockingQueue<SseEvent> receivedEvents = new LinkedBlockingQueue<>();
        CountDownLatch connected = new CountDownLatch(1);

        try {
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + server.getPort() + "/events"))
                    .header("Accept", "text/event-stream")
                    .GET()
                    .build();
            HttpResponse.BodyHandler<Void> bodyHandler = responseInfo -> {
                connected.countDown();
                return new SseEventSubscriber(receivedEvents::add);
            };
            CompletableFuture<HttpResponse<Void>> responseFuture = client.sendAsync(request, bodyHandler);

            Assertions.assertTrue(connected.await(5, TimeUnit.SECONDS), "Client did not connect.");
            Assertions.assertEquals(1, server.getSubscriberCount(), "Client did not subscribe.");

            server.send(SseEvent.format("Hello, World!".getBytes(StandardCharsets.UTF_8)));
            server.send(SseEvent.format("a\nb".getBytes(StandardCharsets.UTF_8), Optional.of("42"), Optional.of("update")));

            SseEvent first = receivedEvents.poll(5, TimeUnit.SECONDS);
            Assertions.assertNotNull(first);
            Assertions.assertArrayEquals("Hello, World!".getBytes(StandardCharsets.UTF_8), first.data());
            Assertions.assertEquals(Optional.empty(), first.id());
            Assertions.assertEquals(Optional.empty(), first.event());

            SseEvent second = receivedEvents.poll(5, TimeUnit.SECONDS);
            Assertions.assertNotNull(second);
            Assertions.assertArrayEquals("a\nb".getBytes(StandardCharsets.UTF_8), second.data());
            Assertions.assertEquals(Optional.of("42"), second.id());
            Assertions.assertEquals(Optional.of("update"), second.event());

            server.close();
            Assertions.assertEquals(200, responseFuture.join().statusCode());
        } finally {
            server.close();
        }
    }
}
