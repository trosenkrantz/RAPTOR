package com.github.trosenkrantz.raptor.sse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

final class SseSubscriber implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(SseSubscriber.class.getName());
    private static final String OK_HEADERS =
            "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: text/event-stream; charset=utf-8\r\n"
                    + "Cache-Control: no-cache\r\n"
                    + "Connection: keep-alive\r\n"
                    + "\r\n";

    private final SseServer server;
    private final Socket socket;
    private final OutputStream out;
    private final String remoteAddress;
    private final boolean accepted;
    private final CountDownLatch ready = new CountDownLatch(1);

    SseSubscriber(SseServer server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.out = socket.getOutputStream();
        this.remoteAddress = String.valueOf(socket.getRemoteSocketAddress());

        accepted = acceptRequest();
        if (!accepted) close();
    }

    boolean isAccepted() {
        return accepted;
    }

    void start() {
        if (!accepted) return;
        try {
            synchronized (this) {
                out.write(OK_HEADERS.getBytes(StandardCharsets.US_ASCII));
                out.flush();
                LOGGER.info("Local socket at " + socket.getLocalSocketAddress() + " connected to remote socket at " + remoteAddress + ".");
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed sending headers to " + remoteAddress + ".", e);
            server.removeSubscriber(this);
            close();
        } finally {
            ready.countDown();
        }
    }

    @Override
    public void run() {
        try {
            while (socket.getInputStream().read() != -1) { // Clients do not send data after the request
            }
            LOGGER.info("Remote socket " + remoteAddress + " closed the connection.");
        } catch (IOException e) {
            // SSE clients can disconnect abruptly; details are only useful while debugging.
            LOGGER.log(Level.FINE, "Error handling subscriber at " + remoteAddress + ".", e);
        } finally {
            server.removeSubscriber(this);
            close();
        }
    }

    private boolean acceptRequest() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));

        String requestLine = reader.readLine();
        if (requestLine == null) {
            sendError("400 Bad Request");
            return false;
        }

        int methodEnd = requestLine.indexOf(' ');
        if (methodEnd < 0 || !"GET".equalsIgnoreCase(requestLine.substring(0, methodEnd))) {
            sendError("405 Method Not Allowed");
            return false;
        }

        String accept = null;
        String header;
        while ((header = reader.readLine()) != null && !header.isEmpty()) {
            if (header.toLowerCase(Locale.ROOT).startsWith("accept:")) {
                accept = header.substring("accept:".length()).trim();
            }
        }
        if (accept != null && !accept.contains("text/event-stream") && !accept.contains("*/*")) {
            sendError("406 Not Acceptable");
            return false;
        }
        return true;
    }

    private void sendError(String status) throws IOException {
        byte[] response = ("HTTP/1.1 " + status + "\r\nContent-Length: 0\r\nConnection: close\r\n\r\n")
                .getBytes(StandardCharsets.US_ASCII);
        out.write(response);
        out.flush();
    }

    void send(byte[] event) {
        try {
            ready.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        synchronized (this) {
            try {
                out.write(event);
                out.flush();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed sending event to " + remoteAddress + ", closing the connection.", e);
                server.removeSubscriber(this);
                close();
            }
        }
    }

    synchronized void close() {
        try {
            socket.close();
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Failed closing socket at " + remoteAddress + ".", e);
        }
    }
}
