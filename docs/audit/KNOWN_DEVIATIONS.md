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

* All client visuals added by waves 4–8: the thaumometer screen and readout, the research graph
  background, the void-robe layers, the node item, the excavation beam, the architect overlay, the
  essence flow, the sparkles, the candles, the gas surface, the crust join, the eldritch stairs,
  the mirror, the bone bow glint, the radial focus menu.
* Runtime mixin injection on both loaders.
* The projective mirror shader compiling in the shader driver.
* Nether wild nodes in a freshly generated Nether.
* The chunk regeneration queue on a real saved world.
* Two physical clients plus a dedicated server (the last accepted run predates waves 4–8).
