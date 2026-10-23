# Public API null/error compatibility review

Date: 2026-10-23. Branch: `agent/e1null`. Baseline: `974fde6a`.
Scope: the **20 groups N01–N20** in `API_QUALITY.md`, section 3 (baseline source line numbers).

## Policy and totals

* **(a) Provably unusable — 4 groups:** N02, N03, N05, N13. Replace accidental deep failures with argument-naming `IllegalArgumentException`; reject empty constructor tags that cannot support `getName()`. Do not turn rejected inputs into successful no-ops.
* **(b) Legitimate difference — 10 groups:** N04, N06, N07, N08, N09, N11, N15, N16, N18, N20. Document actual answers; no executable changes.
* **(c) Maintainer decision — 6 groups:** N01, N10, N12, N14, N17, N19. Document current behavior and retain it pending a compatibility decision.

Null rejection, an explicitly thrown NPE, different absence sentinels, permissive DTO construction, and an intentionally different read/write policy are not automatically bugs. In particular, returning a real theory balance for null knowledge type is questionable, but the read branch deliberately treats every non-OBSERVATION value alike; there is no evidence that existing callers cannot use that fallback. Likewise, accepting null in a mutable setter may be used to clear state before restoring it. Neither is normalized here.

## Complete disposition ledger

Paths below are relative to `common/src/main/java/thaumcraft/api/`.

| Group / baseline location | Class | Disposition and documented contract |
| --- | --- | --- |
| N01 `aspects/Aspect.java:75,121,130` | c | Setters still store null/empty/malformed values and do not re-key the registry; null/empty tags break `getName()`. Constructor tag rejection is handled only under N02. |
| N02 `aspects/Aspect.java:86,102` | a | Validate tag before default-image construction and primary registration: null/empty tag throws IAE naming `tag`. An empty registered aspect previously could not answer its name. Preserve image NPE, arbitrary component arrays, duplicate rejection and native resource-path validation. |
| N03 `aspects/AspectList.java:102–117` | a | List-taking add/merge now reject null `other` with IAE before iteration. Aspect-taking overloads remain null no-ops. No signed-amount normalization. |
| N04 `aspects/AspectList.java:102–128` | b | Add sums signed amounts without overflow validation; merge retains the maximum; reduce returns false for negative amounts; remove ignores negative amounts. Null Aspect handling remains unchanged. |
| N05 `aspects/AspectList.java:169`; `BlockCoordinates.java:109`; `WorldCoordinates.java:61` | a | Reject null NBT `tag` before clearing aspect state, in both overloads. Missing/wrong-type list fields still load empty, unknown aspects are dropped. Block coordinates retain null no-op / missing-field zero defaults; world coordinates retain whole-value Optional.empty refusal. |
| N06 `ThaumcraftApi.java:414`; listener/scanning/theorycraft registries | b | Scan-event registration explicitly throws NPE naming handler; the other three registration methods ignore null. Non-null handlers are not validated at registration. |
| N07 `ThaumcraftApi.java:420–454` | b | Simple null/empty stacks are ignored, null aspects become empty; grouped metadata null/empty arrays are ignored. Tag membership is snapshotted. Complex variants require a server context, reject missing items with IAE and null aspects with NPE after context validation. No-context variant resolves context first. |
| N08 `ThaumcraftApi.java:254`; infusion constructors | b | Facade returns null for null/unsupported output classes or null enchantment. Direct infusion constructor uses input sample for null/other output, rejects missing input and unknown array patch; direct enchantment constructor rejects null enchantment. Null aspects/recipe mean zero costs/components. |
| N09 `ItemApi.java:30`; `items/ItemsTC.java:29`; `blocks/BlocksTC.java:29` | b | ItemApi null/empty names return null. Required-name registries reject them with IAE. Document malformed qualified/plain-name distinction too; ItemApi is not uniformly forgiving. |
| N10 `items/ItemsTC.java:23`; `blocks/BlocksTC.java:23` | c | Preserve malformed namespaced ID => null versus malformed plain path => ResourceLocationException. ItemApi shares this parsing distinction (qualified failures may fall back to reflection). |
| N11 `ThaumcraftApiHelper.java:95,162,175` | b | areItemsEqual equates null/EMPTY and ignores components; itemMatches equates only two nulls and otherwise compares item/damage; component crafting comparison rejects any null and tests recipe-component subset, not item identity. Corrected the misleading preexisting missing-stack prose. |
| N12 `internal/IInternalMethodHandler.java:30`; dummy override | c | Default uses EMPTY even for invalid input; dummy always uses null. Callers must handle both; installed real handler uses EMPTY for invalid cells. |
| N13 `crafting/CrucibleRecipe.java:27` | a | Constructor rejects null result/tags with argument-naming IAE before conversion/hash construction; removal rejects null itags similarly. matches still returns false for null contents/catalyst, while null aspects in other recipe cost conversions still mean zero costs. |
| N14 `capabilities/NativePlayerKnowledge.java:100,132` | c | Write rejects null/non-OBSERVATION; reads treat null as THEORY, and null category/formula yields zero. Preserve valid existing balances. |
| N15 `research/ScanResult.java:28–37` | b | Item factory explicitly rejects null/empty with IAE; entity/node factories explicitly reject null with named NPE. DTO constructor still accepts null/inconsistent payloads; unbound node factory still permits null dimension. |
| N16 `WorldCoordinates.java:23–61` | b | Document null position/tile/copy => NPE, null/noncanonical dimension or invalid coordinates => IAE, distance-to-null => infinity, compare-to-null => NPE (Comparable contract), read-null/malformed => Optional.empty. No coordinate semantics change. |
| N17 `wands/WandCap.java:76,90`; `wands/WandRod.java:75,105` | c | Setters store null/malformed tags without re-keying; byTag returns null on absent queries but may throw on a null stored value/tag. Preserve mutable registry behavior. |
| N18 `aspects/AspectRegistryEvent.java:35`; `research/ScanningManager.java:26`; `aspects/AspectList.java:24` | b | Event post logs RuntimeException and continues (not Error); scanning passes arguments through and propagates failures; stack aspect constructor swallows Exception (not Error), retaining any entries already copied. |
| N19 `visnet/VisNetHandler.java:128–193` | c | Server/native predicate may throw ISE off-thread or without owner; node validity can propagate this/callback failures. Chunk query invokes world callback without thread validation; topology/lifetime reads are cached and return zero for null/untracked worlds without validation. |
| N20 `nodes/NodeBase.java:59` | b | Acceptance always reports capability true, including null. Insertion returns full remainder for null/nonpositive input; removal refuses it. Document rather than redefine the predicate. |

## Regression tests for category (a)

`common/src/test/java/thaumcraft/api/NullPolicyTest.java` contains six tests:

| Group | Test | Before-fix failure / required answer |
| --- | --- | --- |
| N02 | `aspectConstructorsRejectNullTagBeforeBuildingImage` | Default-image overload dereferences null before validation; require IAE naming tag across all four overloads. |
| N02 | `aspectConstructorsRejectEmptyTagWithoutRegisteringIt` | Empty tag successfully registers an object whose getName fails; require IAE and no registration. |
| N03 | `listOverloadsRejectNullWithArgumentNameAndPreserveContents` | Both list overloads dereference null; require named IAE and unchanged amounts. |
| N05 | `nullNbtIsRejectedBeforeClearingContentsInBothOverloads` | Loading clears amounts then throws NPE; require named IAE and preserve state in both overloads. |
| N13 | `crucibleConstructorRejectsNullResultAndTagsAtBoundary` | Hash construction dereferences result/tags; require named IAE at entry. |
| N13 | `crucibleRemovalRejectsNullContentsAtBoundary` | putAll dereferences null contents; require IAE naming itags. |

Execution evidence (Java 21, Gradle 8.12.1, isolated `.gradle-e1`):

* Before executable fixes: `./gradlew :common:test --tests thaumcraft.api.NullPolicyTest --console=plain` — **6 tests, 6 failed**. Five tests reported unexpected deep NPEs; the empty-tag test reported no exception. Both the initial run and a foreground confirmation reproduced all six failures. Local ignored logs: `.null-policy-red.log`, `.null-policy-red-confirm.log`.
* After fixes: the same focused command — **6 tests passed**, BUILD SUCCESSFUL. Local ignored log: `.null-policy-green.log`.
* Final `./gradlew :common:test --console=plain` — **2,531 tests, 401 suites, 0 failures, 0 errors, 0 skipped**, BUILD SUCCESSFUL. Counts are summed from the generated JUnit XML, not inferred from the expected baseline. Local ignored log: `.null-policy-full.log`.
* `git diff --check` passed. Native compilation emitted the two existing mixin mapping warnings; no new compile error occurred.

These are Java boundary/contract checks only, not gameplay acceptance or loader parity evidence. Gradle configures the loader projects as part of the existing multi-project build, but no loader build, GameTest, client or server task was requested.

## Maintainer decisions (not implemented)

| Group | Option 1 and cost | Option 2 and cost |
| --- | --- | --- |
| N01 | Keep permissive setters: preserves clearing/retuning addons; consumers must validate before name/render use and registry identity remains separate. | Validate setter tags/chat colors: early failure but breaks addons clearing or temporarily retuning values; requires agreed color grammar, migration and registry identity policy. |
| N10 | Keep qualified/plain parsing asymmetry: no addon break, but callers must handle both null and ResourceLocationException. | Introduce a consistently forgiving or throwing parser, preferably new named methods: old-method normalization breaks either null checks or exception handling; new methods add API surface/deprecation work. |
| N12 | Keep both sentinels: preserves dummy users, but every consumer must handle null and EMPTY. | Standardize EMPTY via a versioned handler contract: breaks code testing null or interpreting EMPTY specially; third-party overrides still require adaptation. |
| N14 | Keep null-as-THEORY reads: preserves fallback readers, but accidental null type can look like genuine knowledge. | Explicitly reject null on reads (or add a strict reader): changing existing methods breaks fallback users; new method requires migration and must select IAE versus zero deliberately. |
| N17 | Keep permissive tags: preserves temporary clearing/retuning and registry shape, but lookup remains vulnerable to stored nulls. | Validate setters or tolerate nulls in lookup: setter rejection breaks clearing addons; lookup tolerance changes an existing exception into an answer and needs a rule for null query matching and registry re-keying. |
| N19 | Keep per-method validation: preserves cheap cached reads and existing callback/thread behavior; callers must honor documented boundaries. | Enforce one server-thread policy or add explicit snapshot APIs: newly thrown exceptions break off-thread readers; snapshot APIs require lifecycle/thread ownership design and synchronization review. |
