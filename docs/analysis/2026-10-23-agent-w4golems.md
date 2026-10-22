# Agent wave 4 `w4golems` — the last `partial` units of the golemmancy (2026-10-23)

Branch: `agent/w4golems` (worktree `/home/cyberfox-kali/wt/w4golems`).
Domain: `golems_and_tasks` — the 11 units of `/home/cyberfox-kali/units/w4golems.txt`.

Sources used:

* the **oracle** `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (859 files, byte-exact, the authority);
* the **official Mojang 1.7.10 `client.jar`**, downloaded for this wave
  (`https://launcher.mojang.com/v1/objects/e80d9b3bf5085002218d4be59e668bac718abbc6/client.jar`),
  for the vanilla 1.7.10 `assets/minecraft/textures/particle/particles.png` and for the
  bytecode of `net.minecraft.server.management.ItemInWorldManager` (the 1.7.10
  `PlayerInteractionManager`, notch `mx`), decompiled with CFR 0.152;
* the 1.21.1 port `common/src/main/java/thaumcraft/**` and the 1.21.1 `minecraft-merged` jar
  of the leader's Gradle cache (the bytecode of `ServerPlayerGameMode`).

**No Gradle was run in this worktree.** Every test named below is *authored but not executed*.
The changed sources are **type-checked** with `javac` against the access-widened 1.21.1 jar of
the leader's Gradle cache (§6).

---

## 1. The bobber sprite — `RenderGolemBobber.java` (**closed**)

### 1.1 The original

Oracle `thaumcraft/client/renderers/entity/RenderGolemBobber.java:31,40-45`: the texture is the
**vanilla 1.7.10** `textures/particle/particles.png` and the quad reads the cell **(1,2)**: the
sheet fractions `f2 = 0.0625`, `f3 = 0.125`, `f4 = 0.125`, `f5 = 0.1875` of the 128x128 sheet —
the columns 8..15 and the rows 16..23.

### 1.2 The asset is now obtainable

The wave-3 report could not fetch the sheet because `launcherresources.mojang.com` does not
resolve here. It does not have to: the **1.7.10 `client.jar` still carries the assets**
(`assets/minecraft/textures/particle/particles.png`, 3 613 bytes, 128x128, RGBA). The version
manifest (`https://launchermeta.mojang.com/mc/game/version_manifest_v2.json` →
`https://piston-meta.mojang.com/v1/packages/ed5d8789ed29872ea2ef1c348302b0c55e3f3468/1.7.10.json`)
gives the client jar
`https://launcher.mojang.com/v1/objects/e80d9b3bf5085002218d4be59e668bac718abbc6/client.jar`
(5 256 245 bytes); the 1.7.10 asset index lists only the language files and the icons, so the
jar is the only place the sheet exists.

The cell was lifted out of that file and is shipped as
`common/src/main/resources/assets/thaumcraft/textures/entity/golem_bobber.png` (8x8, RGBA).

### 1.3 The change

* **new** `common/src/main/java/thaumcraft/client/renderers/entity/GolemBobberSprite.java` — the
  sheet constants of the original (128, 8, the cell 1/2, the four sheet fractions) and the
  mapping of a sheet fraction into the cell. It holds no Minecraft type, so a test can assert
  the numbers without a client.
* `RenderGolemBobber.java:35` — the texture is the port's own 8x8 asset instead of the vanilla
  fishing hook; `:48-53` — the four corners are the original's four corners of the cell.

The alpha of the cell is only `0` or `255`, so the original's `GL_BLEND` and the native
`RenderType.entityCutout` draw the same pixels.

---

## 2. The Use core — `AIUseItem.java` and `GuiGolem.java` (**closed**)

### 2.1 The original's rule

Oracle `thaumcraft/common/entities/ai/interact/AIUseItem.java`:

* `:135` — the fake player takes the sneaking flag of the third toggle;
* `:142-146` — when the marked cell is an empty space the click lands on the cell **opposite the
  marked side**;
* `:155-157` — the left click breaks the block, the right click uses the carried item;
* `:190` — the first toggle decides whether the marked cell must hold a **block** or an **empty
  space**;
* `:192` — the cell on the marked side must be air.

The three switches are the toggles 0, 1 and 2 of the golem; the original's menu flips the toggle
`button - 50` (oracle `ContainerGolem:92-94`) and draws them at `(42,40)`, `(42,50)`, `(42,60)`
with the captions at `(53,42)`, `(53,62)`, `(53,82)` (oracle `GuiGolem:206-236`).

### 2.2 What the native did

`GolemUsePlatform` was a **closed unit**: the target had to be a lever, a button or farmland, the
hand had to be empty or pristine wheat seeds, and `GolemUseOptions.supportsDefaultBaseline()`
refused every combination but the default. The goal therefore had nothing to switch, and the menu
drew no switch (a switch that changes nothing would be a working-looking fake).

### 2.3 The change

* `GolemUseOptions` — `supportsDefaultBaseline()` is replaced by `isSupported()` (every
  combination of the three switches is an ordinary Use mode) and by the pure rule
  `acceptsTarget(boolean air)`.
* `UseGolemGoals` — the goal uses **any** carried item on **any** marked cell; the rule of the
  marked cell is `GolemUseOptions.acceptsTarget`, and the cell on the marked side must be air.
  The click cell is `GolemMarker.clickPos(emptySpace)` (the marked cell, or the cell behind it).
* `GolemUsePlatform` — the target rule is the switch, the hand is any item, the actor takes the
  sneaking flag, and the **left click** is `ServerPlayerGameMode.destroyBlock` (the native
  equivalent of the 1.7.10 `onLeftClickBlock`, which has no sneaking gate — read from the
  official 1.7.10 `client.jar`, notch `mx`, the method `a(int,int,int,int)`).
* `EntityGolemBase` — `toggleUseOption(int)` is the server side of the original's
  `ContainerGolem:92-94`; `useItemAllowed` accepts any item (the whitelist ghosts still decide
  *which* item leaves the home).
* `ContainerGolem` — the three switches are synced menu data (indices 21..23) and the menu
  buttons 50, 51 and 52 flip them, exactly as the original's shared toggle array does (the
  Fill mode uses 50 and the Guard uses 51..54, so the dispatch is per core, like the original).
* `GuiGolem` — the three switches and the three captions are drawn at the original positions;
  the "not yet ported" blurb of the Use core is gone.

### 2.4 The deliberate deviations that remain

The port keeps its own design, and the row records it: **one** native call per job (the original
clicks on every tick of its 200-tick budget), **no block entity** as a target (the original's
fake player would open a chest), and a **real server actor** instead of the 1.7.10 `FakePlayer`,
so the protection of the world applies to the golem.

---

## 3. The marked essentia transports — `AIEssentiaEmpty.java` and `AIEssentiaGoto.java` (**closed**)

### 3.1 The original

Oracle `AIEssentiaEmpty:64-112` pays a marked `TileJarFillable` with `addToContainer`, a marked
`TileEssentiaReservoir` with `addEssentia` on its own facing and **any other** marked
`IEssentiaTransport` with `addEssentia` on the **first marked side** that accepts the aspect.
`GolemHelper:245-248` selects that third kind as a destination; the "others" are always
candidates, next to the jars.

### 3.2 The change

* `AlchemyGolemGoals` — the destination set admits **every** marked essentia transport (the jar
  and the reservoir keep their own branches and their own rules), the marked side that the
  payment uses is the first one the transport accepts, and the space of a generic transport is
  the cargo itself, because the original hands the whole cargo to the transport and keeps what
  it took.
* `EssentiaTransfers.deliverTransport` — the new **closed** payment profile: the original's
  single `addEssentia` call on the marked side, paired with the cargo debit in the same native
  commit. The existing closed profiles (the jar, the reservoir) are untouched.
* `EntityGolemBase.deliverAlchemyEssentia` dispatches to the jar, the reservoir or the generic
  transport.

The route (`AIEssentiaGoto`) uses the same destination set, so it is closed with it.

### 3.3 What is still open

`AIEssentiaGather` — the **source** set: the original accepts **any** `IEssentiaTransport` at the
home (oracle `AIEssentiaGather:68-77`), the native still gathers out of a marked jar, a marked
reservoir or an alembic of the column. It needs the matching closed payment profile for
`takeEssentia` on a generic transport, in the same shape as `deliverTransport`.

---

## 4. The fluid set — the three liquid units (**still open**)

### 4.1 The decisive oracle reading

The original's crucible is **water only**: oracle `TileCrucible` declares
`public FluidTank tank = new FluidTank(FluidRegistry.WATER, 0, 1000)`, `fill` returns `0` for
every fluid but water and `canFill` answers only for water. The native's water-only crucible is
therefore **faithful**, and the observable fluid set of a crucible home is `{water}` in both.

The real remaining differences are the *rule* and the *extension point*:

| the original | the native |
|---|---|
| the home is **any** `IFluidHandler` (`GolemHelper:341-369`) | the home is the attached `TileCrucible` |
| the source is **any** marked `IFluidHandler` of the wanted fluid (`GolemHelper:420-429`) or a marked fluid block of it | the source is a marked source water block |
| the filter is **any** filled container (`FluidContainerRegistry.getFluidForFilledItem`) | the filter is a pristine water bucket |
| the cargo is a `FluidStack` (a fluid **and** an amount) | the cargo is a water volume in mB |

### 4.2 The blocker

The 1.21.1 **common** module has no loader-independent fluid-handler API, so the wanted fluid
(the home's accepted set ∩ the filter's fluid), the source set, the destination set and the
cargo would all have to become fluid-generic at once, over the whole Decanting subsystem
(`GolemWaterCargo`, the entity data `WATER_MB`, the placer payload, the menu ghost, the
`TileCrucible` tank and the GameTest suite). This wave could not do that **and** verify it (no
Gradle, no GameTest, no client), and a half-widened Decanting golem that could gather lava into
a water-only cargo would be worse than an honest `partial`.

### 4.3 The design that the next wave needs

The design of the wave-3 report is right, and the ledger's blocker is unchanged. Two additions
from this wave's reading of the oracle:

* the **filter** selects the fluid, exactly as the original does
  (`GolemHelper:351-364`: the missing set is the fluids the home can take, and the golem's
  inventory must hold a container of that fluid) — so the filter must accept any filled
  container, not only a water bucket;
* the **cargo** must become a `(fluid, amount)` pair, because the original's cargo is a
  `FluidStack` and the home decides which fluid it can ever be.

The registry of the wave-3 report stays the right shape
(`common/src/main/java/thaumcraft/common/lib/fluid/`): a small port-side interface that the two
loaders implement (the NeoForge `IFluidHandler` capability and the Fabric `FluidStorage.SIDED`
view), the common side registering the port's own containers.

---

## 5. The ledger

| unit | before | after |
|---|---|---|
| `client/renderers/entity/RenderGolemBobber.java` | partial | **parity_verified** |
| `common/entities/ai/interact/AIUseItem.java` | partial | **parity_verified** |
| `client/gui/GuiGolem.java` | partial | **parity_verified** |
| `common/entities/ai/fluid/AIEssentiaEmpty.java` | partial | **parity_verified** |
| `common/entities/ai/fluid/AIEssentiaGoto.java` | partial | **parity_verified** |
| `common/entities/ai/fluid/AIEssentiaGather.java` | partial | partial (the source set) |
| `common/entities/ai/fluid/AILiquidEmpty.java` | partial | partial (the fluid set) |
| `common/entities/ai/fluid/AILiquidGather.java` | partial | partial (the fluid set) |
| `common/entities/ai/fluid/AILiquidGoto.java` | partial | partial (the fluid set) |
| `common/entities/golems/EntityGolemBase.java` | partial | partial (the fluid set) |
| `common/entities/golems/GolemHelper.java` | partial | partial (the fluid half) |

**Five of the eleven units moved from `partial` to `parity_verified`.** The six that stay carry
the exact remaining difference in their note. `counts` was not touched and

```
python3 scripts/porting/update_scope_ledger.py --check --original-root \
    /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle
```

reports `"stale_review_fingerprints": 0`.

---

## 6. The `javac` proof

```
$ /tmp/opencode/check4.sh
MAIN OK
TEST OK
```

The classpath is the access-widened `minecraft-merged-1.21.1-loom…jar` of the leader's Gradle
cache, the remapped `architectury` and `fabric-api` artifacts of the leader's project, the
Minecraft libraries of the Gradle module cache and the JUnit 5 jars. The whole
`common/src/main/java` tree and the whole `common/src/test/java` tree compile with **zero
errors**. No Gradle daemon was started.

---

## 7. The tests that were authored (NOT executed)

* **new** `common/src/test/java/thaumcraft/golems/GolemBobberSpriteTest.java` — three tests: the
  four sheet fractions of the original, the corners of the cell, and the **exact 64 pixels** of
  the shipped asset against the cell of the vanilla sheet (a small PNG reader, no library).
* **new** `common/src/test/java/thaumcraft/golems/GolemUseRulesTest.java` — three tests: the
  "Block or Empty space" rule, the click cell of the empty space mode, and the independence of
  the three switches.
* **changed** `common/src/test/java/thaumcraft/golems/GolemUseOptionsTest.java` — the eight
  combinations are ordinary Use modes.

**No test constructs a `Block`.** The only Minecraft types the new tests touch are
`BlockPos`, `Direction` and `ResourceLocation`.

---

## 8. The files that changed

```
common/src/main/java/thaumcraft/client/renderers/entity/GolemBobberSprite.java              (new)
common/src/main/java/thaumcraft/client/renderers/entity/RenderGolemBobber.java              (the sprite)
common/src/main/resources/assets/thaumcraft/textures/entity/golem_bobber.png                 (new)
common/src/main/java/thaumcraft/client/gui/GuiGolem.java                                    (the three switches)
common/src/main/resources/assets/thaumcraft/lang/en_us.json                                 (the captions)
common/src/main/java/thaumcraft/common/container/ContainerGolem.java                         (the menu state)
common/src/main/java/thaumcraft/common/entities/golems/EntityGolemBase.java                 (the toggle, the delivery)
common/src/main/java/thaumcraft/common/entities/golems/GolemUsePlatform.java                (the use rules)
common/src/main/java/thaumcraft/common/entities/golems/UseGolemGoals.java                   (the use rules)
common/src/main/java/thaumcraft/common/entities/golems/AlchemyGolemGoals.java               (the destination set)
common/src/main/java/thaumcraft/common/items/golems/GolemMarker.java                        (the click cell)
common/src/main/java/thaumcraft/common/items/golems/GolemUseOptions.java                    (the switches)
common/src/main/java/thaumcraft/common/items/golems/ItemGolemPlacer.java                    (the placer)
common/src/main/java/thaumcraft/common/lib/essentia/EssentiaTransfers.java                  (the payment profile)
common/src/test/java/thaumcraft/golems/GolemBobberSpriteTest.java                           (new, authored)
common/src/test/java/thaumcraft/golems/GolemUseRulesTest.java                               (new, authored)
common/src/test/java/thaumcraft/golems/GolemUseOptionsTest.java                             (changed, authored)
docs/porting/scope-ledger.json                                                              (11 rows)
docs/analysis/2026-10-23-agent-w4golems.md                                                  (this report)
```

---

## 9. What could not be verified in this environment

* **No build, no test run, no GameTest, no client.** The wave was told not to run Gradle. The
  type check is not a build: the resources, the mixins and the runtime behaviour were not checked
  at all. The leader has to run `:common:test`, `:fabric:runGameTest` and the client.
* **The client frames.** The three switches of the Use core and the bobber sprite are visible;
  no acceptance criterion is claimed beyond the original's positions.
* The two GameTest scenarios of the golem (`ordercoloursroutemarkersandslotsandthebellcyclesthem`
  and `entropyswitcheschangewhatrealgolemsfetchemptyandsort`) use the Use core? The Use core is
  not part of them, but the Alchemy change touches the Alchemy golem's payment path and must be
  covered by the leader's suite.
