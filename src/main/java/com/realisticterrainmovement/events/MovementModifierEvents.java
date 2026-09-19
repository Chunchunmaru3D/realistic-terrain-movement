package com.realisticterrainmovement.events;

import com.realisticterrainmovement.RealisticTerrainMovementMod;
import com.realisticterrainmovement.config.RealisticTerrainMovementConfig;
import com.realisticterrainmovement.config.TerrainDifficulty;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Combines terrain (block-based) and boots speed effects into a single MOVEMENT_SPEED
 * attribute modifier, applied every tick to players and (optionally) mobs.
 *
 * Bonuses and penalties are tracked separately so that boots can reduce ONLY the penalty
 * portion, never the bonus portion.
 *
 * "Sticky" terrain memory: while an entity is airborne (jumping), the last terrain-derived
 * speed modifier is held for a configurable grace period instead of instantly reverting to
 * neutral. This prevents the obvious abuse of jump-spamming across slow terrain (sand, mud,
 * snow, etc.) to bypass the penalty entirely.
 *
 * Jump distance limiting: on slowing terrain, the horizontal component of a jump is dampened
 * the instant the entity leaves the ground (vertical jump height is never touched), so players
 * can't "long-jump" their way across mud/sand/snow at full distance.
 */
public class MovementModifierEvents {

    public static final ResourceLocation SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(RealisticTerrainMovementMod.MOD_ID, "movement_modifier");

    private Map<ResourceLocation, Double> blockValueCache = null;
    private Map<ResourceLocation, Double> tagValueCache = null;
    private List<? extends String> lastConfigSnapshot = null;

    private Set<ResourceLocation> excludedTypeCache = null;
    private List<? extends String> lastExcludedSnapshot = null;

    /** Per-entity: last terrain-derived attribute value (multiplier - 1.0) computed while grounded. */
    private final Map<UUID, Double> lastTerrainValue = new HashMap<>();
    /** Per-entity: ticks remaining that the held terrain value should still apply while airborne. */
    private final Map<UUID, Integer> stickyTicksRemaining = new HashMap<>();
    /** Per-entity: raw penalty fraction (post-boots) computed while grounded, used for jump-distance limiting. */
    private final Map<UUID, Double> lastPenaltySum = new HashMap<>();
    /** Per-entity: was the entity on ground on the previous tick (used to detect the jump-off moment). */
    private final Map<UUID, Boolean> wasOnGround = new HashMap<>();
    /** Per-entity: smoothed modifier currently applied to the movement attribute. */
    private final Map<UUID, Double> currentTerrainValue = new HashMap<>();
    /** Per-entity: target of the current terrain transition. */
    private final Map<UUID, Double> terrainTransitionTarget = new HashMap<>();
    /** Per-entity: ticks left before the current terrain transition reaches its target. */
    private final Map<UUID, Integer> terrainTransitionTicksRemaining = new HashMap<>();

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (entity.level().isClientSide()) return;
        UUID id = entity.getUUID();
        if (!RealisticTerrainMovementConfig.TERRAIN_ENABLED.get()) {
            clearState(id);
            removeModifier(entity);
            return;
        }

        boolean isPlayer = entity instanceof Player;

        rebuildCacheIfNeeded();
        rebuildExcludedCacheIfNeeded();

        if (!isPlayer && !RealisticTerrainMovementConfig.AFFECT_MOBS.get()) {
            clearState(id);
            removeModifier(entity);
            return;
        }

        if (isExcluded(entity)) {
            clearState(id);
            removeModifier(entity);
            return;
        }

        if (isPlayer) {
            Player player = (Player) entity;
            if (player.getAbilities().flying && !RealisticTerrainMovementConfig.AFFECT_FLYING.get()) {
                clearState(id);
                removeModifier(entity);
                return;
            }
        }

        boolean hasBoots = !entity.getItemBySlot(EquipmentSlot.FEET).isEmpty();
        boolean bootsReductionOn = RealisticTerrainMovementConfig.BOOTS_REDUCTION_ENABLED.get();
        double bootsFactor = (hasBoots && bootsReductionOn) ? RealisticTerrainMovementConfig.BOOTS_PENALTY_REDUCTION.get() : 1.0;

        boolean grounded = entity.onGround();
        double targetTerrainValue;

        if (grounded) {
            // Grounded: compute fresh terrain value from the blocks at/below the entity.
            BlockPos feetPos = entity.blockPosition();
            BlockPos belowPos = feetPos.below();

            double feetValue = bestMatchAtPos(entity, feetPos);
            double belowValue = bestMatchAtPos(entity, belowPos);

            double bonusSum = 0.0;
            double penaltySum = 0.0;

            if (feetValue > 0) bonusSum += feetValue; else penaltySum += -feetValue;
            if (belowValue > 0) bonusSum += belowValue; else penaltySum += -belowValue;

            penaltySum *= getDifficultyPenaltyMultiplier();
            penaltySum *= bootsFactor;

            double netFraction = bonusSum - penaltySum;
            double multiplier = 1.0 + netFraction;
            multiplier = Math.max(0.1, Math.min(3.0, multiplier));
            targetTerrainValue = multiplier - 1.0;

            lastTerrainValue.put(id, targetTerrainValue);
            lastPenaltySum.put(id, penaltySum);
            stickyTicksRemaining.put(id, RealisticTerrainMovementConfig.TERRAIN_STICKY_TICKS.get());
        } else {
            // Airborne: hold the last grounded value for the configured grace period, then decay to neutral.
            int remaining = stickyTicksRemaining.getOrDefault(id, 0);
            if (remaining > 0) {
                targetTerrainValue = lastTerrainValue.getOrDefault(id, 0.0);
                stickyTicksRemaining.put(id, remaining - 1);
            } else {
                targetTerrainValue = 0.0;
            }
        }

        applyModifier(entity, transitionTerrainValue(id, targetTerrainValue));

        // ── Jump distance limiting (horizontal only, never touches vertical jump height) ──
        if (RealisticTerrainMovementConfig.JUMP_DISTANCE_LIMIT_ENABLED.get()) {
            boolean wasGrounded = wasOnGround.getOrDefault(id, grounded);
            if (wasGrounded && !grounded) {
                // Just left the ground this tick — this is the jump-off moment.
                double penalty = lastPenaltySum.getOrDefault(id, 0.0);
                if (penalty > 1.0E-6) {
                    double floor = RealisticTerrainMovementConfig.JUMP_DISTANCE_LIMIT_FLOOR.get();
                    double factor = Math.max(floor, 1.0 - penalty);

                    Vec3 motion = entity.getDeltaMovement();
                    entity.setDeltaMovement(motion.x * factor, motion.y, motion.z * factor);
                    entity.hurtMarked = true;
                }
            }
        }
        wasOnGround.put(id, grounded);
    }

    @SubscribeEvent
    public void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        clearState(event.getEntity().getUUID());
    }

    private double getDifficultyPenaltyMultiplier() {
        return switch (TerrainDifficulty.fromConfig(RealisticTerrainMovementConfig.TERRAIN_DIFFICULTY.get())) {
            case EASY -> RealisticTerrainMovementConfig.EASY_TERRAIN_PENALTY_MULTIPLIER.get();
            case NORMAL -> 1.0;
            case HARDCORE -> RealisticTerrainMovementConfig.HARDCORE_TERRAIN_PENALTY_MULTIPLIER.get();
        };
    }

    private double transitionTerrainValue(UUID id, double target) {
        int transitionTicks = RealisticTerrainMovementConfig.TERRAIN_TRANSITION_TICKS.get();
        if (transitionTicks == 0) {
            currentTerrainValue.put(id, target);
            terrainTransitionTarget.put(id, target);
            terrainTransitionTicksRemaining.put(id, 0);
            return target;
        }

        double current = currentTerrainValue.getOrDefault(id, 0.0);
        double previousTarget = terrainTransitionTarget.getOrDefault(id, current);
        int remaining = terrainTransitionTicksRemaining.getOrDefault(id, 0);

        if (Math.abs(target - previousTarget) > 1.0E-6) {
            remaining = transitionTicks;
            terrainTransitionTarget.put(id, target);
        }

        if (remaining > 0) {
            current += (target - current) / remaining;
            remaining--;
        } else {
            current = target;
        }

        currentTerrainValue.put(id, current);
        terrainTransitionTicksRemaining.put(id, remaining);
        return current;
    }

    private double bestMatchAtPos(LivingEntity entity, BlockPos pos) {
        BlockState state = entity.level().getBlockState(pos);
        Block block = state.getBlock();

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        double best = 0.0;
        boolean found = false;

        if (blockId != null && blockValueCache.containsKey(blockId)) {
            best = blockValueCache.get(blockId);
            found = true;
        }

        for (Map.Entry<ResourceLocation, Double> entry : tagValueCache.entrySet()) {
            TagKey<Block> tag = TagKey.create(Registries.BLOCK, entry.getKey());
            if (state.is(tag)) {
                double v = entry.getValue();
                if (!found || Math.abs(v) > Math.abs(best)) {
                    best = v;
                    found = true;
                }
            }
        }

        return best;
    }

    private boolean isExcluded(LivingEntity entity) {
        if (entity instanceof WaterAnimal) return true;
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return typeId != null && excludedTypeCache.contains(typeId);
    }

    private void rebuildCacheIfNeeded() {
        List<? extends String> current = RealisticTerrainMovementConfig.TERRAIN_MODIFIERS.get();
        if (current.equals(lastConfigSnapshot) && blockValueCache != null) return;

        lastConfigSnapshot = current;
        blockValueCache = new HashMap<>();
        tagValueCache = new HashMap<>();

        int skipped = 0;
        for (String entry : current) {
            try {
                int eq = entry.lastIndexOf('=');
                if (eq < 0) continue;
                String key = entry.substring(0, eq).trim();
                double value = Double.parseDouble(entry.substring(eq + 1).trim());
                value = Math.max(-0.95, Math.min(1.0, value));

                if (key.startsWith("#")) {
                    tagValueCache.put(ResourceLocation.parse(key.substring(1)), value);
                } else {
                    blockValueCache.put(ResourceLocation.parse(key), value);
                }
            } catch (Exception e) {
                skipped++;
            }
        }

        RealisticTerrainMovementMod.LOGGER.info("Terrain modifier cache rebuilt: {} blocks, {} tags ({} skipped/invalid entries)",
                blockValueCache.size(), tagValueCache.size(), skipped);
    }

    private void rebuildExcludedCacheIfNeeded() {
        List<? extends String> current = RealisticTerrainMovementConfig.EXCLUDED_ENTITY_TYPES.get();
        if (current.equals(lastExcludedSnapshot) && excludedTypeCache != null) return;

        lastExcludedSnapshot = current;
        excludedTypeCache = new HashSet<>();
        for (String entry : current) {
            try {
                excludedTypeCache.add(ResourceLocation.parse(entry.trim()));
            } catch (Exception e) {
                RealisticTerrainMovementMod.LOGGER.warn("Invalid excluded entity type '{}'", entry);
            }
        }
    }

    private void applyModifier(LivingEntity entity, double value) {
        AttributeInstance attr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;

        AttributeModifier existing = attr.getModifier(SPEED_MODIFIER_ID);
        if (Math.abs(value) <= 1.0E-6) {
            if (existing != null) attr.removeModifier(SPEED_MODIFIER_ID);
            return;
        }

        if (existing != null && Math.abs(existing.amount() - value) <= 1.0E-6) return;

        attr.addOrUpdateTransientModifier(new AttributeModifier(
                SPEED_MODIFIER_ID,
                value,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
    }

    private void removeModifier(LivingEntity entity) {
        AttributeInstance attr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) attr.removeModifier(SPEED_MODIFIER_ID);
    }

    private void clearState(UUID id) {
        lastTerrainValue.remove(id);
        stickyTicksRemaining.remove(id);
        lastPenaltySum.remove(id);
        wasOnGround.remove(id);
        currentTerrainValue.remove(id);
        terrainTransitionTarget.remove(id);
        terrainTransitionTicksRemaining.remove(id);
    }
}
