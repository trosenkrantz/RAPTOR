package com.github.trosenkrantz.raptor.tcp;

import com.github.trosenkrantz.raptor.PromptEnum;
import com.github.trosenkrantz.raptor.configuration.ConfigurableEnum;

import java.util.EnumMap;
import java.util.Map;

public enum SendStrategy implements PromptEnum, ConfigurableEnum {
    NONE("n", "Do [n]ot send", "none"),
    INTERACTIVE("i", "Prompt over console [i]nteractively", "interactive"),
    AUTO_REPLY("a", "Configure an [a]uto-reply", "autoReply");

    private static final Map<SendStrategy, TcpSendStrategy> STRATEGY_MAP = new EnumMap<>(SendStrategy.class);
    static {
        STRATEGY_MAP.put(NONE, new NoneSendStrategy());
        STRATEGY_MAP.put(INTERACTIVE, new InteractiveSendStrategy());
        STRATEGY_MAP.put(AUTO_REPLY, new AutoReplySendStrategy());
    }

    private final String promptValue;
    private final String description;
    private final String configurationId;

    SendStrategy(String promptValue, String description, String configurationId) {
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

    public TcpSendStrategy getStrategy() {
        return STRATEGY_MAP.get(this);
    }

    @Override
    public String getConfigurationId() {
        return this.configurationId;
    }
}
