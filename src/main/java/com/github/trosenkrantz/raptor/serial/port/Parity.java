package com.github.trosenkrantz.raptor.serial.port;

import com.fazecast.jSerialComm.SerialPort;
import com.github.trosenkrantz.raptor.PromptEnum;

public enum Parity implements PromptEnum {
    NO("n", "[N]o parity", SerialPort.NO_PARITY),
    EVEN("e", "[E]ven parity", SerialPort.EVEN_PARITY),
    ODD("o", "[O]dd parity", SerialPort.ODD_PARITY),
    MARK("m", "[M]ark parity", SerialPort.MARK_PARITY),
    SPACE("s", "[S]pace parity", SerialPort.SPACE_PARITY);

    private final String promptValue;
    private final String description;
    private final int value;

    Parity(String promptValue, String description, int value) {
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
