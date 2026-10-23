# Public API contract documentation — e3docs

Baseline: `974fde6a`, branch `agent/e3docs`, 2026-10-23. Sources are `common/src/main/java/thaumcraft/api/**`. No gameplay implementation, signature, access modifier or executable statement was changed: Java edits are Javadoc and `@Deprecated` annotations only.

## Result and scope

| Disposition of the 510 audited declarations | Count |
| --- | ---: |
| Real API under the requested publication/cross-package-call rule | **345** |
| Real API with local documentation after this pass | **275** |
| Newly documented in this pass | **272** |
| Already locally documented at this worktree's baseline | **3** |
| Real API left **contract unclear** | **70** |
| Not admitted by the publication/caller evidence reviewed here | **165** |

The three already-documented entries are the two cached aspect-list helpers and the numeric-dimension `IInternalMethodHandler.generateVisEffect` default. The 510-item audit predates those local comments. Counts describe declarations, including the audit's types, not just methods. Existing comments on unrelated members are not counted as newly closed gaps. Supplemental legacy notes and five enchantment-field deprecations are outside the 510 callable/type denominator.

**The expected much-smaller subset is not supported by publication evidence.** The original JAR publishes **323** corresponding declarations; another **22** have reviewed native cross-package calls. An unused historical entry point remains real API under the requested rule. This is an eligibility inventory, not evidence that external addons currently use every member or that every compatibility registration participates in gameplay.

### Evidence method

- Read the documentation, deprecation and legacy sections of `API_QUALITY.md` first. Extracted its 510 individual missing-contract entries rather than recounting all public Java tokens.
- Read public class/member metadata directly from the local unmodified `Thaumcraft-1.7.10-4.2.3.5.jar`, restricted to `thaumcraft/api/**`. Did not edit or copy the oracle/decompile into tracked files.
- Matched owner, member and parameter shape, reviewing adapted Minecraft types: World→Level, EntityPlayer→Player, TileEntity→BlockEntity, IInventory→ArcaneRecipeInput, etc. Native dimension keys, enchantment holders and the stack/hand-aware focus adaptation are explicitly not old binary signatures. Name/arity matches alone were insufficient: two new object-tag overloads and four native-looking handler defaults were rejected as historical-publication matches.
- Searched production Java in common/Fabric, excluding strings/comments. Reviewed receiver/call-site evidence instead of accepting a shared method-name token as proof. Examples of rejected false positives: TC6 research getters versus native research objects, NodeAdapter/NodeBase overrides versus calls to TileNode/INode, and generic `get`, `clear`, `sync` or `tick` names. Retained legacy `src/` was not treated as a modern production caller.
- Parsed current declarations with Java 21's compiler tree/DocTrees APIs to associate local Javadoc with the correct overload. Body reads and callers, not the existence of a comment alone, informed the new documentation. Where implementations disagree, did not invent a stronger interface contract.

The 22 additional declarations are: `WorldCoordinates(ResourceKey<Level>, BlockPos)`, `pos`, `dimension`, `isCanonicalDimension`; `Aspect.getCombinationResult`; `AspectList.reduceToPrimals`; all six `IBauble` methods; `IInfusionStabiliser.canStabaliseInfusion`; `ResearchCategory.applyFormula(AspectList,double)`; `TileVisNode.getNodeCounter`, `isNodeTickActive`, `isConsumeActive`, `tick`; all three audited `FocusUpgradeType` methods; and `ItemFocusBasic(Properties)`.

Caller witnesses: `TileNode`/`VisNetHandler`, `ResearchAspectCombination`, `TileNodeEnergized`, `PlayerAccessoryInventory`/`PlayerAccessories`/`ContainerAccessories`, `InfusionStructure`, `NativePlayerKnowledge`, `TileVisRelay`/`TileNodeConverter`, `FocalUpgradeCosts`/`FocusUpgradeData`/`TileFocalManipulator`, and native focus subclass constructors, respectively. All are outside the declaration's own package.

## Exact real-API subset

To avoid duplicating or silently renaming overloaded signatures, **ordinals below refer to the individual entries in `API_QUALITY.md` → “Missing contract inventory”, in their existing order, starting at 1** (`IRunicArmor.INTERFACE`) and ending at 510 (`WandRod.setGlowing(boolean)`). They are not Java line numbers. The ranges identify every selected declaration unambiguously, including overloads. Paths below are relative to `common/src/main/java/thaumcraft/api/`. “Documented” includes the three pre-existing comments identified above; every selected entry not documented is listed in the unclear ledger that follows. All other inventory ordinals are excluded from this pass's real-API subset, not proposed for removal.

| File | Real API | Documented | Inventory ordinals |
| --- | ---: | ---: | --- |
| `IRunicArmor.java` | 2 | 1 | 1, 14 |
| `IVisDiscountGear.java` | 1 | 1 | 2 |
| `ThaumcraftApi.java` | 18 | 18 | 3, 21–32, 35–37, 40–41 |
| `nodes/IRevealer.java` | 2 | 1 | 4, 265 |
| `research/ResearchPage.java` | 15 | 15 | 6, 370–383 |
| `wands/ItemFocusBasic.java` | 15 | 15 | 7, 460–473 |
| `BlockCoordinates.java` | 4 | 4 | 8–11 |
| `IGoggles.java` | 1 | 0 | 12 |
| `IRepairableExtended.java` | 1 | 0 | 13 |
| `IWarpingGear.java` | 1 | 0 | 15 |
| `ItemRunic.java` | 2 | 2 | 16, 18 |
| `ThaumcraftApiHelper.java` | 17 | 9 | 42–58 |
| `WorldCoordinates.java` | 11 | 11 | 60–70 |
| `aspects/Aspect.java` | 14 | 14 | 71–84 |
| `aspects/AspectList.java` | 19 | 19 | 85–92, 94–104 |
| `aspects/IAspectContainer.java` | 9 | 2 | 105–113 |
| `aspects/IEssentiaContainerItem.java` | 2 | 0 | 114–115 |
| `aspects/IEssentiaTransport.java` | 12 | 0 | 116–127 |
| `baubles/IBauble.java` | 6 | 5 | 128–133 |
| `crafting/CrucibleRecipe.java` | 3 | 3 | 162–164 |
| `crafting/IArcaneRecipe.java` | 7 | 0 | 165–171 |
| `crafting/IInfusionStabiliser.java` | 1 | 0 | 172 |
| `crafting/InfusionEnchantmentRecipe.java` | 5 | 5 | 173–177 |
| `crafting/InfusionRecipe.java` | 11 | 11 | 178–188 |
| `crafting/ShapedArcaneRecipe.java` | 11 | 11 | 189–199 |
| `crafting/ShapelessArcaneRecipe.java` | 10 | 10 | 200–209 |
| `internal/DummyInternalMethodHandler.java` | 13 | 13 | 210–222 |
| `internal/IInternalMethodHandler.java` | 13 | 13 | 227, 231–242 |
| `internal/WeightedRandomLoot.java` | 1 | 1 | 256 |
| `nodes/INode.java` | 6 | 0 | 259–264 |
| `research/IScanEventHandler.java` | 1 | 0 | 293 |
| `research/ResearchCategories.java` | 3 | 3 | 301–303 |
| `research/ResearchCategory.java` | 1 | 1 | 306 |
| `research/ResearchCategoryList.java` | 1 | 1 | 307 |
| `research/ResearchItem.java` | 36 | 36 | 334–369 |
| `research/ScanResult.java` | 1 | 1 | 408 |
| `visnet/TileVisNode.java` | 16 | 9 | 427–431, 433–443 |
| `wands/FocusUpgradeType.java` | 3 | 3 | 444–446 |
| `wands/IWandFocus.java` | 9 | 0 | 447–455 |
| `wands/IWandRodOnUpdate.java` | 1 | 0 | 456 |
| `wands/IWandTriggerManager.java` | 1 | 0 | 457 |
| `wands/IWandable.java` | 2 | 0 | 458–459 |
| `wands/StaffRod.java` | 6 | 6 | 474–479 |
| `wands/WandCap.java` | 13 | 13 | 480–492 |
| `wands/WandRod.java` | 18 | 18 | 493–510 |

## Contract unclear — 70 declarations intentionally left without new member Javadoc

These are unresolved **full contracts**, not assertions that the method's basic purpose cannot be inferred. Known units or happy-path behavior alone do not establish a truthful null/ownership/side-effect contract for every implementation. The following ordinal groups are disjoint and exhaust the 70 remaining entries; the original inventory gives each full signature.

| Ordinals | Declarations | Why no complete contract was asserted |
| --- | --- | --- |
| 12–15, 265 | `IGoggles.showIngamePopups`, `IRepairableExtended.doRepair`, `IRunicArmor.getRunicCharge`, `IWarpingGear.getWarp`, `IRevealer.showNodes` | Implementor callbacks have no uniform nullable stack/player/wearer policy or declared side/failure boundary. In particular, repair success must not be described as guaranteed payment or successful durability mutation. |
| 42–44 | Helper username research/discovery queries | Mutable handler delegation and weak server/name-resolution lifecycle do not establish a stable offline/online/null-result/ownership contract. The installed handler's private server capture path and the native Player-scoped service must not be conflated. |
| 54–58 | Helper wand/inventory consumption and two warp mutators | Installed handler versus native service differ: wand payment takes raw amounts into centivis debit and ignores player/crafting flags in the installed implementation, while other native paths perform pricing/authority/sync. No single discount, side, null or transactional policy is safe to promise for the replaceable facade. |
| 105–109, 111, 113 | Seven nondeprecated `IAspectContainer` methods | Ownership/null/signed-amount rules differ by implementation; e.g. native TileNode rejects containment queries that NodeBase answers. A generic all-or-nothing or defensive-copy promise would be false. Deprecated list methods received explicit replacement notes separately. |
| 114–115 | Both `IEssentiaContainerItem` methods | Interface does not settle null/empty stack/list handling or whether ownership is copied or retained by providers; component writes are implementation-specific. |
| 116–127 | All twelve `IEssentiaTransport` methods | Nullable face/aspect meaning and suction-versus-stock semantics need provider-specific contracts; must not label suction numbers centivis or millibuckets, or assume every null face means unrestricted access. Callback mutation/failure boundaries are not declared by this interface. |
| 128 | `IBauble.getBaubleType` | Abstract provider hook has no default or declared null stack/result policy. Documented the five actual default methods instead of projecting their permissiveness onto this method. |
| 165–171 | All seven `IArcaneRecipe` methods | Concrete shaped/shapeless contracts are now documented, but arbitrary addon implementations have no enforced common nullable-player/input/cost/output ownership contract. Native assembly copies while compatibility output access is live; do not infer a blanket interface guarantee. |
| 172 | `IInfusionStabiliser.canStabaliseInfusion` | Native caller establishes a level/block position, but does not define a general addon null, thread, side-effect or callback-failure contract. Avoided inventing one from the default native block implementations. |
| 259–264 | Six audited `INode` type/modifier/base members | Native TileNode and standalone NodeBase differ in validation/state notification, and capacity may be below current stock. The interface's existing short-amount prose is not a reliable universal null/range contract. |
| 293 | `IScanEventHandler.scanPhenomena` | Registration order is documented at type level, but callback null inputs/returned witness ownership/failure and side effects are not specified uniformly for addons. Do not equate a callback result with an accepted authoritative scan. |
| 428, 435, 438, 440–443 | `TileVisNode.isSource`, `consumeVis`, `getParent`, `getRootSource`, `setParent`, `removeThisNode`, `tick` | Source claims, raw network units, graph validation and mutation/callback/reentrancy frames need a unified contract, not seven simplistic read-only/always-debit descriptions. Parent/root resolution invokes node metadata and differs between server graph and client hints. Existing source-debit comments are not a full public contract. |
| 447–455 | Nine audited `IWandFocus` methods | No native dispatcher consumes this interface, so stack-free cost/use callbacks cannot be assigned the actual ItemFocusBasic stack/hand contract. Deprecated the type with a working replacement instead of fabricating callback behavior. |
| 456–459 | `IWandRodOnUpdate.onUpdate`, `IWandTriggerManager.performTrigger`, both audited `IWandable` use/stop callbacks | Addon implementations have no declared universal null, side, exception, return/payment or mutation contract. Native material callbacks and compatibility rod registration are different paths; a registration alone does not install an addon callback in the bounded native catalogue. |

## Seventeen legacy-shape notes

The audit's L01–L17 are **17 findings**, not 17 individual methods: several cover overload families. L15/L16 are later TC6 shapes, not original TC4 1.7.10 features. L18 (functional UUID node identity) is not an outstanding legacy-shape defect and is not counted here.

| Finding / Javadoc location | Then → now → 1.21.1 replacement |
| --- | --- |
| **L01** `ThaumcraftApi.enchant*` (all five fields) | 1.7.10 numeric enchantment identity → mutable -1 sentinel with no native identity → adjacent `ENCHANTMENT_*` ResourceKeys resolved through level registry access. Added individual `@Deprecated` and replacement tags. |
| **L02** `WandTriggerRegistry` and six existing deprecated overloads | Block+metadata selection → ignored metadata; dispatch substitutes default state → exact `BlockState` registration/query/dispatch, or explicit whole-Block forms. Corrected the misleading suggestion that native blocks cannot have state variants. |
| **L03** `ItemApi.getItem/getBlock` | Metadata subtypes → damage points only on damageable item forms → distinct native namespaced IDs, BlockState properties and stack data components. Null/missing-name and lookup/logging behavior described. |
| **L04** `ThaumcraftApi.registerObjectTag` metadata family | Subtype registration → native damage keys/groups, not component predicates → native item identities/TagKey registration. Does not claim a general component-sensitive registration extension exists. |
| **L05** `ScanResult` and constructor/factories | Old item ID/subtype and numeric dimension locators → runtime registry IDs/damage plus stack/position/dimension witnesses → `ScanResult.item(ItemStack)` and nodePosition/nodeDimension; never persist runtime numeric IDs. |
| **L06** numeric `IInternalMethodHandler.generateVisEffect`, including dummy override | Forge dimension ID plus block endpoints/RGB → inert default/installed behavior → `VisNetHandler.generateVisEffect(ServerLevel,BlockPos,BlockPos,Aspect)`. Existing deprecation retained; dummy override now carries replacement guidance too. |
| **L07** native-looking `IInternalMethodHandler` defaults | Added native adaptations beside the old service shape → installed handler still inherits false/null/no-op native overloads → actual implemented common `InternalMethodHandler.INSTANCE` services, world-scoped helper lookup or VisNetHandler effect path, named per default. Marked inert native overloads deprecated. |
| **L08** helper `getBonusObjectTags/generateTags` | World-free original enrichment/derivation → installed bonus merely copies and generation reads direct/catalogue registrations → `getObjectAspects(ItemStack,Level)` for scan-complete results. Explicitly no equivalent public level-aware bonus/generate facade here. |
| **L09** helper `rayTraceIgnoringSource` | Original fluid/miss flags → block-shape traversal, no fluid-shape testing, unobstructed result null even with reportMiss → native `Level.clip`/`ClipContext` when those semantics are required. |
| **L10** `InfusionRecipe` constructor | Object-array NBT patch → five recognized original keys, no arbitrary NBT transform → named `InfusionResultPatches`/data-component changes; unsupported patch names reject with IAE. |
| **L11** `InfusionRecipe.areItemStacksEqual` | OreDictionary fuzzy matching → fuzzy argument completely ignored; directional component/damage comparison → native Ingredient/TagKey matching for equivalence. |
| **L12** `CrucibleRecipe` constructor | Object catalyst including old alternatives → parser rejects ArrayList even though hashing retains that branch → native Ingredient or resolvable namespaced item-tag string; supported inputs/retention/rejections documented. |
| **L13** world-free `ResearchPage(ItemStack)` | Global furnace lookup → deferred/display-only page with null output → `ResearchPage(ItemStack,Level)` and loaded recipe manager. Deprecated world-free constructor, without deleting it. |
| **L14** `ScanningManager` / registration | Later generic scannable compatibility registry, not TC4's original scan dispatch → isolated storage with no native thaumometer caller → `ThaumcraftApi.registerScanEventhandler` for phenomena only; no generic-target equivalent is wired. Deprecated type/registration. |
| **L15** `TheorycraftManager` / registrations | TC6 cards/aids, not TC4 1.7.10 aspect/hex research → isolated HashMap storage, first-name-wins, no ordered iteration or table consumer → TC4 `ResearchItem`/`ResearchPage`/research notes. Deprecated type/registration hooks. |
| **L16** `ResearchEntry` / stage, reward, addendum setters | TC6 staged DTO model, not original TC4 progression → scalar snapshot with selected aliases, setters only store DTO references → actual `ResearchItem` pages/triggers. Deprecated unsupported progression setters and corrected the “never drift apart” claim. |
| **L17** `IWandFocus` type | Legacy no-stack focus contract → unused by native dispatch → extend `ItemFocusBasic` with actual stack/hand-aware methods and supported FocusUpgradeData path. Deprecated type; did not invent per-callback contracts. |

Also added replacement notes for all six already-deprecated multi-aspect node/container methods (P12–P17): single-aspect operations are the replacement, caller loops are not atomic, and native/standalone node list methods return false rather than promising an all/any operation. TC6 alias interfaces were not indiscriminately deprecated.

## Verification and limitations

- Java 21 compiler-tree parse of all API files: **pass**; every audit entry matched a current declaration, with overload-aware parameter matching.
- Stripped Java comments and only the allowed `@Deprecated` annotations, then compared token sequences with baseline: **pass across all 40 changed Java files**. This checks executable tokens, strings, types, signatures, modifiers and field initializers; no behavioral edits were hidden among the comments. Deprecation metadata/compiler warnings intentionally change.
- Required `javac` classpath included the actual cached Mojang-remapped merged 1.21.1 JAR under `~/.gradle-leader/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/`, `common/build/classes/java/main`, and cached dependency JARs (excluding source/Javadoc archives).
- Focused `javac -proc:none -encoding UTF-8 -sourcepath ''` of `BlockCoordinates`, `WorldCoordinates`, `ItemApi`, `ItemRunic`, `IRunicArmor`: **pass**, output to this worktree's `common/build/classes/java/main`.
- Full 1,445-source common `javac` check: **blocked / failed**, not a build pass. This clean worktree had no common classes. Cached Architectury dependencies still reference intermediary `net.minecraft.class_*` names against the remapped Minecraft JAR; the bare jar also lacks project access-widener treatment (e.g. Projectile constructor access). The final attempt reports 742 errors; the same dependency/mapping categories were observed before final source edits. No stubs or gameplay fixes were introduced to mask this. Logs are ignored `.tmp/e3-final-focused-javac.log` and `.tmp/e3-final-full-javac.log`.
- No Gradle/NeoForge build, gameplay test, client run or runtime compatibility pass is claimed. Parent integration needs the normally remapped/widened common classpath for a full compile.
