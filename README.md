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

Fully configurable / extendable via `blockSpeedModifiers` (signed values: positive = bonus, negative = penalty).
Includes ready-made compatibility entries for **Biomes O'Plenty**, **Regions Unexplored**,
**Nature's Spirit**, and **Unearthed (Unofficial Port)** — entries for mods you don't have installed
are simply ignored, no crash. **Geophilic** uses vanilla blocks, so it is supported automatically
without separate entries.

For the current NeoForge 1.21.1 releases, all Biomes O' Plenty sands are covered through
`#minecraft:sand`, its sandstone blocks through `#c:sandstone/blocks`, and its sandstone slabs/stairs
have explicit entries. Regions Unexplored uses its own `silt`, `peat`, and `ash` tags, plus explicit
chalk and argillite surfaces. Legacy IDs remain in the defaults for older compatible releases.

Nature's Spirit pink sand, sandy soil, and all wood types join the vanilla `sand` and `planks` tags.
Its coloured chalk and kaolin use their own material tags; chert, travertine, and pink sandstone include
their natural, slab, stair, polished, brick, tile, and mossy variants. Unearthed's natural rock strata and
cobbled rock variants use the common `#c:stones` and `#c:cobblestones` tags, while its regolith retains
the standard dirt penalty through `#minecraft:dirt`.

### 2. Smooth terrain transitions and anti-jump abuse
Terrain speed changes are blended over **6 ticks** by default, avoiding abrupt speed/FOV jumps
when crossing block boundaries. On a slowing surface, horizontal jump carry is reduced at takeoff;
the last terrain value is only held for **0.4 seconds** by default. Both protections are configurable.

### 3. Boots halve penalties
Anything equipped in the **feet slot** cuts all terrain **penalties** in half. Bonuses
(path, planks, bricks) are unaffected. Can be disabled entirely.

### 4. Applies to mobs too
All the above applies to **every mob**, except:
- **Aquatic creatures** (fish, squid, dolphin, guardians, etc.) — always excluded
- **Legless / flying mobs** — slime, magma cube, shulker, ghast, phantom, bat, blaze, wither, ender dragon, vex, allay (configurable list)

### 5. High-altitude wind (Y ≥ 120)
- A slowly rotating wind pushes you sideways and slows walking against it
- Wind is always active by default; **vanilla rain or thunderstorms** increase its strength by **30%**
- Only active in **open air** (must see the sky — caves/structures are sheltered)
- Wind strength scales up to Y=250
- The client-side wind sound is independent from wind movement: it can remain enabled when `windEnabled=false`
- Sound intensity follows a smooth exponential curve: **5%** at Y=90, **30%** at Y=120,
  **60%** at Y=180, and **100%** at Y=250; it fades instead of restarting during short falls
- **Suspended during a normal jump** — while ascending less than 3 blocks above your last
  ground position, wind is ignored, so climbing terraces/cliffs by jumping still works
- A rider and mount are never pushed separately. By default, ridden mounts are unaffected;
  this can be enabled with `affectsMountedEntities`

### 6. Boats break in the open ocean
Any boat in water within an **ocean biome** instantly breaks and **drops itself as a pickable item**
(matching wood type). This includes boats that drift into an ocean and boats restored from a saved world.

---

## Config (`config/realisticterrainmovement-common.toml`)

### Master switches — `[features]`
| Key | Default | Disables |
|---|---|---|
| `terrainEnabled` | `true` | Block bonuses/penalties + boots reduction entirely |
| `windEnabled` | `true` | High-altitude wind push and slowdown entirely |
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
| | `difficulty` | `NORMAL` | `EASY`, `NORMAL`, or `HARDCORE`; scales penalties only |
| | `easyPenaltyMultiplier` | `0.5` | Penalty multiplier for `EASY` |
| | `hardcorePenaltyMultiplier` | `1.35` | Penalty multiplier for `HARDCORE` |
| | `terrainStickyTicks` | `8` | Ticks (20=1s) a terrain value is held after a jump |
| | `terrainTransitionTicks` | `6` | Ticks used to smooth speed changes; `0` is instant |
| | `jumpDistanceLimitEnabled` | `true` | Reduce horizontal jump carry on slowing terrain |
| | `jumpDistanceLimitFloor` | `0.3` | Minimum horizontal carry fraction after the jump penalty |
| `boots` | `bootsReductionEnabled` | `true` | Whether boots reduce penalties at all |
| | `bootsPenaltyReduction` | `0.5` | Penalty multiplier with boots on |
| `wind` | `windStartY` | `120` | Wind begins here |
| | `windMaxY` | `250` | Full wind strength here |
| | `windMaxPush` | `0.03` | Max sideways push/tick |
| | `windSlowFactor` | `0.2` | Max headwind slowdown |
| | `windDirectionPeriodTicks` | `6000` | Wind rotation period |
| | `windJumpExemptionHeight` | `3.0` | Ascent (blocks) below which wind is ignored |
| | `affectsMountedEntities` | `false` | Apply wind to a ridden mount; its rider is never pushed separately |
| | `windSoundEnabled` | `true` | Enable the independent client-side procedural wind sound |
| | `windSoundMaxVolume` | `0.45` | Maximum volume; its height curve is 5%/30%/60%/100% at Y=90/120/180/250 |
| `boats` | `breakBoatsInOcean` | `true` | Toggle (redundant with `features.boatsEnabled`, kept for fine control) |

All changes take effect live — no restart needed. `NORMAL` preserves the existing block modifier
percentages; `EASY` and `HARDCORE` only scale the final penalty, so modpack authors can still edit
every individual `blockSpeedModifiers` value.

---

## Building

Requirements: **JDK 21**

```bash
./gradlew build      # Linux / macOS
gradlew.bat build     # Windows
```

Output: `build/libs/realistic-terrain-movement-1.1.0.jar` → drop into `mods/`.

---

## Technical notes
- Terrain, boats, and wind movement run **server-side only**, via `EntityTickEvent.Post`.
- Terrain + boots use a combined `MOVEMENT_SPEED` attribute modifier (`ADD_MULTIPLIED_TOTAL`).
  The modifier transitions smoothly and is only updated when its value changes. While airborne,
  the last grounded value is held for the configured short sticky period.
- Wind is applied as **direct velocity manipulation**, not via the attribute — kept reliably
  felt and doesn't trigger vanilla's speed-based FOV widening.
- Wind tracks each entity's last on-ground Y position to detect "is this a normal jump" and
  skips pushing/slowing during jumps under the configured ascent threshold.
- Biome-mod block entries use plain `modid:block` or `#modid:tag` syntax — if that mod isn't
  installed, the registry lookup simply finds nothing and the entry is a no-op.
- Config lists are cached and only re-parsed when the config actually changes.
- Per-entity movement state is removed when an entity leaves the level, preventing long-running
  servers from retaining stale entries.

## Changelog (version 1.1.0)
- Added `EASY`, `NORMAL`, and `HARDCORE` terrain presets while keeping every block modifier configurable.
- Smoothed terrain transitions and improved jump protection; vegetation slowdown is disabled by default but can be re-enabled in `blockSpeedModifiers`.
- Expanded terrain support for Biomes O' Plenty, Regions Unexplored, Nature's Spirit, and Unearthed.
- Wind is always active in open air above `windStartY`; vanilla rain and thunderstorms increase its strength by 30%.
- Added an independent procedural wind sound: smooth fade during falls and 5%/30%/60%/100% volume at Y=90/120/180/250.
- Fixed player-reported bugs.

## Changelog (version 1.0.0)
- Terrain-Based Speed: Walking on paths or bricks grants a +20% speed bonus. Rough terrain like mud, sand, snow, and plants applies penalties (from −5% up to −60%). Effects from the block you stand on and the block you stand in stack.
- Anti-Jump Abuse: Terrain modifiers persist for 2 seconds after jumping, preventing players from bypassing slowdowns by spamming the spacebar.
- Boots Protection: Wearing any footwear automatically cuts all terrain penalties in half (bonuses remain unaffected).
- High-Altitude Wind (Y ≥ 120): Strong winds under the open sky push you sideways and slow you down when walking against them. The wind direction rotates every 5 minutes. Wind is temporarily ignored during normal cliff-climbing jumps.
- Biome-mod compatibility: Biomes O'Plenty and Regions Unexplored block entries; Geophilic works automatically with vanilla blocks
- Mob Compatibility: Mechanics apply to all mobs, except for aquatic, flying, or legless creatures (e.g., fish, ghasts, slimes).
