# Focus pouch and original candle colors

This unit uses the TC4 4.2.3.5 oracle. It is a native1.21.1 implementation;
the1.12 legacy source is not its gameplay authority. Build and runtime results
are pending the root agent's centralized checks. The fixtures below are declared
inputs, not proof of natural acquisition, network dispatch from a physical
client, or visually inspected rendering.

## Original pouch contract

`ItemFocusPouch.getInventory/setInventory` stores eighteen independent physical
item entries. `InventoryFocusPouch` has a one-item insertion limit and accepts
`ItemFocusBasic`; preexisting foreign entries are still visible and can be
withdrawn by an ordinary click. `ContainerFocusPouch` has18 pouch+36 player
slots, protects the selected hotbar slot at45+selected, inserts in ascending
pouch order, and returns to the player in reverse inventory order. Both
directions of quick-move refuse foreign items. The original GUI is175x232 with
the retained `gui_focuspouch.png` art and source-slot marker.

`ConfigItems` registers only `ItemFocusPouchBauble`, a BELT; the base
`ItemFocusPouch` remains an API class. There is one native `focus_pouch` item,
not a second normal-pouch item. `ConfigRecipes:253` is the FOCUSPOUCH arcane
recipe LGL/LBL/LLL, leather/gold/mundane belt, costing10terra10ordo10perditio.

`WandManager.changeFocus:444-523` scans four Baubles slots followed by the36
main slots. Pouch contents can be selected and an existing installed focus
returns to a pouch before loose main-inventory storage. The selected physical
focus is extracted before returning the old one, so a full-storage swap can use
its freed cell. Removal with no destination retains the installed focus.
Creative still moves the real focus. There is no vis cost.

## Native pouch behavior and authority

`ThaumcraftFocusPouches` registers the sole Bauble item and native extended
menu. `DataComponents.CONTAINER` provides native persistence and ItemStack
networking; no parallel item/NBT inventory is authoritative. The API returns
copies and retains complete foreign saved entries. A payload beyond eighteen
slots remains unchanged and refuses activation, rather than silently deleting
the extra entries. Native GUI insertion still accepts only one focus.

The server menu binds the actual connected actor, thread, hand, object identity,
and full pouch component snapshot. The source hotbar slot and offhand swap40
are protected. Every mutation is persisted immediately to that physical item.
Replacing the hand item or changing any pouch component invalidates the mirror;
stale clicks and close cannot withdraw from or overwrite another physical
pouch. An open menu/cursor operation cannot race focus selection. This replaces
the original unconditional stale close snapshot write with explicit native
ownership checks.

`FocusSelectionSources` supplies both radial previews and validated physical
addresses: loose0..35, inventory pouch1000+slot*18+cell, worn pouch2000+slot*18+cell.
Original loose/removal packets retain their two bytes. Pouch intent is four
bytes, hand/marker127/unsigned-short address. Address gaps, illegal hands,
extra bytes and extended aliases for loose slots are refused. No client item,
component, player identity, destination, vis amount or price is accepted.

The production selection receiver plans with independent complete inventory
and accessory snapshots, extracts once, then returns to worn pouches, main
pouches, and finally the first empty main slot. It validates all original
physical objects/components before the callback-free commit. A worn pouch's
container component changes in place; the operation does not manufacture
unequip/equip callbacks. Inventory/accessory synchronization and success sound
occur after all material and wand changes. Failed storage has no drops or
deferred overflow path. The existing native radial continues to display
physical equal-key duplicate stacks; original TreeMap preview erasure is not
reintroduced.

`FocusPouchRuntimeChecks.run` exercises actual native click packets and real
registered recipes with an owned connected actor. It checks immediate saved
contents, slot17 NBT/native-stream preservation, source hotbar/offhand locks,
reverse quick-move, foreign64 ordinary withdrawal, stale identity/component
close, oversize refusal, native belt equip, full-storage worn swap, pouch-first
return, atomic full refusal and actual cursor/menu/late-hand refusals. Direct
custom selection receiver invocation is explicitly not a wire-dispatch claim.

## Original colored candle contract

Original `BlockCandle` exposes all sixteen cloth metadata colors, from white0
through black15, using `Utils.colors`. Each has hardness.1, cloth sound,
light int(.95*15)=14, the6..10 by8-pixel selection box, no physical collision
or sturdy support face, top-solid support below, own-color drop, smoke/flame,
and `IInfusionStabiliser` eligibility.

Original dye recipes accept one WHITE candle and one matching ore-dictionary
dye; colored candles are not directly recolored. The wildcard bleach recipe
accepts any candle, including white, plus bone meal, not modern white dye.
Native blue/black/brown tags retain lapis/ink-sac/cocoa-bean alternatives while
also accepting their modern flattened dye item. Native names use
`<dye>_tallow_candle`. `ThaumcraftColoredCandles.byMeta(0)` and `blockByMeta(0)`
reuse the existing white registry identity; only fifteen new identities are
registered. The existing white block class and recipe remain intact.

`ColoredCandlesRuntimeChecks.run` checks actual native crafting-table result
slot takes for every color and every bleach source, the three original raw
pigment alternatives, wrong-source refusal without input consumption, every
physical BlockItem placement, original RGB/light/collision/support/stabilizer
properties, native color loot, and exactly one correct drop after support
destruction. Client tint providers color only wax faces; retained wick texture
is untinted. Original coordinate-seeded wax-drip geometry is not reproduced by
the existing native candle mesh and remains a visual parity limitation.

## Integration and remaining verification

Common hooks: `ThaumcraftHellbatFoci.register()`,
`ThaumcraftFocusPouches.register()`, `ThaumcraftColoredCandles.register()`.
Client hooks: `ThaumcraftFocusPouchesClient.init()` and
`ThaumcraftColoredCandlesClient.init()`. Initialize native items before Pech
trade tables resolve their suppliers. Original Pech candle entries use0..14;
the black15 identity is implemented for the block's complete original catalog.
Root owns shared research/book aliases, localization, registration and test
wrappers. The added tooltip translation is `tc.port.focus.pouch_slot`.

Pending: centralized compilation, all native runtime methods, actual two-way
client pouch screen and radial accessory synchronization, retained artwork/tint
visual inspection, and end-to-end natural acquisition of leather/belt/research
and all pigment/candle inputs. No PASS result is asserted here.
