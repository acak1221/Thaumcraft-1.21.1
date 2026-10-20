# R15: scan callbacks, public entity tags and available late bindings

Implemented in the native common source; compilation and runtime validation of this change are pending the parent agent's integrated snapshot. This is a **partial native scan API**, not completion of the original Thaumcraft API or full client/multiplayer acceptance.

## Original contract

The immutable TC4 4.2.3.5 oracle outside the repository supplies these anchors:

- `thaumcraft/api/ThaumcraftApi.java:63–77,333–353`: public mutable `scanEventhandlers`/`scanEntities` lists, registration by appending, mutable entity-tag records retaining caller references.
- `thaumcraft/api/research/IScanEventHandler.java`: `scanPhenomena(ItemStack, World, EntityPlayer)` returns a `ScanResult` or null.
- `thaumcraft/common/items/relics/ItemThaumometer.java:86–156`: a pointed entity precedes the physical block; unsupported physical targets stop resolution. Only absence of a physical target reaches callbacks, in registration order, stopping at the first nonnull result. An invalid first callback does not expose a later callback. Use duration is 25 ticks; completion follows 20 stable ticks.
- `thaumcraft/api/research/ScanResult.java:26–43`: type1 equality compares item ID/metadata, type2 compares **entity instance IDs**, type3 compares phenomena text. Its odd `equals(null)`/`equals(non-ScanResult)` true results are retained in the public DTO, but are not used to authorize rewards.
- `thaumcraft/common/lib/research/ScanManager.java:76–130,186–233`: entity aspect selection uses the **last matching registration**, including a later generic override. Conditional predicates compare the original NBT boxed value with Java `equals`; variant identity extends the first matching condition's name/value, then the original child/zombie/creeper/golem modifiers. Player tag generation and null clue witness retain their separate original behavior.
- `thaumcraft/common/lib/utils/Utils.java:255` converts byte/short/int/long/float/double, byte array, String, compound-list, compound and int array. The old list getter specifically requests compound elements; Java arrays compare by reference, not contents. These quirks remain intentional.
- `thaumcraft/common/lib/research/ScanManager.java:235–277,394,493`: only `NODE` phenomena have implemented rewards. Scale sorted current node aspects by `max(4, amount / 10)`, then UNSTABLE/MAX entropy4, HUNGRY/MAX hunger4, TAINTED/MAX taint4, PURE/MAX heal2 then **ADD** order2, DARK/MAX death2 then **ADD** darkness2. Reading does not consume vis.
- `thaumcraft/common/config/ConfigResearch.java:400`: PRIMPEARL's actual item trigger is `itemEldritchObject:3`, the native `thaumcraft:primordial_pearl`. It remains subject to the original item/NBT matching rules.

## Native implementation

`thaumcraft.api.ThaumcraftApi` exposes the scan handler and entity-tag registration portion with the original mutable list/record shapes. Original69 retained ConfigAspects declarations expand the Wisp48 loop into **116 baseline public registrations**. Removing/replacing these entries actually changes entity resolution: there is no hidden immutable fallback. Addons can register either original entity names or current native registry IDs. Selected rewards are copied before resolving a scan, while registered aspect-list references remain live. The original byte `SkeletonType` predicate is supplied for native split skeleton identities; Wisp uses its persisted original `Type` string.

`ScanCallbacks` validates results on the current server thread. Numeric IDs are current native item registry IDs; metadata is native durability only for damageable items. Flattened subtype IDs require their actual native item identity. `ScanResult.item` instead provides an independent full component-bearing native witness, normalized to count1. Type2 results must reference the current indexed, unremoved entity in the same server level. Item entities still earn item knowledge/rewards, but retain entity instance identity during a use session.

Native node callbacks carry position, dimension and `NODE<UUID>` together. Resolution requires an already-loaded block entity with the same current UUID. It does not force-load a distant chunk. This replaces the original process-global node-location index; a bare legacy-shaped NODE string without its native locator cannot resolve. Arbitrary custom phenomena remain unsupported, as in original completion logic. `TileNode.getScanAspects` and physical/callback node selection share `NodeScanAspects`; implementing scan bonuses for saved special types does not claim their natural ecology is complete.

`ItemThaumometer` invokes callbacks with the actual stack/world/actor only after the physical ray misses. It preserves the physical entity/block occlusion ordering. Session identity is now independent of the persistent knowledge key: item ID+damage for type1, entity instance ID for **both living entities and dropped items** of type2, and node UUID for type3. Switching between two otherwise identical mobs/drops cancels the old session without paying research points. Separate instances remain duplicate research targets after a successful scan, as before.

The late PRIMPEARL trigger is bound to its now-present real pearl item. BANNERS/woodenDevice8, primal staff's focusPrimal and OUTERREV's eldritchObject5/10 remain unbound until those actual content identities exist; unrelated items are not substituted.

## Explicit adaptations and remaining work

The original client DTO/packet trust is replaced by repeated server target resolution and native validated locators. Original numeric hashes are not reused for fresh-world knowledge keys. No old-world import is provided.

Callback dispatch takes an ordered list snapshot. In the original ArrayList iterator, a handler that appends another registration and returns null reproducibly causes `ConcurrentModificationException`; the native snapshot lets the new handler participate on the next resolution. Null registration fails early; malformed or modern unsupported NBT predicate values fail closed instead of the original null dereference. Predicates retain exact boxed types and original array/list equality quirks.

Other Thaumcraft API areas, object-tag/group registration, public research registration/completion, general recipe callbacks, unsupported arbitrary phenomena and the missing late-item trigger identities remain separate pending units. A scan-only facade must not be described as full API compatibility. Client scan rendering/callback prediction, multiplayer and survival progression remain unverified here.

## Verification prepared

- `EntityScanApiTest`:4 tests for116-row public baseline/Wisp48, actual replacement/removal, late generic overrides, live registration references versus resolved copies, strict numeric predicates and original array/list quirks.
- `ScanCallbackDispatchTest`:3 tests for first-nonnull ordering (including invalid results), callback registration during dispatch, and the original DTO equality quirks.
- `NodeScanAspectsTest`:2 tests for MAX versus ADD, all special type bonuses, empty inputs and non-mutating reads.
- `ScanApiRuntimeChecks.nativeApiUsesWorldNbtAndNodeLocators`: real Cow NoAI byte NBT, current entity indexing/removal, component-bearing and numeric durable item DTOs, unsupported metadata/phenomena refusal, actual PURE node UUID/loaded-location validation, non-draining scan bonuses and real primordial-pearl NBT matching. PURE is an explicitly loaded saved-node fixture.
- `ScanApiRuntimeChecks.identicalPhysicalTargetsCancelOldSession`: actual native RMB resolution followed by retargeting two matching Cow entities and two matching stone ItemEntities; knowledge remains unpaid and sessions cancel. Also checks callback item drops retain separate entity IDs while sharing item knowledge.
- `ScanApiRuntimeChecks.callbacksRespectOcclusionAndTwentyTickSession`: actual unsupported physical entity and physical block precede callbacks, invalid first callback blocks later callbacks, actual held tool arguments, and repeated real world ticks completing an item callback at20ticks with no early reward.

The three native checks temporarily replace public callback/tag registrations and restore them and actor fixtures. Keep their Fabric wrappers in the same dedicated `m04_scan_api` batch; the timed check needs120 ticks, the others100. No Gradle command or test run was performed by the implementing subagent. Parent integration results and immutable commit evidence belong in the accepted checkpoint documentation after execution.

Integration correction: the three native wrappers use separate batches m04_scan_api_locators, m04_scan_api_identity and m04_scan_api_callbacks. Vanilla runs methods within one batch concurrently; separate batches isolate their temporary public callback-list fixtures.
