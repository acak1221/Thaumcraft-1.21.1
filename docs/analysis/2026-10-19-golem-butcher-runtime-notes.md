# Butcher9 runtime — FINAL owned-source handoff, 2026-10-19

**Implemented, NOT compiled/game-tested.** Root builds actual Fabric after this
FINAL. The supplied Alchemy PASS35s/11 existing data JUnit results predate Butcher
and do not validate it. No agents, Gradle/Java, tests, game, Python, saves/worlds,
native harm/protected-entity operations, fixtures, hashes or proof tooling used.

Owned edits only: `common/src/main/java/thaumcraft/common/entities/golems/EntityGolemBase.java`,
existing same-package `GuardGolemGoals.java`, this note. No extra selector helper.
Authority: FINAL [contract](2026-10-19-golem-butcher.md), [API](2026-10-19-golem-butcher-api.md);
original oracle `common/entities/ai/combat/AINearestButcherTarget.java` (B),
`AIOldestAttackableTargetSorter.java` (S), `AIGolemAttackOnCollide.java` (M).

## Core dispatch / original source

- `isGuardTarget`'s core4-only body is unchanged. Package-private `isCombatTarget`
  dispatches **4 → existing Guard**, **9 → fresh Butcher surplus**, all others false.
  Guard retains retaliation1/nearest-hostile2; Butcher installs **oldest-surplus1
  + shared melee3 only**, no retaliation. Shared swelling-creeper avoidance0,
  doors/idle/ReturnHome6 are unchanged; combat route yielding now also covers9.
- B:47–67 / S:24–27: native actor BBox+(16,4,16) scan, sort **tickCount descending**,
  stable query-order ties; try oldest eligible surplus candidate first. Not breeding
  age, health, nearest distance, marker patrol or a new random/five-tick target gate.
- M:35–79: existing native-path requirement, speed modifier1, look30/30, visible
  repath4+nextInt(7), feet-to-target-X/bbox.minY/Z reach²≤4×width²+1. Cooldown20
  decrements on active every-tick melee updates and survives stop/pause/NoAI; no
  saved hunt/cooldown. Existing action6/status4/folded action getter remains.
- `ownsAttempt` additionally binds the **installed strike core**, victim and current
  target to the actual running private melee attempt. `doHurtTarget` still refuses
  standalone calls. Both hands must be empty; one actual
  `victim.hurt(damageSources().mobAttack(this),2F+strength)` remains the only damage.
  Attempts consume cooldown/action even on native hurt(false). No weapon/enchantment,
  darts, owner/player credit, direct HP write, synthetic meat/loot/XP or warp hook.

## Finite protection / fresh counts

- Public server-only `isButcherCandidate(LivingEntity)` / `hasButcherSurplus(...)`.
  Exact native **Cow/COW, Pig/PIG, Sheep/SHEEP, Chicken/CHICKEN,
  MushroomCow/MOOSHROOM** pairs only; current alive native ID+UUID/level, adult,
  loaded/border/height, native scan-box intersection and floored-feet-to-integer-
  home distance²<16². Cow/Mooshroom and unknown subclasses/type reuse never pool.
- Exclude names, all native leash data (including unresolved leash), ownership/
  taming, allies, **any scoreboard team**, native NoAI/invulnerability flags and
  denied native position permissions. The positive class/type list excludes native
  NPCs/players/villagers/hostiles/golems and every other animal. These protection,
  exact-class and finite-type restrictions intentionally narrow original B/E policy.
  NoAI/invulnerability exclusions conservatively refuse nonbaseline protected stock.
- Package-private `canUseButcherPosition(pos)` calls root's nonbreaking
  `GolemHarvestPlatform.mayInteract`. It preserves the previous busy value, so it
  also works **inside** the checked strike. Root must enable core9 there using
  native loaded/world/profile/spawn permissions, not break/use events, OP or an
  online owner connection. Guard4 never calls this new position query.
- Target **and reserves** must each pass the same permission query. After callbacks,
  recheck earlier animals' identity, position, adult/protection/home/scan eligibility.
  Require three distinct eligible members of the target's **exact class**, target
  included. Only loaded tracked entities are scanned; permission callbacks run
  after native section traversal has produced its list, not inside that traversal.
- These member lists exist for **one call only**, not cached population authority.
  Every chase/strike checks surplus anew. At the actual strike, query again after
  action feedback, refresh native eye sight, then revalidate permitted members,
  current target/core/NoAI/reach/empty hands immediately before native hurt. Population2,
  protected/dead/replaced/unloaded/invalid targets clear the hunt/path, not an injured
  third-animal finishing exception. Query failure/recursion fails closed.
- Native hurt cancellation/mitigation/death/ground drops remain authoritative;
  player/Gather collects, not the Butcher. Later native callbacks/other deaths can
  still alter population: **no global breeding-pair guarantee**, kill reservation,
  journal or universal addon entity-claim/NPC-metadata support is claimed.

## Integration / preserved state

- Root owns `GolemData.BUTCHER=9`, CORE_BUTCHER/coreForType(9), assets/book/lang,
  real **Guard4 + corpus5/bestia5 crucible CoreButcher/COREBUTCHER** acquisition.
  Existing data-driven research warp1 is left alone; no per-craft/per-kill warp.
- Runtime enables9 with EMPTY6/wanted0/modefalse. No GUI/inventoryCore/item AI/
  home item deposit/throw/fishing/water/essentia tasks; markers persist but are ignored.
  Install validates baseline before item pay; apply/configuredData/load use the same
  empty-control rule and retain malformed raw payload rather than normalize/shrink.
- Normal recall preserves9/material/name/markers; foreign equipment refusal includes9.
  Existing real item/water/aspect/Fishing stock handling and positive-stock dismantle
  refusal remain. Core/pause/NoAI/home/death/removal resets clear combat state without
  resetting cooldown or purging stock. Root placement-in-progress canWork/canEditLinks,
  remote links, deferredWaterRemoval/deferredAspectRemoval, health/material attributes,
  safe-home and Fishing RNG/lifecycle/raw directed-throw payment are preserved.

Root's native query/content integration and actual post-FINAL compile remain required.
Deferred: broader/addon animals, generic protected-livestock adapters, upgraded/PvP/
weapon behavior, combat/protection/loot/gameplay/visual acceptance and full-golem parity.
