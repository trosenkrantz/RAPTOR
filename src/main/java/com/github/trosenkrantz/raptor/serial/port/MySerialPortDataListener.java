package com.github.trosenkrantz.raptor.serial.port;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.github.trosenkrantz.raptor.io.BytesFormatter;

import java.util.function.Consumer;
import java.util.logging.Logger;

class MySerialPortDataListener implements SerialPortDataListener {
    private static final Logger LOGGER = Logger.getLogger(MySerialPortDataListener.class.getName());

    private final Consumer<byte[]> onReceivedData;

    public MySerialPortDataListener(Consumer<byte[]> onReceivedData) {
        this.onReceivedData = onReceivedData;
    }

    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_RECEIVED | SerialPort.LISTENING_EVENT_PORT_DISCONNECTED | SerialPort.LISTENING_EVENT_BREAK_INTERRUPT | SerialPort.LISTENING_EVENT_CARRIER_DETECT | SerialPort.LISTENING_EVENT_CTS | SerialPort.LISTENING_EVENT_DSR | SerialPort.LISTENING_EVENT_RING_INDICATOR | SerialPort.LISTENING_EVENT_FRAMING_ERROR | SerialPort.LISTENING_EVENT_FIRMWARE_OVERRUN_ERROR | SerialPort.LISTENING_EVENT_SOFTWARE_OVERRUN_ERROR | SerialPort.LISTENING_EVENT_PARITY_ERROR;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        switch (event.getEventType()) {
            case SerialPort.LISTENING_EVENT_DATA_RECEIVED:
                byte[] data = event.getReceivedData();
                LOGGER.info("Received " + BytesFormatter.bytesToFullyEscapedStringWithType(data));
                onReceivedData.accept(data);
                break;
            case SerialPort.LISTENING_EVENT_PORT_DISCONNECTED:
                LOGGER.info("Port disconnected");
                break;
            case SerialPort.LISTENING_EVENT_BREAK_INTERRUPT:
                LOGGER.info("Break signal received");
                break;
            case SerialPort.LISTENING_EVENT_CARRIER_DETECT:
                LOGGER.info("New carrier detect value: " + event.getSerialPort().getDCD());
                break;
            case SerialPort.LISTENING_EVENT_CTS:
                LOGGER.info("New clear to send value: " + event.getSerialPort().getCTS());
                break;
            case SerialPort.LISTENING_EVENT_DSR:
                LOGGER.info("New data set ready value: " + event.getSerialPort().getDSR());
                break;
            case SerialPort.LISTENING_EVENT_RING_INDICATOR:
                LOGGER.info("New ring indicator value: " + event.getSerialPort().getRI());
                break;
            case SerialPort.LISTENING_EVENT_FIRMWARE_OVERRUN_ERROR:
                LOGGER.info("Device driver buffer overrun detected");
                break;
            case SerialPort.LISTENING_EVENT_SOFTWARE_OVERRUN_ERROR:
                LOGGER.info("Application buffer overrun detected");
                break;
            case SerialPort.LISTENING_EVENT_PARITY_ERROR:
                LOGGER.info("Parity error detected");
                break;
        }
    }
}
