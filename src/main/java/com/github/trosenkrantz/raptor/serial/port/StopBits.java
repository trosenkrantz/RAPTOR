package com.github.trosenkrantz.raptor.serial.port;

import com.fazecast.jSerialComm.SerialPort;
import com.github.trosenkrantz.raptor.PromptEnum;

public enum StopBits implements PromptEnum {
    ONE("1", "1 stop bit", SerialPort.ONE_STOP_BIT),
    ONE_POINT_FIVE("1.5", "1.5 stop bits", SerialPort.ONE_POINT_FIVE_STOP_BITS),
    TWO("2", "2 stop bits", SerialPort.TWO_STOP_BITS);

    private final String promptValue;
    private final String description;
    private final int value;

    StopBits(String promptValue, String description, int value) {
        this.promptValue = promptValue;
        this.description = description;
        this.value = value;
    }

    @Override
    public String getPromptValue() {
        return this.promptValue;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    public int getValue() {
        return value;
    }
}
