package com.github.trosenkrantz.raptor.gateway.network.impairment.bandwidth;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.configuration.IntegerSetting;
import com.github.trosenkrantz.raptor.configuration.SettingBase;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.util.Optional;

public class BandwidthSetting extends SettingBase<Bandwidth> {
    private static final IntegerSetting RATE_SETTING = new IntegerSetting.Builder("r", "rate", "Rate", "The sustained speed limit of the connection [b/s]")
            .validator(value -> {
                if (value <= 0) return Optional.of("Must be positive.");
                else return Optional.empty();
            })
            .build();
    private static final IntegerSetting BURST_LIMIT_SETTING = new IntegerSetting.Builder("b", "burst", "Burst limit", "Maximum idle time to be banked for immediate bursting [ms]")
            .defaultValue(100)
            .build();
    private static final IntegerSetting QUEUE_DURATION_SETTING = new IntegerSetting.Builder("q", "queue", "Queue duration", "Maximum queuing delay before packets are dropped (tail drop) [ms]")
            .defaultValue(1000)
            .build();

    private BandwidthSetting(Builder builder) {
        super(builder);
    }

    @Override
    public Optional<Bandwidth> read(Configuration configuration) {
        Optional<Configuration> bandwidthConfiguration = configuration.getSubConfiguration(getParameterKey());
        if (bandwidthConfiguration.isEmpty()) return Optional.empty();

        Optional<Integer> bitsPerSecond = RATE_SETTING.read(bandwidthConfiguration.get());
        Optional<Integer> burstLimitMillis = BURST_LIMIT_SETTING.read(bandwidthConfiguration.get());
        Optional<Integer> queueDurationMillis = QUEUE_DURATION_SETTING.read(bandwidthConfiguration.get());

        if (bitsPerSecond.isPresent() && burstLimitMillis.isPresent() && queueDurationMillis.isPresent()) return Optional.of(new Bandwidth(bitsPerSecond.get(), burstLimitMillis.get(), queueDurationMillis.get()));
        else return Optional.empty();
    }

    @Override
    public void configure(Configuration configuration) {
        ConsoleIo.writeLine(this.getDescription());

        Configuration bandwidthConfiguration = Configuration.empty();

        RATE_SETTING.configure(bandwidthConfiguration);
        BURST_LIMIT_SETTING.configure(bandwidthConfiguration);
        QUEUE_DURATION_SETTING.configure(bandwidthConfiguration);

        configuration.setSubConfiguration(getParameterKey(), bandwidthConfiguration);
    }

    @Override
    public String valueToString(Bandwidth value) {
        return value.bitsPerSecond() + " b/s (" + value.maxBurstDurationMillis() + " + " + value.queueDurationMillis() + " ms buffers)";
    }

    public static class Builder extends SettingBase.Builder<Bandwidth, BandwidthSetting.Builder> {
        public Builder(String promptValue, String parameterKey, String name, String description) {
            super(promptValue, parameterKey, name, description);
        }

        @Override
        public BandwidthSetting.Builder self() {
            return this;
        }

        @Override
        public BandwidthSetting build() {
            return new BandwidthSetting(this);
        }
    }
}
