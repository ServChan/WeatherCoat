# Weather Coat — User Guide

Mod version: 1.1.0 · Minecraft 26.3 · Fabric · client-only

[Русская версия](USER_GUIDE_RU.md)

## Contents

1. [What the mod does](#what-the-mod-does)
2. [Installation](#installation)
3. [What it looks like in game](#what-it-looks-like-in-game)
4. [Puddles and snow film on the ground](#puddles-and-snow-film-on-the-ground)
5. [What speeds the effect up or slows it down](#what-speeds-the-effect-up-or-slows-it-down)
6. [Which mobs stay unchanged](#which-mobs-stay-unchanged)
7. [Settings](#settings)
8. [Compatibility](#compatibility)
9. [Performance](#performance)
10. [Files](#files)
11. [FAQ](#faq)
12. [Building from source](#building-from-source)

## What the mod does

In the rain mobs and players get wet step by step, in snowfall they get covered in snow, and afterwards they dry off and thaw just as gradually. The mod paints this right onto the textures: a wet mob darkens and gets streaks running down its sides, and snow first settles on the back and head and only then slides down the sides.

The mod only draws. It changes neither the world nor mob behaviour, the server does not need it, and players without the mod see ordinary mobs.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) `0.19.3` or newer for Minecraft 26.3.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) for 26.3 (`0.161.0+26.3` or newer) into the `mods` folder.
3. Put `weather-coat-1.1.0.jar` into `mods` (see [Building from source](#building-from-source)).
4. Optional: [Mod Menu](https://modrinth.com/mod/modmenu), which opens the settings.

Java 25 is required. The mod has no keys: the effect starts on its own when rain or snow begins.

## What it looks like in game

### Stages

The effect builds up over several steps (4 by default, 3 or 5 possible). In the rain each wet stage takes 15 seconds, and drying goes back down at 30 seconds per stage. Snow builds up at 20 seconds per stage and melts at 40 seconds per stage.

Stages grow only under open sky. A mob under a roof, in a cave or under a tree does not get any wetter, but it does not dry instantly either.

### A wet mob

- The coat darkens and gets more saturated.
- Streaks with drops at their ends run down the sides.
- Wool and skin get highlights.
- Drops fall off the mob.

### A snowy mob

- Snow first settles on faces that are open to the sky: the back, the head, the shoulders.
- Then it slides down the edges of the sides.
- Places covered by another part of the model (legs under the body, the body under the head) stay clean.
- The last stages add frost and clinging flakes.
- A walking mob sheds snowflakes.

### Frost

A mob or player freezing in powder snow turns bluish and gets ice crystals. Powder snow itself covers them in snow at once.

### What changes

Textures are built from the mob's own texture and the shape of its model, so the effect works for every mob, every resource pack and mobs from other mods. Separate layers get wet too:

- sheep wool, villager and skeleton clothing, the drowned's outer layer, the slime's outer layer;
- horse markings, the wolf collar, the player cape;
- equipment: player and mob armor, pig, horse and strider saddles, horse, wolf and nautilus armor, llama carpets, the ghast harness, elytra.

Armor trims stay as they are. On dyed wool and dyed leather the snow stays white: it is drawn as a separate undyed layer on top.

Player skins are treated like mob textures. First-person hands get wet and frosty together with the body.

## Puddles and snow film on the ground

**Puddles.** In the rain, see-through puddles gather on open ground (stone, grass, paths, slabs, carpets). They grow over 6 stages, rain drops ripple in them, and they dry up after the rain, faster in sunshine and heat. Puddles appear only where rain actually falls (the same height map vanilla uses), so there are none under roofs. At the edge of a ledge a puddle fades out smoothly instead of ending abruptly.

**Snow film.** In snowfall a thin see-through white layer settles on open ground and treetops: patchy at first, then almost solid. It melts slowly after the snowfall.

Ground effects are drawn around you within 16–64 blocks (32 by default). They are purely visual: the mod never places real snow or water blocks.

**Catch up on sight.** If rain or snow is already falling when you join a world or come back from another dimension, puddles, the snow film and mobs start fully developed. A mob that comes into view in the middle of long rain is wet right away too.

## What speeds the effect up or slows it down

| Condition | Effect | Depends on Climate and heat |
|---|---|---|
| Thunderstorm | gets wet and snowy 1.5 times faster | no |
| A biome where it does not snow (warm) | snow melts 2 times faster | no |
| Walking | snow melts 1.5 times faster, snowflakes fall off | no |
| Hot biome (desert, badlands, savanna) | dries and melts 2 times faster | yes |
| Sunshine overhead, no rain | dries and melts 1.5 times faster | yes |
| Fire, lava, a campfire, magma or a lit furnace within 2 blocks | dries and melts 4 times faster, gives off steam | yes |
| Swamp, mangrove swamp, jungle | dries more slowly (speed 0.6) | yes |
| Mob in water | does not dry | no |
| Mob on fire or in lava | instantly dry and snow-free | no |
| A wolf shakes itself | instantly dry and snow-free | no |

Factors multiply: a mob in a desert, in sunshine, next to a campfire dries 2 × 1.5 × 4 = 12 times faster than normal.

Snow only appears where it actually snows (cold biomes and high mountains), rain only where it rains.

## Which mobs stay unchanged

By default the effect leaves alone:

- **water mobs**: fish, squid, dolphins, axolotls, nautiluses and others — the Skip water creatures option;
- **fire-immune mobs**: blazes, ghasts, magma cubes, striders and others — the Skip fire creatures option;
- **the exclusion list**: guardians, elder guardians, snow golems, allays and vexes.

Edit the list on the Excluded Mobs screen: it lists every mob, modded ones included. Turn a mob off to keep it dry and clean in any weather. Grey rows are already skipped by the water and fire creature options.

## Settings

Open **Mod Menu → Weather Coat**. Save and close saves, Reset restores the defaults.

### General

| Setting | Default | What it does |
|---|---|---|
| Weather on mobs | on | the main switch |
| Players too | on | apply the effect to players, including you in third person |
| Stages | 4 | 3, 4 or 5 steps from clean to fully wet or snowy |
| Catch up on sight | on | a mob that comes into view during long rain or snow looks as if it had been outside all along |
| Distance | 64 blocks | 16, 32, 48, 64, 96, 128, 192 or 256. Mobs farther away keep their current look and are not updated |

### Rain

| Setting | Default | What it does |
|---|---|---|
| Get wet in the rain | on | only under open sky, in biomes where it rains |
| Time per wet stage | 15 s | 5, 10, 15, 20, 30, 45, 60, 90 or 120 s |
| Time per dry stage | 30 s | 10, 20, 30, 40, 60, 90, 120, 180 or 300 s |

### Snow

| Setting | Default | What it does |
|---|---|---|
| Get snowy in the snow | on | only under open sky, in biomes where it snows |
| Time per snow stage | 20 s | 5–120 s |
| Time per melt stage | 40 s | 10–300 s; twice as fast in warm biomes |
| Frost when freezing | on | frost and ice crystals when freezing in powder snow |

### Ground

| Setting | Default | What it does |
|---|---|---|
| Rain puddles | on | puddles on open ground |
| Snow film on the ground | on | a see-through snow layer on the ground and treetops |
| Ground effect radius | 32 blocks | 16, 24, 32, 48 or 64. A smaller radius is lighter on the game |

### World

| Setting | Default | What it does |
|---|---|---|
| Climate and heat | on | the biome, sunshine and heat source factors |
| Particles | on | drops, snowflakes and steam |

### Mobs

| Setting | Default | What it does |
|---|---|---|
| Skip water creatures | on | water mobs keep their look |
| Skip fire creatures | on | fire-immune mobs keep their look |
| Excluded mobs | 5 mobs | opens the exclusion list |

## Compatibility

- **Resource packs** work: textures are built from whatever is loaded, and everything is rebuilt when packs change.
- **Entity Texture Features (ETF)**: ETF random textures are kept and get wet too. Emissive layers (`_e`) are not drawn on a wet mob.
- **Entity Model Features (EMF)**: the snow shape follows the EMF model.
- **Mobs from other mods** work when they are drawn with normal Minecraft layers.
- **Sodium and Iris**: the mod only changes entity textures and draws puddles as a separate layer, but it has not been tested with shader packs specifically.

## Performance

- Textures are generated on a background thread, so the game does not stall.
- Each stage of each texture is built once and shared by every mob with that texture.
- Unused variants are freed after a minute.
- Mobs beyond the Distance are not updated.
- If FPS is low, lower Ground effect radius or turn off puddles and the snow film.

## Files

- `config/weathercoat.json` holds the settings. It is written atomically with a backup `weathercoat.json.bak`.

Out-of-range values are clamped on load: stages 3–5, wet and snow time 2–300 s, dry and melt time 2–600 s, distance 16–256, ground radius 16–64.

## FAQ

**A mob in the rain does not get wet.** Check that it is under open sky, not on the exclusion list and within the Distance. It never rains in deserts and savannas.

**Why is a mob wet when it is snowing?** Precipitation depends on the biome and height. It can rain at the foot of a mountain and snow at the top.

**Puddles on a roof.** Puddles appear where the rain lands, on the topmost block. A slab or full-block roof is a valid surface.

**A mob's glowing eyes (ETF) disappeared in the rain.** That is intended: the emissive layer is not drawn on a wet mob.

**Do other players need the mod?** No. Everyone sees the effect only on their own client, if they have the mod.

## Building from source

JDK 25 is required.

```powershell
git clone https://github.com/ServChan/WeatherCoat.git
cd WeatherCoat
.\gradlew.bat build
```

On Linux and macOS use `./gradlew`. The jar is `build/libs/weather-coat-1.1.0.jar`.
