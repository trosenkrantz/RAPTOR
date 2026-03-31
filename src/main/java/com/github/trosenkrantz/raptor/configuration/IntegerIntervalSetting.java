package com.github.trosenkrantz.raptor.configuration;

import com.github.trosenkrantz.raptor.io.ConsoleIo;
import com.github.trosenkrantz.raptor.io.Validator;

import java.util.Optional;
import java.util.logging.Logger;

public class IntegerIntervalSetting extends SettingBase<IntegerInterval> {
    private static final Logger LOGGER = Logger.getLogger(IntegerIntervalSetting.class.getName());
    public static final String MIN_KEY = "min";
    public static final String MAX_KEY = "max";
    private final Validator<Integer> minValidator;

    private IntegerIntervalSetting(Builder builder, Validator<Integer> minValidator) {
        super(builder);
        this.minValidator = minValidator;
    }

    @Override
    public Optional<IntegerInterval> read(Configuration configuration) {
        Optional<Configuration> intervalConfiguration = configuration.getSubConfiguration(getParameterKey());
        if (intervalConfiguration.isEmpty()) return Optional.empty();

        Optional<Integer> minValue = intervalConfiguration.get().getInt(MIN_KEY);
        Optional<Integer> maxValue = intervalConfiguration.get().getInt(MAX_KEY);
        if (minValue.isPresent()) {
            if (maxValue.isPresent()) {
                return Optional.of(new IntegerInterval(minValue.get(), maxValue.get()));
            } else {
                LOGGER.warning(MIN_KEY + " is set but " + MAX_KEY + " is not. Ignoring both.");
                return Optional.empty();
            }
        } else {
            if (maxValue.isPresent()) {
                LOGGER.warning(MAX_KEY + " is set but " + MIN_KEY + " is not. Ignoring both.");
                return Optional.empty();
            } else {
                return Optional.empty();
            }
        }
    }

    @Override
    public String valueToString(IntegerInterval value) {
        return value.min() + " - " + value.max();
    }

    @Override
    public void configure(Configuration configuration) {
        int min, max;
        if (getDefaultValue().isPresent()) {
            min = ConsoleIo.askForInt(getDescription() + ", minimum value", getDefaultValue().get().min(), minValidator);
            max = ConsoleIo.askForInt(getDescription() + ", maximum value", getDefaultValue().get().max(), maxEntered -> getValidator().validate(new IntegerInterval(min, maxEntered)));
        } else {
            min = ConsoleIo.askForInt(getDescription() + ", minimum value", minValidator);
            max = ConsoleIo.askForInt(getDescription() + ", maximum value", maxEntered -> getValidator().validate(new IntegerInterval(min, maxEntered)));
        }

        Configuration intervalConfiguration = Configuration.empty();
        intervalConfiguration.setInt(MIN_KEY, min);
        intervalConfiguration.setInt(MAX_KEY, max);

        configuration.setSubConfiguration(getParameterKey(), intervalConfiguration);
    }

    public static class Builder extends SettingBase.Builder<IntegerInterval, Builder> {
        private Validator<Integer> minValidator;

        public Builder(String promptValue, String parameterKey, String name, String description) {
            super(promptValue, parameterKey, name, description);
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public IntegerIntervalSetting build() {
            return new IntegerIntervalSetting(this, minValidator);
        }

        public Builder minValidator(Validator<Integer> minValidator) {
            this.minValidator = minValidator;
            return this;
        }
    }
}

