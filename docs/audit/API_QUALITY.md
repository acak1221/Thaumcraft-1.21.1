# Public API quality audit — c1api

Baseline: `3340abe7`, branch `agent/c1api`, 2026-10-23. Scope: **93 Java files**, **6,838 baseline lines**, **103 public types** (including nested types), **998 declared public members**: 688 methods, 73 explicit constructors, 237 fields/enum constants. Package-info is included in the file review, not counted as a type. Implicit default constructors and inherited members are not separately counted. Lines below refer to the baseline unless marked fixed; the only modified API source is WandTriggerRegistry.

**Verdict: not ready to describe as a stable, minimal, misuse-resistant addon API.** Native gameplay tests and compatibility scaffolding exist, but ownership, units, callback failure boundaries and which compatibility surfaces actually participate in gameplay remain unclear. This is a maintainer review, not a claim that ordinary gameplay fails.

## Method and limits

- Read all 93 API files. Parsed declarations with the Java 21 compiler tree API, without dependency resolution; counted implicit-public interface members and enum constants, not regex-only declarations.
- Searched tracked Java across the entire worktree, including retained `src/main/java` and all loader modules. Comments, strings and imports were excluded from the caller-token index. External means a different **declared Java package**, not merely another file. Test uses are distinguished from production uses.
- Caller evidence is deliberately conservative lexical evidence, **not resolved call graphs**: overloads/common field names can hide unused declarations. A zero-hit entry is a reliable candidate for review; a positive token hit is not proof of a caller of that exact member. No external addon corpus or authoritative 4.2.3.5 signature manifest was available in this worktree review. **Do not narrow published access on this evidence alone.** Consequently the no-caller list is a lower bound, not a claim of exact external addon usage.
- Test evidence includes active common unit/GameTests and Fabric unit/GameTests/client tests. Retained root `src/test` is searched for usage but receives **no modern contract-coverage credit**. Class.forName presence checks alone are not behavioral contract coverage. A listed test means partial evidence exists, not that every method/edge case is tested or that it passed in this audit.
- Severity: **blocker** = shared answer corruption or an advertised extension path that cannot work; **should-fix** = missing/ambiguous contract or exposure requiring an explicit compatibility decision; **nit** = naming/retained-surface hygiene. Findings in different sections may describe the same underlying issue; never sum all rows as independent defects.

## 1. Public surface discipline

**Inspected: 103 public types + 998 members. Found: 142 members with no external-production name occurrence.** These are observations, not 142 safe removals. The ledger below lists every zero-hit member and its disposition; the full member ledger at the end makes the remaining inspections visible.

Three concrete minimality decisions need a maintainer:

- **should-fix S01** — `common/src/main/java/thaumcraft/api/crafting/ArcaneRecipes.java:78`: `rows(Object[],int[])` is parsing implementation detail, but retained legacy code also has the name rows, which defeats a naive name-only unused check. Modern callers are the same crafting package. Prefer package-private after checking exact historical publication/addon use; not narrowed here.
- **should-fix S02** — `common/src/main/java/thaumcraft/api/research/ResearchPage.java:153`: `legacyType(String)` interprets extracted Java-expression strings, not an addon domain value. Its only modern main caller is in this class. Prefer private once compatibility policy is approved; tests/provenance tools may currently depend on it.
- **should-fix S03** — `common/src/main/java/thaumcraft/api/visnet/VisNetHandler.java:46`: native machine grant validators (`requireAmuletGrant`, `requireWorkbenchGrant` :54, `requireAdvancedFurnaceGrant` :68) require public cross-package calls, but expose machine-specific implementation in the addon package. They cannot simply be made package-private. Move behind a deliberately internal cross-package bridge in a separately reviewed change.

No access changes were made: absence of in-repository use is not absence of addon use, and the original publication boundary was not independently verified.

### Zero external-production-token ledger

Each line carries an observation severity and an explicit disposition. “Retain” does not assert good documentation or successful runtime integration; those are audited separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/BlockCoordinates.java:75` — `BlockCoordinates.compareWorldCoordinate(BlockCoordinates)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/ItemApi.java:66` — `ItemApi.getBlockItem(String)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:75` — `ThaumcraftApi.armorMatVoidFortress()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:86` — `ThaumcraftApi.int enchantFrugal`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:87` — `ThaumcraftApi.int enchantPotency`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:88` — `ThaumcraftApi.int enchantWandFortune`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:92` — `ThaumcraftApi.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANTMENT_FRUGAL`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:94` — `ThaumcraftApi.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANTMENT_POTENCY`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:96` — `ThaumcraftApi.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANTMENT_WAND_FORTUNE`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:98` — `ThaumcraftApi.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANTMENT_HASTE`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:100` — `ThaumcraftApi.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANTMENT_REPAIR`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:200` — `ThaumcraftApiHelper.getAllAspects(int)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:209` — `ThaumcraftApiHelper.getAllCompoundAspects(int)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/ThaumcraftMaterials.java:29` — `ThaumcraftMaterials.ArmorMaterial ARMORMAT_SPECIAL`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/ThaumcraftMaterials.java:39` — `ThaumcraftMaterials.ArmorMaterial ARMORMAT_VOID_FORTRESS`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/ThaumcraftMaterials.java:58` — `ThaumcraftMaterials.armorMatVoidRobe()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/WorldCoordinates.java:56` — `WorldCoordinates.compareWorldCoordinate(WorldCoordinates)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/aspects/Aspect.java:127` — `Aspect.setBlend(int)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/aspects/Aspect.java:130` — `Aspect.setChatcolor(String)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:88` — `AspectList.getAspectsSortedByAmount()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/aspects/AspectRegistryEvent.java:27` — `AspectRegistryEvent.removeListener(Consumer<AspectRegistryEvent>)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/aspects/AspectRegistryEvent.java:32` — `AspectRegistryEvent.listenerCount()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/blocks/BlocksTC.java:74` — `BlocksTC.auraNode()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/blocks/BlocksTC.java:77` — `BlocksTC.silverwoodKnot()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:17` — `IPlayerKnowledge.EnumKnowledgeType.EnumKnowledgeType THEORY`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:30` — `IPlayerKnowledge.EnumKnowledgeType.getProgression()`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:32` — `IPlayerKnowledge.EnumKnowledgeType.hasFields()`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:34` — `IPlayerKnowledge.EnumKnowledgeType.getAbbreviation()`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:38` — `IPlayerKnowledge.EnumResearchStatus.EnumResearchStatus IN_PROGRESS`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:41` — `IPlayerKnowledge.EnumResearchFlag.EnumResearchFlag POPUP`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:46` — `IPlayerKnowledge.getResearchStatus(String)`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:51` — `IPlayerKnowledge.isResearchKnown(String)`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:54` — `IPlayerKnowledge.getResearchStage(String)`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:58` — `IPlayerKnowledge.setResearchStage(String, int)`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:65` — `IPlayerKnowledge.setResearchFlag(String, EnumResearchFlag)`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:67` — `IPlayerKnowledge.clearResearchFlag(String, EnumResearchFlag)`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:69` — `IPlayerKnowledge.hasResearchFlag(String, EnumResearchFlag)`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:72` — `IPlayerKnowledge.addKnowledge(EnumKnowledgeType, ResearchCategory, int)`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:78` — `IPlayerKnowledge.getKnowledgeRaw(EnumKnowledgeType, ResearchCategory)`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:27` — `NativePlayerKnowledge.<init>(PlayerKnowledge)`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:36` — `NativePlayerKnowledge.getResearchStatus(String)`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:48` — `NativePlayerKnowledge.isResearchKnown(String)`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:52` — `NativePlayerKnowledge.getResearchStage(String)`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:67` — `NativePlayerKnowledge.setResearchStage(String, int)`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:82` — `NativePlayerKnowledge.setResearchFlag(String, EnumResearchFlag)`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:88` — `NativePlayerKnowledge.clearResearchFlag(String, EnumResearchFlag)`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:94` — `NativePlayerKnowledge.hasResearchFlag(String, EnumResearchFlag)`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:100` — `NativePlayerKnowledge.addKnowledge(EnumKnowledgeType, ResearchCategory, int)`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:113` — `NativePlayerKnowledge.getKnowledgeRaw(EnumKnowledgeType, ResearchCategory)`. Retain interface/implementation contract; do not narrow an implementation of a public interface. Review unsupported TC6 semantics separately.
- **nit observation** — `common/src/main/java/thaumcraft/api/crafting/ArcaneRecipes.java:41` — `ArcaneRecipes.aspectCosts(AspectList)`. Prefer package-private only after bridge/publication review; currently shared by crafting views, not a gameplay registration contract.
- **nit observation** — `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:75` — `InfusionEnchantmentRecipe.enchantmentHolder()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:128` — `InfusionRecipe.aspectCosts()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:131` — `InfusionRecipe.primaryAspect()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/crafting/NativeRecipes.java:54` — `NativeRecipes.nativeRecipes()`. Prefer package-private only after bridge/publication review; currently shared by crafting views, not a gameplay registration contract.
- **nit observation** — `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:139` — `ShapedArcaneRecipe.setMirrored(boolean)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java:71` — `DamageSourceIndirectThaumcraftEntity.setUnblockable(Entity, Entity, Holder<DamageType>)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/damagesource/DamageSourceThaumcraft.java:33` — `DamageSourceThaumcraft.ResourceKey<DamageType> DISSOLVE`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/damagesource/DamageSourceThaumcraft.java:44` — `DamageSourceThaumcraft.causeSwarmDamage(LivingEntity)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/items/ItemsTC.java:80` — `ItemsTC.thaumonomicon()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/items/ItemsTC.java:89` — `ItemsTC.emptyPhial()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/items/ItemsTC.java:92` — `ItemsTC.filledPhial()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/items/ItemsTC.java:98` — `ItemsTC.researchNote()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/items/ItemsTC.java:101` — `ItemsTC.primordialPearl()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/items/ItemsTC.java:104` — `ItemsTC.ironWandCap()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:24` — `NodeAdapter.<init>(TileNode)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/IScanThing.java:12` — `IScanThing.checkThing(Player, Object)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/IScanThing.java:18` — `IScanThing.onSuccess(Player, Object)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:16` — `ResearchAddendum.getTextLocalized()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:20` — `ResearchAddendum.setText(String)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:24` — `ResearchAddendum.setRecipes(ResourceLocation[])`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:28` — `ResearchAddendum.setResearch(String[])`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchCategories.java:71` — `ResearchCategories.allResearch()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchCategory.java:14` — `ResearchCategory.ResourceLocation background2`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:30` — `ResearchEntry.EnumResearchMeta.EnumResearchMeta SPIKY`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:31` — `ResearchEntry.EnumResearchMeta.EnumResearchMeta REVERSE`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:54` — `ResearchEntry.setKey(String)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:58` — `ResearchEntry.setCategory(String)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:71` — `ResearchEntry.getParentsClean()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:77` — `ResearchEntry.getParentsStripped()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:90` — `ResearchEntry.setDisplayColumn(int)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:94` — `ResearchEntry.setDisplayRow(int)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:98` — `ResearchEntry.setIcons(Object[])`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:106` — `ResearchEntry.setMeta(EnumResearchMeta[])`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:136` — `ResearchItem.setVirtual()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:209` — `ResearchItem.setComplexity(int)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **should-fix observation** — `common/src/main/java/thaumcraft/api/research/ResearchPage.java:153` — `ResearchPage.legacyType(String)`. Prefer private after publication review; corpus-expression parser is implementation detail (S02).
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:27` — `ResearchStage.Knowledge.<init>(EnumKnowledgeType, ResearchCategory, int)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:36` — `ResearchStage.getTextLocalized()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:40` — `ResearchStage.setText(String)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:44` — `ResearchStage.setRecipes(ResourceLocation[])`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:46` — `ResearchStage.getObtain()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:48` — `ResearchStage.setObtain(Object[])`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:50` — `ResearchStage.getCraft()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:52` — `ResearchStage.setCraft(Object[])`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:54` — `ResearchStage.getCraftReference()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:56` — `ResearchStage.setCraftReference(int[])`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:58` — `ResearchStage.getKnow()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:60` — `ResearchStage.setKnow(Knowledge[])`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:64` — `ResearchStage.setResearch(String[])`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:66` — `ResearchStage.getResearchIcon()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:68` — `ResearchStage.setResearchIcon(String[])`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ScanBlock.java:18` — `ScanBlock.<init>(Block)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ScanBlock.java:22` — `ScanBlock.<init>(String, Block[])`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ScanBlock.java:27` — `ScanBlock.checkThing(Player, Object)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ScanningManager.java:18` — `ScanningManager.addScannableThing(IScanThing)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ScanningManager.java:23` — `ScanningManager.scannableThings()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/ITheorycraftAid.java:8` — `ITheorycraftAid.getAidObject()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/ITheorycraftAid.java:11` — `ITheorycraftAid.getCards()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:16` — `ResearchTableData.int inspiration`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:17` — `ResearchTableData.int inspirationStart`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:18` — `ResearchTableData.int bonusDraws`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:19` — `ResearchTableData.TreeMap<String, Integer> categoryTotals`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:21` — `ResearchTableData.<init>(BlockEntity)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:25` — `ResearchTableData.<init>(Player, BlockEntity)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:30` — `ResearchTableData.hasTotal(String)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:39` — `ResearchTableData.addTotal(String, int)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:43` — `ResearchTableData.addInspiration(int)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:23` — `TheorycraftCard.isAidOnly()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:25` — `TheorycraftCard.getInspirationCost()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:35` — `TheorycraftCard.getRequiredItems()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:38` — `TheorycraftCard.getRequiredItemsConsumed()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:46` — `TheorycraftCard.serialize()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:52` — `TheorycraftCard.deserialize(CompoundTag)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftManager.java:11` — `TheorycraftManager.HashMap<String, ITheorycraftAid> aids`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftManager.java:12` — `TheorycraftManager.HashMap<String, Class<TheorycraftCard>> cards`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftManager.java:16` — `TheorycraftManager.registerAid(ITheorycraftAid)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftManager.java:21` — `TheorycraftManager.registerCard(Class<?>)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftManager.java:28` — `TheorycraftManager.card(String)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftManager.java:31` — `TheorycraftManager.aid(String)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:39` — `TileVisNode.getAttachmentVersion()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:64` — `TileVisNode.getChildren()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:72` — `TileVisNode.getRootSource()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/wands/WandCap.java:62` — `WandCap.nativeCap()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/wands/WandCap.java:72` — `WandCap.setTexture(ResourceLocation)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/wands/WandCap.java:84` — `WandCap.setCraftCost(int)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/wands/WandCap.java:90` — `WandCap.byTag(String)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:71` — `WandRod.nativeRod()`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:79` — `WandRod.setCapacity(int)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:83` — `WandRod.setTexture(ResourceLocation)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:91` — `WandRod.setCraftCost(int)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:95` — `WandRod.setOnUpdate(IWandRodOnUpdate)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.
- **nit observation** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:105` — `WandRod.byTag(String)`. Retain public: addon-facing compatibility/configuration/read surface; no in-tree caller is not removal proof.

### Types with no external-production name occurrence

**22 type observations**, separate from the 142 member observations above. Marker interfaces, subclass contracts and addon facades legitimately may have no mod-internal caller. Keep them public unless a verified compatibility policy retires them; none was narrowed.

- **nit observation** — `common/src/main/java/thaumcraft/api/ItemApi.java:22` `ItemApi`. Retain as addon-facing type or explicitly classify the compatibility-only feature; do not remove solely for zero internal production use.
- **nit observation** — `common/src/main/java/thaumcraft/api/aspects/AspectHelper.java:12` `AspectHelper`. Retain as addon-facing type or explicitly classify the compatibility-only feature; do not remove solely for zero internal production use.
- **nit observation** — `common/src/main/java/thaumcraft/api/aspects/AspectSourceHelper.java:23` `AspectSourceHelper`. Retain as addon-facing type or explicitly classify the compatibility-only feature; do not remove solely for zero internal production use.
- **nit observation** — `common/src/main/java/thaumcraft/api/aura/AuraHelper.java:18` `AuraHelper`. Retain as addon-facing type or explicitly classify the compatibility-only feature; do not remove solely for zero internal production use.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:38` `IPlayerKnowledge.EnumResearchStatus`. Retain as addon-facing type or explicitly classify the compatibility-only feature; do not remove solely for zero internal production use.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:41` `IPlayerKnowledge.EnumResearchFlag`. Retain as addon-facing type or explicitly classify the compatibility-only feature; do not remove solely for zero internal production use.
- **nit observation** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:22` `NativePlayerKnowledge`. Retain as addon-facing type or explicitly classify the compatibility-only feature; do not remove solely for zero internal production use.
- **nit observation** — `common/src/main/java/thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java:28` `DamageSourceIndirectThaumcraftEntity`. Retain as addon-facing type or explicitly classify the compatibility-only feature; do not remove solely for zero internal production use.
- **nit observation** — `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:21` `NodeAdapter`. Retain as addon-facing type or explicitly classify the compatibility-only feature; do not remove solely for zero internal production use.
- **nit observation** — `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:14` `NodeBase`. Retain as addon-facing type or explicitly classify the compatibility-only feature; do not remove solely for zero internal production use.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/IScanThing.java:10` `IScanThing`. Retain as addon-facing type or explicitly classify the compatibility-only feature; do not remove solely for zero internal production use.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:9` `ResearchAddendum`. Retain as addon-facing type or explicitly classify the compatibility-only feature; do not remove solely for zero internal production use.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchCategoriesCompat.java:9` `ResearchCategoriesCompat`. Retain as addon-facing type or explicitly classify the compatibility-only feature; do not remove solely for zero internal production use.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:10` `ResearchStage`. Retain as addon-facing type or explicitly classify the compatibility-only feature; do not remove solely for zero internal production use.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:22` `ResearchStage.Knowledge`. Retain as addon-facing type or explicitly classify the compatibility-only feature; do not remove solely for zero internal production use.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ScanBlock.java:14` `ScanBlock`. Retain as addon-facing type or explicitly classify the compatibility-only feature; do not remove solely for zero internal production use.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/ScanningManager.java:13` `ScanningManager`. Retain as addon-facing type or explicitly classify the compatibility-only feature; do not remove solely for zero internal production use.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/ITheorycraftAid.java:6` `ITheorycraftAid`. Retain as addon-facing type or explicitly classify the compatibility-only feature; do not remove solely for zero internal production use.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:13` `ResearchTableData`. Retain as addon-facing type or explicitly classify the compatibility-only feature; do not remove solely for zero internal production use.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:14` `TheorycraftCard`. Retain as addon-facing type or explicitly classify the compatibility-only feature; do not remove solely for zero internal production use.
- **nit observation** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftManager.java:10` `TheorycraftManager`. Retain as addon-facing type or explicitly classify the compatibility-only feature; do not remove solely for zero internal production use.
- **nit observation** — `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:15` `IWandFocus`. Retain as addon-facing type or explicitly classify the compatibility-only feature; do not remove solely for zero internal production use.

## 2. Contract documentation

**Inspected: 864 public type/callable declarations** (103 types + 688 methods + 73 constructors), plus package-info and field ownership/unit documentation. **510 missing-local-contract findings** after explicitly excluding 22 inherited Java/Minecraft or already-documented node accessors; **33 additional documented-contract findings** below. Raw local-Javadoc absence is 532 declarations; 210 fields also have no individual local Javadoc and are recorded in the member ledger, not added to the callable finding count. An enum constant may be adequately documented by its type.

An overriding method is not automatically required to repeat inherited Javadoc. Overrides whose public API root also lacks a usable contract remain in the gap list: the addon still has no contract to inherit. Missing entries below are **should-fix**, not automatic requests for boilerplate: put the contract at the public interface/base method where appropriate, document null/ownership only where applicable, and give units for quantities.

### Existing documentation that is incomplete or misleading

- **should-fix D01** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:86`: areItemsEqual Javadoc promises false for a missing stack; the implementation returns true for two null/empty stacks. State both cases and that components are ignored.
- **should-fix D02** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:39`: cullTags starts by promising a cap of six, then documents an escape that can return seven or more. Lead with the bounded-effort exception; specify null input and ownership of the returned list.
- **should-fix D03** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:230`: getCraftingRecipes returns a new list, but programmatic recipe elements remain live mutable registrations. Neither the ownership boundary nor supported mutation/lifecycle/thread is stated.
- **should-fix D04** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:315`: getCraftingRecipeKey does not specify the Object[2] element types, page-index origin, null cases, cache scope or ownership.
- **should-fix D05** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:403`: getWarpForResearch describes a bridge, not its non-null precondition or units. It exposes a common-layer ResearchDefinition in the published contract.
- **should-fix D06** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:472`: addLootBagItem documents reference retention but omits weight validity, rarity numbers 0/1/2 and lifecycle/thread requirements.
- **should-fix D07** — `common/src/main/java/thaumcraft/api/aspects/Aspect.java:75`: Aspect constructor documentation omits registration as a side effect, duplicate-tag exception, non-null tag/image, RGB/blend interpretation and whether component entries may be null.
- **should-fix D08** — `common/src/main/java/thaumcraft/api/aspects/Aspect.java:121`: setTag accepts null and does not re-key Aspect.aspects. Explain identity versus display tag and the registry consequences before addons retune it.
- **should-fix D09** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:102`: add documents null aspects, but not negative amounts, integer overflow, aspect units versus centivis or receiver mutation/fluent return. reduce/remove have different signed-amount rules.
- **nit D10** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:63`: Amount sorting is documented precisely, but returned-array ownership and reliance on the publicly writable map are unstated.
- **should-fix D11** — `common/src/main/java/thaumcraft/api/aspects/AspectSourceHelper.java:34`: drainEssentia omits null tile/aspect => false, null direction => unrestricted search, range in blocks and the caller-side/world-thread requirement; it may mutate a container and send FX.
- **should-fix D12** — `common/src/main/java/thaumcraft/api/aspects/AspectSourceHelper.java:60`: findEssentia omits the same null/direction/range contract and whether the search can load chunks or invoke addon callbacks.
- **should-fix D13** — `common/src/main/java/thaumcraft/api/aspects/AspectEventProxy.java:44`: String registerComplexObjectTag claims recipe-derived base inclusion, but delegates directly to simple registerObjectTag. Do not promise complex semantics this implementation does not perform.
- **should-fix D14** — `common/src/main/java/thaumcraft/api/aspects/AspectRegistryEvent.java:35`: post says a listener that throws cannot stop others, but only RuntimeException is caught (not Error). Document that boundary, calling thread and whether addons may repost.
- **should-fix D15** — `common/src/main/java/thaumcraft/api/aura/AuraHelper.java:57`: drainVis documents simulate but not raw network units versus centivis, null/invalid inputs => 0, server-thread enforcement or the partial-drain return.
- **should-fix D16** — `common/src/main/java/thaumcraft/api/nodes/INode.java:30`: Type prose says the original essence amount was short, while only the base setter argument is short and other amounts are int. State the actual units/range and modifier-null policy.
- **should-fix D17** — `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:14`: Type promises current essence never exceeds base, but setNodeVisBase can lower base without reducing current essence. Describe the actual capacity/current-stock separation.
- **should-fix D18** — `common/src/main/java/thaumcraft/api/research/ResearchCategories.java:29`: loadNative describes idempotence but not global state, bootstrap dependencies, callbacks/re-entrancy or partial initialization on exception.
- **should-fix D19** — `common/src/main/java/thaumcraft/api/research/ResearchCategories.java:86`: addResearch describes overlap rejection but not silent rejection of null, missing category or duplicate key; returned void gives no acceptance signal.
- **should-fix D20** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:13`: Type says the two views never drift apart, but of copies scalars, aliases only some arrays, and setters do not write back to ResearchItem. Document snapshot/live split.
- **should-fix D21** — `common/src/main/java/thaumcraft/api/research/ResearchPage.java:94`: World-scoped smelting constructor omits that null input/level, no server or no recipe leaves recipeOutput null, and stores the original input reference.
- **should-fix D22** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:75`: knowledge returns a new array but aliases mutable Knowledge entries; unlike getKnow, no rewards becomes an empty array. State both facts.
- **should-fix D23** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:57`: name/text translate values from methods named getLocalizedName/getLocalizedText a second time. Specify that subclasses supply translation keys, not already localized strings.
- **should-fix D24** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftManager.java:10`: Type promises original registration order, but registries are HashMap. First-wins duplicate behavior is implemented; iteration order is not preserved.
- **should-fix D25** — `common/src/main/java/thaumcraft/api/crafting/NativeRecipes.java:27`: Class Javadoc links install(RecipeManager), which does not exist; actual install also requires HolderLookup.Provider. Fix link and document server/reload ownership.
- **should-fix D26** — `common/src/main/java/thaumcraft/api/crafting/NativeRecipes.java:77`: view promises null when there is no original counterpart, but returns the input object unchanged for unrecognized non-null recipe objects.
- **should-fix D27** — `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:86`: areItemStacksEqual says fuzzy reports that an ore shortcut was requested, but the boolean is entirely ignored; specify this and the directional component/wildcard comparison.
- **should-fix D28** — `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:25`: getVisCost correctly names centivis, but not null => no cost, returned-list ownership or whether the wand/recipe APIs use the same units.
- **should-fix D29** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:16`: Class prose suggests native-catalogue binding, but addon constructors only register in rods and nativeRod remains null; document what native wand operations actually consume.
- **should-fix D30** — `common/src/main/java/thaumcraft/api/wands/WandCap.java:15`: The analogous cap prose does not define nativeCap == null, mutable registration lifetime, discount multiplier units or craftCost units.
- **should-fix D31** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:19`: Claims original 1.7.10 signatures still compile although parameters are now native 1.21.1 classes. This is source adaptation, not source/binary compatibility.
- **should-fix D32** — `common/src/main/java/thaumcraft/api/package-info.java:4`: Package-wide promise that every class is bound to native implementation is too strong: theorycraft and ScanningManager have no gameplay caller, and several default handler services are inert.
- **nit D33** — `common/src/main/java/thaumcraft/api/IWarpingGear.java:8`: Type says warp events are not ported yet although current common warp/event code exists. Replace historical progress prose with a stable caller contract.

### Missing contract inventory

Every line identifies the individual declaration, not just its containing file. Constructors need registration/validation/ownership side effects; callbacks need side, thread, failure and return semantics; numeric accessors need units, and mutable getters need ownership.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/IRunicArmor.java:5` — `IRunicArmor.INTERFACE`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/IVisDiscountGear.java:7` — `IVisDiscountGear.INTERFACE`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:497` — `ThaumcraftApi.EntityTagsNBT.CLASS`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/IRevealer.java:6` — `IRevealer.INTERFACE`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:28` — `ResearchEntry.EnumResearchMeta.ENUM`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchPage.java:175` — `ResearchPage.PageType.ENUM`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:124` — `ItemFocusBasic.WandFocusAnimation.ENUM`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/BlockCoordinates.java:32` — `BlockCoordinates.<init>()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/BlockCoordinates.java:34` — `BlockCoordinates.<init>(int, int, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/BlockCoordinates.java:40` — `BlockCoordinates.<init>(BlockEntity)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/BlockCoordinates.java:47` — `BlockCoordinates.<init>(BlockCoordinates)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/IGoggles.java:8` — `IGoggles.showIngamePopups(ItemStack, LivingEntity)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/IRepairableExtended.java:8` — `IRepairableExtended.doRepair(ItemStack, Player, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/IRunicArmor.java:6` — `IRunicArmor.getRunicCharge(ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/IWarpingGear.java:11` — `IWarpingGear.getWarp(ItemStack, Player)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ItemRunic.java:15` — `ItemRunic.<init>(int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ItemRunic.java:20` — `ItemRunic.<init>(int, Item.Properties)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ItemRunic.java:25` — `ItemRunic.getRunicCharge(ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:62` — `ThaumcraftApi.armorMatThaumiumFortress()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:63` — `ThaumcraftApi.armorMatVoid()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:242` — `ThaumcraftApi.addArcaneCraftingRecipe(String, ItemStack, AspectList, Object[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:248` — `ThaumcraftApi.addShapelessArcaneCraftingRecipe(String, ItemStack, AspectList, Object[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:262` — `ThaumcraftApi.addInfusionEnchantmentRecipe(String, net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment>, int, AspectList, ItemStack[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:269` — `ThaumcraftApi.getInfusionRecipe(ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:285` — `ThaumcraftApi.addCrucibleRecipe(String, ItemStack, Object, AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:291` — `ThaumcraftApi.getCrucibleRecipe(ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:299` — `ThaumcraftApi.getCrucibleRecipeFromHash(int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:368` — `ThaumcraftApi.addWarpToItem(ItemStack, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:373` — `ThaumcraftApi.addWarpToResearch(String, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:378` — `ThaumcraftApi.getWarp(Object)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:414` — `ThaumcraftApi.registerScanEventhandler(IScanEventHandler)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:414` — `ThaumcraftApi.registerEntityTag(String, AspectList, EntityTagsNBT[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:424` — `ThaumcraftApi.registerObjectTag(Item, int, AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:427` — `ThaumcraftApi.registerObjectTag(ResourceLocation, int, AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:431` — `ThaumcraftApi.registerObjectTag(ItemStack, int[], AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:447` — `ThaumcraftApi.exists(Item, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:451` — `ThaumcraftApi.registerComplexObjectTag(ItemStack, AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:454` — `ThaumcraftApi.registerComplexObjectTag(ItemStack, AspectList, Level)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:458` — `ThaumcraftApi.registerComplexObjectTag(Item, int, AspectList, Level)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:493` — `ThaumcraftApi.EntityTags.<init>(String, AspectList, EntityTagsNBT[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:500` — `ThaumcraftApi.EntityTagsNBT.<init>(String, Object)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:106` — `ThaumcraftApiHelper.isResearchComplete(String, String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:116` — `ThaumcraftApiHelper.hasDiscoveredAspect(String, Aspect)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:120` — `ThaumcraftApiHelper.getDiscoveredAspects(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:137` — `ThaumcraftApiHelper.getObjectAspects(ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:148` — `ThaumcraftApiHelper.getBonusObjectTags(ItemStack, AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:153` — `ThaumcraftApiHelper.generateTags(Item, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:158` — `ThaumcraftApiHelper.containsMatch(boolean, ItemStack[], ItemStack[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:181` — `ThaumcraftApiHelper.itemMatches(ItemStack, ItemStack, boolean)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:189` — `ThaumcraftApiHelper.getConnectableTile(Level, int, int, int, Direction)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:193` — `ThaumcraftApiHelper.getConnectableTile(BlockGetter, int, int, int, Direction)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:200` — `ThaumcraftApiHelper.getAllAspects(int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:209` — `ThaumcraftApiHelper.getAllCompoundAspects(int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:218` — `ThaumcraftApiHelper.consumeVisFromWand(ItemStack, Player, AspectList, boolean, boolean)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:222` — `ThaumcraftApiHelper.consumeVisFromWandCrafting(ItemStack, Player, AspectList, boolean)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:226` — `ThaumcraftApiHelper.consumeVisFromInventory(Player, AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:230` — `ThaumcraftApiHelper.addWarpToPlayer(Player, int, boolean)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:234` — `ThaumcraftApiHelper.addStickyWarpToPlayer(Player, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/TileThaumcraft.java:21` — `TileThaumcraft.<init>(BlockEntityType<?>, BlockPos, BlockState)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/WorldCoordinates.java:20` — `WorldCoordinates.<init>(ResourceKey<Level>, BlockPos)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/WorldCoordinates.java:23` — `WorldCoordinates.<init>(int, int, int, ResourceKey<Level>)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/WorldCoordinates.java:29` — `WorldCoordinates.<init>(BlockEntity)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/WorldCoordinates.java:32` — `WorldCoordinates.<init>(WorldCoordinates)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/WorldCoordinates.java:33` — `WorldCoordinates.pos()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/WorldCoordinates.java:34` — `WorldCoordinates.dimension()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/WorldCoordinates.java:35` — `WorldCoordinates.isCanonicalDimension(ResourceKey<Level>)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/WorldCoordinates.java:39` — `WorldCoordinates.getDistanceSquared(int, int, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/WorldCoordinates.java:43` — `WorldCoordinates.getDistanceSquaredToWorldCoordinates(WorldCoordinates)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/WorldCoordinates.java:56` — `WorldCoordinates.compareWorldCoordinate(WorldCoordinates)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/WorldCoordinates.java:57` — `WorldCoordinates.writeNBT(CompoundTag)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/Aspect.java:86` — `Aspect.<init>(String, int, Aspect[], int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/Aspect.java:91` — `Aspect.<init>(String, int, Aspect[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/Aspect.java:95` — `Aspect.<init>(String, int, String, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/Aspect.java:100` — `Aspect.getTag()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/Aspect.java:101` — `Aspect.getColor()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/Aspect.java:102` — `Aspect.getName()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/Aspect.java:103` — `Aspect.getComponents()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/Aspect.java:104` — `Aspect.getImage()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/Aspect.java:105` — `Aspect.getBlend()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/Aspect.java:106` — `Aspect.getChatcolor()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/Aspect.java:132` — `Aspect.getAspect(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/Aspect.java:134` — `Aspect.getPrimalAspects()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/Aspect.java:140` — `Aspect.getCompoundAspects()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/Aspect.java:146` — `Aspect.getCombinationResult(Aspect, Aspect)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:37` — `AspectList.<init>()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:40` — `AspectList.copy()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:46` — `AspectList.size()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:47` — `AspectList.visSize()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:48` — `AspectList.getAmount(Aspect)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:49` — `AspectList.getAspects()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:50` — `AspectList.getPrimalAspects()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:52` — `AspectList.getAspectsSorted()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:88` — `AspectList.getAspectsSortedByAmount()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:107` — `AspectList.add(AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:112` — `AspectList.merge(Aspect, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:117` — `AspectList.merge(AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:122` — `AspectList.reduce(Aspect, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:128` — `AspectList.remove(Aspect, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:135` — `AspectList.remove(Aspect)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:140` — `AspectList.reduceToPrimals(boolean)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:154` — `AspectList.writeToNBT(CompoundTag)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:156` — `AspectList.writeToNBT(CompoundTag, String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:167` — `AspectList.readFromNBT(CompoundTag)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:169` — `AspectList.readFromNBT(CompoundTag, String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:5` — `IAspectContainer.getAspects()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:6` — `IAspectContainer.setAspects(AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:7` — `IAspectContainer.doesContainerAccept(Aspect)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:8` — `IAspectContainer.addToContainer(Aspect, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:9` — `IAspectContainer.takeFromContainer(Aspect, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:10` — `IAspectContainer.takeFromContainer(AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:11` — `IAspectContainer.doesContainerContainAmount(Aspect, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:12` — `IAspectContainer.doesContainerContain(AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:13` — `IAspectContainer.containerContains(Aspect)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/IEssentiaContainerItem.java:7` — `IEssentiaContainerItem.getAspects(ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/IEssentiaContainerItem.java:8` — `IEssentiaContainerItem.setAspects(ItemStack, AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:7` — `IEssentiaTransport.isConnectable(Direction)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:8` — `IEssentiaTransport.canInputFrom(Direction)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:9` — `IEssentiaTransport.canOutputTo(Direction)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:10` — `IEssentiaTransport.setSuction(Aspect, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:11` — `IEssentiaTransport.getSuctionType(Direction)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:12` — `IEssentiaTransport.getSuctionAmount(Direction)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:13` — `IEssentiaTransport.takeEssentia(Aspect, int, Direction)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:14` — `IEssentiaTransport.addEssentia(Aspect, int, Direction)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:15` — `IEssentiaTransport.getEssentiaType(Direction)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:16` — `IEssentiaTransport.getEssentiaAmount(Direction)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:17` — `IEssentiaTransport.getMinimumSuction()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:18` — `IEssentiaTransport.renderExtendedTube()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/baubles/IBauble.java:8` — `IBauble.getBaubleType(ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/baubles/IBauble.java:9` — `IBauble.onWornTick(ItemStack, LivingEntity)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/baubles/IBauble.java:10` — `IBauble.onEquipped(ItemStack, LivingEntity)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/baubles/IBauble.java:11` — `IBauble.onUnequipped(ItemStack, LivingEntity)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/baubles/IBauble.java:12` — `IBauble.canEquip(ItemStack, LivingEntity)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/baubles/IBauble.java:13` — `IBauble.canUnequip(ItemStack, LivingEntity)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:30` — `IPlayerKnowledge.EnumKnowledgeType.getProgression()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:32` — `IPlayerKnowledge.EnumKnowledgeType.hasFields()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:34` — `IPlayerKnowledge.EnumKnowledgeType.getAbbreviation()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:46` — `IPlayerKnowledge.getResearchStatus(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:48` — `IPlayerKnowledge.isResearchComplete(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:56` — `IPlayerKnowledge.addResearch(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:58` — `IPlayerKnowledge.setResearchStage(String, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:60` — `IPlayerKnowledge.removeResearch(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:65` — `IPlayerKnowledge.setResearchFlag(String, EnumResearchFlag)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:67` — `IPlayerKnowledge.clearResearchFlag(String, EnumResearchFlag)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:69` — `IPlayerKnowledge.hasResearchFlag(String, EnumResearchFlag)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:27` — `NativePlayerKnowledge.<init>(PlayerKnowledge)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:34` — `NativePlayerKnowledge.clear()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:36` — `NativePlayerKnowledge.getResearchStatus(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:44` — `NativePlayerKnowledge.isResearchComplete(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:48` — `NativePlayerKnowledge.isResearchKnown(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:52` — `NativePlayerKnowledge.getResearchStage(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:60` — `NativePlayerKnowledge.addResearch(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:67` — `NativePlayerKnowledge.setResearchStage(String, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:73` — `NativePlayerKnowledge.removeResearch(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:80` — `NativePlayerKnowledge.getResearchList()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:82` — `NativePlayerKnowledge.setResearchFlag(String, EnumResearchFlag)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:88` — `NativePlayerKnowledge.clearResearchFlag(String, EnumResearchFlag)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:94` — `NativePlayerKnowledge.hasResearchFlag(String, EnumResearchFlag)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:100` — `NativePlayerKnowledge.addKnowledge(EnumKnowledgeType, ResearchCategory, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:108` — `NativePlayerKnowledge.getKnowledge(EnumKnowledgeType, ResearchCategory)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:113` — `NativePlayerKnowledge.getKnowledgeRaw(EnumKnowledgeType, ResearchCategory)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:120` — `NativePlayerKnowledge.sync(ServerPlayer)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/CrucibleRecipe.java:27` — `CrucibleRecipe.<init>(String, ItemStack, Object, AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/CrucibleRecipe.java:67` — `CrucibleRecipe.catalystMatches(ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/CrucibleRecipe.java:82` — `CrucibleRecipe.getRecipeOutput()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/IArcaneRecipe.java:16` — `IArcaneRecipe.matches(ArcaneRecipeInput, Level, Player)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/IArcaneRecipe.java:18` — `IArcaneRecipe.getCraftingResult(ArcaneRecipeInput)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/IArcaneRecipe.java:20` — `IArcaneRecipe.getRecipeSize()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/IArcaneRecipe.java:22` — `IArcaneRecipe.getRecipeOutput()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/IArcaneRecipe.java:24` — `IArcaneRecipe.getAspects()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/IArcaneRecipe.java:26` — `IArcaneRecipe.getAspects(ArcaneRecipeInput)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/IArcaneRecipe.java:28` — `IArcaneRecipe.getResearch()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/IInfusionStabiliser.java:8` — `IInfusionStabiliser.canStabaliseInfusion(Level, BlockPos)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:35` — `InfusionEnchantmentRecipe.<init>(String, Holder<Enchantment>, int, AspectList, ItemStack[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:77` — `InfusionEnchantmentRecipe.matches(ArrayList<ItemStack>, ItemStack, Level, Player)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:88` — `InfusionEnchantmentRecipe.getEnchantment()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:90` — `InfusionEnchantmentRecipe.getAspects()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:92` — `InfusionEnchantmentRecipe.getResearch()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:34` — `InfusionRecipe.<init>(String, Object, int, AspectList, ItemStack, ItemStack[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:73` — `InfusionRecipe.matches(ArrayList<ItemStack>, ItemStack, Level, Player)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:95` — `InfusionRecipe.getRecipeOutput()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:97` — `InfusionRecipe.getAspects()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:99` — `InfusionRecipe.getInstability()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:101` — `InfusionRecipe.getResearch()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:103` — `InfusionRecipe.getRecipeInput()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:105` — `InfusionRecipe.getComponents()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:107` — `InfusionRecipe.getRecipeOutput(ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:109` — `InfusionRecipe.getAspects(ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:111` — `InfusionRecipe.getInstability(ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:41` — `ShapedArcaneRecipe.<init>(String, Block, AspectList, Object[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:45` — `ShapedArcaneRecipe.<init>(String, Item, AspectList, Object[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:49` — `ShapedArcaneRecipe.<init>(String, ItemStack, AspectList, Object[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:126` — `ShapedArcaneRecipe.getCraftingResult(ArcaneRecipeInput)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:130` — `ShapedArcaneRecipe.getRecipeSize()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:132` — `ShapedArcaneRecipe.getRecipeOutput()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:134` — `ShapedArcaneRecipe.matches(ArcaneRecipeInput, Level, Player)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:139` — `ShapedArcaneRecipe.setMirrored(boolean)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:165` — `ShapedArcaneRecipe.getAspects()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:167` — `ShapedArcaneRecipe.getAspects(ArcaneRecipeInput)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:169` — `ShapedArcaneRecipe.getResearch()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:33` — `ShapelessArcaneRecipe.<init>(String, Block, AspectList, Object[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:37` — `ShapelessArcaneRecipe.<init>(String, Item, AspectList, Object[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:41` — `ShapelessArcaneRecipe.<init>(String, ItemStack, AspectList, Object[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:68` — `ShapelessArcaneRecipe.getRecipeSize()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:70` — `ShapelessArcaneRecipe.getRecipeOutput()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:72` — `ShapelessArcaneRecipe.getCraftingResult(ArcaneRecipeInput)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:76` — `ShapelessArcaneRecipe.matches(ArcaneRecipeInput, Level, Player)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:102` — `ShapelessArcaneRecipe.getAspects()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:104` — `ShapelessArcaneRecipe.getAspects(ArcaneRecipeInput)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:106` — `ShapelessArcaneRecipe.getResearch()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:16` — `DummyInternalMethodHandler.generateVisEffect(int, int, int, int, int, int, int, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:18` — `DummyInternalMethodHandler.isResearchComplete(String, String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:20` — `DummyInternalMethodHandler.hasDiscoveredAspect(String, Aspect)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:22` — `DummyInternalMethodHandler.getDiscoveredAspects(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:24` — `DummyInternalMethodHandler.getStackInRowAndColumn(Object, int, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:26` — `DummyInternalMethodHandler.getObjectAspects(ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:28` — `DummyInternalMethodHandler.getBonusObjectTags(ItemStack, AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:30` — `DummyInternalMethodHandler.generateTags(Item, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:32` — `DummyInternalMethodHandler.consumeVisFromWand(ItemStack, Player, AspectList, boolean, boolean)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:34` — `DummyInternalMethodHandler.consumeVisFromWandCrafting(ItemStack, Player, AspectList, boolean)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:36` — `DummyInternalMethodHandler.consumeVisFromInventory(Player, AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:38` — `DummyInternalMethodHandler.addWarpToPlayer(Player, int, boolean)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:40` — `DummyInternalMethodHandler.addStickyWarpToPlayer(Player, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:22` — `IInternalMethodHandler.generateVisEffect(ServerLevel, int, int, int, int, int, int, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:24` — `IInternalMethodHandler.isResearchComplete(Player, String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:26` — `IInternalMethodHandler.hasDiscoveredAspect(Player, Aspect)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:28` — `IInternalMethodHandler.getDiscoveredAspects(Player)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:30` — `IInternalMethodHandler.getStackInRowAndColumn(Object, int, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:32` — `IInternalMethodHandler.getObjectAspects(Level, ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:34` — `IInternalMethodHandler.getBonusObjectTags(Level, ItemStack, AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:36` — `IInternalMethodHandler.generateTags(Level, Item, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:38` — `IInternalMethodHandler.consumeVisFromWand(ItemStack, Player, AspectList, boolean, boolean)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:40` — `IInternalMethodHandler.consumeVisFromWandCrafting(ItemStack, Player, AspectList, boolean)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:42` — `IInternalMethodHandler.consumeVisFromInventory(Player, AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:44` — `IInternalMethodHandler.addWarpToPlayer(Player, int, boolean)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:46` — `IInternalMethodHandler.addStickyWarpToPlayer(Player, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:48` — `IInternalMethodHandler.generateVisEffect(int, int, int, int, int, int, int, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:50` — `IInternalMethodHandler.isResearchComplete(String, String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:52` — `IInternalMethodHandler.getObjectAspects(ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:54` — `IInternalMethodHandler.getBonusObjectTags(ItemStack, AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:56` — `IInternalMethodHandler.generateTags(Item, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:58` — `IInternalMethodHandler.hasDiscoveredAspect(String, Aspect)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:60` — `IInternalMethodHandler.getDiscoveredAspects(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:33` — `ThaumcraftInternalMethods.<init>()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:35` — `ThaumcraftInternalMethods.generateVisEffect(int, int, int, int, int, int, int, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:39` — `ThaumcraftInternalMethods.isResearchComplete(String, String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:44` — `ThaumcraftInternalMethods.hasDiscoveredAspect(String, Aspect)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:49` — `ThaumcraftInternalMethods.getDiscoveredAspects(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:85` — `ThaumcraftInternalMethods.getObjectAspects(ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:91` — `ThaumcraftInternalMethods.getBonusObjectTags(ItemStack, AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:97` — `ThaumcraftInternalMethods.generateTags(Item, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:105` — `ThaumcraftInternalMethods.consumeVisFromWand(ItemStack, Player, AspectList, boolean, boolean)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:109` — `ThaumcraftInternalMethods.consumeVisFromWandCrafting(ItemStack, Player, AspectList, boolean)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:113` — `ThaumcraftInternalMethods.consumeVisFromInventory(Player, AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:117` — `ThaumcraftInternalMethods.addWarpToPlayer(Player, int, boolean)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:123` — `ThaumcraftInternalMethods.addStickyWarpToPlayer(Player, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/internal/WeightedRandomLoot.java:14` — `WeightedRandomLoot.<init>(ItemStack, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/items/IVisDiscountGear.java:15` — `IVisDiscountGear.getVisDiscount(ItemStack, Player)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/items/IVisDiscountGear.java:17` — `IVisDiscountGear.getVisDiscount(ItemStack, Player, Aspect)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/INode.java:49` — `INode.getNodeType()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/INode.java:51` — `INode.setNodeType(NodeType)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/INode.java:53` — `INode.setNodeModifier(NodeModifier)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/INode.java:55` — `INode.getNodeModifier()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/INode.java:57` — `INode.getNodeVisBase(Aspect)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/INode.java:59` — `INode.setNodeVisBase(Aspect, short)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/IRevealer.java:7` — `IRevealer.showNodes(ItemStack, LivingEntity)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:24` — `NodeAdapter.<init>(TileNode)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:46` — `NodeAdapter.setAspects(AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:50` — `NodeAdapter.getNodeType()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:52` — `NodeAdapter.setNodeType(NodeType)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:56` — `NodeAdapter.setNodeModifier(NodeModifier)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:60` — `NodeAdapter.getNodeModifier()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:64` — `NodeAdapter.getNodeVisBase(Aspect)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:73` — `NodeAdapter.doesContainerAccept(Aspect)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:75` — `NodeAdapter.addToContainer(Aspect, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:77` — `NodeAdapter.takeFromContainer(Aspect, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:79` — `NodeAdapter.takeFromContainer(AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:81` — `NodeAdapter.doesContainerContainAmount(Aspect, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:85` — `NodeAdapter.doesContainerContain(AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:87` — `NodeAdapter.containerContains(Aspect)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:25` — `NodeBase.setAspects(AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:39` — `NodeBase.getNodeType()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:41` — `NodeBase.setNodeType(NodeType)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:43` — `NodeBase.setNodeModifier(NodeModifier)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:45` — `NodeBase.getNodeModifier()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:47` — `NodeBase.getNodeVisBase(Aspect)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:59` — `NodeBase.doesContainerAccept(Aspect)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:61` — `NodeBase.addToContainer(Aspect, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:68` — `NodeBase.takeFromContainer(Aspect, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:73` — `NodeBase.takeFromContainer(AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:75` — `NodeBase.doesContainerContainAmount(Aspect, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:79` — `NodeBase.doesContainerContain(AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:81` — `NodeBase.containerContains(Aspect)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/IScanEventHandler.java:10` — `IScanEventHandler.scanPhenomena(ItemStack, Level, Player)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:14` — `ResearchAddendum.getText()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:16` — `ResearchAddendum.getTextLocalized()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:20` — `ResearchAddendum.setText(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:22` — `ResearchAddendum.getRecipes()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:24` — `ResearchAddendum.setRecipes(ResourceLocation[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:26` — `ResearchAddendum.getResearch()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:28` — `ResearchAddendum.setResearch(String[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchCategories.java:51` — `ResearchCategories.getResearchList(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchCategories.java:61` — `ResearchCategories.getResearch(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchCategories.java:78` — `ResearchCategories.registerCategory(String, ResourceLocation, ResourceLocation)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchCategory.java:19` — `ResearchCategory.<init>(String, String, AspectList, ResourceLocation, ResourceLocation)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchCategory.java:24` — `ResearchCategory.<init>(String, String, AspectList, ResourceLocation, ResourceLocation, ResourceLocation)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchCategory.java:39` — `ResearchCategory.applyFormula(AspectList, double)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchCategoryList.java:22` — `ResearchCategoryList.<init>(ResourceLocation, ResourceLocation)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:52` — `ResearchEntry.getKey()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:54` — `ResearchEntry.setKey(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:56` — `ResearchEntry.getCategory()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:58` — `ResearchEntry.setCategory(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:60` — `ResearchEntry.getName()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:62` — `ResearchEntry.getLocalizedName()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:66` — `ResearchEntry.setName(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:68` — `ResearchEntry.getParents()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:84` — `ResearchEntry.getSiblings()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:88` — `ResearchEntry.getDisplayColumn()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:90` — `ResearchEntry.setDisplayColumn(int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:92` — `ResearchEntry.getDisplayRow()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:94` — `ResearchEntry.setDisplayRow(int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:96` — `ResearchEntry.getIcons()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:98` — `ResearchEntry.setIcons(Object[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:100` — `ResearchEntry.getMeta()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:102` — `ResearchEntry.hasMeta(EnumResearchMeta)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:106` — `ResearchEntry.setMeta(EnumResearchMeta[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:108` — `ResearchEntry.getStages()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:110` — `ResearchEntry.setStages(ResearchStage[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:112` — `ResearchEntry.getRewardItem()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:114` — `ResearchEntry.setRewardItem(ItemStack[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:116` — `ResearchEntry.getRewardKnow()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:118` — `ResearchEntry.setRewardKnow(ResearchStage.Knowledge[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:120` — `ResearchEntry.getAddenda()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:122` — `ResearchEntry.setAddenda(ResearchAddendum[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:43` — `ResearchItem.<init>(String, String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:54` — `ResearchItem.<init>(String, String, AspectList, int, int, int, ResourceLocation)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:67` — `ResearchItem.<init>(String, String, AspectList, int, int, int, ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:126` — `ResearchItem.setSpecial()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:128` — `ResearchItem.setStub()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:130` — `ResearchItem.setLost()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:132` — `ResearchItem.setConcealed()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:134` — `ResearchItem.setHidden()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:136` — `ResearchItem.setVirtual()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:138` — `ResearchItem.setParents(String[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:140` — `ResearchItem.setParentsHidden(String[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:142` — `ResearchItem.setSiblings(String[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:144` — `ResearchItem.setPages(ResearchPage[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:146` — `ResearchItem.getPages()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:148` — `ResearchItem.setItemTriggers(ItemStack[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:150` — `ResearchItem.setEntityTriggers(String[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:152` — `ResearchItem.setAspectTriggers(Aspect[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:173` — `ResearchItem.getEntityTriggers()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:175` — `ResearchItem.getAspectTriggers()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:177` — `ResearchItem.registerResearchItem()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:179` — `ResearchItem.getName()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:181` — `ResearchItem.getText()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:183` — `ResearchItem.isSpecial()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:185` — `ResearchItem.isStub()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:187` — `ResearchItem.isLost()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:189` — `ResearchItem.isConcealed()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:191` — `ResearchItem.isHidden()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:193` — `ResearchItem.isVirtual()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:195` — `ResearchItem.isAutoUnlock()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:197` — `ResearchItem.setAutoUnlock()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:199` — `ResearchItem.isRound()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:201` — `ResearchItem.setRound()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:203` — `ResearchItem.isSecondary()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:205` — `ResearchItem.setSecondary()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:207` — `ResearchItem.getComplexity()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:209` — `ResearchItem.setComplexity(int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchPage.java:33` — `ResearchPage.<init>(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchPage.java:38` — `ResearchPage.<init>(String, String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchPage.java:44` — `ResearchPage.<init>(Recipe<?>)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchPage.java:50` — `ResearchPage.<init>(Recipe<?>[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchPage.java:55` — `ResearchPage.<init>(IArcaneRecipe[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchPage.java:60` — `ResearchPage.<init>(CrucibleRecipe[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchPage.java:65` — `ResearchPage.<init>(InfusionRecipe[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchPage.java:70` — `ResearchPage.<init>(List<Recipe<?>>)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchPage.java:75` — `ResearchPage.<init>(IArcaneRecipe)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchPage.java:81` — `ResearchPage.<init>(CrucibleRecipe)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchPage.java:105` — `ResearchPage.<init>(InfusionRecipe)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchPage.java:111` — `ResearchPage.<init>(InfusionEnchantmentRecipe)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchPage.java:116` — `ResearchPage.<init>(ResourceLocation, String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchPage.java:122` — `ResearchPage.<init>(AspectList)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:27` — `ResearchStage.Knowledge.<init>(EnumKnowledgeType, ResearchCategory, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:34` — `ResearchStage.getText()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:36` — `ResearchStage.getTextLocalized()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:40` — `ResearchStage.setText(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:42` — `ResearchStage.getRecipes()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:44` — `ResearchStage.setRecipes(ResourceLocation[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:46` — `ResearchStage.getObtain()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:48` — `ResearchStage.setObtain(Object[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:50` — `ResearchStage.getCraft()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:52` — `ResearchStage.setCraft(Object[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:54` — `ResearchStage.getCraftReference()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:56` — `ResearchStage.setCraftReference(int[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:58` — `ResearchStage.getKnow()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:60` — `ResearchStage.setKnow(Knowledge[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:62` — `ResearchStage.getResearch()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:64` — `ResearchStage.setResearch(String[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:66` — `ResearchStage.getResearchIcon()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:68` — `ResearchStage.setResearchIcon(String[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:70` — `ResearchStage.getWarp()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:72` — `ResearchStage.setWarp(int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ScanBlock.java:18` — `ScanBlock.<init>(Block)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ScanBlock.java:22` — `ScanBlock.<init>(String, Block[])`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ScanBlock.java:27` — `ScanBlock.checkThing(Player, Object)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ScanBlock.java:38` — `ScanBlock.getResearchKey(Player, Object)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ScanResult.java:24` — `ScanResult.<init>(byte, int, int, Entity, String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ScanResult.java:28` — `ScanResult.item(ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ScanResult.java:33` — `ScanResult.entity(Entity)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ScanResult.java:37` — `ScanResult.node(TileNode)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/ScanningManager.java:18` — `ScanningManager.addScannableThing(IScanThing)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:21` — `ResearchTableData.<init>(BlockEntity)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:25` — `ResearchTableData.<init>(Player, BlockEntity)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:30` — `ResearchTableData.hasTotal(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:34` — `ResearchTableData.getTotal(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:39` — `ResearchTableData.addTotal(String, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:43` — `ResearchTableData.addInspiration(int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:17` — `TheorycraftCard.getSeed()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:25` — `TheorycraftCard.getInspirationCost()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:30` — `TheorycraftCard.getLocalizedName()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:32` — `TheorycraftCard.getLocalizedText()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:43` — `TheorycraftCard.setSeed(long)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:52` — `TheorycraftCard.deserialize(CompoundTag)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftManager.java:16` — `TheorycraftManager.registerAid(ITheorycraftAid)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftManager.java:21` — `TheorycraftManager.registerCard(Class<?>)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:33` — `TileVisNode.getRange()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:34` — `TileVisNode.isSource()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:35` — `TileVisNode.getAttunement()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:37` — `TileVisNode.getLocation()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:38` — `TileVisNode.getNodeCounter()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:39` — `TileVisNode.getAttachmentVersion()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:40` — `TileVisNode.isNodeTickActive()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:41` — `TileVisNode.isConsumeActive()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:56` — `TileVisNode.consumeVis(Aspect, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:57` — `TileVisNode.triggerConsumeEffect(Aspect)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:58` — `TileVisNode.parentChanged()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:60` — `TileVisNode.getParent()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:64` — `TileVisNode.getChildren()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:72` — `TileVisNode.getRootSource()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:76` — `TileVisNode.setParent(WeakReference<TileVisNode>)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:84` — `TileVisNode.removeThisNode()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:86` — `TileVisNode.tick(Level, BlockPos, BlockState, TileVisNode)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:62` — `FocusUpgradeType.getAspects()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:80` — `FocusUpgradeType.get(short)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:81` — `FocusUpgradeType.get(int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:16` — `IWandFocus.getFocusColor()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:24` — `IWandFocus.getAnimation()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:26` — `IWandFocus.getVisCost()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:28` — `IWandFocus.isVisCostPerTick()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:30` — `IWandFocus.onFocusRightClick(net.minecraft.world.item.ItemStack, Level, Player, net.minecraft.world.phys.BlockHitResult)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:33` — `IWandFocus.onUsingFocusTick(net.minecraft.world.item.ItemStack, Player, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:35` — `IWandFocus.onPlayerStoppedUsingFocus(net.minecraft.world.item.ItemStack, Level, Player, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:37` — `IWandFocus.getSortingHelper(net.minecraft.world.item.ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:39` — `IWandFocus.onFocusBlockStartBreak(net.minecraft.world.item.ItemStack, int, int, int, Player)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/IWandRodOnUpdate.java:12` — `IWandRodOnUpdate.onUpdate(ItemStack, Player)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/IWandTriggerManager.java:13` — `IWandTriggerManager.performTrigger(Level, ItemStack, Player, int, int, int, int, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/IWandable.java:23` — `IWandable.onUsingWandTick(ItemStack, Player, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/IWandable.java:25` — `IWandable.onWandStoppedUsing(ItemStack, Level, Player, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:22` — `ItemFocusBasic.<init>(Properties)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:26` — `ItemFocusBasic.isVisCostPerTick(ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:27` — `ItemFocusBasic.getFocusColor(ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:28` — `ItemFocusBasic.getSortingHelper(ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:34` — `ItemFocusBasic.getActivationCooldown(ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:35` — `ItemFocusBasic.getAnimation(ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:73` — `ItemFocusBasic.onFocusRightClick(ItemStack, Level, Player, InteractionHand)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:74` — `ItemFocusBasic.onUsingFocusTick(ItemStack, Player, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:75` — `ItemFocusBasic.onPlayerStoppedUsingFocus(ItemStack, Level, Player, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:77` — `ItemFocusBasic.getAppliedUpgrades(ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:78` — `ItemFocusBasic.getPossibleUpgradesByRank(ItemStack, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:79` — `ItemFocusBasic.getUpgradeLevel(ItemStack, FocusUpgradeType)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:80` — `ItemFocusBasic.isUpgradedWith(ItemStack, FocusUpgradeType)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:82` — `ItemFocusBasic.canApplyUpgrade(ItemStack, Player, FocusUpgradeType, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/StaffRod.java:15` — `StaffRod.<init>(String, int, ItemStack, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/StaffRod.java:20` — `StaffRod.<init>(String, int, ItemStack, int, IWandRodOnUpdate, ResourceLocation)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/StaffRod.java:24` — `StaffRod.<init>(String, int, ItemStack, int, IWandRodOnUpdate)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/StaffRod.java:29` — `StaffRod.<init>(String, int, ItemStack, int, ResourceLocation)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/StaffRod.java:33` — `StaffRod.hasRunes()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/StaffRod.java:35` — `StaffRod.setRunes(boolean)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandCap.java:27` — `WandCap.<init>(String, float, ItemStack, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandCap.java:38` — `WandCap.<init>(String, float, List<Aspect>, float, ItemStack, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandCap.java:64` — `WandCap.getBaseCostModifier()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandCap.java:66` — `WandCap.getSpecialCostModifierAspects()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandCap.java:68` — `WandCap.getSpecialCostModifier()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandCap.java:70` — `WandCap.getTexture()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandCap.java:72` — `WandCap.setTexture(ResourceLocation)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandCap.java:74` — `WandCap.getTag()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandCap.java:76` — `WandCap.setTag(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandCap.java:78` — `WandCap.getItem()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandCap.java:80` — `WandCap.setItem(ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandCap.java:82` — `WandCap.getCraftCost()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandCap.java:84` — `WandCap.setCraftCost(int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:28` — `WandRod.<init>(String, int, ItemStack, int, ResourceLocation)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:38` — `WandRod.<init>(String, int, ItemStack, int, IWandRodOnUpdate, ResourceLocation)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:49` — `WandRod.<init>(String, int, ItemStack, int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:54` — `WandRod.<init>(String, int, ItemStack, int, IWandRodOnUpdate)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:73` — `WandRod.getTag()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:75` — `WandRod.setTag(String)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:77` — `WandRod.getCapacity()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:79` — `WandRod.setCapacity(int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:81` — `WandRod.getTexture()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:83` — `WandRod.setTexture(ResourceLocation)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:85` — `WandRod.getItem()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:87` — `WandRod.setItem(ItemStack)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:89` — `WandRod.getCraftCost()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:91` — `WandRod.setCraftCost(int)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:93` — `WandRod.getOnUpdate()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:95` — `WandRod.setOnUpdate(IWandRodOnUpdate)`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:97` — `WandRod.isGlowing()`.
- **should-fix missing contract** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:99` — `WandRod.setGlowing(boolean)`.

Inherited/local-absence exclusions (not findings): `common/src/main/java/thaumcraft/api/BlockCoordinates.java:53` `BlockCoordinates.equals`; `common/src/main/java/thaumcraft/api/BlockCoordinates.java:104` `BlockCoordinates.compareTo`; `common/src/main/java/thaumcraft/api/TileThaumcraft.java:25` `TileThaumcraft.loadAdditional`; `common/src/main/java/thaumcraft/api/TileThaumcraft.java:41` `TileThaumcraft.getUpdatePacket`; `common/src/main/java/thaumcraft/api/WorldCoordinates.java:46` `WorldCoordinates.equals`; `common/src/main/java/thaumcraft/api/WorldCoordinates.java:49` `WorldCoordinates.hashCode`; `common/src/main/java/thaumcraft/api/WorldCoordinates.java:50` `WorldCoordinates.compareTo`; `common/src/main/java/thaumcraft/api/WorldCoordinates.java:70` `WorldCoordinates.toString`; `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:42` `NodeAdapter.getId`; `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:44` `NodeAdapter.getAspects`; `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:48` `NodeAdapter.getAspectsBase`; `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:21` `NodeBase.getId`; `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:23` `NodeBase.getAspects`; `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:37` `NodeBase.getAspectsBase`; `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:98` `TileVisNode.setRemoved`; `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:107` `TileVisNode.clearRemoved`; `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:108` `TileVisNode.setLevel`; `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:180` `TileVisNode.getUpdateTag`; `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:187` `TileVisNode.getUpdatePacket`; `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:82` `FocusUpgradeType.equals`; `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:83` `FocusUpgradeType.hashCode`; `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:99` `ItemFocusBasic.appendHoverText`.

## 3. Null and error policy

**Inspected: 761 methods/explicit constructors**, comparing overloads, builders, handlers and equivalent lookups. **20 inconsistency groups.** Null rejection is not itself a defect; the problem is undisclosed differences between nearby entry points. No normalization was applied because existing addons may depend on the differing answers.

- **should-fix N01** — `common/src/main/java/thaumcraft/api/aspects/Aspect.java:75`: Constructor rejects null tag/image, but setTag(:121) and setChatcolor(:130) accept null. Later getName/render paths dereference them; mutable identity has no consistent validation policy.
- **should-fix N02** — `common/src/main/java/thaumcraft/api/aspects/Aspect.java:86`: Default-image constructor calls tag.toLowerCase before primary-constructor validation; empty tags are accepted yet getName(:102) cannot handle an empty string.
- **should-fix N03** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:102`: add/merge with a null Aspect do nothing; add/merge with a null AspectList (:107/:117) throw. Same verb, undisclosed null policy.
- **should-fix N04** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:122`: reduce/remove reject negative amounts (:122/:128), while add/merge (:102/:112) retain them. Document signed amounts or agree validation before changing behavior.
- **should-fix N05** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:169`: readFromNBT clears state before dereferencing null; BlockCoordinates.readNBT(:109) ignores null and WorldCoordinates.readNBT(:61) returns empty. Loading error semantics differ materially.
- **should-fix N06** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:414`: registerScanEventhandler rejects null with NPE, while AspectRegistryEvent.addListener(:22), ScanningManager.addScannableThing(:18) and TheorycraftManager.registerAid(:16) silently ignore it.
- **should-fix N07** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:420`: Simple registerObjectTag(ItemStack,...) silently ignores null/empty; complex overloads (:454 onward) throw IllegalArgumentException. Aspect null becomes empty in TagKey registration (:436), but complex registration requires non-null aspects.
- **should-fix N08** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:254`: Infusion registration returns null for unsupported output classes; constructor InfusionRecipe(:34) accepts other objects via an input sample, but Object[] with unknown patch throws. Null enchantment registration returns null while direct enchantment constructor throws.
- **should-fix N09** — `common/src/main/java/thaumcraft/api/ItemApi.java:30`: Null/empty names return null; ItemsTC.get(:29) and BlocksTC.get(:29) instead throw via id. Document the distinction between forgiving lookup and required-name lookup.
- **should-fix N10** — `common/src/main/java/thaumcraft/api/items/ItemsTC.java:23`: Namespaced malformed ids use tryParse and return null, while unnamespaced malformed paths use throwing fromNamespaceAndPath. BlocksTC.id(:23) has the same inconsistency.
- **should-fix N11** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:95`: areItemsEqual treats null as equivalent to EMPTY; itemMatches(:175) considers two nulls equal but compares an actual EMPTY via item/damage; component crafting comparison (:162) always rejects any null. Document the three different relations.
- **should-fix N12** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:30`: Default getStackInRowAndColumn returns EMPTY, DummyInternalMethodHandler(:24) returns null, and the installed real handler returns EMPTY for invalid input. Implementers cannot infer the absence sentinel.
- **should-fix N13** — `common/src/main/java/thaumcraft/api/crafting/CrucibleRecipe.java:27`: Constructor dereferences result/tags; infusion/arcane cost conversion accepts null aspects as zero costs. Crucible.matches(null,cat) returns false but removeMatching(null) throws.
- **should-fix N14** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:100`: addKnowledge(null,...) rejects the type, but rawAmount(:132) interprets any non-OBSERVATION type, including null, as THEORY. Read and write disagree on invalid enum input.
- **should-fix N15** — `common/src/main/java/thaumcraft/api/research/ScanResult.java:28`: item(null) throws IAE; entity(null)/node(null) throw NPE (:33/:37); public constructor permits impossible combinations. Factory validation and DTO permissiveness need an explicit boundary.
- **should-fix N16** — `common/src/main/java/thaumcraft/api/WorldCoordinates.java:23`: Null dimension produces IAE, null position/other/tile produces NPE, distance-to-null returns infinity (:43), compare-to-null throws (:50), read-null returns Optional.empty. Make error/null contracts explicit.
- **should-fix N17** — `common/src/main/java/thaumcraft/api/wands/WandCap.java:76`: setTag accepts null, then byTag dereferences cap.tag. WandRod(:75/:105) repeats this policy; neither setter re-keys its registry.
- **should-fix N18** — `common/src/main/java/thaumcraft/api/aspects/AspectRegistryEvent.java:35`: Event listener RuntimeExceptions are logged and iteration continues; ScanningManager.find(:26) lets handler exceptions escape, and AspectList(ItemStack)(:24) silently swallows Exception. Similar addon callback boundaries use three undisclosed failure policies.
- **should-fix N19** — `common/src/main/java/thaumcraft/api/visnet/VisNetHandler.java:140`: Predicates may throw off-thread (isNativeServerLevel); isChunkLoaded(:193) and topology/lifetime reads (:128/:129) do not enforce the same thread check. Callers need per-method threading and failure contracts, not boolean-looking assumptions.
- **should-fix N20** — `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:59`: doesContainerAccept(null) reports true while addToContainer(null,amount) returns the entire remainder and takeFromContainer(null,amount) refuses. Define whether acceptance is capability-wide or argument validation.

## 4. Immutability and exposure

**Inspected: 93 files / 998 public members**, tracing returned/retained arrays, collections, AspectLists, ItemStacks and native-model references. **28 exposure groups**, including three shared-answer corruption blockers. Original deliberate public mutability is still a published-contract hazard, not permission to silently break compatibility.

- **blocker E01** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:200`: getAllAspects returns the cached mutable AspectList itself. An addon can clear or corrupt all subsequent answers for that amount. Return copies or make caching internal.
- **blocker E02** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:209`: getAllCompoundAspects repeats the same cached-list escape. Tests check contents, not mutation isolation.
- **blocker E03** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:331`: First successful getCraftingRecipeKey lookup stores found in keyCache and returns that same Object[] via researchVisible. Only cache-hit reads clone. Editing the first answer poisons future answers or causes casts to fail.
- **should-fix E04** — `common/src/main/java/thaumcraft/api/aspects/Aspect.java:13`: Public global aspects map can be cleared/replaced entry-by-entry, including putting null values; every aspect traversal trusts it. Legacy mutability needs controlled registration/read-only access in a future API.
- **should-fix E05** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:16`: Public final map still permits null keys/amounts and arbitrary negative values; add null guards do not protect callers that directly mutate the map. Keep compatibility but do not describe this type as misuse-proof.
- **should-fix E06** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:102`: portableHoleBlackList is an exposed mutable mirror of an authoritative tag. Caller edits may be discarded by refreshPortableHoleBlackList, so this looks writable without defining authoritative write behavior.
- **should-fix E07** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:105`: scanEventhandlers, scanEntities, objectTags and groupedObjectTags (:105-108) expose live mutable lists/maps; values include mutable AspectList and int[]. ConcurrentHashMap does not make those values safe.
- **should-fix E08** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:489`: EntityTags stores/exports caller AspectList and EntityTagsNBT[] references, and EntityTagsNBT values (:497) are arbitrary mutable Object. Copy-on-scan is not copy-on-registration.
- **should-fix E09** — `common/src/main/java/thaumcraft/api/internal/WeightedRandomLoot.java:8`: Loot item stacks and three global lists (:10-12) are mutable aliases. addLootBagItem intentionally retains its stack. Changing a previously registered stack changes shared selection data.
- **should-fix E10** — `common/src/main/java/thaumcraft/api/crafting/CrucibleRecipe.java:27`: Constructor stores output, aspects and catalyst references; getRecipeOutput(:82) returns internal output. Editing catalyst/aspects can also diverge from the already constructed Ingredient/hash.
- **should-fix E11** — `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:103`: getRecipeInput/getComponents/getRecipeOutput/getAspects (:103-109) expose stacks, component array and AspectList. Constructor clones the array but not component stacks; nativeRecipe was snapshotted separately.
- **should-fix E12** — `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:26`: Public aspects/components/enchantment/instability/recipeXP are live mutable fields; components are only shallow-cloned. Native matching uses another object, so visible configuration can stop matching execution.
- **should-fix E13** — `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:28`: Public output/input/aspects/width/height and getInput/getRecipeOutput/getAspects (:132/:163/:165) expose mutable internals. Native result/key are separate snapshots, so edits need not affect actual crafting.
- **should-fix E14** — `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:70`: getRecipeOutput/getInput/getAspects (:70/:82/:103) expose mutable data while native matching uses immutable ingredient-list structure and a separate result copy.
- **should-fix E15** — `common/src/main/java/thaumcraft/api/wands/WandCap.java:66`: getSpecialCostModifierAspects and getItem(:78) return internal mutable list/stack; constructor/setItem retain incoming references. caps(:17) is replaceable and globally mutable.
- **should-fix E16** — `common/src/main/java/thaumcraft/api/wands/WandRod.java:85`: getItem returns internal stack, constructors and setItem retain it, and rods(:18) is a replaceable global mutable registry. StaffRod inherits the same exposure.
- **should-fix E17** — `common/src/main/java/thaumcraft/api/research/ResearchCategories.java:22`: researchCategories and ResearchCategoryList.research(:20) expose live registries; callers can bypass overlap/bounds checks. allResearch makes only a shallow list copy.
- **should-fix E18** — `common/src/main/java/thaumcraft/api/research/ResearchCategory.java:17`: Public mutable formula is retained directly by constructors and controls knowledge arithmetic. Addon edits can alter already published categories globally.
- **should-fix E19** — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:19`: tags, icon_item(:25), parents/hidden/siblings (:20-22), pages and trigger setters/getters (:138-175) alias mutable data. Both caller-owned inputs and returned arrays can change registered research.
- **should-fix E20** — `common/src/main/java/thaumcraft/api/research/ResearchPage.java:23`: Public recipe, recipeOutput, aspects and constructors retain mutable stacks/lists/arrays, sometimes shared with the recipe itself. No ownership contract distinguishes views from builders.
- **should-fix E21** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:43`: of aliases ResearchItem parents/siblings/icon stack. Array getters/setters (:68-122) also expose icons, meta, stages, rewards and addenda; this is not an isolated legacy view.
- **should-fix E22** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:42`: All array setters/getters (:42-68) retain/return internal arrays. knowledge(:75) only shallow-copies mutable Knowledge entries; callers can still change rewards through the supposedly copied answer.
- **should-fix E23** — `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:22`: getRecipes/getResearch return internal arrays and setters retain them. A text/recipe addendum cannot currently offer snapshot semantics.
- **should-fix E24** — `common/src/main/java/thaumcraft/api/research/ScanBlock.java:24`: Constructor retains incoming Block[] although blocks() returns a clone. Mutation through the original array changes registered scan matching.
- **should-fix E25** — `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:19`: Public categoryTotals map permits null/negative amounts and bypasses addTotal. Public table/player/session counters have no validation or ownership boundary.
- **should-fix E26** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftManager.java:11`: Public aids/cards maps are writable despite register methods promising first-wins registration; callers can replace entries and insert nulls.
- **should-fix E27** — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:32`: knowledge() deliberately exports the mutable native model. This should be identified as trusted low-level access, not a stable addon abstraction. getResearchList itself is safe: native implementation returns a snapshot.
- **should-fix E28** — `common/src/main/java/thaumcraft/api/research/ScanResult.java:19`: Public stack can be mutated after item() copies the input; id/meta and stack can disagree. Keep DTO compatibility but document which witness wins and do not use equals for identity/security.

Positive boundaries checked: Aspect.getComponents/setComponents clone arrays; INode/NodeAdapter/NodeBase aspect views copy; NativePlayerKnowledge.getResearchList returns the native snapshot; ScanningManager.scannableThings and WandTriggerRegistry.modIds copy; TileVisNode/VisNetHandler source/edge snapshots allocate fresh WeakReferences so clearing a returned reference does not remove a graph edge. ThaumcraftApi.getSmeltingBonus copies stacks. These are not findings. Collection structure snapshots do not imply deep copies of contained objects.

## 5. Deprecation

**Inspected: all 1,101 public declarations for compatibility/supersession**, including all six pre-existing @Deprecated methods. **23 member/type findings; six fixed.** Unsupported is not the same as deprecated: choosing whether to keep TC6 peers needs a maintainer, not an indiscriminate annotation sweep.

- **should-fix P01** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:86`: enchantFrugal remains mutable and always starts at -1. Add @Deprecated and per-field @deprecated guidance to ENCHANTMENT_FRUGAL; numeric enchantment ids are not identities in 1.21.1.
- **should-fix P02** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:87`: enchantPotency remains mutable and always starts at -1. Add @Deprecated and per-field @deprecated guidance to ENCHANTMENT_POTENCY; numeric enchantment ids are not identities in 1.21.1.
- **should-fix P03** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:88`: enchantWandFortune remains mutable and always starts at -1. Add @Deprecated and per-field @deprecated guidance to ENCHANTMENT_WAND_FORTUNE; numeric enchantment ids are not identities in 1.21.1.
- **should-fix P04** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:89`: enchantHaste remains mutable and always starts at -1. Add @Deprecated and per-field @deprecated guidance to ENCHANTMENT_HASTE; numeric enchantment ids are not identities in 1.21.1.
- **should-fix P05** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:90`: enchantRepair remains mutable and always starts at -1. Add @Deprecated and per-field @deprecated guidance to ENCHANTMENT_REPAIR; numeric enchantment ids are not identities in 1.21.1.
- **should-fix P06** — `common/src/main/java/thaumcraft/api/wands/WandTriggerRegistry.java:37`: FIXED: registerWandBlockTrigger(manager,event,Block,int,String) discards metadata. Now @Deprecated with replacement Javadoc; binary signatures and implementation are unchanged.
- **should-fix P07** — `common/src/main/java/thaumcraft/api/wands/WandTriggerRegistry.java:47`: FIXED: registerWandBlockTrigger(manager,event,Block,int) discards metadata. Now @Deprecated with replacement Javadoc; binary signatures and implementation are unchanged.
- **should-fix P08** — `common/src/main/java/thaumcraft/api/wands/WandTriggerRegistry.java:68`: FIXED: hasTrigger(Block,int) discards metadata. Now @Deprecated with replacement Javadoc; binary signatures and implementation are unchanged.
- **should-fix P09** — `common/src/main/java/thaumcraft/api/wands/WandTriggerRegistry.java:79`: FIXED: hasTrigger(Block,int,String) discards metadata. Now @Deprecated with replacement Javadoc; binary signatures and implementation are unchanged.
- **should-fix P10** — `common/src/main/java/thaumcraft/api/wands/WandTriggerRegistry.java:128`: FIXED: performTrigger(...,Block,int) discards metadata. Now @Deprecated with replacement Javadoc; binary signatures and implementation are unchanged.
- **should-fix P11** — `common/src/main/java/thaumcraft/api/wands/WandTriggerRegistry.java:133`: FIXED: performTrigger(...,Block,int,String) discards metadata. Now @Deprecated with replacement Javadoc; binary signatures and implementation are unchanged.
- **should-fix P12** — `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:10`: takeFromContainer(AspectList) already has @Deprecated but no @deprecated Javadoc explaining takeFromContainer(Aspect,int), noting that a caller loop is not atomic.
- **should-fix P13** — `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:12`: doesContainerContain(AspectList) already has @Deprecated but no @deprecated Javadoc explaining doesContainerContainAmount(Aspect,int), specifying all-versus-any behavior.
- **should-fix P14** — `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:79`: takeFromContainer(AspectList) already has @Deprecated but no @deprecated Javadoc explaining the single-aspect overload.
- **should-fix P15** — `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:85`: doesContainerContain(AspectList) already has @Deprecated but no @deprecated Javadoc explaining doesContainerContainAmount.
- **should-fix P16** — `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:73`: takeFromContainer(AspectList) already has @Deprecated but no @deprecated Javadoc explaining the single-aspect overload; this body always returns false.
- **should-fix P17** — `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:79`: doesContainerContain(AspectList) already has @Deprecated but no @deprecated Javadoc explaining doesContainerContainAmount; this body always returns false.
- **nit P18** — `common/src/main/java/thaumcraft/api/items/IGoggles.java:10`: TC6 compatibility alias identifies a canonical replacement (thaumcraft.api.IGoggles) but has no @Deprecated marker. Decide whether TC6 aliases are supported peers or transitional; do not remove based on no in-tree consumer.
- **nit P19** — `common/src/main/java/thaumcraft/api/items/IRevealer.java:10`: TC6 compatibility alias identifies a canonical replacement (thaumcraft.api.nodes.IRevealer) but has no @Deprecated marker. Decide whether TC6 aliases are supported peers or transitional; do not remove based on no in-tree consumer.
- **nit P20** — `common/src/main/java/thaumcraft/api/items/IVisDiscountGear.java:14`: TC6 compatibility alias identifies a canonical replacement (thaumcraft.api.IVisDiscountGear (aspect-aware overload)) but has no @Deprecated marker. Decide whether TC6 aliases are supported peers or transitional; do not remove based on no in-tree consumer.
- **should-fix P21** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:48`: Integer-dimension generateVisEffect is retained but does nothing. Mark deprecated and direct callers to VisNetHandler.generateVisEffect(ServerLevel,BlockPos,BlockPos,Aspect); do not recommend another inert default hook.
- **should-fix P22** — `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:35`: Integer-dimension generateVisEffect is retained but does nothing. Mark deprecated and direct callers to VisNetHandler.generateVisEffect(ServerLevel,BlockPos,BlockPos,Aspect); do not recommend another inert default hook.
- **should-fix P23** — `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:16`: Integer-dimension generateVisEffect is retained but does nothing. Mark deprecated and direct callers to VisNetHandler.generateVisEffect(ServerLevel,BlockPos,BlockPos,Aspect); do not recommend another inert default hook.

## 6. Naming and consistency

**Inspected: 688 method declarations, all 103 type names and the 237 fields/constants.** Found **10 naming/semantic-convention groups**. Also inspected **108 exact-spelling duplicate-signature groups (293 declarations)**; every group is listed below. Repeated interface/implementation signatures and intentionally shared recipe getters are usually correct, not API pollution.

- **nit C01** — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:88`: getAspectsSortedAmount and getAspectsSortedByAmount are exact aliases for the same operation. Keep the original spelling for compatibility; deprecate or clearly label the newer synonym.
- **nit C02** — `common/src/main/java/thaumcraft/api/aspects/AspectHelper.java:16`: Object-aspect lookup is exposed through AspectHelper, ThaumcraftApiHelper and IInternalMethodHandler; registerObjectTag/getBonusObjectTags/generateTags use tag for the same AspectList concept. Designate a canonical facade; do not rename TC4 entry points.
- **nit C03** — `common/src/main/java/thaumcraft/api/wands/WandCap.java:90`: Same registry lookup idea is named getAspect (Aspect), get (FocusUpgradeType), byTag (WandCap/WandRod), get/require (ItemsTC/BlocksTC), and getResearch. Document the conventions rather than add more aliases.
- **nit C04** — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:36`: getTextLocalized versus ResearchEntry.getLocalizedName, FocusUpgradeType.getLocalizedText and ResearchPage.getTranslatedText name localization inconsistently; some return null, some empty strings.
- **should-fix C05** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:30`: getLocalizedName/getLocalizedText subclass methods function as translation-key suppliers for name/text, unlike same-named FocusUpgradeType methods returning translated text. Same concept is not the same contract.
- **should-fix C06** — `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:45`: IWandFocus.WandFocusAnimation and ItemFocusBasic.WandFocusAnimation are distinct public enums with identical WAVE/CHARGE names. They cannot be assigned to each other; preserve binary compatibility and choose one canonical extension surface.
- **nit C07** — `common/src/main/java/thaumcraft/api/crafting/IInfusionStabiliser.java:8`: Type uses Stabiliser but method spells Stabalise. This is original published spelling, not an internal typo eligible for a safe rename.
- **nit C08** — `common/src/main/java/thaumcraft/api/WorldCoordinates.java:56`: compareWorldCoordinate duplicates Comparable.compareTo, and the plural WorldCoordinates contrasts BlockCoordinates for the same coordinate-holder idea. Keep compatibility alias, document delegation.
- **nit C09** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:414`: registerScanEventhandler/scanEventhandlers spell handler differently from Java-style EventHandler; historical public names must not be silently corrected.
- **should-fix C10** — `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:25`: Vis means centivis in focus costs and rod capacity, recipe vis in ArcaneRecipes.vis, and raw network units in VisNetHandler. AspectList is shared across them with no type-level unit distinction; add explicit per-entrypoint units.

### Same-signature cross-type ledger

Parameter type spelling is compared as parsed (unqualified versus fully qualified spellings may hide more equivalent signatures). Return type is not part of a Java method signature. **nit observation**, unless a semantic difference is identified above; interface/implementation duplication is retained, not proposed for deletion.
- **nit observation** — `addKnowledge(EnumKnowledgeType, ResearchCategory, int)`: `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:72` `IPlayerKnowledge`; `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:100` `NativePlayerKnowledge`.
- **nit observation** — `addResearch(String)`: `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:56` `IPlayerKnowledge`; `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:60` `NativePlayerKnowledge`.
- **nit observation** — `addStickyWarpToPlayer(Player, int)`: `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:234` `ThaumcraftApiHelper`; `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:40` `DummyInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:46` `IInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:123` `ThaumcraftInternalMethods`.
- **nit observation** — `addToContainer(Aspect, int)`: `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:8` `IAspectContainer`; `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:75` `NodeAdapter`; `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:61` `NodeBase`.
- **nit observation** — `addWarpToPlayer(Player, int, boolean)`: `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:230` `ThaumcraftApiHelper`; `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:38` `DummyInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:44` `IInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:117` `ThaumcraftInternalMethods`.
- **nit observation** — `armorMatThaumium()`: `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:61` `ThaumcraftApi`; `common/src/main/java/thaumcraft/api/ThaumcraftMaterials.java:52` `ThaumcraftMaterials`.
- **nit observation** — `armorMatThaumiumFortress()`: `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:62` `ThaumcraftApi`; `common/src/main/java/thaumcraft/api/ThaumcraftMaterials.java:54` `ThaumcraftMaterials`.
- **nit observation** — `armorMatVoid()`: `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:63` `ThaumcraftApi`; `common/src/main/java/thaumcraft/api/ThaumcraftMaterials.java:56` `ThaumcraftMaterials`.
- **nit observation** — `byTag(String)`: `common/src/main/java/thaumcraft/api/wands/WandCap.java:90` `WandCap`; `common/src/main/java/thaumcraft/api/wands/WandRod.java:105` `WandRod`.
- **nit observation** — `checkThing(Player, Object)`: `common/src/main/java/thaumcraft/api/research/IScanThing.java:12` `IScanThing`; `common/src/main/java/thaumcraft/api/research/ScanBlock.java:27` `ScanBlock`.
- **nit observation** — `clear()`: `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:44` `IPlayerKnowledge`; `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:34` `NativePlayerKnowledge`.
- **nit observation** — `clearResearchFlag(String, EnumResearchFlag)`: `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:67` `IPlayerKnowledge`; `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:88` `NativePlayerKnowledge`.
- **nit observation** — `consumeVisFromInventory(Player, AspectList)`: `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:226` `ThaumcraftApiHelper`; `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:36` `DummyInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:42` `IInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:113` `ThaumcraftInternalMethods`.
- **nit observation** — `consumeVisFromWand(ItemStack, Player, AspectList, boolean, boolean)`: `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:218` `ThaumcraftApiHelper`; `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:32` `DummyInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:38` `IInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:105` `ThaumcraftInternalMethods`.
- **nit observation** — `consumeVisFromWandCrafting(ItemStack, Player, AspectList, boolean)`: `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:222` `ThaumcraftApiHelper`; `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:34` `DummyInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:40` `IInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:109` `ThaumcraftInternalMethods`.
- **nit observation** — `containerContains(Aspect)`: `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:13` `IAspectContainer`; `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:87` `NodeAdapter`; `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:81` `NodeBase`.
- **nit observation** — `doesContainerAccept(Aspect)`: `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:7` `IAspectContainer`; `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:73` `NodeAdapter`; `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:59` `NodeBase`.
- **nit observation** — `doesContainerContain(AspectList)`: `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:12` `IAspectContainer`; `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:85` `NodeAdapter`; `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:79` `NodeBase`.
- **nit observation** — `doesContainerContainAmount(Aspect, int)`: `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:11` `IAspectContainer`; `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:81` `NodeAdapter`; `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:75` `NodeBase`.
- **nit observation** — `equals(Object)`: `common/src/main/java/thaumcraft/api/BlockCoordinates.java:53` `BlockCoordinates`; `common/src/main/java/thaumcraft/api/WorldCoordinates.java:46` `WorldCoordinates`; `common/src/main/java/thaumcraft/api/research/ScanResult.java:46` `ScanResult`; `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:82` `FocusUpgradeType`.
- **nit observation** — `generateTags(Item, int)`: `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:153` `ThaumcraftApiHelper`; `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:30` `DummyInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:56` `IInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:97` `ThaumcraftInternalMethods`.
- **nit observation** — `generateVisEffect(int, int, int, int, int, int, int, int)`: `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:16` `DummyInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:48` `IInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:35` `ThaumcraftInternalMethods`.
- **nit observation** — `get(String)`: `common/src/main/java/thaumcraft/api/blocks/BlocksTC.java:29` `BlocksTC`; `common/src/main/java/thaumcraft/api/items/ItemsTC.java:29` `ItemsTC`.
- **nit observation** — `getAspects()`: `common/src/main/java/thaumcraft/api/aspects/AspectList.java:49` `AspectList`; `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:5` `IAspectContainer`; `common/src/main/java/thaumcraft/api/crafting/IArcaneRecipe.java:24` `IArcaneRecipe`; `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:90` `InfusionEnchantmentRecipe`; `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:97` `InfusionRecipe`; `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:165` `ShapedArcaneRecipe`; `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:102` `ShapelessArcaneRecipe`; `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:44` `NodeAdapter`; `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:23` `NodeBase`; `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:62` `FocusUpgradeType`.
- **nit observation** — `getAspects(ArcaneRecipeInput)`: `common/src/main/java/thaumcraft/api/crafting/IArcaneRecipe.java:26` `IArcaneRecipe`; `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:167` `ShapedArcaneRecipe`; `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:104` `ShapelessArcaneRecipe`.
- **nit observation** — `getAspects(ItemStack)`: `common/src/main/java/thaumcraft/api/aspects/IEssentiaContainerItem.java:7` `IEssentiaContainerItem`; `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:109` `InfusionRecipe`.
- **nit observation** — `getAspectsBase()`: `common/src/main/java/thaumcraft/api/nodes/INode.java:47` `INode`; `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:48` `NodeAdapter`; `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:37` `NodeBase`.
- **nit observation** — `getBonusObjectTags(ItemStack, AspectList)`: `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:148` `ThaumcraftApiHelper`; `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:28` `DummyInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:54` `IInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:91` `ThaumcraftInternalMethods`.
- **nit observation** — `getComponents()`: `common/src/main/java/thaumcraft/api/aspects/Aspect.java:103` `Aspect`; `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:105` `InfusionRecipe`.
- **nit observation** — `getCraftCost()`: `common/src/main/java/thaumcraft/api/wands/WandCap.java:82` `WandCap`; `common/src/main/java/thaumcraft/api/wands/WandRod.java:89` `WandRod`.
- **nit observation** — `getCraftingResult(ArcaneRecipeInput)`: `common/src/main/java/thaumcraft/api/crafting/IArcaneRecipe.java:18` `IArcaneRecipe`; `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:126` `ShapedArcaneRecipe`; `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:72` `ShapelessArcaneRecipe`.
- **nit observation** — `getDiscoveredAspects(String)`: `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:120` `ThaumcraftApiHelper`; `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:22` `DummyInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:60` `IInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:49` `ThaumcraftInternalMethods`.
- **nit observation** — `getDistanceSquared(int, int, int)`: `common/src/main/java/thaumcraft/api/BlockCoordinates.java:92` `BlockCoordinates`; `common/src/main/java/thaumcraft/api/WorldCoordinates.java:39` `WorldCoordinates`.
- **nit observation** — `getId()`: `common/src/main/java/thaumcraft/api/nodes/INode.java:38` `INode`; `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:42` `NodeAdapter`; `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:21` `NodeBase`.
- **nit observation** — `getInput()`: `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:163` `ShapedArcaneRecipe`; `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:82` `ShapelessArcaneRecipe`.
- **nit observation** — `getItem()`: `common/src/main/java/thaumcraft/api/wands/WandCap.java:78` `WandCap`; `common/src/main/java/thaumcraft/api/wands/WandRod.java:85` `WandRod`.
- **nit observation** — `getKnowledge(EnumKnowledgeType, ResearchCategory)`: `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:75` `IPlayerKnowledge`; `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:108` `NativePlayerKnowledge`.
- **nit observation** — `getKnowledgeRaw(EnumKnowledgeType, ResearchCategory)`: `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:78` `IPlayerKnowledge`; `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:113` `NativePlayerKnowledge`.
- **nit observation** — `getLocalizedName()`: `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:62` `ResearchEntry`; `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:30` `TheorycraftCard`; `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:70` `FocusUpgradeType`.
- **nit observation** — `getLocalizedText()`: `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:32` `TheorycraftCard`; `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:78` `FocusUpgradeType`.
- **nit observation** — `getMaxAreaSize(ItemStack)`: `common/src/main/java/thaumcraft/api/IArchitect.java:71` `IArchitect`; `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:63` `ItemFocusBasic`.
- **nit observation** — `getName()`: `common/src/main/java/thaumcraft/api/aspects/Aspect.java:102` `Aspect`; `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:60` `ResearchEntry`; `common/src/main/java/thaumcraft/api/research/ResearchItem.java:179` `ResearchItem`.
- **nit observation** — `getNodeModifier()`: `common/src/main/java/thaumcraft/api/nodes/INode.java:55` `INode`; `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:60` `NodeAdapter`; `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:45` `NodeBase`.
- **nit observation** — `getNodeType()`: `common/src/main/java/thaumcraft/api/nodes/INode.java:49` `INode`; `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:50` `NodeAdapter`; `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:39` `NodeBase`.
- **nit observation** — `getNodeVisBase(Aspect)`: `common/src/main/java/thaumcraft/api/nodes/INode.java:57` `INode`; `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:64` `NodeAdapter`; `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:47` `NodeBase`.
- **nit observation** — `getObjectAspects(ItemStack)`: `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:137` `ThaumcraftApiHelper`; `common/src/main/java/thaumcraft/api/aspects/AspectHelper.java:16` `AspectHelper`; `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:26` `DummyInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:52` `IInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:85` `ThaumcraftInternalMethods`.
- **nit observation** — `getPrimalAspects()`: `common/src/main/java/thaumcraft/api/aspects/Aspect.java:134` `Aspect`; `common/src/main/java/thaumcraft/api/aspects/AspectList.java:50` `AspectList`.
- **nit observation** — `getRecipeOutput()`: `common/src/main/java/thaumcraft/api/crafting/CrucibleRecipe.java:82` `CrucibleRecipe`; `common/src/main/java/thaumcraft/api/crafting/IArcaneRecipe.java:22` `IArcaneRecipe`; `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:95` `InfusionRecipe`; `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:132` `ShapedArcaneRecipe`; `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:70` `ShapelessArcaneRecipe`.
- **nit observation** — `getRecipeSize()`: `common/src/main/java/thaumcraft/api/crafting/IArcaneRecipe.java:20` `IArcaneRecipe`; `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:130` `ShapedArcaneRecipe`; `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:68` `ShapelessArcaneRecipe`.
- **nit observation** — `getRecipes()`: `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:22` `ResearchAddendum`; `common/src/main/java/thaumcraft/api/research/ResearchStage.java:42` `ResearchStage`.
- **nit observation** — `getResearch()`: `common/src/main/java/thaumcraft/api/crafting/IArcaneRecipe.java:28` `IArcaneRecipe`; `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:92` `InfusionEnchantmentRecipe`; `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:101` `InfusionRecipe`; `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:169` `ShapedArcaneRecipe`; `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:106` `ShapelessArcaneRecipe`; `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:26` `ResearchAddendum`; `common/src/main/java/thaumcraft/api/research/ResearchStage.java:62` `ResearchStage`; `common/src/main/java/thaumcraft/api/wands/WandCap.java:87` `WandCap`; `common/src/main/java/thaumcraft/api/wands/WandRod.java:102` `WandRod`.
- **nit observation** — `getResearch(String)`: `common/src/main/java/thaumcraft/api/research/ResearchCategories.java:61` `ResearchCategories`; `common/src/main/java/thaumcraft/api/research/ResearchCategoriesCompat.java:13` `ResearchCategoriesCompat`; `common/src/main/java/thaumcraft/api/research/ResearchCategoryList.java:28` `ResearchCategoryList`.
- **nit observation** — `getResearchKey(Player, Object)`: `common/src/main/java/thaumcraft/api/research/IScanThing.java:15` `IScanThing`; `common/src/main/java/thaumcraft/api/research/ScanBlock.java:38` `ScanBlock`.
- **nit observation** — `getResearchList()`: `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:63` `IPlayerKnowledge`; `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:80` `NativePlayerKnowledge`.
- **nit observation** — `getResearchStage(String)`: `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:54` `IPlayerKnowledge`; `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:52` `NativePlayerKnowledge`.
- **nit observation** — `getResearchStatus(String)`: `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:46` `IPlayerKnowledge`; `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:36` `NativePlayerKnowledge`.
- **nit observation** — `getRunicCharge(ItemStack)`: `common/src/main/java/thaumcraft/api/IRunicArmor.java:6` `IRunicArmor`; `common/src/main/java/thaumcraft/api/ItemRunic.java:25` `ItemRunic`.
- **nit observation** — `getStackInRowAndColumn(Object, int, int)`: `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:133` `ThaumcraftApiHelper`; `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:24` `DummyInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:30` `IInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:70` `ThaumcraftInternalMethods`.
- **nit observation** — `getTag()`: `common/src/main/java/thaumcraft/api/aspects/Aspect.java:100` `Aspect`; `common/src/main/java/thaumcraft/api/wands/WandCap.java:74` `WandCap`; `common/src/main/java/thaumcraft/api/wands/WandRod.java:73` `WandRod`.
- **nit observation** — `getText()`: `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:14` `ResearchAddendum`; `common/src/main/java/thaumcraft/api/research/ResearchItem.java:181` `ResearchItem`; `common/src/main/java/thaumcraft/api/research/ResearchStage.java:34` `ResearchStage`.
- **nit observation** — `getTextLocalized()`: `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:16` `ResearchAddendum`; `common/src/main/java/thaumcraft/api/research/ResearchStage.java:36` `ResearchStage`.
- **nit observation** — `getTexture()`: `common/src/main/java/thaumcraft/api/wands/WandCap.java:70` `WandCap`; `common/src/main/java/thaumcraft/api/wands/WandRod.java:81` `WandRod`.
- **nit observation** — `getUpdatePacket()`: `common/src/main/java/thaumcraft/api/TileThaumcraft.java:41` `TileThaumcraft`; `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:187` `TileVisNode`.
- **nit observation** — `getVisDiscount(ItemStack, Player, Aspect)`: `common/src/main/java/thaumcraft/api/IVisDiscountGear.java:9` `IVisDiscountGear`; `common/src/main/java/thaumcraft/api/items/IVisDiscountGear.java:17` `IVisDiscountGear`.
- **nit observation** — `hasDiscoveredAspect(String, Aspect)`: `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:116` `ThaumcraftApiHelper`; `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:20` `DummyInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:58` `IInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:44` `ThaumcraftInternalMethods`.
- **nit observation** — `hasResearchFlag(String, EnumResearchFlag)`: `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:69` `IPlayerKnowledge`; `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:94` `NativePlayerKnowledge`.
- **nit observation** — `hashCode()`: `common/src/main/java/thaumcraft/api/BlockCoordinates.java:65` `BlockCoordinates`; `common/src/main/java/thaumcraft/api/WorldCoordinates.java:49` `WorldCoordinates`; `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:83` `FocusUpgradeType`.
- **nit observation** — `holder()`: `common/src/main/java/thaumcraft/api/potions/PotionFluxTaint.java:31` `PotionFluxTaint`; `common/src/main/java/thaumcraft/api/potions/PotionVisExhaust.java:31` `PotionVisExhaust`.
- **nit observation** — `id(String)`: `common/src/main/java/thaumcraft/api/blocks/BlocksTC.java:23` `BlocksTC`; `common/src/main/java/thaumcraft/api/items/ItemsTC.java:23` `ItemsTC`.
- **nit observation** — `instance()`: `common/src/main/java/thaumcraft/api/potions/PotionFluxTaint.java:26` `PotionFluxTaint`; `common/src/main/java/thaumcraft/api/potions/PotionVisExhaust.java:26` `PotionVisExhaust`.
- **nit observation** — `isRegistered(String)`: `common/src/main/java/thaumcraft/api/blocks/BlocksTC.java:49` `BlocksTC`; `common/src/main/java/thaumcraft/api/items/ItemsTC.java:51` `ItemsTC`.
- **nit observation** — `isResearchComplete(Player, String)`: `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:111` `ThaumcraftApiHelper`; `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:24` `IInternalMethodHandler`.
- **nit observation** — `isResearchComplete(String)`: `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:48` `IPlayerKnowledge`; `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:44` `NativePlayerKnowledge`.
- **nit observation** — `isResearchComplete(String, String)`: `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:106` `ThaumcraftApiHelper`; `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:18` `DummyInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:50` `IInternalMethodHandler`; `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:39` `ThaumcraftInternalMethods`.
- **nit observation** — `isResearchKnown(String)`: `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:51` `IPlayerKnowledge`; `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:48` `NativePlayerKnowledge`.
- **nit observation** — `itemMatches(ItemStack, ItemStack, boolean)`: `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:181` `ThaumcraftApiHelper`; `common/src/main/java/thaumcraft/api/crafting/CrucibleRecipe.java:85` `CrucibleRecipe`.
- **nit observation** — `knowledge()`: `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:32` `NativePlayerKnowledge`; `common/src/main/java/thaumcraft/api/research/ResearchStage.java:75` `ResearchStage`.
- **nit observation** — `matches(ArcaneRecipeInput, Level, Player)`: `common/src/main/java/thaumcraft/api/crafting/IArcaneRecipe.java:16` `IArcaneRecipe`; `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:134` `ShapedArcaneRecipe`; `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:76` `ShapelessArcaneRecipe`.
- **nit observation** — `matches(ArrayList<ItemStack>, ItemStack, Level, Player)`: `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:77` `InfusionEnchantmentRecipe`; `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:73` `InfusionRecipe`.
- **nit observation** — `names()`: `common/src/main/java/thaumcraft/api/blocks/BlocksTC.java:52` `BlocksTC`; `common/src/main/java/thaumcraft/api/items/ItemsTC.java:54` `ItemsTC`.
- **nit observation** — `nativeRecipe()`: `common/src/main/java/thaumcraft/api/crafting/CrucibleRecipe.java:46` `CrucibleRecipe`; `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:52` `InfusionEnchantmentRecipe`; `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:52` `InfusionRecipe`; `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:100` `ShapedArcaneRecipe`; `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:57` `ShapelessArcaneRecipe`.
- **nit observation** — `readNBT(CompoundTag)`: `common/src/main/java/thaumcraft/api/BlockCoordinates.java:109` `BlockCoordinates`; `common/src/main/java/thaumcraft/api/WorldCoordinates.java:62` `WorldCoordinates`.
- **nit observation** — `registerComplexObjectTag(ItemStack, AspectList)`: `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:451` `ThaumcraftApi`; `common/src/main/java/thaumcraft/api/aspects/AspectEventProxy.java:34` `AspectEventProxy`.
- **nit observation** — `registerObjectTag(ItemStack, AspectList)`: `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:420` `ThaumcraftApi`; `common/src/main/java/thaumcraft/api/aspects/AspectEventProxy.java:14` `AspectEventProxy`.
- **nit observation** — `registerObjectTag(ItemStack, int[], AspectList)`: `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:431` `ThaumcraftApi`; `common/src/main/java/thaumcraft/api/aspects/AspectEventProxy.java:19` `AspectEventProxy`.
- **nit observation** — `registerObjectTag(String, AspectList)`: `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:441` `ThaumcraftApi`; `common/src/main/java/thaumcraft/api/aspects/AspectEventProxy.java:29` `AspectEventProxy`.
- **nit observation** — `registries()`: `common/src/main/java/thaumcraft/api/crafting/ArcaneRecipes.java:73` `ArcaneRecipes`; `common/src/main/java/thaumcraft/api/crafting/NativeRecipes.java:45` `NativeRecipes`; `common/src/main/java/thaumcraft/api/research/ResearchPage.java:171` `ResearchPage`.
- **nit observation** — `removeResearch(String)`: `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:60` `IPlayerKnowledge`; `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:73` `NativePlayerKnowledge`.
- **nit observation** — `require(String)`: `common/src/main/java/thaumcraft/api/blocks/BlocksTC.java:36` `BlocksTC`; `common/src/main/java/thaumcraft/api/items/ItemsTC.java:36` `ItemsTC`.
- **nit observation** — `setAspects(AspectList)`: `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:6` `IAspectContainer`; `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:46` `NodeAdapter`; `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:25` `NodeBase`.
- **nit observation** — `setCraftCost(int)`: `common/src/main/java/thaumcraft/api/wands/WandCap.java:84` `WandCap`; `common/src/main/java/thaumcraft/api/wands/WandRod.java:91` `WandRod`.
- **nit observation** — `setItem(ItemStack)`: `common/src/main/java/thaumcraft/api/wands/WandCap.java:80` `WandCap`; `common/src/main/java/thaumcraft/api/wands/WandRod.java:87` `WandRod`.
- **nit observation** — `setNodeModifier(NodeModifier)`: `common/src/main/java/thaumcraft/api/nodes/INode.java:53` `INode`; `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:56` `NodeAdapter`; `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:43` `NodeBase`.
- **nit observation** — `setNodeType(NodeType)`: `common/src/main/java/thaumcraft/api/nodes/INode.java:51` `INode`; `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:52` `NodeAdapter`; `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:41` `NodeBase`.
- **nit observation** — `setNodeVisBase(Aspect, short)`: `common/src/main/java/thaumcraft/api/nodes/INode.java:59` `INode`; `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:67` `NodeAdapter`; `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:50` `NodeBase`.
- **nit observation** — `setRecipes(ResourceLocation[])`: `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:24` `ResearchAddendum`; `common/src/main/java/thaumcraft/api/research/ResearchStage.java:44` `ResearchStage`.
- **nit observation** — `setResearch(String[])`: `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:28` `ResearchAddendum`; `common/src/main/java/thaumcraft/api/research/ResearchStage.java:64` `ResearchStage`.
- **nit observation** — `setResearchFlag(String, EnumResearchFlag)`: `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:65` `IPlayerKnowledge`; `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:82` `NativePlayerKnowledge`.
- **nit observation** — `setResearchStage(String, int)`: `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:58` `IPlayerKnowledge`; `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:67` `NativePlayerKnowledge`.
- **nit observation** — `setTag(String)`: `common/src/main/java/thaumcraft/api/aspects/Aspect.java:121` `Aspect`; `common/src/main/java/thaumcraft/api/wands/WandCap.java:76` `WandCap`; `common/src/main/java/thaumcraft/api/wands/WandRod.java:75` `WandRod`.
- **nit observation** — `setText(String)`: `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:20` `ResearchAddendum`; `common/src/main/java/thaumcraft/api/research/ResearchStage.java:40` `ResearchStage`.
- **nit observation** — `setTexture(ResourceLocation)`: `common/src/main/java/thaumcraft/api/wands/WandCap.java:72` `WandCap`; `common/src/main/java/thaumcraft/api/wands/WandRod.java:83` `WandRod`.
- **nit observation** — `size()`: `common/src/main/java/thaumcraft/api/aspects/AspectList.java:46` `AspectList`; `common/src/main/java/thaumcraft/api/research/ResearchCategoryList.java:33` `ResearchCategoryList`.
- **nit observation** — `stack(String)`: `common/src/main/java/thaumcraft/api/blocks/BlocksTC.java:43` `BlocksTC`; `common/src/main/java/thaumcraft/api/items/ItemsTC.java:43` `ItemsTC`.
- **nit observation** — `sync(ServerPlayer)`: `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:81` `IPlayerKnowledge`; `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:120` `NativePlayerKnowledge`.
- **nit observation** — `takeFromContainer(Aspect, int)`: `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:9` `IAspectContainer`; `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:77` `NodeAdapter`; `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:68` `NodeBase`.
- **nit observation** — `takeFromContainer(AspectList)`: `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:10` `IAspectContainer`; `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:79` `NodeAdapter`; `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:73` `NodeBase`.
- **nit observation** — `writeNBT(CompoundTag)`: `common/src/main/java/thaumcraft/api/BlockCoordinates.java:117` `BlockCoordinates`; `common/src/main/java/thaumcraft/api/WorldCoordinates.java:57` `WorldCoordinates`.

## 7. Legacy shapes and nonfunctional compatibility contracts

**Inspected: all 93 files / 1,101 public declarations**, including signatures and adapter bodies. **18 migration-contract observations**, of which the final UUID item is an already documented, functional migration boundary rather than an outstanding defect. No actual ForgeDirection, NBTTagCompound, EntityPlayer or World legacy import remains in compiled API signatures; those names occur in provenance comments. CompoundTag persistence on block entities is valid 1.21.1 code and was not misclassified as obsolete item NBT.

- **should-fix L01** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:86`: Five enchant* numeric-id fields cannot identify data-driven enchantments. Replacement: the adjacent ResourceKey<Enchantment> constants, resolved using the level registry access.
- **should-fix L02** — `common/src/main/java/thaumcraft/api/wands/WandTriggerRegistry.java:37`: Six block+metadata overloads cannot represent state selection and silently ignore meta; dispatch even substitutes defaultBlockState. Replacement: BlockState registration/query/dispatch, or explicit whole-Block forms. Deprecation warnings added here; behavior retained.
- **should-fix L03** — `common/src/main/java/thaumcraft/api/ItemApi.java:30`: getItem/getBlock metadata cannot select flattened variants: only damageable stacks get wear. Replacement: native namespaced item/block ids, BlockState for stateful blocks, data components for stack variants.
- **should-fix L04** — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:424`: Object-tag metadata overloads are damage keys, not historical subtype metadata. Replacement: distinct Item/ResourceLocation identities and native item TagKey; component-sensitive predicates require an explicit new contract, not a pretend metadata number.
- **should-fix L05** — `common/src/main/java/thaumcraft/api/research/ScanResult.java:14`: id/meta are runtime registry ids/damage, not persistent old item identities. Replacement: ScanResult.item(ItemStack) and its stack witness, nodePosition plus nodeDimension for nodes. Do not persist integer ids across registries.
- **blocker L06** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:48`: generateVisEffect(int dim,...) is a no-op in the installed handler; numeric dimensions cannot select a native ServerLevel. Replacement: VisNetHandler.generateVisEffect with ServerLevel and positions.
- **blocker L07** — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:22`: The newer ServerLevel effect hook and native player/world-scoped service overloads remain default no-ops/null/false in installed ThaumcraftInternalMethods. Native-looking overloads are not functional replacements. Route through implemented services or implement before publishing them as real.
- **should-fix L08** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:148`: getBonusObjectTags without a level only copies the supplied list, and generateTags without a level does not derive recipe tags. Replacement: getObjectAspects(ItemStack,Level) for scan-complete results; no equivalent public level-aware bonus/generate facade is currently implemented here.
- **should-fix L09** — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:238`: rayTraceIgnoringSource accepts includeFluid/reportMiss legacy flags, but the manual traversal never tests fluid shapes and unobstructed rays always return null. Replacement: a documented native Level.clip/ClipContext implementation when those semantics are required.
- **should-fix L10** — `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:34`: Object[] result patches only recognize five hard-coded old keys and ignore arbitrary old NBT payload semantics. Replacement: named native InfusionResultPatches/data-component changes; arbitrary addon component transforms need a deliberately supported extension.
- **should-fix L11** — `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:86`: fuzzy argument cannot provide old OreDictionary equivalence and is ignored. Replacement: native Ingredient/TagKey matching; do not infer ore equivalence from this comparator.
- **should-fix L12** — `common/src/main/java/thaumcraft/api/crafting/CrucibleRecipe.java:27`: Constructor keeps legacy Object catalyst shape, but ArcaneRecipes.ingredient does not accept the ArrayList<ItemStack> form that this constructor still hashes (:39). Replacement: Ingredient or namespaced item tag; document supported types.
- **should-fix L13** — `common/src/main/java/thaumcraft/api/research/ResearchPage.java:88`: World-free smelting page cannot resolve recipeOutput. Replacement: ResearchPage(ItemStack,Level); the old constructor remains a deferred/display-only shape, not a resolved furnace recipe.
- **blocker L14** — `common/src/main/java/thaumcraft/api/research/ScanningManager.java:18`: Scannable registration/find form an isolated registry with no common gameplay caller. An addon can register and unit-test a handler that the thaumometer never invokes. Replacement today: ThaumcraftApi.registerScanEventhandler for phenomena; no equivalent generic target callback dispatch is wired.
- **should-fix L15** — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftManager.java:16`: TC6-style card/aid registration has no native research-table consumer. TC4 uses aspect/hex research, not this theorycraft flow. Replacement: actual TC4 ResearchItem/ResearchPage and research-note contracts, or mark this explicitly unsupported compatibility data.
- **should-fix L16** — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:108`: TC6 stages/addenda/reward setters are DTO storage only, not connected to TC4 ResearchItem/gameplay. Replacement: TC4 ResearchItem registration and supported page/trigger methods; do not imply setting stages changes progression.
- **should-fix L17** — `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:15`: Legacy no-stack focus contract is not consumed or implemented by native focus dispatch, which uses ItemFocusBasic methods with stack/hand arguments. Replacement: ItemFocusBasic and the actual supported FocusUpgradeData path; do not treat implementing IWandFocus as native focus registration.
- **nit L18** — `common/src/main/java/thaumcraft/api/nodes/INode.java:38`: Node identity is now a persisted UUID, not dim:x:y:z; this is already documented and functional. Addon code must treat it as opaque or UUID rather than parse numeric dimensions. Not a remaining ForgeDirection/NBTTagCompound signature defect.

## 8. Contract test coverage

**Inspected: all 103 public types, against 731 active common/Fabric test source files. Found: 24 types with no identified active behavioral contract test.** This is a source audit, not an executed coverage report. The full table distinguishes partial behavioral evidence from no contract evidence. Indirect facade tests are credited only for observed behavior (not merely matching names); e.g. AspectEventProxy is exercised through event.register and NativeRecipes through ThaumcraftApi.installRecipeManager/getCraftingRecipes.

Common root `src/test` has useful historical tests (e.g. repair callbacks), but they are not part of the modern common source set and are not counted as current coverage. ApiSurfaceTest’s Class.forName list keeps names public but would not detect a method silently becoming inert. Generic IScanThing list immutability does not test its callbacks. Matching class names from common crafting/potion implementations are not credited to their API counterparts.

| Public type / baseline source | Contract evidence / gap |
|---|---|
| `thaumcraft.api.BlockCoordinates` — `common/src/main/java/thaumcraft/api/BlockCoordinates.java:22` | Partial: `common/src/test/java/thaumcraft/api/PublicApiContractTest.java` |
| `thaumcraft.api.IArchitect` — `common/src/main/java/thaumcraft/api/IArchitect.java:27` | **should-fix uncovered** — add an active behavioral contract test; class-presence checks are insufficient. |
| `thaumcraft.api.IArchitect.EnumAxis` — `common/src/main/java/thaumcraft/api/IArchitect.java:33` | Partial: `common/src/test/java/thaumcraft/api/IArchitectAxisTest.java` |
| `thaumcraft.api.IGoggles` — `common/src/main/java/thaumcraft/api/IGoggles.java:7` | Partial: `common/src/gameTest/java/thaumcraft/common/testing/FortressRuntimeChecks.java` |
| `thaumcraft.api.IRepairable` — `common/src/main/java/thaumcraft/api/IRepairable.java:4` | Partial: `common/src/gameTest/java/thaumcraft/common/testing/FortressRuntimeChecks.java` |
| `thaumcraft.api.IRepairableExtended` — `common/src/main/java/thaumcraft/api/IRepairableExtended.java:7` | **should-fix uncovered** — add an active behavioral contract test; class-presence checks are insufficient. |
| `thaumcraft.api.IRunicArmor` — `common/src/main/java/thaumcraft/api/IRunicArmor.java:5` | Partial: `common/src/gameTest/java/thaumcraft/common/testing/FortressRuntimeChecks.java` |
| `thaumcraft.api.IScribeTools` — `common/src/main/java/thaumcraft/api/IScribeTools.java:4` | Partial: `common/src/gameTest/java/thaumcraft/common/testing/ResearchNoteRuntimeChecks.java` |
| `thaumcraft.api.IVisDiscountGear` — `common/src/main/java/thaumcraft/api/IVisDiscountGear.java:7` | Partial: `common/src/gameTest/java/thaumcraft/common/testing/GogglesRuntimeChecks.java` |
| `thaumcraft.api.IWarpingGear` — `common/src/main/java/thaumcraft/api/IWarpingGear.java:10` | Partial: `common/src/gameTest/java/thaumcraft/common/testing/EquipmentItemRuntimeChecks.java` |
| `thaumcraft.api.ItemApi` — `common/src/main/java/thaumcraft/api/ItemApi.java:22` | Partial: `common/src/test/java/thaumcraft/api/ItemsBlocksApiTest.java` |
| `thaumcraft.api.ItemRunic` — `common/src/main/java/thaumcraft/api/ItemRunic.java:12` | **should-fix uncovered** — add an active behavioral contract test; class-presence checks are insufficient. |
| `thaumcraft.api.ThaumcraftApi` — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:49` | Partial: `common/src/test/java/thaumcraft/api/ApiSurfaceTest.java` |
| `thaumcraft.api.ThaumcraftApi.EntityTags` — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:489` | Partial: `common/src/test/java/thaumcraft/research/EntityScanApiTest.java` |
| `thaumcraft.api.ThaumcraftApi.EntityTagsNBT` — `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:497` | Partial: `common/src/test/java/thaumcraft/research/EntityScanApiTest.java` |
| `thaumcraft.api.ThaumcraftApiHelper` — `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:32` | Partial: `common/src/test/java/thaumcraft/api/PublicApiContractTest.java` |
| `thaumcraft.api.ThaumcraftMaterials` — `common/src/main/java/thaumcraft/api/ThaumcraftMaterials.java:23` | **should-fix uncovered** — add an active behavioral contract test; class-presence checks are insufficient. |
| `thaumcraft.api.TileThaumcraft` — `common/src/main/java/thaumcraft/api/TileThaumcraft.java:20` | **should-fix uncovered** — add an active behavioral contract test; class-presence checks are insufficient. |
| `thaumcraft.api.WorldCoordinates` — `common/src/main/java/thaumcraft/api/WorldCoordinates.java:16` | Partial: `common/src/gameTest/java/thaumcraft/common/testing/VisNetRuntimeChecks.java` |
| `thaumcraft.api.aspects.Aspect` — `common/src/main/java/thaumcraft/api/aspects/Aspect.java:12` | Partial: `common/src/test/java/thaumcraft/api/ApiSurfaceTest.java` |
| `thaumcraft.api.aspects.AspectEventProxy` — `common/src/main/java/thaumcraft/api/aspects/AspectEventProxy.java:12` | Partial: `common/src/test/java/thaumcraft/api/AspectApiTest.java` (facade/implementation behavior) |
| `thaumcraft.api.aspects.AspectHelper` — `common/src/main/java/thaumcraft/api/aspects/AspectHelper.java:12` | Partial: `common/src/test/java/thaumcraft/api/AspectApiTest.java` |
| `thaumcraft.api.aspects.AspectList` — `common/src/main/java/thaumcraft/api/aspects/AspectList.java:15` | Partial: `common/src/test/java/thaumcraft/api/ApiSurfaceTest.java` |
| `thaumcraft.api.aspects.AspectRegistryEvent` — `common/src/main/java/thaumcraft/api/aspects/AspectRegistryEvent.java:15` | Partial: `common/src/test/java/thaumcraft/api/AspectApiTest.java` |
| `thaumcraft.api.aspects.AspectSourceHelper` — `common/src/main/java/thaumcraft/api/aspects/AspectSourceHelper.java:23` | Partial: `common/src/test/java/thaumcraft/api/AspectApiTest.java` |
| `thaumcraft.api.aspects.IAspectContainer` — `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:4` | Partial: `common/src/test/java/thaumcraft/tiles/TileInventorySurfaceTest.java` |
| `thaumcraft.api.aspects.IAspectSource` — `common/src/main/java/thaumcraft/api/aspects/IAspectSource.java:4` | Partial: `common/src/gameTest/java/thaumcraft/common/testing/InfusionComponentPredicateGameTests.java` |
| `thaumcraft.api.aspects.IEssentiaContainerItem` — `common/src/main/java/thaumcraft/api/aspects/IEssentiaContainerItem.java:6` | Partial: `common/src/gameTest/java/thaumcraft/common/testing/CrystallizerRuntimeChecks.java` |
| `thaumcraft.api.aspects.IEssentiaTransport` — `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:6` | Partial: `common/src/gameTest/java/thaumcraft/common/testing/AlembicRuntimeChecks.java` |
| `thaumcraft.api.aura.AuraHelper` — `common/src/main/java/thaumcraft/api/aura/AuraHelper.java:18` | Partial: `common/src/test/java/thaumcraft/api/AuraHelperDrainTest.java` |
| `thaumcraft.api.baubles.BaubleType` — `common/src/main/java/thaumcraft/api/baubles/BaubleType.java:4` | Partial: `common/src/test/java/thaumcraft/baubles/AccessorySlotTypesTest.java` |
| `thaumcraft.api.baubles.IBauble` — `common/src/main/java/thaumcraft/api/baubles/IBauble.java:7` | Partial: `common/src/gameTest/java/thaumcraft/common/testing/AccessoriesRuntimeChecks.java` |
| `thaumcraft.api.blocks.BlocksTC` — `common/src/main/java/thaumcraft/api/blocks/BlocksTC.java:19` | Partial: `common/src/test/java/thaumcraft/api/ItemsBlocksApiTest.java` |
| `thaumcraft.api.capabilities.IPlayerKnowledge` — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:14` | Partial: `common/src/test/java/thaumcraft/api/CapabilitiesApiTest.java` |
| `thaumcraft.api.capabilities.IPlayerKnowledge.EnumKnowledgeType` — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:16` | Partial: `common/src/test/java/thaumcraft/api/CapabilitiesApiTest.java` |
| `thaumcraft.api.capabilities.IPlayerKnowledge.EnumResearchStatus` — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:38` | Partial: `common/src/test/java/thaumcraft/api/CapabilitiesApiTest.java` |
| `thaumcraft.api.capabilities.IPlayerKnowledge.EnumResearchFlag` — `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:41` | Partial: `common/src/test/java/thaumcraft/api/CapabilitiesApiTest.java` |
| `thaumcraft.api.capabilities.NativePlayerKnowledge` — `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:22` | Partial: `common/src/test/java/thaumcraft/api/CapabilitiesApiTest.java` |
| `thaumcraft.api.crafting.ArcaneRecipes` — `common/src/main/java/thaumcraft/api/crafting/ArcaneRecipes.java:24` | Partial: `common/src/test/java/thaumcraft/api/CraftingRecipeListTest.java` |
| `thaumcraft.api.crafting.CrucibleRecipe` — `common/src/main/java/thaumcraft/api/crafting/CrucibleRecipe.java:19` | Partial: `common/src/test/java/thaumcraft/api/CraftingApiTest.java` |
| `thaumcraft.api.crafting.IArcaneRecipe` — `common/src/main/java/thaumcraft/api/crafting/IArcaneRecipe.java:15` | Partial: `common/src/test/java/thaumcraft/api/CraftingApiTest.java` (facade/implementation behavior) |
| `thaumcraft.api.crafting.IInfusionStabiliser` — `common/src/main/java/thaumcraft/api/crafting/IInfusionStabiliser.java:7` | Partial: `common/src/gameTest/java/thaumcraft/common/testing/ColoredCandlesRuntimeChecks.java` |
| `thaumcraft.api.crafting.InfusionEnchantmentRecipe` — `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:25` | **should-fix uncovered** — add an active behavioral contract test; class-presence checks are insufficient. |
| `thaumcraft.api.crafting.InfusionRecipe` — `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:25` | Partial: `common/src/test/java/thaumcraft/api/CraftingApiTest.java` |
| `thaumcraft.api.crafting.NativeRecipes` — `common/src/main/java/thaumcraft/api/crafting/NativeRecipes.java:35` | Partial: `common/src/test/java/thaumcraft/api/NativeRecipeApiTest.java` (facade/implementation behavior) |
| `thaumcraft.api.crafting.ShapedArcaneRecipe` — `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:27` | Partial: `common/src/test/java/thaumcraft/api/CraftingApiTest.java` |
| `thaumcraft.api.crafting.ShapelessArcaneRecipe` — `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:25` | Partial: `common/src/test/java/thaumcraft/api/CraftingApiTest.java` |
| `thaumcraft.api.damagesource.DamageSourceIndirectThaumcraftEntity` — `common/src/main/java/thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java:28` | **should-fix uncovered** — add an active behavioral contract test; class-presence checks are insufficient. |
| `thaumcraft.api.damagesource.DamageSourceThaumcraft` — `common/src/main/java/thaumcraft/api/damagesource/DamageSourceThaumcraft.java:25` | Partial: `common/src/test/java/thaumcraft/api/DamageSourceThaumcraftTest.java` |
| `thaumcraft.api.entities.IEldritchMob` — `common/src/main/java/thaumcraft/api/entities/IEldritchMob.java:3` | **should-fix uncovered** — add an active behavioral contract test; class-presence checks are insufficient. |
| `thaumcraft.api.entities.ITaintedMob` — `common/src/main/java/thaumcraft/api/entities/ITaintedMob.java:4` | Partial: `common/src/gameTest/java/thaumcraft/common/testing/FluxOverfullRuntimeChecks.java` |
| `thaumcraft.api.entities.IThaumcraftBoss` — `common/src/main/java/thaumcraft/api/entities/IThaumcraftBoss.java:16` | Partial: `common/src/gameTest/java/thaumcraft/common/testing/BossFightRuntimeChecks.java` |
| `thaumcraft.api.internal.DummyInternalMethodHandler` — `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:15` | **should-fix uncovered** — add an active behavioral contract test; class-presence checks are insufficient. |
| `thaumcraft.api.internal.IInternalMethodHandler` — `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:21` | Partial: `common/src/test/java/thaumcraft/api/ApiSurfaceTest.java` |
| `thaumcraft.api.internal.ThaumcraftInternalMethods` — `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:31` | Partial: `common/src/test/java/thaumcraft/api/PublicApiContractTest.java` (facade/implementation behavior) |
| `thaumcraft.api.internal.WeightedRandomLoot` — `common/src/main/java/thaumcraft/api/internal/WeightedRandomLoot.java:7` | Partial: `common/src/test/java/thaumcraft/loot/LootBagRegistrationTest.java` |
| `thaumcraft.api.items.IGoggles` — `common/src/main/java/thaumcraft/api/items/IGoggles.java:10` | **should-fix uncovered** — add an active behavioral contract test; class-presence checks are insufficient. |
| `thaumcraft.api.items.IRevealer` — `common/src/main/java/thaumcraft/api/items/IRevealer.java:10` | **should-fix uncovered** — add an active behavioral contract test; class-presence checks are insufficient. |
| `thaumcraft.api.items.IVisDiscountGear` — `common/src/main/java/thaumcraft/api/items/IVisDiscountGear.java:14` | **should-fix uncovered** — add an active behavioral contract test; class-presence checks are insufficient. |
| `thaumcraft.api.items.ItemsTC` — `common/src/main/java/thaumcraft/api/items/ItemsTC.java:19` | Partial: `common/src/test/java/thaumcraft/api/ItemsBlocksApiTest.java` |
| `thaumcraft.api.nodes.INode` — `common/src/main/java/thaumcraft/api/nodes/INode.java:30` | Partial: `common/src/test/java/thaumcraft/api/NodeAdapterContractTest.java` |
| `thaumcraft.api.nodes.IRevealer` — `common/src/main/java/thaumcraft/api/nodes/IRevealer.java:6` | Partial: `common/src/gameTest/java/thaumcraft/common/testing/FortressRuntimeChecks.java` |
| `thaumcraft.api.nodes.NodeAdapter` — `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:21` | Partial: `common/src/test/java/thaumcraft/api/NodeAdapterContractTest.java` |
| `thaumcraft.api.nodes.NodeBase` — `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:14` | Partial: `common/src/test/java/thaumcraft/api/NodesApiTest.java` |
| `thaumcraft.api.nodes.NodeModifier` — `common/src/main/java/thaumcraft/api/nodes/NodeModifier.java:4` | Partial: `common/src/test/java/thaumcraft/api/NodeAdapterContractTest.java` |
| `thaumcraft.api.nodes.NodeType` — `common/src/main/java/thaumcraft/api/nodes/NodeType.java:4` | Partial: `common/src/test/java/thaumcraft/api/NodeAdapterContractTest.java` |
| `thaumcraft.api.potions.PotionFluxTaint` — `common/src/main/java/thaumcraft/api/potions/PotionFluxTaint.java:18` | **should-fix uncovered** — add an active behavioral contract test; class-presence checks are insufficient. |
| `thaumcraft.api.potions.PotionVisExhaust` — `common/src/main/java/thaumcraft/api/potions/PotionVisExhaust.java:18` | **should-fix uncovered** — add an active behavioral contract test; class-presence checks are insufficient. |
| `thaumcraft.api.research.IScanEventHandler` — `common/src/main/java/thaumcraft/api/research/IScanEventHandler.java:8` | Partial: `common/src/test/java/thaumcraft/research/ScanCallbackDispatchTest.java` |
| `thaumcraft.api.research.IScanThing` — `common/src/main/java/thaumcraft/api/research/IScanThing.java:10` | **should-fix uncovered** — add an active behavioral contract test; class-presence checks are insufficient. |
| `thaumcraft.api.research.ResearchAddendum` — `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:9` | **should-fix uncovered** — add an active behavioral contract test; class-presence checks are insufficient. |
| `thaumcraft.api.research.ResearchCategories` — `common/src/main/java/thaumcraft/api/research/ResearchCategories.java:19` | Partial: `common/src/test/java/thaumcraft/api/ResearchApiTest.java` |
| `thaumcraft.api.research.ResearchCategoriesCompat` — `common/src/main/java/thaumcraft/api/research/ResearchCategoriesCompat.java:9` | Partial: `common/src/test/java/thaumcraft/api/ResearchApiTest.java` |
| `thaumcraft.api.research.ResearchCategory` — `common/src/main/java/thaumcraft/api/research/ResearchCategory.java:13` | Partial: `common/src/test/java/thaumcraft/api/CapabilitiesApiTest.java` |
| `thaumcraft.api.research.ResearchCategoryList` — `common/src/main/java/thaumcraft/api/research/ResearchCategoryList.java:13` | Partial: `common/src/test/java/thaumcraft/api/ResearchApiTest.java` (facade/implementation behavior) |
| `thaumcraft.api.research.ResearchEntry` — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:13` | Partial: `common/src/test/java/thaumcraft/api/ResearchApiTest.java` |
| `thaumcraft.api.research.ResearchEntry.EnumResearchMeta` — `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:28` | **should-fix uncovered** — add an active behavioral contract test; class-presence checks are insufficient. |
| `thaumcraft.api.research.ResearchItem` — `common/src/main/java/thaumcraft/api/research/ResearchItem.java:16` | Partial: `common/src/test/java/thaumcraft/api/ResearchApiTest.java` |
| `thaumcraft.api.research.ResearchPage` — `common/src/main/java/thaumcraft/api/research/ResearchPage.java:22` | Partial: `common/src/test/java/thaumcraft/api/ResearchApiTest.java` |
| `thaumcraft.api.research.ResearchPage.PageType` — `common/src/main/java/thaumcraft/api/research/ResearchPage.java:175` | Partial: `common/src/test/java/thaumcraft/api/ResearchApiTest.java` |
| `thaumcraft.api.research.ResearchStage` — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:10` | **should-fix uncovered** — add an active behavioral contract test; class-presence checks are insufficient. |
| `thaumcraft.api.research.ResearchStage.Knowledge` — `common/src/main/java/thaumcraft/api/research/ResearchStage.java:22` | **should-fix uncovered** — add an active behavioral contract test; class-presence checks are insufficient. |
| `thaumcraft.api.research.ScanBlock` — `common/src/main/java/thaumcraft/api/research/ScanBlock.java:14` | **should-fix uncovered** — add an active behavioral contract test; class-presence checks are insufficient. |
| `thaumcraft.api.research.ScanResult` — `common/src/main/java/thaumcraft/api/research/ScanResult.java:12` | Partial: `common/src/test/java/thaumcraft/research/ScanCallbackDispatchTest.java` |
| `thaumcraft.api.research.ScanningManager` — `common/src/main/java/thaumcraft/api/research/ScanningManager.java:13` | Partial: `common/src/test/java/thaumcraft/api/PublicApiContractTest.java` |
| `thaumcraft.api.research.theorycraft.ITheorycraftAid` — `common/src/main/java/thaumcraft/api/research/theorycraft/ITheorycraftAid.java:6` | Partial: `common/src/test/java/thaumcraft/api/TheorycraftApiTest.java` |
| `thaumcraft.api.research.theorycraft.ResearchTableData` — `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:13` | Partial: `common/src/test/java/thaumcraft/api/TheorycraftApiTest.java` |
| `thaumcraft.api.research.theorycraft.TheorycraftCard` — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:14` | Partial: `common/src/test/java/thaumcraft/api/TheorycraftApiTest.java` |
| `thaumcraft.api.research.theorycraft.TheorycraftManager` — `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftManager.java:10` | Partial: `common/src/test/java/thaumcraft/api/TheorycraftApiTest.java` |
| `thaumcraft.api.visnet.TileVisNode` — `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:20` | Partial: `common/src/gameTest/java/thaumcraft/common/testing/AdvancedFurnaceCallbackRuntimeChecks.java` |
| `thaumcraft.api.visnet.VisNetHandler` — `common/src/main/java/thaumcraft/api/visnet/VisNetHandler.java:38` | Partial: `common/src/test/java/thaumcraft/api/AuraHelperDrainTest.java` |
| `thaumcraft.api.wands.FocusUpgradeType` — `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:11` | Partial: `common/src/test/java/thaumcraft/wands/FocusUpgradeDataTest.java` |
| `thaumcraft.api.wands.IWandFocus` — `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:15` | **should-fix uncovered** — add an active behavioral contract test; class-presence checks are insufficient. |
| `thaumcraft.api.wands.IWandFocus.WandFocusAnimation` — `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:45` | **should-fix uncovered** — add an active behavioral contract test; class-presence checks are insufficient. |
| `thaumcraft.api.wands.IWandRodOnUpdate` — `common/src/main/java/thaumcraft/api/wands/IWandRodOnUpdate.java:11` | **should-fix uncovered** — add an active behavioral contract test; class-presence checks are insufficient. |
| `thaumcraft.api.wands.IWandTriggerManager` — `common/src/main/java/thaumcraft/api/wands/IWandTriggerManager.java:12` | Partial: `common/src/test/java/thaumcraft/api/WandApiTest.java` |
| `thaumcraft.api.wands.IWandable` — `common/src/main/java/thaumcraft/api/wands/IWandable.java:12` | **should-fix uncovered** — add an active behavioral contract test; class-presence checks are insufficient. |
| `thaumcraft.api.wands.ItemFocusBasic` — `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:21` | Partial: `common/src/gameTest/java/thaumcraft/common/testing/CombatFocusSpecialRuntimeChecks.java` |
| `thaumcraft.api.wands.ItemFocusBasic.WandFocusAnimation` — `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:124` | Partial: `common/src/gameTest/java/thaumcraft/common/testing/CombatFocusSpecialRuntimeChecks.java` |
| `thaumcraft.api.wands.StaffRod` — `common/src/main/java/thaumcraft/api/wands/StaffRod.java:12` | Partial: `common/src/test/java/thaumcraft/api/WandApiTest.java` |
| `thaumcraft.api.wands.WandCap` — `common/src/main/java/thaumcraft/api/wands/WandCap.java:15` | Partial: `common/src/test/java/thaumcraft/api/WandApiTest.java` |
| `thaumcraft.api.wands.WandRod` — `common/src/main/java/thaumcraft/api/wands/WandRod.java:16` | Partial: `common/src/test/java/thaumcraft/api/WandApiTest.java` |
| `thaumcraft.api.wands.WandTriggerRegistry` — `common/src/main/java/thaumcraft/api/wands/WandTriggerRegistry.java:21` | Partial: `common/src/test/java/thaumcraft/api/WandApiTest.java` |

Important holes even among partially covered types: mutation of the first cached recipe-key answer; cache-list mutation; metadata deprecation/replacement parity; callback exception behavior; null overload parity; invalid names; recipe-input/output ownership; TC6 surfaces not dispatched by gameplay; arbitrary infusion result-patch rejection. Existing tests often prove a happy path, not contract stability.

## Safe changes and verification

- Retained all six metadata-shaped wand trigger signatures and bodies. Added `@Deprecated` plus `@deprecated` replacement guidance to each; addons still link, but compilers now warn about ignored metadata.
- Added four missing Javadocs covering whole-block versus exact-state queries, null handling, mod filtering, dispatch precedence, block-coordinate units, passed-through callback arguments, callback exceptions and unspecified cross-mod order. No invented thread or success guarantees.
- Added `common/src/test/java/thaumcraft/api/WandTriggerDeprecationTest.java`: two tests assert all six metadata overloads remain deprecated and state-aware lookup replacements remain supported.
- **PASS:** Java 21 `javac --release 21 -proc:none` on changed WandTriggerRegistry plus actual IWandTriggerManager, using the remapped Minecraft 1.21.1 merged jar, requested `common/build/classes/java/main` classpath entry and 312 cached dependency jars (313 jars including Minecraft). `common/build/classes/java/main` does not exist in this worktree; no other worktree or main checkout was used. These two real source files compile without those missing project classes. No stubs.
- **PASS:** compiled the new test with javac and ran it using JUnit Platform Launcher 1.11.4/Jupiter 5.11.4: **2 started, 2 successful, 0 failed**. The launcher helper/output live in ignored `.tmp`, not the deliverable.
- **PASS:** targeted `javadoc -quiet -Xdoclint:all,-missing` on WandTriggerRegistry and IWandTriggerManager, using the same real dependency classpath: exit 0, no diagnostics. Replacement links resolve. This does not validate Javadoc across the whole API.
- No Gradle, full build, full API compilation, runtime gameplay, client or multiplayer run was performed. No claim that all existing tests pass or that whole-project doclint passes.

## Summary

Counts are baseline review counts, so fixed items remain visible. Observations and inherited contracts are not silently converted into defects. The eight rows overlap by design.

| Checklist | Inspected | Findings / observations | Fixed |
|---|---:|---:|---:|
| 1. Public surface | 103 types + 998 members | 142 member + 22 type no-external-token observations; 3 minimality decisions | 0 access changes |
| 2. Documentation | 864 types/callables + package-info | 510 missing contracts + 33 existing-document issues; 22 inherited exclusions | 4 missing method Javadocs |
| 3. Null/error | 761 callables | 20 inconsistency groups | 0 (compatibility decisions) |
| 4. Exposure | 93 files / 998 members | 28 groups; 3 blockers | 0 (ownership decisions) |
| 5. Deprecation | 1,101 declarations | 23 members/types | 6 ignored-metadata overloads |
| 6. Naming | 688 methods + 340 type/field names | 10 issue groups; 108 duplicate groups reviewed | 0 public renames |
| 7. Legacy shapes | 93 files / 1,101 declarations | 17 outstanding groups + 1 documented migration boundary | 6 overload warnings, not semantic redesign |
| 8. Tests | 103 public types | 24 uncovered; others only partially covered | 2 new deprecation tests pass |

Human decisions: choose a supported TC4-versus-TC6 extension boundary; decide ownership/snapshot semantics for old mutable registries and recipe views; choose fail-fast versus forgiving null/error behavior; identify the canonical facade and unit conventions; approve any access narrowing only against a real published manifest/addon compatibility policy. Shared-cache corruption and inert advertised hooks should be addressed before promoting this as a stable addon contract.

## Appendix — full declared-public-member inspection ledger

This ledger includes every field, enum constant, explicit constructor and method, not inherited members. External evidence is the count of different-package production files containing the member name (not resolved invocations). `0` entries are all listed in section 1; field-name collisions and overloads require a follow-up semantic call graph before deletion. Local Javadoc presence is not contract sufficiency. The type/test table above inventories every public type.

| Baseline declaration | Kind | External token files | Local Javadoc |
|---|---|---:|---|
| `common/src/main/java/thaumcraft/api/BlockCoordinates.java:24` — `BlockCoordinates.int x` | field | 675 | yes |
| `common/src/main/java/thaumcraft/api/BlockCoordinates.java:27` — `BlockCoordinates.int y` | field | 641 | yes |
| `common/src/main/java/thaumcraft/api/BlockCoordinates.java:30` — `BlockCoordinates.int z` | field | 610 | yes |
| `common/src/main/java/thaumcraft/api/BlockCoordinates.java:32` — `BlockCoordinates.<init>()` | constructor | 10 | no |
| `common/src/main/java/thaumcraft/api/BlockCoordinates.java:34` — `BlockCoordinates.<init>(int, int, int)` | constructor | 10 | no |
| `common/src/main/java/thaumcraft/api/BlockCoordinates.java:40` — `BlockCoordinates.<init>(BlockEntity)` | constructor | 10 | no |
| `common/src/main/java/thaumcraft/api/BlockCoordinates.java:47` — `BlockCoordinates.<init>(BlockCoordinates)` | constructor | 10 | no |
| `common/src/main/java/thaumcraft/api/BlockCoordinates.java:53` — `BlockCoordinates.equals(Object)` | method | 345 | no |
| `common/src/main/java/thaumcraft/api/BlockCoordinates.java:65` — `BlockCoordinates.hashCode()` | method | 40 | yes |
| `common/src/main/java/thaumcraft/api/BlockCoordinates.java:75` — `BlockCoordinates.compareWorldCoordinate(BlockCoordinates)` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/BlockCoordinates.java:80` — `BlockCoordinates.set(int, int, int, int)` | method | 256 | yes |
| `common/src/main/java/thaumcraft/api/BlockCoordinates.java:92` — `BlockCoordinates.getDistanceSquared(int, int, int)` | method | 2 | yes |
| `common/src/main/java/thaumcraft/api/BlockCoordinates.java:100` — `BlockCoordinates.getDistanceSquaredToWorldCoordinates(BlockCoordinates)` | method | 2 | yes |
| `common/src/main/java/thaumcraft/api/BlockCoordinates.java:104` — `BlockCoordinates.compareTo(BlockCoordinates)` | method | 6 | no |
| `common/src/main/java/thaumcraft/api/BlockCoordinates.java:109` — `BlockCoordinates.readNBT(CompoundTag)` | method | 3 | yes |
| `common/src/main/java/thaumcraft/api/BlockCoordinates.java:117` — `BlockCoordinates.writeNBT(CompoundTag)` | method | 3 | yes |
| `common/src/main/java/thaumcraft/api/IArchitect.java:34` — `IArchitect.EnumAxis.EnumAxis X` | field | 24 | no |
| `common/src/main/java/thaumcraft/api/IArchitect.java:35` — `IArchitect.EnumAxis.EnumAxis Y` | field | 27 | no |
| `common/src/main/java/thaumcraft/api/IArchitect.java:36` — `IArchitect.EnumAxis.EnumAxis Z` | field | 21 | no |
| `common/src/main/java/thaumcraft/api/IArchitect.java:51` — `IArchitect.getArchitectBlocks(ItemStack, Level, int, int, int, int, Player)` | method | 8 | yes |
| `common/src/main/java/thaumcraft/api/IArchitect.java:63` — `IArchitect.showAxis(ItemStack, Level, Player, int, EnumAxis)` | method | 9 | yes |
| `common/src/main/java/thaumcraft/api/IArchitect.java:71` — `IArchitect.getMaxAreaSize(ItemStack)` | method | 10 | yes |
| `common/src/main/java/thaumcraft/api/IArchitect.java:74` — `IArchitect.getAxisModes()` | method | 3 | yes |
| `common/src/main/java/thaumcraft/api/IGoggles.java:8` — `IGoggles.showIngamePopups(ItemStack, LivingEntity)` | method | 8 | no |
| `common/src/main/java/thaumcraft/api/IRepairableExtended.java:8` — `IRepairableExtended.doRepair(ItemStack, Player, int)` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/IRunicArmor.java:6` — `IRunicArmor.getRunicCharge(ItemStack)` | method | 42 | no |
| `common/src/main/java/thaumcraft/api/IVisDiscountGear.java:9` — `IVisDiscountGear.getVisDiscount(ItemStack, Player, Aspect)` | method | 20 | yes |
| `common/src/main/java/thaumcraft/api/IWarpingGear.java:11` — `IWarpingGear.getWarp(ItemStack, Player)` | method | 33 | no |
| `common/src/main/java/thaumcraft/api/ItemApi.java:30` — `ItemApi.getItem(String, int)` | method | 394 | yes |
| `common/src/main/java/thaumcraft/api/ItemApi.java:42` — `ItemApi.getBlock(String, int)` | method | 274 | yes |
| `common/src/main/java/thaumcraft/api/ItemApi.java:66` — `ItemApi.getBlockItem(String)` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/ItemRunic.java:15` — `ItemRunic.<init>(int)` | constructor | 3 | no |
| `common/src/main/java/thaumcraft/api/ItemRunic.java:20` — `ItemRunic.<init>(int, Item.Properties)` | constructor | 3 | no |
| `common/src/main/java/thaumcraft/api/ItemRunic.java:25` — `ItemRunic.getRunicCharge(ItemStack)` | method | 42 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:57` — `ThaumcraftApi.Tier toolMatThaumium` | field | 1 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:58` — `ThaumcraftApi.Tier toolMatVoid` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:59` — `ThaumcraftApi.Tier toolMatElemental` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:61` — `ThaumcraftApi.armorMatThaumium()` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:62` — `ThaumcraftApi.armorMatThaumiumFortress()` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:63` — `ThaumcraftApi.armorMatVoid()` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:69` — `ThaumcraftApi.armorMatSpecial()` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:75` — `ThaumcraftApi.armorMatVoidFortress()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:86` — `ThaumcraftApi.int enchantFrugal` | field | 0 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:87` — `ThaumcraftApi.int enchantPotency` | field | 0 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:88` — `ThaumcraftApi.int enchantWandFortune` | field | 0 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:89` — `ThaumcraftApi.int enchantHaste` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:90` — `ThaumcraftApi.int enchantRepair` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:92` — `ThaumcraftApi.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANTMENT_FRUGAL` | field | 0 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:94` — `ThaumcraftApi.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANTMENT_POTENCY` | field | 0 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:96` — `ThaumcraftApi.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANTMENT_WAND_FORTUNE` | field | 0 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:98` — `ThaumcraftApi.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANTMENT_HASTE` | field | 0 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:100` — `ThaumcraftApi.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANTMENT_REPAIR` | field | 0 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:102` — `ThaumcraftApi.ArrayList<Block> portableHoleBlackList` | field | 2 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:104` — `ThaumcraftApi.IInternalMethodHandler internalMethods` | field | 3 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:105` — `ThaumcraftApi.ArrayList<IScanEventHandler> scanEventhandlers` | field | 2 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:106` — `ThaumcraftApi.ArrayList<EntityTags> scanEntities` | field | 3 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:107` — `ThaumcraftApi.ConcurrentHashMap<List<?>, AspectList> objectTags` | field | 7 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:108` — `ThaumcraftApi.ConcurrentHashMap<List<?>, int[]> groupedObjectTags` | field | 3 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:135` — `ThaumcraftApi.addSmeltingBonus(ItemStack, ItemStack)` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:145` — `ThaumcraftApi.addSmeltingBonus(String, ItemStack)` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:158` — `ThaumcraftApi.getSmeltingBonus(ItemStack)` | method | 2 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:195` — `ThaumcraftApi.reloadOriginalSmeltingBonuses()` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:230` — `ThaumcraftApi.getCraftingRecipes()` | method | 5 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:237` — `ThaumcraftApi.installRecipeManager(RecipeManager, net.minecraft.core.HolderLookup.Provider)` | method | 2 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:242` — `ThaumcraftApi.addArcaneCraftingRecipe(String, ItemStack, AspectList, Object[])` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:248` — `ThaumcraftApi.addShapelessArcaneCraftingRecipe(String, ItemStack, AspectList, Object[])` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:255` — `ThaumcraftApi.addInfusionCraftingRecipe(String, Object, int, AspectList, ItemStack, ItemStack[])` | method | 2 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:262` — `ThaumcraftApi.addInfusionEnchantmentRecipe(String, net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment>, int, AspectList, ItemStack[])` | method | 2 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:269` — `ThaumcraftApi.getInfusionRecipe(ItemStack)` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:285` — `ThaumcraftApi.addCrucibleRecipe(String, ItemStack, Object, AspectList)` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:291` — `ThaumcraftApi.getCrucibleRecipe(ItemStack)` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:299` — `ThaumcraftApi.getCrucibleRecipeFromHash(int)` | method | 2 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:315` — `ThaumcraftApi.getCraftingRecipeKey(net.minecraft.world.entity.player.Player, ItemStack)` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:368` — `ThaumcraftApi.addWarpToItem(ItemStack, int)` | method | 5 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:373` — `ThaumcraftApi.addWarpToResearch(String, int)` | method | 6 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:378` — `ThaumcraftApi.getWarp(Object)` | method | 33 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:403` — `ThaumcraftApi.getWarpForResearch(ResearchDefinition)` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:408` — `ThaumcraftApi.refreshPortableHoleBlackList()` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:414` — `ThaumcraftApi.registerScanEventhandler(IScanEventHandler)` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:414` — `ThaumcraftApi.registerEntityTag(String, AspectList, EntityTagsNBT[])` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:420` — `ThaumcraftApi.registerObjectTag(ItemStack, AspectList)` | method | 6 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:424` — `ThaumcraftApi.registerObjectTag(Item, int, AspectList)` | method | 6 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:427` — `ThaumcraftApi.registerObjectTag(ResourceLocation, int, AspectList)` | method | 6 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:431` — `ThaumcraftApi.registerObjectTag(ItemStack, int[], AspectList)` | method | 6 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:436` — `ThaumcraftApi.registerObjectTag(TagKey<Item>, AspectList)` | method | 6 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:441` — `ThaumcraftApi.registerObjectTag(String, AspectList)` | method | 6 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:447` — `ThaumcraftApi.exists(Item, int)` | method | 10 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:451` — `ThaumcraftApi.registerComplexObjectTag(ItemStack, AspectList)` | method | 3 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:454` — `ThaumcraftApi.registerComplexObjectTag(ItemStack, AspectList, Level)` | method | 3 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:458` — `ThaumcraftApi.registerComplexObjectTag(Item, int, AspectList, Level)` | method | 3 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:472` — `ThaumcraftApi.addLootBagItem(ItemStack, int, int[])` | method | 2 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:490` — `ThaumcraftApi.EntityTags.String entityName` | field | 8 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:491` — `ThaumcraftApi.EntityTags.EntityTagsNBT[] nbts` | field | 2 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:492` — `ThaumcraftApi.EntityTags.AspectList aspects` | field | 195 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:493` — `ThaumcraftApi.EntityTags.<init>(String, AspectList, EntityTagsNBT[])` | constructor | 3 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:498` — `ThaumcraftApi.EntityTagsNBT.String name` | field | 124 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:499` — `ThaumcraftApi.EntityTagsNBT.Object value` | field | 269 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:500` — `ThaumcraftApi.EntityTagsNBT.<init>(String, Object)` | constructor | 4 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:49` — `ThaumcraftApiHelper.cullTags(AspectList)` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:99` — `ThaumcraftApiHelper.areItemsEqual(ItemStack, ItemStack)` | method | 4 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:106` — `ThaumcraftApiHelper.isResearchComplete(String, String)` | method | 77 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:111` — `ThaumcraftApiHelper.isResearchComplete(Player, String)` | method | 77 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:116` — `ThaumcraftApiHelper.hasDiscoveredAspect(String, Aspect)` | method | 40 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:120` — `ThaumcraftApiHelper.getDiscoveredAspects(String)` | method | 7 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:133` — `ThaumcraftApiHelper.getStackInRowAndColumn(Object, int, int)` | method | 11 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:137` — `ThaumcraftApiHelper.getObjectAspects(ItemStack)` | method | 12 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:143` — `ThaumcraftApiHelper.getObjectAspects(ItemStack, Level)` | method | 12 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:148` — `ThaumcraftApiHelper.getBonusObjectTags(ItemStack, AspectList)` | method | 7 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:153` — `ThaumcraftApiHelper.generateTags(Item, int)` | method | 8 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:158` — `ThaumcraftApiHelper.containsMatch(boolean, ItemStack[], ItemStack[])` | method | 7 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:169` — `ThaumcraftApiHelper.areItemStackTagsEqualForCrafting(ItemStack, ItemStack)` | method | 9 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:181` — `ThaumcraftApiHelper.itemMatches(ItemStack, ItemStack, boolean)` | method | 3 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:189` — `ThaumcraftApiHelper.getConnectableTile(Level, int, int, int, Direction)` | method | 15 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:193` — `ThaumcraftApiHelper.getConnectableTile(BlockGetter, int, int, int, Direction)` | method | 15 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:200` — `ThaumcraftApiHelper.getAllAspects(int)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:209` — `ThaumcraftApiHelper.getAllCompoundAspects(int)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:218` — `ThaumcraftApiHelper.consumeVisFromWand(ItemStack, Player, AspectList, boolean, boolean)` | method | 7 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:222` — `ThaumcraftApiHelper.consumeVisFromWandCrafting(ItemStack, Player, AspectList, boolean)` | method | 7 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:226` — `ThaumcraftApiHelper.consumeVisFromInventory(Player, AspectList)` | method | 13 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:230` — `ThaumcraftApiHelper.addWarpToPlayer(Player, int, boolean)` | method | 16 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:234` — `ThaumcraftApiHelper.addStickyWarpToPlayer(Player, int)` | method | 14 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:244` — `ThaumcraftApiHelper.rayTraceIgnoringSource(Level, Vec3, Vec3, boolean, boolean, boolean)` | method | 2 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftMaterials.java:24` — `ThaumcraftMaterials.Tier TOOLMAT_THAUMIUM` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftMaterials.java:25` — `ThaumcraftMaterials.Tier TOOLMAT_VOID` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftMaterials.java:26` — `ThaumcraftMaterials.Tier TOOLMAT_ELEMENTAL` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/ThaumcraftMaterials.java:29` — `ThaumcraftMaterials.ArmorMaterial ARMORMAT_SPECIAL` | field | 0 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftMaterials.java:39` — `ThaumcraftMaterials.ArmorMaterial ARMORMAT_VOID_FORTRESS` | field | 0 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftMaterials.java:52` — `ThaumcraftMaterials.armorMatThaumium()` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftMaterials.java:54` — `ThaumcraftMaterials.armorMatThaumiumFortress()` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftMaterials.java:56` — `ThaumcraftMaterials.armorMatVoid()` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftMaterials.java:58` — `ThaumcraftMaterials.armorMatVoidRobe()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/ThaumcraftMaterials.java:60` — `ThaumcraftMaterials.layer(String)` | method | 36 | yes |
| `common/src/main/java/thaumcraft/api/TileThaumcraft.java:21` — `TileThaumcraft.<init>(BlockEntityType<?>, BlockPos, BlockState)` | constructor | 53 | no |
| `common/src/main/java/thaumcraft/api/TileThaumcraft.java:25` — `TileThaumcraft.loadAdditional(CompoundTag, HolderLookup.Provider)` | method | 60 | no |
| `common/src/main/java/thaumcraft/api/TileThaumcraft.java:31` — `TileThaumcraft.readCustomNBT(CompoundTag)` | method | 52 | yes |
| `common/src/main/java/thaumcraft/api/TileThaumcraft.java:39` — `TileThaumcraft.writeCustomNBT(CompoundTag)` | method | 52 | yes |
| `common/src/main/java/thaumcraft/api/TileThaumcraft.java:41` — `TileThaumcraft.getUpdatePacket()` | method | 53 | no |
| `common/src/main/java/thaumcraft/api/WorldCoordinates.java:17` — `WorldCoordinates.int x` | field | 675 | no |
| `common/src/main/java/thaumcraft/api/WorldCoordinates.java:17` — `WorldCoordinates.int y` | field | 641 | no |
| `common/src/main/java/thaumcraft/api/WorldCoordinates.java:17` — `WorldCoordinates.int z` | field | 610 | no |
| `common/src/main/java/thaumcraft/api/WorldCoordinates.java:18` — `WorldCoordinates.ResourceKey<Level> dimension` | field | 117 | no |
| `common/src/main/java/thaumcraft/api/WorldCoordinates.java:20` — `WorldCoordinates.<init>(ResourceKey<Level>, BlockPos)` | constructor | 11 | no |
| `common/src/main/java/thaumcraft/api/WorldCoordinates.java:23` — `WorldCoordinates.<init>(int, int, int, ResourceKey<Level>)` | constructor | 11 | no |
| `common/src/main/java/thaumcraft/api/WorldCoordinates.java:29` — `WorldCoordinates.<init>(BlockEntity)` | constructor | 11 | no |
| `common/src/main/java/thaumcraft/api/WorldCoordinates.java:32` — `WorldCoordinates.<init>(WorldCoordinates)` | constructor | 11 | no |
| `common/src/main/java/thaumcraft/api/WorldCoordinates.java:33` — `WorldCoordinates.pos()` | method | 716 | no |
| `common/src/main/java/thaumcraft/api/WorldCoordinates.java:34` — `WorldCoordinates.dimension()` | method | 117 | no |
| `common/src/main/java/thaumcraft/api/WorldCoordinates.java:35` — `WorldCoordinates.isCanonicalDimension(ResourceKey<Level>)` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/WorldCoordinates.java:39` — `WorldCoordinates.getDistanceSquared(int, int, int)` | method | 2 | no |
| `common/src/main/java/thaumcraft/api/WorldCoordinates.java:43` — `WorldCoordinates.getDistanceSquaredToWorldCoordinates(WorldCoordinates)` | method | 2 | no |
| `common/src/main/java/thaumcraft/api/WorldCoordinates.java:46` — `WorldCoordinates.equals(Object)` | method | 345 | no |
| `common/src/main/java/thaumcraft/api/WorldCoordinates.java:49` — `WorldCoordinates.hashCode()` | method | 40 | no |
| `common/src/main/java/thaumcraft/api/WorldCoordinates.java:50` — `WorldCoordinates.compareTo(WorldCoordinates)` | method | 6 | no |
| `common/src/main/java/thaumcraft/api/WorldCoordinates.java:56` — `WorldCoordinates.compareWorldCoordinate(WorldCoordinates)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/WorldCoordinates.java:57` — `WorldCoordinates.writeNBT(CompoundTag)` | method | 3 | no |
| `common/src/main/java/thaumcraft/api/WorldCoordinates.java:62` — `WorldCoordinates.readNBT(CompoundTag)` | method | 3 | yes |
| `common/src/main/java/thaumcraft/api/WorldCoordinates.java:70` — `WorldCoordinates.toString()` | method | 132 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:13` — `Aspect.LinkedHashMap<String, Aspect> aspects` | field | 179 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:15` — `Aspect.Aspect AIR` | field | 153 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:16` — `Aspect.Aspect EARTH` | field | 66 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:17` — `Aspect.Aspect FIRE` | field | 79 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:18` — `Aspect.Aspect WATER` | field | 102 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:19` — `Aspect.Aspect ORDER` | field | 59 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:20` — `Aspect.Aspect ENTROPY` | field | 69 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:21` — `Aspect.Aspect VOID` | field | 24 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:22` — `Aspect.Aspect LIGHT` | field | 16 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:23` — `Aspect.Aspect WEATHER` | field | 2 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:24` — `Aspect.Aspect MOTION` | field | 11 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:25` — `Aspect.Aspect COLD` | field | 8 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:26` — `Aspect.Aspect CRYSTAL` | field | 23 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:27` — `Aspect.Aspect LIFE` | field | 15 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:28` — `Aspect.Aspect POISON` | field | 20 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:29` — `Aspect.Aspect ENERGY` | field | 18 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:30` — `Aspect.Aspect EXCHANGE` | field | 16 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:31` — `Aspect.Aspect METAL` | field | 26 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:32` — `Aspect.Aspect DEATH` | field | 17 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:33` — `Aspect.Aspect FLIGHT` | field | 7 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:34` — `Aspect.Aspect DARKNESS` | field | 17 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:35` — `Aspect.Aspect SOUL` | field | 8 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:36` — `Aspect.Aspect HEAL` | field | 11 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:37` — `Aspect.Aspect TRAVEL` | field | 11 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:38` — `Aspect.Aspect ELDRITCH` | field | 37 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:39` — `Aspect.Aspect MAGIC` | field | 28 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:40` — `Aspect.Aspect AURA` | field | 11 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:41` — `Aspect.Aspect TAINT` | field | 28 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:42` — `Aspect.Aspect SLIME` | field | 7 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:43` — `Aspect.Aspect PLANT` | field | 31 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:44` — `Aspect.Aspect TREE` | field | 6 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:45` — `Aspect.Aspect BEAST` | field | 11 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:46` — `Aspect.Aspect FLESH` | field | 12 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:47` — `Aspect.Aspect UNDEAD` | field | 16 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:48` — `Aspect.Aspect MIND` | field | 13 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:49` — `Aspect.Aspect SENSES` | field | 12 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:50` — `Aspect.Aspect MAN` | field | 6 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:51` — `Aspect.Aspect CROP` | field | 3 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:52` — `Aspect.Aspect MINE` | field | 9 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:53` — `Aspect.Aspect TOOL` | field | 21 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:54` — `Aspect.Aspect HARVEST` | field | 14 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:55` — `Aspect.Aspect WEAPON` | field | 13 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:56` — `Aspect.Aspect ARMOR` | field | 29 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:57` — `Aspect.Aspect HUNGER` | field | 27 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:58` — `Aspect.Aspect GREED` | field | 20 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:59` — `Aspect.Aspect CRAFT` | field | 11 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:60` — `Aspect.Aspect CLOTH` | field | 10 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:61` — `Aspect.Aspect MECHANISM` | field | 7 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:62` — `Aspect.Aspect TRAP` | field | 9 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:75` — `Aspect.<init>(String, int, Aspect[], ResourceLocation, int)` | constructor | 360 | yes |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:86` — `Aspect.<init>(String, int, Aspect[], int)` | constructor | 360 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:91` — `Aspect.<init>(String, int, Aspect[])` | constructor | 360 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:95` — `Aspect.<init>(String, int, String, int)` | constructor | 360 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:100` — `Aspect.getTag()` | method | 133 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:101` — `Aspect.getColor()` | method | 84 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:102` — `Aspect.getName()` | method | 181 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:103` — `Aspect.getComponents()` | method | 31 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:104` — `Aspect.getImage()` | method | 22 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:105` — `Aspect.getBlend()` | method | 7 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:106` — `Aspect.getChatcolor()` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:112` — `Aspect.getLocalizedDescription()` | method | 4 | yes |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:118` — `Aspect.isPrimal()` | method | 37 | yes |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:121` — `Aspect.setTag(String)` | method | 37 | yes |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:124` — `Aspect.setComponents(Aspect[])` | method | 2 | yes |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:127` — `Aspect.setBlend(int)` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:130` — `Aspect.setChatcolor(String)` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:132` — `Aspect.getAspect(String)` | method | 135 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:134` — `Aspect.getPrimalAspects()` | method | 35 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:140` — `Aspect.getCompoundAspects()` | method | 5 | no |
| `common/src/main/java/thaumcraft/api/aspects/Aspect.java:146` — `Aspect.getCombinationResult(Aspect, Aspect)` | method | 5 | no |
| `common/src/main/java/thaumcraft/api/aspects/AspectEventProxy.java:14` — `AspectEventProxy.registerObjectTag(ItemStack, AspectList)` | method | 6 | yes |
| `common/src/main/java/thaumcraft/api/aspects/AspectEventProxy.java:19` — `AspectEventProxy.registerObjectTag(ItemStack, int[], AspectList)` | method | 6 | yes |
| `common/src/main/java/thaumcraft/api/aspects/AspectEventProxy.java:29` — `AspectEventProxy.registerObjectTag(String, AspectList)` | method | 6 | yes |
| `common/src/main/java/thaumcraft/api/aspects/AspectEventProxy.java:34` — `AspectEventProxy.registerComplexObjectTag(ItemStack, AspectList)` | method | 3 | yes |
| `common/src/main/java/thaumcraft/api/aspects/AspectEventProxy.java:44` — `AspectEventProxy.registerComplexObjectTag(String, AspectList)` | method | 3 | yes |
| `common/src/main/java/thaumcraft/api/aspects/AspectHelper.java:16` — `AspectHelper.getObjectAspects(ItemStack)` | method | 11 | yes |
| `common/src/main/java/thaumcraft/api/aspects/AspectList.java:16` — `AspectList.LinkedHashMap<Aspect, Integer> aspects` | field | 179 | no |
| `common/src/main/java/thaumcraft/api/aspects/AspectList.java:24` — `AspectList.<init>(ItemStack)` | constructor | 233 | yes |
| `common/src/main/java/thaumcraft/api/aspects/AspectList.java:37` — `AspectList.<init>()` | constructor | 233 | no |
| `common/src/main/java/thaumcraft/api/aspects/AspectList.java:40` — `AspectList.copy()` | method | 304 | no |
| `common/src/main/java/thaumcraft/api/aspects/AspectList.java:46` — `AspectList.size()` | method | 406 | no |
| `common/src/main/java/thaumcraft/api/aspects/AspectList.java:47` — `AspectList.visSize()` | method | 27 | no |
| `common/src/main/java/thaumcraft/api/aspects/AspectList.java:48` — `AspectList.getAmount(Aspect)` | method | 150 | no |
| `common/src/main/java/thaumcraft/api/aspects/AspectList.java:49` — `AspectList.getAspects()` | method | 154 | no |
| `common/src/main/java/thaumcraft/api/aspects/AspectList.java:50` — `AspectList.getPrimalAspects()` | method | 35 | no |
| `common/src/main/java/thaumcraft/api/aspects/AspectList.java:52` — `AspectList.getAspectsSorted()` | method | 31 | no |
| `common/src/main/java/thaumcraft/api/aspects/AspectList.java:63` — `AspectList.getAspectsSortedAmount()` | method | 9 | yes |
| `common/src/main/java/thaumcraft/api/aspects/AspectList.java:88` — `AspectList.getAspectsSortedByAmount()` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/aspects/AspectList.java:102` — `AspectList.add(Aspect, int)` | method | 700 | yes |
| `common/src/main/java/thaumcraft/api/aspects/AspectList.java:107` — `AspectList.add(AspectList)` | method | 700 | no |
| `common/src/main/java/thaumcraft/api/aspects/AspectList.java:112` — `AspectList.merge(Aspect, int)` | method | 50 | no |
| `common/src/main/java/thaumcraft/api/aspects/AspectList.java:117` — `AspectList.merge(AspectList)` | method | 50 | no |
| `common/src/main/java/thaumcraft/api/aspects/AspectList.java:122` — `AspectList.reduce(Aspect, int)` | method | 14 | no |
| `common/src/main/java/thaumcraft/api/aspects/AspectList.java:128` — `AspectList.remove(Aspect, int)` | method | 229 | no |
| `common/src/main/java/thaumcraft/api/aspects/AspectList.java:135` — `AspectList.remove(Aspect)` | method | 229 | no |
| `common/src/main/java/thaumcraft/api/aspects/AspectList.java:140` — `AspectList.reduceToPrimals(boolean)` | method | 15 | no |
| `common/src/main/java/thaumcraft/api/aspects/AspectList.java:154` — `AspectList.writeToNBT(CompoundTag)` | method | 56 | no |
| `common/src/main/java/thaumcraft/api/aspects/AspectList.java:156` — `AspectList.writeToNBT(CompoundTag, String)` | method | 56 | no |
| `common/src/main/java/thaumcraft/api/aspects/AspectList.java:167` — `AspectList.readFromNBT(CompoundTag)` | method | 41 | no |
| `common/src/main/java/thaumcraft/api/aspects/AspectList.java:169` — `AspectList.readFromNBT(CompoundTag, String)` | method | 41 | no |
| `common/src/main/java/thaumcraft/api/aspects/AspectRegistryEvent.java:17` — `AspectRegistryEvent.AspectEventProxy register` | field | 190 | yes |
| `common/src/main/java/thaumcraft/api/aspects/AspectRegistryEvent.java:22` — `AspectRegistryEvent.addListener(Consumer<AspectRegistryEvent>)` | method | 13 | yes |
| `common/src/main/java/thaumcraft/api/aspects/AspectRegistryEvent.java:27` — `AspectRegistryEvent.removeListener(Consumer<AspectRegistryEvent>)` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/aspects/AspectRegistryEvent.java:32` — `AspectRegistryEvent.listenerCount()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/aspects/AspectRegistryEvent.java:35` — `AspectRegistryEvent.post()` | method | 10 | yes |
| `common/src/main/java/thaumcraft/api/aspects/AspectSourceHelper.java:34` — `AspectSourceHelper.drainEssentia(BlockEntity, Aspect, Direction, int)` | method | 5 | yes |
| `common/src/main/java/thaumcraft/api/aspects/AspectSourceHelper.java:60` — `AspectSourceHelper.findEssentia(BlockEntity, Aspect, Direction, int)` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:5` — `IAspectContainer.getAspects()` | method | 154 | no |
| `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:6` — `IAspectContainer.setAspects(AspectList)` | method | 72 | no |
| `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:7` — `IAspectContainer.doesContainerAccept(Aspect)` | method | 39 | no |
| `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:8` — `IAspectContainer.addToContainer(Aspect, int)` | method | 42 | no |
| `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:9` — `IAspectContainer.takeFromContainer(Aspect, int)` | method | 43 | no |
| `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:10` — `IAspectContainer.takeFromContainer(AspectList)` | method | 43 | no |
| `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:11` — `IAspectContainer.doesContainerContainAmount(Aspect, int)` | method | 37 | no |
| `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:12` — `IAspectContainer.doesContainerContain(AspectList)` | method | 36 | no |
| `common/src/main/java/thaumcraft/api/aspects/IAspectContainer.java:13` — `IAspectContainer.containerContains(Aspect)` | method | 40 | no |
| `common/src/main/java/thaumcraft/api/aspects/IEssentiaContainerItem.java:7` — `IEssentiaContainerItem.getAspects(ItemStack)` | method | 154 | no |
| `common/src/main/java/thaumcraft/api/aspects/IEssentiaContainerItem.java:8` — `IEssentiaContainerItem.setAspects(ItemStack, AspectList)` | method | 72 | no |
| `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:7` — `IEssentiaTransport.isConnectable(Direction)` | method | 38 | no |
| `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:8` — `IEssentiaTransport.canInputFrom(Direction)` | method | 34 | no |
| `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:9` — `IEssentiaTransport.canOutputTo(Direction)` | method | 35 | no |
| `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:10` — `IEssentiaTransport.setSuction(Aspect, int)` | method | 30 | no |
| `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:11` — `IEssentiaTransport.getSuctionType(Direction)` | method | 35 | no |
| `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:12` — `IEssentiaTransport.getSuctionAmount(Direction)` | method | 37 | no |
| `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:13` — `IEssentiaTransport.takeEssentia(Aspect, int, Direction)` | method | 31 | no |
| `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:14` — `IEssentiaTransport.addEssentia(Aspect, int, Direction)` | method | 31 | no |
| `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:15` — `IEssentiaTransport.getEssentiaType(Direction)` | method | 34 | no |
| `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:16` — `IEssentiaTransport.getEssentiaAmount(Direction)` | method | 36 | no |
| `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:17` — `IEssentiaTransport.getMinimumSuction()` | method | 31 | no |
| `common/src/main/java/thaumcraft/api/aspects/IEssentiaTransport.java:18` — `IEssentiaTransport.renderExtendedTube()` | method | 32 | no |
| `common/src/main/java/thaumcraft/api/aura/AuraHelper.java:22` — `AuraHelper.node(Level, BlockPos)` | method | 47 | yes |
| `common/src/main/java/thaumcraft/api/aura/AuraHelper.java:28` — `AuraHelper.getVis(Level, BlockPos)` | method | 21 | yes |
| `common/src/main/java/thaumcraft/api/aura/AuraHelper.java:37` — `AuraHelper.getVis(Level, BlockPos, Aspect)` | method | 21 | yes |
| `common/src/main/java/thaumcraft/api/aura/AuraHelper.java:57` — `AuraHelper.drainVis(Level, BlockPos, int, boolean)` | method | 17 | yes |
| `common/src/main/java/thaumcraft/api/aura/AuraHelper.java:63` — `AuraHelper.addVis(Level, BlockPos, AspectList)` | method | 13 | yes |
| `common/src/main/java/thaumcraft/api/baubles/BaubleType.java:4` — `BaubleType.BaubleType AMULET` | field | 10 | no |
| `common/src/main/java/thaumcraft/api/baubles/BaubleType.java:4` — `BaubleType.BaubleType RING` | field | 12 | no |
| `common/src/main/java/thaumcraft/api/baubles/BaubleType.java:4` — `BaubleType.BaubleType BELT` | field | 11 | no |
| `common/src/main/java/thaumcraft/api/baubles/IBauble.java:8` — `IBauble.getBaubleType(ItemStack)` | method | 17 | no |
| `common/src/main/java/thaumcraft/api/baubles/IBauble.java:9` — `IBauble.onWornTick(ItemStack, LivingEntity)` | method | 10 | no |
| `common/src/main/java/thaumcraft/api/baubles/IBauble.java:10` — `IBauble.onEquipped(ItemStack, LivingEntity)` | method | 9 | no |
| `common/src/main/java/thaumcraft/api/baubles/IBauble.java:11` — `IBauble.onUnequipped(ItemStack, LivingEntity)` | method | 10 | no |
| `common/src/main/java/thaumcraft/api/baubles/IBauble.java:12` — `IBauble.canEquip(ItemStack, LivingEntity)` | method | 9 | no |
| `common/src/main/java/thaumcraft/api/baubles/IBauble.java:13` — `IBauble.canUnequip(ItemStack, LivingEntity)` | method | 8 | no |
| `common/src/main/java/thaumcraft/api/blocks/BlocksTC.java:23` — `BlocksTC.id(String)` | method | 241 | yes |
| `common/src/main/java/thaumcraft/api/blocks/BlocksTC.java:29` — `BlocksTC.get(String)` | method | 853 | yes |
| `common/src/main/java/thaumcraft/api/blocks/BlocksTC.java:36` — `BlocksTC.require(String)` | method | 19 | yes |
| `common/src/main/java/thaumcraft/api/blocks/BlocksTC.java:43` — `BlocksTC.stack(String)` | method | 594 | yes |
| `common/src/main/java/thaumcraft/api/blocks/BlocksTC.java:49` — `BlocksTC.isRegistered(String)` | method | 2 | yes |
| `common/src/main/java/thaumcraft/api/blocks/BlocksTC.java:52` — `BlocksTC.names()` | method | 5 | yes |
| `common/src/main/java/thaumcraft/api/blocks/BlocksTC.java:61` — `BlocksTC.crystal(String)` | method | 13 | yes |
| `common/src/main/java/thaumcraft/api/blocks/BlocksTC.java:68` — `BlocksTC.jar()` | method | 31 | yes |
| `common/src/main/java/thaumcraft/api/blocks/BlocksTC.java:71` — `BlocksTC.voidJar()` | method | 3 | yes |
| `common/src/main/java/thaumcraft/api/blocks/BlocksTC.java:74` — `BlocksTC.auraNode()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/blocks/BlocksTC.java:77` — `BlocksTC.silverwoodKnot()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/blocks/BlocksTC.java:80` — `BlocksTC.table()` | method | 27 | yes |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:17` — `IPlayerKnowledge.EnumKnowledgeType.EnumKnowledgeType THEORY` | field | 0 | no |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:18` — `IPlayerKnowledge.EnumKnowledgeType.EnumKnowledgeType OBSERVATION` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:30` — `IPlayerKnowledge.EnumKnowledgeType.getProgression()` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:32` — `IPlayerKnowledge.EnumKnowledgeType.hasFields()` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:34` — `IPlayerKnowledge.EnumKnowledgeType.getAbbreviation()` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:38` — `IPlayerKnowledge.EnumResearchStatus.EnumResearchStatus UNKNOWN` | field | 7 | no |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:38` — `IPlayerKnowledge.EnumResearchStatus.EnumResearchStatus COMPLETE` | field | 4 | no |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:38` — `IPlayerKnowledge.EnumResearchStatus.EnumResearchStatus IN_PROGRESS` | field | 0 | no |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:41` — `IPlayerKnowledge.EnumResearchFlag.EnumResearchFlag PAGE` | field | 7 | no |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:41` — `IPlayerKnowledge.EnumResearchFlag.EnumResearchFlag RESEARCH` | field | 4 | no |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:41` — `IPlayerKnowledge.EnumResearchFlag.EnumResearchFlag POPUP` | field | 0 | no |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:44` — `IPlayerKnowledge.clear()` | method | 141 | yes |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:46` — `IPlayerKnowledge.getResearchStatus(String)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:48` — `IPlayerKnowledge.isResearchComplete(String)` | method | 78 | no |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:51` — `IPlayerKnowledge.isResearchKnown(String)` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:54` — `IPlayerKnowledge.getResearchStage(String)` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:56` — `IPlayerKnowledge.addResearch(String)` | method | 29 | no |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:58` — `IPlayerKnowledge.setResearchStage(String, int)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:60` — `IPlayerKnowledge.removeResearch(String)` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:63` — `IPlayerKnowledge.getResearchList()` | method | 5 | yes |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:65` — `IPlayerKnowledge.setResearchFlag(String, EnumResearchFlag)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:67` — `IPlayerKnowledge.clearResearchFlag(String, EnumResearchFlag)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:69` — `IPlayerKnowledge.hasResearchFlag(String, EnumResearchFlag)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:72` — `IPlayerKnowledge.addKnowledge(EnumKnowledgeType, ResearchCategory, int)` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:75` — `IPlayerKnowledge.getKnowledge(EnumKnowledgeType, ResearchCategory)` | method | 2 | yes |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:78` — `IPlayerKnowledge.getKnowledgeRaw(EnumKnowledgeType, ResearchCategory)` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/capabilities/IPlayerKnowledge.java:81` — `IPlayerKnowledge.sync(ServerPlayer)` | method | 26 | yes |
| `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:27` — `NativePlayerKnowledge.<init>(PlayerKnowledge)` | constructor | 0 | no |
| `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:32` — `NativePlayerKnowledge.knowledge()` | method | 71 | yes |
| `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:34` — `NativePlayerKnowledge.clear()` | method | 141 | no |
| `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:36` — `NativePlayerKnowledge.getResearchStatus(String)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:44` — `NativePlayerKnowledge.isResearchComplete(String)` | method | 78 | no |
| `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:48` — `NativePlayerKnowledge.isResearchKnown(String)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:52` — `NativePlayerKnowledge.getResearchStage(String)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:60` — `NativePlayerKnowledge.addResearch(String)` | method | 29 | no |
| `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:67` — `NativePlayerKnowledge.setResearchStage(String, int)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:73` — `NativePlayerKnowledge.removeResearch(String)` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:80` — `NativePlayerKnowledge.getResearchList()` | method | 5 | no |
| `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:82` — `NativePlayerKnowledge.setResearchFlag(String, EnumResearchFlag)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:88` — `NativePlayerKnowledge.clearResearchFlag(String, EnumResearchFlag)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:94` — `NativePlayerKnowledge.hasResearchFlag(String, EnumResearchFlag)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:100` — `NativePlayerKnowledge.addKnowledge(EnumKnowledgeType, ResearchCategory, int)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:108` — `NativePlayerKnowledge.getKnowledge(EnumKnowledgeType, ResearchCategory)` | method | 2 | no |
| `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:113` — `NativePlayerKnowledge.getKnowledgeRaw(EnumKnowledgeType, ResearchCategory)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/capabilities/NativePlayerKnowledge.java:120` — `NativePlayerKnowledge.sync(ServerPlayer)` | method | 26 | no |
| `common/src/main/java/thaumcraft/api/crafting/ArcaneRecipes.java:28` — `ArcaneRecipes.vis(AspectList)` | method | 35 | yes |
| `common/src/main/java/thaumcraft/api/crafting/ArcaneRecipes.java:41` — `ArcaneRecipes.aspectCosts(AspectList)` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/crafting/ArcaneRecipes.java:54` — `ArcaneRecipes.ingredient(Object)` | method | 13 | yes |
| `common/src/main/java/thaumcraft/api/crafting/ArcaneRecipes.java:73` — `ArcaneRecipes.registries()` | method | 100 | yes |
| `common/src/main/java/thaumcraft/api/crafting/ArcaneRecipes.java:78` — `ArcaneRecipes.rows(Object[], int[])` | method | 21 | yes |
| `common/src/main/java/thaumcraft/api/crafting/CrucibleRecipe.java:21` — `CrucibleRecipe.Object catalyst` | field | 12 | no |
| `common/src/main/java/thaumcraft/api/crafting/CrucibleRecipe.java:22` — `CrucibleRecipe.AspectList aspects` | field | 187 | no |
| `common/src/main/java/thaumcraft/api/crafting/CrucibleRecipe.java:23` — `CrucibleRecipe.String key` | field | 209 | no |
| `common/src/main/java/thaumcraft/api/crafting/CrucibleRecipe.java:24` — `CrucibleRecipe.int hash` | field | 11 | no |
| `common/src/main/java/thaumcraft/api/crafting/CrucibleRecipe.java:27` — `CrucibleRecipe.<init>(String, ItemStack, Object, AspectList)` | constructor | 23 | no |
| `common/src/main/java/thaumcraft/api/crafting/CrucibleRecipe.java:46` — `CrucibleRecipe.nativeRecipe()` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/crafting/CrucibleRecipe.java:51` — `CrucibleRecipe.of(thaumcraft.common.crafting.CrucibleRecipe)` | method | 335 | yes |
| `common/src/main/java/thaumcraft/api/crafting/CrucibleRecipe.java:57` — `CrucibleRecipe.matches(AspectList, ItemStack)` | method | 105 | yes |
| `common/src/main/java/thaumcraft/api/crafting/CrucibleRecipe.java:67` — `CrucibleRecipe.catalystMatches(ItemStack)` | method | 2 | no |
| `common/src/main/java/thaumcraft/api/crafting/CrucibleRecipe.java:73` — `CrucibleRecipe.removeMatching(AspectList)` | method | 3 | yes |
| `common/src/main/java/thaumcraft/api/crafting/CrucibleRecipe.java:82` — `CrucibleRecipe.getRecipeOutput()` | method | 17 | no |
| `common/src/main/java/thaumcraft/api/crafting/CrucibleRecipe.java:85` — `CrucibleRecipe.itemMatches(ItemStack, ItemStack, boolean)` | method | 3 | yes |
| `common/src/main/java/thaumcraft/api/crafting/IArcaneRecipe.java:16` — `IArcaneRecipe.matches(ArcaneRecipeInput, Level, Player)` | method | 105 | no |
| `common/src/main/java/thaumcraft/api/crafting/IArcaneRecipe.java:18` — `IArcaneRecipe.getCraftingResult(ArcaneRecipeInput)` | method | 6 | no |
| `common/src/main/java/thaumcraft/api/crafting/IArcaneRecipe.java:20` — `IArcaneRecipe.getRecipeSize()` | method | 2 | no |
| `common/src/main/java/thaumcraft/api/crafting/IArcaneRecipe.java:22` — `IArcaneRecipe.getRecipeOutput()` | method | 17 | no |
| `common/src/main/java/thaumcraft/api/crafting/IArcaneRecipe.java:24` — `IArcaneRecipe.getAspects()` | method | 147 | no |
| `common/src/main/java/thaumcraft/api/crafting/IArcaneRecipe.java:26` — `IArcaneRecipe.getAspects(ArcaneRecipeInput)` | method | 147 | no |
| `common/src/main/java/thaumcraft/api/crafting/IArcaneRecipe.java:28` — `IArcaneRecipe.getResearch()` | method | 28 | no |
| `common/src/main/java/thaumcraft/api/crafting/IInfusionStabiliser.java:8` — `IInfusionStabiliser.canStabaliseInfusion(Level, BlockPos)` | method | 7 | no |
| `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:26` — `InfusionEnchantmentRecipe.AspectList aspects` | field | 187 | no |
| `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:27` — `InfusionEnchantmentRecipe.String research` | field | 146 | no |
| `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:28` — `InfusionEnchantmentRecipe.ItemStack[] components` | field | 35 | no |
| `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:29` — `InfusionEnchantmentRecipe.Enchantment enchantment` | field | 29 | no |
| `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:30` — `InfusionEnchantmentRecipe.int recipeXP` | field | 3 | no |
| `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:31` — `InfusionEnchantmentRecipe.int instability` | field | 24 | no |
| `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:35` — `InfusionEnchantmentRecipe.<init>(String, Holder<Enchantment>, int, AspectList, ItemStack[])` | constructor | 13 | no |
| `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:52` — `InfusionEnchantmentRecipe.nativeRecipe()` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:59` — `InfusionEnchantmentRecipe.of(thaumcraft.common.crafting.InfusionEnchantmentRecipe)` | method | 335 | yes |
| `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:75` — `InfusionEnchantmentRecipe.enchantmentHolder()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:77` — `InfusionEnchantmentRecipe.matches(ArrayList<ItemStack>, ItemStack, Level, Player)` | method | 105 | no |
| `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:88` — `InfusionEnchantmentRecipe.getEnchantment()` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:90` — `InfusionEnchantmentRecipe.getAspects()` | method | 147 | no |
| `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:92` — `InfusionEnchantmentRecipe.getResearch()` | method | 28 | no |
| `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:95` — `InfusionEnchantmentRecipe.calcInstability(ItemStack)` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:104` — `InfusionEnchantmentRecipe.calcXP(ItemStack)` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:109` — `InfusionEnchantmentRecipe.getEssentiaMod(ItemStack)` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:34` — `InfusionRecipe.<init>(String, Object, int, AspectList, ItemStack, ItemStack[])` | constructor | 11 | no |
| `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:52` — `InfusionRecipe.nativeRecipe()` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:60` — `InfusionRecipe.of(InfusionStackRecipe)` | method | 335 | yes |
| `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:73` — `InfusionRecipe.matches(ArrayList<ItemStack>, ItemStack, Level, Player)` | method | 105 | no |
| `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:86` — `InfusionRecipe.areItemStacksEqual(ItemStack, ItemStack, boolean)` | method | 16 | yes |
| `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:95` — `InfusionRecipe.getRecipeOutput()` | method | 17 | no |
| `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:97` — `InfusionRecipe.getAspects()` | method | 147 | no |
| `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:99` — `InfusionRecipe.getInstability()` | method | 7 | no |
| `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:101` — `InfusionRecipe.getResearch()` | method | 28 | no |
| `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:103` — `InfusionRecipe.getRecipeInput()` | method | 5 | no |
| `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:105` — `InfusionRecipe.getComponents()` | method | 30 | no |
| `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:107` — `InfusionRecipe.getRecipeOutput(ItemStack)` | method | 17 | no |
| `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:109` — `InfusionRecipe.getAspects(ItemStack)` | method | 147 | no |
| `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:111` — `InfusionRecipe.getInstability(ItemStack)` | method | 7 | no |
| `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:128` — `InfusionRecipe.aspectCosts()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:131` — `InfusionRecipe.primaryAspect()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/crafting/NativeRecipes.java:42` — `NativeRecipes.manager()` | method | 21 | yes |
| `common/src/main/java/thaumcraft/api/crafting/NativeRecipes.java:45` — `NativeRecipes.registries()` | method | 100 | yes |
| `common/src/main/java/thaumcraft/api/crafting/NativeRecipes.java:48` — `NativeRecipes.install(RecipeManager, HolderLookup.Provider)` | method | 26 | yes |
| `common/src/main/java/thaumcraft/api/crafting/NativeRecipes.java:54` — `NativeRecipes.nativeRecipes()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/crafting/NativeRecipes.java:63` — `NativeRecipes.views()` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/crafting/NativeRecipes.java:77` — `NativeRecipes.view(Object)` | method | 19 | yes |
| `common/src/main/java/thaumcraft/api/crafting/NativeRecipes.java:88` — `NativeRecipes.aspects(java.util.Map<String, Integer>)` | method | 187 | yes |
| `common/src/main/java/thaumcraft/api/crafting/NativeRecipes.java:99` — `NativeRecipes.sample(Ingredient)` | method | 19 | yes |
| `common/src/main/java/thaumcraft/api/crafting/NativeRecipes.java:106` — `NativeRecipes.holder(ResourceLocation)` | method | 28 | yes |
| `common/src/main/java/thaumcraft/api/crafting/NativeRecipes.java:115` — `NativeRecipes.byId(ResourceLocation)` | method | 15 | yes |
| `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:28` — `ShapedArcaneRecipe.ItemStack output` | field | 57 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:29` — `ShapedArcaneRecipe.Object[] input` | field | 77 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:30` — `ShapedArcaneRecipe.AspectList aspects` | field | 187 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:31` — `ShapedArcaneRecipe.String research` | field | 146 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:32` — `ShapedArcaneRecipe.int width` | field | 138 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:33` — `ShapedArcaneRecipe.int height` | field | 209 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:41` — `ShapedArcaneRecipe.<init>(String, Block, AspectList, Object[])` | constructor | 6 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:45` — `ShapedArcaneRecipe.<init>(String, Item, AspectList, Object[])` | constructor | 6 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:49` — `ShapedArcaneRecipe.<init>(String, ItemStack, AspectList, Object[])` | constructor | 6 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:100` — `ShapedArcaneRecipe.nativeRecipe()` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:114` — `ShapedArcaneRecipe.of(ArcaneShapedRecipe)` | method | 335 | yes |
| `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:126` — `ShapedArcaneRecipe.getCraftingResult(ArcaneRecipeInput)` | method | 6 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:130` — `ShapedArcaneRecipe.getRecipeSize()` | method | 2 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:132` — `ShapedArcaneRecipe.getRecipeOutput()` | method | 17 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:134` — `ShapedArcaneRecipe.matches(ArcaneRecipeInput, Level, Player)` | method | 105 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:139` — `ShapedArcaneRecipe.setMirrored(boolean)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:163` — `ShapedArcaneRecipe.getInput()` | method | 4 | yes |
| `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:165` — `ShapedArcaneRecipe.getAspects()` | method | 147 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:167` — `ShapedArcaneRecipe.getAspects(ArcaneRecipeInput)` | method | 147 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java:169` — `ShapedArcaneRecipe.getResearch()` | method | 28 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:28` — `ShapelessArcaneRecipe.AspectList aspects` | field | 187 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:29` — `ShapelessArcaneRecipe.String research` | field | 146 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:33` — `ShapelessArcaneRecipe.<init>(String, Block, AspectList, Object[])` | constructor | 5 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:37` — `ShapelessArcaneRecipe.<init>(String, Item, AspectList, Object[])` | constructor | 5 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:41` — `ShapelessArcaneRecipe.<init>(String, ItemStack, AspectList, Object[])` | constructor | 5 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:57` — `ShapelessArcaneRecipe.nativeRecipe()` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:62` — `ShapelessArcaneRecipe.of(ArcaneShapelessRecipe)` | method | 335 | yes |
| `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:68` — `ShapelessArcaneRecipe.getRecipeSize()` | method | 2 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:70` — `ShapelessArcaneRecipe.getRecipeOutput()` | method | 17 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:72` — `ShapelessArcaneRecipe.getCraftingResult(ArcaneRecipeInput)` | method | 6 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:76` — `ShapelessArcaneRecipe.matches(ArcaneRecipeInput, Level, Player)` | method | 105 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:82` — `ShapelessArcaneRecipe.getInput()` | method | 4 | yes |
| `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:102` — `ShapelessArcaneRecipe.getAspects()` | method | 147 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:104` — `ShapelessArcaneRecipe.getAspects(ArcaneRecipeInput)` | method | 147 | no |
| `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java:106` — `ShapelessArcaneRecipe.getResearch()` | method | 28 | no |
| `common/src/main/java/thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java:32` — `DamageSourceIndirectThaumcraftEntity.create(Entity, Entity, Holder<DamageType>)` | method | 356 | yes |
| `common/src/main/java/thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java:37` — `DamageSourceIndirectThaumcraftEntity.create(Entity, Entity, ResourceKey<DamageType>, RegistryAccess)` | method | 356 | yes |
| `common/src/main/java/thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java:52` — `DamageSourceIndirectThaumcraftEntity.setFireDamage(Entity, Entity, Holder<DamageType>)` | method | 3 | yes |
| `common/src/main/java/thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java:71` — `DamageSourceIndirectThaumcraftEntity.setUnblockable(Entity, Entity, Holder<DamageType>)` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/damagesource/DamageSourceThaumcraft.java:27` — `DamageSourceThaumcraft.ResourceKey<DamageType> TAINT` | field | 29 | yes |
| `common/src/main/java/thaumcraft/api/damagesource/DamageSourceThaumcraft.java:29` — `DamageSourceThaumcraft.ResourceKey<DamageType> TENTACLE` | field | 2 | yes |
| `common/src/main/java/thaumcraft/api/damagesource/DamageSourceThaumcraft.java:31` — `DamageSourceThaumcraft.ResourceKey<DamageType> SWARM` | field | 2 | yes |
| `common/src/main/java/thaumcraft/api/damagesource/DamageSourceThaumcraft.java:33` — `DamageSourceThaumcraft.ResourceKey<DamageType> DISSOLVE` | field | 0 | yes |
| `common/src/main/java/thaumcraft/api/damagesource/DamageSourceThaumcraft.java:38` — `DamageSourceThaumcraft.taint(Level)` | method | 18 | yes |
| `common/src/main/java/thaumcraft/api/damagesource/DamageSourceThaumcraft.java:40` — `DamageSourceThaumcraft.dissolve(Level)` | method | 2 | yes |
| `common/src/main/java/thaumcraft/api/damagesource/DamageSourceThaumcraft.java:42` — `DamageSourceThaumcraft.causeTentacleDamage(LivingEntity)` | method | 2 | yes |
| `common/src/main/java/thaumcraft/api/damagesource/DamageSourceThaumcraft.java:44` — `DamageSourceThaumcraft.causeSwarmDamage(LivingEntity)` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/entities/IThaumcraftBoss.java:21` — `IThaumcraftBoss.generateName()` | method | 9 | yes |
| `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:16` — `DummyInternalMethodHandler.generateVisEffect(int, int, int, int, int, int, int, int)` | method | 6 | no |
| `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:18` — `DummyInternalMethodHandler.isResearchComplete(String, String)` | method | 76 | no |
| `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:20` — `DummyInternalMethodHandler.hasDiscoveredAspect(String, Aspect)` | method | 37 | no |
| `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:22` — `DummyInternalMethodHandler.getDiscoveredAspects(String)` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:24` — `DummyInternalMethodHandler.getStackInRowAndColumn(Object, int, int)` | method | 8 | no |
| `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:26` — `DummyInternalMethodHandler.getObjectAspects(ItemStack)` | method | 10 | no |
| `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:28` — `DummyInternalMethodHandler.getBonusObjectTags(ItemStack, AspectList)` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:30` — `DummyInternalMethodHandler.generateTags(Item, int)` | method | 6 | no |
| `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:32` — `DummyInternalMethodHandler.consumeVisFromWand(ItemStack, Player, AspectList, boolean, boolean)` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:34` — `DummyInternalMethodHandler.consumeVisFromWandCrafting(ItemStack, Player, AspectList, boolean)` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:36` — `DummyInternalMethodHandler.consumeVisFromInventory(Player, AspectList)` | method | 10 | no |
| `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:38` — `DummyInternalMethodHandler.addWarpToPlayer(Player, int, boolean)` | method | 13 | no |
| `common/src/main/java/thaumcraft/api/internal/DummyInternalMethodHandler.java:40` — `DummyInternalMethodHandler.addStickyWarpToPlayer(Player, int)` | method | 11 | no |
| `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:22` — `IInternalMethodHandler.generateVisEffect(ServerLevel, int, int, int, int, int, int, int)` | method | 6 | no |
| `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:24` — `IInternalMethodHandler.isResearchComplete(Player, String)` | method | 76 | no |
| `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:26` — `IInternalMethodHandler.hasDiscoveredAspect(Player, Aspect)` | method | 37 | no |
| `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:28` — `IInternalMethodHandler.getDiscoveredAspects(Player)` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:30` — `IInternalMethodHandler.getStackInRowAndColumn(Object, int, int)` | method | 8 | no |
| `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:32` — `IInternalMethodHandler.getObjectAspects(Level, ItemStack)` | method | 10 | no |
| `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:34` — `IInternalMethodHandler.getBonusObjectTags(Level, ItemStack, AspectList)` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:36` — `IInternalMethodHandler.generateTags(Level, Item, int)` | method | 6 | no |
| `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:38` — `IInternalMethodHandler.consumeVisFromWand(ItemStack, Player, AspectList, boolean, boolean)` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:40` — `IInternalMethodHandler.consumeVisFromWandCrafting(ItemStack, Player, AspectList, boolean)` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:42` — `IInternalMethodHandler.consumeVisFromInventory(Player, AspectList)` | method | 10 | no |
| `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:44` — `IInternalMethodHandler.addWarpToPlayer(Player, int, boolean)` | method | 13 | no |
| `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:46` — `IInternalMethodHandler.addStickyWarpToPlayer(Player, int)` | method | 11 | no |
| `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:48` — `IInternalMethodHandler.generateVisEffect(int, int, int, int, int, int, int, int)` | method | 6 | no |
| `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:50` — `IInternalMethodHandler.isResearchComplete(String, String)` | method | 76 | no |
| `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:52` — `IInternalMethodHandler.getObjectAspects(ItemStack)` | method | 10 | no |
| `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:54` — `IInternalMethodHandler.getBonusObjectTags(ItemStack, AspectList)` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:56` — `IInternalMethodHandler.generateTags(Item, int)` | method | 6 | no |
| `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:58` — `IInternalMethodHandler.hasDiscoveredAspect(String, Aspect)` | method | 37 | no |
| `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:60` — `IInternalMethodHandler.getDiscoveredAspects(String)` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java:63` — `IInternalMethodHandler.generateVisEffect(net.minecraft.world.level.Level, net.minecraft.core.BlockPos, net.minecraft.core.BlockPos, thaumcraft.api.aspects.Aspect)` | method | 6 | yes |
| `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:33` — `ThaumcraftInternalMethods.<init>()` | constructor | 1 | no |
| `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:35` — `ThaumcraftInternalMethods.generateVisEffect(int, int, int, int, int, int, int, int)` | method | 6 | no |
| `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:39` — `ThaumcraftInternalMethods.isResearchComplete(String, String)` | method | 76 | no |
| `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:44` — `ThaumcraftInternalMethods.hasDiscoveredAspect(String, Aspect)` | method | 37 | no |
| `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:49` — `ThaumcraftInternalMethods.getDiscoveredAspects(String)` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:70` — `ThaumcraftInternalMethods.getStackInRowAndColumn(Object, int, int)` | method | 8 | yes |
| `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:85` — `ThaumcraftInternalMethods.getObjectAspects(ItemStack)` | method | 10 | no |
| `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:91` — `ThaumcraftInternalMethods.getBonusObjectTags(ItemStack, AspectList)` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:97` — `ThaumcraftInternalMethods.generateTags(Item, int)` | method | 6 | no |
| `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:105` — `ThaumcraftInternalMethods.consumeVisFromWand(ItemStack, Player, AspectList, boolean, boolean)` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:109` — `ThaumcraftInternalMethods.consumeVisFromWandCrafting(ItemStack, Player, AspectList, boolean)` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:113` — `ThaumcraftInternalMethods.consumeVisFromInventory(Player, AspectList)` | method | 10 | no |
| `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:117` — `ThaumcraftInternalMethods.addWarpToPlayer(Player, int, boolean)` | method | 13 | no |
| `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:123` — `ThaumcraftInternalMethods.addStickyWarpToPlayer(Player, int)` | method | 11 | no |
| `common/src/main/java/thaumcraft/api/internal/WeightedRandomLoot.java:8` — `WeightedRandomLoot.ItemStack item` | field | 229 | no |
| `common/src/main/java/thaumcraft/api/internal/WeightedRandomLoot.java:9` — `WeightedRandomLoot.int itemWeight` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/internal/WeightedRandomLoot.java:10` — `WeightedRandomLoot.ArrayList<WeightedRandomLoot> lootBagCommon` | field | 4 | no |
| `common/src/main/java/thaumcraft/api/internal/WeightedRandomLoot.java:11` — `WeightedRandomLoot.ArrayList<WeightedRandomLoot> lootBagUncommon` | field | 4 | no |
| `common/src/main/java/thaumcraft/api/internal/WeightedRandomLoot.java:12` — `WeightedRandomLoot.ArrayList<WeightedRandomLoot> lootBagRare` | field | 4 | no |
| `common/src/main/java/thaumcraft/api/internal/WeightedRandomLoot.java:14` — `WeightedRandomLoot.<init>(ItemStack, int)` | constructor | 4 | no |
| `common/src/main/java/thaumcraft/api/items/IVisDiscountGear.java:15` — `IVisDiscountGear.getVisDiscount(ItemStack, Player)` | method | 20 | no |
| `common/src/main/java/thaumcraft/api/items/IVisDiscountGear.java:17` — `IVisDiscountGear.getVisDiscount(ItemStack, Player, Aspect)` | method | 20 | no |
| `common/src/main/java/thaumcraft/api/items/ItemsTC.java:23` — `ItemsTC.id(String)` | method | 241 | yes |
| `common/src/main/java/thaumcraft/api/items/ItemsTC.java:29` — `ItemsTC.get(String)` | method | 853 | yes |
| `common/src/main/java/thaumcraft/api/items/ItemsTC.java:36` — `ItemsTC.require(String)` | method | 19 | yes |
| `common/src/main/java/thaumcraft/api/items/ItemsTC.java:43` — `ItemsTC.stack(String)` | method | 591 | yes |
| `common/src/main/java/thaumcraft/api/items/ItemsTC.java:46` — `ItemsTC.stack(String, int)` | method | 591 | yes |
| `common/src/main/java/thaumcraft/api/items/ItemsTC.java:51` — `ItemsTC.isRegistered(String)` | method | 2 | yes |
| `common/src/main/java/thaumcraft/api/items/ItemsTC.java:54` — `ItemsTC.names()` | method | 5 | yes |
| `common/src/main/java/thaumcraft/api/items/ItemsTC.java:63` — `ItemsTC.material(String)` | method | 79 | yes |
| `common/src/main/java/thaumcraft/api/items/ItemsTC.java:70` — `ItemsTC.shard(String)` | method | 9 | yes |
| `common/src/main/java/thaumcraft/api/items/ItemsTC.java:77` — `ItemsTC.thaumometer()` | method | 2 | yes |
| `common/src/main/java/thaumcraft/api/items/ItemsTC.java:80` — `ItemsTC.thaumonomicon()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/items/ItemsTC.java:83` — `ItemsTC.wand()` | method | 84 | yes |
| `common/src/main/java/thaumcraft/api/items/ItemsTC.java:86` — `ItemsTC.inkwell()` | method | 4 | yes |
| `common/src/main/java/thaumcraft/api/items/ItemsTC.java:89` — `ItemsTC.emptyPhial()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/items/ItemsTC.java:92` — `ItemsTC.filledPhial()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/items/ItemsTC.java:95` — `ItemsTC.crystalEssence()` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/items/ItemsTC.java:98` — `ItemsTC.researchNote()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/items/ItemsTC.java:101` — `ItemsTC.primordialPearl()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/items/ItemsTC.java:104` — `ItemsTC.ironWandCap()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/nodes/INode.java:38` — `INode.getId()` | method | 78 | yes |
| `common/src/main/java/thaumcraft/api/nodes/INode.java:47` — `INode.getAspectsBase()` | method | 8 | yes |
| `common/src/main/java/thaumcraft/api/nodes/INode.java:49` — `INode.getNodeType()` | method | 27 | no |
| `common/src/main/java/thaumcraft/api/nodes/INode.java:51` — `INode.setNodeType(NodeType)` | method | 11 | no |
| `common/src/main/java/thaumcraft/api/nodes/INode.java:53` — `INode.setNodeModifier(NodeModifier)` | method | 11 | no |
| `common/src/main/java/thaumcraft/api/nodes/INode.java:55` — `INode.getNodeModifier()` | method | 23 | no |
| `common/src/main/java/thaumcraft/api/nodes/INode.java:57` — `INode.getNodeVisBase(Aspect)` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/nodes/INode.java:59` — `INode.setNodeVisBase(Aspect, short)` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/nodes/IRevealer.java:7` — `IRevealer.showNodes(ItemStack, LivingEntity)` | method | 10 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:24` — `NodeAdapter.<init>(TileNode)` | constructor | 0 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:35` — `NodeAdapter.of(net.minecraft.world.level.block.entity.BlockEntity)` | method | 341 | yes |
| `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:40` — `NodeAdapter.node()` | method | 47 | yes |
| `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:42` — `NodeAdapter.getId()` | method | 78 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:44` — `NodeAdapter.getAspects()` | method | 158 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:46` — `NodeAdapter.setAspects(AspectList)` | method | 74 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:48` — `NodeAdapter.getAspectsBase()` | method | 8 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:50` — `NodeAdapter.getNodeType()` | method | 27 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:52` — `NodeAdapter.setNodeType(NodeType)` | method | 11 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:56` — `NodeAdapter.setNodeModifier(NodeModifier)` | method | 11 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:60` — `NodeAdapter.getNodeModifier()` | method | 23 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:64` — `NodeAdapter.getNodeVisBase(Aspect)` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:67` — `NodeAdapter.setNodeVisBase(Aspect, short)` | method | 4 | yes |
| `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:73` — `NodeAdapter.doesContainerAccept(Aspect)` | method | 39 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:75` — `NodeAdapter.addToContainer(Aspect, int)` | method | 42 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:77` — `NodeAdapter.takeFromContainer(Aspect, int)` | method | 44 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:79` — `NodeAdapter.takeFromContainer(AspectList)` | method | 44 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:81` — `NodeAdapter.doesContainerContainAmount(Aspect, int)` | method | 38 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:85` — `NodeAdapter.doesContainerContain(AspectList)` | method | 36 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:87` — `NodeAdapter.containerContains(Aspect)` | method | 40 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:21` — `NodeBase.getId()` | method | 78 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:23` — `NodeBase.getAspects()` | method | 158 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:25` — `NodeBase.setAspects(AspectList)` | method | 74 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:37` — `NodeBase.getAspectsBase()` | method | 8 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:39` — `NodeBase.getNodeType()` | method | 27 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:41` — `NodeBase.setNodeType(NodeType)` | method | 11 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:43` — `NodeBase.setNodeModifier(NodeModifier)` | method | 11 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:45` — `NodeBase.getNodeModifier()` | method | 23 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:47` — `NodeBase.getNodeVisBase(Aspect)` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:50` — `NodeBase.setNodeVisBase(Aspect, short)` | method | 4 | yes |
| `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:59` — `NodeBase.doesContainerAccept(Aspect)` | method | 39 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:61` — `NodeBase.addToContainer(Aspect, int)` | method | 42 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:68` — `NodeBase.takeFromContainer(Aspect, int)` | method | 44 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:73` — `NodeBase.takeFromContainer(AspectList)` | method | 44 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:75` — `NodeBase.doesContainerContainAmount(Aspect, int)` | method | 38 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:79` — `NodeBase.doesContainerContain(AspectList)` | method | 36 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeBase.java:81` — `NodeBase.containerContains(Aspect)` | method | 40 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeModifier.java:5` — `NodeModifier.NodeModifier BRIGHT` | field | 12 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeModifier.java:6` — `NodeModifier.NodeModifier PALE` | field | 12 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeModifier.java:7` — `NodeModifier.NodeModifier FADING` | field | 12 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeType.java:5` — `NodeType.NodeType NORMAL` | field | 26 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeType.java:6` — `NodeType.NodeType UNSTABLE` | field | 13 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeType.java:7` — `NodeType.NodeType DARK` | field | 15 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeType.java:8` — `NodeType.NodeType TAINTED` | field | 12 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeType.java:9` — `NodeType.NodeType HUNGRY` | field | 11 | no |
| `common/src/main/java/thaumcraft/api/nodes/NodeType.java:10` — `NodeType.NodeType PURE` | field | 11 | no |
| `common/src/main/java/thaumcraft/api/potions/PotionFluxTaint.java:20` — `PotionFluxTaint.ResourceKey<MobEffect> KEY` | field | 13 | yes |
| `common/src/main/java/thaumcraft/api/potions/PotionFluxTaint.java:26` — `PotionFluxTaint.instance()` | method | 101 | yes |
| `common/src/main/java/thaumcraft/api/potions/PotionFluxTaint.java:31` — `PotionFluxTaint.holder()` | method | 28 | yes |
| `common/src/main/java/thaumcraft/api/potions/PotionVisExhaust.java:20` — `PotionVisExhaust.ResourceKey<MobEffect> KEY` | field | 13 | yes |
| `common/src/main/java/thaumcraft/api/potions/PotionVisExhaust.java:26` — `PotionVisExhaust.instance()` | method | 101 | yes |
| `common/src/main/java/thaumcraft/api/potions/PotionVisExhaust.java:31` — `PotionVisExhaust.holder()` | method | 28 | yes |
| `common/src/main/java/thaumcraft/api/research/IScanEventHandler.java:10` — `IScanEventHandler.scanPhenomena(ItemStack, Level, Player)` | method | 10 | no |
| `common/src/main/java/thaumcraft/api/research/IScanThing.java:12` — `IScanThing.checkThing(Player, Object)` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/research/IScanThing.java:15` — `IScanThing.getResearchKey(Player, Object)` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/research/IScanThing.java:18` — `IScanThing.onSuccess(Player, Object)` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:14` — `ResearchAddendum.getText()` | method | 2 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:16` — `ResearchAddendum.getTextLocalized()` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:20` — `ResearchAddendum.setText(String)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:22` — `ResearchAddendum.getRecipes()` | method | 2 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:24` — `ResearchAddendum.setRecipes(ResourceLocation[])` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:26` — `ResearchAddendum.getResearch()` | method | 29 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchAddendum.java:28` — `ResearchAddendum.setResearch(String[])` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchCategories.java:22` — `ResearchCategories.LinkedHashMap<String, ResearchCategoryList> researchCategories` | field | 7 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchCategories.java:29` — `ResearchCategories.loadNative()` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchCategories.java:51` — `ResearchCategories.getResearchList(String)` | method | 5 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchCategories.java:57` — `ResearchCategories.getCategoryName(String)` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchCategories.java:61` — `ResearchCategories.getResearch(String)` | method | 29 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchCategories.java:71` — `ResearchCategories.allResearch()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchCategories.java:78` — `ResearchCategories.registerCategory(String, ResourceLocation, ResourceLocation)` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchCategories.java:86` — `ResearchCategories.addResearch(ResearchItem)` | method | 28 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchCategoriesCompat.java:13` — `ResearchCategoriesCompat.getResearch(String)` | method | 29 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchCategoriesCompat.java:19` — `ResearchCategoriesCompat.getResearchCategory(String)` | method | 2 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchCategory.java:14` — `ResearchCategory.ResourceLocation background2` | field | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchCategory.java:15` — `ResearchCategory.String researchKey` | field | 10 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchCategory.java:16` — `ResearchCategory.String key` | field | 203 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchCategory.java:17` — `ResearchCategory.AspectList formula` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchCategory.java:19` — `ResearchCategory.<init>(String, String, AspectList, ResourceLocation, ResourceLocation)` | constructor | 3 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchCategory.java:24` — `ResearchCategory.<init>(String, String, AspectList, ResourceLocation, ResourceLocation, ResourceLocation)` | constructor | 3 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchCategory.java:35` — `ResearchCategory.applyFormula(AspectList)` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchCategory.java:39` — `ResearchCategory.applyFormula(AspectList, double)` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchCategoryList.java:14` — `ResearchCategoryList.int minDisplayColumn` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchCategoryList.java:15` — `ResearchCategoryList.int minDisplayRow` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchCategoryList.java:16` — `ResearchCategoryList.int maxDisplayColumn` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchCategoryList.java:17` — `ResearchCategoryList.int maxDisplayRow` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchCategoryList.java:18` — `ResearchCategoryList.ResourceLocation icon` | field | 12 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchCategoryList.java:19` — `ResearchCategoryList.ResourceLocation background` | field | 4 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchCategoryList.java:20` — `ResearchCategoryList.Map<String, ResearchItem> research` | field | 127 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchCategoryList.java:22` — `ResearchCategoryList.<init>(ResourceLocation, ResourceLocation)` | constructor | 7 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchCategoryList.java:28` — `ResearchCategoryList.getResearch(String)` | method | 29 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchCategoryList.java:33` — `ResearchCategoryList.size()` | method | 407 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:29` — `ResearchEntry.EnumResearchMeta.EnumResearchMeta ROUND` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:30` — `ResearchEntry.EnumResearchMeta.EnumResearchMeta SPIKY` | field | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:31` — `ResearchEntry.EnumResearchMeta.EnumResearchMeta REVERSE` | field | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:32` — `ResearchEntry.EnumResearchMeta.EnumResearchMeta HIDDEN` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:33` — `ResearchEntry.EnumResearchMeta.EnumResearchMeta AUTOUNLOCK` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:34` — `ResearchEntry.EnumResearchMeta.EnumResearchMeta HEX` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:38` — `ResearchEntry.of(ResearchItem)` | method | 337 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:52` — `ResearchEntry.getKey()` | method | 93 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:54` — `ResearchEntry.setKey(String)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:56` — `ResearchEntry.getCategory()` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:58` — `ResearchEntry.setCategory(String)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:60` — `ResearchEntry.getName()` | method | 177 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:62` — `ResearchEntry.getLocalizedName()` | method | 7 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:66` — `ResearchEntry.setName(String)` | method | 8 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:68` — `ResearchEntry.getParents()` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:71` — `ResearchEntry.getParentsClean()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:77` — `ResearchEntry.getParentsStripped()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:84` — `ResearchEntry.getSiblings()` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:88` — `ResearchEntry.getDisplayColumn()` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:90` — `ResearchEntry.setDisplayColumn(int)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:92` — `ResearchEntry.getDisplayRow()` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:94` — `ResearchEntry.setDisplayRow(int)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:96` — `ResearchEntry.getIcons()` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:98` — `ResearchEntry.setIcons(Object[])` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:100` — `ResearchEntry.getMeta()` | method | 2 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:102` — `ResearchEntry.hasMeta(EnumResearchMeta)` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:106` — `ResearchEntry.setMeta(EnumResearchMeta[])` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:108` — `ResearchEntry.getStages()` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:110` — `ResearchEntry.setStages(ResearchStage[])` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:112` — `ResearchEntry.getRewardItem()` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:114` — `ResearchEntry.setRewardItem(ItemStack[])` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:116` — `ResearchEntry.getRewardKnow()` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:118` — `ResearchEntry.setRewardKnow(ResearchStage.Knowledge[])` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:120` — `ResearchEntry.getAddenda()` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchEntry.java:122` — `ResearchEntry.setAddenda(ResearchAddendum[])` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:17` — `ResearchItem.String key` | field | 203 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:18` — `ResearchItem.String category` | field | 34 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:19` — `ResearchItem.AspectList tags` | field | 40 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:20` — `ResearchItem.String[] parents` | field | 12 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:21` — `ResearchItem.String[] parentsHidden` | field | 7 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:22` — `ResearchItem.String[] siblings` | field | 11 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:23` — `ResearchItem.int displayColumn` | field | 2 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:24` — `ResearchItem.int displayRow` | field | 2 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:25` — `ResearchItem.ItemStack icon_item` | field | 2 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:26` — `ResearchItem.ResourceLocation icon_resource` | field | 2 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:43` — `ResearchItem.<init>(String, String)` | constructor | 20 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:54` — `ResearchItem.<init>(String, String, AspectList, int, int, int, ResourceLocation)` | constructor | 20 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:67` — `ResearchItem.<init>(String, String, AspectList, int, int, int, ItemStack)` | constructor | 20 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:81` — `ResearchItem.of(ResearchDefinition)` | method | 337 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:124` — `ResearchItem.definition()` | method | 16 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:126` — `ResearchItem.setSpecial()` | method | 3 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:128` — `ResearchItem.setStub()` | method | 6 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:130` — `ResearchItem.setLost()` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:132` — `ResearchItem.setConcealed()` | method | 6 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:134` — `ResearchItem.setHidden()` | method | 7 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:136` — `ResearchItem.setVirtual()` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:138` — `ResearchItem.setParents(String[])` | method | 7 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:140` — `ResearchItem.setParentsHidden(String[])` | method | 3 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:142` — `ResearchItem.setSiblings(String[])` | method | 3 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:144` — `ResearchItem.setPages(ResearchPage[])` | method | 6 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:146` — `ResearchItem.getPages()` | method | 2 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:148` — `ResearchItem.setItemTriggers(ItemStack[])` | method | 3 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:150` — `ResearchItem.setEntityTriggers(String[])` | method | 3 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:152` — `ResearchItem.setAspectTriggers(Aspect[])` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:163` — `ResearchItem.getItemTriggers()` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:173` — `ResearchItem.getEntityTriggers()` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:175` — `ResearchItem.getAspectTriggers()` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:177` — `ResearchItem.registerResearchItem()` | method | 6 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:179` — `ResearchItem.getName()` | method | 177 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:181` — `ResearchItem.getText()` | method | 2 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:183` — `ResearchItem.isSpecial()` | method | 10 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:185` — `ResearchItem.isStub()` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:187` — `ResearchItem.isLost()` | method | 9 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:189` — `ResearchItem.isConcealed()` | method | 3 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:191` — `ResearchItem.isHidden()` | method | 12 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:193` — `ResearchItem.isVirtual()` | method | 10 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:195` — `ResearchItem.isAutoUnlock()` | method | 11 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:197` — `ResearchItem.setAutoUnlock()` | method | 5 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:199` — `ResearchItem.isRound()` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:201` — `ResearchItem.setRound()` | method | 6 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:203` — `ResearchItem.isSecondary()` | method | 9 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:205` — `ResearchItem.setSecondary()` | method | 5 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:207` — `ResearchItem.getComplexity()` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:209` — `ResearchItem.setComplexity(int)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchItem.java:212` — `ResearchItem.getResearchPrimaryTag()` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:23` — `ResearchPage.PageType type` | field | 405 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:24` — `ResearchPage.String text` | field | 44 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:25` — `ResearchPage.String research` | field | 127 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:26` — `ResearchPage.ResourceLocation image` | field | 10 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:27` — `ResearchPage.AspectList aspects` | field | 197 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:28` — `ResearchPage.Object recipe` | field | 49 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:29` — `ResearchPage.ItemStack recipeOutput` | field | 7 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:31` — `ResearchPage.String recipeKey` | field | 5 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:33` — `ResearchPage.<init>(String)` | constructor | 8 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:38` — `ResearchPage.<init>(String, String)` | constructor | 8 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:44` — `ResearchPage.<init>(Recipe<?>)` | constructor | 8 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:50` — `ResearchPage.<init>(Recipe<?>[])` | constructor | 8 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:55` — `ResearchPage.<init>(IArcaneRecipe[])` | constructor | 8 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:60` — `ResearchPage.<init>(CrucibleRecipe[])` | constructor | 8 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:65` — `ResearchPage.<init>(InfusionRecipe[])` | constructor | 8 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:70` — `ResearchPage.<init>(List<Recipe<?>>)` | constructor | 8 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:75` — `ResearchPage.<init>(IArcaneRecipe)` | constructor | 8 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:81` — `ResearchPage.<init>(CrucibleRecipe)` | constructor | 8 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:88` — `ResearchPage.<init>(ItemStack)` | constructor | 8 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:94` — `ResearchPage.<init>(ItemStack, Level)` | constructor | 8 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:105` — `ResearchPage.<init>(InfusionRecipe)` | constructor | 8 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:111` — `ResearchPage.<init>(InfusionEnchantmentRecipe)` | constructor | 8 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:116` — `ResearchPage.<init>(ResourceLocation, String)` | constructor | 8 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:122` — `ResearchPage.<init>(AspectList)` | constructor | 8 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:128` — `ResearchPage.of(thaumcraft.common.lib.research.ResearchDefinition.Page)` | method | 337 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:153` — `ResearchPage.legacyType(String)` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:164` — `ResearchPage.getTranslatedText()` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:171` — `ResearchPage.registries()` | method | 103 | yes |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:176` — `ResearchPage.PageType.PageType TEXT` | field | 2 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:177` — `ResearchPage.PageType.PageType TEXT_CONCEALED` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:178` — `ResearchPage.PageType.PageType IMAGE` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:179` — `ResearchPage.PageType.PageType CRUCIBLE_CRAFTING` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:180` — `ResearchPage.PageType.PageType ARCANE_CRAFTING` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:181` — `ResearchPage.PageType.PageType ASPECTS` | field | 3 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:182` — `ResearchPage.PageType.PageType NORMAL_CRAFTING` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:183` — `ResearchPage.PageType.PageType INFUSION_CRAFTING` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:184` — `ResearchPage.PageType.PageType COMPOUND_CRAFTING` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:185` — `ResearchPage.PageType.PageType INFUSION_ENCHANTMENT` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchPage.java:186` — `ResearchPage.PageType.PageType SMELTING` | field | 3 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchStage.java:23` — `ResearchStage.Knowledge.EnumKnowledgeType type` | field | 405 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchStage.java:24` — `ResearchStage.Knowledge.ResearchCategory category` | field | 34 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchStage.java:25` — `ResearchStage.Knowledge.int amount` | field | 341 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchStage.java:27` — `ResearchStage.Knowledge.<init>(EnumKnowledgeType, ResearchCategory, int)` | constructor | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchStage.java:34` — `ResearchStage.getText()` | method | 2 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchStage.java:36` — `ResearchStage.getTextLocalized()` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchStage.java:40` — `ResearchStage.setText(String)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchStage.java:42` — `ResearchStage.getRecipes()` | method | 2 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchStage.java:44` — `ResearchStage.setRecipes(ResourceLocation[])` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchStage.java:46` — `ResearchStage.getObtain()` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchStage.java:48` — `ResearchStage.setObtain(Object[])` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchStage.java:50` — `ResearchStage.getCraft()` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchStage.java:52` — `ResearchStage.setCraft(Object[])` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchStage.java:54` — `ResearchStage.getCraftReference()` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchStage.java:56` — `ResearchStage.setCraftReference(int[])` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchStage.java:58` — `ResearchStage.getKnow()` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchStage.java:60` — `ResearchStage.setKnow(Knowledge[])` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchStage.java:62` — `ResearchStage.getResearch()` | method | 29 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchStage.java:64` — `ResearchStage.setResearch(String[])` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchStage.java:66` — `ResearchStage.getResearchIcon()` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchStage.java:68` — `ResearchStage.setResearchIcon(String[])` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchStage.java:70` — `ResearchStage.getWarp()` | method | 35 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchStage.java:72` — `ResearchStage.setWarp(int)` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ResearchStage.java:75` — `ResearchStage.knowledge()` | method | 71 | yes |
| `common/src/main/java/thaumcraft/api/research/ScanBlock.java:18` — `ScanBlock.<init>(Block)` | constructor | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ScanBlock.java:22` — `ScanBlock.<init>(String, Block[])` | constructor | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ScanBlock.java:27` — `ScanBlock.checkThing(Player, Object)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ScanBlock.java:38` — `ScanBlock.getResearchKey(Player, Object)` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ScanBlock.java:41` — `ScanBlock.blocks()` | method | 210 | yes |
| `common/src/main/java/thaumcraft/api/research/ScanResult.java:13` — `ScanResult.byte type` | field | 405 | no |
| `common/src/main/java/thaumcraft/api/research/ScanResult.java:14` — `ScanResult.int id` | field | 240 | no |
| `common/src/main/java/thaumcraft/api/research/ScanResult.java:15` — `ScanResult.int meta` | field | 127 | no |
| `common/src/main/java/thaumcraft/api/research/ScanResult.java:16` — `ScanResult.Entity entity` | field | 415 | no |
| `common/src/main/java/thaumcraft/api/research/ScanResult.java:17` — `ScanResult.String phenomena` | field | 5 | no |
| `common/src/main/java/thaumcraft/api/research/ScanResult.java:19` — `ScanResult.ItemStack stack` | field | 592 | yes |
| `common/src/main/java/thaumcraft/api/research/ScanResult.java:21` — `ScanResult.BlockPos nodePosition` | field | 1 | yes |
| `common/src/main/java/thaumcraft/api/research/ScanResult.java:22` — `ScanResult.ResourceKey<Level> nodeDimension` | field | 1 | no |
| `common/src/main/java/thaumcraft/api/research/ScanResult.java:24` — `ScanResult.<init>(byte, int, int, Entity, String)` | constructor | 8 | no |
| `common/src/main/java/thaumcraft/api/research/ScanResult.java:28` — `ScanResult.item(ItemStack)` | method | 230 | no |
| `common/src/main/java/thaumcraft/api/research/ScanResult.java:33` — `ScanResult.entity(Entity)` | method | 415 | no |
| `common/src/main/java/thaumcraft/api/research/ScanResult.java:37` — `ScanResult.node(TileNode)` | method | 47 | no |
| `common/src/main/java/thaumcraft/api/research/ScanResult.java:46` — `ScanResult.equals(Object)` | method | 349 | yes |
| `common/src/main/java/thaumcraft/api/research/ScanningManager.java:18` — `ScanningManager.addScannableThing(IScanThing)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/ScanningManager.java:23` — `ScanningManager.scannableThings()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/research/ScanningManager.java:26` — `ScanningManager.find(Player, Object)` | method | 15 | yes |
| `common/src/main/java/thaumcraft/api/research/theorycraft/ITheorycraftAid.java:8` — `ITheorycraftAid.getAidObject()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/research/theorycraft/ITheorycraftAid.java:11` — `ITheorycraftAid.getCards()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:14` — `ResearchTableData.BlockEntity table` | field | 26 | no |
| `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:15` — `ResearchTableData.String player` | field | 672 | no |
| `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:16` — `ResearchTableData.int inspiration` | field | 0 | no |
| `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:17` — `ResearchTableData.int inspirationStart` | field | 0 | no |
| `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:18` — `ResearchTableData.int bonusDraws` | field | 0 | no |
| `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:19` — `ResearchTableData.TreeMap<String, Integer> categoryTotals` | field | 0 | no |
| `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:21` — `ResearchTableData.<init>(BlockEntity)` | constructor | 0 | no |
| `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:25` — `ResearchTableData.<init>(Player, BlockEntity)` | constructor | 0 | no |
| `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:30` — `ResearchTableData.hasTotal(String)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:34` — `ResearchTableData.getTotal(String)` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:39` — `ResearchTableData.addTotal(String, int)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/theorycraft/ResearchTableData.java:43` — `ResearchTableData.addInspiration(int)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:17` — `TheorycraftCard.getSeed()` | method | 15 | no |
| `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:20` — `TheorycraftCard.initialize(Player, ResearchTableData)` | method | 11 | yes |
| `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:23` — `TheorycraftCard.isAidOnly()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:25` — `TheorycraftCard.getInspirationCost()` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:28` — `TheorycraftCard.getResearchCategory()` | method | 2 | yes |
| `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:30` — `TheorycraftCard.getLocalizedName()` | method | 7 | no |
| `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:32` — `TheorycraftCard.getLocalizedText()` | method | 3 | no |
| `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:35` — `TheorycraftCard.getRequiredItems()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:38` — `TheorycraftCard.getRequiredItemsConsumed()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:41` — `TheorycraftCard.activate(Player, ResearchTableData)` | method | 1 | yes |
| `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:43` — `TheorycraftCard.setSeed(long)` | method | 8 | no |
| `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:46` — `TheorycraftCard.serialize()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:52` — `TheorycraftCard.deserialize(CompoundTag)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:57` — `TheorycraftCard.name()` | method | 126 | yes |
| `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftCard.java:60` — `TheorycraftCard.text()` | method | 49 | yes |
| `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftManager.java:11` — `TheorycraftManager.HashMap<String, ITheorycraftAid> aids` | field | 0 | no |
| `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftManager.java:12` — `TheorycraftManager.HashMap<String, Class<TheorycraftCard>> cards` | field | 0 | no |
| `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftManager.java:16` — `TheorycraftManager.registerAid(ITheorycraftAid)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftManager.java:21` — `TheorycraftManager.registerCard(Class<?>)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftManager.java:28` — `TheorycraftManager.card(String)` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/research/theorycraft/TheorycraftManager.java:31` — `TheorycraftManager.aid(String)` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:30` — `TileVisNode.boolean nodeRefresh` | field | 5 | no |
| `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:33` — `TileVisNode.getRange()` | method | 30 | no |
| `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:34` — `TileVisNode.isSource()` | method | 14 | no |
| `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:35` — `TileVisNode.getAttunement()` | method | 7 | no |
| `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:37` — `TileVisNode.getLocation()` | method | 7 | no |
| `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:38` — `TileVisNode.getNodeCounter()` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:39` — `TileVisNode.getAttachmentVersion()` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:40` — `TileVisNode.isNodeTickActive()` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:41` — `TileVisNode.isConsumeActive()` | method | 5 | no |
| `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:56` — `TileVisNode.consumeVis(Aspect, int)` | method | 6 | no |
| `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:57` — `TileVisNode.triggerConsumeEffect(Aspect)` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:58` — `TileVisNode.parentChanged()` | method | 2 | no |
| `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:60` — `TileVisNode.getParent()` | method | 10 | no |
| `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:64` — `TileVisNode.getChildren()` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:72` — `TileVisNode.getRootSource()` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:76` — `TileVisNode.setParent(WeakReference<TileVisNode>)` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:84` — `TileVisNode.removeThisNode()` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:86` — `TileVisNode.tick(Level, BlockPos, BlockState, TileVisNode)` | method | 231 | no |
| `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:98` — `TileVisNode.setRemoved()` | method | 6 | no |
| `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:107` — `TileVisNode.clearRemoved()` | method | 3 | no |
| `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:108` — `TileVisNode.setLevel(Level)` | method | 2 | no |
| `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:180` — `TileVisNode.getUpdateTag(HolderLookup.Provider)` | method | 53 | no |
| `common/src/main/java/thaumcraft/api/visnet/TileVisNode.java:187` — `TileVisNode.getUpdatePacket()` | method | 53 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:12` — `FocusUpgradeType.FocusUpgradeType potency` | field | 17 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:13` — `FocusUpgradeType.FocusUpgradeType frugal` | field | 26 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:14` — `FocusUpgradeType.FocusUpgradeType treasure` | field | 13 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:15` — `FocusUpgradeType.FocusUpgradeType enlarge` | field | 17 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:16` — `FocusUpgradeType.FocusUpgradeType alchemistsfire` | field | 2 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:17` — `FocusUpgradeType.FocusUpgradeType alchemistsfrost` | field | 2 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:18` — `FocusUpgradeType.FocusUpgradeType architect` | field | 11 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:19` — `FocusUpgradeType.FocusUpgradeType extend` | field | 6 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:20` — `FocusUpgradeType.FocusUpgradeType silktouch` | field | 9 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:21` — `FocusUpgradeType.FocusUpgradeType fireball` | field | 4 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:22` — `FocusUpgradeType.FocusUpgradeType firebeam` | field | 2 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:23` — `FocusUpgradeType.FocusUpgradeType scattershot` | field | 2 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:24` — `FocusUpgradeType.FocusUpgradeType iceboulder` | field | 2 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:25` — `FocusUpgradeType.FocusUpgradeType batbombs` | field | 2 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:26` — `FocusUpgradeType.FocusUpgradeType devilbats` | field | 2 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:27` — `FocusUpgradeType.FocusUpgradeType nightshade` | field | 4 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:28` — `FocusUpgradeType.FocusUpgradeType seeker` | field | 4 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:29` — `FocusUpgradeType.FocusUpgradeType chainlightning` | field | 3 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:30` — `FocusUpgradeType.FocusUpgradeType earthshock` | field | 3 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:31` — `FocusUpgradeType.FocusUpgradeType vampirebats` | field | 2 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:32` — `FocusUpgradeType.FocusUpgradeType dowsing` | field | 7 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:37` — `FocusUpgradeType.short id` | field | 238 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:38` — `FocusUpgradeType.String name` | field | 125 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:39` — `FocusUpgradeType.String text` | field | 48 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:40` — `FocusUpgradeType.ResourceLocation icon` | field | 17 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:62` — `FocusUpgradeType.getAspects()` | method | 159 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:70` — `FocusUpgradeType.getLocalizedName()` | method | 6 | yes |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:78` — `FocusUpgradeType.getLocalizedText()` | method | 3 | yes |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:80` — `FocusUpgradeType.get(short)` | method | 849 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:81` — `FocusUpgradeType.get(int)` | method | 849 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:82` — `FocusUpgradeType.equals(Object)` | method | 346 | no |
| `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java:83` — `FocusUpgradeType.hashCode()` | method | 43 | no |
| `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:16` — `IWandFocus.getFocusColor()` | method | 23 | no |
| `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:19` — `IWandFocus.getFocusDepthLayerIcon()` | method | 4 | yes |
| `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:22` — `IWandFocus.getOrnament()` | method | 4 | yes |
| `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:24` — `IWandFocus.getAnimation()` | method | 9 | no |
| `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:26` — `IWandFocus.getVisCost()` | method | 24 | no |
| `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:28` — `IWandFocus.isVisCostPerTick()` | method | 5 | no |
| `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:30` — `IWandFocus.onFocusRightClick(net.minecraft.world.item.ItemStack, Level, Player, net.minecraft.world.phys.BlockHitResult)` | method | 22 | no |
| `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:33` — `IWandFocus.onUsingFocusTick(net.minecraft.world.item.ItemStack, Player, int)` | method | 8 | no |
| `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:35` — `IWandFocus.onPlayerStoppedUsingFocus(net.minecraft.world.item.ItemStack, Level, Player, int)` | method | 3 | no |
| `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:37` — `IWandFocus.getSortingHelper(net.minecraft.world.item.ItemStack)` | method | 24 | no |
| `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:39` — `IWandFocus.onFocusBlockStartBreak(net.minecraft.world.item.ItemStack, int, int, int, Player)` | method | 2 | no |
| `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:42` — `IWandFocus.acceptsEnchant(net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment>)` | method | 11 | yes |
| `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:46` — `IWandFocus.WandFocusAnimation.WandFocusAnimation WAVE` | field | 6 | no |
| `common/src/main/java/thaumcraft/api/wands/IWandFocus.java:47` — `IWandFocus.WandFocusAnimation.WandFocusAnimation CHARGE` | field | 9 | no |
| `common/src/main/java/thaumcraft/api/wands/IWandRodOnUpdate.java:12` — `IWandRodOnUpdate.onUpdate(ItemStack, Player)` | method | 84 | no |
| `common/src/main/java/thaumcraft/api/wands/IWandTriggerManager.java:13` — `IWandTriggerManager.performTrigger(Level, ItemStack, Player, int, int, int, int, int)` | method | 3 | no |
| `common/src/main/java/thaumcraft/api/wands/IWandable.java:18` — `IWandable.onWandRightClick(Level, ItemStack, Player, int, int, int, int, int)` | method | 19 | yes |
| `common/src/main/java/thaumcraft/api/wands/IWandable.java:21` — `IWandable.onWandRightClick(Level, ItemStack, Player)` | method | 19 | yes |
| `common/src/main/java/thaumcraft/api/wands/IWandable.java:23` — `IWandable.onUsingWandTick(ItemStack, Player, int)` | method | 15 | no |
| `common/src/main/java/thaumcraft/api/wands/IWandable.java:25` — `IWandable.onWandStoppedUsing(ItemStack, Level, Player, int)` | method | 15 | no |
| `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:22` — `ItemFocusBasic.<init>(Properties)` | constructor | 53 | no |
| `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:25` — `ItemFocusBasic.getVisCost(ItemStack)` | method | 24 | yes |
| `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:26` — `ItemFocusBasic.isVisCostPerTick(ItemStack)` | method | 5 | no |
| `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:27` — `ItemFocusBasic.getFocusColor(ItemStack)` | method | 23 | no |
| `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:28` — `ItemFocusBasic.getSortingHelper(ItemStack)` | method | 24 | no |
| `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:34` — `ItemFocusBasic.getActivationCooldown(ItemStack)` | method | 15 | no |
| `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:35` — `ItemFocusBasic.getAnimation(ItemStack)` | method | 9 | no |
| `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:45` — `ItemFocusBasic.getOrnament(ItemStack)` | method | 4 | yes |
| `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:55` — `ItemFocusBasic.getFocusDepthLayerIcon(ItemStack)` | method | 4 | yes |
| `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:63` — `ItemFocusBasic.getMaxAreaSize(ItemStack)` | method | 9 | yes |
| `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:71` — `ItemFocusBasic.onFocusBlockStartBreak(ItemStack, int, int, int, Player)` | method | 2 | yes |
| `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:73` — `ItemFocusBasic.onFocusRightClick(ItemStack, Level, Player, InteractionHand)` | method | 22 | no |
| `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:74` — `ItemFocusBasic.onUsingFocusTick(ItemStack, Player, int)` | method | 8 | no |
| `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:75` — `ItemFocusBasic.onPlayerStoppedUsingFocus(ItemStack, Level, Player, int)` | method | 3 | no |
| `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:77` — `ItemFocusBasic.getAppliedUpgrades(ItemStack)` | method | 5 | no |
| `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:78` — `ItemFocusBasic.getPossibleUpgradesByRank(ItemStack, int)` | method | 25 | no |
| `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:79` — `ItemFocusBasic.getUpgradeLevel(ItemStack, FocusUpgradeType)` | method | 11 | no |
| `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:80` — `ItemFocusBasic.isUpgradedWith(ItemStack, FocusUpgradeType)` | method | 20 | no |
| `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:82` — `ItemFocusBasic.canApplyUpgrade(ItemStack, Player, FocusUpgradeType, int)` | method | 12 | no |
| `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:93` — `ItemFocusBasic.applyUpgrade(ItemStack, FocusUpgradeType, int)` | method | 3 | yes |
| `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:99` — `ItemFocusBasic.appendHoverText(ItemStack, TooltipContext, List<Component>, TooltipFlag)` | method | 41 | no |
| `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:124` — `ItemFocusBasic.WandFocusAnimation.WandFocusAnimation WAVE` | field | 6 | no |
| `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java:124` — `ItemFocusBasic.WandFocusAnimation.WandFocusAnimation CHARGE` | field | 9 | no |
| `common/src/main/java/thaumcraft/api/wands/StaffRod.java:15` — `StaffRod.<init>(String, int, ItemStack, int)` | constructor | 2 | no |
| `common/src/main/java/thaumcraft/api/wands/StaffRod.java:20` — `StaffRod.<init>(String, int, ItemStack, int, IWandRodOnUpdate, ResourceLocation)` | constructor | 2 | no |
| `common/src/main/java/thaumcraft/api/wands/StaffRod.java:24` — `StaffRod.<init>(String, int, ItemStack, int, IWandRodOnUpdate)` | constructor | 2 | no |
| `common/src/main/java/thaumcraft/api/wands/StaffRod.java:29` — `StaffRod.<init>(String, int, ItemStack, int, ResourceLocation)` | constructor | 2 | no |
| `common/src/main/java/thaumcraft/api/wands/StaffRod.java:33` — `StaffRod.hasRunes()` | method | 4 | no |
| `common/src/main/java/thaumcraft/api/wands/StaffRod.java:35` — `StaffRod.setRunes(boolean)` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/wands/WandCap.java:17` — `WandCap.LinkedHashMap<String, WandCap> caps` | field | 11 | yes |
| `common/src/main/java/thaumcraft/api/wands/WandCap.java:27` — `WandCap.<init>(String, float, ItemStack, int)` | constructor | 10 | no |
| `common/src/main/java/thaumcraft/api/wands/WandCap.java:38` — `WandCap.<init>(String, float, List<Aspect>, float, ItemStack, int)` | constructor | 10 | no |
| `common/src/main/java/thaumcraft/api/wands/WandCap.java:51` — `WandCap.<init>(thaumcraft.common.items.wands.WandMaterials.Cap, ItemStack)` | constructor | 10 | yes |
| `common/src/main/java/thaumcraft/api/wands/WandCap.java:62` — `WandCap.nativeCap()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/wands/WandCap.java:64` — `WandCap.getBaseCostModifier()` | method | 2 | no |
| `common/src/main/java/thaumcraft/api/wands/WandCap.java:66` — `WandCap.getSpecialCostModifierAspects()` | method | 2 | no |
| `common/src/main/java/thaumcraft/api/wands/WandCap.java:68` — `WandCap.getSpecialCostModifier()` | method | 2 | no |
| `common/src/main/java/thaumcraft/api/wands/WandCap.java:70` — `WandCap.getTexture()` | method | 9 | no |
| `common/src/main/java/thaumcraft/api/wands/WandCap.java:72` — `WandCap.setTexture(ResourceLocation)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/wands/WandCap.java:74` — `WandCap.getTag()` | method | 133 | no |
| `common/src/main/java/thaumcraft/api/wands/WandCap.java:76` — `WandCap.setTag(String)` | method | 35 | no |
| `common/src/main/java/thaumcraft/api/wands/WandCap.java:78` — `WandCap.getItem()` | method | 395 | no |
| `common/src/main/java/thaumcraft/api/wands/WandCap.java:80` — `WandCap.setItem(ItemStack)` | method | 65 | no |
| `common/src/main/java/thaumcraft/api/wands/WandCap.java:82` — `WandCap.getCraftCost()` | method | 8 | no |
| `common/src/main/java/thaumcraft/api/wands/WandCap.java:84` — `WandCap.setCraftCost(int)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/wands/WandCap.java:87` — `WandCap.getResearch()` | method | 34 | yes |
| `common/src/main/java/thaumcraft/api/wands/WandCap.java:90` — `WandCap.byTag(String)` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/wands/WandRod.java:18` — `WandRod.LinkedHashMap<String, WandRod> rods` | field | 10 | yes |
| `common/src/main/java/thaumcraft/api/wands/WandRod.java:28` — `WandRod.<init>(String, int, ItemStack, int, ResourceLocation)` | constructor | 11 | no |
| `common/src/main/java/thaumcraft/api/wands/WandRod.java:38` — `WandRod.<init>(String, int, ItemStack, int, IWandRodOnUpdate, ResourceLocation)` | constructor | 11 | no |
| `common/src/main/java/thaumcraft/api/wands/WandRod.java:49` — `WandRod.<init>(String, int, ItemStack, int)` | constructor | 11 | no |
| `common/src/main/java/thaumcraft/api/wands/WandRod.java:54` — `WandRod.<init>(String, int, ItemStack, int, IWandRodOnUpdate)` | constructor | 11 | no |
| `common/src/main/java/thaumcraft/api/wands/WandRod.java:60` — `WandRod.<init>(thaumcraft.common.items.wands.WandMaterials.Rod, ItemStack)` | constructor | 11 | yes |
| `common/src/main/java/thaumcraft/api/wands/WandRod.java:71` — `WandRod.nativeRod()` | method | 0 | yes |
| `common/src/main/java/thaumcraft/api/wands/WandRod.java:73` — `WandRod.getTag()` | method | 133 | no |
| `common/src/main/java/thaumcraft/api/wands/WandRod.java:75` — `WandRod.setTag(String)` | method | 35 | no |
| `common/src/main/java/thaumcraft/api/wands/WandRod.java:77` — `WandRod.getCapacity()` | method | 7 | no |
| `common/src/main/java/thaumcraft/api/wands/WandRod.java:79` — `WandRod.setCapacity(int)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/wands/WandRod.java:81` — `WandRod.getTexture()` | method | 9 | no |
| `common/src/main/java/thaumcraft/api/wands/WandRod.java:83` — `WandRod.setTexture(ResourceLocation)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/wands/WandRod.java:85` — `WandRod.getItem()` | method | 395 | no |
| `common/src/main/java/thaumcraft/api/wands/WandRod.java:87` — `WandRod.setItem(ItemStack)` | method | 65 | no |
| `common/src/main/java/thaumcraft/api/wands/WandRod.java:89` — `WandRod.getCraftCost()` | method | 8 | no |
| `common/src/main/java/thaumcraft/api/wands/WandRod.java:91` — `WandRod.setCraftCost(int)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/wands/WandRod.java:93` — `WandRod.getOnUpdate()` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/wands/WandRod.java:95` — `WandRod.setOnUpdate(IWandRodOnUpdate)` | method | 0 | no |
| `common/src/main/java/thaumcraft/api/wands/WandRod.java:97` — `WandRod.isGlowing()` | method | 2 | no |
| `common/src/main/java/thaumcraft/api/wands/WandRod.java:99` — `WandRod.setGlowing(boolean)` | method | 1 | no |
| `common/src/main/java/thaumcraft/api/wands/WandRod.java:102` — `WandRod.getResearch()` | method | 34 | yes |
| `common/src/main/java/thaumcraft/api/wands/WandRod.java:105` — `WandRod.byTag(String)` | method | 0 | yes |
