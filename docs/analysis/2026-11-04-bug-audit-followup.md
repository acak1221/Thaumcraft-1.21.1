# Follow-up bug audit — 2026-11-04

This continues the owner's bug-finding goal after commit `72bbd52b`. The first
batch and its exact inputs remain in [the earlier report](2026-11-04-bug-audit.md).
This batch fixes item loss through a normal container action, negative Magic Box
extraction, and invalid senders in both research request handlers.

## Hungry Chest copied research loss

A research table can legitimately produce a two-item copied discovery stack even
though the item has a normal stack size of one. The existing shared menu adapter
moves that whole stack into an empty destination. Native `ChestMenu` debits the
source first; `TileChestHungry.setItem` then rejected count two and discarded both
paid notes. Original `TileChestHungry.java:62–67` accepts count two: it only clamps
to the container's 64-item limit, rather than the item's stack size.

The Hungry Chest setter now preserves whole `ItemResearchNotes` stacks using the
same exception as the existing native chest adapters. Other oversized stacks and
unstackable items retain their existing refusal guard. Contact ingestion and its
stack limits are unchanged.

The existing `deviceWorldHungryChestCollectsPhysicalInput` test now performs a
real paid research copy (one paper, one black dye and the original aspect prices),
passes native quick-move packets through the actual research and chest menus,
saves/loads the registered chest block entity, and moves the complete stack back.
Count two is acquired by that payment. Count 128 is an explicit codec-boundary
fixture, not a claim of 127 additional acquisitions. It follows the port's
existing whole-stack discovery preservation policy instead of truncating at the
original chest's limit. The test also covers full-chest refusal, components,
cursor/drop conservation and the unchanged stone65/pickaxe2 rejection controls.
The previous physical-contact iron-ingestion scenario remains in the same test.

## Magic Box negative extraction

`TileMagicBox.removeItem` passed a negative quantity directly to native
`ItemStack.split`. Subtracting a negative request increased the stored count;
`Integer.MIN_VALUE` could overflow and erase it. The guard now returns an empty
result before resolving or changing the inventory for nonpositive quantities.
Oracle `TileMagicBox.java:64–79` has the same defect (split at line 72), so this is
an intentional resource-conservation correction.

The existing `magic_box_chain_owns_twenty_seven_slots_per_box` native case covers
zero, negative and minimum-integer requests without changing stack identity,
saved NBT or the chunk dirty flag. Positive extraction preserves components,
marks the debit dirty, and survives save/load with removed plus remaining count
equal to the original two. Maximum-integer extraction removes only the remainder.
The other direct split paths in shared tiles were checked; no additional
unprotected path was found in that bounded review.

## Research purchase and note sender authority

Both shared request handlers accepted captured `ServerPlayer` objects without
checking whether they were still the live, connected player. Funded spectator
or dead senders could purchase research or generate a note; an old pre-respawn
object could still mutate its retired knowledge and inventory.

The handlers now reject dead, spectator, removed, disconnected, noncurrent or
noncanonical senders before planning, puzzle RNG, RP/paper/ink payment, delivery
or feedback. Rejection uses the existing `UNAVAILABLE` result. A current live
player, including the replacement after respawn, keeps the original admission
rules and prices; no held-book requirement is added.

Original `PacketPlayerCompleteToServer.java:77–82` resolves the player afresh
from the addressed world. It predates the native spectator mode. The new guards
preserve current-sender authority and apply the modern read-only spectator rule,
rather than trusting a queued object after its lifecycle ends.

Five existing Fabric wrappers now use the existing connected-player fixture in
place of detached mocks; their price, replay, prerequisites, persistence and
last-ink assertions are unchanged. The new shared native case exercises funded
live controls, spectator/dead refusals, actual `PERFORM_RESPAWN` packet handling,
retired/current actor snapshots, and successful requests by the replacement.
Direct shared-handler calls do not claim network C2S dispatch.

## Verification

The first combined run passed common JUnit and seven of eight focused server
cases, but failed the new respawn assertion. The test had called
`PlayerList.respawn` directly; native `ServerGamePacketListenerImpl` performs the
final `connection.player` reassignment. The fixture now invokes the actual
`PERFORM_RESPAWN` packet handler. All original assertions remain; production code
was not changed to accommodate the fixture. The first 7/8 run is rejected and
does not count as accepted native verification.

Final `:common:test :fabric:build :fabric:runGameTest` with the eight focused
targets passed after the final source changes. Common JUnit: 492 suites, 2,912
cases, 2,907 passes, five existing oracle skips, zero failures/errors. All eight
required dedicated-server cases passed; worlds saved and the server stopped
normally. Fabric production/test isolation and `git diff --check` passed. The
scope check reports zero stale review fingerprints.

Final Fabric JAR: `Thaumcraft-fabric-0.1.0-alpha.24.jar`, 16,027,858 bytes,
SHA-256 `e9a9d65027ef3ee911773cd90b5638104a790738f5b80f929f0c5fc2fbcdc5ab`.
Exact source hashes, fixtures, test names and rejected/accepted run distinctions:
[receipt](../porting/bug-audit-followup-2026-11-04.json).

No new visual/client, survival acquisition, remote multiplayer, packaged
production-server or NeoForge acceptance is claimed by these server checks.
Earlier JEI evidence retains the exact inputs recorded for `72bbd52b`.
