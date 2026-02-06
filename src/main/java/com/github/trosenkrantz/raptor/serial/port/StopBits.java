package com.github.trosenkrantz.raptor.serial.port;

import com.fazecast.jSerialComm.SerialPort;
import com.github.trosenkrantz.raptor.PromptEnum;
import com.github.trosenkrantz.raptor.configuration.ConfigurableEnum;

public enum StopBits implements PromptEnum, ConfigurableEnum {
    ONE("1", "1 stop bit", SerialPort.ONE_STOP_BIT, "1"),
    ONE_POINT_FIVE("1.5", "1.5 stop bits", SerialPort.ONE_POINT_FIVE_STOP_BITS, "1.5"),
    TWO("2", "2 stop bits", SerialPort.TWO_STOP_BITS, "2");

    private final String promptValue;
    private final String description;
    private final int value;
    private final String configurationId;

    StopBits(String promptValue, String description, int value, String configurationId) {
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
