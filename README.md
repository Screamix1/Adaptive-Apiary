# Adaptive Apiary

A beekeeping mod for **Minecraft 1.21.1 and NeoForge 21.1** by **Screamix**.

Inspired by Forestry, Adaptive Apiary adds wild hives, princesses, drones, breeding, and genetic
mutations. The mod creates **adaptive ore bees** for the ore materials it detects, including ores
from other mods, without requiring their authors to add support.

## Features

- Seven wild hives that generate in the world: Forest, Meadows, Modest, Tropical, Wintry, Marshy,
  and Rocky. Rocky Hives spawn in caves.
- Sixteen standard bee species plus ore bees generated for the detected materials.
- Bee genetics for species, speed, lifespan, and fertility, with an active and an inactive allele
  for each trait.
- An Apiary with Hive Frames, a Centrifuge, a Scoop, and a Beealyzer.
- A Bee Bestiary that records analyzed species and links to the author's Telegram, CurseForge, and GitHub.
- Nine types of combs, plus Beeswax, Honey Drops, Honeydew, Propolis, and recipes that use them.
- English and Russian translations.
- Hopper and pipe support. Insert bees or extract products from any side of the Apiary.
- Flying bees around wild hives in clear daylight and around working Apiaries. They match the colors
  of their inventory sprites and face the direction they fly.
- Active hives and Apiaries buzz within a range of 10 blocks.
- Custom Apiary and Centrifuge screens with empty-slot hints, a queen lifespan bar, cycle progress,
  color-coded status messages, and a spinning centrifuge drum.
- Optional JEI support for comb processing, bee products, and mutations.

The mod focuses on beekeeping. Forestry features outside its scope include trees, butterflies, farms,
engines, power, mail, backpacks, alvearies, temperature and humidity, bee effects, and beekeeper suits.

## Getting Started

1. Craft a **Scoop** from sticks and wool. Break a wild hive with it to collect a princess, one to
   three drones, and sometimes a comb. A hive broken without a Scoop drops nothing.
2. Craft a **Hive Frame** from sticks and string, then craft an **Apiary** with slabs, planks, and a
   frame in the center.
3. Put a princess and a drone in the Apiary to produce a queen. She can produce combs each work
   cycle and leaves behind a new princess and drones when she dies.
4. Keep the species' required flowers or other blocks within four blocks of the Apiary. Most
   bees also need daylight, open sky, and dry weather. The Apiary displays any missing requirement.
   Rocky, Mineral, and ore bees need stone nearby and can work underground, at night, and in the rain.
5. Use a **Centrifuge**, crafted from iron, glass, and redstone, to process combs into beeswax,
   honey, and other resources. It runs without power.
6. Right-click with the **Beealyzer** to analyze the bees in your inventory. It costs one Honey
   Drop per stack.
7. Add Hive Frames to increase production by 50% each. Frames wear out over time.
8. Craft the **Bee Bestiary** from a book and Beeswax, then right-click to open it. The Beealyzer
   records both species in an analyzed bee's genome, including ore species. To record a bee
   analyzed before the Bestiary was added, open the book with that bee in your inventory.

## Mutations

When a queen dies, the game checks for mutations between her species and the drone's. The two
parent species must differ.

| Parents | Result | Chance |
|---|---|---|
| Two different wild surface species | Common | 15% |
| Common + a wild surface species | Cultivated | 12% |
| Common + Cultivated | Noble | 10% |
| Noble + Cultivated | Majestic | 8% |
| Noble + Majestic | Imperial | 8% |
| Common + Cultivated | Diligent | 10% |
| Diligent + Cultivated | Unweary | 8% |
| Diligent + Unweary | Industrious | 8% |
| Rocky + Diligent | Mineral | 12% |
| **Mineral + Cultivated with an ore block under the Apiary** | **Bee for that ore** | 10% (configurable) |

Each half of the offspring's genome rolls separately, so the first result is usually a hybrid.
Continue breeding to establish a pure line.

## Adaptive Ore Bees

When a world loads, Adaptive Apiary discovers ores through four sources.

1. Item tags such as `c:ores/<material>` and the older `forge:ores/<material>`.
2. The same tags on blocks. Some mods tag the ore block only.
3. Block names such as `silver_ore`, `deepslate_silver_ore`, `nether_silver_ore`,
   `end_stone_silver_ore`, `ore_silver`, `deepslate_ore_silver`, and `silver_ore_nether`.
   This also finds untagged ores such as `azeotrope:silver_ore`.
4. Manual mappings in the `extraOres` configuration option.

The mod looks for the corresponding comb output in this order.

1. Material tags under `c:raw_materials/`, `c:gems/`, `c:dusts/`, or `c:ingots/`, including
   their `forge:` equivalents. If multiple mods provide a matching item, vanilla takes priority,
   followed by the mod that provides the ore.
2. The ore's furnace or blast furnace output.
3. An item with a matching name, such as `raw_silver`, `silver`, `silver_gem`, `silver_ingot`,
   `silver_crystal`, `silver_dust`, `silver_shard`, or `crushed_silver`.
4. The ore itself as a fallback, so every detected ore material gets a bee.

Common materials have translated species names. For other materials the mod uses the name of the
ore from its own mod. The output item's texture determines the bee and comb colors.

To breed an ore bee, put an ore block or a storage block for that material under the Apiary,
then cross Mineral and Cultivated bees. Processing a resource comb in a Centrifuge has a 25%
chance to yield one unit of the material by default.

Run `/adaptiveapiary materials` to see the detected materials and their comb outputs, or
`/adaptiveapiary materials silver` to filter for silver. Hover over a result to see how the ore
and output were found. The server log also reports the results under
`Adaptive Apiary: found N ore materials`.

To register a missing ore, add a mapping to `extraOres`, such as `"somemod:weird_rock=silver"`.

## Configuration

The configuration file is `config/adaptiveapiary-common.toml`.

| Setting | Default | Description |
|---|---|---|
| `cycleTicks` | 300 | Duration of a work cycle in ticks (550 in Forestry) |
| `breedingTicks` | 100 | Duration of breeding in ticks |
| `centrifugeTicks` | 100 | Time to process one comb in ticks |
| `environmentChecks` | true | Check for nearby flowers, daylight, open sky, and dry weather |
| `oreMutationChance` | 0.10 | Chance of a mutation into an ore bee |
| `resourceYield` | 0.25 | Chance to recover a material from a resource comb |
| `materialBlacklist` | [] | Materials excluded from ore bee generation, such as `["diamond", "netherite_scrap"]` |
| `extraOres` | [] | Manual ore mappings in the format `"modid:item=material"` |

## JEI Integration

JEI 19.19 or newer adds the following features. [Just Enough Items](https://www.curseforge.com/minecraft/mc-mods/jei)

- **Centrifuge** recipes show all possible outputs and their chances for each comb, including
  resource combs.
- **Bee Products** lists the combs and other products each species can produce and their chances
  per work cycle.
- **Bee Mutations** shows parents, offspring, and mutation chances. Ore mutations also show the
  block required under the Apiary.
- JEI lists each bee species and each material's resource comb separately.
- The **Uses** view includes descriptions for the Scoop, Hive Frame, Apiary, Beealyzer, and wild hives.

JEI is optional.

## Building from Source

Use Java 17 or newer to run Gradle. Gradle can download the Java 21 toolchain used to compile the
mod. An internet connection is required to download dependencies.

```powershell
.\gradlew.bat build
```

On Linux and macOS, run `./gradlew build`. Place the resulting
`build/libs/adaptiveapiary-1.0.jar` in Minecraft's `mods` directory. To launch a development client,
use `.\gradlew.bat runClient` on Windows or `./gradlew runClient` on Linux and macOS.

The GitHub Actions build workflow can build the mod without a local Java installation. The JAR
is available in the workflow run's **Artifacts** section. [Build workflow](.github/workflows/build.yml)

## Verification

- The Gradle build passed, and the Minecraft 1.21.1 development client launched with NeoForge 21.1.256.
- A server-side NeoForge GameTest verified that the Bestiary records both species of a hybrid,
  retains discoveries across saves and deaths, stores progress separately for each player,
  and loads the crafting recipe.
- Java 21 compilation against dependency stubs checked internal types. Calls to external APIs
  were checked against NeoForge 1.21.1, Forestry: Community Edition for 1.21.1, and JEI for 1.21.1.
- Ore name detection was checked against examples including `deepslate_ore_lead`,
  `nether_silver_ore`, and `silver_ore_stairs`.
- Recipe, loot, and world generation JSON formats were checked against Minecraft 1.21.1 data.
- Model, texture, translation, and recipe IDs were checked against the code.
- The genetics implementation was tested against the mutation chances in the table above.

The in-world Bestiary interface, wild hive generation, balance, and compatibility with other mods
still need in-game testing.

## Project Layout

```text
src/main/java/com/screamix/adaptiveapiary/
  genetics/       Bee species, genomes, mutations, ore detection, and products (Materials.java)
  block/          Wild hives, the Apiary, and the Centrifuge
  blockentity/    Apiary and Centrifuge logic
  item/           Bees, combs, tools, and the Bee Bestiary
  menu/, client/  Screens, item tinting, and flying bee particles
  compat/jei/     Optional JEI categories
  command/        The /adaptiveapiary materials command
  registry/       Mod content registration
src/main/resources/   Models, textures, translations, recipes, and world generation
tools/generate_textures.py   Texture generator (Python 3 with Pillow)
```

Add a standard species in `SpeciesRegistry.java`, its mutations in `Mutations.java`, and its
localized names in `lang/en_us.json` and `lang/ru_ru.json`.

## Credits and License

The beekeeping concept and species names were inspired by Forestry by SirSengir and contributors.
Adaptive Apiary uses original code and textures. The hive buzz comes from recordings made by the
mod's author. The build scaffold comes from the official NeoForge MDK under the MIT license.
[NeoForge MDK template license](TEMPLATE_LICENSE.txt)

Adaptive Apiary's code and original assets are licensed under GNU LGPL version 3.
[LGPLv3](COPYING.LESSER) · [GPLv3](COPYING) · [Copyright notices](NOTICE)

The Gradle Wrapper is licensed under Apache 2.0. [Gradle Wrapper license](GRADLE_WRAPPER_LICENSE.txt)
