# Pech, curse focus, mana beans and pods — implementation review

Oracle: immutable TC4 4.2.3.5 EntityPech, ContainerPech, AIPechItemEntityGoto,
AIAttackOnCollide, ItemFocusPech, EntityPechBlast, ItemManaBean, BlockManaPod,
TileManaPod and their original client models/renderers. Vanilla dependencies
were checked against the immutable 1.7.10 classes outside this repository:
`/workspace/work/pech-vanilla-1.7-audit/{rv,sw,vd,zc,yz}.java` and the joined
SRG/field mapping files. No original/donor file was changed.

Implemented scope:
- One saved Pech entity, three original equipment-defined variants,30health,
  attack6/speed.5/armor+2/capped20, nine strict-component pack stacks, greedy
  one-item taming, group anger and original reload/no-target behavior.
- Original default5-tick item search, full-stack admission, nearest16-block
  target and failed-path penalty; original10-active-tick melee and20..50 bow /
  20..30 curse ranged policies. Native navigation/equipment enchantments,
  voxel collision, protection and ordinary combat are platform adapters.
- Original death pack .88 WORLD draws, primal-bean ENTITY draws, WORLD gold
  coin resource18 gate, recent-player ENTITY rare knowledge-fragment gate.
  Explicit-motion ItemEntity emission preserves gameplay RNG streams.
- Original spawn equipment20-way table and BOTH original superclass Gaussian
  FOLLOW_RANGE bonuses. The native superclass would substitute triangular
  WORLD draws and handedness; those unrelated changes are not used.
- One-input/four-output175x232 menu; original ordered three tables, duplicate
  Silverwood-sapling tier5 entries, Stalker candles0..14, real native accessories,
  real focus pouch and real seven crystal clusters. Optional external metal
  entries have no substitutes in a fresh TC4-only world.
- Original curse focus cooldown250ms, EARTH/ENTROPY/WATER10 centivis and
  Nightshade15. As in TC4, tooltip focus costs six primals but actual outer-wand
  payment still queries BASE three primals. Potency/Extend/Frugal and rank
  choices, owner-tagged native projectile and NPC Mage/Bow branches are real.
- Curse splash excludes owner/allPech, applies P+2 thrown damage and one of
  PoisonP/Slowness(P+1)/WeaknessP for100+40E; Nightshade allthree. Refused
  damage does not suppress status application. Native projectile flight and
  server-authoritative impact events drive the complete original client FX.
- Actual ten-tick always-edible mana bean, nutrition1/saturation.5, item-local
  ANY-aspect lazy initialization, initialized-empty and unrelated-component
  guards, typed color/knowledge-gated tooltip, canonical40-slot effect lottery
  and .25 first-aspect RP award. No arbitrary modern-effect registry sampling.
- Actual hanging pod placement, original six vanilla unstripped log/wood species (LOG axis12 becomes native bark WOOD)
  plusTC4 magical logs, real MAGICAL biome tag, AGE0..7/light0..7, original
  collision shape, WORLD1/30 random growth and original ordered horizontal
  third-growth crossbreeding. Mature readonly aspect report deliberately has
  no essentia extraction/insertion. Native BLOCK_ENTITY loot snapshot retains
  typed one/two-bean drops after BE removal, replacing old static cache.
- Complete original Pech mesh/three skins/arm-held items/jaw+pack animation,
  GUI and dice sound, low-alpha additive curse wisps/standard-alpha smoke,
  pod stem and pulsing aspect-colored fruit model. Native render and resource
  code has not been visually inspected in a running client.

Critical preserved quirks established by dependency audit:
- Dungeon fallback tests inherited itemWeight<=5 AND maximumCount<=1;
  minimumCount is NOT this predicate. Selection is UNIFORM among descriptors,
  retries49 then accepts the50th even if its weight/count is invalid; actual
  reward copies descriptor base stack without rolling min/max counts. The
  descriptor list is rebuilt on each fallback branch so its original enchanted
  book initialization consumes the appropriate WORLD RNG before selection.
- Original fresh potion array is40: vanilla1..23; null0/24..30; TC4 custom31..39.
  Saturation23 is instant PotionHealth, but original affectEntity handles only
  Heal/Harm: it is a NO-OP and consumes no duration roll. Native Saturation's
  extra nutrition is explicitly not substituted.
- Pech wand spawn addVis inputs are WHOLE vis multiplied100. Typical Mage
  Earth/Entropy/Water are200..700 raw; Air/Fire/Order0..300 raw.
- ItemResource18 is GOLD COIN, not a vanilla gold nugget.
- PechBlast power/duration/nightshade are not saved by the original; reload
  still resets them to zero/false. Age persists as a lifetime/grace safety fix.
- Pech type is a signed saved byte and anger a signed short (native synced int
  carries the narrowed short). Renderer safely uses Forager skin for unknown
  signed values rather than reproducing the original array-index crash.

Known remaining Pech goal parity before this unit can be called complete:
- The current native AvoidEntityGoal uses modern combat targeting, which
  excludes creative/invisible players and checks line of sight. Original
  EntityAIAvoidEntity's Player branch calls World.getClosestPlayer: strict
  distance8 includes creative/invisible players and has no sight requirement.
  Replace this bounded goal with the original selector/policy while using
  current native flee-path geometry; this was found during read-only review.
- Original EntityAIWatchClosest2 at priority9 owns BOTH MOVE and LOOK flags;
  current native LookAtPlayerGoal owns LOOK only. Its original nearest-player
  and40..79-tick watching policy is also pending exact port. Ordinary
  EntityAIWatchClosest at priority10 owns LOOK and uses the original nearest
  Mob query. These are explicit missing behaviors, not compile failures or
  completed original parity.
- Natural MagicalForest/Eerie Pech spawning and pod survival/acquisition still
  depend on actual TC4 biome registration/world integration. No production
  vanilla biome is tagged magical to bypass that dependency.

Documented original authority/resource/liveness fixes:
1. Original ContainerPech validity only tests tamed, permitting dead/distant
   or wrong-session access. Native trade verifies its exact connected actor,
   actual current menu, live same-level Pech, sole customer, distance<=8,
   original input and complete pack snapshots immediately before payment.
2. Original direct-table rejection and empty-dungeon refunds may loop forever.
   Native plans preserve ordinary draw order but cap total rounds128 and tier
   rejection512, then refuse the unpaid transaction without changing input,
   pack or tame state. RNG already used for a refused plan is not rewound.
3. Original output merging silently discards leftovers. Native four-slot
   merge preflights whole rewards; overflow refuses the unpaid transaction.
   Taming changes, pack rewards, offering debit and four outputs commit
   together. Original onCrafting(...,0) callbacks follow that paid commit,
   with ManaBean's only identity-changing initialization staged privately.
   This is NOT an invented recipe CRAFT_ITEM event or a second warp award.
4. Original close clears its five stacks and ignores native admission failure.
   Native DeferredItemDrops has an additive Pech return producer preserving
   original four PLAYER float draws, throw motion,40-tick pickup delay and
   a persisted ENTITY PechDrop marker. Actual slot ownership transfers to the
   existing durable queue before native admission/callbacks; refused adds
   retain the same UUID and stack. Existing custody/retry/settlement machinery
   is reused; item components and pickup-owner UUID are not repurposed.
5. Modern ItemEntity supports only UUID throwers, while original used literal
   Thrower="PechDrop". Dedicated entity NBT boolean adapts that string predicate
   and survives save/load without changing reward stacking identity.
6. ManaBean planting charges only after admitted pod state+BE, avoiding the
   original unchecked setBlock path spending a bean on refused placement.
   Nonfinite projectile motion is stopped; removed projectiles return instead
   of performing old post-removal movement. Curse age is saved so reload
   cannot rejuvenate the500-tick lifetime.

Verification state after parent compile3:
- Common production and Fabric main/clientTest classes PASS in parent
  .tmp/full-port-fourth-compile3.log. This is code loading/compilation, not
  inspected client rendering or native server acceptance.
- Seven focused native-stack planner JUnit regressions PASS, exact XML
  common/build/test-results/test/TEST-thaumcraft.pech.PechTradeTest.xml at
  2026-10-20T11:01:10 (7 tests,0 failures/errors). Full691 suite had1
  unrelated notification failure handled by its owner; no full-suite pass.
- First GameTest compilation found3 Pech fixture errors: protected partial
  NBT APIs and a shadowed local. Sources now use actual public Entity
  saveWithoutId/load and a distinct crowdExpected name; rerun is pending.
- Four native server GameTest entrypoints are written: entity/trade/curse/beans.
  Actual10-tick meal/Saturation/RP,1/30 random tick, registered spawn predicate,
  native NPC ranged emission,10-tick melee, death-loot RNG, full paid native
  trade, repeated-interaction authority, cancelled close/retry/reload UUID
  ownership and all curse status branches are covered but have not yet run.
  Connected actors, high supported lanes, declared seeds and temporary native
  biome-holder MAGICAL tag are fixtures. Tag state is restored in finally;
  no production vanilla biome is labelled magical by these tests.
- No fresh-world magical-biome natural spawn/pod acquisition replay, no
  multiplayer/client visual acceptance, no NeoForge validation. Actual TC4
  MagicalForest/Eerie biomes and their world integration remain a later unit.
