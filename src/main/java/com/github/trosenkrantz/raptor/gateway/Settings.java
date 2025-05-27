package com.github.trosenkrantz.raptor.gateway;

import com.github.trosenkrantz.raptor.configuration.IntegerSetting;
import com.github.trosenkrantz.raptor.configuration.Setting;

public class Settings {
    private Settings() {
    }

    public static Setting<Integer> LATENCY = new IntegerSetting("l", "latency", "Latency [ms]");
}
