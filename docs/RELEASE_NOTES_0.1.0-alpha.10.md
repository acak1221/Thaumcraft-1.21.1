## Thaumcraft 4 Reload 0.1.0-alpha.10

Minecraft **1.21.1**, Java **21**. This release corrects the Thaumometer and the complete bundled Thaumonomicon after original-data and live-client audits.

- Restore the Thaumometer screen's filtering, lit additive aspect glyphs/text, original aspect order, independent hand animation and centred dropped model.
- Restore 25 missing research icons in enabled chapters, six resolvable conditional icons and eight missing recipe/construct pages.
- Correct eight original recipes: Thaumometer, bone wand rod, clay golem, tiny visor/armor, essentia mirror, fertility lamp and eldritch eye.
- Restore transparent vis rows, matching advanced-golem previews, typed phial previews and correct Russian bold text wrapping.
- Restore original item sprites, multilayer foci/golems/labels and complete research table, deconstruction table, brain jar, flux scrubber, alembic and reservoir models. Preserve them when assets are regenerated.

Live Fabric validation covers all **273 available recipe descriptors**, 415 recipe variants per language, 201 research entries/icons, 48 aspects and resource reload in English and Russian: **647 + 645 captured views**, zero reported errors or overflowing text lines. Two virtual research icons and the disabled optional silver cap are intentionally empty. Conditional Silver/Lead/Tin chapters without native items remain disabled.

Choose the production JAR for Fabric or NeoForge. Fabric needs Loader 0.16.14+, Fabric API and Architectury API 13.0.11+. NeoForge needs 21.1.259+ and Architectury API 13.0.11+. Source JARs are for development. Fresh 1.21.1 worlds only; no 1.7.10 conversion.

Checks: common JUnit2846 (5 existing skips), Fabric GameTest559/559, NeoForge GameTest538/538, NeoForge JUnit15/15, Python155 (9 existing skips), both production builds/isolation and asset/scope checks passed.

Validation and exact coverage: [audit report](https://github.com/acak1221/Thaumcraft-1.21.1/blob/v0.1.0-alpha.10/docs/analysis/2026-11-01-thaumometer-book-review.md), [all recipe/icon records](https://github.com/acak1221/Thaumcraft-1.21.1/blob/v0.1.0-alpha.10/docs/porting/thaumonomicon-visual-audit.json). Client inputs and knowledge were explicit supplied fixtures, not survival acquisition. NeoForge client and multiplayer were not replayed.

## Русский

Проверены таумометр и весь стандартный Таумономикон. Исправлены отрисовка экрана/аспектов, пустые значки, пропущенные схемы, восемь неверных рецептов, чёрный фон символов vis, отображение улучшенных големов и перенос жирного текста. Восстановлены исходные слои, цвета и детали предметов.

В настоящем клиенте Fabric просмотрены все **273 доступные страницы с рецептами**, **415 вариантов на язык**, значки и обе локали; снято **647 EN + 645 RU кадров**, проверена перезагрузка ресурсов. Все выводимые строки помещаются в страницы. Выберите обычный JAR своего загрузчика и установите зависимости, перечисленные выше.

Thaumcraft and its original material belong to Azanor. This unofficial fan port is not endorsed by the original author. See [THIRD_PARTY_NOTICES.md](https://github.com/acak1221/Thaumcraft-1.21.1/blob/v0.1.0-alpha.10/THIRD_PARTY_NOTICES.md).
