# Ordinary Guard4 — runtime final, 2026-10-19

**Source final for root integration; not compiled or executed.** Root's preceding
Harvest build2 PASS42s / six existing aspect JUnit results do not validate Guard.
No Gradle/Java, game, tests, Python, saves, fixtures or agents ran for this change.

## Owned paths / authority

Under `common/src/main/java/thaumcraft/common/entities/golems/`:

- `EntityGolemBase.java`: Guard core/menu/baseline integration, shared target
  policy, checked mob-damage entry and pause/removal cleanup.
- `GuardGolemGoals.java` (new): melee3, self-retaliation target1, nearest target2.

No other production file was edited or read as WIP. In particular,
`GolemCrops.java` is **unchanged**, preserving root's actual `Block.class` melon
fix. Harvest/Inventory/Gather/HomeInventory/Enum/queue/items/Data/menu/client/
registry/platform/resources remain outside this author's changes. Existing
material attributes/FOLLOW_RANGE, health/armor/regen, safe-home behavior,
`canEditLinks` delegation, physical cargo and raw throw detach remain intact.

Read final `2026-10-19-golem-guard.md`. Oracle anchors below are under
`../work/thaumcraft-4.2.3.5-oracle/thaumcraft/common/`:

| Original | Runtime mapping |
| --- | --- |
| `entities/golems/EntityGolemBase.java:296–302,460–473,1146–1178` | Ordinary hostile-class policy, floored-target/home distance²<256, melee3 and self/nearest target priorities1/2. No owner-defense/pack goal. |
| `entities/ai/combat/AINearestAttackableTarget.java:46–64` | Actor bounding box inflated(16,4,16), nearest valid living candidate, no custom five-tick/random acquisition gate. |
| `entities/ai/combat/AIHurtByTarget.java:24–49`; `AITarget.java:82–127` | Actual golem's attacker only; same fixed policy, current identity and home bound. Native hurt timestamp prevents stale-event restarts; no alert-others. |
| `entities/ai/combat/AIGolemAttackOnCollide.java:35–79` | Native path required, MOVE/LOOK mutex, visible repath4+nextInt(7), target-width reach and20 active-update attack cooldown. |
| `entities/golems/EntityGolemBase.java:1010–1018,1102–1128,1190–1192` | Idle action6/status4 before hurt, raw unarmed2+material strength, mob-attack attribution; advanced/enchantment extensions deferred. |

## Targeting / actual melee

New public entity methods:

```java
public boolean isGuardTarget(LivingEntity target);
public int getAttackSpeed(); // ordinary20
```

- The single policy requires a live/current Guard on the native server thread,
  `canWork`, same-level live target with actual ID+UUID identity and loaded target
  chunk. It positively requires native **Enemy**, excluding every Creeper,
  Player, AbstractGolem, villager and bat, owned pets (`OwnableEntity` owner UUID),
  and allies in either direction. Passive farm animals are not Enemy; hostile
  Enemy animals such as hoglins still qualify. Neutral hostile-class members are
  not filtered by anger or owner attack history.
- Target **floored feet block position** must be strictly inside the home-centered
  Euclidean radius16. Acquisition separately uses the actor's expanded box and
  actor-distance nearest ordering, retaining traversal order for ties. Markers
  and attached inventory faces never select enemies or patrol routes.
- Both target selectors and melee continuation recheck this same predicate.
  Nearest retention uses the existing native FOLLOW_RANGE attribute and60-update
  lost-sight memory; attributes are not rewritten. Self-retaliation cannot select
  a player/pet/cow/creeper merely because it damaged the golem.
- Native path creation is mandatory; movement modifier1 uses material speed.
  Visible-only repath counter is4…10. Existing conservative loaded-path-region
  guards are reused; no path teleport, forced chunks or global NoAI changes.
- Melee requires every-tick updates. Reach is
  `distance²(actorFeet,victimX/bbox.minY/Z) <= 4*victimWidth²+1`.
  Cooldown decrements only on active melee updates, survives start/stop/repath/
  configuration pause, and is paid as20 **before** the attempt. It is transient,
  not persisted or converted to wall-clock time.
- A private active-goal attempt guard authorizes the entity's overridden
  `doHurtTarget(Entity)`. Inside the actual busy guard it starts idle action6,
  then revalidates policy, real target, reach, loaded region and **fresh native
  line of sight** immediately before one
  `victim.hurt(damageSources().mobAttack(this), 2F + getGolemStrength())`.
  Native cancellation/invulnerability false still spends cooldown/action.
  Calls outside that real worker intent cannot bypass the checked path.
- Strike sight is the declared correction to original through-wall reach hits.
  The native hurt/cancellation pipeline is the entity-harm permission path: no
  crop fake-player bridge, duplicate permission events, raw health edits, owner
  credit, direct loot/XP grants, IronGolem random damage/launch or invented effects.

## Baseline menu/data / preserved stock

- Root supplies finite `GolemData.GUARD=4` support in the unchanged five-field
  schema and exact `coreForType(4)`. Supported **material** IDs remain unchanged;
  Flesh material4 is still refused independently of Guard core4.
- `hasSupportedCore()` and `hasConfigurationMenu()` now include Guard4, retaining
  existing current/supported/refusal checks. Harvest3 still has **no menu**.
  Guard is excluded from `inventoryCore` and Inventory AI install/tick; pickup,
  deposit and world throws remain unavailable to it.
- Guard requires six EMPTY filters, wanted0 and false Fill mode. Existing config,
  apply, legacy/native load retention and coreless-install guards now cover4.
  Nonbaseline input is refused/retained, never stripped; invalid native saves
  freeze through `unsupported`/`retainedConfiguration` with cargo separate.
  Ghost/wanted/Fill setters cannot edit Guard's nonexistent controls.
- Normal baseline recall preserves4, markers, material placer/name and separate
  cargo; dismantle resolves root's core4 item. Markers persist but do not affect
  combat. No GuardOptions field, weapon ghost slot or marker patrol schema exists.
- Real equipment is never created, cleared, swapped or consumed here. Holding
  either hand item refuses this first unarmed slice's damage (attempt accounting
  remains paid). Guard recall/dismantle refuses **any nonempty native equipment**
  rather than discarding that real stock into a five-field placer with no equipment
  storage. Equipment transfer/enchanted-weapon recall is explicitly not implemented.
- Opening Guard configuration immediately clears its target/route/attempt state;
  selectors/strikes refuse work while any opener remains. Cooldown is not reset.
  Reset/rehome/removal also clears private combat state. Shared swell avoidance0,
  doors/ReturnHome/look remain installed, and active melee yields no path to
  lower-priority ReturnHome through the existing route-yield hook.

Root owns actual core/recipe/book/assets/Data and the zero-ghost Guard GUI with
locked-category explanation. Buttons51…54 must remain refused server-side
without Order, not hidden-client-only controls. Player/PvP, animal/creeper category
enablement, upgrades, enchanted offensive effects, advanced/decorations and R
darts remain deferred; independent arm timers stay0. Root's actual final Fabric
build follows integration. Combat, cancellation/protection behavior, visuals and
deferred gameplay acceptance are unverified; no previous PASS is reused here.
