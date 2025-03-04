package com.github.trosenkrantz.raptor.serial.port;

import com.github.trosenkrantz.raptor.PromptEnum;

import java.util.EnumMap;
import java.util.Map;

public enum SendStrategy implements PromptEnum {
    NONE("n", "Do [n]ot send"),
    INTERACTIVE("i", "Prompt over console [i]nteractively"),
    AUTO_REPLY("a", "Configure an [a]uto-reply");

    private static final Map<SendStrategy, SerialPortSendStrategy> STRATEGY_MAP = new EnumMap<>(SendStrategy.class);
    static {
        STRATEGY_MAP.put(NONE, new NoneSendStrategy());
        STRATEGY_MAP.put(INTERACTIVE, new InteractiveSendStrategy());
        STRATEGY_MAP.put(AUTO_REPLY, new AutoReplySendStrategy());
    }

    private final String promptValue;
    private final String description;

    SendStrategy(String promptValue, String description) {
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

    public SerialPortSendStrategy getStrategy() {
        return STRATEGY_MAP.get(this);
    }
}
