# Research settings and login snapshot — 2026-10-20

Original specification: TC4 Config and PacketConfig, GuiResearchBrowser's research mode branch,
ResearchManager's scan cap and Wuss research rewards. Seven server facts now use
`config/thaumcraft.properties`: cheat-sheet permission false, warded stone true, mirrors true,
hard nodes true, Wuss false, research difficulty 0, aspect total cap 100. Missing defaults and
malformed-value fallbacks are distinct: malformed difficulty becomes 8; malformed warded-stone
permission becomes false, matching the original Forge getters.

Difficulty -1 purchases every research, 0 purchases Secondary research, every other signed integer
uses notes. Purchase and note issuance validate the same policy on the server; browser actions and
frames read the login snapshot. Scan awards retain the original signed-cap soft-floor arithmetic,
including zero/negative caps. Wuss suppresses research/sibling warp rewards. Disabled research
conditions also gate automatic login restoration.

The server loads settings before research/knowledge initialization and sends them before the
knowledge login snapshot. Client state is confined to its recorded client thread and cleared on
disconnect; an integrated server keeps its separate authoritative record. There is no client-to-server
configuration receiver. Settings are restart-scoped. The compact payload has five flags and two
signed VarInts; it rejects unknown flags/truncation. Unlike the old byte difficulty field, full integers
avoid client/server mode disagreement for unusual configured values.

Pure tests cover missing/malformed defaults, all payload flag combinations and integer boundaries,
client/server thread separation, easy-primary/hard-secondary issuance/payment, Wuss completion,
and signed scan caps. Native packet delivery/client presentation still require an inspected client
session. The cheat-sheet and hard-node permissions are defined here; their remaining consumers
are separate ongoing content units and are not accepted by these settings tests.

Validation is recorded with the next integrated commit/CI run, not inherited from d508ada.
