package com.github.trosenkrantz.raptor.configuration;

public interface SettingGroup<T> {
    Configuration configure();

    String configurationToString(Configuration configuration);

    T readAndRequire(Configuration configuration);
}