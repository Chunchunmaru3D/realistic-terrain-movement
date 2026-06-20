# Realistic Terrain Movement — NeoForge 1.21.1

Terrain affects your pace, high-altitude wind fights you,
and the open ocean is rough on boats. Every mechanic can be toggled independently.

---

## Mechanics

### 1. Terrain speed modifiers
Checks the block you're **standing on** AND the block you're **standing in** — both contribute.

| Surface | Effect |
|---|---|
| Dirt path | +20% |
| Planks (any wood) | +20% |
| Bricks / stone bricks / chiseled stone / deepslate / blackstone / etc. | +20% |
| Dirt, coarse dirt, podzol, mycelium | −5% |
| Sand / red sand | −20% |
| Gravel | −10% |
| Snow | −35% |
| Soul sand | −50% |
| Soul soil | −40% |
| Powder snow | −60% |
| Mud | −35% |
| Ice | −10% |
| Tall grass, ferns, flowers, crops, saplings, vines | −10% |

Fully configurable / extendable via `blockSpeedModifiers` (signed values: positive = bonus, negative = penalty).
Includes ready-made compatibility entries for **Biomes O'Plenty**, **Regions Unexplored**, and **Geophilic** —
entries for mods you don't have installed are simply ignored, no crash.

### 2. Sticky terrain on jump (anti-abuse)
Jumping no longer cancels terrain penalties/bonuses. The last computed value is held for
**2 seconds** (configurable) after leaving the ground, so jump-spamming across sand/mud/snow
to bypass the slowdown doesn't work anymore.

### 3. Boots halve penalties
Anything equipped in the **feet slot** cuts all terrain **penalties** in half. Bonuses
(path, planks, bricks) are unaffected. Can be disabled entirely.

### 4. Applies to mobs too
All the above applies to **every mob**, except:
- **Aquatic creatures** (fish, squid, dolphin, guardians, etc.) — always excluded
- **Legless / flying mobs** — slime, magma cube, shulker, ghast, phantom, bat, blaze, wither, ender dragon, vex, allay (configurable list)

### 5. High-altitude wind (Y ≥ 120)
- A slowly rotating wind pushes you sideways and slows walking against it
- Only active in **open air** (must see the sky — caves/structures are sheltered)
- Wind strength scales up to Y=250
- **Suspended during a normal jump** — while ascending less than 3 blocks above your last
  ground position, wind is ignored, so climbing terraces/cliffs by jumping still works

### 6. Boats break in the open ocean
Placing a boat in water within an **ocean biome** instantly breaks it and **drops itself as
a pickable item** (matching wood type). Boats already floating from a saved world are left
alone — only freshly placed boats are affected.

---

## Config (`config/realisticterrainmovement-common.toml`)

### Master switches — `[features]`
| Key | Default | Disables |
|---|---|---|
| `terrainEnabled` | `true` | Block bonuses/penalties + boots reduction entirely |
| `windEnabled` | `true` | High-altitude wind entirely |
| `boatsEnabled` | `true` | Boat-breaking mechanic entirely |

Turning a feature off here means **zero performance cost** for that mechanic — it's skipped
before any other logic runs.

### Everything else
| Section | Key | Default | Description |
|---|---|---|---|
| `general` | `affectMobs` | `true` | Apply terrain/wind to mobs too |
| | `affectFlying` | `false` | Apply to flying players |
| | `excludedEntityTypes` | (list) | Mobs always excluded |
| `terrain` | `blockSpeedModifiers` | (list) | Signed block speed values, incl. biome-mod blocks |
| | `terrainStickyTicks` | `40` | Ticks (20=1s) a terrain value is held after a jump |
| `boots` | `bootsReductionEnabled` | `true` | Whether boots reduce penalties at all |
| | `bootsPenaltyReduction` | `0.5` | Penalty multiplier with boots on |
| `wind` | `windStartY` | `120` | Wind begins here |
| | `windMaxY` | `250` | Full wind strength here |
| | `windMaxPush` | `0.03` | Max sideways push/tick |
| | `windSlowFactor` | `0.2` | Max headwind slowdown |
| | `windDirectionPeriodTicks` | `6000` | Wind rotation period |
| | `windJumpExemptionHeight` | `3.0` | Ascent (blocks) below which wind is ignored |
| `boats` | `breakBoatsInOcean` | `true` | Toggle (redundant with `features.boatsEnabled`, kept for fine control) |

All changes take effect live — no restart needed.

---

## Building

Requirements: **JDK 21**

```bash
./gradlew build      # Linux / macOS
gradlew.bat build     # Windows
```

Output: `build/libs/altitude-penalty-1.0.0.jar` → drop into `mods/`.

---

## Technical notes
- All movement logic runs **server-side only**, via `EntityTickEvent.Post`.
- Terrain + boots use a combined `MOVEMENT_SPEED` attribute modifier (`ADD_MULTIPLIED_TOTAL`).
  While airborne, the last grounded value is held (sticky) instead of resetting to neutral.
- Wind is applied as **direct velocity manipulation**, not via the attribute — kept reliably
  felt and doesn't trigger vanilla's speed-based FOV widening.
- Wind tracks each entity's last on-ground Y position to detect "is this a normal jump" and
  skips pushing/slowing during jumps under the configured ascent threshold.
- Biome-mod block entries use plain `modid:block` or `#modid:tag` syntax — if that mod isn't
  installed, the registry lookup simply finds nothing and the entry is a no-op.
- Config lists are cached and only re-parsed when the config actually changes.

## Changelog (version 1.0.0)
- Terrain-Based Speed: Walking on paths or bricks grants a +20% speed bonus. Rough terrain like mud, sand, snow, and plants applies penalties (from −5% up to −60%). Effects from the block you stand on and the block you stand in stack.
- Anti-Jump Abuse: Terrain modifiers persist for 2 seconds after jumping, preventing players from bypassing slowdowns by spamming the spacebar.
- Boots Protection: Wearing any footwear automatically cuts all terrain penalties in half (bonuses remain unaffected).
- High-Altitude Wind (Y ≥ 120): Strong winds under the open sky push you sideways and slow you down when walking against them. The wind direction rotates every 5 minutes. Wind is temporarily ignored during normal cliff-climbing jumps.
- Biome-mod compatibility: Biomes O'Plenty, Regions Unexplored, Geophilic block entries
- Mob Compatibility: Mechanics apply to all mobs, except for aquatic, flying, or legless creatures (e.g., fish, ghasts, slimes).
