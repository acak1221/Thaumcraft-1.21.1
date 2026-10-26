<div align="center">

<img src="docs/images/readme/hero.jpg" alt="An aura node glowing above an infusion altar in a Magical Forest" width="100%">

# Thaumcraft 4 Reload

**Azanor's Thaumcraft 4.2.3.5, carried from Minecraft 1.7.10 to 1.21.1 — on Fabric and NeoForge.**

[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=flat-square)](#installation)
[![Fabric](https://img.shields.io/badge/Fabric-supported-DBD0B4?style=flat-square)](#installation)
[![NeoForge](https://img.shields.io/badge/NeoForge-supported-D7742F?style=flat-square)](#installation)
[![Java 21](https://img.shields.io/badge/Java-21-E76F00?style=flat-square)](#building-from-source)
[![Status: alpha](https://img.shields.io/badge/status-alpha-8A4BD6?style=flat-square)](#project-status)

**English** · [Русский](README.ru.md)

</div>

---

Thaumcraft 4 Reload is an unofficial, faithful port of the classic magic mod. The original
4.2.3.5 release is the specification: the research tree and its keys, the 48 aspects and the
aspect values of items, recipes, vis costs, mob behaviour and world generation follow the 1.7.10
mod, quirks included. Where 1.21.1 has no equivalent (item metadata, the ore dictionary, Baubles),
the port maps the original onto the modern game and writes every deviation down in
[`docs/audit/KNOWN_DEVIATIONS.md`](docs/audit/KNOWN_DEVIATIONS.md).

On top of that it adds what a modern pack expects: recipe viewer pages, Jade tooltips, a
configuration screen and a set of optional quality-of-life tweaks that you can switch off to get
the 1.7.10 behaviour back.

## Screenshots

<table>
  <tr>
    <td width="50%"><img src="docs/images/readme/infusion.jpg" alt="An infusion altar drawing essentia from jars"></td>
    <td width="50%"><img src="docs/images/readme/magical-forest.jpg" alt="A Magical Forest from above"></td>
  </tr>
  <tr>
    <td align="center"><sub>Runic infusion: pedestals, pillars and jars of essentia</sub></td>
    <td align="center"><sub>Magical Forest with greatwood and silverwood</sub></td>
  </tr>
  <tr>
    <td><img src="docs/images/readme/thaumonomicon-map.jpg" alt="The Thaumonomicon research map"></td>
    <td><img src="docs/images/readme/thaumonomicon-page.jpg" alt="A Thaumonomicon page with an arcane recipe"></td>
  </tr>
  <tr>
    <td align="center"><sub>The Thaumonomicon, with search and a completion counter</sub></td>
    <td align="center"><sub>Research pages with their arcane, crucible and infusion recipes</sub></td>
  </tr>
  <tr>
    <td><img src="docs/images/readme/research-table.jpg" alt="Research notes on the research table"></td>
    <td><img src="docs/images/readme/thaumometer.jpg" alt="Scanning an aura node with the thaumometer"></td>
  </tr>
  <tr>
    <td align="center"><sub>Solving research notes on the research table</sub></td>
    <td align="center"><sub>Scanning an aura node with the thaumometer</sub></td>
  </tr>
</table>

## What is in it

| | |
|---|---|
| **Research** | The Thaumonomicon with all six categories and 199 research entries, the research table and its hex puzzle, the thaumometer, aspect discovery and combination, warp and its events |
| **Thaumaturgy** | Wands, staves and sceptres from every rod and cap, vis and its discounts, foci and their upgrades, the focal manipulator, the focus pouch |
| **Alchemy** | The crucible, essentia, jars, tubes, valves and buffers, alembics, the alchemical furnace, the centrifuge, the thaumatorium, crystallizers, the essentia mirror |
| **Artifice** | The arcane workbench, the infusion altar and its instability, enchantment infusion, runic shielding, goggles, travel gear, hover girdle, thaumium and void gear, the arcane bore, lamps, mirrors and wards |
| **Golemancy** | Golems of every material, cores, upgrades, the golem bell and its markers |
| **Eldritch** | The Outer Lands, the eldritch portal and its maze, cultists, guardians, the eldritch golem and warden |
| **World** | Aura nodes and their ecology, Magical Forest, Eerie and Tainted Land biomes, taint and its creatures, wisps, pech, firebats, cinnabar and amber ores, shimmerleaf, cinderpearl, mana beans |
| **Baubles** | Built in: an amulet, two rings and a belt. Open them with **B** or the button next to your character; use a bauble in your hand to put it on. No extra mod required |

About 320 items, 160 blocks and 55 entities are ported. Interface languages: English and Russian.

## Quality of life

Every tweak is a setting in the configuration screen (Mod Menu on Fabric, the mod list on
NeoForge) and on by default; switched off, the original rule applies.

- **Aspects for every item.** The original aspect table and its ore dictionary groups, mapped to
  1.21.1 items and `c:` tags, plus an inference for items that did not exist in 1.7.10.
- **Thaumonomicon.** Search, a completion counter, research bookmarks, keyboard navigation, reopening
  the page you left.
- **Ideas from [TC4Tweaks](https://github.com/Glease/TC4Tweaks)**, re-implemented: varied loot,
  earth shock that leaves items alone, infusion that keeps enchantments and names, filled phials
  that stay in your hand, a button that clears notifications.
- **Recipe viewers.** Arcane, crucible, infusion and enchantment infusion recipes, and aspect
  combinations, in [EMI](https://modrinth.com/mod/emi) (including "fill the workbench") and
  [JEI](https://modrinth.com/mod/jei).
- **[Jade](https://modrinth.com/mod/jade).** Essentia in jars, tubes and machines behind the goggles
  of revealing, the state of furnaces, crucibles and infusions, a golem's core and upgrades.

## Installation

| | Fabric | NeoForge |
|---|---|---|
| Minecraft | 1.21.1 | 1.21.1 |
| Loader | Fabric Loader 0.16.14 or newer | NeoForge 21.1 (built against 21.1.259) |
| Required | [Fabric API](https://modrinth.com/mod/fabric-api), [Architectury API](https://modrinth.com/mod/architectury-api) 13.0.11+ | [Architectury API](https://modrinth.com/mod/architectury-api) 13.0.11+ |
| Optional | EMI, JEI, Jade, Mod Menu | EMI, JEI, Jade |
| Java | 21 | 21 |

Download the jar for your loader from [Releases](../../releases) and put it in `mods` together with
the required libraries. Start a **new world**: worlds from Thaumcraft 4 on 1.7.10 cannot be
converted.

## Project status

This is an **alpha**. All of the original content is in and exercised by automated tests, but the
port has not yet had long survival playthroughs by many players. Please report what you find.

What backs the port up:

- every original class, recipe, research entry and asset is accounted for in
  [`docs/porting/scope-ledger.json`](docs/porting/scope-ledger.json) (1044 of 1044 units with a
  verdict and its evidence);
- about 2,600 unit tests and about 480 in-game GameTest scenarios, nearly all of them run on both
  loaders;
- visual checks against the original in a real client, with screenshots under
  [`docs/analysis`](docs/analysis).

Known gaps are listed in [`docs/audit/KNOWN_DEVIATIONS.md`](docs/audit/KNOWN_DEVIATIONS.md) §7;
NeoForge has had less client and multiplayer testing than Fabric.

## Building from source

```bash
./gradlew :common:test :fabric:build :neoforge:build
```

The jars land in `fabric/build/libs` and `neoforge/build/libs`. The GameTest suites need a fresh
test world:

```bash
rm -rf fabric/run-gametest/world && ./gradlew :fabric:runGameTest
rm -rf neoforge/run-gametest/world && ./gradlew :neoforge:runGameTest
```

`./gradlew :fabric:runClient -PwithEmi` (or `-PwithJei`, `-PwithJade`) starts a development client
with an optional integration. [`docs/HANDOFF.md`](docs/HANDOFF.md) describes the layout of the
project and the state of every part of the port.

## FAQ

**Is this the official Thaumcraft?** No. Thaumcraft is Azanor's mod; this is a fan port of its
4.2.3.5 version and is not endorsed by Azanor.

**Do I need Baubles, Thaumic Tinkerer or other addons?** No. The bauble slots are built in. Old
1.7.10 addons do not work on 1.21.1.

**Why Thaumcraft 4 and not 6?** Thaumcraft 4 is the version many players remember best and the one
the original addons and packs were built around. This port keeps its rules rather than mixing in
later versions.

## Credits and license

- **Thaumcraft** was created by **Azanor**. Its name, textures, sounds, models and game design are
  his work.
- The port: Thaumcraft 4 Reload contributors. It grew out of a Forge 1.12.2 port by Andrey Bash and
  contributors, whose MIT notice is kept in [`LICENSE`](LICENSE).
- References, adapted algorithms and their licenses are listed in
  [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md), among them OpenTC4, Minecraft Forge 1.7.10 and
  the ideas of TC4Tweaks by Glease (no code copied).

The MIT license in [`LICENSE`](LICENSE) covers the port's own work. It grants no rights to
Azanor's original material, which is not relicensed. This project is not affiliated with Azanor,
Mojang Studios or Microsoft.
