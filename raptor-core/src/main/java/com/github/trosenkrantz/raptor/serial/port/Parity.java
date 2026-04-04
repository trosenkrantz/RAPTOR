package com.github.trosenkrantz.raptor.serial.port;

import com.fazecast.jSerialComm.SerialPort;
import com.github.trosenkrantz.raptor.PromptEnum;
import com.github.trosenkrantz.raptor.configuration.ConfigurableEnum;

public enum Parity implements PromptEnum, ConfigurableEnum {
    NO("n", "[N]o parity", SerialPort.NO_PARITY, "no"),
    EVEN("e", "[E]ven parity", SerialPort.EVEN_PARITY, "even"),
    ODD("o", "[O]dd parity", SerialPort.ODD_PARITY, "odd"),
    MARK("m", "[M]ark parity", SerialPort.MARK_PARITY, "mark"),
    SPACE("s", "[S]pace parity", SerialPort.SPACE_PARITY, "space");

    private final String promptValue;
    private final String description;
    private final int value;
    private final String configurationId;

    Parity(String promptValue, String description, int value, String configurationId) {
        this.promptValue = promptValue;
        this.description = description;
        this.value = value;
        this.configurationId = configurationId;
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

    @Override
    public String getConfigurationId() {
        return this.configurationId;
    }
}
