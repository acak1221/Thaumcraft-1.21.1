# g20nodes — aura-node time hypothesis

Worktree `/home/cyberfox-kali/wt/g20nodes`, branch `agent/g20nodes`.
Started clean at `38bdcb87`. No other worktree or main checkout modified.

## Scope and finding candidate

Read the private4.2.3.5 oracle `TileNode`, `TileNodeEnergized`,
`TileNodeConverter`, `AspectList`, and `Config`. Compared native lifecycle,
stabilizer rules, converter, energized reducer, and world-RNG call sites.

The broad hypothesis needs qualification: TC4 has no passive decay of stocked
normal nodes and no random success gate on recharge. PALE is not a node type.
The candidate mismatch is native `decreaseBase`: direct short-narrowed map
assignment bypasses the oracle setter's `AspectList.reduce` refusal of an
overdraw. A depleted key at base0 can therefore become base-1 on a1200-tick poll.
The new regression uses only public state initialization and production ticks.

## Validation

Runtime evidence pending. See `docs/audit/NODE_FINDINGS.md` for the explicit
inventory and test entry points. No client, multiplayer, NeoForge, or broad
distribution-frequency evidence is claimed by synchronous server-ticker tests.
