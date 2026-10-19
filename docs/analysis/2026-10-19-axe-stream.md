# Axe of the Stream — playable Lumber prerequisite (2026-10-19)

**Source contract/proposal, not implementation or PASS.** Original TC4 4.2.3.5 only: `O = ../work/thaumcraft-4.2.3.5-oracle/thaumcraft/`. `A` = `common/items/equipment/ItemElementalAxe.java`; `B` = `common/lib/utils/BlockUtils.java`; `F` = `common/entities/EntityFollowingItem.java`. Config files are in `common/config/`. Final [Lumber contract](2026-10-19-golem-lumber.md) supplies the shared finite tree walk; current Entity/Harvest WIP was not read.

## Item, tier and legitimate acquisition

- Original `itemAxeElemental` / registered **ItemAxeElemental**, display **Axe of the Stream** (`ConfigItems.java:427–428`; original `assets/thaumcraft/lang/en_US.lang`). It is an actual `ItemAxe`, **tool class `axe`**, `IRepairable`, stack1, **rare** (`A:62–95`), not another thaumium axe renamed as an infusion ingredient.
- `api/ThaumcraftApi.java:50`: **THAUMIUM_ELEMENTAL**, harvest tier**3** (diamond-level), durability**1500**, effective speed**10**, material damage bonus**3**, enchantability**18**. Inherited legacy axe base3 gives **attack modifier+6** (ordinary player base1 →7 total), not native diamond-axe damage. Post-1.9 attack-speed/cooldown is an explicit compatibility choice. Map `minecraft:axes`, `minecraft:mineable/axe`, diamond-tier incorrect-for-drops and mining/durability enchantment groups; tier3 is not pickaxe permission. Repair uses **thaumium ingot**, original itemResource2 (`A:93–95`), through the existing native ingredient/tag/anvil path.
- **ElementalAxe / ELEMENTALAXE**, **infusion instability1**: central **undamaged thaumium axe**; ordered component array **water shard2, water shard2, diamond, Greatwood log0**, one each; **aqua16/arbor8 essentia**, not recurring tool-use vis (`ConfigRecipes.java:364`). Preserve both shard entries; original matching is a multiset, not a new clockwise-pedestal requirement (`api/crafting/InfusionRecipe.java:37–70`).
- Research **ARTIFICE (-7,4)**, complexity2, concealed, parents **THAUMIUM + INFUSION**, **instrumentum3/aqua3/motus3**; pages `tc.research_page.ELEMENTALAXE.1`, `ElementalAxe`, `.2` (`ConfigResearch.java:227`). Normal output is a fresh, unenchanted axe—not builtin Repair or automatic central-item enchant/name inheritance (`InfusionRecipe.java:121–122`; `TileInfusionMatrix.java:578–584`; `TilePedestal.java:94–99`). Original recipe damage matching is exact0 (`InfusionRecipe.java:44–49,93–94`): used axes need repair before these infusion inputs, including the Stream/iron axes consumed by **CoreLumber** (`ConfigRecipes.java:372`). Map that to native DAMAGE0 rather than silently accepting arbitrary worn tools.

## Player chopping: replace the endpoint, not the axe or whole tree

- `A:146–158`: on the player's **completed break interception**, a non-sneaking recognized log claims/cancels normal anchor breaking and calls `B.breakFurthestBlock(..., realPlayer, followItem=true, color=10)`. It removes **one selected endpoint per attempt**, retaining the anchor when a remote endpoint was chosen. Reuse the Lumber note's greedy same-Block walk/bounds, not flood-fill/instant tree deletion; placed log structures also qualify. Sneaking/non-log work falls through to the ordinary axe.
- `B:66–97,155–166` uses the **actual player's held tool**, harvestability/removal callbacks and normal loot, so legitimate Silk/Fortune/components matter; creative produces no survival drops. The axe charges **one durability** after the special interception (`A:155`); ordinary axe block/hit behavior remains inherited. Original charges even after failed work and `B:97` can report success after failed removal: native must instead charge **once on real committed removal**, respecting creative/Unbreaking, with no double `mineBlock` plus manual charge.
- Source feedback: blue `PacketFXBlockBubble` at the anchor to32 blocks and `thaumcraft:bubble` volume0.15/pitch1 (`A:151–153`). Keep normal leaf updates/decay; no replanting, special sapling payment or equipped Lumber-golem axe is implied.

## Active vacuum and automatic chopped-drop following are different

- Right-click starts **BOW use animation**, duration**72000**; every using tick pulls live item entities in **player-centered AABB±10**, not a spherical/LOS beam (`A:97–143`; `common/lib/utils/EntityUtils.java:181–191`). It skips dead entities and `EntityFollowingItem` with a non-null target. No owner/pickup-delay/LOS filter appears in the original motion loop; ordinary pickup rules are separate.
- Exact delta is `(itemX-playerX, itemY-playerY+playerHeight/2, itemZ-playerZ)`; subtract **normalized delta×0.3** from velocity, clamp **each component to±0.35** (`A:115–140`). Preserve the Y-expression explicitly when adapting native coordinate origins; guard zero length/non-finite motion. Cost is **zero durability/vis/items**. Blue bubbles surround affected drops. **No water creation/draining/water-walking ability exists.** There is no passive inventory/held-item vacuum scan in this class.
- Research prose says Shift suspends all specials, but **code checks Shift only for chopping**; `onUsingTick` has no sneak guard. Do not silently invent a Shift-vacuum disable. Native server owns velocity; client handles use pose/FX. Avoid accidentally inheriting modern axe stripping/scraping/wax-removal `useOn` behavior that swallows the original vacuum action.
- Automatic following applies only after successful special chopping: item entities in **endpoint-centered AABB±2**, **age exactly0**, alive, **not already EntityFollowingItem**, are replaced with full-stack copies targeting the real player (`B:81–92`), not all nearby old items. `F:45–108`: target follows the player's box center, noClip while flying; counter20→1 controls approach, distance≤0.5 clears target/noClip; normal item pickup remains, not direct inventory injection. Type10 uses blue bubbles. Never steal an assigned following target.
- Native safety adaptation: preserve complete stacks and pickup/owner restrictions; release invalid/removed/cross-dimension targets without teleporting drops. Prefer retargeting an existing live drop; if replacement is necessary, **successful admission before original discard**—the unchecked spawn-then-kill can lose stock. No duplicate drops, remote insertion, forced chunks or fake player/item stock. Following-item fire immunity (`common/entities/EntitySpecialItem.java:45–49`), collision/settling and FX parity must be declared if narrowed.

## Native components / Repair are not free regeneration

- Reuse current thaumium equipment patterns, but supply the **elemental** values above: native MAX_DAMAGE/DAMAGE, TOOL rules, attributes, rarity and enchantability; normal full-component ItemStacks/loot. No custom “charge,” alternate-mode NBT or journal is required; A's two alternate booleans are unused. The `IRepairable` marker alone does **nothing** without an actual **Repair I/II enchantment**; no default enchantment is in the recipe/item.
- If Repair is advertised, port its native enchantment/compatibility and paid server behavior, not Mending/free healing: maximum2, excludes Unbreaking (`common/lib/enchantment/EnchantmentRepair.java:33–46`). Every40 ticks, damaged survival-player hotbar/armor items repair1/2 only after vis payment: reduce the item's object-aspect tag to primals, per-primal cost **floor(sqrt(2×amount))×level** (`common/lib/events/EventHandlerEntity.java:245–269,294–308`). Payment checks vis amulets then real inventory wands (`common/items/wands/WandManager.java:104–118`). Anvil thaumium repair suffices for the first unenchanted tool; optional Repair support must remain visibly deferred if absent.

## Existing target-claim bridge: required narrow extension

```text
tryPlayerAxeChop(real ServerPlayer, actual hand/live tool,
                anchor + expectedAnchorState, endpoint + expectedEndpointState)
  -> existing claim outcome: PASS / REJECTED / COMMITTED
```

- `PASS` only for normal/sneaking/non-log work. Special denied/stale/unloaded work is still claimed: do not fall back and unexpectedly break the anchor. Hook **before completed anchor removal**, not a first-click instant miner or post-removal `mineBlock`. Reuse existing prediction/resync/claim callbacks and one scoped reentry guard; bypass only this axe's redirect during endpoint commit, **never protection callbacks**.
- Validate ordinary reach/intent at **anchor**; remote endpoint is the explicitly bounded axe ability, not a teleported/fake player's reach. Check loaded state, border/build/claim permission **at each actual endpoint**, revalidate after callbacks, use native real-player loot/held-tool hooks, and pay damage exactly once. No OP/NPC actor substitution, whole-tree permission grant, global `lastx/y/z`, new provider/journal/auditor or copied Bore framework.
- First real slice must include the researched infusion, actual usable tier/durability/repair axe, one-endpoint redirected chops with real loot/following, Shift-normal chopping, and held-use item vacuum. Reuse Lumber's finite classic/Greatwood/ordinary-Silverwood scope; declare arbitrary ore/mod logs (numeric-ID/Block tuple mismatch: `Config.java:725–727`, `Utils.java:202–211`), flattened mixed-species traversal, node-bearing Silverwood/BEs/essence, newer-wood special chopping, and any unported optional Repair/following-item/particle details as gaps. A recipe/icon-only or generic vanilla axe is **not** this outcome and completes neither Lumber nor full M08.

**Presentation:** original is a static **16×16 `textures/items/elementalaxe.png`** sprite (`A:79–87`, original JAR header), ordinary extruded/handheld tool rendering. `client/ClientProxy.java:453–462` binds **no custom axe renderer/OBJ**. Native `item/handheld` + retained sprite and BOW use pose suffice; no body texture or animated axe frames are required. Preserve/declare blue particle/sound adaptations, not an invented 3D axe asset.

Only this new note was written. No production/otherdocs, current gameplay WIP, Gradle/Java/game/tests/Python/world/agents; no compile, visual or gameplay PASS is claimed.

## Root integration findings after handoff

**Superseded:** the optional central `input_damage` landed afterwards
([infusion-input-damage](2026-10-19-infusion-input-damage.md)) and the Axe is implemented
([axe-stream-implementation](2026-10-19-axe-stream-implementation.md)). Text below is the historical finding.

Current `InfusionStackRecipe` supports explicit component damage but no declared
central-input damage. Before adding ElementalAxe, extend initial recipe matching/
CODEC/stream with an optional input DAMAGE0 predicate, keeping old constructors/
recipes unchanged. Do **not** silently change `InfusionMatching.centralStillMatches`:
the original running ritual intentionally ignores wear in central retention.
Initial pristine admission and later central retention are different contracts.
The completed-break redirect must compose with the existing
`ExcavationServerPlayerGameModeMixin` callsites; a competing duplicate removeBlock
redirect would collide. No axe code or shared-hook change has yet been authored.
