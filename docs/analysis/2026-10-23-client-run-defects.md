# 2026-10-23: two defects the client run surfaced

Both came out of a single full `:fabric:runClientSmoke` run that reached frame 46. Neither was
findable by the Green-suite reasoning that preceded it: one is a real gameplay defect that only
shows up when the world simulates long enough, the other is a test that could never have passed
for the right reason.

## 1. A cinderpearl died on the desert sand it is generated on (real defect, fixed)

**Symptom.** A client scene placed a cinderpearl on sand next to a shimmerleaf on grass. After
the observation window the cinderpearl was `minecraft:air` and `isRandomlyTicking() == false`,
while the shimmerleaf was alive and ticking.

**Cause.** `BlockCustomPlant.mayPlaceOn` answered the inherited *vanilla* `BlockBush` table —
grass, dirt, farmland — and `canSurvive` asked it, so the plant's own random tick destroyed it
on sand. The original does not behave that way, and the reason is a platform layer the port had
missed:

* the vanilla 1.7.10 `BlockBush.func_149854_a` (`ajr`) really is grass/dirt/farmland;
* but **Forge 1.7.10 patches `BlockBush`**. Its binary patch for `ajr` ships in
  `binpatches.pack.lzma`; its `inputChecksum` equals the adler32 of the vanilla `ajr.class` in
  the official client jar, so it provably applies to that exact class. It rewrites
  `func_149718_j` to `below.canSustainPlant(world, x, y - 1, z, UP, this)`, and the patched
  `Block.canSustainPlant` switches on `IPlantable.getPlantType`;
* TC4's `BlockCustomPlant.getPlantType` answers `EnumPlantType.Desert` for the cinderpearl
  metadata (`BlockCustomPlant.java:119-128`), and the Desert case returns `this == Blocks.sand`.

So the original `getPlantType` is live, load-bearing code, and TC4's own world generator plants
cinderpearls on sand (`WorldGenCustomFlowers.java:33` accepts grass **or** sand;
`ThaumcraftWorldGenerator.java:476-477` gates on `topBlock == sand` and `temperature > 1.0`).

**Fix.** `mayPlaceOn` now asks `staySoil(cinderpearl, soil)`, which reuses the Forge soil table
the file already had for the item (`soilSupports`) instead of the bare vanilla `bushSoil`. Sand
is admitted for the cinderpearl, still refused for the shimmerleaf, and bare stone is refused
for both. `PlantSoilRuleParityTest.theCinderpearlStaysOnTheDesertSandItIsGeneratedOn` guards it.

**Why the wrong answer looked right.** The port had reasoned from the vanilla `ajr` bytecode,
which is genuine, correctly read and *not* what the original runs. The lesson is recorded here
because it will recur: a 1.7.10 behaviour question is not settled by the vanilla jar alone.

## 2. The flower-particle assertion read the wrong queue (test defect, fixed)

**Symptom.** A live, randomly-ticking shimmerleaf produced zero wisp particles over 900 ticks
while a cinderpearl beside it produced 1124 flame and 917 smoke.

**Cause.** `Tc4ParticleEngineMixin` intercepts `ParticleEngine.add` at `HEAD` and, for every
`Tc4Particle`, diverts it into Thaumcraft's own `client.fx.ParticleEngine` and cancels the
vanilla insertion. `ResourceParticle` (the shimmerleaf wisp) implements `Tc4Particle`;
`FlameParticle`/`SmokeParticle` do not. The harness scanned only vanilla's `particles` map, so
it could observe the cinderpearl's particles and could never observe the shimmerleaf's, no
matter how long it waited. The earlier accepted record (`docs/porting/fire-focus-client.json`,
values `3/1/1`) was a run where the wisp happened to be counted before this diversion existed
or through a different path — it was not a stable observation.

**Fix.** The reader now scans both stores: vanilla's `particles` map and every layer of
`thaumcraft.client.fx.ParticleEngine.PARTICLES` for the current dimension, through a new
read-only `ParticleLayers.layer(dimension, layer)`. It also accumulates what each channel has
delivered across the whole observation window instead of demanding all three in one instant —
`ClientLevel.animateTick` samples one random block per call out of thousands, and the shimmerleaf
gate is `nextInt(3) == 0` on top of that, so a single-instant triple coincidence is a lottery,
not an observation. The frame is still captured only on a tick where particles are actually
present, and no particle is ever synthesised by the harness.

**Not a mod defect.** No gameplay code changed for this one.

## 3. Reading a wand's baked model: one real test bug, repeated in four places

**Symptom.** `InfusionClientSequence` failed at `REWARD_WORLD` with "held reward selects
original Silverwood rod and gold cap sprites, not a fallback" — after the infusion itself had
succeeded and the wand had been crafted.

**Cause.** The wand is a `builtin/entity` item. `models/item/wand_shape.json` declares
`"parent": "minecraft:builtin/entity"` and every per-material model parents to it, so the baked
model is the empty custom-renderer shell and `getQuads(...)` returns nothing for the materials.
The pixels come from `ThaumcraftGearModelsClient.renderWand`, which binds
`ModelWand.rodTexture(rod.baseTag())` and `ModelWand.capTexture(cap.tag())` directly, and draws the
focus cube with `ModelWand.FOCUS_TEXTURE` tinted by `ItemFocusBasic.getFocusColor(focusStack)`.

Four separate scenes asserted on those quads. Each one therefore asserted on a layer that cannot
contain the answer, and each one was going to fail (or, worse, could only ever pass by accident):

| Scene | Was asserting | Now asserts |
| --- | --- | --- |
| `DistillationClientSequence` | `getQuads` sprites `wand_rod_greatwood`/`wand_cap_gold` | `rodTexture("greatwood")`/`capTexture("gold")` + sheets present |
| `InfusionClientSequence` | `getQuads` sprites `wand_rod_silverwood`/`wand_cap_gold` | `rodTexture("silverwood")`/`capTexture("gold")` + sheets present |
| `ThaumiumCapsClientSequence` | `getQuads` sprites `wand_rod_silverwood`/`wand_cap_thaumium` | same, through `ModelWand` |
| `ExcavationClientSequence`, `FrostClientSequence` | six tinted `models/wand` quads + item-colour tint | `rodTexture`/`capTexture`/`FOCUS_TEXTURE` + `getFocusColor(...)` |

The three older scenes had passed in their recorded runs, which is itself suspicious: the accepted
JSON records `actual_sprites` with the material names in them, so at that time the model did return
quads. The `builtin/entity` parent is what removed them, and the scenes were never re-run against
it. This is exactly the failure mode a saved evidence record cannot catch on its own.

**Not a mod defect.** The renderer was right all along; only the tests were reading the wrong layer.

## 4. The book printed a page counter the original never had (real defect, removed)

`GuiResearchRecipe` drew `"<page> / <pages>"` centred 15 px above the bottom of the page, where it
was also vertically clipped by the page edge. The oracle has no such counter anywhere: no
`drawCenteredString` in `GuiResearchRecipe.java` at all, no `" / "` concatenation in any GUI of
`thaumcraft/`. Page position is communicated by the turn arrows alone. The line was removed.

Two earlier analysis documents (`2026-10-23-agent-clientrun.md:353`,
`2026-10-23-agent-clientrun2.md:340`) recorded this counter as *original* and passed it. That is a
false acceptance and is corrected here: what they saw was port-added UI, not parity.

## 5. The focus tooltip carried a port status note (real defect, removed)

`ItemFocusBasic.appendHoverText` appended
`"Frugal and Potency are implemented; Excavation also supports Treasure and Enlarge. Other branches
are not yet available."` to every focus tooltip, so a player hovering a focus read a note about the
state of the port. It is not original text and it is not a gameplay fact the player can act on.
Removed; `tc.port.focus.upgrades.partial` is now unused.

The same audit found `tc.port.golem.use_default` ("…Other ordinary modes are not yet ported."),
which is referenced by no Java and no data file — dead text, left in place for now and recorded here
rather than silently rewritten.

## 6. Research graph: the background repeated one identical window (real defect, fixed)

`drawCategoryBackground` tiled the 224x196 screen window of the texture and sampled the *same*
`(offsetX, offsetY)` in every tile. A texture that does not tile then meets itself at a visible seam
grid: on the 1280x720 frame the seams sat at `graphLeft` + 224·n. Each tile now continues the sheet
where the previous one stopped (`offsetX + x/2`, wrapped by `floorMod`), so the texel density stays
2 screen pixels per texel while the seams disappear. Independently found and fixed the same way by
the `visualrun` lane, which also verified the 1:2 density numerically: four source texels map to
exactly 2x2 GUI pixels in the captured PNG.

## 7. Notable but NOT defects

* **The tall pillar in frame 55** is a fixture: it stands in frames 43, 46 and 55 alike and is not
  part of the crafted wand. It does, however, sit between the camera and the altar, so frame 55
  does not show the altar it claims to. That is a scene-composition flaw worth fixing in the scene,
  not a renderer defect.
* **Frame 43's radial menu** correctly shows the focus list, the "Gold Banded Greatwood Wand" label
  and the Remove/Cancel buttons; the only problem was the port note, removed above.

