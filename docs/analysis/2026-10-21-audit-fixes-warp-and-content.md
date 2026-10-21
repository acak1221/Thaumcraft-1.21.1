# Warp, wand and base-content audit fixes — 2026-10-21

Branch `port/fixwarp`. Oracle: `/home/Acak/work/thaumcraft-4.2.3.5-oracle`
(`Thaumcraft-1.7.10-4.2.3.5.jar`). Every number below is the oracle value; the
1.7.10 blast resistance setter value is translated with the house rule of the port
(`setResistance(R)` stores `3R`, the vanilla getter divides the stored value by five,
so the **effective** resistance is `R*3/5`).

## Fixed

1. **Champion «grim» inflicts wither, not weakness.**
   `oracle thaumcraft/common/entities/monster/mods/ChampionModGrim.java:26-30` —
   `new PotionEffect(Potion.field_82731_v.field_76415_H, 200)`; `Potion.field_82731_v`
   is the wither (the port's own `WarpEvents:177` already reads the same field as
   `MobEffects.WITHER`). The port used `Status.WEAKNESS` for two hundred ticks.
   Now: `IChampionModifierEffect.Status.WITHER` and `ChampionRules.status` maps it
   to `MobEffects.WITHER`.

2. **The taint blocks were correct; the audit misread the oracle.** The audit took
   `BlockTaint:248-259` (`func_149712_f`) for a light level. In 1.7.10 `func_149712_f`
   is `getHardness` (float): the oracle's own `BlockCosmeticSolid:213` has the same
   method for the hardness 30/4/2 and a separate `getLightValue` for the light 9/4.
   The 1.12 reference agrees (`src/main/java/thaumcraft/common/blocks/BlockTaint.java:166`
   — `getBlockHardness` returns 1.75/1.5/0.2). The taint block emits **no** light, the
   crust keeps the hardness 1.75, the soil 1.5 and the flesh 0.2, and the blast
   resistance is the effective 6 of the setter value 10 — exactly what the port had
   (`docs/analysis/2026-10-20-taint-blocks.md`, `2026-10-19-golem-flesh.md`).
   The numbers now live in the pure `TaintBlockRules` and are pinned by a test.

3. **The focus potency reaches the damage again.** `oracle
   thaumcraft/common/items/wands/ItemWandCasting.java:262-267` — `getFocusPotency` is
   the potency upgrade **plus one** when the wand carries a staff rod with runes
   (`ConfigItems:325`: only `STAFF_ROD_PRIMAL` has them). The port had no
   `getFocusPotency` at all, so the primal staff lost its bonus in every focus
   (hellbat `damBonus`, fire, shock, frost, excavation, pech). Now: `Rod.hasRunes`,
   `WandMaterials.focusPotency` and `ItemWandCasting.focusPotency(ItemStack)`, used by
   the six foci.
   `oracle thaumcraft/common/entities/monster/EntityTaintSwarm.java:100` — the swarm
   attack damage is `2 + damBonus`; the port now states the same formula where the
   original sets the attribute (while the entity is created). The original never gives
   a swarm a bonus outside a save file and does not recalculate the attribute after the
   save is read, so the port keeps that quirk (its game test documents it).

4. **The effective blast resistance of the raw setter values.**
   `BlockMetalDevice:102` (17 → 10.2), `BlockWoodenDevice:83` (10 → 6.0),
   `BlockStoneDevice:94` (25 → 15.0), `BlockCosmeticOpaque:66` (5 → 3.0),
   `BlockCustomOre:60` (5 → 3.0), `BlockArcaneDoor:68` (999 → 599.4) and
   `BlockMagicBox:34-38` (the original sets **no** resistance at all, so the effective
   value is 0 — the port had an invented 12.5). The same rule was applied to the
   blocks of those families that still carried the raw value (the crucible, the alembic,
   the vis relay, the alchemy furnace, the node stabilizer, the node transducer, the
   focal manipulator), to `BlockEldritch:68` (20000 → 12000), to
   `BlockEldritchNothing:49` (6000000 → 3600000) and to `BlockMirror:67` (10 → 6.0).

5. **The warded glass is unbreakable and blast proof again.**
   `oracle thaumcraft/common/blocks/BlockCosmeticOpaque.java:253-266` — the metadata
   two has the hardness `Config.wardedStone ? -1.0f : 5.0f` and the blast resistance
   **999** (always). Minecraft 1.21.1 binds the hardness and the blast resistance to
   the block and not to the block state, so the warded glass became a block of its own
   (`cosmetic_opaque_warded`); the amber block and the amber brick stay the metadata
   zero and one of `cosmetic_opaque`. The unbreakable state is a mining progress of
   zero (`BlockBehaviour.getDestroyProgress`), because 1.21.1 binds the destroy speed
   to the block state. The arcane door gets the same unbreakable state
   (`BlockArcaneDoor:68`).

6. **The light of the jar family.** `oracle BlockJar:299-305` — only the node jar
   emits light, level 11; the ordinary and the void jar emit none. The port gave the
   ordinary jar a light of 9 and the node jar none.

7. **The brain jar keeps its experience.** `oracle BlockJar:182-193` — breaking the
   brain jar drops the stored experience as orbs; `oracle BlockJar:223-242` — a right
   click sets `eatDelay = 40` and hands a random share of at most 64 experience back.
   The port had neither.

8. **`IWandable` is live again.** `oracle
   thaumcraft/common/items/wands/ItemWandCasting.java:373-390` (`onItemUseFirst`) and
   `:492-509` (`onItemRightClick`) ask the clicked block and its block entity through
   the `IWandable` contract; the port declared the interface but never called it, so
   the node jar could never be turned back into a node. Now: the wand dispatches the
   contract, and `TileJarNode` implements it (`oracle TileJarNode:222-241`: the jar
   becomes a node again, with the stored aspects, the node type, the node modifier and
   the identity; the original never consumes the wand — the port did).

9. **`thaumcraft:triple_meat_treat` has a name.** The item was registered with a model
   but the language files only carried the 1.12 key `item.thaumcraft.triple_meat_treat.name`,
   which 1.21.1 does not read. Now `item.thaumcraft.triple_meat_treat` in both
   languages ("Triple Meat Treat" / "Мясное ассорти", the original names of the 1.12
   reference `src/main/resources/assets/thaumcraft/lang/ru_ru.lang:266`).
   The asset test now also walks the **item** models, so a new item without a name
   fails `BlockResourceCoverageTest` instead of showing a raw translation key.

## Not touched

The recipes, the maze and the remaining findings of the two audits (the node jar that
loses its aspects when it breaks, the jar step sound, the taint brain's eating radius
and the crucible's fluid handler) belong to other domains.
