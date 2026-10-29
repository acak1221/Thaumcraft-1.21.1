# KamiWorld: requests to the Thaumcraft API and to the loader modules

Group KamiWorld: the Bedrock dimension, the Bedrock portal, the Celestial Gateway, the Soul Hearts, the dimensional shard drops
and `/iskamiunlocked`. The numbers `A1`..`A13` are those of `docs/API_GAPS.md` section 2. Nothing in the group reaches into
`thaumcraft.common.*` (the test `TinkererApiOnlyTest` passes over every source, and a text scan of the group's files found no
internal name): the one real gap is a loader hook (section 1); everything else is answered by the API or by Architectury.

## 0. What the group uses from the API (verified in the sources of 2026-10-28)

| Need | Used | Where |
|---|---|---|
| keep Thaumcraft's ores, aura and vegetation out of the new dimension (API_GAPS 1.4) | `ThaumcraftApi.addDimBlacklist(ResourceKey<Level>, 2)` | `KamiWorld.ready()` |
| the ore dictionary names of `EnumOreFrequency` | `OreDictionaryNames.itemTag(name)` (`oreCoal` is `c:ores/coal`, the infused ores and amber and cinnabar are `thaumcraft:original_ore/*`) | `EnumOreFrequency` |
| `Config.allowMirrors` gates the gateway's research and recipe (A5) | `ThaumcraftApi.isMirrorsAllowed()` | `BlockWarpGate.getResearchItem/getRecipeItem` |
| the paving stone of travel, `blockCosmeticSolid:2`, as the centre of the gateway's infusion | `ItemApi.getBlock("blockCosmeticSolid", 2)` | `BlockWarpGate.getRecipeItem` |
| research of the player | `ThaumcraftApiHelper.getCompletedResearch(player)`, `ResearchCategories.getResearch("ICHOR").parentsHidden` | `KamiUnlockedCommand` |
| the wand sound | `ThaumcraftSoundIds.WAND` / `ThaumcraftSoundIds.event` | `TileWarpGate` (the stable key K2/A7 asked for) |

Not needed after all: A8 (client sparkles) for the gateway, because the original's sparkles of the teleport never ran (they sit
in `if (worldObj.isRemote)` of a method that only runs on the server).

## 1. A hook that changes the amount of a hit (S1, new, P2; shared with K1 of `KamiGear.md`)

**Need.** 1.7.10 `SoulHeartHandler.onPlayerDamage` (`LivingHurtEvent`, fired before armour and potion reduction) sets
`event.ammount = removeHP(player, (int) event.ammount)`: the whole points of every hit of every player are taken off the soul
hearts and the rest, a whole number, is the damage that goes on. Architectury's `EntityEvent.LIVING_HURT` can only cancel a hit
(Fabric `ServerLivingEntityEvents.ALLOW_DAMAGE` is the same), so the add-on cannot set the amount from common code.

**What the add-on does today.** `SoulHeartHandler.onLivingHurt` (the Architectury listener): for a player **that has soul hearts**,
outside the invulnerability frames, takes the points off the pool; a hit that the pool covers is cancelled (the damage event of
`ServerLevel.broadcastDamageEvent` still plays the hurt animation and sound), a hit that it does not cover is cancelled and
dealt again as `player.hurt(source, rest)` with the difficulty scaling of `Player.hurt` undone first. It loses the knockback and the
invulnerability frames of a covered hit, it cannot see the frames' `lastHurt` (a protected field), and it leaves players without
hearts alone, where 1.7.10 still cut the fraction off every hit. See `docs/deviations/KamiWorld.md` section 5.

**Proposed (to the lead).** One loader hook that runs before the armour formula and may replace the amount:

- NeoForge: `LivingIncomingDamageEvent` (`event.setAmount(...)`), for `ServerPlayer` victims;
- Fabric: a mixin in the add-on's Fabric module (`@ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)` on
  `LivingEntity`, after `Player.hurt`'s difficulty scaling), or the Fabric API event of a later version if it gets one.

Both call the existing seam, which already implements the exact rule:

```java
float SoulHeartHandler.modifyIncomingDamage(ServerPlayer player, float amount) // saves the pool, tells the client, returns the new amount
```

and set `SoulHeartHandler.exactDamageHook = true` once at start-up, which silences the Architectury fallback. The same hook would
also serve `KamiGear`'s armour absorption (K1, `ISpecialArmor`), which has the same limit.

## 2. Loader modules (to the lead)

Nothing else of this group needs a loader hook:

- the HUD, the screens, the menu, the block entity renderer, the render type, the S2C payload, the commands, the chunk generator
  type, the loot table hook, the player events and the damage event (fallback) are Architectury;
- the dimension and its biome and dimension type are data (`data/thaumictinkerer/...`), loaded by both loaders as part of the
  mod's data pack. NeoForge parity was not looked at (`AGENTS.md`: Fabric first).

Two files in the shared `minecraft` namespace are new in this group (tags that keep the portal block from the Wither and the
Ender Dragon, the 1.7.10 `canEntityDestroy == false`): `data/minecraft/tags/block/wither_immune.json` and `dragon_immune.json`,
entries with `required: false`. If another group needs the same tags, merge the two lists (a tag file of the same path in one
mod would otherwise be the last writer's).

## 3. Not requested, but worth knowing

- `Level.getSeaLevel()`-style questions of the dimension are answered by `ChunkGeneratorBedrock.getSeaLevel()` = 1
  (1.7.10 `getAverageGroundLevel`); nothing of Thaumcraft reads it.
- The dimension's special effects are the overworld's (a mod cannot register `DimensionSpecialEffects` in common code on both
  loaders); if the lead wants the constant fog of 1.7.10 at night too, that needs a loader hook
  (Fabric `DimensionRenderingRegistry`, NeoForge `RegisterDimensionSpecialEffectsEvent`) with an effects id of the add-on.
