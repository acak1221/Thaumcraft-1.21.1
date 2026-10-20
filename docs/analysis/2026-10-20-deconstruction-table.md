# Deconstruction Table — original contract and native unit

Gameplay authority: original TC4 4.2.3.5 oracle outside the checkout, `TileDeconstructionTable`, `ContainerDeconstructionTable`, `GuiDeconstructionTable`, `TileDeconstructionTableRenderer`, `BlockTable` metadata 14, `ConfigRecipes:254`, `ConfigResearch:385`, `ResearchManager:466-494`. The retained legacy source is not the gameplay specification.

## Implemented contract

- The real arcane recipe is ` S /ATP`: thaumometer above a base table, golden axe on its left and golden pickaxe on its right. It requires **DECONSTRUCTOR** and **20 Entropy vis**, with actual wand/gear pricing. The native ID is `thaumcraft:deconstruction_table`.
- One physical input slot, capacity 64 under ordinary GUI/automation limits. An item is accepted when the existing original-derived object-aspect resolver, including item/contained/enchantment bonuses, returns aspects. Untagged items supplied externally are retained without processing.
- Server work takes **40 ticks** per item. The first tick starts at 40 and immediately decrements to 39. Missing/invalid input or an unclaimed reward resets elapsed work to zero. Redstone does not pause the original table. Work progress is transient and restarts after reload.
- Reduce the complete item aspects to primals with the original **SUM** method: every component path adds its full quantity, including overlap. Reuse the existing guarded `FocalUpgradeCosts.reduceToPrimals` implementation of that same `ResearchManager` method. The energized-node MAX reducer is deliberately not used.
- Roll `nextInt(80) < sum(primal amounts)`. A failed roll still consumes one item. Success performs one further `nextInt(distinct primal count)` and stores that primal: the type draw is uniform, rather than weighted by amounts. The native server RNG remains the source.
- An unclaimed primal halts the machine. Real menu button **1** grants its actual sender exactly one RP of the stored primal, clears the reward, then synchronizes player and table state. Replay, closed/stale menus, another viewer, spectators, out-of-range and replaced tables cannot claim it. Claiming requires no extra research, matching the original.
- The upper face exposes no automation slots and refuses insertion. Other faces expose slot 0 and accept aspect-bearing items. Extraction is allowed; the original unconditional extraction predicate remains true even on the unexposed upper face.
- Input count/components, unclaimed primal and custom name survive native NBT. Elapsed work is not saved. Native implicit custom-name components transfer during block-item placement. Block destruction returns the physical input once through the existing durable drop queue; the unclaimed knowledge is not a physical dropped item. The block loot is one deconstruction table.
- The real screen uses `gui_decontable`, the original input/reward/progress coordinates and hhoff claim sound. The block renderer reuses the already ported original `ModelArcaneWorkbench` geometry with the original decontable texture, displays its fixed thaumometer, rotating actual input and actual unclaimed primal.
- Static inventory geometry uses a byte-identical alias of the existing `decontable_inventory.png` in the block-atlas directory. That 128x128 sheet duplicates each row of the original 128x64 art exactly twice, preserving normalized UVs without putting a non-square effect sheet into the atlas. The tile renderer continues to use the unchanged original 128x64 texture directly.

## Authority and preservation

The server resolves aspects against a private stack snapshot and rechecks the actual input and block-entity owner before payment. Both reward and player RP change before notifications. Malformed saved stacks/rewards remain in a rejected NBT payload and cannot process or grant knowledge. Cyclic/invalid aspect graphs and arithmetic overflow reject work before an item is spent.

Two explicit data-preservation adaptations avoid destructive original behavior: externally supplied oversized stacks are retained instead of truncating them to 64, and the saved one-item component template has a separate full native `InputCount`, avoiding the vanilla stack codec's count-99 limit. Ordinary Slots and hoppers still enforce their actual limits. A full integer RP pool keeps the unclaimed reward instead of overflowing its counter.

## Verification state

**Implemented:** recipe/registry/block, real server ticking, actual input inventory and automation, original reduction/chance/selection, authoritative menu claim, screen and block renderer, persistence and destruction delivery. **Built / game-tested / seen:** pending final root integration; no Gradle command was run by the author agent. All five new JSON resources parse; owned-file whitespace checks pass.

`DeconstructionRulesTest` checks overlapping SUM paths, every built-in aspect against the original recursive SUM oracle, the strict chance boundary and RNG-call count, unweighted distinct-primal selection, empty input and malformed/overflow/cyclic rejection.

`DeconstructionRuntimeChecks` uses the actual native arcane menu to acquire and pay for the table (including an unresearched refusal), places that resulting block item, exercises 39/40 tick boundaries with declared deterministic server-RNG seeds for both failure and success, waits on an unclaimed point, round-trips actual input components/reward/name, replaces the actual BE to reject stale menus, refuses invalid/remote/closed claim packets, grants exactly one point and rejects replay, verifies transient progress and retained malformed NBT, drives native upper/side/lower hopper routes, removes the actual block and checks durable input preservation, then leaves the final craft entirely to the registered ordinary ticker next to a redstone block. Test RNG seeding occurs only inside its synchronous fixture phase; it is not multiplayer evidence.

**Unverified:** inspected client GUI/rendering, original additive input hologram appearance (native item rendering currently displays the real item with its regular material), exact entity-item billboard/bobbing appearance, real save/restart beyond NBT and actual multiplayer/protection-mod behavior. NeoForge remains deferred. These limits do not close the full TC4 migration.
