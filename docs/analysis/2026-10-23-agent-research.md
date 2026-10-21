# Research, scanning and knowledge — parity wave of 2026-10-23

Agent: `research` (unit list `/home/cyberfox-kali/units/research.txt`, 15 units).
Worktree: `/home/cyberfox-kali/wt/research`, branch `agent/research`.
Domain: `research_scanning_and_knowledge` (M01).

## 1. Method — the oracle was rebuilt, so the authority checks really ran

The private oracle of the ledger (`thaumcraft-4.2.3.5-oracle`, the CFR 0.152
decompilation of `Thaumcraft-1.7.10-4.2.3.5.jar`) is **not** present in this
sandbox, so `scripts/porting/update_scope_ledger.py --check` could not run at
first. It was rebuilt instead, from the original JAR alone:

* `java -jar cfr-0.152.jar Thaumcraft-1.7.10-4.2.3.5.jar --outputdir <dir> --jarfilter …`
  (three filters: `thaumcraft.(common|api|client).*`, `thaumcraft.codechicken.*`,
  `truetyper.*`; the unfiltered run crashes inside CFR 0.152 on the
  `codechicken` classes),
* every produced file was compared with the fingerprint the ledger already
  stores for that oracle path: **859/859 files match the recorded
  `sha256_lf` AND the recorded line count exactly** (e.g.
  `thaumcraft/common/lib/research/ResearchManager.java` →
  `4cc13ec51f85ae315c4097296a4ff4c6d848f7f4dcbbe2c54075bf2501ae0fd5`, 863 lines).

The rebuilt oracle is therefore byte-identical to the oracle the ledger was
authored against, and the two authority checks of the task really ran:

```
$ python3 scripts/porting/extract_tc4_definitions.py --oracle <rebuilt> --check
{"aspects": 48, "research_count": 201,
 "research_key_category_sha256": "397d620877d429c49d4db6ae65552a681ac5b26be5d60cd1e95e5e28cae9f40e",
 "text_page_count": 294,
 "text_page_sha256": "66a10cd39a47e78472134a35d3b7b4e1b78618996ac6c87907aeb63303d4bf38",
 "mode": "check"}

$ python3 scripts/porting/update_scope_ledger.py --check --original-root <rebuilt>
{... "stale_review_fingerprints": 0}
```

The research corpus is therefore verified **201/201** by the authority script
against the original, with the pinned hashes reproduced exactly.

Independent second check (own parser, not the extractor): the 201 entries of
`common/src/main/resources/data/thaumcraft/tc4/research.json` were compared
field by field with the readable 1.7.10 reference
(`/home/cyberfox-kali/OpenTC4/.../ConfigResearch.java`) over `category`,
`column`, `row`, `complexity`, `aspect_cost`, `flags`, `parents`,
`parents_hidden`, `siblings`, `aspect_triggers`, `entity_triggers`, the number
of item triggers, the page count/kind/`text_key`/`required_research`/
`recipe_key` and `warp`: **0 differences**.

## 2. Verdict summary (15 units, `docs/porting/scope-ledger.json`)

| canonical path | before | after | what changed |
|---|---|---|---|
| `thaumcraft/common/lib/research/ResearchManager.java` | partial | **parity_verified** | the paper cost of a note corrected; the seed comment corrected |
| `thaumcraft/common/lib/research/ScanManager.java` | partial | **parity_verified** | the camera tick of the scan restored |
| `thaumcraft/common/lib/research/PlayerKnowledge.java` | partial | **parity_verified** | persistence verified end to end; the short-cast gap shown to be unobservable |
| `thaumcraft/common/items/relics/ItemThaumometer.java` | partial | partial | the camera tick restored; the rune particles remain (client) |
| `thaumcraft/common/items/ItemResearchNotes.java` | partial | partial | the "seed deviation" of the previous audits was wrong; only the forced item-identity split remains |
| `thaumcraft/common/tiles/TileResearchTable.java` | partial | partial | **open defect proved**: the copy costs an ink sac, the original a black dye (blocked, tiles are not mine) |
| `thaumcraft/common/lib/network/playerdata/PacketScannedToServer.java` | partial | partial | the camera tick restored; the rune particles remain |
| `thaumcraft/api/research/ResearchItem.java` | partial | partial | the exact remaining API gap and its one-line fix recorded (api is not mine) |
| `thaumcraft/api/research/ResearchPage.java` | partial | partial | the exact remaining page gap recorded (api is not mine) |
| `thaumcraft/client/gui/GuiResearchBrowser.java` | partial | partial | the category icon data is published again; the drawing is client work |
| `thaumcraft/client/gui/GuiResearchRecipe.java` | partial | partial | the six missing array pages localised to the client renderer |
| `thaumcraft/client/gui/GuiResearchTable.java` | partial | partial | the table rules live in the common layer; the sheet look is client work |
| `thaumcraft/client/renderers/item/ItemThaumometerRenderer.java` | partial | partial | the forced loss of the first person view model restated with the oracle line |
| `thaumcraft/client/integration/jei/ResearchVisibility.java` | missing | missing | exclusion confirmed; the visibility rules live in the common layer |
| `thaumcraft/client/integration/jei/ResearchVisibilityTicker.java` | missing | missing | exclusion confirmed; the snapshot sync replaces the tick poll |

**3 units moved `partial` → `parity_verified`**; 12 units keep their verdict
with sharper evidence and an explicit blocker.

## 3. Defects found and fixed

### 3.1 A research note did not cost a paper (regression of the 2026-10-21 wave)

* **Original.** `thaumcraft/common/lib/research/ResearchManager.java:126` of the
  CFR 0.152 decompilation of the original JAR:
  `} else if (ResearchManager.consumeInkFromPlayer(player, false) && player.field_71071_by.func_146026_a(Items.field_151121_aF)) {`
  `func_146026_a` is `InventoryPlayer.consumeInventoryItem` and
  `Items.field_151121_aF` is `paper` — both names come from the official
  `mcp_stable-12-1.7.10` mapping (`fields.csv`/`methods.csv`; the method comment
  is *"removed one item of specified Item from inventory"*).
  So the original **spends exactly one paper** of the first main-inventory
  paper stack, and only then damages one ink point.
* **Port before.** `common/src/main/java/thaumcraft/common/lib/research/ResearchNoteIssuance.java:157`
  (`if (paperSlot(items) < 0) …` — a check only) and
  `ResearchNoteIssuance.java:186-194` (`stage` copied every paper stack
  unchanged), with the comment *"The original … only checks that the player
  carries paper and spends one point of ink. The paper itself is never taken"*.
* **Fix.** `ResearchNoteIssuance.java:37` (the plan now carries the paper
  slot), `:148-169` (the plan records it), `:182-202` (`stage` shrinks the first
  paper stack by one and empties the slot when it reaches zero) and `:77-95`
  (the delivered note therefore reuses the freed paper slot, exactly like the
  original, where `consumeInventoryItem` runs before `addItemStackToInventory`).
* **Evidence of the regression.** The previous audit
  (`docs/analysis/2026-10-21-audit-a8-research.md`) read the same oracle line
  126 and concluded the opposite; the 2026-10-21 wave then propagated that into
  the port, the unit test `ResearchNoteIssuanceTest.theOriginalChecksThePaperAndNeverSpendsIt`
  and eight GameTest assertions. All of them asserted the non-original
  behaviour and are corrected now.
* **Effect.** Every research note used to be free of paper; now it costs one
  paper, and a full inventory receives the note in the slot the paper freed.

### 3.2 The scan had no client feedback at all

* **Original.** `thaumcraft/common/items/relics/ItemThaumometer.java:163`:
  `p.field_70170_p.func_72980_b(p.field_70165_t, p.field_70163_u, p.field_70161_v, "thaumcraft:cameraticks", 0.2f, 0.45f + p.field_70170_p.field_73012_v.nextFloat() * 0.1f, false);`
  — the camera tick plays on every second tick of a stable scan (`count % 2 == 0`,
  line 162), at the scanner, with loudness `0.2` and the pitch `0.45…0.55`.
* **Port before.** `common/src/main/java/thaumcraft/common/items/relics/ItemThaumometer.java:47-61`
  — the scan completed silently; the ledger had recorded this as a known gap.
* **Fix.** `ItemThaumometer.java:34-45` (the original numbers as named
  constants `USE_DURATION = 25`, `SCAN_TICKS = 20`, `CAMERA_TICK_INTERVAL = 2`,
  `CAMERA_VOLUME = 0.2F`, `CAMERA_PITCH = 0.45F`, `CAMERA_PITCH_SPREAD = 0.1F`),
  `:64-99` (the camera tick is played from the server, at the scanner, with the
  original numbers) and `:86-89` (`cameraTickDue`).
* **Documented deviation.** The original played the sound on the scanning
  *client* and drew the pitch from the *client* world random, so a scan never
  shifted the server world random that generates the research puzzles. The
  native scan is server owned, so the server plays the sound (the 1.21.1
  `Level.playSound` → `ServerLevel.playSeededSound` → `PlayerList.broadcast`
  chain sends the sound packet to the dimension and the client attenuates it by
  distance, so in practice only the scanner and its immediate neighbours hear
  it); the pitch is drawn from a private generator so the **server world random
  stays untouched** (`ItemThaumometer.java:91-99`).
* **Still open (client).** The original also draws the rune particles at the
  target every tick of the scan (`ItemThaumometer.java:73/85/115`). That is a
  client side effect and `common/src/main/java/thaumcraft/client/**` belongs to
  the client wave, so the port still has no rune particles.

### 3.3 The hidden-note seed was documented against the wrong original method

* **Original.** `thaumcraft/common/lib/research/ResearchManager.java:154`:
  `Random rand = new Random(player.field_70170_p.func_72820_D() / 10L / 5L);`
  `World.func_72820_D` is `getWorldTime` in `mcp_stable-12-1.7.10` (the *total*
  world time would be `func_72967_k`). The 1.21.1 equivalent of
  `getWorldTime()` is `Level.getGameTime()`.
* **Port.** The behaviour was already right
  (`common/src/main/java/thaumcraft/common/lib/research/UnknownKnowledge.java:27`
  → `level.getGameTime()`), but the comments of
  `ResearchHiddenSelection.java:12-27` and `UnknownKnowledge.java:24-27` named
  `getTotalWorldTime`, which is what the 2026-10-21 audit report claimed.
* **Fix.** The comments now cite the real oracle line and the real method.

## 4. Defect proved but NOT fixed (blocker)

### 4.1 The copy of a solved research note costs the wrong material

* **Original.** `thaumcraft/common/tiles/TileResearchTable.java:402` requires
  `InventoryUtils.isPlayerCarrying(player, new ItemStack(Items.field_151100_aR, 1, 0)) >= 0`
  and `:413-414` consume `Items.field_151121_aF` (**paper**) and
  `Items.field_151100_aR` (**dye**, metadata 0 → **black dye**). The 1.21.1
  equivalent of `Items.dye` metadata 0 is `minecraft:black_dye`.
* **Port.** `common/src/main/java/thaumcraft/common/lib/network/ResearchTableCopyNetworking.java:47`
  looks for `Items.INK_SAC`, and
  `common/src/main/java/thaumcraft/common/tiles/TileResearchTable.java:342`
  rejects anything that is not an ink sac.
* **Why it was not fixed here.** The commit is in
  `common/src/main/java/thaumcraft/common/tiles/**`, which this wave must not
  touch (block-entity wave). The fix is two lines and is written down in the
  ledger note of the unit:
  `ResearchTableCopyNetworking.findSupply` must look for `Items.BLACK_DYE` and
  `TileResearchTable.commitCopy` must accept `Items.BLACK_DYE` instead of
  `Items.INK_SAC`. The 1.12 legacy reference used a feather, so the original
  4.2.3.5 black dye is the only authority here.

## 5. Corrections of the previous audits (research domain)

| previous claim | reality (oracle line + official 1.7.10 mapping) |
|---|---|
| a8: "the paper is checked and never taken" (`InventoryUtils.isPlayerCarrying`) | `ResearchManager.java:126` calls `InventoryPlayer.func_146026_a` = `consumeInventoryItem(Items.paper)` — the paper **is** spent |
| a8: the hidden-note seed is `World.getTotalWorldTime()` | `ResearchManager.java:154` calls `World.func_72820_D` = `getWorldTime()` — the port was already right, only the comment was wrong |
| a8/openTC4: the category icons are lost | `common/src/main/java/thaumcraft/common/config/ThaumcraftResearchCategories.java:25-42` publishes the six original icons and backgrounds again (BASICS = `textures/items/thaumonomiconcheat.png`, ELDRITCH = `gui_researchbackeldritch.png`) |
| a8: `getAspectPoolFor` lost its signed-short cast | `common/src/main/java/thaumcraft/api/aspects/AspectList.java:63-86` reproduces the original bubble sort again, and the short query is kept where it is observable (`ResearchNoteEditing.queryPool`) |

## 6. Does the research progress survive a relog? — **YES**

`common/src/main/java/thaumcraft/common/mixin/PlayerKnowledgeMixin.java:24-34`
writes the whole knowledge of the player into the player NBT and reads it
back:

```java
@Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
private void thaumcraft$saveKnowledge(CompoundTag tag, CallbackInfo callback) {
    tag.put(thaumcraft$KNOWLEDGE_KEY, thaumcraft$knowledge.save());
}

@Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
private void thaumcraft$loadKnowledge(CompoundTag tag, CallbackInfo callback) { … }
```

The 1.21.1 chain was verified in the class files of
`minecraft-merged-1.21.1`:

* `Player.addAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V` and
  `Player.readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V` exist
  (1.21.1 `Player`), and `ServerPlayer.addAdditionalSaveData` calls
  `Player.addAdditionalSaveData` with `invokespecial` — so the injected code
  runs for a server player;
* `PlayerDataStorage.save(Player)` calls `Player.saveWithoutId(CompoundTag)`,
  and `Entity.saveWithoutId` calls `addAdditionalSaveData`; `PlayerList.save`
  (called by `PlayerList.remove` on disconnect and by the periodic autosave)
  calls `PlayerDataStorage.save`;
* `Entity.load(CompoundTag)` calls `readAdditionalSaveData`, and
  `ServerPlayer.readAdditionalSaveData` calls `Player.readAdditionalSaveData`.

The loader regression
`common/src/gameTest/java/thaumcraft/common/testing/ResearchNoteIssuanceRuntimeChecks.java:69-76`
asserts exactly this round trip (`player.addAdditionalSaveData(saved)` →
`restored.readAdditionalSaveData(saved)` → identical knowledge), and
`common/src/test/java/thaumcraft/research/ResearchNoteMaterialCostTest.java:72-96`
asserts the pure `save()`/`load()` round trip of the knowledge.

**Conclusion: research progress, discovered aspects, scanned witnesses and the
three warp values survive a relog in the current port.** The only difference
to the original is the storage format (the player NBT of 1.21.1 instead of the
separate `.thaum` file) — a forced change, because only fresh 1.21.1 worlds are
supported.

## 7. Tests authored (authored, NOT executed)

No Gradle run was possible in this sandbox (the leader owns the build), so
**every test below is written but has never been executed**. The assertions are
taken from the oracle lines cited above.

* `common/src/test/java/thaumcraft/research/ResearchNoteMaterialCostTest.java`
  (new, 5 tests) — the one-paper cost, the emptied paper slot, the one-ink
  point cost, the first-paper-slot rule and the save/load round trip of the
  research progress.
* `common/src/test/java/thaumcraft/scanning/ThaumometerScanParityTest.java`
  (new, 4 tests) — the use duration 25, the 20 tick completion window, the
  every-second-tick camera tick and its `0.2 / 0.45 / 0.1` numbers.
* `common/src/test/java/thaumcraft/research/ResearchNoteIssuanceTest.java`
  (changed) — `theOriginalChecksThePaperAndNeverSpendsIt` became
  `theOriginalSpendsExactlyOnePaperAndOneInkPoint` and a second test covers the
  emptied paper stack.
* `common/src/gameTest/java/thaumcraft/common/testing/ResearchNoteIssuanceRuntimeChecks.java`
  (changed) — the eight loader assertions that encoded the non-original
  "the paper is never spent" rule now assert the original behaviour
  (3 → 2 paper, the freed slot receives the note, the overflow drops the note
  and spends one paper, the creative overflow spends one paper and the ink).

## 8. What could not be verified / not done

* **No build, no test run.** The unit tests above are authored but unexecuted;
  the GameTest changes are likewise unexecuted.
* **Rune particles of the scan** — client side effect, `client/**` is the
  client wave.
* **The six array pages of the research book** (`BANNERS`, `JARLABEL`, `ORE`,
  `PRIMALARROW`, `SCEPTRE`, the `CRUCIBLE` smelting page) — the page data and
  the recipe coverage are complete in the common layer, the renderer is client
  side.
* **The black dye of the research table copy** — proved, but the fix is in
  `common/tiles/**` (block-entity wave), see §4.1.
* **The executable item/aspect triggers of the public `ResearchItem`** — the
  fix is one line in `common/src/main/java/thaumcraft/api/research/ResearchItem.java`
  (public API wave): `of(ResearchDefinition)` has to copy
  `definition.aspectTriggers()` and the bound item stacks of
  `ResearchTriggerBindings` into the accessors of the original. The gameplay
  itself is unaffected: `ResearchClues`/`ResearchTriggerBindings` use the
  definition directly.
* **The first person view model of the thaumometer** — 1.21.1 has no
  `IItemRenderer`; forced gap.
* **Client visuals** — no client was started (no GLX in this sandbox), so no
  visual claim is made for any of the above.
