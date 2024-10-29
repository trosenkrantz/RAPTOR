package com.github.trosenkrantz.raptor;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface PromptEnum {
    String getPromptValue();

    String getDescription();

    static <T extends Enum<T> & PromptEnum> List<PromptOption<T>> getPromptOptions(Class<T> enumClass) {
        return Stream.of(enumClass.getEnumConstants())
                .map(value -> new PromptOption<>(value.getPromptValue(), value.getDescription(), value))
                .collect(Collectors.toList());
    }
}
