package com.realisticterrainmovement.events;

import com.realisticterrainmovement.RealisticTerrainMovementMod;
import com.realisticterrainmovement.config.RealisticTerrainMovementConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Boats break apart instantly upon entering any ocean biome while in water —
 * the open sea is too rough for a simple wooden boat. This breaks regardless
 * of whether a player is riding it (the rider is ejected, not removed).
 *
 * Two detection paths are used together:
 *  - EntityJoinLevelEvent: catches a boat being PLACED directly in ocean water.
 *    The join itself is CANCELLED (not just discarded afterward) so the boat
 *    entity never actually exists/renders — avoids a "ghost boat" flash.
 *  - EntityTickEvent.Post: catches a boat that DRIFTS into an ocean biome while
 *    already floating/being ridden (e.g. paddling from a river out to sea) —
 *    this is the case a join-only check would miss entirely, since the boat
 *    entity already exists and no new join event fires as it moves.
 *
 * Boats already floating from a saved world (loaded from disk) are exempt from
 * the join-time check, but WILL still break on the next tick if found sitting
 * in ocean water — consistent with "any boat in the ocean breaks" behavior.
 */
public class BoatBreakEvents {

    private final BoatBreakPolicy boatBreakPolicy = new BoatBreakPolicy();

    public void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() == RealisticTerrainMovementConfig.SPEC) {
            boatBreakPolicy.reloadFromConfig();
        }
    }

    @SubscribeEvent
    public void onBoatJoin(EntityJoinLevelEvent event) {
        if (!RealisticTerrainMovementConfig.BOATS_ENABLED.get()) return;
        if (!RealisticTerrainMovementConfig.BREAK_BOATS_IN_OCEAN.get()) return;
        if (event.getLevel().isClientSide()) return;
        if (event.loadedFromDisk()) return;
        if (!(event.getEntity() instanceof Boat boat) || !boatBreakPolicy.shouldBreak(boat)) return;

        Level level = (Level) event.getLevel();
        BlockPos pos = boat.blockPosition();

        if (isInOceanWater(boat, level, pos)) {
            breakBoat(boat, level, pos);
            event.setCanceled(true); // prevent the boat from ever actually joining the level
        }
    }

    @SubscribeEvent
    public void onBoatTick(EntityTickEvent.Post event) {
        if (!RealisticTerrainMovementConfig.BOATS_ENABLED.get()) return;
        if (!RealisticTerrainMovementConfig.BREAK_BOATS_IN_OCEAN.get()) return;
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Boat boat) || !boatBreakPolicy.shouldBreak(boat)) return;

        Level level = (Level) boat.level();
        BlockPos pos = boat.blockPosition();

        if (isInOceanWater(boat, level, pos)) {
            breakBoat(boat, level, pos);
        }
    }

    private boolean isInOceanWater(Boat boat, Level level, BlockPos pos) {
        boolean inOceanBiome = level.getBiome(pos).is(BiomeTags.IS_OCEAN);
        boolean inWater = boat.isInWater() || !level.getFluidState(pos).isEmpty();
        return inOceanBiome && inWater;
    }

    private void breakBoat(Boat boat, Level level, BlockPos pos) {
        ItemStack drop = new ItemStack(boat.getDropItem());
        ItemEntity itemEntity = new ItemEntity(level,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
        level.addFreshEntity(itemEntity);

        level.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);

        boat.ejectPassengers(); // don't take the rider down with it — they'll end up swimming
        boat.discard();

        RealisticTerrainMovementMod.LOGGER.debug("Boat broke apart in open ocean at {}, dropped {}", pos, drop);
    }
}
