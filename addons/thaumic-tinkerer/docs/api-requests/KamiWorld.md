# KamiWorld: requests to the Thaumcraft API and to the loader modules

Group KamiWorld: the Bedrock dimension, the Bedrock portal, the Celestial Gateway, the Soul Hearts, the dimensional shard drops
and `/iskamiunlocked`. The numbers `A1`..`A13` are those of `docs/API_GAPS.md` section 2. Nothing in the group reaches into
`thaumcraft.common.*` (the test `TinkererApiOnlyTest` passes over every source, and a text scan of the group's files found no
internal name): the one real gap was a loader hook (sections 1 and 1b: **answered** by `TinkererEvents.LIVING_DAMAGE` and `LIVING_DROPS`,
`docs/HOOKS.md`, which need a NeoForge poster); everything else is answered by the API or by Architectury.

## 0. What the group uses from the API (verified in the sources of 2026-10-28)

| Need | Used | Where |
|---|---|---|
| keep Thaumcraft's ores, aura and vegetation out of the new dimension (API_GAPS 1.4) | `ThaumcraftApi.addDimBlacklist(ResourceKey<Level>, 2)` | `KamiWorld.ready()` |
| the ore dictionary names of `EnumOreFrequency` | `OreDictionaryNames.itemTag(name)` (`oreCoal` is `c:ores/coal`, the infused ores and amber and cinnabar are `thaumcraft:original_ore/*`) | `EnumOreFrequency` |
| `Config.allowMirrors` gates the gateway's research and recipe (A5) | `ThaumcraftApi.isMirrorsAllowed()` | `BlockWarpGate.getResearchItem/getRecipeItem` |
| the paving stone of travel, `blockCosmeticSolid:2`, as the centre of the gateway's infusion | `ItemApi.getBlock("blockCosmeticSolid", 2)` | `BlockWarpGate.getRecipeItem` |
| research of the player | `ThaumcraftApiHelper.getCompletedResearch(player)`, `ResearchCategories.getResearch("ICHOR").parentsHidden` | `KamiUnlockedCommand` |
| the wand sound | `ThaumcraftSoundIds.WAND` / `ThaumcraftSoundIds.event` | `TileWarpGate` (the stable key K2/A7 asked for) |

Not needed after all: A8 (client sparkles) for the gateway. The original's sparkles of the teleport sit in `if (worldObj.isRemote)` of a method that ran on the server
only on the gateway's own menu and map path, so they never showed there; on the Recall path of the Focus of Celestial Recall they did show on the client, and
the port sends them from the server through `KamiHooks.sparklesAround` (`TileWarpGate.teleportPlayer(ServerPlayer, BlockPos)`, `deviations/KamiWorld.md` section 4),
which needs no client particle API.

## 1. A hook that changes the amount of a hit (S1, new, P2; shared with K1 of `KamiGear.md`) - ANSWERED

**Answered by** `TinkererEvents.LIVING_DAMAGE` (`docs/HOOKS.md` section 3; Fabric: `LivingEntityHurtMixin`; NeoForge: the
`LivingIncomingDamageEvent` handler, **not written yet**). The group uses it: `SoulHeartHandler.onLivingDamage` is registered by
`SoulHeartHandler.register()` and applies the exact rule `SoulHeartHandler.modifyIncomingDamage` (the 1.7.10 handler: `(int) amount`
off the pool for every hit of every player, the rest as the new amount, the client told). The Architectury `LIVING_HURT` fallback
and its flag `exactDamageHook` are deleted. K1 of `KamiGear.md` (`ISpecialArmor`) is **not** served by this event (it sat after the
event in 1.7.10; `HOOKS.md` section 3), and is not this group's.

**Need (kept for the record).** 1.7.10 `SoulHeartHandler.onPlayerDamage` (`LivingHurtEvent`, fired before armour and potion
reduction) sets `event.ammount = removeHP(player, (int) event.ammount)`: the whole points of every hit of every player are taken
off the soul hearts and the rest, a whole number, is the damage that goes on.

**What the event gives.** The amount before the shield, the freezing and helmet adjustments, the invulnerability frames, armour,
Protection, Resistance and Absorption, for every living entity including a server player (after the difficulty scaling, as in 1.7.10);
a listener may return another amount; hits that the game's frames certainly refuse are not posted (`TinkererEvents.framesRefuse`).

**What it does not give, and what the group does about it** (full account in `docs/deviations/KamiWorld.md` section 5):

- *A cancel starts no frames and is a refusal of the whole hit.* In 1.7.10 `hurtResistantTime` was set **before** the event, so a covered
  hit began the frames and a source that hits every tick cost the pool once per ten ticks; here the same hit would be posted again on the
  next tick and the pool would be gone about ten times faster. The group keeps the original's frames itself, per player (a tick stamp,
  the strongest raw amount and what the game's `lastHurt` holds), charges the pool by the rule of the original inside and outside them, and
  hands the game `handed + rest` while the game's own frames run (`invulnerableTime > 10`) and `rest` alone when they are over, so the game
  deals exactly the rest whichever frames end first. Real lava is a GameTest (`soulHeartsAreChargedOncePerTenTicksByLava`), the
  frames that begin and end at different times another (`soulHeartsAgreeWithTheOriginalWhenTheGamesFramesBeginLater`).
- *A covered hit is silent* (no knockback, no hurt animation or sound from the game, no `lastHurtByMob`, an arrow bounces off); 1.7.10's
  cancel only skipped `damageEntity`. The group sends the damage event for a covered hit that begins the frames, so the hurt animation
  and sound remain; knockback of a covered hit is lost. **A hook that let a listener keep the hit alive with zero damage** (frames,
  knockback and animation, no armour wear, no health change) would remove this difference; not requested, the group lives with it.
- *A listener before the pool runs for the hits the pool's frames refuse.* The poster filters only the game's refusals
  (`TinkererEvents.framesRefuse`); the pool's frames refuse a repeat after a covered hit (the game has no frames then) and a weaker hit
  after a partly covered one, and those hits are posted. The Enchantments group's listener is earlier in the chain (the order of
  `ContentGroups.ALL`, and of 1.7.10), so for such a hit it heals the attacker (Vampirism) and advances the counter of Focused and
  Dispersed Strikes before the pool refuses the hit; in 1.7.10 the hit was refused before any event. **Request to the lead (optional):**
  let a listener register a refusal test that `framesRefuse` consults, with the raw amount, before the chain runs
  (`TinkererEvents.FRAMES_REFUSE.register((victim, source, amount) -> boolean)`, or the like); the Soul Hearts would register theirs
  (the player's frames running and `amount <= lastDamage`), which also gives back the raw `lastDamage` comparison of 1.7.10 where the
  group now compares the amount that earlier listeners left. The other way is for the Enchantments group to guard its side effects. The
  difference is written down in `docs/deviations/KamiWorld.md` section 5.
- *The game's `lastHurt` cannot be read from common code.* The pool tracks the last amount it handed back instead. A shield (the game
  sets `lastHurt` to 0 after the block) and a helmet against a falling block change it behind the pool's back, and the next stronger hit
  inside the game's frames is then dealt as another amount than the rest. **Request to the lead (optional):** a reader of the entity's
  `lastHurt` in `TinkererEvents` (the posters already read it for `framesRefuse`; Fabric has the `@Shadow`, NeoForge needs the accessor
  anyway); the group would read it instead of tracking it and the corner cases of section 5 would go.
- *No poster on NeoForge yet*: the Soul Hearts do not act there until the `LIVING_DAMAGE` handler of `HOOKS.md` section 3 exists.

## 1b. The drops of a death (the dimensional shards) - ANSWERED

The first version had no event for `LivingDropsEvent`, and added a loot pool to the two entity loot tables instead (`LootEvent.MODIFY_LOOT_TABLE`).
`TinkererEvents.LIVING_DROPS` (`docs/HOOKS.md` section 8; Fabric: `LivingEntityDropsMixin` and the capture mixins; NeoForge: the
`LivingDropsEvent` handler, **not written yet**) answers it, and `DimensionalShardDropHandler.onLivingDrops` is the 1.7.10 handler on it; the
loot-table pools are deleted. What the event gives: the victim, the source (so `getEntity() instanceof Player`), the list of item entities
before they are in the world, which a listener appends to. What it does not: nothing the group needed (the experience is not an item, which
the shard is). The one thing a test needs is a certain roll, so the chance is drawn from `DimensionalShardDropHandler.dice` (default
`Math::random`).

## 2. Loader modules (to the lead)

Nothing else of this group needs a loader hook:

- the HUD, the screens, the menu, the block entity renderer, the render type, the S2C payload, the commands, the chunk generator
  type and the player events (join, respawn, clone, quit, server stop) are Architectury; the damage of a hit (the Soul Hearts) and the
  drops of a death (the dimensional shards) are the add-on's own events of `docs/HOOKS.md` (`LIVING_DAMAGE`, `LIVING_DROPS`), which
  need a poster on each loader: Fabric has them, **NeoForge does not yet** (the group's effects do not happen there until it does);
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
