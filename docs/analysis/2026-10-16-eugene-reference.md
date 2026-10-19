# Найденный пользователем reference для следующего контента

https://github.com/EugeneHeisst/thaumcraft-port — изучен read-only16.10.2026.
Checkout: `../work/references/EugeneHeisst-thaumcraft-port`, main/tag `v0.17.169`,
commit `a9b7a613c9c3e5a4418f30e0cf38553ac328b1e6`.
Target: Minecraft1.21.1/Java21/NeoForge21.1.256, ModDevGradle2.0.148,
Gradle9.2.1/Parchment2024.11.17. Наши версии/Architectury build не заменялись.

Самое полезное для следующей игровой unit — `src/main/java/dev/eugheist/tc4modern/`
`ShardOreBlock.java`, `ShardOreFeature.java`, `AlchemyContent.java`,
`CinnabarAmberFeature.java` и соответствующие worldgen/biome_modifier JSON.
Следующая естественная цепочка: infused ores → добываемые shards → таумометр.
Однако depth-weighted аспекты, deepslate/новый диапазон высот и увеличенные
cinnabar attempts — изменения донора, не TC4 specification. Значения/поведение
сверять с original oracle; Fabric требует отдельного biome integration.

Также полезны API-подходы в `ArcaneWorkbench{Block,Entity,Menu}.java`,
`ArcaneRecipes.java`, `Crucible{Block,Entity,Recipes}.java`, `AuraNodeFeature.java`,
`BasicWandItem.java`. Namespace `tc4modern`/CUSTOM_DATA не заменяет наши IDs/components.

Это не полный воспроизводимый публичный source port: build подключает внешние
`ported-src`, `ported-resources`, `tc4-local-resources.zip`;81 из87 классов manifest
отсутствуют, из108 явно загружаемых arcane JSON опубликованы85. Отсутствуют, среди
прочего, `ResearchManager`, `ResearchTableBonus`, `CrucibleState`, `VisStorage`.
Build/бинарный release этого reference не запускались, runtime verdict не делался.

Код/ресурсы оттуда пока не копировались. `LICENSE.txt` сохраняет Azanor ARR;
MIT относится к MDK шаблону, не автоматически к gameplay/TC4 assets. Literal reuse
потребует проверки прав и provenance. Original TC4 4.2.3.5 остаётся спецификацией.
