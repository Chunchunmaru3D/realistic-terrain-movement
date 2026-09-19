package com.realisticterrainmovement;

import com.realisticterrainmovement.config.RealisticTerrainMovementConfig;
import com.realisticterrainmovement.events.BoatBreakEvents;
import com.realisticterrainmovement.events.MovementModifierEvents;
import com.realisticterrainmovement.events.WindEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(RealisticTerrainMovementMod.MOD_ID)
public class RealisticTerrainMovementMod {

    public static final String MOD_ID = "realisticterrainmovement";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public RealisticTerrainMovementMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, RealisticTerrainMovementConfig.SPEC);

        NeoForge.EVENT_BUS.register(new MovementModifierEvents());
        NeoForge.EVENT_BUS.register(new WindEvents());
        BoatBreakEvents boatBreakEvents = new BoatBreakEvents();
        NeoForge.EVENT_BUS.register(boatBreakEvents);
        modEventBus.addListener(boatBreakEvents::onConfigChanged);

        LOGGER.info("Realistic Terrain Movement mod loaded! Mechanics: terrain (sticky on jump), wind, boats.");
    }
}
