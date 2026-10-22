# g1perm — permission hypothesis

- Worktree `/home/cyberfox-kali/wt/g1perm`, branch `agent/g1perm`; baseline `d6ae5c78`.
- First requested status/log command succeeded, clean tree. No other worktree or primary checkout accessed.
- Reviewed the 16 requested feature families: 14 implemented block-entity families, one entity (Traveling Trunk), one absent modern implementation (golem press).
- Full source inventory and explicit untested boundaries: `docs/audit/PERMISSION_FINDINGS.md`.
- Read retained legacy trunk container before changing modern authority behavior; modern 10-block trunk menu range and canonical upgrade rules are not intentionally changed.
- Tests invoke real mutation methods and inspect real block/entity inventory/state. They intentionally bypass native packet dispatch to test the custom authority boundary; this does not by itself demonstrate an externally reachable packet exploit.
- Baseline/fixed execution and commits: pending.
- Initial wrapper download was deliberately stopped (exit 143) before any test result. Seeded the isolated `.gradle-g1` with copies of the general `/home/cyberfox-kali/.gradle/{caches,wrapper}` (not another worktree); subsequent baseline uses `--offline`. No shared cache is used as the writable Gradle home.
- Copied cache metadata proved unusable; indexed its existing artifacts as an ignored local Maven repository under `.tmp/g1-local-maven`, with an ignored init script. Production build files remain unchanged. An initial offline local-repository build compiled common/Fabric gameplay and GameTests, but stopped at Architectury's dynamic `fabric-loader:+` transformer dependency. The init script pins only that tooling configuration to the cached project-compatible `0.16.14`; baseline retry pending. This is not a NeoForge task run (Gradle configures the retained module as part of the existing root build).
