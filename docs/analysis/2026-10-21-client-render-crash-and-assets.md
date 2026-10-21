# Client crash of the render thread and the assets it named

Date: 2026-10-21. Source of truth: the oracle
`/home/Acak/work/thaumcraft-4.2.3.5-oracle`, the client log of the instance
`Thaumcraft 1.21.1 (1)`.

## 1. The crash of the render thread

The crash report of the client
`crash-reports/crash-2026-10-21_20.26.26-client.txt` names the cause:

```
java.lang.UnsupportedOperationException: ClientWorld.getColor called with unregistered ColorResolver
    at knot//net.minecraft.class_1163.handler$bep000$thaumcraft$grass(class_1163.java:534)
    at knot//net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo.blockColor(...)
```

`class_1163` is `net.minecraft.client.color.block.BlockColors`. The port mixed
into it and asked the level for the colour of the grass with a
`ColorResolver` of its own. Minecraft 1.21.1 knows three resolvers only
(`BiomeColors.GRASS_COLOR`, `BiomeColors.FOLIAGE_COLOR`, `BiomeColors.WATER_COLOR`)
and the renderer refuses every other one: the fabric renderer v1 turns the
unknown resolver into the `UnsupportedOperationException` above.

The original of TC4 has no such resolver: the option `blue_magical_forest`
changes the colour of the magical forest itself (oracle
`thaumcraft/common/config/ConfigBiomes` / the magical forest of the original is
blue while the option is on).

The port now changes the colour of the biome instead of the resolver:
`common/src/main/java/thaumcraft/common/mixin/client/Tc4BiomeColorMixin.java`
mixes `Biome.getGrassColor(double,double)` and `Biome.getFoliageColor()` and
returns the blue of the original for the magical forest while the option is on.
The native neighbour blending of 1.21.1 is kept, because the blending reads the
colour of the biome.

The old mixin `Tc4BiomeColorsMixin` is gone, the mixin list of
`common/src/main/resources/thaumcraft-common.mixins.json` names the new one.

## 2. The blockstates of the groups with a single metadata value

Minecraft 1.21.1 binds the state space of a block to its property. The port
split the metadata families of the original into one block per group and gave
every group a property with the range of that group. A group with a single
metadata value has no property at all and therefore exactly one state, which
the blockstate has to name with the empty key. The four blockstates of those
groups still named `variant=<n>` and the client reported

```
Exception loading blockstate definition: 'thaumcraft:blockstates/eldritch_deco.json' ... for variant: 'variant=6': Unknown blockstate property: 'variant'
```

The four files (`eldritch_deco`, `eldritch_crystal`, `cosmetic_opaque_warded`,
`airy_unbreakable`) now name the default state, the tests of the assets read the
same key.

## 3. The bone bow

`assets/thaumcraft/models/item/bone_bow.json` named its three pull models
`thaumcraft:bone_bow_pulling_0..2`, but they live in `models/item/`. The
references are now `thaumcraft:item/bone_bow_pulling_0..2`.

`scripts/porting/convert_assets.py` only checked the `parent` of a model, so
the broken references of the overrides passed the check. The verification now
walks `overrides[].model` as well: 2903 local model references, none missing.

## 4. The sampler warning of the vanilla shader

```
Shader rendertype_entity_translucent_emissive could not find sampler named Sampler2 in the specified shader program.
```

This warning belongs to the vanilla program of 1.21.1: the core shader
`assets/minecraft/shaders/core/rendertype_entity_translucent_emissive.json`
declares `Sampler0`, `Sampler1` and `Sampler2`, its fragment shader uses only
`Sampler0`, and the compiler drops the unused sampler, so the lookup of its
location fails (`ShaderInstance.updateLocations`). The port ships only the
programs `thaumcraft_*` and does not touch the core shaders, so the warning is
the one of the vanilla game and does not describe a defect of the port.

## 5. The trade table of the villages

`common/src/gameTest/java/thaumcraft/common/testing/VillageTradeRuntimeChecks.java`
finds the chunks the village plan gives a banker home to, generates them and
looks for the villager of the banker profession.

The villager of the original is placed by the village piece itself (oracle
`thaumcraft/common/lib/world/ComponentBankerHome:112`). The 1.21.1 port places
it as an entity of the generated chunk: the entity is written into the
`ProtoChunk` of the chunk and the world adds it when the chunk reaches the full
status (`ChunkStatusTasks.postLoadProtoChunk`).

The check failed once in seven runs while the isolated run of the same case
passed. The reason is the life of the entities of a chunk in 1.21.1:

- The entities of a chunk are only tracked after the chunk status of the world
  changed (`ChunkHolder.scheduleFullChunkPromotion` ->
  `ChunkMap.onFullChunkStatusChange` -> the entity manager starts to track the
  entities of the chunk). The change runs as a task of the main thread after
  the generation of the chunk.
- The chunk system unloads a chunk that no ticket holds, and the entities of a
  chunk leave the world with the chunk.

The check now holds a ticket on every chunk it generates, so the chunk and its
villager stay in the world, and it reads the villagers in a later tick, when the
entity manager has tracked them. The message of the failure names the
professions of the villagers that were found, so the next failure says whether
the villager is missing or carries another profession.
