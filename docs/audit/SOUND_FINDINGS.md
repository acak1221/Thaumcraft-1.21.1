# Sound and particle resolution audit — 2026-10-23 (g11sound)

## Result and limits

**PROVEN:** 66 bundled Thaumcraft sound events / 111 OGG references inspected, plus four invalid named vanilla events (70 identifiers total). Four names had no sound definition and necessarily resolved to no audio. All 66 bundled events lacked registry declarations and subtitle metadata; all audio files already existed. Fixed the four names, registered all 66 events, and supplied 66 subtitle keys in each of EN/RU. **24 sampled volume/pitch call sites: zero mismatches.**

Also **PROVEN and fixed:** two golem client-event sound call sites used the server-broadcast API (`IRON_GOLEM_ATTACK` and `jacobs`). That is an overlapping attack-name defect plus one additional silenced existing sound, not two more missing identifiers.

**Particles:** zero custom `ParticleType` declarations, 37 direct-created particle implementation classes, and 16 vanilla particle types inspected (53 effect/type entries). Zero missing particle resources found. `ParticleDeflection` is a utility, excluded from the 37. Direct construction into the client particle engine does not require a `ParticleType`, provider registration, or particle-definition JSON. Do not invent unused types/providers to satisfy that inapplicable condition.

**Important distinction:** an unregistered `SoundEvent` is not by itself proof of silence in 1.21.1: direct-holder sound packets and local client sounds can resolve by location. The four nonexistent golem locations are the proven silent events. The registry omission is separately proven and repaired; no claim that all 66 were previously inaudible. Subtitles are an additive 1.21 accessibility adaptation, not original TC4 text.

## Tests and execution

- **S1:** `SoundResourceAuditTest`: exact registration-declaration/JSON set equality; bootstrap wiring; nonempty sound lists; each OGG exists and has `OggS` signature; subtitle exists and is nonempty in both locales. Literal helper names (including inherited helpers), direct event constructions, and `CustomSoundType` names must resolve. Dynamic harness toggle, unknown-note branch, and champion gore identifiers inspected separately. Removing an event, OGG, registration declaration, bootstrap call, subtitle field, or either translation fails this test.
- **S2:** `EldritchGolemSoundTest`: rejects `sound("mob.irongolem.*")` and requires all four native vanilla constants. Red run failed on the original source, then passed after the fix.
- **S4:** the same test class rejects `playSound` inside the golem client-event handler and requires both local calls. Its independent red run failed before the routing fix. Cached Minecraft 1.21.1 bytecode was inspected with Java 21 `javap`: `Entity.playSound` calls `Level.playSound(null,...)`; `Level` delegates to `ClientLevel.playSeededSound`, which compares the supplied player with `Minecraft.player` before playing. Null cannot match an active local player. Oracle golem:270 explicitly uses world local playback. Both event sounds now use `playLocalSound`, preserving coordinates/category/volume/pitch and the attack silent flag.
- **S3:** Fabric `sound_registry_covers_bundled_events`: live dedicated-server registry contains every bundled sound ID. Passed, **1/1 required tests**, fresh world. This catches actual registration failure, not just text declarations.
- **V1:** `SoundValueParityTest`: locks the 24 source expressions listed below against independently read TC4 4.2.3.5 oracle call sites.
- **P1:** `ParticleResourceAuditTest`: checks literal particle resource bindings, shared sheets/bolt textures, hemisphere model, hemis0..15, ripple1..15. Resource existence, not rendered appearance.
- Asset converter verification passed: 66 sound events, 111 file references, zero event-to-event references; 518 block-atlas sprites, 3944 model texture/atlas references, 3018 local model references, zero missing references.
- `convert_assets.py --verify` checks sound JSON/file references and model/block-atlas references. It **does not check sound registration, subtitles, particle providers, direct-render texture bindings, or the vanilla particle atlas**. The new tests close the applicable gaps rather than treating its output as particle acceptance.
- Converter Python suite: 58 tests. Focused JUnit/build and server logs are local `.tmp/g11-*.log`; no original source or worlds committed.

## PROVEN sound inventory (all IDs below use `thaumcraft:`)

For every row, JSON and every OGG resolved before and after. `R+L` means missing registry/subtitles repaired by `ThaumcraftSounds` plus the generated subtitle field and EN/RU translations. Tests S1/S3 apply to **every row**. Audio bytes/weights/pitch metadata are unchanged.

| Event | OGG references | JSON/audio | Fix | Tests |
|---|---:|---|---|---|
| heartbeat | 1 | resolves | R+L | S1/S3 |
| spill | 1 | resolves | R+L | S1/S3 |
| bubble | 4 | resolves | R+L | S1/S3 |
| alembicknock | 1 | resolves | R+L | S1/S3 |
| creak | 2 | resolves | R+L | S1/S3 |
| squeek | 2 | resolves | R+L | S1/S3 |
| golemironshoot | 1 | resolves | R+L | S1/S3 |
| jar | 4 | resolves | R+L | S1/S3 |
| swarm | 3 | resolves | R+L | S1/S3 |
| swarmattack | 1 | resolves | R+L | S1/S3 |
| fly | 2 | resolves | R+L | S1/S3 |
| key | 1 | resolves | R+L | S1/S3 |
| doorfail | 1 | resolves; no current caller found | R+L | S1/S3 |
| cameraticks | 1 | resolves | R+L | S1/S3 |
| cameraclack | 3 | resolves | R+L | S1/S3 |
| pump | 3 | resolves | R+L | S1/S3 |
| page | 2 | resolves | R+L | S1/S3 |
| learn | 1 | resolves | R+L | S1/S3 |
| write | 2 | resolves | R+L | S1/S3 |
| erase | 1 | resolves | R+L | S1/S3 |
| brain | 4 | resolves | R+L | S1/S3 |
| crystal | 1 | resolves | R+L | S1/S3 |
| wispdead | 1 | resolves | R+L | S1/S3 |
| wisplive | 3 | resolves | R+L | S1/S3 |
| wand | 3 | resolves | R+L | S1/S3 |
| wandfail | 1 | resolves | R+L | S1/S3 |
| rumble | 1 | resolves | R+L | S1/S3 |
| ice | 3 | resolves | R+L | S1/S3 |
| jacobs | 1 | resolves | R+L | S1/S3 |
| hhoff | 1 | resolves | R+L | S1/S3 |
| hhon | 1 | resolves | R+L | S1/S3 |
| pech_idle | 3 | resolves | R+L | S1/S3 |
| pech_trade | 1 | resolves | R+L | S1/S3 |
| pech_dice | 1 | resolves | R+L | S1/S3 |
| pech_hit | 2 | resolves | R+L | S1/S3 |
| pech_death | 1 | resolves | R+L | S1/S3 |
| pech_charge | 2 | resolves | R+L | S1/S3 |
| shock | 2 | resolves | R+L | S1/S3 |
| fireloop | 1 | resolves | R+L | S1/S3 |
| zap | 2 | resolves | R+L | S1/S3 |
| craftfail | 1 | resolves | R+L | S1/S3 |
| craftstart | 1 | resolves | R+L | S1/S3 |
| runicshieldeffect | 1 | resolves | R+L | S1/S3 |
| runicshieldcharge | 1 | resolves | R+L | S1/S3 |
| swing | 2 | resolves | R+L | S1/S3 |
| wind | 2 | resolves | R+L | S1/S3 |
| tool | 2 | resolves | R+L | S1/S3 |
| gore | 2 | resolves | R+L | S1/S3 |
| roots | 3 | resolves | R+L | S1/S3 |
| tentacle | 3 | resolves | R+L | S1/S3 |
| upgrade | 1 | resolves | R+L | S1/S3 |
| whispers | 1 | resolves | R+L | S1/S3 |
| monolith | 1 | resolves | R+L | S1/S3 |
| infuser | 1 | resolves | R+L | S1/S3 |
| infuserstart | 1 | resolves | R+L | S1/S3 |
| egidle | 2 | resolves | R+L | S1/S3 |
| egattack | 1 | resolves | R+L | S1/S3 |
| egdeath | 1 | resolves | R+L | S1/S3 |
| egscreech | 1 | resolves | R+L | S1/S3 |
| crabclaw | 1 | resolves | R+L | S1/S3 |
| crabdeath | 1 | resolves | R+L | S1/S3 |
| crabtalk | 3 | resolves | R+L | S1/S3 |
| chant | 3 | resolves | R+L | S1/S3 |
| coins | 1 | resolves | R+L | S1/S3 |
| urnbreak | 1 | resolves (`CustomSoundType`) | R+L | S1/S3 |
| evilportal | 1 | resolves | R+L | S1/S3 |

### PROVEN silent named calls: Eldritch Golem

Oracle root read-only: `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle/`; paths below relative to `thaumcraft/`. Original `common/entities/monster/boss/EntityEldritchGolem.java` uses vanilla names, without a `thaumcraft:` prefix. The port's shared helper incorrectly prefixed those four names.

| Invalid former identifier | Oracle line | Fixed native constant | Test |
|---|---:|---|---|
| thaumcraft:mob.irongolem.hit | 150 | SoundEvents.IRON_GOLEM_HURT | S2 |
| thaumcraft:mob.irongolem.death | 154 | SoundEvents.IRON_GOLEM_DEATH | S2 |
| thaumcraft:mob.irongolem.walk | 158 | SoundEvents.IRON_GOLEM_STEP | S2/V1 |
| thaumcraft:mob.irongolem.throw | 248 | SoundEvents.IRON_GOLEM_ATTACK | S2/V1 |

No missing OGG was fabricated and no invalid-name aliases were added. Native sound identities replace removed 1.7 identifiers.

## PROVEN volume/pitch sample: 24 call sites, zero mismatches

`r`, `a`, `b` denote independent `nextFloat()` draws; all listed expressions preserve the oracle's distribution. Oracle paths are relative to `thaumcraft/`; modern paths under `common/src/main/java/thaumcraft/` are the same except boss classes moved out of `monster/boss`, and sword sweep moved to `common/lib/world/ElementalSwordSweep`. Every row is locked by V1.

| # | Oracle class:line / call | Volume | Pitch | Result |
|---:|---|---:|---|---|
| 1 | EntityEldritchGolem:239 egattack | 1 | 1+r*.1 | match |
| 2 | EntityEldritchGolem:248 throw | 1 | 1 | match |
| 3 | EntityEldritchGolem:158 walk | 1 | 1 | match |
| 4 | EntityEldritchGolem:270 jacobs | .8 | 1+(a-b)*.05 | match |
| 5 | EntityEldritchWarden:236 zap | 1 | .9+r*.1 | match |
| 6 | EntityEldritchWarden:340 egattack | 2 | 1+r*.1 | match |
| 7 | EntityEldritchWarden:355 egscreech | 4 | 1+r*.1 | match |
| 8 | EntityEldritchGuardian:311 egattack | 2 | 1+r*.1 | match |
| 9 | EntityEldritchGuardian:324 egscreech | 3 | 1+r*.1 | match |
| 10 | EntityEldritchCrab:174 crabclaw | 1 | .9+r*.2 | match |
| 11 | EntityEldritchCrab:220 spider step | .15 | 1 | match |
| 12 | EntityWisp:210 zap | 1 | 1.1 | match |
| 13 | ItemFocusHellbat:124 ice | .2 | .95+r*.1 | match |
| 14 | ItemFocusHellbat:126 wandfail | .1 | .8+r*.1 | match |
| 15 | ItemFocusPrimal:75 ice | .3 | .8+r*.1 | match |
| 16 | ItemFocusPech:76 ice | .4 | 1+r*.1 | match |
| 17 | ItemElementalPickaxe:85 wandfail | .2 | .2+r*.2 | match |
| 18 | ItemElementalSword:142 wind | .5 | .9+r*.2 | match |
| 19 | ItemElementalSword:161 swing | 1 | .9+r*.2 | match |
| 20 | ItemLootBag:97 coins | .75 | 1 | match |
| 21 | TileCrucible:287 bubble | .2 | 1+r*.4 | match |
| 22 | EntityShockOrb:63 shock | 1 | 1+(a-b)*.2 | match |
| 23 | GuiResearchTable:654 write | .2 | 1 | match |
| 24 | GuiResearchTable:658 erase | .2 | 1+r*.1 | match |

## Particle inventory: 37 direct-created effects

All paths are `common/src/main/java/thaumcraft/client/fx/particles/<class>.java`. `sheet` = `textures/misc/particles.png`; `sheet2` = `particles2.png`; `bolts` = `textures/misc/p_large.png` and `p_small.png`; `nodes` = `textures/misc/nodes.png`. All local bindings and frame families exist; P1 applies to local resources. `native` rows use vanilla block/item/particle atlases, not the mod's sprite sets. No fix was needed. Provider/registry: **N/A direct construction for every row**.

| Class | Binding | Resolution/test |
|---|---|---|
| BlockRuneParticle | sheet | exists / P1 |
| BlockSparkleParticle | sheet | exists / P1 |
| BoreDigSparkleParticle | sheet | exists / P1 |
| CrystalSparkParticle | sheet2 | exists / P1 |
| EssentiaTrailParticle | sheet | exists / P1 |
| FXArc | misc/beamh.png | exists / P1 |
| FXArcSparkle | sheet | exists / P1 |
| FXSmokeSpiral | sheet | exists / P1 |
| FXVent | sheet | exists / P1 |
| FocusFireballParticle | sheet | exists / P1 |
| FocusFrostSparkleParticle | sheet | exists / P1 |
| GrowthLampParticle | inherited BlockSparkle sheet | exists / P1 |
| HarnessBoltParticle | bolts | exists / P1 |
| HungryNodeFragment | native TerrainParticle block sprite | vanilla-owned; visual unverified |
| InfusionArcParticle | bolts | exists / P1 |
| InfusionParticle | sheet or native item model particle sprite | local exists / P1; item appearance unverified |
| MonsterSpriteParticle | nodes + sheet | exists / P1 |
| PechCurseParticle | sheet | exists / P1 |
| PrimalWispParticle | sheet | exists / P1 |
| ResourceParticle | sheet | exists / P1 |
| RunicHitParticle | hemis.obj + hemis1..15.png | exists / P1 |
| SanityBubbleParticle | sheet | exists / P1 |
| ShockSparkParticle | sheet2 | exists / P1 |
| SlimeJumpParticle | native slime-ball item sprite | vanilla-owned; visual unverified |
| SlurryBubbleParticle | sheet | exists / P1 |
| TaintDropParticle | native drip_hang/fall/land | all 3 PNGs + directory atlas source verified in cached Minecraft 1.21.1 merged JAR |
| TaintLandParticle | native slime-ball item sprite | vanilla-owned; visual unverified |
| TaintSplooshParticle | native slime-ball item sprite | vanilla-owned; visual unverified |
| TaintSwarmParticle | sheet | exists / P1 |
| TaintacleAriseParticle | native slime-ball item sprite | vanilla-owned; visual unverified |
| ThaumcraftBoltParticle | bolts | exists / P1 |
| TypeSparkleParticle | sheet | exists / P1 |
| WardHitParticle | hemis0..15.png | exists / P1 |
| WarpGuardianParticle | sheet | exists / P1 |
| WarpOrbParticle | sheet | exists / P1 |
| WarpSonicParticle | hemis.obj + ripple1..15.png | exists / P1 |
| WispBoltParticle | bolts | exists / P1 |

### Vanilla ParticleTypes used: 16

These are existing vanilla constants, registered/provided by Minecraft, not mod declarations. No mod-owned sprite/provider registration is applicable. Static ownership inspection is not a live render test.

| Type | Registration/provider/assets | Fix |
|---|---|---|
| ANGRY_VILLAGER | vanilla-owned | none |
| BLOCK | vanilla-owned; supplied block state sprite | none |
| BUBBLE | vanilla-owned | none |
| BUBBLE_POP | vanilla-owned | none |
| ENCHANT | vanilla-owned | none |
| ENTITY_EFFECT | vanilla-owned | none |
| EXPLOSION | vanilla-owned | none |
| FLAME | vanilla-owned | none |
| HAPPY_VILLAGER | vanilla-owned | none |
| HEART | vanilla-owned | none |
| LARGE_SMOKE | vanilla-owned | none |
| LAVA | vanilla-owned | none |
| NOTE | vanilla-owned | none |
| PORTAL | vanilla-owned | none |
| SMOKE | vanilla-owned | none |
| SPLASH | vanilla-owned | none |

## Side audit — source evidence, not listening evidence

| Path/call family | Source ownership observed | Finding |
|---|---|---|
| TileCrucible bubble/pop | ServerLevel branch | single authoritative server broadcast |
| EntityShockOrb shock | ServerLevel impact handler | server broadcast |
| EntityEldritchGolem handleEntityEvent attack/jacobs | client event; formerly Entity.playSound(null-player route) | **PROVEN silent route; fixed both to playLocalSound; S4** |
| ItemFocusHellbat ice/fail | ServerPlayer/ServerLevel operation | server broadcast |
| ItemFocusPrimal / ItemFocusPech ice | server use operation | server broadcast |
| ItemElementalSword wind/sweep | ServerLevel operation | server broadcast |
| TileFocalManipulator | server state machine | server sound |
| ThaumcraftTravelGearClient hhon/hhoff/jacobs | client tick/local player | intended local sound |
| GuiResearchTable write/erase/click | client GUI/local player | intended local sound |
| ThaumcraftShockOrbsClient jacobs | playLocalSound | intended local ambient sound |
| TaintSwarmParticle fly | ClientLevel.playLocalSound | intended local ambient sound |
| ContainerTravelingTrunk open/close | shared menu, playSound(null,...) | client null-player route ignored; server broadcasts; no static duplicate route |
| EntityTaintSpider/TaintSwarm/TaintSpore overrides | shared tick, playSeededSound(null,...) | client null-player route ignored; server broadcasts; no static duplicate route |

## Reasoned-only / not claimed complete

1. Audible outcome and subtitle wording quality have not been inspected in a client; EN/RU presence is tested, not native-speaker approval.
2. Source-side ownership above is not a two-client audio-capture test. Double playback, attenuation, distance, category balance, and listener exclusion remain unverified.
3. Vanilla particle provider operation, custom shader registration, direct-particle spawn reachability, sprite UV/frame appearance, item/block sprite appearance, and packet-to-client effects are not established by file existence.
4. The 24-call sample is not exhaustive numeric parity for every sound caller; no claim about unsampled values.
5. NeoForge compilation/runtime was not tested. Gradle configures the retained project, but only common/Fabric tasks were invoked.
