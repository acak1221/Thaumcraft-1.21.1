## Thaumcraft 4 Reload 0.1.0-alpha.9

Minecraft **1.21.1**, Java **21**. This alpha completes aura nodes in jars.

- Capture a node with the original 26 glass / nine wooden slab structure and the NODEJAR research. The wand pays 70 vis of each primal, with normal equipment modifiers.
- Watch the glass jar and its node shrink over one second. The stored node remains visible without goggles.
- Break, carry, save and place the filled jar without losing its aspects, node type, quality or identity.
- Release the node with a wand for free. Correct the invalid block variant and filled-item codec crash.
- Respect protection vetoes and preserve paid resources through rejected or reentrant native block lifecycle callbacks. Retain the original 75% quality-degradation chance on capture.

Choose the production jar for **Fabric** or **NeoForge**. Fabric requires Loader 0.16.14+, Fabric API and Architectury API 13.0.11+. NeoForge requires 21.1.259+ and Architectury API 13.0.11+. Source jars are for development. Use fresh 1.21.1 worlds; 1.7.10 worlds cannot be converted.

Validation results are recorded in [the implementation report](https://github.com/acak1221/Thaumcraft-1.21.1/blob/v0.1.0-alpha.9/docs/analysis/2026-11-01-node-jar.md). The animation and resource reload were inspected in a running Fabric client using a supplied creative fixture. Multiplayer, survival acquisition and the NeoForge client were not replayed.

## Русский

Доработан **узел ауры в банке**: захват из стеклянной конструкции, видимое сжатие за секунду, сохранение содержимого при переносе и бесплатный выпуск жезлом. Исправлены падение при выпуске, потеря аспектов и ошибка сохранения заполненной банки. Добавлены проверки защиты территории и сохранности ресурсов при отмене или повторном входе в обработчики блока. Исходные цена и шанс ухудшения узла сохранены.

Скачайте обычный JAR для своего загрузчика, положите в `mods` и установите перечисленные выше зависимости. Анимация осмотрена в клиенте Fabric; мультиплеер, получение в выживании и клиент NeoForge не перепроходились.

Thaumcraft and its original material belong to Azanor. This unofficial fan port is not endorsed by the original author. See [THIRD_PARTY_NOTICES.md](https://github.com/acak1221/Thaumcraft-1.21.1/blob/v0.1.0-alpha.9/THIRD_PARTY_NOTICES.md).
