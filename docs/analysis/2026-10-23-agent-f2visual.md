# f2visual — 2026-10-23

Branch: `agent/f2visual`; baseline `0464d084`. Initial status clean. Worked only
in `/home/cyberfox-kali/wt/f2visual`; read the explicitly permitted private oracle.
No Gradle, client, compilation, server or visual acceptance run.

## Outcome

Closed visual finding R4 in implementation: advanced stabilizer item callback
registered on Fabric and NeoForge, stack guard accepts it, and both item/world
rendering use advanced red piston overlays. Active advanced world bubble is red.
The original uses **the same geometry**, not a separate advanced mesh:
`BlockStoneDeviceRenderer:90-92` -> `TileNodeStabilizer:23-25` ->
`TileNodeStabilizerRenderer:38-83,95`. Both metadata values draw `lock` plus four
`piston` instances from one OBJ; lock2 changes overlay/bubble colour only.
No basic-colour alias or placeholder model was introduced.

## Sweep and evidence

`docs/audit/VISUAL_FIXES.md` records oracle citations, per-asset and per-source
tables, and explicit limits. It is reproducible with:

```sh
python3 scripts/porting/audit_visual_registration.py > docs/audit/VISUAL_FIXES.md
```

The script completed successfully, including assertions for both registrations,
item/world advanced routing, independent byte colour constants and the retained
OBJ's 28 lock / 12 piston triangles. This is source-contract validation, **not**
executed Java rendering. Basic and advanced item JSON display transforms were
also read and are equivalent. `git diff --check` passed.

Counts: 30 direct builtin item models, 995 inheriting models (990 wand override
variants plus five jar/template descendants), 1,025 total. All live owning items
have callbacks on both loaders after this fix. 188 modern renderer/helper source
files: 178 have production references; ten remain unused/superseded. Their full
list and replacement paths are in the audit. None warrants registering an unused
fallback or an obsolete model just to make the count zero. Archived 1.12 tree:
18 direct builtin assets and 181 renderer/helper files, separately enumerated,
not compiled or newly registered on either modern loader. Test sources are also
listed separately, not counted as production renderers.

## Handoff

Leader must compile and inspect Fabric first, then validate NeoForge parity.
Inspect advanced/basic items in GUI, both hands, ground and frame contexts and
active/inactive placed blocks; check advanced red overlays and red bubble versus
basic white, including interleaved rendering to catch colour leakage. No runtime
or seen-on-screen acceptance is claimed here. Existing S1/S2 suspected findings
from the prior audit remain outside this registration task.
