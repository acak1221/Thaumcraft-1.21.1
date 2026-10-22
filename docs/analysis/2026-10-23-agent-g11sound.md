# g11sound — sound/particle resolution hypothesis

Worktree `/home/cyberfox-kali/wt/g11sound`, branch `agent/g11sound`; clean start at `535f8d80`. No other worktree or primary checkout was accessed. Read-only original oracle at `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`.

## Proven findings and changes

1. Four Eldritch Golem names incorrectly became `thaumcraft:mob.irongolem.{hit,death,walk,throw}`. No matching JSON/OGG definitions exist. Replaced with native `SoundEvents.IRON_GOLEM_{HURT,DEATH,STEP,ATTACK}`. Oracle lines 150/154/158/248. Regression test red before fix, green after.
2. No sound registry existed. Added Architectury common deferred registration for all 66 bundled events, wired at initialization. Added one focused Fabric GameTest to inspect the actual frozen registry and each event's audio location. This is not a claim that direct-holder playback of unregistered events was always silent.
3. All 66 sounds lacked subtitle metadata/translations. Added subtitle fields and 66 EN + 66 RU keys, plus deterministic converter input `scripts/porting/sound-subtitles.json`. OGG bytes and original sound lists remain untouched. This is additive accessibility metadata; not original TC4 wording. Existing language keys/values are unchanged (JSON comparison: no removals, no modifications, exactly 66 additions per locale).
4. Golem entity-event attack and arc sound used `Entity.playSound` on the client, whose null-player route is ignored by `ClientLevel`. Proved from the resolved 1.21.1 Entity/Level/ClientLevel bytecode and a red static routing test. Switched both event calls to local playback. Existing pitch/volume preserved. This overlaps the invalid attack name; the arc (`jacobs`) is an additional wrong-side failure, not another unresolved identifier.

## Scope/counts

- 66 bundled sound events / 111 OGG references + 4 invalid named IDs = **70 identifiers** inspected.
- **4 unresolved IDs**, **66 missing registration declarations**, **66 missing subtitle fields / 132 missing translations**, **2 client-event routing failures**. All repaired.
- **24 independent oracle volume/pitch samples, zero mismatches**; source-expression assertions lock the sample.
- **0 custom ParticleTypes**, **37 direct-created custom effect implementations**, **16 vanilla ParticleTypes**; no missing resources found. No particle fix needed. TaintDrop's three vanilla drip textures and particle-atlas directory source checked in the cached Minecraft JAR. Custom local sheets/bolt textures/frame sequences covered by JUnit.

Full inventory, numerical sample, side audit and reasoned-only limits: `docs/audit/SOUND_FINDINGS.md`.

## Verification

Java 21 with private `GRADLE_USER_HOME=/home/cyberfox-kali/wt/g11sound/.gradle-g11` (seeded by copying the normal global cache, not another worktree). Removed `fabric/run-gametest/world` before every Gradle invocation.

- `.tmp/g11-red.log`: original golem namespace test failed, as expected.
- `.tmp/g11-resource-red.log`: golem namespace fix passed; missing registry resource-contract test failed, as expected.
- `.tmp/g11-side-red.log`: independent client event routing test failed, as expected.
- Focused JUnit classes: `EldritchGolemSoundTest` (2), `SoundResourceAuditTest` (2), `SoundValueParityTest` (1), `ParticleResourceAuditTest` (1): **6 tests**.
- Fabric production build and focused dedicated-server `sound_registry_covers_bundled_events` run; server result **1/1 required tests passed**. Logs `.tmp/g11-final-side.log` / `.tmp/g11-final-registry.log`.
- `python3 scripts/porting/convert_assets.py --verify`: 66 events / 111 sound refs; 3944 model texture/atlas refs, 3018 model refs, 518 available atlas sprites; zero missing.
- `python3 -m unittest discover -s scripts/porting -p test_convert_assets.py`: **58 passed**.

The converter's full generation also rewrites foundation wand/thaumometer models and unrelated name aliases. Its incidental output was inspected and **only my generated unrelated changes were restored**; existing model data and native translation values are preserved. Sound manifest hashes updated narrowly.

## Not established

Audible client acceptance; multiplayer listener/double-play capture; subtitle editorial approval; custom particle shader/spawn reachability and visual appearance; exhaustive numerical parity outside the 24 samples; NeoForge build/runtime. No claim that file-existence checks prove visual or audio acceptance. No public push or original-source changes.
