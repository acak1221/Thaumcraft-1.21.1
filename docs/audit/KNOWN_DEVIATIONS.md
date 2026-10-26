# Known deviations

Every entry here is a behaviour that differs from TC4 4.2.3.5 and is **accepted** rather than
closed. Each has the oracle citation and the reason it cannot or should not be changed. Nothing
here is a guess: an entry exists only after the oracle was read.

The scope ledger marks these in the row notes with `ACCEPTED DEVIATION`.

---

## 1. Node identity: a persisted UUID instead of `dimension:x:y:z`

**Oracle** `TileNode.generateId()` (`thaumcraft/common/tiles/TileNode.java:103-114`) derives the
identity from the position: `dim + ":" + x + ":" + y + ":" + z`.

**Native** `TileNode:63` keeps `scanId = UUID.randomUUID().toString()` and persists it
(`:869-888`).

**Why it stays.** The UUID is bound into accepted evidence — the client acceptance parses it with
`UUID.fromString`
(`fabric/src/clientTest/java/thaumcraft/fabric/clienttest/FocalManipulatorClientSequence.java:509`)
— and into every saved world. Changing the format invalidates the accepted client and multiplayer
runs.

**Observable consequence.** A node that is moved keeps its identity in the native and changes it in
the original. The scan signature differs in form but is stable per node.

---

## 2. Jar step/break audio on the brain jar and the node jar: fixed

This one was **not** accepted — it was a real defect the audit found and the leader fixed:
`jar_brain` and `jar_node` used `SoundType.GLASS` while the plain jar used the original
`CustomSoundType("jar", 1.0F, 1.0F)`. The oracle sets the sound in the `BlockJar` constructor
(`oracle BlockJar.java:88`), so all three share it. Both now use
`ThaumcraftContent.JAR_SOUND`. Recorded here because the audit named it as a deviation to accept;
it turned out to be fixable.

---

## 3. 1.7.10 font rendering

The original ships a bitmap font with its own glyph metrics. 1.21.1 has no equivalent API for
replacing the font atlas per mod without shipping a full font provider. The port uses the vanilla
font.

**Not verified:** whether the glyph widths match closely enough for the research book layout. No
frame-level comparison has been made.

---

## 4. OptiFine/Iris shader pack integration

The original reads OptiFine's shader-pack state to decide whether to draw certain effects. 1.21.1
has no equivalent hook. The port draws the effects unconditionally.

---

## 5. Particle count per layer

**Resolved in code, not a deviation.** The original evicts the oldest entry at 2000 per layer and
dimension (`oracle ParticleEngine.java:138-196`). An earlier wave recorded this as impossible; the
wave-8 lane implemented it (`Tc4ParticleEngineMixin`, `ParticleLevelAccess`,
`Tc4ParticleWorldRenderMixin`).

**Not verified:** runtime mixin injection and the eviction under load.

---

## 6. What is NOT in this file, and why

Several things were previously reported as impossible limitations and were **disproved** by later
lanes reading the actual vanilla code. They are not deviations:

| Formerly claimed impossible | Actually done |
|---|---|
| The linked mirror's view-dependent texture (`glTexGen`) | Projective core shader, using `rendertype_end_portal` as the demonstrated technique |
| The chunk regeneration queue | `ChunkRegeneration` drains ten chunks per tick with the oracle's seed |
| The scattered-feature node and `auraGen` exclusivity | `AuraGenRules` maps the native structure starts |
| The 2000-particle cap | Implemented in wave 8 |
| The ore overlay's "fast graphics" condition | It is anisotropic filtering, reproduced |

This is the pattern the whole effort kept hitting: an agent wrote "no 1.21.1 counterpart" without
opening the vanilla class, and the claim survived into the ledger. The audit's job is to keep
finding them.

---

## 7. Unverified, not deviated

These are not differences — they are things **nobody has looked at on a real frame or a real
world**. They must not be reported as verified until someone does:

* The thaumometer screen and readout, the excavation beam, the architect overlay, the essence flow,
  the sparkles, the bone bow glint.
* Runtime mixin injection on Fabric. On NeoForge it is now checked: a Mixin audit on a real
  NeoForge client (2026-10-26) applied every pending target without error, after
  `TaintOverlayChunkLayerMixin` was fixed for NeoForge's delegating `SectionCompiler.compile`
  (see `docs/analysis/2026-10-26-neoforge-client-ru.md`).
* The projective mirror shader compiling in the shader driver.
* Nether wild nodes in a freshly generated Nether.
* The chunk regeneration queue on a real saved world.
* Two physical clients plus a dedicated server (the last accepted run predates waves 4–8).

### 7.1 Now seen on a real frame (2026-10-23)

These were in the list above and have now actually been looked at. What the frames show is
recorded here so the claim is bounded by what was seen, not by a test count:

* **Research graph background** — the category sheet is the original purple nebula, not parchment,
  and it now covers the pane continuously with no crop seams. The 1:2 texel density was verified
  numerically: four source texels match the captured PNG at exactly 2×2 GUI pixels. See
  `docs/analysis/2026-10-23-agent-visualrun.md`.
* **Void robe** — worn on the body in third person, front and back: black-faced purple hood, layered
  pointed shoulder plates, long robe, split side panels, diagonal back accessory and belt pouches.
  No missing-texture checkerboard. Both frames are dark, and the floor shows hard rectangular black
  regions whose cause is **not** established; not patched speculatively.
* **Node item and jar node** — the stabilizer draws as a held gold-rimmed 3D item; the placed jar
  shows its glowing contents; the aura node draws as a bounded orb within the view.
* **Candles, gas surface, crust, eldritch stairs** — one frame carries all four: a lit candle with
  smoke, a purple crust block, dark two-step eldritch stairs and a lavender gas cell in glass.
* **The mirror** — a real native bind and place produced authoritative reciprocal links, and both
  faces show the dark purple speckled projected field with its inset border, not a blank plane.
  Static frames do not establish projective motion parity.
* **Radial focus menu** — opens on held F with the focus list, the wand label and Remove/Cancel.
* **Eldritch obelisk** — renders coherently (coherent housing, pointed caps) and no longer crashes
  the client. The tunnel effect inside it is **not** readable as a receding passage; it reads as
  faint cyan marks on dark inset faces. Left open as a real rendering gap.
* **Shimmerleaf and cinderpearl** — both decorative plants draw their crossed-quad models, the
  shimmerleaf emits its blue wisp and the cinderpearl its flame/smoke while standing on sand.

