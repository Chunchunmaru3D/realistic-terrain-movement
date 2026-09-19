package com.realisticterrainmovement.client;

import com.realisticterrainmovement.RealisticTerrainMovementMod;
import com.realisticterrainmovement.config.RealisticTerrainMovementConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = RealisticTerrainMovementMod.MOD_ID, value = Dist.CLIENT)
public final class AltitudeWindSoundEvents {

    private static final float WEATHER_STRENGTH_MULTIPLIER = 1.3F;
    private static final double MIN_SOUND_Y = 90.0;
    private static final double LOW_SOUND_Y = 120.0;
    private static final double MEDIUM_SOUND_Y = 180.0;
    private static final double MAX_SOUND_Y = 250.0;
    private static AltitudeWindSoundInstance windSound;

    private AltitudeWindSoundEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.isPaused() || minecraft.level == null || player == null
                || !RealisticTerrainMovementConfig.WIND_SOUND_ENABLED.get()) {
            stopWindSound();
            return;
        }

        BlockPos pos = player.blockPosition();
        float heightStrength = minecraft.level.canSeeSky(pos) ? getHeightStrength(player.getY()) : 0.0F;
        if (heightStrength > 0.0F && (minecraft.level.isRainingAt(pos) || minecraft.level.isThundering())) {
            heightStrength = Math.min(1.0F, heightStrength * WEATHER_STRENGTH_MULTIPLIER);
        }
        float maxVolume = RealisticTerrainMovementConfig.WIND_SOUND_MAX_VOLUME.get().floatValue();

        if (windSound == null || windSound.isStopped()) {
            if (heightStrength <= 0.0F || maxVolume <= 0.0F) return;
            windSound = new AltitudeWindSoundInstance(player);
            minecraft.getSoundManager().play(windSound);
        }
        windSound.setTargetStrength(heightStrength, maxVolume);
    }

    private static float getHeightStrength(double y) {
        if (y < MIN_SOUND_Y) return 0.0F;
        if (y <= LOW_SOUND_Y) return exponentialInterpolate(0.05F, 0.30F, (y - MIN_SOUND_Y) / (LOW_SOUND_Y - MIN_SOUND_Y));
        if (y <= MEDIUM_SOUND_Y) return exponentialInterpolate(0.30F, 0.60F, (y - LOW_SOUND_Y) / (MEDIUM_SOUND_Y - LOW_SOUND_Y));
        if (y <= MAX_SOUND_Y) return exponentialInterpolate(0.60F, 1.0F, (y - MEDIUM_SOUND_Y) / (MAX_SOUND_Y - MEDIUM_SOUND_Y));
        return 1.0F;
    }

    private static float exponentialInterpolate(float start, float end, double progress) {
        double clampedProgress = Mth.clamp(progress, 0.0, 1.0);
        double smoothProgress = clampedProgress * clampedProgress * (3.0 - 2.0 * clampedProgress);
        return (float) (start * Math.pow(end / start, smoothProgress));
    }

    private static void stopWindSound() {
        if (windSound != null) {
            windSound.stopPlayback();
            windSound = null;
        }
    }

    private static final class AltitudeWindSoundInstance extends AbstractTickableSoundInstance {

        private final LocalPlayer player;
        private float targetStrength;
        private float currentStrength;
        private float maxVolume;
        private int silentTicks;

        private AltitudeWindSoundInstance(LocalPlayer player) {
            super(SoundEvents.ELYTRA_FLYING, SoundSource.AMBIENT, RandomSource.create());
            this.player = player;
            this.looping = true;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.volume = 0.01F;
            this.pitch = 0.7F;
        }

        private void setTargetStrength(float strength, float maxVolume) {
            this.targetStrength = strength;
            this.maxVolume = maxVolume;
        }

        private void stopPlayback() {
            stop();
        }

        @Override
        public void tick() {
            if (player.isRemoved()) {
                stop();
                return;
            }

            currentStrength += (targetStrength - currentStrength) * 0.08F;
            if (targetStrength <= 0.0F) {
                silentTicks++;
                if (silentTicks >= 40 && currentStrength < 0.002F) {
                    stop();
                    return;
                }
            } else {
                silentTicks = 0;
            }

            long gameTime = player.level().getGameTime();
            float gust = (float) (Math.sin(gameTime * 0.11D) * 0.08D
                    + Math.sin(gameTime * 0.037D + 1.7D) * 0.05D);
            float gustedStrength = Mth.clamp(currentStrength * (1.0F + gust), 0.0F, 1.0F);
            volume = maxVolume * gustedStrength;
            pitch = 0.62F + gustedStrength * 0.3F + gust * 0.2F;
        }
    }
}
