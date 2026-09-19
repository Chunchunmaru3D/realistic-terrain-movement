package com.realisticterrainmovement.events;

import java.util.Set;
import java.util.function.Predicate;

final class BoatBreakRules {

    private static final float HULL_SIZE_TOLERANCE = 0.05F;

    private final float vanillaBoatWidth;
    private final float vanillaBoatHeight;
    private Set<String> whitelistedTypes = Set.of();
    private Set<String> whitelistedTags = Set.of();
    private Set<String> blacklistedTypes = Set.of();
    private Set<String> blacklistedTags = Set.of();

    BoatBreakRules(float vanillaBoatWidth, float vanillaBoatHeight) {
        this.vanillaBoatWidth = vanillaBoatWidth;
        this.vanillaBoatHeight = vanillaBoatHeight;
    }

    void reload(Set<String> whitelistedTypes, Set<String> whitelistedTags,
                Set<String> blacklistedTypes, Set<String> blacklistedTags) {
        this.whitelistedTypes = Set.copyOf(whitelistedTypes);
        this.whitelistedTags = Set.copyOf(whitelistedTags);
        this.blacklistedTypes = Set.copyOf(blacklistedTypes);
        this.blacklistedTags = Set.copyOf(blacklistedTags);
    }

    boolean shouldBreak(BoatInfo boat) {
        return shouldBreak(boat.typeId(), boat.entityTypeTags()::contains, boat.width(), boat.height(), boat.hasStandardBoatItem());
    }

    boolean shouldBreak(String typeId, Predicate<String> hasTag, float width, float height, boolean hasStandardBoatItem) {
        if (matches(typeId, blacklistedTypes, blacklistedTags, hasTag)) return false;
        if (matches(typeId, whitelistedTypes, whitelistedTags, hasTag)) return true;

        return hasVanillaBoatHull(width, height) && hasStandardBoatItem;
    }

    private boolean matches(String typeId, Set<String> typeIds, Set<String> tagIds, Predicate<String> hasTag) {
        if (typeIds.contains(typeId)) return true;
        for (String tagId : tagIds) {
            if (hasTag.test(tagId)) return true;
        }
        return false;
    }

    private boolean hasVanillaBoatHull(float width, float height) {
        return isWithinTolerance(width, vanillaBoatWidth) && isWithinTolerance(height, vanillaBoatHeight);
    }

    private boolean isWithinTolerance(float actual, float expected) {
        return Math.abs(actual - expected) <= expected * HULL_SIZE_TOLERANCE;
    }

    record BoatInfo(String typeId, Set<String> entityTypeTags, float width, float height, boolean hasStandardBoatItem) {
    }
}
