# Original warp deltas and notification HUD — 2026-10-20

Source implementation; actual target compilation/runtime and inspected client evidence
remain pending. Oracle: original PacketWarpMessage, Thaumcraft.addWarpToPlayer /
addStickyWarpToPlayer, ResearchManager.completeResearch510–526,
CommandThaumcraft.addWarp/setWarp, PlayerNotifications and REHNotifyHandler.

Warp feedback is a real native S2C custom payload, original requested int delta
followed by signed byte channel, exactly five bytes. It does not update knowledge
or infer delta from a later snapshot. Server mutations still own their existing
native save/sync path. Command ADD sends its requested amount even after clamping,
including zero; command SET sends no delta packet. Extra warp arguments select
sticky before constructing feedback. Permanent and positive sticky gains whisper
locally at volume0.5/pitch1; temporary gain has no whisper. Sticky loss has its own
text, no whisper. Permanent/temporary loss and every zero delta intentionally
produce no popup: their removal translations are unreachable in the original
outer positive guards. Unknown signed-byte kinds retain the original positive
TEMP fallback instead of inventing a registry index.

Actual consumers are command ADD; newly committed ordered research from admin
grants, paid Secondary purchases, learned physical notes and normal/cheat book
unlock passes; positive crafted-item warp; actual zombie-brain completion;
infusion instability and temporary-warp utility. The old actionbar feedback and
server-side duplicate whispers are replaced only at those feedback sites. No
research, warp or item is awarded by a packet. Research emits the original
PERM ceil(warp/2), then sticky floor(warp/2) separately for each newly learned
key. Existing successful-operation key sets or before/after completed-key
snapshots determine which keys completed. Known/refused operations and Wuss
completion cannot replay feedback or award warp a second time.

The original notification queue is now an actual native client HUD consumer,
not an actionbar approximation. Text is drawn half-scale at the lower right,
8px row spacing, original alpha/shift calculations, 16-cell particle row5
entrance animation and optional colored8px images. Creation is one quarter of
the delay ahead; the first entry gets a half-delay expiry bonus. Queued entries
retain their original creation instant while extending expiry until reaching
the front, including original expired-prefix/strict-boundary behavior. Duplicate
messages are not collapsed. The default delay5000ms and visible rows15 match
original Config; both live in restart-scoped `thaumcraft-client.properties`
`notification_delay` / `notification_max`. Delay<4 (quarter-delay zero division)
and negative counts fall back to defaults, a bounded configuration crash fix.
World identity/disconnect reset clears transient queue entries; no notifications
can cross into another server session. HUD restores pose/color state.

Meaningful verification sources:

- WarpNotificationRulesTest covers positive/negative/zero channels, unreachable
  permanent/temp removal branches and unknown signed-byte fallback.
- NotificationQueueTest covers first-entry bonus, pinned tails/original creation,
  expired-prefix transition, strict expiry boundary, duplicates and reset.
- WarpNotificationRuntimeChecks uses a real connected actor and registered
  dispatcher, captures actual ClientboundCustomPayloadPackets and checks exact
  wire order. It covers command clamp/requested delta/zero/set/extra-tail kinds,
  actual admin warp3 split2/1 once, real paid COREBUTCHER permanent1 purchase and
  refusal, physical note consumption with declared trusted solved-state INPUT,
  actual cheat-sheet single missing key/replay and Wuss. Native tests do not
  prove transport into a second client or inspected HUD/audio.

CI04208de exposed a fixture transport mistake: Architectury13.0.11's actual
NetworkAggregator.collectPackets wraps typed payloads in BufCustomPacketPayload
before Fabric sends ClientboundCustomPayloadPacket. Inspecting only a direct
WarpMessagePayload instance incorrectly ignored every real outbound message.
The native fixture now matches the actual packet type ID and decodes its actual
five-byte wrapper body through the production codec, rejecting trailing bytes.
All requested-delta, clamp, ordering and replay expectations remain unchanged;
the production command/network/gameplay sources did not change for this fix.

PacketAspectPool flying aspect icons, research popup/highlight/clue messages and
other original notification consumers are separate remaining client units.
This change provides their real shared text/image queue without claiming them
implemented or seen. No full client, multiplayer or survival acquisition replay
is inferred from server checks.
