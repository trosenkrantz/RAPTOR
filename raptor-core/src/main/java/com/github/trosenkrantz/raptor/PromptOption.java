package com.github.trosenkrantz.raptor;

public record PromptOption<T>(String promptValue, String description, T value) {
}
