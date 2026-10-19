# Use options data prerequisite — FINAL, 2026-10-19

Implemented data source only, **not compiled/tested, registered or connected Use8 gameplay**. Authority: [final Use contract](2026-10-19-golem-use.md), including its correction that the first flag is **Block / Empty space**, not entity targeting. No WIP, Entity/Goals/Menu/GUI/Placer/registry/recipe/client/platform/stock/test sources were read or edited for this task.

## Frozen files / API

- New `common/src/main/java/thaumcraft/common/items/golems/GolemUseOptions.java`.
- Updated only `common/src/main/java/thaumcraft/common/items/golems/GolemData.java`, plus this new note.

```java
public record GolemUseOptions(boolean emptySpace, boolean leftClick, boolean sneaking)
GolemUseOptions.DEFAULT // false,false,false = Block / Right click / Not sneaking
GolemUseOptions.CODEC
GolemUseOptions.STREAM_CODEC
options.isDefault()
options.supportsDefaultBaseline() // exactly all three false; pure future runtime gate
```

The three booleans are independent ordinary source features, **not upgrade-gated**, not Fill/Guard flags and not stock. Standalone nondefault values are constructible and persistable; `supportsDefaultBaseline` merely supports a future deliberately bounded runtime and grants no action permission.

## Strict JSON/NBT format

Standalone example: `{"empty_space":true,"left_click":false,"sneaking":false}`. Every field is optional and defaults false **only when absent**; `{}` is DEFAULT. False fields may be omitted on encoding. Unknown/future/duplicate keys, non-objects, malformed/present-null flags and wrong types are rejected; there is no entity-target key or silent future-field stripping.

Flags compare against the DynamicOps canonical boolean representation, rather than its coercing boolean getter: **JSON true/false and native NBT byte0/1** are accepted. JSON0/1/0.0, strings, NBT Int/Short/Float/Double values and Byte values other than0/1 are rejected. Optional-field values are checked before record decoding, so a malformed flag never silently becomes false. RegistryOps delegates to the underlying canonical representation; no world or registry lookup is needed for options.

## GolemData integration / intentionally inactive core

- Appended sixth record member **`GolemUseOptions useOptions`**, accessor `useOptions()`, JSON/NBT optional key **`use_options`**, default DEFAULT. Old payloads missing the key retain old behavior; explicit `"use_options":{}` is also valid. The existing **two-argument and full five-argument constructors remain** and supply DEFAULT. The new canonical six-argument constructor accepts options; null is rejected. Equality/hash include options.
- Outer strict known-key validation now admits only this one additional key and explicitly parses a present value **before FIELDS decoding**. Malformed nested data cannot fall back through optional defaults. For every **non-Use core**, nondefault options are rejected by construction and codecs, **not zeroed**.
- **`USE=8` is vocabulary only. `isSupportedCore` still omits8.** Consequently all currently valid GolemData must have DEFAULT options, and core8 is rejected by constructor/JSON/NBT/stream even with DEFAULT. Standalone nondefault options do not bypass that admission rule. No core/item/recipe/menu/action was registered or enabled.
- Straight dormant guards for future core8 require **EMPTY filter→wanted0, populated filter→wanted1, Fill=false**. These cannot activate it. Root must enable8 only with the real runtime and explicitly gate supported option combinations; ordinary nondefault modes remain unsupported by a first DEFAULT-only worker, not falsely reassigned to upgrades.
- Preserved ROOT **ALCHEMY6/BUTCHER9** admission and empty-filter/count0/Fill=false baselines. **LIQUID5** still allows only slot0's pristine water-bucket exemplar, all wanted counts0, other filters empty and Fill=false. All six defensive full-component ItemStack copies, normal EMPTY0 defaults, count bound256, bounded64 markers/deduplication, native position/face encoding and existing validation remain. AspectCargo/WaterCargo are **not added to GolemData** or read here.

## Native stream / remaining integration

`GolemUseOptions.STREAM_CODEC` writes **three actual boolean fields** in emptySpace/leftClick/sneaking order. Decode requires canonical0/1 bytes, refusing arbitrary nonzero bytes rather than treating them as true. `GolemData.STREAM_CODEC` appends those three bytes **after the existing marker list**, leaving existing fields/count bounds intact. Both native endpoints need the same updated build; old packets lacking the tail are not a cross-version protocol promise. No new version/token/journal/provider fields were introduced.

Root still owns real core8 admission, Entity configuration/save/normal recall, menu/client synchronization and controls, actor/stock/protection/action execution and recipes. Future copies/reconfiguration must **forward `useOptions()` into the six-argument constructor**; the compatibility five-argument overload deliberately defaults and is not itself future option-retention wiring. No such caller/Entity/menu/placement changes were made here.

Source readback only: **no tests added/read/run, Gradle/compiler/Java/game/Python/worlds/agents or proof/source-pin manifests**. Root performs the actual Fabric compile after Butcher + this FINAL. Codec/stream round trips, integration and runtime behavior remain unexecuted; this unconnected data prerequisite is **not Use8 completion or PASS**.
