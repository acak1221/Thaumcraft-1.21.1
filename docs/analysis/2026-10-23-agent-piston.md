# The piston of the port asks the question of the 1.7.10 original — 23.10.2026 (agent `piston`)

Input: the open item of the report of the `blockvis` agent
([2026-10-23-agent-blockvis.md](2026-10-23-agent-blockvis.md) §6.1): *"a variant that carries a tile is still refused
by the piston, and 1.7.10 pushed it"* — the two missing pieces were a mixin on `PistonBaseBlock#isPushable` and the
carrying of the block entity through `PistonMovingBlockEntity`.

Result in one line: **the gap is closed, but the premise of the task is corrected by the vanilla 1.7.10 source — the
1.7.10 piston decided per class and not per metadata, so the port now reproduces the original exactly: the families
whose 1.7.10 class was a plain `Block` are moved (the tile of the metadata that carried one is destroyed, as in
1.7.10), and the families whose 1.7.10 class was a `BlockContainer` are refused, for every metadata.**

---

## 1. The specification of the original (1.7.10)

### 1.1 The rule of the vanilla piston

The piston of 1.7.10 asks **one** question about a block, and the metadata is not part of it. Vanilla 1.7.10
`net.minecraft.block.BlockPistonBase#canPushBlock` (MCP-decompiled vanilla 1.7.10, `MCP-Decompiled/1.7.10-12`,
`src/minecraft/net/minecraft/block/BlockPistonBase.java:358`) ends with:

```java
return !(p_150080_0_ instanceof ITileEntityProvider);
```

`net.minecraft.block.ITileEntityProvider` is the interface of the vanilla 1.7.10
(`src/minecraft/net/minecraft/block/ITileEntityProvider.java`, package `net.minecraft.block`), and every
`BlockContainer` of the original implements it
(`src/minecraft/net/minecraft/block/BlockContainer.java:11` — `public abstract class BlockContainer extends Block
implements ITileEntityProvider`).

So the rule of the original is:

> **a block is moved by the piston iff the class of the block did not implement `ITileEntityProvider`** — a
> `BlockContainer` is never moved, for any of its metadata; a plain `Block` is moved for every metadata.

The move **destroys the tile** of the metadata that carried one: the 1.7.10
`net.minecraft.tileentity.TileEntityPiston` carries the block and the metadata only
(`TileEntityPiston.java`: the fields `storedBlock` and `storedMetadata`, the methods `getStoredBlockID()` and
`getBlockMetadata()`), and the block that arrives is put back with
`this.worldObj.setBlock(x, y, z, this.storedBlock, this.storedMetadata, 3)` (`TileEntityPiston#updateEntity` and
`TileEntityPiston#clearPistonTileEntity`). The 1.7.10 `World#setBlock` does not create a tile entity
(`World.java:350-402`), so the moved block arrives with **no** tile at all.

### 1.2 The families of Thaumcraft (the oracle)

The oracle is the 1.7.10 jar of the mod itself; the class of every family is in the oracle:

| Family of the original | oracle | 1.7.10 class | 1.7.10 `hasTileEntity(int)` | the piston of the original |
|---|---|---|---|---|
| `BlockCosmeticSolid` | `thaumcraft/common/blocks/BlockCosmeticSolid.java:60-61` | `Block` | `:278-285` — md 3 (the warding stone) and md 8 (the node) | **moved**, every md |
| `BlockEldritchNothing` | `thaumcraft/common/blocks/BlockEldritchNothing.java:42-43` | `Block` | `:81-83` — md 1 (the exposed filler) | **moved**, every md |
| `BlockEldritchPortal` | `thaumcraft/common/blocks/BlockEldritchPortal.java:38-39` | `Block` | `:119-121` — always | **moved**, every md |
| `BlockMagicalLog` | `thaumcraft/common/blocks/BlockMagicalLog.java:50-51` | `BlockRotatedPillar` | `:160-166` — md 2 (the silverwood node) | **moved**, every md |
| `BlockManaPod` | `thaumcraft/common/blocks/BlockManaPod.java:51-52` | `Block` | `:232-234` — always | **moved**, every md |
| `BlockCustomPlant` | `thaumcraft/common/blocks/BlockCustomPlant.java:55-56` | `BlockBush` | `:97-102` — md 4 (the ethereal bloom) | **moved**, every md |
| `BlockCosmeticOpaque` | `thaumcraft/common/blocks/BlockCosmeticOpaque.java:58-59` | `BlockContainer` | inherited — always (the tile is created for md 2 only, `:214-218`) | **never moved** |
| `BlockEldritch` | `thaumcraft/common/blocks/BlockEldritch.java:60-61` | `BlockContainer` | `:151-153` — md 0, 1, 3, 8, 9, 10 | **never moved** |
| `BlockAiry` | `thaumcraft/common/blocks/BlockAiry.java:77-78` | `BlockContainer` | inherited — always (the tile is created for md 0, 1, 5, `:313-326`) | **never moved** |

Every other family of Thaumcraft that carries a tile is a `BlockContainer` in 1.7.10
(`BlockAlchemyFurnace`, `BlockArcaneDoor`, `BlockArcaneFurnace`, `BlockChestHungry`, `BlockCrystal`,
`BlockEssentiaReservoir`, `BlockHole`, `BlockJar`, `BlockLifter`, `BlockMagicBox`, `BlockMetalDevice`,
`BlockMirror`, `BlockStoneDevice`, `BlockTable`, `BlockTube`, `BlockWarded`, `BlockWoodenDevice` — the list is the
`extends BlockContainer` list of the oracle) and was therefore **never moved** by the piston of the original.

### 1.3 The correction of the premise of the task

The task (and the `blockvis` report, and the evidence of the ledger of the wave 1) says: *"1.7.10 decided per
metadata: `hasTileEntity(int)` answered per variant, so a family that carried a tile for one variant was pushable for
the others"*, and it names *"the aura node, the wand pedestal, the infusion pillar, the arcane furnace"* as blocks
that the original pushed.

The vanilla 1.7.10 source does not say that:

* the question of the piston was **not** `hasTileEntity(int)`, it was `instanceof ITileEntityProvider`, so it was
  **per class**;
* the aura node of 1.7.10 is the md 8 of `BlockCosmeticSolid` (a plain `Block`) and the md 2 of `BlockMagicalLog`
  (a plain `BlockRotatedPillar`) — those two **were** pushed, and the move destroyed the tile of the node;
* the wand pedestal (`BlockWoodenDevice`, a `BlockContainer`), the infusion pillar (`BlockStoneDevice`, a
  `BlockContainer`) and the arcane furnace (`BlockArcaneFurnace`, a `BlockContainer`) were **never** pushed, in
  1.7.10, for any of their metadata.

The per-metadata rule of the *block entity* (`hasTileEntity(int)`) is real and is the answer of the original for
**the creation of the tile**; it was never the answer of the original for **the piston**. The port conflated the two
questions, and the wave 1 (`blockvis`) closed the second one in the wrong direction: the tile-less states of the
`BlockContainer` families became pushable, and they were not pushable in the original.

**The implementation below therefore reproduces the original exactly, and the evidence of the ledger of the four
units that carried the wrong claim is corrected.**

## 2. The mechanism of 1.21.1 (the real bytecode)

The remapped jar of the cache of Loom
(`~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/1.21.1-loom.mappings.1_21_1.layered+hash.40545-v2/minecraft-merged-1.21.1-loom.mappings.1_21_1.layered+hash.40545-v2.jar`),
decompiled with **CFR 0.152**:

* `net/minecraft/world/level/block/piston/PistonBaseBlock.java:269-306` — `public static boolean isPushable(BlockState,
  Level, BlockPos, Direction, boolean, Direction)`; the last line (`:305`) is `return !blockState.hasBlockEntity();`
* `net/minecraft/world/level/block/state/BlockBehaviour$BlockStateBase.java:552-554` — `hasBlockEntity()` is
  `this.getBlock() instanceof EntityBlock`, that is **per block and not per state**;
* `net/minecraft/world/level/block/piston/PistonStructureResolver.java:55` — the resolver asks
  `PistonBaseBlock.isPushable(...)` for the first block of the line, `:93` and `:110` for the sticky line and `:136`
  for every further block: **the resolver is what decides which blocks move and which are destroyed**;
* `net/minecraft/world/level/block/piston/PistonBaseBlock.java:336` — the destroy path takes the block entity of the
  destroyed block, `:352` — the move path writes a `PistonMovingBlockEntity` that carries **the state only**;
* `net/minecraft/world/level/block/piston/PistonMovingBlockEntity.java:79` — the field `private BlockState movedState`
  and nothing else; `:324-341` — `loadAdditional`/`saveAdditional` carry the state, the direction, the progress, the
  two flags; `:277-288` (`finalTick`) and `:290-322` (`tick`) put the moved state back with
  `level.setBlock(this.worldPosition, blockState, 3)`;
* `net/minecraft/world/level/chunk/LevelChunk.java:301-316` — the block entity of the block that arrives is created
  **empty** (`((EntityBlock) block).newBlockEntity(blockPos, blockState)`), the old one was removed at `:343-352`.

⇒ In 1.21.1 the piston refuses every block that is an `EntityBlock`, and a block that carries a tile in the port is
an `EntityBlock`, so it is refused. That is the gap.

## 3. The implementation

### 3.1 The contract (common, loader neutral)

* `common/src/main/java/thaumcraft/common/blocks/OriginalPistonPushable.java` — the contract:
  `boolean isPushableInOriginal(BlockState state)`, the answer of the original for a state.
* `common/src/main/java/thaumcraft/common/blocks/OriginalPistonRule.java` — the answer of the original for the
  families of Thaumcraft: the enum `Family` (one constant per family of 1.7.10, with the citation of the oracle in
  its javadoc) and the pure function `static boolean isPushableInOriginal(Family family, int metadata)`. The class
  builds no block and holds none, so the rule can be read and tested without a registry.
* The nine block classes of the port answer the contract: `BlockCosmeticSolid:184`, `BlockEldritchNothing:74`,
  `BlockEldritchPortal:54`, `BlockSilverwoodNode:49` (the md 2 of `BlockMagicalLog`), `BlockManaPod:56`,
  `BlockCustomPlant:71` (moved, like the original) and `BlockCosmeticOpaque:192`, `BlockEldritch:224`,
  `BlockAiry:202` (refused, like the original).

### 3.2 The mixin (common, one file, both loaders)

* `common/src/main/java/thaumcraft/common/mixin/PistonPushabilityMixin.java` — `@Mixin(PistonBaseBlock.class)` with

  ```java
  @Redirect(method = "isPushable", at = @At(value = "INVOKE",
          target = "Lnet/minecraft/world/level/block/state/BlockState;hasBlockEntity()Z"), require = 1)
  private static boolean thaumcraft$originalPushability(BlockState state) { ... }
  ```

  The redirect replaces the question of the vanilla code (`hasBlockEntity()`) by the answer of the original for the
  blocks of Thaumcraft, and by the vanilla answer for every other block. The vanilla code reads the value as
  `!hasBlockEntity()`, so the value that is returned is the negation of the answer of the original.
* The mixin is registered in `common/src/main/resources/thaumcraft-common.mixins.json` — the file that **both**
  loaders read (`fabric.mod.json` and `neoforge.mods.toml` point at it), so **no loader module needed a copy** and
  the tree of `fabric/src` and `neoforge/src` is untouched.
* The contract of the block entity (`PerStateBlockEntity`, the wave 1) is **not** changed: it still decides whether a
  block entity is created, and the two questions are apart now. The javadoc of the interface says so.

### 3.3 The block entity of a moved block

The NBT of the block entity is **not** carried through `PistonMovingBlockEntity`, and that is the answer of the
original, not an omission: the 1.7.10 `TileEntityPiston` carried the block and the metadata only, so the move
destroyed the tile. The port reproduces it: the tile of the source is removed when the moving piston is put there
(`LevelChunk.java:343-352`) and a **fresh, empty** tile is created at the destination
(`LevelChunk.java:301-316`). The one difference to 1.7.10 is that 1.7.10 left the moved block with **no** tile and
1.21.1 leaves it with an **empty** one — the content is lost in both.

⇒ the second half of the recipe of the `blockvis` report (a mixin on `PistonMovingBlockEntity` that carries the NBT
of the tile) would have been a **deviation** from the original. It is not done, and the report says why.

## 4. The verification of the target of the mixin

The target and its descriptor were read out of the **real 1.21.1 bytecode**, never guessed:

```
$ javap -c -p net/minecraft/world/level/block/piston/PistonBaseBlock.class
  public static boolean isPushable(net.minecraft.world.level.block.state.BlockState,
      net.minecraft.world.level.Level, net.minecraft.core.BlockPos, net.minecraft.core.Direction,
      boolean, net.minecraft.core.Direction);
...
  237: aload_0
  238: invokevirtual #505   // Method net/minecraft/world/level/block/state/BlockState.hasBlockEntity:()Z
  241: ifne          248
  244: iconst_1
  245: goto          249
  248: iconst_0
  249: ireturn
```

* the **method** `isPushable` is declared in `PistonBaseBlock` itself (the class of the mixin) — the safest possible
  kind of target, no inheritance and no remapping of the owner;
* the **instruction** that is redirected is the only call of `hasBlockEntity()` in the method (offset 238), and its
  owner in the constant pool is `net/minecraft/world/level/block/state/BlockState` — the string of the `@At` target is
  exactly that owner;
* `hasBlockEntity()` is declared in `BlockBehaviour$BlockStateBase` and *inherited* by `BlockState`; the owner of the
  instruction is nevertheless `BlockState`, because javac writes the static type of the receiver into the constant
  pool. The mixin of the project already uses that shape and runs: `BoreChunkMutationMixin.java:75` redirects
  `Lnet/minecraft/world/level/block/state/BlockState;onRemove(...)V`, and `onRemove` is declared in `BlockBehaviour`,
  not in `BlockState`. `MemberInfo#matches` of Mixin 0.8.7 compares the owner exactly
  (`sponge-mixin-0.16.4+mixin.0.8.7.jar`, `org/spongepowered/asm/mixin/injection/struct/MemberInfo#matches`), so the
  owner of the target string has to be the owner of the instruction — it is.

The redirect handler is `private static` because `isPushable` is static, and it takes the receiver of the redirected
call (the `BlockState`) as its first argument, which is the rule of Mixin for an instance call
(`RedirectInjector$RedirectedInvokeData#handlerArgs`).

**The risk of the mixin** is the ordinary one of a `@Redirect`: if the instruction were not where the target says, the
mixin would not apply and the game would stop at the start. It is limited here by `require = 1` (the mixin fails
loudly instead of silently) and by the three checks above. The mixin touches **nothing** but the question of the
piston: the other checks of `isPushable` (the world border, the height, obsidian, the destroy speed, the push
reaction) stay the vanilla ones, in the vanilla order.

## 5. The tests (authored, and **executed** outside Gradle)

`common/src/test/java/thaumcraft/blocks/PistonPushabilityTest.java` (JUnit 5, 11 tests) — the rule is read through the
pure function that the production code calls, and **no block is constructed** (a constructed block leaves an
unregistered intrusive holder in the registry and breaks the registry freeze of the whole suite):

* `theCosmeticStonesWereMovedByThePistonOfTheOriginal` — the sixteen metadata of `BlockCosmeticSolid`;
* `theEldritchFillerWasMovedByThePistonOfTheOriginal` — the two metadata of `BlockEldritchNothing`;
* `theEldritchGateWasMovedByThePistonOfTheOriginal` — the two metadata of `BlockEldritchPortal`;
* `theSilverwoodNodeWasMovedByThePistonOfTheOriginal` — the metadata two of `BlockMagicalLog`;
* `theManaPodWasMovedByThePistonOfTheOriginal` — the eight ages of `BlockManaPod`;
* `theEtherealBloomWasMovedByThePistonOfTheOriginal` — the metadata four of `BlockCustomPlant`;
* `theAmberBlocksWereNeverMovedByThePistonOfTheOriginal` — the three metadata of `BlockCosmeticOpaque`;
* `theEldritchStonesWereNeverMovedByThePistonOfTheOriginal` — the twelve metadata of `BlockEldritch`;
* `theAiryFamilyWasNeverMovedByThePistonOfTheOriginal` — the thirteen metadata of `BlockAiry`;
* `theBlocksThatDecideTheirBlockEntityPerStateAlsoAnswerThePistonOfTheOriginal` — by reflection (the classes are
  read with `Class.forName(name, false, loader)`, without being built): every block that answers the contract of the
  block entity answers the contract of the piston as well;
* `theMixinThatAsksTheContractIsRegistered` — the common mixin configuration names the new mixin.

**The run** (Gradle is not allowed in this wave, so the classes were built with `javac` against the real
`minecraft-merged-1.21.1` jar of the cache of Loom and the suite was started with the `junit-platform-launcher`):

```
[        11 tests found           ]
[         0 tests skipped         ]
[        11 tests started         ]
[         0 tests aborted         ]
[        11 tests successful      ]
[         0 tests failed          ]
```

`common/src/test/java/thaumcraft/blocks/BlockVisibilityAndPistonTest.java` (the suite of the wave 1) was **corrected**:
its test `theStatesWithoutATileAreTheOnesThePistonMoves` asserted the wave 1 claim, that the states without a tile
are the states that the piston moves. The **assertions** (fourteen cosmetic stones and five eldritch stones without a
tile) are about the block entity and they did not change; the test was renamed
(`theStatesWithoutATileAreTheOnesTheBlockEntityContractLeavesEmpty`) and its comment says that the question of the
piston is a question of its own. **That suite was not executed in this session** (it builds the blocks of the port and
needs the whole bootstrap of the loader), so the leader has to run it.

## 6. The units of the ledger

Nine units carry the new evidence; the `counts` were not touched and the shape of the file is preserved
(`json.dumps(indent=2)` reproduces the file byte for byte):

| unit | before | after | the change |
|---|---|---|---|
| `thaumcraft/common/blocks/BlockCosmeticSolid.java` | `parity_verified` | `parity_verified` | the piston moves the whole family now, the claim of the wave 1 is corrected |
| `thaumcraft/common/blocks/BlockEldritchNothing.java` | `parity_verified` | `parity_verified` | the piston moves the whole family now |
| `thaumcraft/common/blocks/BlockCosmeticOpaque.java` | `parity_verified` | `parity_verified` | the piston refuses the family now, the claim of the wave 1 is corrected |
| `thaumcraft/common/blocks/BlockEldritch.java` | `parity_verified` | `parity_verified` | the piston refuses the family now, the claim of the wave 1 is corrected |
| `thaumcraft/common/blocks/BlockAiry.java` | `parity_verified` | `parity_verified` | the piston refuses the family now |
| `thaumcraft/common/blocks/BlockEldritchPortal.java` | `parity_verified` | `parity_verified` | the piston moves the gate |
| `thaumcraft/common/blocks/BlockManaPod.java` | `parity_verified` | `parity_verified` | the piston moves the pod |
| `thaumcraft/common/blocks/BlockCustomPlant.java` | `parity_verified` | `parity_verified` | the piston moves the plant |
| `thaumcraft/common/blocks/BlockMagicalLog.java` | `parity_verified` | `parity_verified` | the piston moves the silverwood node |

No unit was moved to another coverage: the units were `parity_verified` and the piston was only one of their
aspects; the change corrects that aspect.

`python3 scripts/porting/update_scope_ledger.py --check --original-root
/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` → **`stale_review_fingerprints: 0`**.

## 7. What is still open

1. **The NBT of the tile is not carried by a move** — on purpose, it is the answer of the 1.7.10 original (§1.1). If
   the leader wants the tile to travel with the block (a deviation from the original, a nicer behaviour), the recipe
   is: a `@Redirect`/`@Inject` on `PistonMovingBlockEntity#saveAdditional` that writes the tile into the tag of the
   moving piston and a `loadAdditional` that reads it back, plus the creation of the tile at the destination in
   `LevelChunk#setBlockState` (the vanilla code creates an **empty** one, so the content would have to be written
   into it after the block is placed). This is a deviation and was not done.
2. **The tile of the ethereal bloom is missing in the port** (`BlockCustomPlant` carries no block entity at all,
   although the metadata four of the original carries a `TileEtherealBloom`, oracle
   `BlockCustomPlant.java:97-108`). It is the domain of the tiles and was not touched; the answer of the piston is
   right in the meantime (the original moved the plant, and the port moves it).
3. **The game was not run.** The mixin applies itself in the world and the piston really moves the block — that is
   the acceptance criterion of the leader: put a warding stone (the md 3 of the cosmetic solid), a silverwood node, a
   mana pod or an eldritch gate on a piston and power it — the block has to move, and the tile of the block that
   arrives has to be empty. Put an eldritch stone, an amber block or an airy block on a piston: it has to stay.
4. **The suite of the wave 1 was not executed here** (§5).

## 8. The files

Added (4): `common/src/main/java/thaumcraft/common/blocks/OriginalPistonPushable.java`,
`common/src/main/java/thaumcraft/common/blocks/OriginalPistonRule.java`,
`common/src/main/java/thaumcraft/common/mixin/PistonPushabilityMixin.java`,
`common/src/test/java/thaumcraft/blocks/PistonPushabilityTest.java`.

Changed (13): the nine block classes of §3.1, `common/src/main/java/thaumcraft/common/blocks/PerStateBlockEntity.java`
(the javadoc only, the contract is untouched), `common/src/main/resources/thaumcraft-common.mixins.json` (one line),
`common/src/test/java/thaumcraft/blocks/BlockVisibilityAndPistonTest.java` (the name and the comment of one test) and
`docs/porting/scope-ledger.json` (the evidence of the nine units of §6; `counts` untouched).
