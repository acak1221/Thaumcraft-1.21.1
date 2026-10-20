# Original RP, discovery, clue and research feedback — 2026-10-20

Implemented source group; target compilation/native runtime/client inspection remain pending. The oracle is
TC4 PacketAspectPool / PacketAspectDiscovery / PacketResearchComplete,
PlayerNotifications, REHNotifyHandler, GuiResearchPopup and every original server
callsite, including ScanManager.checkAndSyncAspectKnowledge420–440,
TileResearchTable164/172/411, WarpEvents217–253 and ItemResource metadata9.

The native full KnowledgeSync snapshot remains the sole client knowledge
authority. The old signed-short amount/total feedback wire is preserved, but
never overwrites an authoritative raw-int snapshot: raw33000 has original
queried/wire SHORT−32536. Reapplying that old total would replace raw33000 with
−32536 on the modern client. This is an explicit forced protocol-integration
adapter for fresh1.21.1 worlds; the original truncation/raw-storage mismatch is
recorded, not silently normalized. Requested signed-short delta, canonical
aspect validation and original positive-only HUD condition remain intact.
No feedback payload can award research, RP or warp. Native server accounting,
query quirks and point/prerequisite/material admission are unchanged.

Actual gain packets create one original half-scale text/image queue message and
one aspect flight per positive requested SHORT point. Flights start at .4..6 of
screen width/height, with independent0..999ms future birth and1500ms lifetime.
The quadratic path ends at(right edge,−8), using original truncated double/float
coordinates, .3/.66/.34 alpha segments, .075×alpha geometry, aspect-color tint
and .66 alpha. The book icon appears16px at top-right using maximum active
flight alpha. Pickup sound is volume.1, pitch .9..1.1, wall-clock100ms throttle.
Discovery has separate gold text/image, .2-volume/.5..7-pitch sound and
ASPECTS/BASICS highlights. Unknown keys are ignored. Timing/coordinates use
original precision, including double×float truncation and fractional GUI scale.

Payload selection is path-specific:

- Scans interleave actual newly discovered aspect and positive award packets
  in the native action-local observation order, then the actually created clue.
  Duplicate/refused scans emit no reward. No holder/world cache is introduced.
- Actual table combination RP inputs send delta0; only output1 or first
  discovery3 produces flight feedback. Negative Secondary purchase costs send
  negative deltas; committed normal/sibling completion has its own notice.
- Note placement, erase refunds and copying send delta0, including a real +1
  refund: original refund sound is server-owned, not an extra gained-RP popup.
- Mana-bean use and deconstruction claim send +1 without inventing a separate
  aspect-discovery notification. Warp research keeps every actual repeated
  random primal award and its momentary signed total, not an aggregate; newly
  reached @BATHSALTS/ELDRITCHMINOR/MAJOR gates have their original notices.
- Original research completion notices come from successful purchase, learned
  physical notes and actual clues/warp gates/PrimalCharm, not blanket admin/book
  synchronization. @KEY produces green clue text and learn .2-volume/
  1..1.1-pitch sound; nonvirtual actual keys queue their normal research toast
  and key/category highlight. Virtual keys produce no toast. Packet handlers
  never complete research again after native full sync.

The real top-left toast uses the exact original160×32 vanilla1.7.10 achievement
frame crop, literal English `Research Completed!`, original name-width125
scaling and actual native item/resource icons. Each entry lasts3000ms with
quartic36px entrance/exit, exact expiry boundary and a blank transition frame.
Client HUD draws while no screen is open; a narrow Screen.renderWithTooltip
TAIL mixin draws above actual inventory/book screens, matching original
RenderTick END ordering. Queue advances exactly once per rendered frame.
New normal keys update the browser's remembered original coordinates and
key/category particle-row5 highlights; closing the browser clears highlights.
Pose/color/depth state is restored. Dedicated bootstrap never loads client code.

Asset provenance: Mojang Minecraft1.7.10 client JAR SHA256
`a4fc2284657544e0f4bcc964f927c2fda3e3a205178ed1d5d58883aaf9780cce`,
entry `assets/minecraft/textures/gui/achievement/achievement_background.png`,
PNG SHA256 `b99ec87194c344874949bcf3dcc40745128800c82def4a75c48b6d38e1694575`.
Exact crop x96/y202/width160/height32 becomes
`assets/thaumcraft/textures/gui/research_toast.png`, current PNG SHA256
`42b1722ce07f63d2b1a20f2d10e84aa550a93f8ee218774b47c7ba13dc687c79`.
Only that small frame is retained; neither JAR nor whole atlas is committed.
`scripts/porting/extract_research_toast.py` reproduces it from verified inputs.
THIRD_PARTY_NOTICES attributes this retained Mojang asset independently of TC4.
Viewing the extracted asset is not inspected evidence of the running HUD.

Meaningful verification sources will distinguish pure motion/boundary tests,
actual native server packet/resource transitions, real client packet consumer
checks, actual inspected HUD/audio, multiplayer and survival acquisition.
None of the last three is implied by headless source compilation.

The original metadata9 knowledge fragment is also a real consumable, replacing
its plain native Item template. Native survival RMB pays exactly one physical
fragment, preserves remaining count/components and awards original independent
WORLD nextInt(2)+1 to each primal in the original registration order (the six
native TC4 primals). Creative retains its original free use; modern offhand
pays its own actual stack. No research, clue, scan, warp/counter, cooldown,
point cap or difficulty prerequisite is invented. All six award payloads retain
their exact momentary signed totals. Canonical connected actor/hand/count/
components/creative mode and complete knowledge snapshot are revalidated after
RNG planning; physical shrink and preallocated knowledge replacement are adjacent
before sync/notifications. A private native-use guard blocks RNG reentry. This
forced native authority fence prevents detached hand replacement from creating
free points; it preserves ordinary original draw/order/charges and never refunds
or replays an already committed fragment if feedback fails.

Verification sources include four pure motion/boundary/precision cases, actual
server scan/discovery/clue, live table input/output/copy/refund and purchase
packets, actual ten-tick mana-bean food completion, actual40-tick physical
Deconstruction work plus native reward-button claim/replay, full-snapshot raw33000
versus old signed total, and actual WarpEvents per-draw/gate/clarity payloads with
unchanged independently replayed native RNG. KnowledgeFragmentRuntimeChecks
uses real registered Item/game-mode survival/creative/offhand use, six actual
world draws and physical counts/components; an existing scoped native world-RNG
fixture proves reentry and physical hand replacement cannot yield a free grant.
These are declared resource/RNG/solved-note inputs, not progression acquisition.

Root integrated both actual receivers/HUD+screen hooks and KnowledgeFragment registration, and retained exact Mojang crop attribution. Final combined local compile and704JUnit/84Python passed; the notification and fragment gameplay scenarios are required in the243-case remote manifest and await execution. No physical-client toast/audio acceptance is inferred.
