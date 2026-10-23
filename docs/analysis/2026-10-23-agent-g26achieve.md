# g26achieve — automated devices

- Worktree `/home/cyberfox-kali/wt/g26achieve`, branch `agent/g26achieve`.
- Initial status clean; base `7f37e8a6`.
- Scope: prove/disprove consumption/output failure of actual built devices, not decorative state or manually advanced isolated machine fixtures.
- Added world-scheduled GameTests and reused bounded real-world formation/menu helpers. Detailed coverage and limitations: `docs/audit/DEVICE_FINDINGS.md`.
- Initial `:fabric:compileGameTestJava --offline` failed in the new private `.gradle-g26` home because Loom was not cached. Online resolution started with Java21 and that same private Gradle home. No shared caches, other worktrees or primary checkout modified.
- Every Gradle launch is preceded by removing only this worktree’s `fabric/run-gametest/world`.
- No production change yet: hypothesis not confirmed by source reasoning alone.

Runtime results and final commit IDs pending.
