package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.PromptEnum;

import java.util.EnumMap;
import java.util.Map;

public enum EndpointMode implements PromptEnum {
    MULTICAST("m", "[M]ulticast, bidirectional"),
    BROADCAST("b", "[B]roadcast, bidirectional");

    private static final Map<EndpointMode, Mode> MAP = new EnumMap<>(EndpointMode.class);
    static {
        MAP.put(MULTICAST, Mode.MULTICAST);
        MAP.put(BROADCAST, Mode.BROADCAST);
    }

    private final String promptValue;
    private final String description;

    EndpointMode(String promptValue, String description) {
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
