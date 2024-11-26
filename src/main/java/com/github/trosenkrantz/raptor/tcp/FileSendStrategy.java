package com.github.trosenkrantz.raptor.tcp;

import com.github.trosenkrantz.raptor.Configuration;
import com.github.trosenkrantz.raptor.io.BytesFormatter;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.logging.Logger;

class FileSendStrategy implements TcpSendStrategy {
    private static final Logger LOGGER = Logger.getLogger(FileSendStrategy.class.getName());

    private byte[] fileContentToSend;

    @Override
    public void load(Configuration configuration) throws IOException {
        // Read file immediately to provide early feedback
        fileContentToSend = Files.readAllBytes(Paths.get(configuration.requireString(TcpService.PARAMETER_SEND_FILE)));
    }

    @Override
    public Consumer<byte[]> initialise(Socket socket, Runnable shutDownAction) throws IOException {
        socket.getOutputStream().write(fileContentToSend);
        LOGGER.info("Sent " + BytesFormatter.bytesToFullyEscapedStringWithType(fileContentToSend));
        return input -> { // Nothing to send on inputs
        };
    }
}
