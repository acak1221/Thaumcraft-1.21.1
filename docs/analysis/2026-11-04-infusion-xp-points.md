# Enchantment infusion consumes experience points

The owner requested the same fixed experience-point policy for enchantment
infusion as for focal upgrades. Original TC4 computes a nominal level price as
`max(1, firstLevelMinimumCost / 3) * (1 + current target enchantment level)`.
The original matrix removes one whole level from the first nearby player with
experience during each XP payment cycle. Its specification is recorded in
[the original enchantment-infusion audit](2026-10-20-enchantment-infusion.md),
including `InfusionEnchantmentRecipe` and matrix lines 421–451.

New rituals convert that nominal level price into the cumulative points required
to reach it from zero under Minecraft 1.21.1. A nominal two-level price becomes
16 points; 12 levels become 216 points. The price is independent of the paying
player's level. The matrix still collects XP before essentia and physical
components, and a player with a partial level can contribute their remaining
whole points. Spendable XP comes from the current level and rounded bar progress,
not the historical `totalExperience` counter. Native `giveExperiencePoints`
performs the debit. The altar continues to charge creative players, as the
original did; the focal manipulator's separate creative exemption does not apply.

The original approximate payment duration is retained through a fixed installment
computed from the full point price. The matrix finds the greatest nominal level
threshold within that price, then rounds points divided by that number of cycles
upwards. Each cycle pays at most that installment, the unpaid debt and the nearby
player's available points. Full installments therefore keep approximately the
old number of one-second payment cycles; partial contributions can take longer.
The installment is frozen for the session and survives reload instead of being
recomputed from a shrinking debt. Original damage, shortage behavior and the
transient cycle-delay reset on load are retained.

## Session format and compatibility

New saves store the remaining point debt in `recipexp`, identify it with
`RecipeXPUnit = "points"` and persist the positive integer
`RecipeXPPerCycle`. Reload restores both values exactly without rematching the
recipe or charging an already paid installment again.

Earlier native saves have neither new field and store only unpaid nominal levels.
Only that remaining level debt is converted to cumulative points; the full recipe
is not priced again and previous payment is not reconstructed. The next save
writes the explicit point format, so the migration runs once. This concerns
earlier 1.21.1 port saves, not import of original 1.7/1.12 worlds. Negative debt,
unknown units, incomplete/wrongly typed new fields, nonpositive installments and
legacy debts whose point price cannot fit an integer retain their original NBT
and disable gameplay through the existing rejected-data owner. A legacy crafting
enchantment session must explicitly contain its integer debt; a missing field
cannot silently become a zero-price session.

`getRemainingXPPoints()` reports currency. The deprecated
`getRemainingXPLevels()` compatibility method reports the ceiling of remaining
debt divided by the frozen installment, an approximate remaining cycle count.
It no longer reports a spendable level price.

Native `xpLevels()` and the public virtual `calcXP()` retain nominal level units.
New native `xpPoints()` and public `calcXPPoints()` return checked integer point
prices. The default point API converts the virtual nominal method, preserving
existing add-on overrides. Programmatic preparation calls the virtual point
method and freezes its answer in `PreparedInfusion.xp`; explicit point overrides
are honored as well. Overflow is rejected before preparing/admitting a ritual.
The API view of a native recipe now derives its nominal base from that recipe's
stored minimum cost, so it agrees with the native price rather than substituting
the modern enchantment definition's minimum.

Book, EMI and JEI prices identify point units. Programmatic recipe viewers retain
their existing native base view; custom add-on virtual prices govern actual
preparation but are not newly projected into that native display record.
Essentia, physical components, instability, enchantment tier progression and
research gates are unchanged. Ordinary stack/runic infusion, focal prices and
other enchantment systems receive no new cost policy in this change.

## Verified result

The final combined invocation completed **BUILD SUCCESSFUL in 7m28s**:
`:common:test :fabric:clientTestClasses :fabric:build :fabric:runGameTest :fabric:runInfusionXpClient`,
with the six focused filters listed in the receipt. This ran after the final missing-legacy-debt guard and integration of upstream
`72bbd52b` / `58dc296f`; those changes and both new proof records are preserved.
The initial full pass also succeeded, but the merged run is the final acceptance.
A later upstream `69e35c85` changes only NeoForge test wrappers and the required
GameTest catalogue. It was also integrated; a scoped diff confirms no change to
Fabric/common runtime sources, compatibility classes or target build inputs, so
the accepted Fabric binary and its hash are unchanged.

| Check | Result |
| --- | --- |
| Common JUnit, including cost boundaries, virtual overrides, overflow and session migration | 2,924 total, 2,919 passed, five existing oracle skips, zero failures/errors |
| Fabric build and production/test isolation | PASS |
| Six focused dedicated-server GameTests | PASS: original/custom/modern enchantments, API ritual, partial/empty bars, stale total, creative charge, reload/migration and completion |
| Focused native client | PASS, stage 14, two complete natural-tick Survival rituals from real client wand interaction packets |
| Native book and inspected client frames | All six RU/EN/HUD frames inspected by root and two reviewers |
| EMI/JEI price details and JEI full-detail hover | Compiled; native recipe-viewer clients not rerun for this change |
| Resource verification | PASS, zero missing references, 52 composed sites and 294 files checked |

Accepted client run: `infusion-xp-2026-11-04T06-28-06.796792892Z`.
Sharpness I → II costs exactly **16 points**: low payer 32 → 16 (native level 3 → 2),
high payer 1412 → 1396 (native level remains 30). Native XP packets synchronize
client/server level, progress and total. Each ritual then consumes exactly eight
Praecantatio, sixteen Telum and both physical components, preserves damage 7,
and raises the retained sword to Sharpness II once. The first result remains in
the inventory during the second ritual. A further forty natural ticks produce no
repeated reward or debit.

The book frames directly show «Острота II — 16 очк. опыта» and
“Sharpness II — 16 XP points”, without clipping. World frames show the level/bar,
altar, glinting sword and ritual/completion effects. The completion camera crops
the overhead matrix at the viewport edge; the central reward and HUD are clear.
Exact raw balances, enchantment identities and finite-resource totals come from
the paired native server/client ledger, not from reading numeric points out of
screenshots.

The supplied complete altar, stabilization skulls, research, wand, components,
essentia and XP baselines are declared fixtures. No fresh survival acquisition,
pillar formation, remote multiplayer or NeoForge acceptance is claimed. The
preliminary core run passed before the final strict guard; it is superseded by
the final combined run above. Transient logs/worlds are ignored under `.tmp` and
`fabric/run-infusion-xp-client`.

The [receipt](../porting/infusion-xp-points.json) contains exact cases, balances,
frame hashes, limitations and the artifact checksum. Fabric JAR:
`Thaumcraft-fabric-0.1.0-alpha.24.jar`, 16029717 bytes,
SHA-256 `3cc760e2ca30443a23b5c118c30f661d49ca61202096c6ad140511c238752e2b`.

[Russian price](../images/infusion-xp-points/book-ru-sharpness-II-cost16-points.png) ·
[English price](../images/infusion-xp-points/book-en-sharpness-II-cost16-points.png) ·
[Low payer](../images/infusion-xp-points/level3-completed-sharpness-II-finite-resources.png) ·
[High payer](../images/infusion-xp-points/level30-completed-sharpness-II-finite-resources.png).
