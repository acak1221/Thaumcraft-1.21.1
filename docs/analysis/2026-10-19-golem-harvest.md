# Ordinary Harvest core3 — next farmer contract (2026-10-19)

**Read-only proposal, not implementation/PASS.** Original TC4 4.2.3.5 only: `O = ../work/thaumcraft-4.2.3.5-oracle/thaumcraft/`; `A` = `common/entities/ai/interact/AIHarvestCrops.java`, `C` = `common/lib/utils/CropUtils.java`, `E` = `common/entities/golems/EntityGolemBase.java`. Paths below are under O.

Other anchors: `Config*` in `common/config/`, `Aspect.java` in `api/aspects/`, `ItemGolemCore.java` beside E, `AIItemPickup.java` in `common/entities/ai/inventory/`; `Logs` is `AIHarvestLogs.java` beside A.

## Identity / acquisition

- **Harvest3**, blank100: crucible **one blank + meto5 + messis5 → one Harvest core**, alias **CoreHarvest**, gate **COREHARVEST** (`common/config/ConfigRecipes.java:175`; `api/aspects/Aspect.java:62,65`). Item stacks64, uncommon; install/pay one on a coreless golem, no material prerequisite (`golems/ItemGolemCore.java:36–40,77–78`; `E:978–989`). No ongoing fuel/vis/tool payment.
- Research **GOLEMANCY (-2,5)**, complexity2, **meto6/messis3/iter3**, concealed, parent **COREGATHER**, not secondary, no siblings. Pages: `tc.research_page.COREHARVEST.1`, `CoreHarvest`, Order-gated `.2` (`ConfigResearch.java:334`; `Aspect.java:48,62,65`). Sorting/INFUSION/UPGRADEORDER are **not** ordinary Harvest prerequisites.
- **No GUI, ghost filters or seed inventory** (`ItemGolemCore.java:81–120`; `E:198–218`). Proposed item `golem_core_harvest`; original icon `textures/items/golem_core_harvest.png` (`ItemGolemCore.java:48`).

## Actual search / work loop

- **Harvest goal priority2**, plus shared avoidance/door/return-home/look tasks; no pickup/deposit goals (`E:265–268,292–294,346–350`). Center is placed **home**; inventory attachment is unnecessary; **markers are ignored** (also original COREHARVEST.1 text).
- Distance **ceil(getRange()/4)**: ordinary16 → **4**, **9×9 X/Z** shuffled column queue, one column per eligible scan, Y **home−3…+3** ascending, first ripe match—not nearest. Admission: idle navigation, delay<0, `ticks % golemDelay==0` (default5/minimum1). Refill/shuffle only when queue empties (`A:80–100,161–182`; `E:460–468`; `Config.java:425–429,879`).
- Path to target center at material speed; budget **200 active updates**. Continue only with unchanged block+metadata, remaining budget and **(positive delay or active path)**. Work/look-at within **d²≤4**, not four-block reach (`A:103–126,151–159`).
- Work delay **int(max(10,(20−2×strength)×hardness))**, decremented only in reach; action/sound/crack pulses while idle-navigation, harvest at0, clear cracks on stop (`A:110–124,151–154`). Native active goal must update each server tick, not silently halve cadence.
- After harvest, first ripe neighbor in **X/Z±2,Y±1**, constrained to **home-axis±4 including Y**, restarts path/budget; chaining may reach homeY±4 (`A:129–159`). One-target chaining, not instant area harvesting/teleportation.

## Which plants count

`C:84–98`: non-air `IGrowable` unable to grow further **except stems**; unregistered `BlockCrops` metadata7; ripe wart/cocoa; registered standard/clickable states; stacked entries **with same block below**. Defaults: `Config.java:815–820`; wildcard registers metadata0…15 (`C:32–82`). Do not translate this into “break every modern bonemealable block.”

| Meaningful initial native vanilla allowlist | Original criterion → 1.21.1 adaptation |
| --- | --- |
| Wheat/carrot/potato | Age7 → each supported native crop's maximum age. |
| Nether wart | Metadata≥3 → native age3. |
| Cocoa | Growth bits≥2 → age2; pod/facing, not supporting jungle log. |
| Melon/pumpkin fruit | Standard wildcard; leave stems. Map natural pumpkin fruit, not carved/lit decorations. |
| Cane/cactus | Any metadata **above same-block segment**; preserve bottom, not top-only. Normal upper-block drops, never duplicate loot. |

**No tree-felling**: Lumber7 installs `AIHarvestLogs`, not Harvest3 (`E:317–319`; Logs:147–151). Defer TC4 mana pods (default metadata7) until native crop support, Forestry/addon clickable registrations, generic-growable parity and newer crops (beetroot/berries/vines/torchflower/pitcher/bamboo/kelp). Fruit eligibility has no player-placed provenance test; protection/bounds remain necessary.

## Drops, seeds, and the Gather partner

- Base uses **destroy-with-drops**, not age reset; registered clickable crops instead receive activation (`A:185–193,248`). Ordinary loot/seeds stay **on the ground**: no cargo insertion, guaranteed seed, bonus Fortune or player seed debit. Never emit drops twice.
- **Order4 alone replants**, deferred (`A:194–245`; `ConfigResearch.java:346`; original COREHARVEST.2). It uses real drops within6, including older drops; generic plantables try center+eight neighbors, debit one per successful use; fresh drops may be nudged. Cocoa/mana special paths pre-decrement before unchecked placement: future native placement/payment must commit safely, not lose/invent seed. **No free base replanting.**
- Pair a **separate Gather2** (or real player/other collection): its home-range16 covers drops, empty filters accept all, otherwise include produce **and seeds**; deposit via its own attached inventory (`AIItemPickup.java:34–55,69–98`; `E:286–290`). No shared cargo/merged core. Player reseeds annual crops until Order; stems and stacked bases regrow naturally.

## Bounded native integration / safety

- Declared native safety adaptations: server-only **loaded** candidates; border/build/protection and mob-griefing/claim policy; recheck exact ripe state at commit; one mutation/normal tile-drop emission. Rejected/changed/unloaded targets cause no block/item mutation or fake success. Original direct destruction has no such protection check.
- Original profile **FakeThaumcraftGolem** (`A:188–193,223,232,247`) is **not** license for fake-player OP grants/arbitrary right clicks. Vanilla allowlisted breaking needs no impersonation. Honor existing protection; if it needs an unavailable legitimate actor, defer that action, not a blanket online-owner rule. No arbitrary hand/inventory access or forced loading.
- Minimal integration: allow core3 in item/config/recall and `coreForType(3)`, **not inventory-routing/menu predicates**; reuse public home/range/native navigation/strength. If absent, expose only **`getGolemStrength()` / `startActionTimer()`**. Original starts idle counter6/event4; getter `3−abs(action−3)` (`E:850–854,1010–1018`). Reuse model/action/badge; **no new model, cargo API, menu or packet**. One goal owns private transient scan/target/delay; no provider/audit framework. Keep bell recall/selection, not marker-dependent harvesting.

**Next playable outcome after Sorting:** paid Harvest3 navigates/harvests ripe crops; Gather collects/deposits real drops; immature crops, bases/stems and protected targets remain. Only this note was written; current WIP was not read. No production/otherdocs, Gradle/Java/game/tests/Python/saves/agents; no completion claim.
