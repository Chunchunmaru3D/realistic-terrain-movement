package com.realisticterrainmovement.events;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoatBreakRulesTest {

    private static final float VANILLA_WIDTH = 1.375F;
    private static final float VANILLA_HEIGHT = 0.5625F;

    @Test
    void standardModdedBoatBreaksAutomatically() {
        BoatBreakRules rules = new BoatBreakRules(VANILLA_WIDTH, VANILLA_HEIGHT);

        assertTrue(rules.shouldBreak(boat("quark:azalea_boat", Set.of(), VANILLA_WIDTH, VANILLA_HEIGHT, true)));
    }

    @Test
    void largeShipIsSkippedByAutomaticDetection() {
        BoatBreakRules rules = new BoatBreakRules(VANILLA_WIDTH, VANILLA_HEIGHT);

        assertFalse(rules.shouldBreak(boat("smallships:cog", Set.of(), 3.0F, 1.5F, false)));
    }

    @Test
    void whitelistCanIncludeAnOtherwiseSkippedBoatOrTag() {
        BoatBreakRules rules = new BoatBreakRules(VANILLA_WIDTH, VANILLA_HEIGHT);
        rules.reload(Set.of("smallships:rowboat"), Set.of("test:fragile_boats"), Set.of(), Set.of());

        assertTrue(rules.shouldBreak(boat("smallships:rowboat", Set.of(), 3.0F, 1.5F, false)));
        assertTrue(rules.shouldBreak(boat("test:custom_boat", Set.of("test:fragile_boats"), 3.0F, 1.5F, false)));
    }

    @Test
    void blacklistOverridesWhitelistAndAutomaticDetection() {
        BoatBreakRules rules = new BoatBreakRules(VANILLA_WIDTH, VANILLA_HEIGHT);
        rules.reload(Set.of("test:boat"), Set.of(), Set.of("test:boat"), Set.of());

        assertFalse(rules.shouldBreak(boat("test:boat", Set.of(), VANILLA_WIDTH, VANILLA_HEIGHT, true)));
    }

    private static BoatBreakRules.BoatInfo boat(String typeId, Set<String> tags, float width, float height,
                                                boolean hasStandardBoatItem) {
        return new BoatBreakRules.BoatInfo(typeId, tags, width, height, hasStandardBoatItem);
    }
}
