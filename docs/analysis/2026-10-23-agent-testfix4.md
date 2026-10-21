# Agent `testfix4` — the four red JUnit tests of wave 2 (2026-10-23)

## Scope

Four JUnit tests of `:common:test` were red since the parallel wave wrote them; nobody had
executed them. This agent settles every one of them against the **byte-exact oracle** of the
original jar,

    /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle

and leaves the suite green. `/home/cyberfox-kali/OpenTC4` was only read as a second opinion; the
oracle wins everywhere (for the village piece weight the two agree, for the trade tables the
oracle carries the exact metadata numbers).

| # | test | verdict |
|---|------|---------|
| 1 | `SpecialMiningSafetyTest.theRollBoundaryIsTheOriginalOne` | **test** bug |
| 2 | `SpecialMiningSafetyTest.anUnknownReplacementKeepsTheOriginalDrop` | **production** bug |
| 3 | `VillageComponentParityTest.thePieceWeightDrawIsTheVillageSizeOrOneMore` | **test** bug |
| 4 | `VillageComponentParityTest.theTradeTablesKeepTheOriginalRows` | **test** bug |

One production defect, three wrong expectations. No test was weakened: every assertion that
survived asserts the original value, and the two assertions that were wrong were replaced by the
original value, not deleted.

---

## 1. `theRollBoundaryIsTheOriginalOne` — the test probed the wrong threshold

**Original.** `thaumcraft/common/lib/utils/Utils.java:174-183`:

```java
public static ItemStack findSpecialMiningResult(ItemStack is, float chance, Random rand) {
    ItemStack dropped = is.func_77946_l();
    float r = rand.nextFloat();
    List<Object> ik = Arrays.asList(is.func_77973_b(), is.func_77960_j());
    if (specialMiningResult.containsKey(ik) && r <= chance * specialMiningChance.get(ik).floatValue()) {
```

The roll that lands exactly on the threshold **succeeds** (`<=`, `Utils.java:178`). The threshold is
`chance * factor`, and the chance of the original is `0.2f + fortune * 0.075f`
(`thaumcraft/common/lib/events/EventHandlerWorld.java:182`, the call site is
`EventHandlerWorld.java:186`).

**Port.** `common/src/main/java/thaumcraft/common/lib/utils/SpecialMining.java:64-65`:

```java
/** Original {@code r <= chance * specialMiningChance}: a roll exactly on the threshold still succeeds. */
public static boolean succeeds(float roll, float chance, float factor) { return roll <= chance * factor; }
```

The production rule is exactly the original rule.

**The test.** `common/src/test/java/thaumcraft/crash/SpecialMiningSafetyTest.java:64` (before):

```java
assertTrue(SpecialMining.succeeds(0.2F, 1.0F, 1.0F));
assertFalse(SpecialMining.succeeds(0.20001F, 1.0F, 1.0F));
```

The second argument of `succeeds` is the **chance**, and the test passed `1.0` — a hundred per cent
chance, whose threshold is `1.0`, not `0.2`. Every roll below 1.0 succeeds, so the second
assertion could never hold. The test asserted a boundary the original never has.

**Verdict: the test was wrong, the production code is right.**

**Fix** (`SpecialMiningSafetyTest.java:64-73`): the test now probes the real threshold, the base
chance of the original:

```java
assertTrue(SpecialMining.succeeds(0.2F, SpecialMining.BASE_CHANCE, 1.0F));
assertFalse(SpecialMining.succeeds(0.20001F, SpecialMining.BASE_CHANCE, 1.0F));
assertTrue(SpecialMining.succeeds(0.17F, SpecialMining.BASE_CHANCE, SpecialMining.GOLD));
assertFalse(SpecialMining.succeeds(0.19F, SpecialMining.BASE_CHANCE, SpecialMining.GOLD));
```

The two added assertions cover the factor 0.9 of the gold and the cinnabar cluster
(`SpecialMining.java:41`), whose threshold is `0.2 * 0.9 = 0.18`.

---

## 2. `anUnknownReplacementKeepsTheOriginalDrop` — the production guard never fired

**Original.** `Utils.java:174-183` again. The original table is filled at start up with the
**registered** clusters (`Utils.addSpecialMiningResult`, `Utils.java:169-172`, called from
`Thaumcraft.java:243` and `Config.java:611-631`), so the original never meets a missing cluster:
the replacement stack always exists.

**Port.** `common/src/main/java/thaumcraft/common/lib/utils/SpecialMining.java:91-100` (before the
fix):

```java
Item output = BuiltInRegistries.ITEM.get(result.output());
// A cluster that the registry does not know keeps the original drop.
if (output == null) return dropped.copy();
return new ItemStack(output, dropped.getCount());
```

The rule the comment states is never executed. `BuiltInRegistries.ITEM` is a
**`DefaultedRegistry<Item>`** (`javap net.minecraft.core.registries.BuiltInRegistries`:
`public static final net.minecraft.core.DefaultedRegistry<net.minecraft.world.item.Item> ITEM;`),
and a defaulted registry answers its **default value** — the air item — for an unknown id, never
`null`. So the guard was dead code and the drop was replaced by
`new ItemStack(Items.AIR, count)`, which `ItemStack.isEmpty()` reports as empty
(`javap -c net.minecraft.world.item.ItemStack` — `isEmpty()` is true when the item is
`Items.AIR`): **the drop of the block was destroyed**.

**Verdict: the production code was wrong, the test is the specification.**

**Fix** (`SpecialMining.java:91-100`): the lookup is now checked by key, so an unknown cluster
keeps the original drop:

```java
if (!BuiltInRegistries.ITEM.containsKey(result.output())) return dropped.copy();
Item output = BuiltInRegistries.ITEM.get(result.output());
// A cluster that the registry does not know keeps the original drop.
if (output == null) return dropped.copy();
```

`Registry.containsKey(ResourceLocation)` is a plain map lookup on the registered locations
(`javap -c net.minecraft.core.MappedRegistry`), so it is false for an id that was never
registered and true for every real cluster.

The registered path is unchanged: the GameTest `pickaxeOfTheCoreBurnsWearsAndFindsClusters`
(`fabric/src/gameTest/java/thaumcraft/fabric/testing/ThaumcraftGameTests.java:926`,
`common/src/gameTest/java/thaumcraft/common/testing/ElementalPickaxeRuntimeChecks.java:154-176`)
still swaps raw iron, raw gold and the cinnabar ore into their clusters.

The test itself is unchanged — it is the specification — and only its comment was extended with
the defaulting of the registry (`SpecialMiningSafetyTest.java:50-62`).

---

## 3. `thePieceWeightDrawIsTheVillageSizeOrOneMore` — the test alternated by position, not by value

**Original.** `thaumcraft/common/lib/world/VillageBankerManager.java:58-60`:

```java
public StructureVillagePieces.PieceWeight getVillagePieceWeight(Random random, int i) {
    return new StructureVillagePieces.PieceWeight(ComponentBankerHome.class, 25, MathHelper.func_76136_a((Random)random, (int)(0 + i), (int)(1 + i)));
}
```

and `thaumcraft/common/lib/world/VillageWizardManager.java:55-57` with the weight 15 and the
wizard tower. `MathHelper.getRandomIntegerInRange(random, min, max)` is
`min + random.nextInt(max - min + 1)`, so the draw of the original is `i + nextInt(2)`: the two
values `i` and `i + 1`, and the draw itself is `0` or `1`. The helper survived into 1.21.1 as
`Mth.nextInt(random, min, max)` with the same body
(`javap -c net.minecraft.util.Mth`: `min + random.nextInt(max - min + 1)`).

**Port.** `common/src/main/java/thaumcraft/common/lib/world/VillageWorldGenRules.java:102`:

```java
public static int pieceWeightMinSize(int villageSize, int roll) {
    return villageSize + Math.floorMod(roll, 2);
}
```

The port models the two wide draw exactly and folds any draw outside the domain of the original
by its parity: an even draw is the village size, an odd draw the village size plus one.

**The test.** `common/src/test/java/thaumcraft/worldgen/VillageComponentParityTest.java:162`
(before):

```java
assertEquals(size,     ...pieceWeightMinSize(size, 0));
assertEquals(size + 1, ...pieceWeightMinSize(size, 1));
assertEquals(size,     ...pieceWeightMinSize(size, 2));
assertEquals(size + 1, ...pieceWeightMinSize(size, 3));
assertEquals(size,     ...pieceWeightMinSize(size, -1));   // <- odd draw, expected the size
assertEquals(size + 1, ...pieceWeightMinSize(size, -2));   // <- even draw, expected the size plus one
```

The six expectations alternate `size, size+1` **by their position in the list**. That is right for
the draws `0, 1, 2, 3` (even → size, odd → size + 1) and wrong for the two negative draws: `-1`
is odd and `-2` is even, so the original's two wide draw gives `size + 1` and `size`. No
implementation of a two wide draw can satisfy the old expectations: the parity of the *value* is
the only thing that can pick between the two values of the range.

**Verdict: the test was wrong, the production code is right.** The claim of the `worldgen` agent
(`docs/analysis/2026-10-23-agent-worldgen.md:99-113`, defect D3) — that the 1.7.10 helper is
`min + nextInt(max - min + 1)` and the draw is `i` or `i + 1` — was checked against the oracle and
is correct; the oracle expression `MathHelper.func_76136_a(random, 0 + i, 1 + i)` is exactly that
helper.

**Fix** (`VillageComponentParityTest.java:162-176`): the two wrong expectations are replaced by
the original values, and the comment states the rule and the oracle lines:

```java
assertEquals(size + 1, VillageWorldGenRules.pieceWeightMinSize(size, -1));
assertEquals(size,     VillageWorldGenRules.pieceWeightMinSize(size, -2));
```

The javadoc of the rule (`VillageWorldGenRules.java:84-101`) now states the fold of a draw that
is outside the domain of the original.

---

## 4. `theTradeTablesKeepTheOriginalRows` — a list compared against a single id

**Original.** The trade tables are `thaumcraft/common/lib/world/VillageBankerManager.java:41-56`
(twelve rows) and `thaumcraft/common/lib/world/VillageWizardManager.java:35-52` (fifteen rows).
Every row of the original is one pay stack and one receive stack, e.g.

```java
recipeList.add((Object)new MerchantRecipe(new ItemStack(ConfigItems.itemResource, 4 + random.nextInt(3), 3), new ItemStack(Items.field_151166_bC)));  // VillageWizardManager:39
```

`ConfigItems.itemResource` metadata 3 is the quicksilver and metadata 6 the amber
(`thaumcraft/common/config/ConfigItems.java:274-275`).

**Port.** `common/src/main/java/thaumcraft/common/lib/world/VillageWorldGenRules.java:632`:

```java
public record TradeSpec(String payItem, int payMin, int payMax, List<String> receiveItems, int receiveMin, int receiveMax)
```

`payItem` is a **single** id and `receiveItems` is the list of the metadata variants of the
receive side. The port table matches the oracle row for row:

| row | oracle | port |
|-----|--------|------|
| wizard 3 (index 2) | `VillageWizardManager.java:39` — resource metadata 3, the quicksilver, four to six | `thaumcraft:quick_silver`, 4..6 |
| wizard 5 (index 4) | `VillageWizardManager.java:41` — resource metadata 6, the amber, four to six | `thaumcraft:amber_gem`, 4..6 |
| wizard 7 (index 6) | `VillageWizardManager.java:43` — `itemNuggetChicken`, 24..31 | `thaumcraft:nugget_chicken`, 24..31 |
| wizard 9 (index 8) | `VillageWizardManager.java:45` — `itemNuggetBeef`, 24..31 | `thaumcraft:nugget_beef`, 24..31 |
| wizard 10 (index 9) | `VillageWizardManager.java:46` — the shard, two or three of the six metadata values | six shards, 2..3 |
| wizard 15 (index 14) | `VillageWizardManager.java:51` — the bauble blank, one of the six metadata values | six blanks, 1..1 |

**The test.** `VillageComponentParityTest.java:191` (before):

```java
assertEquals(List.of("thaumcraft:quick_silver"), wizard.get(2).payItem());
```

A `List` was compared against a `String`; the four assertions of the pay item could never hold.
The three other pay item assertions of the same test had the same defect and were never reached
because the first one aborted the test.

**Verdict: the test was wrong, the production table is right** (it is the same table the other
test `thaumcraft/world/VillageWorldGenRulesTest.java:224-258` already asserts as a string).

**Fix** (`VillageComponentParityTest.java:191-217`): the pay item is compared as the single id it
is, and every row got its oracle line:

```java
assertEquals("thaumcraft:quick_silver", wizard.get(2).payItem());
assertEquals("thaumcraft:amber_gem", wizard.get(4).payItem());
assertEquals("thaumcraft:nugget_chicken", wizard.get(6).payItem());
assertEquals("thaumcraft:nugget_beef", wizard.get(8).payItem());
```

---

## Files changed

```
common/src/main/java/thaumcraft/common/lib/utils/SpecialMining.java              (+6)  production fix, failure 2
common/src/main/java/thaumcraft/common/lib/world/VillageWorldGenRules.java       (+4)  javadoc of the fold, failure 3
common/src/test/java/thaumcraft/crash/SpecialMiningSafetyTest.java               (~)  failures 1 and 2
common/src/test/java/thaumcraft/worldgen/VillageComponentParityTest.java         (~)  failures 3 and 4
```

No unit test constructs a `Block`; the special mining rule is a pure static function
(`SpecialMining.succeeds`, `SpecialMining.find`) and the village rule is a pure static function
(`VillageWorldGenRules.pieceWeightMinSize`), both called by the production code.

## Verification

- `:common:test` — **2059 tests, 0 failures, 0 errors, 0 skipped** (the four tests of this
  report are green: `SpecialMiningSafetyTest` 6/6, `VillageComponentParityTest` 9/9, and the
  village table of `thaumcraft.world.VillageWorldGenRulesTest` 39/39 is still green).
  The run used a private Gradle home
  (`GRADLE_USER_HOME=<worktree>/.gradle-fix4`), so the shared Loom cache of the leader was not
  touched.
- `:fabric:runGameTest` — see below.

## Commits

| commit | what |
|--------|------|
| `chore(build): ignore the private gradle home of the test agent` | `.gradle-fix4/` is never committed |
| `fix(utils): an unknown cluster keeps the original drop` | the production defect of failure 2 and the boundary test of failure 1 |
| `fix(worldgen): the piece weight draw and the trade rows of the village` | failures 3 and 4 |
| `docs(testfix4): the four red tests against the oracle` | this report |
