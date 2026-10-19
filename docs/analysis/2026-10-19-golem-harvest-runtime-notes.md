# Ordinary Harvest3 — runtime final, 2026-10-19

**Source final for root integration, not compiled or executed.** The reported
automation Fabric PASS42s covers Sorting/throws/brain, not these Harvest sources.
No Gradle/Java, game, crop fixtures, tests, Python, saves or agents were run here.

## Owned paths / original authority

Under `common/src/main/java/thaumcraft/common/entities/golems/`:

- `EntityGolemBase.java`: core3/baseline/menu split, worker/action integration,
  guarded harvest entry and crack cleanup on removal.
- `HarvestGolemGoals.java` (new): one priority2 goal with private transient search,
  exact target, path budget and work delay.
- `GolemCrops.java` (new): finite shared native ripe-crop predicate.

No inventory/home/Gather helpers, Enum, queue, items/Data, menu/client, registry,
resources, loader/network or permission-platform sources were edited/read as WIP.
Root's `canEditLinks`/`canConfigure` delegation, raw directed-throw detach,
Sorting guards, seven materials/Flesh refusal and saved health/fire/names remain.

Read final `2026-10-19-golem-harvest.md` and `2026-10-19-golem-harvest-api.md`.
Oracle anchors below are under
`../work/thaumcraft-4.2.3.5-oracle/thaumcraft/common/`:

| Original | Native runtime |
| --- | --- |
| `entities/ai/interact/AIHarvestCrops.java:80–104,156–182` | Home-centered radius `ceil(range/4)=4`, shuffled9×9 X/Z columns, one column each eligible five-server-tick scan, homeY−3…+3 ascending, first ripe match. |
| `AIHarvestCrops.java:103–126` | Exact unchanged state, native center path/material speed,200 active-update budget, feet-to-target-center distance²≤4, hardness/strength work delay only decremented in reach. |
| `AIHarvestCrops.java:129–159` | After one successful harvest, first ripe neighbor in X/Z±2,Y±1, with all home axes bounded±4; reset path/budget, not instant area breaking. Chaining may reach homeY±4. |
| `AIHarvestCrops.java:151–154,185–193,248–249`; `lib/utils/CropUtils.java:84–98` | Clear native cracks, ordinary destroy-with-drops and action pulse; no base replanting or collection. Native safety/finite-crop restrictions are explicit adaptations. |
| `entities/golems/EntityGolemBase.java:292–294,346–350,1010–1018` | Harvest priority2 plus existing avoidance/doors/ReturnHome/look; idle raw action6/status4, original shaped action getter. |

## Exact work / crop scope

`GolemCrops.ripe(ServerLevel, BlockPos, BlockState)` is package-private/static.
It requires an already-loaded, in-height/in-border position and the same actual
state, then accepts only canonical vanilla block identities **and exact native
classes**:

- Wheat/carrot/potato at their native maximum age; wart3 and cocoa2.
- Natural melon/pumpkin fruits, not stems, carved pumpkins or jack-o-lanterns.
- Cane/cactus segments with the same block directly below, preserving the bottom.
  Native upper-segment neighbor drops are not manually recreated or doubled.

No broad Bonemealable/growable check, crop/provider registry, tools, vis/fuel,
seed grants, generic activation/right-click or world recipe is used.

Search runs from `harvest.serverTick()` to retain the actual five-server-tick
admission despite native new-goal throttling: idle navigation, delay<0, no pending
or active target. The81-column queue refills/shuffles only when empty (or explicit
reset/rehome); Y is ascending. Pending/active farm targets use the existing shared
ReturnHome-yield hook, without installing any inventory-routing goal for core3.

The goal requires every-tick updates. Work delay is exactly
`int(max(10,(20−2*strength)*hardness))`; `mod = delay / round(delay/6f)`.
After decrement, only positive countdown multiples of mod **while navigation is
idle** emit action, native block sound and crack stage
`int(9*(1−delay/maxDelay))`. Sound uses the actual block SoundType break sound
(oracle `func_150495_a`), volume `(volume+.7)/8`, pitch `pitch*.5`; the native API
names were read, not guessed. No per-tick invented thump or new renderer/packet.
Cracks use `destroyBlockProgress(actualEntityId,target,stage)` and stage−1 on
stop/reset/rehome/pause/removal. Paths stop on target cleanup.

At delay0 there is exactly one guarded attempt. Success, refusal or exception
retires that target before it can be ticked again. Only true permits completion
action/neighbor chaining in the same current level, with a fresh native path and
budget; a changed/unloaded target aborts. Existing loaded-path-region guards are
reused, with no forced chunks, teleports to crops or global NoAI manipulation.

## Exact root API / protection boundary

```java
public boolean hasConfigurationMenu();
void startActionTimer(); // package-private; existing ACTION6/status4/getter shape
boolean harvestCrop(BlockPos pos, BlockState expected); // package-private worker
```

- `hasSupportedCore()` includes3; `hasConfigurationMenu()` accepts **only0/1/2/10**
  on a live/current supported-material, non-unsupported/non-retiring entity with
  readable cargo. Root ContainerGolem must use this menu predicate alongside
  existing player checks. `mobInteract`/`startConfiguration` already use it here;
  Harvest has no empty dummy filter menu. Bell selection/link-edit/recall remain.
- The harvest entry requires its own goal's private active delay0 attempt, native
  server thread/current ID+UUID, `canWork`, exact ripe state and reach≤4, then
  enters the actual entity busy guard and delegates once to root's agreed
  package-private `GolemHarvestPlatform.harvest(golem,pos,expected)`, clearing busy
  in `finally`. No copied loot, simulated inventory or manual drops are produced.
- Root owns permission/profile integration: canonical validation, mobGriefing,
  spawn/claim policy and Architectury/Fabric callbacks using unspawned non-OP
  `FakeThaumcraftGolem`, revalidation after BEFORE callbacks, then one native
  `destroyBlock(pos,true,golem)`. This runtime does not bypass that hook or impose
  an online-owner lock. False is refusal, **not rollback of callbacks' own changes**.

## Data / preserved behavior / gaps

Root supplies `GolemData.HARVEST=3` in the unchanged five-field schema. As with
Sorting, Harvest requires six EMPTY filters, wanted0 and false Fill mode.
Configuration/apply/recall reject nonbaseline settings; native load retains raw
invalid configuration and separate cargo via `unsupported`/`retainedConfiguration`
rather than normalizing it away. Live installation is coreless-only and refuses
nonbaseline controls before consuming a core; the three ghost/mode setters cannot
edit Harvest controls. Markers persist unchanged but farming never reads them.

Normal recall retains core3, material placer/native name and markers; dismantling
uses existing root `coreForType(3)`. Inventory AI install/tick excludes Harvest,
pickup stays Gather-only and home insertion explicitly refuses Harvest. Existing
saved cargo is retained for recall/death, not used as farmer inventory. Throws
remain Fill/Empty-only, with their raw queue handoff unchanged. A separate Gather
or real player collects the ordinary ground drops.

Order4 replanting, mana pods, addon/clickable crops, generic-growable parity,
newer crops (including beetroot/berries/bamboo/kelp), and Lumber/tree felling remain
outside this baseline. Root owns item/recipe/research/menu/assets/platform changes
and the actual integrated Fabric build after final sources. Gameplay, loot,
protection callbacks, pathing and FX/visual acceptance remain unverified; no full
power-loss atomicity or deferred-final-check PASS is claimed.
