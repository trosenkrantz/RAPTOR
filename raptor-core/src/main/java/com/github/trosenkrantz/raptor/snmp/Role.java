package com.github.trosenkrantz.raptor.snmp;

import com.github.trosenkrantz.raptor.PromptEnum;
import com.github.trosenkrantz.raptor.configuration.ConfigurableEnum;

public enum Role implements PromptEnum, ConfigurableEnum {
    GET_REQUEST("g", "Send [G]ET request, typically as manager to agent", "getRequest"),
    SET_REQUEST("S", "Send [S]ET request, typically as manager to agent", "setRequest"),
    RESPOND("r", "[R]esond to requests, typically as agent back to manager", "respond"),
    TRAP("t", "Send [T]RAP, typically as agent to manager", "trap"),
    LISTEN("l", "[L]isten to PDUs, typically as manager listening to TRAPs from an agent", "listen");

    private final String promptValue;
    private final String description;
    private final String configurationId;

    Role(String promptValue, String description, String configurationId) {
        this.promptValue = promptValue;
        this.description = description;
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

    @Override
    public String getConfigurationId() {
        return this.configurationId;
    }
}
