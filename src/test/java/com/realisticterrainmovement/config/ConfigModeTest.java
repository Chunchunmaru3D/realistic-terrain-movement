package com.realisticterrainmovement.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigModeTest {

    @Test
    void terrainDifficultyParsesKnownValuesAndFallsBackToNormal() {
        assertEquals(TerrainDifficulty.EASY, TerrainDifficulty.fromConfig("easy"));
        assertEquals(TerrainDifficulty.HARDCORE, TerrainDifficulty.fromConfig("HARDCORE"));
        assertEquals(TerrainDifficulty.NORMAL, TerrainDifficulty.fromConfig("unknown"));
    }

}
