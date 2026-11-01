# Thaumcraft 4 Reload 0.1.0-alpha.11

Исправлен поставленный расщепитель: таумометр на столе больше не увеличен в1.6раза и не перекрывает символ результата. Восстановлены исходные Forge ENTITY размер, высота и поворот. У символа исправлены перевёрнутый V и исходное аддитивное смешивание; освещение lightmap сохранено. Исправление общей модели также действует в инвентаре и Таумономиконе. PNG стола соответствует сохранённому оригиналу и не перекрашивался.

Живая проверка Fabric:13 итоговых кадров, виды с разных сторон, настоящий разбор предмета и GUI, темнота, resource reload. Common JUnit2847 (5 прежних skips), Fabric build/isolation, Python155 (9 прежних skips), assets0 missing — PASS. NeoForge build/isolation и его15 JUnit — PASS. NeoForge client и survival acquisition не проверялись.

[Отчёт, исходная цепочка и кадры](analysis/2026-11-01-deconstructor-world-review.md).

Fixed the placed deconstruction table: restore the original Forge ENTITY helper so its scanner has the correct size, height and rotation, leaving the reward glyph visible. Correct the reward glyph's V coordinates and additive blending while retaining inherited light. The shared item model also benefits in inventory and the Thaumonomicon; original table pixels are preserved.

Validated in the actual Fabric client with native deconstruction, GUI, darkness and resource reload. Both loader builds and production isolation checks pass. NeoForge client and survival acquisition were not replayed.
