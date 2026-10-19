# Два дополнительных проекта: адресная польза для TC4 1.21.1

По запросу пользователя два независимых read-only agents проверили **код**, не
только README. Ни build scripts, ни JAR не исполнялись; donor sources/oracle не
изменялись, code/assets не копировались. Pins добавлены в `porting/source-lock.json`,
происхождение/ограничения — в `THIRD_PARTY_NOTICES.md`. Это аудит референсов, не
runtime verdict и не замена нашего принятого checkpoint.

## Выбор основного референса

**TC4 Reforged полезнее первым: отдельная ветка уже использует наш MC1.21.1/Java21.**

| Проект / закреплённая ветка | Фактическая платформа | Права и доказательства |
|---|---|---|
| [Reforged mc/1.21.1](https://github.com/notlikethiss/thaumcraft-4-reforged/tree/d1e64cc527a8fa0c99ae738cc9c0bd0370d4bd7e) | NeoForge21.1.256, Java21, ModDevGradle2.0.148, Gradle9.2.1, Mojmap/Parchment2024.11.17; TerraBlender4.1.0.1/Curios9.5.1. Нет Fabric/Architectury | All Rights Reserved, README personal-use. Upstream CI build успешен, но не игровой прогон агентом |
| [Reforged main](https://github.com/notlikethiss/thaumcraft-4-reforged/tree/15c4ac0e9198049228a1d102137fd07fce44c5eb) | Уже MC26.3, Java25, NeoForge26.3.0.57-beta | Не выдавать новые golem modules main за наличие их в1.21.1 branch |
| [DJDMM TC6 main](https://github.com/DJDMM/Thaumcraft-6-1.20.1-port/tree/3ee6ed6ec5a55520cc9f599a944e46f89ac3ba74) | Forge1.20.1/47.4.10, Java17, ForgeGradle6.0.54, official mappings/TerraBlender3.0.1.10. Нет Fabric/NeoForge modules | All rights reserved в mods.toml. MIT только для переводов не лицензирует code/assets. Авторские отчёты HEAD не воспроизведены |

TC6 release `v0.10.0-dev` JAR прочитан без исполнения: SHA256
`038ed0bb73c7fa6bb00f0b0ebe72cc8fa2f3f59c232fbf2a5b7508b0ab6350dc`,377class/major61.
Он старше новых native TubeBlockEntity/FocusProjectileEntity и **не доказывает HEAD**.
Отчёт автора0.36/1488checks/23scenes — заявление автора; его logs/screenshots в
checkout не обнаружены. Не смешивать это с нашими собственными проверками.

## Конкретные полезные части

Пути относительно donor `src/main/java/thaumcraft/`; Reforged здесь — **1.21.1 pin**,
если отдельно не указан main.

1. **M06 трубы — ближайшая польза обоих проектов.** Reforged
   [`essentia/TubeBlockEntity.java:66–189`](https://github.com/notlikethiss/thaumcraft-4-reforged/blob/d1e64cc527a8fa0c99ae738cc9c0bd0370d4bd7e/src/main/java/thaumcraft/essentia/TubeBlockEntity.java#L66-L189):
   ticker→suction→transfer1, открытые стороны, valve/filter/oneway/buffer и save.
   TC6 [`essentia/transport/TubeBlock.java:76–121`](https://github.com/DJDMM/Thaumcraft-6-1.20.1-port/blob/3ee6ed6ec5a55520cc9f599a944e46f89ac3ba74/src/main/java/thaumcraft/essentia/transport/TubeBlock.java#L76-L121):
   native connection shapes и точный hit-point рукава вместо CodeChicken raytracer,
   BE sync/клапанный BER. У нас `IEssentiaTransport` и alembic уже существуют;
   нужны наши TileTube/ThaumcraftTubes и transport/ticker банки, не новая storage.
2. **M07 матрица/пьедесталы/беспроводная эссенция.** Reforged
   [`infusion/InfusionMatrixBlockEntity.java:220–352`](https://github.com/notlikethiss/thaumcraft-4-reforged/blob/d1e64cc527a8fa0c99ae738cc9c0bd0370d4bd7e/src/main/java/thaumcraft/infusion/InfusionMatrixBlockEntity.java#L220-L352)
   реально ведёт craftingStart/cycle/ingredient/finish/symmetry/persistence;
   `essentia/EssentiaHandler.java:28–151` даёт native source cache/mirror guard.
   Рендер TC6 `infusion/client/InfusionMatrixRenderer.java:24–64` показывает native
   вращающиеся части/rune additive pass/halo, pedestal renderer — реальный ItemStack.
   Recipe/instability/timing остаются по TC4 oracle, не донорским формулам.
3. **Рендер жезла Reforged**, `client/render/WandSpecialRenderer.java:88–175`:
   focus ornament/depth/цвет, glow/runes, wave/charge. Наш basic focus/head/UI уже
   реализованы, это референс дальнейшей visual полноты; BEWLR hook NeoForge не
   переносится напрямую в Fabric.
4. **OBJ/vertex bridge TC6**, `catalog/entities/client/ObjMesh.java:17–72`:
   groups/negative indices/UV/triangulation/tint. Независимый небольшой adapter для
   наших retained TC4 meshes полезнее массового импорта чужих renderer classes.
   Не вводить второй particle manager; batching/shader примеры
   `InfernalPollutionEffects`/`CondenserSparkShader` полезны позже.
5. **PURE biome ecology Reforged**, `node/NodeBlockEntity.java:624–638`:
   настоящий handlePureNode/biome sync, не просто knot block. Нужны наши биомы и
   native biome-edit lifecycle; это не снятие нынешнего PURE blocker одной вставкой.
6. **Позднее:** Reforged main содержит реальные golem cores/markers/goals/save
   (`entity/golem/GolemBase.java:232–271`), но их нет в его1.21.1 branch. Реальные
   Portable Hole/area tools/armor тоже заслуживают адресного сравнения с oracle.
   TC6 bolt/projectile adapters полезны будущим эффектам, не замене нашего Ember.

## Почему не импортируем целиком

- В обоих проектах разрешение на copy/distribution не установлено. Факты о native
  API и архитектурные идеи можно исследовать; это не выдаётся за разрешение
  скопировать классы/assets. Следующие реализации пишутся поверх наших APIs по
  original TC4 и с отдельной provenance для любого допустимого заимствования.
- TC6: graph FocusPlan, chunk aura payment, Observation/Theory/cards и jar capacity250
  **не TC4**. У нас vis платится из wand по tick, jars64; базовый Fire не beam.
  Catalogue-мобы TC6 с noAI/пустым registerGoals — не готовые игровые мобы.
- Найдено конкретное отличие Reforged: instability explosion использует
  ExplosionInteraction.BLOCK, тогда как TC4 TileInfusionMatrix401/570 не разрушает
  terrain. Потребность сверять каждый эффект не отменяется названием TC4 проекта.
- Reforged spider/mound chest используют SIMPLE_DUNGEON вместо исходного Forge
  dungeonChest: наш loot blocker этим **не закрывается**. Его1.21.1 silverwood пока
  не имеет shimmerleaf-pass, который уже добавлен в нашем текущем новом срезе.
- Donor build green ≠ runtime/visual parity. Tube test `sum<=10` допускает потерю;
  наш transfer должен проверять точную сохранность и interrupted/reload пути.
- Не заменять наши проверенные знания/research, RecipeManager/components/bootstrap
  и Fabric+NeoForge интеграцию чужими enum-aspects/attachments/Forge channels.

## Решение по очереди

Закрепить текущий basic Fire+plants checkpoint, затем **M06 tube/suction/side selection
поверх наших API** с банками/алембиками; использовать Reforged1.21.1 для API примеров
и TC6 shape/BER подходы. После этого **M07 pedestal/matrix/wireless sources** с одним
настоящим рецептом-наградой и original instability, а не декоративным результатом.
Новые refs уже не требуют поиска API вслепую; ни одной донорской системе пока не
присвоен наш runtime PASS и ни один полный milestone ими автоматически не закрыт.
