package com.github.trosenkrantz.raptor.gateway;

import com.github.trosenkrantz.raptor.configuration.DoubleSetting;
import com.github.trosenkrantz.raptor.configuration.IntegerSetting;
import com.github.trosenkrantz.raptor.configuration.Setting;

import java.util.Optional;

public class Settings {
    private Settings() {
    }

    public static Setting<Integer> LATENCY = new IntegerSetting.Builder("l", "latency", "Latency [ms]", "Latency [ms]")
            .validator(value -> {
                if (value < 0) {
                    return Optional.of("Latency must be a non-negative integer.");
                }
                return Optional.empty();
            })
            .build();

    public static Setting<Double> CORRUPTION = new DoubleSetting.Builder("c", "corruption", "Corruption chance", "Corruption chance between 0 and 1")
            .defaultValue(0.0)
            .validator(value -> {
                if (value < 0 || value > 1) {
                    return Optional.of("Corruption chance must be between 0 and 1.");
                }
                return Optional.empty();
            })
            .build();
}
