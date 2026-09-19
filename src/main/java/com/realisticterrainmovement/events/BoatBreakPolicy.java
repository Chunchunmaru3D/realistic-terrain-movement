package com.realisticterrainmovement.events;

import com.realisticterrainmovement.RealisticTerrainMovementMod;
import com.realisticterrainmovement.config.RealisticTerrainMovementConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.BoatItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class BoatBreakPolicy {

    private final BoatBreakRules rules = new BoatBreakRules(EntityType.BOAT.getWidth(), EntityType.BOAT.getHeight());
    private Map<String, TagKey<EntityType<?>>> tagsById = Map.of();

    void reloadFromConfig() {
        reload(
                RealisticTerrainMovementConfig.OCEAN_BOAT_WHITELIST.get(),
                RealisticTerrainMovementConfig.OCEAN_BOAT_BLACKLIST.get()
        );
    }

    private void reload(List<? extends String> whitelistEntries, List<? extends String> blacklistEntries) {
        BoatRuleSet whitelist = BoatRuleSet.parse(whitelistEntries, "whitelist");
        BoatRuleSet blacklist = BoatRuleSet.parse(blacklistEntries, "blacklist");
        Map<String, TagKey<EntityType<?>>> tagKeys = new HashMap<>(whitelist.tags());
        tagKeys.putAll(blacklist.tags());
        tagsById = Map.copyOf(tagKeys);
        rules.reload(whitelist.typeIds(), whitelist.tags().keySet(), blacklist.typeIds(), blacklist.tags().keySet());
    }

    boolean shouldBreak(Boat boat) {
        return rules.shouldBreak(
                EntityType.getKey(boat.getType()).toString(),
                tagId -> boat.getType().is(tagsById.get(tagId)),
                boat.getBbWidth(),
                boat.getBbHeight(),
                boat.getDropItem() instanceof BoatItem
        );
    }

    private record BoatRuleSet(Set<String> typeIds, Map<String, TagKey<EntityType<?>>> tags) {

        private static BoatRuleSet parse(List<? extends String> entries, String listName) {
            Map<String, TagKey<EntityType<?>>> tags = new HashMap<>();
            Set<String> typeIds = new java.util.HashSet<>();
            for (String entry : entries) {
                try {
                    String value = entry.trim();
                    if (value.startsWith("#")) {
                        ResourceLocation tagId = ResourceLocation.parse(value.substring(1));
                        tags.put(tagId.toString(), TagKey.create(Registries.ENTITY_TYPE, tagId));
                    } else {
                        typeIds.add(ResourceLocation.parse(value).toString());
                    }
                } catch (Exception error) {
                    RealisticTerrainMovementMod.LOGGER.warn("Invalid ocean boat {} entry '{}'", listName, entry);
                }
            }
            return new BoatRuleSet(Set.copyOf(typeIds), Map.copyOf(tags));
        }
    }
}
