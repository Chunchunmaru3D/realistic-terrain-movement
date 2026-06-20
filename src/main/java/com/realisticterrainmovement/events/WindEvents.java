package com.realisticterrainmovement.events;

import com.realisticterrainmovement.config.RealisticTerrainMovementConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Above a configurable Y-level, a slowly rotating wind pushes entities sideways
 * and makes walking directly into the wind noticeably slower. Only applies in
 * open air (sky must be visible) — being inside a structure or cave blocks the wind.
 *
 * Wind is suspended while an entity is in the middle of a normal jump (ascending
 * less than ~3 blocks above the point it last left the ground) so players can still
 * climb terraces/cliffs without being shoved off mid-jump.
 */
public class WindEvents {

    private Set<ResourceLocation> excludedTypeCache = null;
    private List<? extends String> lastExcludedSnapshot = null;

    /** Y position each entity was last standing on solid ground, used to detect "is this just a jump?" */
    private final Map<UUID, Double> lastGroundY = new HashMap<>();

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        if (!RealisticTerrainMovementConfig.WIND_ENABLED.get()) return;
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (entity.level().isClientSide()) return;

        boolean isPlayer = entity instanceof Player;
        if (!isPlayer && !RealisticTerrainMovementConfig.AFFECT_MOBS.get()) return;
        if (entity instanceof WaterAnimal) return;

        rebuildExcludedCacheIfNeeded();
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (typeId != null && excludedTypeCache.contains(typeId)) return;

        if (isPlayer && ((Player) entity).getAbilities().flying) return;

        // ── Jump exemption: track last-known ground Y, skip wind while ascending <3 blocks ──
        UUID id = entity.getUUID();
        if (entity.onGround()) {
            lastGroundY.put(id, entity.getY());
        } else {
            double groundY = lastGroundY.getOrDefault(id, entity.getY());
            double ascent = entity.getY() - groundY;
            if (ascent >= 0 && ascent < RealisticTerrainMovementConfig.WIND_JUMP_EXEMPTION_HEIGHT.get()) {
                return; // mid-jump, leave the entity alone
            }
        }

        double y = entity.getY();
        int startY = RealisticTerrainMovementConfig.WIND_START_Y.get();
        int maxY = RealisticTerrainMovementConfig.WIND_MAX_Y.get();
        if (y < startY) return;

        Level level = (Level) entity.level();
        BlockPos pos = entity.blockPosition();
        if (!level.canSeeSky(pos)) return;

        double progress = Math.min((y - startY) / Math.max(1.0, (maxY - startY)), 1.0);
        Vec3 windDir = computeWindDirection(level.getGameTime());

        double pushStrength = progress * RealisticTerrainMovementConfig.WIND_MAX_PUSH.get();
        Vec3 push = windDir.scale(pushStrength);

        Vec3 motion = entity.getDeltaMovement();
        Vec3 newMotion = motion.add(push.x, 0, push.z);

        Vec3 horizontal = new Vec3(newMotion.x, 0, newMotion.z);
        if (horizontal.lengthSqr() > 1.0E-4) {
            Vec3 norm = horizontal.normalize();
            double dot = norm.dot(windDir); // negative = moving against the wind

            if (dot < 0) {
                double slowFactor = 1.0 + dot * progress * RealisticTerrainMovementConfig.WIND_SLOW_FACTOR.get();
                slowFactor = Math.max(0.3, slowFactor);
                newMotion = new Vec3(newMotion.x * slowFactor, newMotion.y, newMotion.z * slowFactor);
            }
        }

        entity.setDeltaMovement(newMotion);
        entity.hurtMarked = true; // ensures velocity change is synced to clients (incl. the player itself)
    }

    /** Slowly rotating wind direction, deterministic from world time (no extra state needed). */
    private Vec3 computeWindDirection(long gameTime) {
        long period = RealisticTerrainMovementConfig.WIND_DIRECTION_PERIOD_TICKS.get();
        double angle = ((double) (gameTime % period) / (double) period) * Math.PI * 2.0;
        return new Vec3(Math.cos(angle), 0, Math.sin(angle));
    }

    private void rebuildExcludedCacheIfNeeded() {
        List<? extends String> current = RealisticTerrainMovementConfig.EXCLUDED_ENTITY_TYPES.get();
        if (current.equals(lastExcludedSnapshot) && excludedTypeCache != null) return;

        lastExcludedSnapshot = current;
        excludedTypeCache = new HashSet<>();
        for (String entry : current) {
            try {
                excludedTypeCache.add(ResourceLocation.parse(entry.trim()));
            } catch (Exception ignored) {
                // skip malformed entries
            }
        }
    }
}
