# Agent `testfix9` — the eight red tests of the second wave

Date: 2026-10-23
Worktree: `/home/cyberfox-kali/wt/testfix9` (branch `agent/testfix9`)

The authority for every behaviour below is the byte-exact oracle
`/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (CFR 0.152 decompilation of
`Thaumcraft-1.7.10-4.2.3.5.jar`). `/home/cyberfox-kali/OpenTC4` and the retained 1.12.2
legacy source of this repository were read as second opinions; where they disagree the
oracle and the 1.7.10 jar win. The three vanilla 1.7.10 classes that the oracle only
*uses* (`ModelRenderer`, `ModelBox`, `TexturedQuad`, `MathHelper`) were read from the
1.7.10 MCP sources and are cited by their MCP names.

**Summary: seven of the eight failures were wrong expectations in the tests, one was a
production defect.** The production code of the models, of the death aspect orbs and of
the research trigger bindings was already right; the tests asserted values that the
original never produced. The single production defect is the configuration: the public
options of `thaumcraft.common.config.Config` carried only the runtime values, so the
declared defaults of the original (its static block) were destroyed as soon as any
settings were applied.

**The suite:** `:common:test` runs **2172 tests, 1 failure** — the only failure is
`thaumcraft.common.blocks.BaseContentItemParityTest.theSanitySoapKeepsTheOriginalDurationAndChances`,
which belongs to another agent. Every one of the eight failures of this wave is green.

| # | failure | verdict |
|---|---------|---------|
| 1 | `DeathAspectOrbRulesTest.aRefusedRollDropsNoOrbAndDrawsNoValue` | test |
| 2 | `EldritchClientModelParityTest.theGuardianKeepsTheFiftyFourPartsOfTheOriginal` | test |
| 3 | `EldritchClientModelParityTest.theGuardianMirrorsOnlyTheLeftShoulderPlate` | test |
| 4 | `EldritchClientModelParityTest.theGolemSwingsWithTheOriginalCosine` | test |
| 4 | `EldritchClientModelParityTest.theCrabSwingsItsLegsWithTheOriginalCosine` | test |
| 5 | `ResearchTriggerBindingsTest.theOtherOriginalItemTriggersKeepTheirNativeIdentity` | test |
| 6 | `ResearchItemTriggerTest.theCorpusIsReachableThroughThePublicRegistry` | test |
| 7 | `ConfigDefaultsTest.theResearchOptionsKeepTheirOriginalDefaults` | **production** |

---

## 1. The death aspect orbs — the draw list of the test was one boolean short

**The original.** Oracle `thaumcraft/common/lib/events/EventHandlerEntity.java:508-516`:

```java
} else if (!event.entityLiving.worldObj.isRemote && EntityUtils.getRecentlyHit(event.entityLiving) > 0
        && (aspectsCompound = ScanManager.generateEntityAspects((Entity)event.entityLiving)) != null
        && aspectsCompound.size() > 0) {
    AspectList aspects = ResearchManager.reduceToPrimals(aspectsCompound);
    for (Aspect aspect : aspects.getAspects()) {
        if (!event.entityLiving.worldObj.field_73012_v.nextBoolean()) continue;
        EntityAspectOrb orb = new EntityAspectOrb(event.entityLiving.worldObj, ..., aspect,
                1 + event.entityLiving.worldObj.field_73012_v.nextInt(aspects.getAmount(aspect)));
        event.entityLiving.worldObj.func_72838_d((Entity)orb);
    }
}
```

One `nextBoolean()` is drawn **per primal aspect**; the value `1 + nextInt(amount)` is
drawn only for a primal whose roll succeeded, and it is drawn **immediately** after that
roll. The reduction to the primals is oracle
`thaumcraft/common/lib/research/ResearchManager.java:466-495`
(`reduceToPrimals(al, false)`), which for the aspects `TAINT 2, AIR 2` yields the four
primals **air 4, entropy 4, order 2, fire 2** in that order (TAINT → MAGIC, ENTROPY →
VOID, ENERGY → AIR, ENTROPY, ORDER, FIRE).

**The port.** `common/src/main/java/thaumcraft/common/lib/entities/DeathAspectOrbRules.java:56-69`
draws exactly that: one boolean per primal (line 61) and the value right after a
successful roll (line 64). `common/src/main/java/thaumcraft/common/entities/DeathAspectOrbDrop.java:85`
calls it with the world random.

**The failure.** `common/src/test/java/thaumcraft/common/lib/entities/DeathAspectOrbRulesTest.java:71`
fed only three booleans (`false, false, true`) and one value roll into the four primals,
so the fourth boolean of the original ran past the end of the draw list
(`IndexOutOfBoundsException: Index 4 out of bounds for length 4`).

**Verdict: the test was wrong, the production rule was right.** The original draws a
boolean for *every* primal, also for the primals after the one that dropped an orb.

**The change.** The draw list of the test now carries the fourth boolean:
`new Draws(false, false, true, new Roll(2, 0), false)`. The test is now *stronger*: it
proves that the fourth primal is rolled as well.

---

## 2. The guardian: `Hood2` is a child of `Hood1`, not of the root

**The original.** Oracle `thaumcraft/client/renderers/models/entities/ModelEldritchGuardian.java`
creates **54** `ModelRenderer`s (lines 90-410) and hangs thirteen of them under a parent:

* `:149-151` — `Hood1.func_78792_a(Hood2)`, `Hood1.func_78792_a(Hood3)`, `Hood1.func_78792_a(Hood4)`
* `:202-203` — `ArmL1` → `ArmL2`, `ArmL3`
* `:218-219` — `ArmR1` → `ArmR2`, `ArmR3`
* `:412-419` — `LegpanelC1` → `LegpanelC2` → `LegpanelC3`, `SidepanelL2` → `SidepanelL3` → `SidepanelL4`,
  `SidepanelR2` → `SidepanelR3` → `SidepanelR4`, `Cloak1` → `Cloak2` → `Cloak3`

`func_78792_a` is the 1.7.10 `ModelRenderer.addChild`, so in the original `Hood2` is a
**grandchild** of the model root, exactly like in the port.

**The port.** `common/src/main/java/thaumcraft/client/renderers/models/ModelEldritchGuardian.java:76-129`
builds the same 54 parts with the same parents (`Hood1.addOrReplaceChild("Hood2", ...)` at
line 84, `ArmL1` → `ArmL2`/`ArmL3` at :94-95, …), and its own resolver
`part(String)` (lines 16-72) walks the same paths.

**The failure.** The test looked the parts up with `root.hasChild(part)`, which is false
for the thirteen descendants in the original as well, and its list of names had 53
entries: the original's `Chestplate` (oracle `:117-121`) was missing.

**Verdict: the test was wrong, the model was right.**

**The change.** `common/src/test/java/thaumcraft/eldritch/EldritchClientModelParityTest.java`
now walks the original's paths (`"Hood1/Hood2"`, `"Cloak1/Cloak2/Cloak3"`, …) and lists all
54 parts, `Chestplate` included. The count of 54 parts is unchanged and still asserted.

---

## 3. The mirror of the left shoulder plate

**The original.** Oracle `ModelEldritchGuardian.java:220-222`:

```java
this.ShoulderL = new ModelRenderer((ModelBase)this, 56, 35);
this.ShoulderL.field_78809_i = true;                                   // the mirror flag, BEFORE the box
this.ShoulderL.func_78789_a(-1.5f, -2.5f, -2.5f, 5, 5, 5);           // the box
```

`field_78809_i` is the 1.7.10 `ModelRenderer.mirror` (the retained 1.12.2 legacy source
of this repository has the same statement as `this.ShoulderL.mirror = true;` before the
same `addBox`, `src/main/java/thaumcraft/client/renderers/models/entities/ModelEldritchGuardian.java:220-222`).
The 1.7.10 `ModelBox` constructor reads that flag **while the box is built** and then
swaps the two x edges of the box and calls `TexturedQuad.func_78235_a()` on every face,
which reverses the four vertices of the face. Every other mirror flag of the guardian
comes *after* its `addBox` (oracle `:186`, `:230`, `:238`, `:245`, `:252`, `:301`, …) and
therefore never had any effect.

**The port.** `ModelEldritchGuardian.java:99` builds the left plate with
`CubeListBuilder.create().texOffs(56, 35).mirror().addBox(-1.5F, -2.5F, -2.5F, 5, 5, 5)`,
which is the 1.21.1 equivalent: the native `ModelPart.Cube` constructor swaps the two x
edges of a mirrored cube and the native `ModelPart$Polygon` constructor reverses the
vertex order of every face of a mirrored cube — the same two effects as the 1.7.10
`ModelBox`. The right plate (line 92) and every other part carry no mirror, exactly like
the original.

**The failure.** The test compared the *sequence* of the u coordinates of the two boxes
and expected them to differ. They cannot differ: the u of a face is assigned as
`[u2, u1, u1, u2]` (1.7.10 `TexturedQuad`, 1.21.1 `ModelPart$Polygon`), and the mirror
reverses the four vertices of the face, so the sequence stays palindromic. The test
method could not detect the mirror in the original either.

**Verdict: the test was wrong, the model was right.**

**The change.** The test now measures the effect of the mirror that the original really
had: the texture of a mirrored box runs **against** the x axis (the smaller x carries the
larger u), the texture of a plain box runs **with** it. The new helper `mirrored(...)`
renders the part in its own frame (the rest rotation is dropped) and compares the u of
the two x edges of every face that spans the x axis. The left plate must be mirrored and
the right plate must not.

---

## 4. The two cosines: the original swings with a lookup table, not with `Math.cos`

**The original.** The 1.7.10 `MathHelper.func_76134_b` (the cosine) is **not**
`Math.cos`:

```java
public static final float func_76134_b(float p_76134_0_) {
    return field_76144_a[(int)(p_76134_0_ * 10430.378F + 16384.0F) & '\uffff'];
}
static { for (int var0 = 0; var0 < 65536; ++var0)
    field_76144_a[var0] = (float)Math.sin((double)var0 * 3.141592653589793D * 2.0D / 65536.0D); }
```

The eldritch models use it everywhere: oracle `ModelEldritchGolem.java:259-260`
(the cloth), `:312-313` (the legs), `:323-324` (the arms) and `ModelEldritchCrab.java:163-164`
(the legs), `:181-182` (the tail).

**The port.** `common/src/main/java/thaumcraft/client/renderers/models/entities/ModelEldritchGolem.java:194-221`
and `.../ModelEldritchCrab.java:152-169` use `Mth.cos(...)`, and the native
`net.minecraft.util.Mth.cos(float)` is **the same table with the same index formula**
(`SIN[(int)(f * 10430.378f + 16384.0f) & 0xFFFF]` over the same 65536 sine values), so
the port reproduces the original exactly.

**The failure.** The expectations of the two tests were computed with `Math.cos`, whose
result differs from the table in the sixth decimal: at the phase 0.5 the golem's cloth is
`1.3662564` with `Math.cos` and `1.3662764` with the original's table; the crab's right
legs are `-0.31447032` against `-0.3144873`. The failing values were the **test's** ones.

**Verdict: the test was wrong, the production code was right.** (The leader's hypothesis
— a double against a float — was close but not the cause: the original is a 65536 entry
sine table, and the difference is the quantisation of that table.)

**The change.** The test now computes the expectations with the original's own table: the
static `ORIGINAL_SINE` block and the helper `originalCos(float)` reproduce the 1.7.10
`MathHelper` exactly, and every expectation uses the original's own expression
(`originalCos(limb * .44F) * 1.4F * 1.0F` and so on). The tolerance was not widened; the
values are the exact original ones.

---

## 5. The research item triggers: the port's table is right, the test was stale

**The original.** Oracle `thaumcraft/common/config/ConfigResearch.java` binds the item
triggers of eleven entries:

| key | oracle | original expression |
|-----|--------|---------------------|
| `BANNERS` | `:199` | `new ItemStack(ConfigBlocks.blockWoodenDevice, 1, 8)` |
| `BONEBOW` | `:221` | `new ItemStack((Item)Items.field_151031_f, 1, Short.MAX_VALUE)`, `new ItemStack(Items.field_151103_aS)` |
| `JARBRAIN` | `:235` | `new ItemStack(ConfigItems.itemResource, 1, 3)` |
| `MIRROR` | `:213` | the ender pearl `Items.field_151079_bi` and the three portal blocks `Blocks.field_150427_aO`, `Blocks.field_150384_bq`, `Blocks.field_150378_br` |
| `OUTERREV` | `:399` | `new ItemStack(ConfigBlocks.blockEldritch, 1, 5)`, `new ItemStack(ConfigBlocks.blockEldritch, 1, 10)` |
| `PRIMPEARL` | `:400` | `new ItemStack(ConfigItems.itemEldritchObject, 1, 3)` |
| `ROD_primal_staff` | `:414` | `new ItemStack(ConfigItems.itemFocusPrimal)` |
| `TINYHAT`, `TINYGLASSES`, `TINYBOWTIE`, `TINYFEZ` | `:352-355` | `new ItemStack(Blocks.field_150325_L, 1, Short.MAX_VALUE)` (the wool block) |

**The port.** `common/src/main/java/thaumcraft/common/lib/research/ResearchTriggerBindings.java:46-56`
maps every one of those eleven entries to its native identity, one id per original
expression and in the original order (`MIRROR` → the ender pearl, the nether portal, the
end portal and the end portal frame).

**The failure.** The test still expected the table of before the second wave: one id for
`MIRROR` and *no* id at all for `BANNERS` and `OUTERREV`. The wave-2 commit `f0148de6`
("the executable item and aspect triggers of the public research entry") had extended the
table to all eleven original entries; the test was never updated.

**Verdict: the test was stale, the production table was right.** (The leader's summary
of this failure — "the original only one" — does not hold for `MIRROR`: oracle
`ConfigResearch:213` binds four triggers. Two of them, the nether portal and the end
portal, have no item form in 1.7.10 and can therefore never match a scanned item; the
third one, the end portal frame, does have one, so the *effective* trigger set of the
original is the ender pearl and the end portal frame. The port reproduces that exactly:
`boundStacks(...)` resolves the four ids to the two items that exist in 1.21.1, and
`boundItems(...)` publishes the four original identities.)

**The change.** The test now expects the original's eleven entries: four ids for
`MIRROR`, one for `BANNERS` and two for `OUTERREV`, each with the oracle citation.

---

## 6. The complexity of the advanced alchemy furnace

**The original.** Oracle `thaumcraft/common/config/ConfigResearch.java:403` creates the
entry with the complexity **one**:

```java
new ResearchItem("ADVALCHEMYFURNACE", "ELDRITCH", new AspectList().add(Aspect.AURA, 1)..., -2, 6, 1, new ItemStack(ConfigBlocks.blockMetalDevice, 1, 3)) ...
```

and the 1.7.10 `ResearchItem` constructor clamps the complexity to one..three
(oracle `thaumcraft/api/research/ResearchItem.java:63-69`).

**The port.** `common/src/main/java/thaumcraft/api/research/ResearchItem.java:88-90`
clamps the same way, and the corpus entry carries the complexity one
(`common/src/main/resources/data/thaumcraft/tc4/research.json`).

**The failure.** The test expected `3`.

**Verdict: the test was wrong.** No entry of the original has a complexity of three
unless the corpus says so; the advanced alchemy furnace has one.

**The change.** The expectation is `1`, with the oracle citation.

---

## 7. The configuration options — the production defect

**The original.** Oracle `thaumcraft/common/config/Config.java:102-200` declares the
eighty-six public options and the static block at `:830-895` gives their defaults:

```java
hardNode = true;  wuss = false;  allowMirrors = true;  wardedStone = true;
allowCheatSheet = true;  researchDifficulty = 0;  aspectTotalCap = 100;
CwardedStone = true;  CallowCheatSheet = true;  CallowMirrors = true;  ChardNode = true;
Cwuss = false;  CresearchDifficulty = 0;  CaspectTotalCap = 100;  …
```

The original then replaces a part of them with the values of its configuration file
(`:388-392` and `:457-459`) and with the snapshot the server sends to the client
(oracle `thaumcraft/common/lib/network/misc/PacketConfig.java:33-53`). The one option
whose **configuration** default differs from its **declared** default is the cheat
sheet: the configuration file writes `false` for it (oracle `Config.java:456-458`),
while the static block declares `true` (`:869`).

**The port.** `common/src/main/java/thaumcraft/common/config/Config.java` published the
eighty-six options with the values of the static block, and
`ThaumcraftSettings.configureServer(...)` copied the seven research facts of the native
settings into them (`Config.apply(ResearchSettings)`, `Config.java:195-211`). The
settings carry the *configuration* defaults (`ResearchSettings.DEFAULTS`, cheat sheet
**false** — the original's configuration file default, see
`common/src/test/java/thaumcraft/utils/SharedInfrastructureParityTest.java:75-84`), so
the **declared** default of the cheat sheet was destroyed by the first test that applied
a settings snapshot. The public option of the original only carries the runtime value
too, but the original's *declared* defaults are published by its static block and are
the values the port has to reproduce.

**The failure.** `ConfigDefaultsTest.theResearchOptionsKeepTheirOriginalDefaults` read
the live option `Config.allowCheatSheet` and found `false` (the applied settings) where
the original's static block declares `true`.

**Verdict: the production class was incomplete (the declared defaults were not
published) and the test read a runtime value where it claimed to check a default.**

**The change.**

* `common/src/main/java/thaumcraft/common/config/Config.java` — a new public nested
  holder `Config.Defaults` publishes the declared defaults of the options that a
  configuration replaces (the seven research facts and the three taint facts) with the
  oracle citations, and the public options are initialised from that holder. The
  runtime values keep following the configuration, exactly like the original.
* `common/src/test/java/thaumcraft/common/config/ConfigDefaultsTest.java` — the research
  method asserts the declared defaults of `Config.Defaults`; a new method
  `thePublicOptionsFollowTheAppliedConfiguration` asserts that the public options do
  follow an applied configuration (the cheat sheet becomes `false`, the mirrors follow
  the settings) and restores the previous values.

---

## The GameTests

`:fabric:runGameTest` was run in this worktree with the own Gradle home
(`.gradle-fix9`): **318 GameTest cases, 6 failures**
(`fabric/build/gametest-results.xml`). The six failures are the pre-existing ones of the
merged waves and belong to other lanes, not to this one:

* `magicalbiomesgeneratenaturallyandspawnactualpech` - "region generation admits actual
  typed BE with original generation-plus-growth age3..7"
* `fluxoriginalexposureandgootail` - "original lower25 MISS falls through30 HIT then
  boolean; exact observed calls=[nextInt(25)]"
* `originalbiometreesanddungeonsuserealworldgen` - "original Forge BlockBush soil rule
  admits each actual plant in unmodified native darkness"
* `fluxmovesandexhaustswithoriginalrules` - "pending real Thaumic Slime branch does not
  silently consume goo (M11 not complete)"
* `fullinventoryreusesfreedpaperslotordrops` - "creative overflow spends one paper and
  the ink, and creates no note entity"
* `magicalflowersgrowharvestandrefine` - "original Forge Bush/Plains/Desert soil rule:
  cinderpearl=true; soil=Block{minecraft:dirt}"

The same six failures with the same 318 cases are in the last full report of the leader
(`Thaumcraft-1.21.1/fabric/build/gametest-results.xml`, 2026-10-22), so they predate
this wave. **None of the changes of this agent touches the gameplay** - the only
production change is the publication of the declared defaults of the configuration
options, which keeps the values of the options unchanged - so no GameTest can be
affected by it, and none of the six failures is in a lane of this agent.

(The first two attempts of the run were killed by the OOM killer of the machine - exit
137 - while the other agents ran their own game servers in parallel; the third attempt
completed.)

## Observations that are **not** part of the eight failures

* The oracle's guardian renders its `HoodEye` box (oracle `ModelEldritchGuardian:122-127`;
  the 1.7.10 `ModelRenderer.isRender` defaults to true and the original never clears it),
  while the port hides it in the model constructor
  (`common/src/main/java/thaumcraft/client/renderers/models/ModelEldritchGuardian.java:15`,
  "Warden eye is separate"). The warden renderer draws the eye on its own
  (`RenderEldritchWarden:35,77`), but the guardian renderer uses the same model. This is
  a client visual and was **not** changed here — the leader's client inspection owns it.
* The 1.7.10 mirror of a box is read by the `ModelBox` **while the box is built**, so
  every mirror flag that the original sets after its `addBox` is dead. The port follows
  that rule in every model it converted (for example `ModelJar`, which documents it);
  the guardian is the only model where a mirror before the box is effective.

## Files changed

* `common/src/main/java/thaumcraft/common/config/Config.java` — the `Defaults` holder.
* `common/src/test/java/thaumcraft/common/config/ConfigDefaultsTest.java`
* `common/src/test/java/thaumcraft/common/lib/entities/DeathAspectOrbRulesTest.java`
* `common/src/test/java/thaumcraft/eldritch/EldritchClientModelParityTest.java`
* `common/src/test/java/thaumcraft/research/ResearchTriggerBindingsTest.java`
* `common/src/test/java/thaumcraft/api/ResearchItemTriggerTest.java`
