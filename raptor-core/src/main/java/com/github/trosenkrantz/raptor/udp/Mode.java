package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.PromptEnum;
import com.github.trosenkrantz.raptor.configuration.ConfigurableEnum;

public enum Mode implements PromptEnum, ConfigurableEnum {
    UNICAST("u", "Unicast", "unicast"),
    MULTICAST("m", "Multicast", "multicast"),
    BROADCAST("b", "Broadcast", "broadcast");

    private final String promptValue;
    private final String description;
    private final String configurationId;

    Mode(String promptValue, String description, String configurationId) {
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
