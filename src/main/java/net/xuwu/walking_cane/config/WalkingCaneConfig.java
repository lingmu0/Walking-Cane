package net.xuwu.walking_cane.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class WalkingCaneConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "walking_cane.toml";

    private static final HandMode DEFAULT_HAND_MODE = HandMode.MAIN_HAND;
    private static final double DEFAULT_DASH_STRENGTH = 2.5;
    private static final double DEFAULT_TELEPORT_BASE_DISTANCE = 50.0;
    private static final double DEFAULT_TELEPORT_DISTANCE_PER_PEARL = 100.0;

    private static final double MAX_DASH_STRENGTH = 20.0;
    private static final double MAX_TELEPORT_DISTANCE = 1_000_000.0;

    public static final HandMode HAND_MODE;
    public static final double DASH_STRENGTH;
    public static final double TELEPORT_BASE_DISTANCE;
    public static final double TELEPORT_DISTANCE_PER_PEARL;

    static {
        LoadedValues values = load();
        HAND_MODE = values.handMode();
        DASH_STRENGTH = values.dashStrength();
        TELEPORT_BASE_DISTANCE = values.teleportBaseDistance();
        TELEPORT_DISTANCE_PER_PEARL = values.teleportDistancePerPearl();
        LOGGER.info(
                "Loaded Walking Cane config: hand_mode={}, dash_strength={}, teleport_base_distance={}, teleport_distance_per_pearl={}",
                HAND_MODE,
                DASH_STRENGTH,
                TELEPORT_BASE_DISTANCE,
                TELEPORT_DISTANCE_PER_PEARL
        );
    }

    private WalkingCaneConfig() {
    }

    public static boolean isHandEnabled(InteractionHand hand) {
        return HAND_MODE.allows(hand);
    }

    private static LoadedValues load() {
        HandMode handMode = DEFAULT_HAND_MODE;
        double dashStrength = DEFAULT_DASH_STRENGTH;
        double teleportBaseDistance = DEFAULT_TELEPORT_BASE_DISTANCE;
        double teleportDistancePerPearl = DEFAULT_TELEPORT_DISTANCE_PER_PEARL;
        Path path = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);

        try {
            Files.createDirectories(path.getParent());
            try (CommentedFileConfig config = CommentedFileConfig.builder(path).sync().build()) {
                config.load();

                handMode = readHandMode(config.get("hand_mode"));
                dashStrength = readDouble(
                        config.get("dash_strength"),
                        DEFAULT_DASH_STRENGTH,
                        0.0,
                        MAX_DASH_STRENGTH,
                        "dash_strength"
                );
                teleportBaseDistance = readDouble(
                        config.get("teleport_base_distance"),
                        DEFAULT_TELEPORT_BASE_DISTANCE,
                        0.0,
                        MAX_TELEPORT_DISTANCE,
                        "teleport_base_distance"
                );
                teleportDistancePerPearl = readDouble(
                        config.get("teleport_distance_per_pearl"),
                        DEFAULT_TELEPORT_DISTANCE_PER_PEARL,
                        0.0,
                        MAX_TELEPORT_DISTANCE,
                        "teleport_distance_per_pearl"
                );

                config.set("hand_mode", handMode.name());
                config.setComment(
                        "hand_mode",
                        " Hand slots in which walking canes are active: MAIN_HAND, OFF_HAND, or BOTH. Requires a restart."
                );
                config.set("dash_strength", dashStrength);
                config.setComment(
                        "dash_strength",
                        " Dash velocity multiplier. Range: 0.0 to 20.0. Requires a restart."
                );
                config.set("teleport_base_distance", teleportBaseDistance);
                config.setComment(
                        "teleport_base_distance",
                        " Ender cane teleport distance without ender pearls. Range: 0.0 to 1000000.0. Requires a restart."
                );
                config.set("teleport_distance_per_pearl", teleportDistancePerPearl);
                config.setComment(
                        "teleport_distance_per_pearl",
                        " Extra teleport distance for each consumed ender pearl. Range: 0.0 to 1000000.0. Requires a restart."
                );
                config.save();
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load {}, using default walking cane settings", path, exception);
            handMode = DEFAULT_HAND_MODE;
            dashStrength = DEFAULT_DASH_STRENGTH;
            teleportBaseDistance = DEFAULT_TELEPORT_BASE_DISTANCE;
            teleportDistancePerPearl = DEFAULT_TELEPORT_DISTANCE_PER_PEARL;
        }

        return new LoadedValues(
                handMode,
                dashStrength,
                teleportBaseDistance,
                teleportDistancePerPearl
        );
    }

    private static HandMode readHandMode(Object rawValue) {
        if (rawValue == null) {
            return DEFAULT_HAND_MODE;
        }

        try {
            return HandMode.valueOf(rawValue.toString().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            LOGGER.warn(
                    "Invalid hand_mode value '{}'; expected MAIN_HAND, OFF_HAND, or BOTH. Using {}",
                    rawValue,
                    DEFAULT_HAND_MODE
            );
            return DEFAULT_HAND_MODE;
        }
    }

    private static double readDouble(
            Object rawValue,
            double defaultValue,
            double minimum,
            double maximum,
            String key
    ) {
        if (rawValue == null) {
            return defaultValue;
        }

        double value;
        if (rawValue instanceof Number number) {
            value = number.doubleValue();
        } else {
            try {
                value = Double.parseDouble(rawValue.toString());
            } catch (NumberFormatException exception) {
                LOGGER.warn("Invalid {} value '{}'; using {}", key, rawValue, defaultValue);
                return defaultValue;
            }
        }

        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            LOGGER.warn(
                    "{} value {} is outside the allowed range {} to {}; using {}",
                    key,
                    value,
                    minimum,
                    maximum,
                    defaultValue
            );
            return defaultValue;
        }
        return value;
    }

    public enum HandMode {
        MAIN_HAND,
        OFF_HAND,
        BOTH;

        public boolean allows(InteractionHand hand) {
            return this == BOTH
                    || this == MAIN_HAND && hand == InteractionHand.MAIN_HAND
                    || this == OFF_HAND && hand == InteractionHand.OFF_HAND;
        }
    }

    private record LoadedValues(
            HandMode handMode,
            double dashStrength,
            double teleportBaseDistance,
            double teleportDistancePerPearl
    ) {
    }
}
