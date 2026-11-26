package com.github.trosenkrantz.raptor.replay;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorIntegrationTest;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.images.builder.Transferable;

import java.io.IOException;
import java.util.List;

public class RecordReplayIntegrationTest extends RaptorIntegrationTest {
    private static final String REPLAY_LOG_PATH = "/app/recording.log";

    @Test
    public void tcp() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor server = new Raptor(network);
             Raptor capture = new Raptor(network);
             Raptor replay = new Raptor(network)) {
            network.startAll();

            server.runConfiguration("""
                    {
                      "service": "tcp",
                      "role": "server",
                      "localPort": 50000,
                      "tlsVersion": "none",
                      "sendStrategy": "none"
                    }
                    """);
            server.expectNumberOfOutputLineContains(1, "Waiting for client to connect");

            capture.runConfiguration(String.format("""
                    {
                      "service": "tcp",
                      "role": "client",
                      "remoteHost": "%s",
                      "remotePort": 50000,
                      "tlsVersion": "none",
                      "sendStrategy": "interactive"
                    }
                    """, server.getRaptorHostname()));
            server.expectNumberOfOutputLineContains(1, "connected", capture.getRaptorIpAddress());

            String message = "Hello, World!";
            capture.writeLineToStdIn(message);
            server.expectNumberOfOutputLineContains(1, "received", message);

            useCapturedLog(capture, "Sent \"" + message + "\"", replay);
            capture.stop(); // TCP servers handle one client at a time
            server.expectNumberOfOutputLineContains(2, "Waiting for client to connect");

            replay.runConfiguration(String.format("""
                    {
                      "service": "tcp",
                      "role": "client",
                      "remoteHost": "%s",
                      "remotePort": 50000,
                      "tlsVersion": "none",
                      "sendStrategy": "replay",
                      "replayLogFile": "recording.log",
                      "replayDirection": "sent"
                    }
                    """, server.getRaptorHostname()));
            replay.expectNumberOfOutputLineContains(1, "Loaded 1 sent replay event from");
            server.expectNumberOfOutputLineContains(2, "received", message);
        }
    }

    @Test
    public void serverSentEvents() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor captureServer = new Raptor(network);
             Raptor captureClient = new Raptor(network);
             Raptor replayServer = new Raptor(network);
             Raptor replayClient = new Raptor(network)) {
            network.startAll();

            captureServer.runConfiguration("""
                    {
                      "service": "sse",
                      "role": "server",
                      "port": 50000,
                      "sendStrategy": "interactive",
                      "tlsVersion": "none"
                    }
                    """);
            captureServer.expectNumberOfOutputLineContains(1, "Waiting for client to connect");
            captureClient.runConfiguration(String.format("""
                    {
                      "service": "sse",
                      "role": "client",
                      "uri": "http://%s:50000/events",
                      "tlsVersion": "none"
                    }
                    """, captureServer.getRaptorHostname()));
            captureServer.expectNumberOfOutputLineContains(1, "connected", captureClient.getRaptorIpAddress());
            captureClient.expectNumberOfOutputLineContains(1, "connected to server at");

            String message = "With metadata";
            sendEvent(captureServer, message, "42", "update");
            captureClient.expectNumberOfOutputLineContains(1, "received", "id=\"42\"", "event=\"update\"", message);

            useCapturedLog(captureServer, "Sent [event=\"update\",id=\"42\"] \"" + message + "\"", replayServer);
            replayServer.runConfiguration("""
                    {
                      "service": "sse",
                      "role": "server",
                      "port": 50000,
                      "sendStrategy": "replay",
                      "replayLogFile": "recording.log",
                      "replayDirection": "sent",
                      "tlsVersion": "none"
                    }
                    """);
            replayServer.expectNumberOfOutputLineContains(1, "Waiting for client to connect");
            replayClient.runConfiguration(String.format("""
                    {
                      "service": "sse",
                      "role": "client",
                      "uri": "http://%s:50000/events",
                      "tlsVersion": "none"
                    }
                    """, replayServer.getRaptorHostname()));
            replayServer.expectNumberOfOutputLineContains(1, "connected", replayClient.getRaptorIpAddress());
            replayClient.expectNumberOfOutputLineContains(1, "received", "id=\"42\"", "event=\"update\"", message);
        }
    }

    @Test
    public void webSocket() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor captureServer = new Raptor(network);
             Raptor captureClient = new Raptor(network);
             Raptor replayServer = new Raptor(network);
             Raptor replayClient = new Raptor(network)) {
            network.startAll();

            captureServer.runConfiguration("""
                    {
                      "service": "webSocket",
                      "role": "server",
                      "port": 50000,
                      "tlsVersion": "none",
                      "sendStrategy": "interactive"
                    }
                    """);
            captureServer.expectNumberOfOutputLineContains(1, "Waiting for client to connect");
            captureClient.runConfiguration(String.format("""
                    {
                      "service": "webSocket",
                      "role": "client",
                      "uri": "ws://%s:50000",
                      "tlsVersion": "none",
                      "sendStrategy": "none"
                    }
                    """, captureServer.getRaptorHostname()));
            captureServer.expectNumberOfOutputLineContains(1, "connected", captureClient.getRaptorIpAddress());

            String textMessage = "Hello, World!";
            captureServer.writeLineToStdIn(textMessage);
            captureClient.expectNumberOfOutputLineContains(1, "received", "frame=\"text\"", textMessage);

            String binaryMessage = "\\\\x00\\\\x01";
            captureServer.writeLineToStdIn(binaryMessage);
            captureClient.expectNumberOfOutputLineContains(1, "received", "frame=\"binary\"", binaryMessage);

            useCapturedLog(captureServer, "Sent [frame=\"binary\"] \"\\\\x00\\\\x01\"", replayServer);
            replayServer.runConfiguration("""
                    {
                      "service": "webSocket",
                      "role": "server",
                      "port": 50000,
                      "tlsVersion": "none",
                      "sendStrategy": "replay",
                      "replayLogFile": "recording.log",
                      "replayDirection": "sent"
                    }
                    """);
            replayServer.expectNumberOfOutputLineContains(1, "Waiting for client to connect");
            replayClient.runConfiguration(String.format("""
                    {
                      "service": "webSocket",
                      "role": "client",
                      "uri": "ws://%s:50000",
                      "tlsVersion": "none",
                      "sendStrategy": "none"
                    }
                    """, replayServer.getRaptorHostname()));
            replayClient.expectNumberOfOutputLineContains(1, "received", "frame=\"text\"", textMessage);
            replayClient.expectNumberOfOutputLineContains(1, "received", "frame=\"binary\"", binaryMessage);
        }
    }

    private static void useCapturedLog(Raptor source, String expectedCaptureLine, Raptor target) {
        long deadline = System.currentTimeMillis() + Raptor.TIMEOUT_MS;
        String log = null;
        while (log == null && System.currentTimeMillis() < deadline) {
            for (String content : source.readLogFiles()) {
                if (content.contains(expectedCaptureLine)) {
                    log = content;
                    break;
                }
            }
            if (log == null) {
                try {
                    Thread.sleep(Raptor.EXPECT_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        Assertions.assertNotNull(log, "No captured log line found: " + expectedCaptureLine);
        target.copyFileToContainer(Transferable.of(log), REPLAY_LOG_PATH);
    }

    private static void sendEvent(Raptor server, String data, String id, String event) {
        server.writeLineToStdIn(data);
        server.writeLineToStdIn(id);
        server.writeLineToStdIn(event);
    }
}
