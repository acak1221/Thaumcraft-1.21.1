# Advanced Alchemical Furnace — native gameplay unit

Implemented source group, pending root target compilation/runtime and inspected
client evidence. Original oracle audit is recorded separately in
`2026-10-20-advanced-alchemical-furnace-audit.md`. Sources are TC4 4.2.3.5
`WandManager744–807`, `TileAlchemyFurnaceAdvanced`, its nozzle, original
`BlockAlchemyFurnace`, renderer, `ConfigRecipes100/291`, and
`EventHandlerWorld163–167`.

The native advanced construct is a real arcane recipe: `VAV/APA/VAV`, four void
ingots, four ordinary constructs and one primordial pearl, producing FOUR blocks
for aqua10/ordo30/terra10 whole vis. Only that exact server-native recipe retains
its SAME physical pearl in center slot4; all modern components/count/reference
remain there. Retention is in the paid matrix commit, reproducing the original
craft event's pre-shrink increment. It is not an item-wide container remainder,
inventory-first return or free-output hook. Other pearl uses still consume it.

Original formed metadata0 also remains a real native creative-tab block item.
Its separate closed native BlockItem factory binds only ONE empty working core
with persisted `StandaloneCreative` mode; it never auto-places the full structure.
That core draws real power, processes physical items and preserves its mode/stock
on reload, independently of the paid17-part survival formation. A creative-sourced
item handed to survival still consumes one item through ordinary native placement;
there is no invented creative-only research/permission gate. Block-item components
do not carry saved slurry/power/owner NBT. Breaking it returns one NORMAL furnace,
not a reusable advanced core or stored slurry. Item rendering uses the actual
empty original OBJ through the native builtin renderer, without a fake ticking BE.

The normal ALCHEMICAL FURNACE, stoneDevice metadata0, is the lower center.
It is not a crucible: original `BlockStoneDevice257–259` directly constructs
`TileAlchemyFurnace`; `ConfigRecipes100/280` corroborate the conversion chain.
Eight advanced constructs surround it. Upper corners are four alembics and the
cross is four ordinary constructs. Upper center is unconstrained: it is neither
validated/replaced nor stripped of its BE/inventory. The original first candidate
normal-furnace search terminates on a bad first candidate rather than trying a
later valid one. Native permissions, exact17 loaded states/BEs and held wand are
validated, then original ignis50/aqua50/ordo50 crafting vis is paid before any
callback-bearing world installation. A partially interrupted paid installation
never refunds/replays or rolls back over callback replacements; its owned parts
retire through the same native coherent/orphan checks.

Nonempty original devices ARE accepted. The normal furnace's existing removal
hook durably returns its two physical input/fuel stacks, matching original
`InventoryUtils.dropItems`; its slurry/remaining energy are lost. Alembic removal
returns its physical label, while its essentia is lost, matching original
`BlockMetalDevice534–535`. Formation does not copy those old buffers into the new
machine or invent an empty-input refusal.

The working core has no fuel slot, GUI or inventory. A real dropped ItemEntity
collision consumes ONE actual stack unit, adds the full native object-aspect
resolver base+bonus list and debits2a heat/a perditio/a aqua. Mixed slurry holds
500 total. Missing energy, empty/unknown tags, overflow and transient processing
cooldown refuse without partial debits. Cooldown is computed from REMAINING
heat as `(int)(5F+max(0F,(1F-heat/500F)*100F))`. No bellows or redstone pause is
invented. Physical input debit and preallocated new pool/energy/cooldown commit
are adjacent before entity-data, sound or block notifications.

Every fifth actual core update decrements heat without a zero clamp, then draws
50 raw CV each of ignis/perditio/aqua when that pool is <=500. Requests are not
room-clamped: pools may reach550, and normal550 heat emits13. Native
VisNetHandler has a CLOSED final-core receive frame; preflight validates the
private exact current aspect/phase/scalar state, then actual root debit and raw
receiver credit occur before route pulses/FX. No generic callback or public
credit-plan capability is added. Removing the machine from the first pulse
cannot leave a detached late credit, refill the source or charge following
aspects. Existing workbench and amulet receive paths remain independent.

The four lower cross nozzles share exactly ONE immutable insertion-ordered core
map. Original one-time DOWN/UP/N/S/W/E initialization chooses the outward face;
only that side outputs, no input, no suction, minimum0, no extended tube.
Type/amount name the FIRST insertion key, never sample randomly. Native
EssentiaTransfers locks both the physical nozzle and shared core before any
source observations, prepares both endpoint replacements and assigns the debit
before notification. Another nozzle, core tick or reload cannot bypass that
same lock through an alias. Public container extraction uses the same executor.
Snapshots are defensive; negative extraction is refused. Empty or unavailable
ports return null/zero instead of original unbound int-unboxing NPE. Native
empty-list length0 is guarded because original empty AspectList returned[null].
The original comparator precedence quirk is preserved: only NOZZLES output
Boolean0/1 rather than0..15.

NBT preserves original signed SHORT heat/power1/power2/vis and mixed stock.
Live heat may fall below−32768; writing then reloading has original short-wrap
behavior. Processing/count/destroy remain transient. Unknown aspects, invalid
counts/duplicate keys, mismatched saved totals or impossible non-heat power are
quarantined rather than normalized into spendable stock. Client display packets
carry only original vis/heat; reading them never replaces the authoritative
server pool. Native light property forces0..15; ordinary reachable550 remains13,
while extreme positive short-wrap heat cannot create an out-of-range state.

The approved authority/save adaptation adds durable center+machine UUID custody
BEs to the original twelve non-BE shell pieces, without adding stock/inventories.
Only the exact paid formation's current factory position obtains that identity.
All17 cells must belong to the exact core. Unloaded terrain pauses, while loaded
missing pieces cause original material reversion even after a restart lost the
original transient destroy flag. Core break reverts loaded OWNED parts at once;
unloaded parts retain their own saved identity and revert when their owner is
known missing after reload. A new/replaced neighboring core cannot claim an old
machine's parts. Upper center remains untouched. Quiet known-shape retirement
writes are used when native UPDATE_ALL would query an unavailable neighboring
chunk; normal fully loaded neighbor updates remain native. This avoids terrain
force-loads and rollback over unrelated replacements.

Client code retains original OBJ Base116 and Tank64 triangles, four rotated
Tank groups, four original active/inactive model textures, actual slurry/heat
masked quads, original packed brightness literals and3×2×3 render bounds. Ambient
bubbles retain original144..150 atlas cells, lifetime15..19, age5 lift, rise0.005
and final half-speed tail; lava-pop remains chance1/50. Native minimal particle
settings suppress ambient effects. All particle draw blend/depth/cull/shader/
texture/color state is restored. Dedicated-server initialization has no client
class dependency. No visual inspection is claimed by source compilation.

Focused validation sources (not yet claimed passing):

- `AdvancedFurnaceRulesTest`: exact17-cell blueprint/unconstrained center,
  original post-debit cooldown and Boolean comparator/light13.
- `AdvancedFurnaceRuntimeChecks.run`: real menu and four-output retained pearl
  crafts, research/shortfall refusals, real paid wand formation, first-candidate
  quirk, nonempty input/fuel/label returns, untouched actual upper chest,
  renewable real node epochs/50-CV overshoot, native entity collision and one
  input, all four shared ports, defensive/negative/unbound cases, real standalone creative
  core placement/power/collision/save/normal-material drop, real NBT
  replacement, capacity-only refusal, negative heat/short wrap, every break type
  and restart with missing part.
- `AdvancedFurnaceCallbackRuntimeChecks.run`: a real vanilla comparator with a
  test-only nonfinal ComparatorBlockEntity probe triggers native notification
  reentry; exact paid stock/core lock blocks a second nozzle/tick/reload. A real
  relay pulse removes the core after raw source debit+receiver credit and checks
  no following-aspect charge. No final production class is subclassed.
- `AdvancedFurnaceUnloadRuntimeChecks.run`: remote paid machine crosses a chunk
  boundary; ordinary tickets/player logout cause ACTUAL old-BE retirement and
  getChunkNow absence for BOTH core and shell. A radius0 FULL core ticket retains
  its neighboring holder at ticket34, so the earlier fixture could not require
  old shell-BE retirement while retaining that core. After both truly unload,
  a radius0 ticket reloads a NEW core retaining its UUID and paid inventory;
  the unavailable shell pauses processing without forced loading. Breaking the
  core before reloading the NEW saved shell proves original orphan reversion.
  This is not loadStatic or a reflected visibility fixture relabeled as full
  unload. Timeout1200 covers all three bounded native phases. The corrected
  scenario is authored; acceptance awaits the next GitHub run.

Server tests explicitly provide raw materials, late wand energy, ordinary node
capacities and physical pearls. These are unit fixtures, not survival acquisition
or a full progression replay. Original boss/rare-loot pearl acquisition, full
client inspection, multiplayer and eventual NeoForge acceptance stay separate.

075df78 proved both real chunk unloads and the NEW paid-core reload, then failed at the quiet core removal's no-force-load assertion. The outer break already used flags18; a loaded inner part used flags3 and propagated shapes/neighbors indirectly into the unavailable shell chunk. Disassembly now preflights all17positions and uses quiet18 for ALL available reversions if ANY part is unavailable, retaining normal updates when fully loaded. The unchanged true disk-unload/custody/orphan fixture checks the actual quiet native onRemove path, not an ordinary player break with vanilla neighbor updates. Local compile/JUnit passed after this fix; native rerun pending.
