package com.github.trosenkrantz.raptor.web.sse;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Minimal HTTP/1.1 server that keeps GET requests open and streams Server-Sent Events to them.
 */
class SseServer {
    private static final Logger LOGGER = Logger.getLogger(SseServer.class.getName());

    private static final String OK_HEADERS =
            "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: text/event-stream; charset=utf-8\r\n"
                    + "Cache-Control: no-cache\r\n"
                    + "Connection: keep-alive\r\n"
                    + "\r\n";

    private final ServerSocket serverSocket;
    private final Set<Subscriber> subscribers = ConcurrentHashMap.newKeySet();
    private volatile boolean running = true;

    SseServer(int port, SSLContext sslContext) throws IOException {
        if (sslContext == null) {
            serverSocket = new ServerSocket(port);
        } else {
            SSLServerSocket sslServerSocket =
                    (SSLServerSocket) sslContext.getServerSocketFactory().createServerSocket(port);
            sslServerSocket.setWantClientAuth(false);
            serverSocket = sslServerSocket;
        }
    }

    /** Accepts subscribers until the server is closed. */
    void run() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                Subscriber subscriber = new Subscriber(socket);
                if (!subscriber.isAccepted()) continue; // Invalid request, response already sent
                subscribers.add(subscriber); // Only add after headers are sent, so events cannot overtake them
                Thread.ofVirtual().start(subscriber);
            } catch (SocketException e) {
                if (running) {
                    LOGGER.log(Level.SEVERE, "Error accepting connection.", e);
                    return;
                }
                LOGGER.info("Server closed normally.");
                return;
            } catch (IOException e) {
                if (running) LOGGER.log(Level.FINE, "Error accepting connection.", e);
                return;
            }
        }
    }

    int getPort() {
        return serverSocket.getLocalPort();
    }

    int getSubscriberCount() {
        return subscribers.size();
    }

    /** Sends one event to all current subscribers. */
    void send(String event) {
        byte[] bytes = event.getBytes(StandardCharsets.UTF_8);
        for (Subscriber subscriber : subscribers) {
            subscriber.send(bytes);
        }
    }

    void close() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed closing server socket.", e);
        }
        subscribers.forEach(Subscriber::close);
        subscribers.clear();
    }

    private class Subscriber implements Runnable {
        private final Socket socket;
        private final OutputStream out;
        private final boolean accepted;

        Subscriber(Socket socket) throws IOException {
            this.socket = socket;
            this.out = socket.getOutputStream();

            accepted = acceptRequest();
            if (accepted) {
                out.write(OK_HEADERS.getBytes(StandardCharsets.US_ASCII));
                out.flush();
                // Log only once the subscriber is ready to receive events
                LOGGER.info("Local socket at " + socket.getLocalSocketAddress()
                        + " connected to remote socket at " + socket.getRemoteSocketAddress() + ".");
            } else {
                close();
            }
        }

        boolean isAccepted() {
            return accepted;
        }

        @Override
        public void run() {
            try {
                // Clients do not send data after the request; reading detects disconnects
                while (socket.getInputStream().read() != -1) {
                    // Ignore unexpected data from the client
                }
                LOGGER.info("Remote socket closed the connection.");
            } catch (IOException e) {
                if (running) LOGGER.log(Level.FINE, "Error handling subscriber.", e);
            } finally {
                subscribers.remove(this);
                close();
            }
        }

        /** @return true if the request is a GET that accepts text/event-stream */
        private boolean acceptRequest() throws IOException {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));

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

        synchronized void send(byte[] event) {
            try {
                out.write(event);
                out.flush();
            } catch (IOException e) {
                LOGGER.info("Failed sending event to " + socket.getRemoteSocketAddress()
                        + ", closing the connection.");
                subscribers.remove(this);
                close();
            }
        }

        synchronized void close() {
            try {
                socket.close();
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Failed closing socket.", e);
            }
        }
    }
}
