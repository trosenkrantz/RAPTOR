package com.github.trosenkrantz.raptor.sse;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Minimal HTTP/1.1 server that keeps GET requests open and streams Server-Sent Events to them. */
class SseServer {
    private static final Logger LOGGER = Logger.getLogger(SseServer.class.getName());

    private final ServerSocket serverSocket;
    private final Set<SseSubscriber> subscribers = ConcurrentHashMap.newKeySet();
    private volatile boolean running = true;

    SseServer(int port, SSLContext sslContext) throws IOException {
        if (sslContext == null) {
            serverSocket = new ServerSocket(port);
        } else {
            SSLServerSocket sslServerSocket = (SSLServerSocket) sslContext.getServerSocketFactory().createServerSocket(port);
            sslServerSocket.setWantClientAuth(false);
            serverSocket = sslServerSocket;
        }
    }

    /** Accepts subscribers until the server is closed. */
    void run() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                SseSubscriber subscriber = new SseSubscriber(this, socket);
                if (subscriber.isAccepted()) {
                    subscribers.add(subscriber);
                    subscriber.start(); // Send headers only after registration, so events cannot overtake them
                    Thread.ofVirtual().start(subscriber);
                }
            } catch (SocketException e) {
                if (running) {
                    LOGGER.log(Level.SEVERE, "Error accepting connection.", e);
                    return;
                }
                LOGGER.info("Server closed normally.");
                return;
            } catch (IOException e) {
                if (running) LOGGER.log(Level.SEVERE, "Error accepting connection.", e);
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

    void removeSubscriber(SseSubscriber subscriber) {
        subscribers.remove(subscriber);
    }

    /** Sends one event to all current subscribers. */
    void send(String event) {
        byte[] bytes = event.getBytes(StandardCharsets.UTF_8);
        for (SseSubscriber subscriber : subscribers) {
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
        subscribers.forEach(SseSubscriber::close);
        subscribers.clear();
    }
}
