# The test suite of the 1.21.1 port — the wave of 2026-10-23 (agent `testsuite`)

**Worktree**: `/home/cyberfox-kali/wt/testsuite` — **branch**: `agent/testsuite`

**Result**: `:common:test` is green.
**2015 tests, 0 failures, 0 errors, 0 skipped** (298 test classes).

The run is repeatable and **order independent**: the class order of the suite is random
with a fixed seed (`common/src/test/resources/junit-platform.properties`), and the suite was
run green with the seeds `20261023` and `987654321`. A third seed (`4242`) was not
completed: the machine ran out of memory during that run (three other agents were building
in their own worktrees at the same time and the kernel killed the Gradle daemon, see
`dmesg`: `oom-kill … task=java`), so it proves nothing either way.

```
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
cd /home/cyberfox-kali/wt/testsuite
./gradlew --console=plain :common:test
```

---

## 1. The state the wave started from

The brief reported 2013 tests and 14 failures. The real causes were **not** the ones the
brief guessed: **one** root cause explains 10 of the 14 failures, and it was not the
`BaseContentParityTest` of the brief but the test registry of the block visibility test.
The other four failures have their own cause.

| # | Failing test (as reported) | Reported cause | **Real cause** |
|---|---|---|---|
| 1 | `BaseContentParityTest.thePlantItemRefusesThePlacementTheOriginalRefuses` | "Registry is already frozen" | the test built a `BlockCustomPlant` — a block with an intrusive holder |
| 2 | `BaseContentParityTest.theCosmeticStairsKeepTheOriginalLightOpacity` | "Registry is already frozen" | the test built a `BlockCosmeticStairs` — a block with an intrusive holder |
| 3 | `ApiSurfaceTest.initializationError` | "Some intrusive holders were not registered" | **the trigger**: the 24 blocks of `BlockVisibilityTestRegistry` were built and never registered |
| 4–8 | `BoreInventoryPlanTest` (5) | "Cannot freeze complete stack components" | collateral: the data component type registry was never frozen |
| 9 | `ChampionRulesTest.originalWardedChampionTakesHalfOfItsTotalMaxHealthAsItsWard` | unbound `minecraft:attribute` | collateral: the attribute registry was never frozen |
| 10 | `AdvancedDeviceParityTest.theFluxScrubberRemovesTheFluxGooAndTheFluxGas` | "check whether it is collateral" | **its own cause**: the test built a flux goo and a flux gas |
| 11–12 | `AspectBonusParityTest` (2 of 3) | "Registry minecraft:enchantment not found" | **its own cause**: the enchantments are a dynamic registry |
| 13 | `AspectBonusParityTest.theBowThePickaxeAndTheArmourKeepTheOriginalBonuses` | unbound `minecraft:armor_material` | collateral: the armor material registry was never frozen |
| 14 | `InventoryUtilsParityTest.theDamageWildcardOfTheOriginalIsKept` | "expected: false but was: true" | **a real assertion**: the expectation of the test contradicts the oracle, the production code is right |

The leader's commit `8d2e2126` ("fix(tests): keep the base content parity test from
breaking the registry freeze of the whole suite") fixed failures 1 and 2 only. It was
cherry-picked into this branch as `905d81ca`; the remaining 12 failures are the ones
described here.

---

## 2. The root cause: an intrusive holder that is never registered

### 2.1 The mechanism (Minecraft 1.21.1, decompiled from `minecraft-merged-mojang.jar`)

The line numbers are the ones of the stack traces of the failing run (and of the CFR
decompilation of the jar, which is the same class file).

* `net/minecraft/world/level/block/Block`:81 — the constructor of a block asks the **block
  registry** for an **intrusive holder**:
  `this.builtInRegistryHolder = BuiltInRegistries.BLOCK.createIntrusiveHolder(this);`
* `net/minecraft/core/MappedRegistry`:371 (`createIntrusiveHolder`) — the holder is put
  into the map `unregisteredIntrusiveHolders` and the registry refuses to work when it is
  already frozen (`validateWrite`, `MappedRegistry`:116 → *"Registry is already frozen"*).
* `net/minecraft/core/MappedRegistry`:359 (`freeze`) — the **freeze** of the registry
  refuses to finish while such a holder is still there:
  `throw new IllegalStateException("Some intrusive holders were not registered: " + ...)`.
* `net/minecraft/core/MappedRegistry`:337 (`freeze`) — the freeze is also the place where
  the values are bound: `this.byValue.forEach((v, h) -> h.bindValue(v));`. A registry that
  is never frozen therefore keeps **unbound** holders, and its `freeze` would have
  complained with *"Unbound values in registry …"* (`MappedRegistry`:340).
* `net/minecraft/server/Bootstrap`:65 — `bootStrap()` sets `isBootstrapped = true`
  **before** it freezes the registries. A freeze that throws therefore leaves the flag set:
  every later `Bootstrap.bootStrap()` returns at once and **no registry is ever frozen
  again** in that JVM.
* `net/minecraft/core/registries/BuiltInRegistries`:248 (`bootStrap`) and :264 (`freeze`) —
  `bootStrap()` calls `createContents()` and then `freeze()`, and `freeze()` freezes the
  registries **in the order of their registration**: the block registry
  (`BuiltInRegistries`:268) comes **before** the attribute registry (`:282`), the armor
  material registry (`:331`) and the data component type registry (`:332`).
* The same rule holds for an item: `net/minecraft/world/item/Item`:88 asks the **item**
  registry for its intrusive holder.

So one block that is built and never registered does two kinds of damage:

1. the freeze of the block registry throws, and
2. every registry behind the block registry stays unfrozen, so every holder of it stays
   unbound and every later test that reads one fails with
   *"Trying to access unbound value '…'"*.

### 2.2 The trigger: `BlockVisibilityTestRegistry`

`common/src/test/java/thaumcraft/blocks/BlockVisibilityTestRegistry.java` is a
`LauncherSessionListener`: it runs when the JUnit session opens, before the first test. It
built **24** blocks of the port (3 cosmetic solid, 6 eldritch, 4 airy, 2 cosmetic opaque,
the eldritch nothing, the eldritch gate, the mirror, 3 device families, the magic box, the
arcane door and the arcane pressure plate) and **never registered any of them**.

The proof is in the failure message of the very first test class that boots the registry:

```
java.lang.IllegalStateException: Some intrusive holders were not registered:
[Reference{null=Block{[unregistered]}}, … 24 entries …]
	at net.minecraft.core.MappedRegistry.freeze(MappedRegistry.java:359)
	at net.minecraft.core.registries.BuiltInRegistries.freeze(BuiltInRegistries.java:264)
	at net.minecraft.core.registries.BuiltInRegistries.bootStrap(BuiltInRegistries.java:248)
	at net.minecraft.server.Bootstrap.bootStrap(Bootstrap.java:65)
	at thaumcraft.api.ApiSurfaceTest.bootstrap(ApiSurfaceTest.java:23)
```

The 24 entries are exactly the 24 blocks of the registry. The eldritch family registry
(`thaumcraft.world.EldritchTestRegistry`:57-60) does register its six blocks and was never
a problem — that is the model the fix follows.

### 2.3 The fix

`common/src/test/java/thaumcraft/blocks/BlockVisibilityTestRegistry.java` now registers
every block of the test under a test only id of the block registry
(`thaumcraft_test:<path>`), before the freeze:

```java
private static <T extends Block> T register(String path, T block) {
    ResourceLocation key = ResourceLocation.fromNamespaceAndPath(TEST_NAMESPACE, path);
    if (!BuiltInRegistries.BLOCK.containsKey(key)) {
        Registry.register(BuiltInRegistries.BLOCK, key, block);
    }
    return block;
}
```

`MappedRegistry.register` removes the value from `unregisteredIntrusiveHolders` and binds
the key of the holder (`MappedRegistry`:158-163 of the CFR decompilation), so the freeze of
the block registry succeeds and the registries behind it are frozen as well.

**Commit**: `fcc900e5 fix(tests): register the blocks of the visibility test so the block registry can freeze`

### 2.4 The collateral damage that disappeared with it

* `ApiSurfaceTest` — the class had **one** `initializationError` instead of its three
  tests; the class now runs all three. This is why the number of tests went from **2013**
  to **2015** (three real tests replace the one error).
* `BoreInventoryPlanTest` (5) — `DataComponentPatch$PatchKey` needs the bound value of
  `minecraft:data_component_type`; the registry was never frozen →
  *"Trying to access unbound value 'ResourceKey[minecraft:data_component_type / minecraft:custom_name]'"*
  → *"Cannot freeze complete stack components"*.
* `ChampionRulesTest` — the attribute registry was never frozen →
  *"Trying to access unbound value 'ResourceKey[minecraft:attribute / minecraft:generic.max_health]'"*.
* `AspectBonusParityTest.theBowThePickaxeAndTheArmourKeepTheOriginalBonuses` — the armor
  material registry was never frozen →
  *"Trying to access unbound value 'ResourceKey[minecraft:armor_material / minecraft:iron]'"*.

No production code was touched for these four: they were the symptom of the trigger, not
a defect of the port.

---

## 3. The failures with their own cause

### 3.1 `BaseContentParityTest` (2) — the two blocks of the test

Already fixed by the leader's commit `8d2e2126` (cherry-picked here as `905d81ca`): the
two rules are now pure static functions of the production code and the test asserts them
without building a block.

* `common/src/main/java/thaumcraft/common/blocks/BlockCustomPlant.java:48`
  `public static boolean soilSupports(boolean cinderpearl, BlockState soil)` — the plant
  type of the original is `EnumPlantType.Desert` for the metadata three
  (oracle `thaumcraft/common/blocks/BlockCustomPlantItem.java:93-96`), the plains type
  otherwise.
* `common/src/main/java/thaumcraft/common/blocks/BlockCosmeticStairs.java:52`
  `public static int lightOpacity(boolean waterlogged, int vanillaOpacity)` — the original
  sets the light opacity of the cosmetic stairs to zero
  (oracle `thaumcraft/common/blocks/BlockCosmeticStairs.java:32`, `this.func_149713_g(0);`).

### 3.2 `AdvancedDeviceParityTest.theFluxScrubberRemovesTheFluxGooAndTheFluxGas`

**Cause**: the test built a flux goo and a flux gas to ask the rule of the scrubber. After
the freeze of the registry a block cannot be built at all
(`MappedRegistry`:116 → *"Registry is already frozen"*), and **before** the freeze a built
block leaves an unregistered intrusive holder behind — the test was an order dependency of
the whole suite, not only a failure of its own.

**Oracle**: the original scrubber asks for the **material** of the block
(`thaumcraft/common/tiles/TileFluxScrubber.java:67-70`):

```java
boolean isFlux(int x, int y, int z) {
    Material mat = this.field_145850_b.func_147439_a(x, y, z).func_149688_o();
    return mat == Config.fluxGoomaterial;
}
```

and the two flux blocks are the two that carry that material:

* `thaumcraft/common/blocks/BlockFluxGoo.java:49` — `super(ConfigBlocks.FLUXGOO, Config.fluxGoomaterial);`
* `thaumcraft/common/blocks/BlockFluxGas.java:46` — `super(ConfigBlocks.FLUXGAS, Config.fluxGoomaterial);`

**Fix**: the rule is now asked for the **class** of a block, so the test asserts the same
thing without a block and without a world
(`common/src/main/java/thaumcraft/common/lib/tiles/TileParityRules.java:272-289`):

```java
public static boolean fluxScrubberRemoves(net.minecraft.world.level.block.state.BlockState state) {
    return state != null && fluxScrubberRemovesBlock(state.getBlock().getClass());
}

public static boolean fluxScrubberRemovesBlock(Class<?> block) {
    return block != null && FluxFiniteBlock.class.isAssignableFrom(block);
}
```

`BlockFluxGoo` and `BlockFluxGas` are the two subclasses of `FluxFiniteBlock`
(`common/src/main/java/thaumcraft/common/blocks/BlockFluxGoo.java:37`,
`common/src/main/java/thaumcraft/common/blocks/BlockFluxGas.java:19`), so the material
test of the original is the whole flux family.

**Commit**: `95d7c5a8 fix(tiles): the flux scrubber rule is asked for the class of a block`

### 3.3 `AspectBonusParityTest` (2) — the dynamic enchantment registry

**Cause**: the enchantments are a **dynamic** registry in 1.21.1. They are not a member of
the static registry of the vanilla bootstrap, so the lookup of the lookup of a unit test
throws *"Registry minecraft:enchantment not found"*. The leader's commit tried
`RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).lookupOrThrow(Registries.ENCHANTMENT)`
— that lookup can never work in a unit test.

The production code asks the **holder** of the enchantment for its key
(`common/src/main/java/thaumcraft/common/lib/research/ObjectAspectResolver.java:163`):

```java
String id = entry.getKey().unwrapKey().map(key -> key.location().toString()).orElse("");
```

so the test needs a holder that carries a **key**.

**Fix**: the test builds a stand alone holder
(`Holder.Reference.createStandAlone`, Minecraft 1.21.1
`net/minecraft/core/Holder$Reference`: the key is the second argument, the value stays
null). `HolderOwner` has no abstract method in 1.21.1 (only the default
`canSerializeIn`), so it is an anonymous class and not a lambda — the port already uses
that form in `common/src/gameTest/java/thaumcraft/common/testing/LootBagRuntimeChecks.java:144`.

```java
Holder<Enchantment> holder = Holder.Reference.createStandAlone(new HolderOwner<Enchantment>() {}, key);
ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
mutable.set(holder, level);
stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
```

**Commit**: `e17ac98d fix(tests): the enchantment of the aspect bonus test is a stand alone holder`

### 3.4 `InventoryUtilsParityTest.theDamageWildcardOfTheOriginalIsKept` — the verdict

**The production code is right, the expectation of the test was wrong.**

The oracle, `thaumcraft/common/lib/utils/InventoryUtils.java:333-339` (the rule of
`areItemStacksEqual`; the identical rule of `areItemStacksEqualForCrafting` is at
`:303-309`):

```java
boolean bl = t2 = stack0.func_77960_j() != stack1.func_77960_j();
if (ignoreDamage && stack0.func_77984_f() && stack1.func_77984_f()) {
    t2 = false;
}
if (t2 && ignoreDamage && (stack0.func_77960_j() == Short.MAX_VALUE || stack1.func_77960_j() == Short.MAX_VALUE)) {
    t2 = false;
}
```

* `func_77960_j` is `ItemStack.getItemDamage()`, the metadata of 1.7.10
  (`OpenTC4/src/main/methods.csv`).
* `func_77984_f` is `ItemStack.isItemStackDamageable()`
  (`OpenTC4/src/main/methods.csv:2161`).
* `Short.MAX_VALUE` is 32767, the "any metadata" wildcard of the original.

The **wildcard rule does not ask for two damageable stacks**: it needs only that the
caller ignores the damage and that one of the two metadata values is the wildcard. The
port keeps that rule verbatim in
`common/src/main/java/thaumcraft/common/lib/utils/InventoryUtils.java:406-410`:

```java
static boolean damageMatches(int damage0, int damage1, boolean ignoreDamage, boolean damageable0, boolean damageable1) {
    boolean different = damage0 != damage1;
    if (ignoreDamage && damageable0 && damageable1) different = false;
    if (different && ignoreDamage && (damage0 == ANY_DAMAGE || damage1 == ANY_DAMAGE)) different = false;
    return !different;
}
```

The failing assertion of the test was

```java
assertFalse(InventoryUtils.damageMatches(0, 32767, true, true, false),
        "A damageable stack and an ordinary one are never equalised by the wildcard alone.");
```

which contradicts the oracle: with `ignoreDamage = true` and one of the two damage values
being the wildcard, the original sets `t2 = false` and the two stacks are equal, **even
when only one of them is damageable**. The expectation was corrected to `assertTrue` and
the comment now quotes the oracle.

The wrong oracle line numbers of the javadoc of the production code
(`InventoryUtils.java:330-337` and `:366-373`, and `:339` for the stack size) were
repaired to the real ones (`:333-339`, `:303-309` and `:310`), and the class javadoc that
still claimed the wildcard "has no native form" was corrected — the port keeps it.

The other two oracle citations of the same test were checked and repaired as well:
`ThaumcraftApiHelper.areItemStackTagsEqualForCrafting` lives in
`thaumcraft/api/ThaumcraftApiHelper.java:128-146` (called from
`thaumcraft/common/lib/utils/InventoryUtils.java:301`), not in `InventoryUtils.java:327`.
The rule of the port (`InventoryUtils.java:357-371`) is the rule of the original.

**Commit**: `d69dafee test(utils): the damage wildcard of the original equalises a pair with one damageable stack`

---

## 4. The order independence of the suite

The suite runs in **one** JVM, so a single unregistered intrusive holder or a single
frozen registry breaks every later test of the suite. Two measures keep that from
happening again:

1. **No test builds a block or an item.** A grep over `common/src/test/java` for
   `new Block…(` and `new Item…(` finds only the constructions that are registered
   (`EldritchTestRegistry`:55/:64) or that were replaced by a pure rule
   (`BlockCustomPlant.soilSupports`, `BlockCosmeticStairs.lightOpacity`,
   `TileParityRules.fluxScrubberRemovesBlock`). The classes of 1.21.1 that create an
   intrusive holder are `Block` (`Block`:81), `Item` (`Item`:88), `Fluid`,
   `BlockEntityType` and `EntityType`; no test builds any of the last three.
2. **The class order of the suite is random with a fixed seed**
   (`common/src/test/resources/junit-platform.properties`):

   ```properties
   junit.jupiter.testclass.order.default=org.junit.jupiter.api.ClassOrderer$Random
   junit.jupiter.execution.order.random.seed=20261023
   ```

   The suite was run green with the seeds `20261023` and `987654321`
   (2015 tests, 0 failures each time). A failure stays reproducible because the seed is
   fixed.

The seed in the file is `20261023`; the other seed was used for a verification run only.

---

## 5. The files that were changed

| File | What |
|---|---|
| `common/src/test/java/thaumcraft/blocks/BlockVisibilityTestRegistry.java` | registers the 24 blocks of the test under a test only id |
| `common/src/main/java/thaumcraft/common/lib/tiles/TileParityRules.java` | `fluxScrubberRemovesBlock(Class<?>)`, the rule of the scrubber without a block |
| `common/src/test/java/thaumcraft/tiles/AdvancedDeviceParityTest.java` | asks the new rule, builds no block |
| `common/src/test/java/thaumcraft/crafting/AspectBonusParityTest.java` | the stand alone holder of the enchantment |
| `common/src/main/java/thaumcraft/common/lib/utils/InventoryUtils.java` | the corrected oracle citations of the javadoc (no behaviour change) |
| `common/src/test/java/thaumcraft/common/lib/utils/InventoryUtilsParityTest.java` | the corrected expectation of the wildcard rule |
| `common/src/test/resources/junit-platform.properties` | the random class order of the suite |
| `common/src/main/java/thaumcraft/common/blocks/BlockCustomPlant.java`, `…/BlockCosmeticStairs.java`, `common/src/test/java/thaumcraft/common/blocks/BaseContentParityTest.java` | the leader's commit `8d2e2126`, cherry-picked as `905d81ca` |

**No production behaviour was changed by this wave.** The only production change is the new
pure rule `TileParityRules.fluxScrubberRemovesBlock`, which is the rule of the original
expressed on the class of a block; the rule that the scrubber uses at runtime
(`TileParityRules.fluxScrubberRemoves`) answers exactly the same.

## 6. The commits of the branch

| Commit | What |
|---|---|
| `905d81ca` | the leader's `8d2e2126`, cherry-picked (the two pure rules of the base content) |
| `fcc900e5` | the blocks of the visibility test are registered, the block registry can freeze |
| `95d7c5a8` | the flux scrubber rule is asked for the class of a block |
| `e17ac98d` | the enchantment of the aspect bonus test is a stand alone holder |
| `d69dafee` | the damage wildcard of the original equalises a pair with one damageable stack |
| `docs(testsuite)` | this report and the random class order |

## 7. What is still open

* Nothing of the 14 failures is open.
* The rule of the plant soil (`BlockCustomPlant.soilSupports`) is the port's expression of
  the Forge `EnumPlantType` of the original
  (`thaumcraft/common/blocks/BlockCustomPlantItem.java:85-101`). The port has no cave
  plant variant, so the cave type of the original (the metadata four and five) has no
  native counterpart; the two rules that the port does have (plains and desert) are
  covered by the test of the base content.
* The class order of the suite is now random. The leader may prefer the default order; the
  file `common/src/test/resources/junit-platform.properties` is the only place that has to
  be deleted for that.
