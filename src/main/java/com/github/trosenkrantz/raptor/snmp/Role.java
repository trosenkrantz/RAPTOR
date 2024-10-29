package com.github.trosenkrantz.raptor.snmp;

import com.github.trosenkrantz.raptor.PromptEnum;

public enum Role implements PromptEnum {
    GET_REQUEST("g", "Send [G]ET request, typically as manager to an agent"),
    GET_RESPOND("r", "[R]esond to GET requests, typically as agent back to a manager"),
    TRAP("t", "Send [T]RAP, typically as agent to a manager"),
    LISTEN("l", "[l]isten to PDUs, typically as manager listening to TRAPs from an agent");

    private final String promptValue;
    private final String description;

    Role(String promptValue, String description) {
        this.promptValue = promptValue;
        this.description = description;
    }

    @Override
    public String getPromptValue() {
        return this.promptValue;
    }

    @Override
    public String getDescription() {
        return this.description;
    }
}
