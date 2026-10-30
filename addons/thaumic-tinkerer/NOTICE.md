# Thaumic Tinkerer for Thaumcraft 4 Reload: sources and licence

This directory is a port of **Thaumic Tinkerer** (Minecraft 1.7.10, an add-on for Thaumcraft 4) to Minecraft 1.21.1,
written against the public `thaumcraft.api` of Thaumcraft 4 Reload. It is a separate mod (`thaumictinkerer`) with its
own jar; it is not part of the Thaumcraft jar.

## Licence

Thaumic Tinkerer was created by **Vazkii** (continuing his *Elemental Tinkerer*), later maintained by **pixlepix** and
**nekosune** and, for 1.7.10, by the **GTNewHorizons** contributors. It is distributed under the
**Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported** licence (`LICENSE` in this directory, legal code
unchanged from the upstream repository). Every Java file ported from the original keeps its original header
("created by Vazkii ... Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012") and adds the line that it was
ported to 1.21.1.

Consequences, because the licence is ShareAlike and NonCommercial:

- The adaptation (everything under `addons/thaumic-tinkerer/common` and `addons/thaumic-tinkerer/fabric`/`neoforge`
  except the build scripts named below) is licensed under **CC BY-NC-SA 3.0**, not under the MIT licence of the rest of
  this repository. It must not be sold or used commercially, and further adaptations must carry the same licence.
- The MIT licence of the repository root covers the files outside this directory and the build files that wire the
  add-on into the build: `fabric.gradle` and `tools/`, and in `standalone/` (the template of the standalone repository) `settings.gradle`,
  `build.gradle`, `gradle.properties`, `.gitignore`, `.github/`, `LICENSE-MIT`, `tools/` and `scripts/porting/README.md`. They contain no
  Thaumic Tinkerer code. (There is no NeoForge build file of the add-on yet; one that wires the add-on into the build would be covered by the
  same sentence.) `standalone/README.md`, `standalone/README.ru.md` and `standalone/NOTICE.md` describe the add-on and are under CC BY-NC-SA 3.0
  like the files they replace in the export; so is `docs/EXPORT.md`, like everything in `docs/`. The Python tools the export copies from
  `scripts/porting` are MIT files of the repository root. The Gradle wrapper that the export copies (`gradlew`, `gradlew.bat`, `gradle/wrapper/`)
  is Gradle's own work under the Apache License 2.0, copied unchanged. The scripts and Gradle files named here carry an
  `SPDX-License-Identifier: MIT` line.
- The add-on jar is a separate download; the Thaumcraft jar contains none of it.
- Publishing the port does not establish rights in Thaumcraft 4 (Azanor), which Thaumic Tinkerer is a derivative work
  of, nor in Minecraft.

## Pinned sources

- Specification of the behaviour: <https://github.com/GTNewHorizons/ThaumicTinkerer>, branch `master`, commit
  `28f4def938e80d7a0b6f433c2b84300dfb7c35ce` ("update+gradle+bs+deps+gitignore", Minecraft 1.7.10, Forge 10.13.4.1614).
- Textures and the language files come from the same commit. `scripts/porting/tt_assets.py` converts them
  deterministically (lower-case snake_case paths, `.lang` to JSON) and records the hash of every source file in
  `scripts/porting/tt_assets_map.json`.
- Read as a reference for the structure of a later port only, no code or assets taken:
  <https://github.com/Thaumic-Tinkerer/ThaumicTinkerer>, commit `a43bc8e226e1efab36dd2796abc4e8050e89992e` (that
  branch is Minecraft 1.12.2 for Thaumcraft 6, a different mod design).

## What changed

Every deviation from the 1.7.10 behaviour is listed in `docs/DEVIATIONS.md` with its reason (1.21.1 has no metadata,
no OreDictionary, no numeric ids, no `EnumHelper`; enchantments, dimensions and recipes are data; the ComputerCraft and
OpenComputers peripherals, NEI, Forge Multipart, EnderIO, Botania, Blood Magic, IC2 and Tinkers' Construct integrations
depend on 1.7.10 mods that do not exist on 1.21.1).
