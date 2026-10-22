# g16config — 2026-10-23

Branch/worktree: `agent/g16config`, `/home/cyberfox-kali/wt/g16config`.
First required status/log command passed; baseline `d2abbdf0`, clean tree.
No other worktree or primary checkout was read or modified. The private oracle outside
the checkouts was read only. No subagents or public push.

## Hypothesis and results

Configuration is ignored, read on the wrong side, or not synchronized.
Full ledger: [`../audit/CONFIG_FINDINGS.md`](../audit/CONFIG_FINDINGS.md).

* 39 file keys inspected, including 25 editable catalogue entries; 0 without a parser.
* 1 gameplay-inert parsed key (`generate_taint`), intentionally inert in the original too.
* 86 public compatibility fields compared with oracle initializers; 0 mismatches.
  85 have no direct production reader, but native record consumers replace many of them.
* 0 proven wrong-side native gameplay readers.
* Original seven server facts are sent at join and applied to the client snapshot.
* Found/fixed one catalogue default mismatch: `glowing_taint` was false instead of true.
* Found/fixed one lifecycle defect affecting seven fields: client initialization and
  disconnect used hard-coded defaults instead of the configured local snapshot.

## Changes / commits

1. `1a457289` — `fix(config): preserve original glowing taint editor default`.
   Includes catalogue-reader/default/file-roundtrip audit and private Gradle-home ignore.
2. `89c25ca0` — `fix(config): restore configured client settings after disconnect`.
   `ThaumcraftSettings.initializeClient/resetClient` now use
   the local server record. Remote receipt still does not mutate server authority.
   Strengthened `ResearchSettingsTest`: all seven local values differ from defaults,
   all remote values differ from local, and server-thread state stays authoritative.
3. Added complete 39-key and 86-field ledger, network evidence, runtime limitations,
   and reasoned-only acceptance gaps. No removed/renamed compatibility fields.

## Validation

Java: `/usr/lib/jvm/java-21-openjdk-amd64`.
Private Gradle home: `/home/cyberfox-kali/wt/g16config/.gradle-g16`.
Before every Gradle invocation: `rm -rf fabric/run-gametest/world`.

Initial invocation:

```sh
./gradlew :common:test \
  --tests thaumcraft.common.config.ConfigDeclarationAuditTest \
  --tests thaumcraft.research.ResearchSettingsTest \
  --tests thaumcraft.common.config.ConfigDefaultsTest \
  :fabric:compileJava --no-daemon -Porg.gradle.parallel=false
```

Started against an empty private dependency cache. **BUILD SUCCESSFUL in 11m 51s**:
the selected JUnit tests and actual Fabric Java compilation passed. Root Gradle configuration
also prepared the existing NeoForge module's dependencies; no NeoForge compile/test/run task
was requested and no NeoForge acceptance is claimed.

Final-source invocation, serialized after that build:

```sh
./gradlew :common:test \
  --tests thaumcraft.common.config.ConfigDeclarationAuditTest \
  --tests thaumcraft.research.ResearchSettingsTest \
  --tests thaumcraft.common.config.ConfigDefaultsTest \
  --tests thaumcraft.common.lib.network.ResearchSettingsPayloadTest \
  :fabric:runGameTest \
  -PgameTestFilter=cheatSheetUsesServerPermissionAndOriginalKnowledgeRules --no-daemon
```

This focused dedicated-server case exercises actual registered cheat-book use, dynamically
changed server feature gates, creative tab rebuilds, and Wuss-mode behavior; it is not a
substitute for a remote-client disconnect test. **BUILD SUCCESSFUL in 45s** on final
sources: **15 JUnit tests passed, 0 failures/errors**, and **1/1 required dedicated-server
GameTests passed** (`thaumcraftgametests.cheatsheetusesserverpermissionandoriginalknowledgerules`).
Fabric test-root isolation verification also passed. Existing mixin/deprecation warnings,
missing data-fixer logs, and first-run missing server.properties/eula warnings appeared;
the server nevertheless started, ran the focused assertion, saved, and shut down successfully.

Local ignored evidence:

* `common/build/test-results/test/TEST-*.xml` (3 audit + 7 defaults + 2 codec + 3 settings).
* `fabric/build/gametest-focused-results.xml`.
* `fabric/run-gametest/logs/latest.log`.

Independent static validation passed: exactly 39 known parsed keys, 25 catalogue defaults
matching independent oracle values, and complete 39/86 report rows. `git diff --check` passed.

## Not claimed

No observed real two-client/dedicated multiplayer session, visual acceptance, or NeoForge validation.
Original in-session config-change callback exists, whereas the modern editor advertises
restart and only writes disk: live reload parity is not implemented here. Compatibility-field
mutation by add-ons, reconnect visuals/cache ordering, and unsupported original options
remain explicit follow-up items rather than claimed fixes.
