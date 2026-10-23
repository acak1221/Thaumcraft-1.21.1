# Public API blocker fixes — c1fix

Baseline: `d2ca5717`, branch `agent/c1fix`, 2026-10-23.

## E01 — all aspects cache ownership

Regression: `ApiCacheOwnershipTest.allAspectsFirstAnswerAndCacheHitsAreCallerOwned`.
Clear the first answer's public map, then check the next answer's full contents and amount;
poison a cache-hit answer's amount and check a third answer. Both the list and map must be independent.
Fix: return a real `AspectList` copy on cache misses and hits; retain the original cached list.
The existing `AspectList.copy()` creates a new list and copies all entries into its independent map.

## E02 — compound aspects cache ownership

Regression: `ApiCacheOwnershipTest.compoundAspectsFirstAnswerAndCacheHitsAreCallerOwned`.
The same mutation sequence checks compound-only contents and amounts.
Fix: the same copy boundary, without changing cache keys, size or stored values.

## E03 — first recipe answer cache ownership

Regressions (Fabric GameTests, real recipe manager/player/research):
`apiRecipeFirstAnswerCannotPoisonLaterAnswers` and
`apiRecipeFirstAnswerCannotPoisonElementTypes`.
Both force a cache miss for the real enchanted fabric output. One replaces the first answer's
research/page values; the other inserts incompatible element types. The next lookup must remain
visible, preserve the original research/page, and allow the documented `String`/`Integer` casts.
Cache-hit answers are mutated again to verify the third lookup too. GameTests explicitly install
their level's recipe manager and registry access. A scoped handler implements only the legacy
username visibility seam from the mock player's real knowledge: mock players lack logged-in
sessions, so the normal name-to-online-player bridge cannot see them. The original handler,
manager and registries are restored and the key cache is cleared in `finally`.
Fix: clone the first successful answer before handing it through the visibility gate, matching
the existing cache-hit boundary. Cache contents and miss caching are unchanged.

## L06 — explicit inert compatibility overload (option b)

Regression: `ApiCacheOwnershipTest.numericDimensionEffectIsExplicitlyDeprecatedOnTheInstalledHandler`
checks deprecation on the actual installed implementation's numeric method and its interface contract.
It covers the interface, the API's default installed `ThaumcraftInternalMethods` implementation,
and `InternalMethodHandler.INSTANCE`; the explicit override in `ThaumcraftInternalMethods` is
also deprecated and documents the replacement (Java does not inherit method annotations).

Oracle: `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle/`,
`thaumcraft/common/lib/InternalMethodHandler.java:27-30` delegates to
`Utils.generateVisEffect(int, ...)`; `thaumcraft/common/lib/utils/Utils.java:294-304`
throttles by dimension/position and sends `PacketFXVisDrain` using Forge's
`NetworkRegistry.TargetPoint(dim, x, y, z, 64.0)`.

The numeric ID was a Forge dimension identity, not an ordinal of modern registered worlds.
The compatibility method has no server argument and neither handler has an authoritative legacy-number
to `ResourceKey<Level>` mapping. `ThaumcraftInternalMethods` has a weak server reference for online-name
knowledge lookup, but that does not provide a dimension-ID mapping. The native `ServerLevel` overload
on `InternalMethodHandler` is reachable when the caller supplies
a level, but cannot be selected honestly from this integer. Do not guess overworld/nether/end IDs
or use a global server that breaks multi-server isolation. Retain the signature and inert behavior,
mark it `@Deprecated`, explicitly document that no effect is emitted, and name
`VisNetHandler.generateVisEffect(ServerLevel, BlockPos, BlockPos, Aspect)` as replacement.

## Allocation / hot paths

Searched active `common` and `fabric` sources: none of these three getters has a production caller;
there is no current per-tick hot path. The retained, uncompiled root `src` has a recipe GUI caller,
not evidence of a native ticking caller. Keep the existing amount/recipe caches and copy their
answers; never cache/reuse a caller-visible mutable copy. A real mutable `AspectList` snapshot needs
its own map; the recipe copy is only two immutable values. No cache shrinking or recomputation of
recipe lookup was introduced.

## Verification

Test-first evidence before production fixes:

- `.tmp-c1fix-red-unit-retry.log`: **3 tests, 3 failures**. E01 expected 48 aspects after clearing the
  first answer, got 0; E02 expected 42, got 0; L06 expected deprecation, got false.
- `.tmp-c1fix-red-gametest-final.log`: **2 required tests failed**. E03 value mutation failed
  `mutating the first answer cannot hide later answers`; incompatible-type mutation threw
  `ClassCastException: java.lang.Object cannot be cast to java.lang.String` on the next lookup.
- Initial fixture runs did not prove E03: they failed before mutation because a mock player's
  name was not visible through the online-name bridge. The scoped visibility fixture corrected
  this before the decisive red run. An initial overlapping Gradle resource-copy failure was
  retried serially; all decisive red and green executions were serial.

Final Java 21 / Gradle 8.12.1 results (private `.gradle-c1fix` home):

- `:common:test --tests thaumcraft.api.ApiCacheOwnershipTest --tests thaumcraft.api.ThaumcraftApiHelperTest`
  — **11 passed** (`.tmp-c1fix-green-focused-unit.log`).
- `:fabric:runGameTest -PgameTestFilter=apiRecipeFirstAnswerCannotPoisonLaterAnswers,apiRecipeFirstAnswerCannotPoisonElementTypes`
  — **2 passed** (`fabric/build/gametest-focused-results.xml`).
- `:common:test` — **2,522 passed, 0 failures/errors/skips**
  (`common/build/test-results/test/TEST-*.xml`, `.tmp-c1fix-green-common.log`).
- Unfiltered `:fabric:runGameTest` — **439 passed, 0 failures/errors/skips**
  (`fabric/build/gametest-results.xml`, `.tmp-c1fix-green-gametest.log`).

The root build configures the retained NeoForge project even for these commands; no NeoForge
build/test task was requested or run, and no NeoForge parity is claimed.
No new test constructs a `Block`.
