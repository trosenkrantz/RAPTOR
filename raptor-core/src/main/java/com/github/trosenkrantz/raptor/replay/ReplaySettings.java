package com.github.trosenkrantz.raptor.replay;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.configuration.DoubleSetting;
import com.github.trosenkrantz.raptor.configuration.EnumSetting;
import com.github.trosenkrantz.raptor.configuration.StringSetting;
import com.github.trosenkrantz.raptor.io.RaptorPaths;
import com.github.trosenkrantz.raptor.logging.CaptureDirection;

import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

/** Shared configuration for replay send strategies. */
public final class ReplaySettings {
    private static final Logger LOGGER = Logger.getLogger(ReplaySettings.class.getName());

    public static final StringSetting LOG_FILE_SETTING = new StringSetting.Builder(
            "l", "replayLogFile", "Replay log file", "Path of the log file to replay"
    ).build();

    public static final EnumSetting<CaptureDirection> DIRECTION_SETTING = new EnumSetting.Builder<>(
            "d", "replayDirection", "Replay direction", "Which captured direction to replay", CaptureDirection.class
    ).defaultValue(CaptureDirection.RECEIVED).build();

    public static final DoubleSetting SPEED_SETTING = new DoubleSetting.Builder(
            "s", "replaySpeed", "Replay speed", "Replay speed factor"
    ).defaultValue(1.0).validator(value -> value > 0 ? Optional.empty() : Optional.of("Replay speed must be greater than zero.")).build();

    public static final EnumSetting<ReplayMode> MODE_SETTING = new EnumSetting.Builder<>(
            "m", "replayMode", "Replay mode", "How many times to replay the capture", ReplayMode.class
    ).defaultValue(ReplayMode.ONCE).build();

    private ReplaySettings() {
    }

    public record Loaded(ReplayTimeline timeline, ReplayMode mode) {
    }

    public static void configure(Configuration configuration) {
        LOG_FILE_SETTING.configure(configuration);
        DIRECTION_SETTING.configure(configuration);
        SPEED_SETTING.configure(configuration);
        MODE_SETTING.configure(configuration);
    }

    public static Loaded load(Configuration configuration) {
        String pathValue = LOG_FILE_SETTING.readAndRequireOrDefault(configuration);
        Path path = RaptorPaths.resolve(pathValue);
        CaptureDirection direction = DIRECTION_SETTING.readAndRequireOrDefault(configuration);
        double speed = SPEED_SETTING.readAndRequireOrDefault(configuration);
        ReplayMode mode = MODE_SETTING.readAndRequireOrDefault(configuration);

        ReplayTimeline timeline = ReplayTimeline.fromLogFile(path, direction, speed);
        LOGGER.info("Loaded " + timeline.size() + " " + direction.getConfigurationId() + " replay events from " + path + ".");
        return new Loaded(timeline, mode);
    }
}
