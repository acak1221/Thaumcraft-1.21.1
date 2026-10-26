package thaumcraft.eldritch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/** Pins the actual immutable TC4 artifact, not a later donor's spawn rules. */
class OrphanMobOracleContractTest {
    private static final Path ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final String SHA256 = "3dba9786966974701578a658d1bb369bf35bdf5f363079f5ac9c4910a39113be";
    private static final String CONFIG = "thaumcraft/common/config/ConfigEntities.class";

    @Test void watcherIsUnregisteredDeadCodeNotAClientSpawnEffect() throws Exception {
        // Oracle ConfigEntities.java:86-167 (no registration), :206 (whitelist only).
        // ClientProxy.java:464-517 has no Watcher renderer registration or creator.
        try (var jar = oracle()) {
            assertNotNull(jar.getEntry("thaumcraft/common/entities/monster/EntityWatcher.class"));
            assertEquals(Set.of(CONFIG,
                    "thaumcraft/client/renderers/entity/RenderWatcher.class",
                    "thaumcraft/client/renderers/models/entities/ModelWatcher.class",
                    "thaumcraft/common/entities/monster/EntityWatcher.class",
                    "thaumcraft/common/entities/monster/EntityWatcher$1.class",
                    "thaumcraft/common/entities/monster/EntityWatcher$AIGuardianAttack.class",
                    "thaumcraft/common/entities/monster/EntityWatcher$GuardianLookHelper.class",
                    "thaumcraft/common/entities/monster/EntityWatcher$GuardianMoveHelper.class"),
                    references(jar, "EntityWatcher"), "No original external creator, including client effects");
        }
        noNewServerReferences("EntityWatcher", "WATCHER", "watcher", Set.of(
                "config/ThaumcraftEldritchMobs.java", "entities/monster/EntityWatcher.java",
                "lib/mods/ChampionRules.java", "lib/world/EldritchMobRules.java"));
    }

    @Test void greaterPortalIsNotTheOriginalCultistBoss() throws Exception {
        // Oracle ConfigEntities.java:129-130; TileEldritchLock.java:319-321 creates CultistPortal.
        absentFromOracle("EntityCultistPortalGreater");
        try (var jar = oracle()) {
            assertNotNull(jar.getEntry("thaumcraft/common/entities/monster/boss/EntityCultistPortal.class"));
        }
        noNewServerReferences("EntityCultistPortalGreater", "CULTIST_PORTAL_GREATER", "cultist_portal_greater",
                Set.of("config/ThaumcraftCultists.java", "entities/monster/EntityCultistPortalGreater.java"));
    }

    @Test void crawlerIsNotAnOriginalTaintSpreadProduct() throws Exception {
        // Oracle ConfigEntities.java:139-164; BlockTaint.java:188-196 creates SporeSwarmer instead.
        absentFromOracle("EntityTaintCrawler");
        noNewServerReferences("EntityTaintCrawler", "TAINT_CRAWLER", "taint_crawler",
                Set.of("config/ThaumcraftTaintAnimals.java", "entities/monster/EntityTaintCrawler.java",
                        "lib/taint/TaintAnimalRules.java"));
    }

    @Test void seedIsNotAnOriginalTaintSpreadProduct() throws Exception {
        // Oracle BlockTaintFibres.java:181-185 creates Spore, not Seed; ConfigEntities.java:139-164.
        absentFromOracle("EntityTaintSeed");
        noNewServerReferences("EntityTaintSeed", "TAINT_SEED", "taint_seed",
                Set.of("config/ThaumcraftTaintAnimals.java", "entities/monster/EntityTaintSeed.java",
                        "entities/monster/EntityTaintSeedPrime.java", "lib/taint/TaintAnimalRules.java"));
    }

    @Test void primeSeedIsNotAnOriginalTaintSpreadProduct() throws Exception {
        // Oracle BlockTaintFibres.java:194-204 spreads fibre metadata; ConfigEntities.java:139-164.
        absentFromOracle("EntityTaintSeedPrime");
        noNewServerReferences("EntityTaintSeedPrime", "TAINT_SEED_PRIME", "taint_seed_prime",
                Set.of("config/ThaumcraftTaintAnimals.java", "entities/monster/EntityTaintSeedPrime.java"));
    }

    private static ZipFile oracle() throws Exception {
        Path path = ROOT.resolve("Thaumcraft-1.7.10-4.2.3.5.jar");
        // The original JAR is Azanor's release and is not tracked; it is checked where it is present.
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isRegularFile(path), "TC4 4.2.3.5 JAR not present at the repository root");
        assertEquals(SHA256, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(path))), "Oracle citation requires the exact TC4 4.2.3.5 artifact");
        return new ZipFile(path.toFile());
    }

    private static Set<String> references(ZipFile jar, String token) throws IOException {
        Set<String> result = new TreeSet<>();
        for (var entry : jar.stream().filter(e -> e.getName().endsWith(".class")).toList()) {
            try (var in = jar.getInputStream(entry)) {
                if (new String(in.readAllBytes(), StandardCharsets.ISO_8859_1).contains(token)) {
                    result.add(entry.getName());
                }
            }
        }
        return result;
    }

    private static void absentFromOracle(String type) throws Exception {
        try (var jar = oracle()) {
            assertTrue(references(jar, type).isEmpty(), type + " has no class or caller in TC4 4.2.3.5");
        }
    }

    /** Static tripwire, not a dynamic impossibility proof: new creators must revisit the oracle. */
    private static void noNewServerReferences(String type, String field, String id, Set<String> allowed) throws IOException {
        Path common = ROOT.resolve("common/src/main/java/thaumcraft/common");
        for (Path base : List.of(common, ROOT.resolve("fabric/src/main/java"))) {
            try (var files = Files.walk(base)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String relative = base.relativize(file).toString().replace('\\', '/');
                    if (base.equals(common) && allowed.contains(relative)) continue;
                    if (relative.contains("/client/")) continue;
                    String source = Files.readString(file);
                    assertFalse(source.contains(type) || source.contains(field) || source.contains('"' + id + '"'),
                            file + ": deliberately unreachable in TC4; see docs/audit/ORPHAN_MOB_FINDINGS.md");
                }
            }
        }
    }
}
