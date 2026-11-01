## Thaumcraft 4 Reload 0.1.0-alpha.8

Minecraft **1.21.1**, Java **21**. This alpha includes production jars for **Fabric and NeoForge** and all 52 fixes from the two repository reviews.

### Changes

- Prevent Earthmover slab duplication and preserve tool enchantments, protection checks and XP when the Primal Crusher mines adjacent blocks.
- Preserve items and XP when mirror, traveling trunk or infernal furnace entity delivery is cancelled or reentered; retain Order trunk slot positions.
- Restore intrinsic flight permissions after Hover Harness use; fix doors, owned blocks, keys, phials and death-fluid bucket interactions.
- Correct crafting vis costs, equipment aspect bonuses, potion scan identities, research grants and recipe/tag reload caches.
- Fix Thaumonomicon recipe caching, live add-on previews, ingredient navigation and search updates; preserve runic HUD and warp events during resource reload.
- Add death-fluid rendering on both loaders, JEI component subtypes and NeoForge multi-tank fluid acceptance; correct machine output, merged XP and remaining GUI/FX defects.

### Install

Choose the jar for your loader and place it in `mods`:

- **Fabric:** Fabric Loader 0.16.14+, Fabric API and Architectury API 13.0.11+.
- **NeoForge:** NeoForge 21.1.259+ and Architectury API 13.0.11+.

Use a fresh world. Worlds from Minecraft 1.7.10 cannot be converted. `-sources.jar` files are for add-on development, not installation.

### Validation

The review fixes passed common JUnit (2797 tests, 5 existing skips), Fabric GameTest **542/542**, NeoForge GameTest **531/531**, NeoForge JUnit **15/15**, Python (143 tests, 9 existing skips), assets and scope checks. Original-JAR-dependent tests account for the skips. The release artifacts are rebuilt with the new version and checked for production/test isolation. Their gameplay classes match the tested builds; only the version and generated packaging names changed. Client frames, multiplayer and long survival playthroughs were not verified for these fixes.

Details: [first review fixes](https://github.com/acak1221/Thaumcraft-1.21.1/blob/v0.1.0-alpha.8/docs/analysis/2026-11-01-review-fixes.md), [second review fixes](https://github.com/acak1221/Thaumcraft-1.21.1/blob/v0.1.0-alpha.8/docs/analysis/2026-11-01-review2-fixes.md), [known deviations](https://github.com/acak1221/Thaumcraft-1.21.1/blob/v0.1.0-alpha.8/docs/audit/KNOWN_DEVIATIONS.md).

---

## Русский

Новая альфа для **Minecraft 1.21.1 / Java 21** с JAR для **Fabric и NeoForge** и всеми **52 исправлениями** двух ревью.

Устранены дюп плит, потеря предметов и XP, ошибки временного полёта, добычи Crusher, vis, аспектов, исследований и перезагрузки рецептов/тегов. Исправлены книга исследований, сохранение HUD при перезагрузке ресурсов, отрисовка жидкости смерти, JEI и несколько баков NeoForge.

Для Fabric нужны Fabric Loader 0.16.14+, Fabric API и Architectury API 13.0.11+. Для NeoForge — NeoForge 21.1.259+ и Architectury API 13.0.11+. Выберите один JAR под свой загрузчик и положите в `mods`. Создайте свежий мир; миры 1.7.10 не конвертируются.

Код исправлений прошёл GameTest **542/542 на Fabric** и **531/531 на NeoForge**, unit-тесты и проверки ресурсов. Клиентские кадры, мультиплеер и длительное выживание этого набора не проверялись.

Thaumcraft and its original material belong to Azanor. This unofficial fan port is not endorsed by the original author. See [THIRD_PARTY_NOTICES.md](https://github.com/acak1221/Thaumcraft-1.21.1/blob/v0.1.0-alpha.8/THIRD_PARTY_NOTICES.md).
