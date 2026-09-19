package com.realisticterrainmovement.config;

import java.util.Locale;

public enum TerrainDifficulty {
    EASY,
    NORMAL,
    HARDCORE;

    public static TerrainDifficulty fromConfig(String value) {
        try {
            return TerrainDifficulty.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return NORMAL;
        }
    }

    public static boolean isValid(String value) {
        return fromConfig(value).name().equalsIgnoreCase(value == null ? "" : value.trim());
    }
}
