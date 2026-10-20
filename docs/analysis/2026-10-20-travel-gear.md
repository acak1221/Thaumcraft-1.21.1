# Traveller boots and Hover Harness native contract

Implemented native source, not yet accepted by compile or runtime checks.
Root authorized activation after checkpoint b7c7845. No Gradle/build/runtime PASS
is claimed; root owns centralized builds and shared wiring.

## Public identities and entry points

* `ThaumcraftTravelGear.register()` registers the own armor materials,
  `BOOTS_TRAVELLER`(`thaumcraft:boots_traveller`),
  `HOVER_HARNESS`(`thaumcraft:hover_harness`), native `HARNESS_MENU`,
  `HOVER_CHARGE`(persistent0..360) and `HOVER_ENABLED`(persistent boolean).
* `HoverNetworking.init()` registers one empty sender-bound C2S toggle intent
  and the actual controller lifecycle. No player id/slot/material/permission is
  accepted. Native equipment components and native abilities synchronize back.
* Add `TravelGearTick.tick(player)` exactly once to existing
  `EquipmentEnchantments.playerLivingTick`: immediately AFTER its existing
  `PlayerAccessories.tick(player)` and BEFORE Repair/Haste/Runic. It runs boots
  before chest armor, matching original feet/chest order. Do not add another
  accessory tick. The real Girdle owner already supplies its own worn callback.
* Add one common mixin `PlayerTravelGearMixin`: actual Player jump tail, air
  movement return, airborne mining-speed return and save-output permission
  provenance. Existing `ServerGamePacketListenerFloatAccess` is reused.
* Common client `ThaumcraftTravelGearClient.init()` registers H, the actual fuel
  screen, viewer-aware tooltip and real client movement/sounds/effects. Fabric
  `FabricTravelGearClient.init()` registers the exact custom armor renderer.
  `ThaumcraftTravelGearClient.reset()` belongs to actual client resource reload
  and level teardown, alongside existing client reset hooks.
* `shared-patches/WispLightningShape.patch` is the approved overloaded shape
  constructor. Default Wisp ctor and RNG schedule are unchanged. Root approved
  and previous owner warp explicitly ceded this small shared edit; no active
  helper was edited during HOLD; the approved overload is activated now.

## Shared resources/book hooks

* Add `boots_traveller` to `minecraft:foot_armor` and
  `thaumcraft:enchantable/repair`, `thaumcraft:enchantable/haste`.
* Add `hover_harness` to `minecraft:chest_armor` and
  `thaumcraft:enchantable/repair`, `thaumcraft:enchantable/haste`. Original Haste
  also accepts the harness; its separate real Haste level changes flight drag.
  If any native application predicate currently restricts Haste only to feet,
  add the actual `ItemHoverHarness` class there; do not allow other chest armor.
* Research icon aliases `ConfigItems.itemBootsTraveller`→`boots_traveller` and
  `ConfigItems.itemHoverHarness`→`hover_harness`; recipe aliases
  `BootsTraveller`→`infusion/boots_traveller`,
  `HoverHarness`→`infusion/hover_harness`. Existing retained research parents
  remain ENCHFABRIC/INFUSION and BOOTSTRAVELLER respectively.
* Item display labels use retained original names; the new key translation is
  `key.thaumcraft.hover` (EN Activate Hover Harness; RU Включить парящий ранец).
  `container.hoverharness`, `tc.break.fly`, discount/aspect text already exist.
* Native raw fish ingredient explicitly includes cod/salmon/tropical/puffer,
  excludes cooked fish. Actual bellows native id is `thaumcraft:bellows`.
* Original worn textures are retained byte-for-byte under the native armor layer
  names. The custom harness renderer uses the original body PNG and complete
  retained OBJ/back PNG. Existing atlas directory sources already include the
  animated `items/lightningring`; no new placeholder atlas entry is needed.

## Runtime wrappers

* `TravelGearRuntimeChecks.boots(test)`, timeout200, batchm08_traveller_boots.
* `TravelGearRuntimeChecks.harnessFuelAndPermissions(test)`, timeout300,
  batchm08_hover_fuel. Manual calls drive actual native living steps and are
  declared as such; there is no simulated material counter setter except the
  explicit173-counter save/reload input used to distinguish persistence.
* `TravelGearRuntimeChecks.harnessMenu(test)`, timeout200,batchm08_hover_menu.

Meaningful assertions cover actual native equip/click/movement/jump, duplicate
recipe components/pristine centers/raw fish wildcard, generic Repair/Runic
eligibility, separate Haste acceleration, real water/ground/air state, real
harness-linked boots.03, exact361/289 fuel boundaries, paid final frame,
empty-jar refusal, real belt dependency, native float reset, temporary flight
save-NBT sanitization and actual reload, unequip/late source refusal, intrinsic
permissions and creative/spectator transitions, actual one-jar menu/cursor,
full native ItemStack component stream/foreign64 mirror and withdrawal,
stale-component close and oversize refusal. Direct toggle receiver invocation
is not physical-client network-dispatch acceptance.

## Original semantics and explicit native authority adapters

Sources read: ItemBootsTraveller, ItemHoverHarness, Hover,
Container/InventoryHoverHarness, GuiHoverHarness, ModelHoverHarness,
ItemGirdleHover, KeyHandler89-104, EventHandlerEntity278-286/312-346/621-623,
ConfigItems409-410/441, ConfigRecipes369 and HoverHarness line, Api SPECIAL52.

Original Boots350/rare/armor1, Harness400/epic/armor3 share enchantability25.
Boots have no ordinary material repair; Harness accepts gold. Runic base is0.
Boots forward ground.055F(water/4), air.05F or REAL Hover.03F; fall subtract.25F
unclamped, jump adds(double).275F. Previous client step height is captured once
and restored only on sneak/removal, not idle or flight. Haste remains separate.

Original Harness fuel is one physical Potentia jar, outer SHORT counter,360
successful increments and361st expenditure; Girdle uses288/289. The last unit
returns false AFTER consumption, mode goes off but native flying retains that
paid closing frame. Idle worn Harness multiplies server fall distance by.75;
active successful frames set it0 and reset the native float kick counter.
Original Haste drag is min(1,.7+.075*level+(.21 if Girdle)). Held RMB opens the
fuel menu; it does not automatically equip. Airborne Hover mining multiplies
the already air-penalized speed5. Outer Lands disables flight every20ticks.

The original arbitrary-player-id flight packet is replaced by sender-bound
actual gear/fuel authorization. Native temporary mayfly rights are captured
and restored with provenance, never persisted as permanent ability privileges:
save-output abilities contain intrinsic pre-grant rights while the physical
gear/counter/mode remain saved. Native client movement consumes synchronized
server state; it does not spend a second authoritative material copy. Per-player
runtime identity is weak and never keyed by reusable entity integer ids.
Creative/spectator game-mode transitions retain their intrinsic permissions.

Pending acceptance: root compile/JUnit/runtime, actual physical-client H/menu/
equipment stream, client step restoration, full renderer/OBJ/rings/bolt visual
inspection and natural survival acquisition. No original source/JAR was copied
into the repository; original geometry is read from retained existing assets.

Integrated shared hooks, native armor/haste/repair tags, actual Hover Harness haste category, research aliases and EN/RU labels are now active. Local final common/Fabric source-set compilation and704 combined JUnit tests passed after all fixes;84Python and zero missing resources passed. Three actual native gear scenarios are required in the243-case remote suite but have not yet run. Physical client movement/menu/OBJ/rings/bolt presentation and multiplayer/survival remain unverified.
