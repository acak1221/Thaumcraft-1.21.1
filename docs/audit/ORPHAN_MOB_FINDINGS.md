# Orphan mob audit — g15client (2026-10-23)

## Outcome

**All five are deliberately unreachable in TC4-faithful survival. No missing TC4 spawn mechanism was found; no spawn path was invented.** Commands, retained eggs and direct test construction are not survival acquisition. Existing extra entity registrations/implementations are retained, not deleted.

Oracle root: `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`. Citations below are relative to that root, with CFR line numbers. Independently checked the bundled immutable `Thaumcraft-1.7.10-4.2.3.5.jar`, SHA256 `3dba9786966974701578a658d1bb369bf35bdf5f363079f5ac9c4910a39113be`: enumerated every ZIP entry and searched **every class's bytes** for the five class names. This avoids treating a potentially incomplete decompilation or missing registry entry alone as proof. `javap -p -c` of original `ConfigEntities` additionally confirms its registration/whitelist distinction.

## Per mob

### `watcher` — deliberately unreachable

- **Oracle:** `thaumcraft/common/config/ConfigEntities.java:86-167` registers no Watcher; `:206` only adds `EntityWatcher.class` to the champion whitelist. `thaumcraft/client/ClientProxy.java:464-517` registers no Watcher renderer. Exhaustive original class-byte references are limited to ConfigEntities, EntityWatcher and its four nested classes, RenderWatcher and ModelWatcher. No world event, ritual, entity or client creator references it.
- **Interpretation:** unused/dead entity implementation, **not an established client-only visual effect**. An absent renderer alone would not establish this; the full caller inventory does. No claim about the author's intent or a later TC version.
- **Port/change:** keep the existing extra registration and implementation; intentionally add no natural/ritual/client spawn.
- **JUnit:** `OrphanMobOracleContractTest.watcherIsUnregisteredDeadCodeNotAClientSpawnEffect` pins the original artifact hash and exact eight-class reference inventory, and guards against new server-side references outside the existing implementation/config/rule files.

### `cultist_portal_greater` — deliberately unreachable

- **Oracle:** `thaumcraft/common/config/ConfigEntities.java:129-130` registers **only** `EntityCultistPortal` / `CultistPortal`. `thaumcraft/common/tiles/TileEldritchLock.java:319-321` constructs and inserts that ordinary type in the cultist boss room. No `EntityCultistPortalGreater` class or class-byte reference exists anywhere in the original JAR.
- **Interpretation:** the suggested greater-portal ritual is not a TC4 4.2.3.5 mechanic. The actual TC4 boss portal is the separate, already reachable `cultist_portal`; a second spawn for its modern alias would invent gameplay.
- **Port/change:** retain alias behavior; clarify its class comment with the exact original caller and deliberate no-survival-path decision. Do not redirect the existing boss-room creator.
- **JUnit:** `OrphanMobOracleContractTest.greaterPortalIsNotTheOriginalCultistBoss` pins absence of the greater type/callers and presence of the ordinary boss class, plus the server-reference tripwire.

### `taint_crawler` — deliberately unreachable

- **Oracle:** no `EntityTaintCrawler` class or class-byte reference in the JAR; `thaumcraft/common/config/ConfigEntities.java:139-164` lists the actual taint entity registrations. `thaumcraft/common/blocks/BlockTaint.java:188-196` consumes taint block metadata 0 and spawns **EntityTaintSporeSwarmer**, not a crawler.
- **Interpretation:** no TC4 crawler spread mechanism is missing. The retained port class already identifies its implementation as a TC6 1.12.2 extra, not TC4 specification.
- **Port/change:** no gameplay edit; keep the later extra deliberately without survival creation.
- **JUnit:** `OrphanMobOracleContractTest.crawlerIsNotAnOriginalTaintSpreadProduct` pins original absence and guards new server references.

### `taint_seed` — deliberately unreachable

- **Oracle:** no `EntityTaintSeed` class or class-byte reference in the JAR; `thaumcraft/common/config/ConfigEntities.java:139-164`. `thaumcraft/common/blocks/BlockTaintFibres.java:181-185` changes metadata 3 to 4 and spawns **EntityTaintSpore**, not a seed. `:194-204` spreads fibre blocks/metadata, not seeds.
- **Interpretation:** TC4 spread is spore/fibre-based, not the later seed ecology. Existing port class Javadoc explicitly identifies the TC6 donor; its ability to spread after manual construction is not a survival creator.
- **Port/change:** no gameplay edit; no imported seed growth rule.
- **JUnit:** `OrphanMobOracleContractTest.seedIsNotAnOriginalTaintSpreadProduct` pins original absence and guards new server references.

### `taint_seed_prime` — deliberately unreachable

- **Oracle:** no `EntityTaintSeedPrime` class or class-byte reference in the JAR; same complete registration list `thaumcraft/common/config/ConfigEntities.java:139-164`, actual spore branch `thaumcraft/common/blocks/BlockTaintFibres.java:181-185` and spread routine `:194-204`.
- **Interpretation:** later extra, not a missing TC4 advanced seed or ritual product.
- **Port/change:** no gameplay edit; no new creation rule.
- **JUnit:** `OrphanMobOracleContractTest.primeSeedIsNotAnOriginalTaintSpreadProduct` pins original absence and guards new server references.

## Test scope and remaining work

The five JUnit contracts read the exact bundled original artifact (hash checked), rather than relying on an external oracle directory at test time. The static port-reference tripwires cover common server gameplay and Fabric main sources; they are **not** dynamic impossibility proofs or a complete reflective/data-driven creator detector. Allowed implementation/config files retain manual constructors, registrations, eggs and attribute setup, not survival creators. Any intentional new gameplay must revisit these citations rather than silently importing TC5/TC6 behavior.

No reachable target remains in this five-mob set, so no positive mechanism GameTest was added: directly constructing an orphan and asserting its existence would falsely claim survival reachability. Existing ordinary portal/spore/swarmer paths are outside this task; their full acquisition GameTests remain deferred as in `SPAWN_FINDINGS.md`. No new client, multiplayer, NeoForge or full-survival acceptance claim.

Execution results and exact command: `docs/analysis/2026-10-23-agent-g15client.md`.

**Verified:** all five JUnit contracts passed (zero failures/errors/skips); actual common and Fabric production sources compiled, `BUILD SUCCESSFUL in 2m 28s`. No runtime GameTest was run or claimed.
