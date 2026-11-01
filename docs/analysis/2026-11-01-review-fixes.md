# Исправления ревью от 01.11.2026

Базис ревью: `4c38e3f3bb44c3455718d8f3e04073921ec2a932`. Запрос владельца: исправить найденные дефекты. Оригинальные JAR, oracle, retained legacy `src/` и `thaumcraft_src` не изменялись.

## Исправления

Все 29 пунктов исходного отчёта исправлены. Номера соответствуют исходному ревью; проверки ниже обозначают authored coverage, результаты реальных прогонов приведены отдельно.

| ID | Приоритет | Дефект | Проверка |
|---|---|---|---|
| R01 | P1 | Верхняя половина арканной двери вызывает исключение при открытии | reviewDoorPlacesAndBothHalvesOpen |
| R02 | P1 | Генерация Outer Lands использует RNG серверного мира из фонового потока | reviewRoomSpawnsUseOnlyTheirDeterministicStream |
| R03 | P2 | Арканная дверь не устанавливается обычным правым кликом | reviewDoorPlacesAndBothHalvesOpen |
| R04 | P2 | Арканная плита и защитное стекло остаются без владельца | reviewOwnedPlacementKeysAndReclaim |
| R05 | P2 | Ключи не распознают арканные плиты | reviewOwnedPlacementKeysAndReclaim |
| R06 | P2 | Фиала не наносит аспект на знамя | reviewBannerUsesActualPhialAndHand |
| R07 | P2 | Ведро смерти обходит ограничения Adventure | reviewDeathBucketHonorsPermissionsSolidsAndDrops |
| R08 | P2 | Ведро смерти заменяет твёрдые блоки, не являющиеся полными визуальными кубами | reviewDeathBucketHonorsPermissionsSolidsAndDrops |
| R09 | P2 | Банка мозга теряет XP объединённых шариков | reviewBrainJarConservesMergedExperience |
| R10 | P2 | Адская печь не выдаёт XP за обычные переплавки | reviewFurnaceUsesInputExperienceAndPreservesBonusData |
| R11 | P2 | Тауматорий останавливается перед пустой бочкой и другими обычными контейнерами | reviewThaumatoriumUnsupportedOutputFallback |
| R12 | P2 | Fabric Transfer helpers не извлекают воду из заполненного тигля | reviewCrucibleUnboundedTransfer |
| R13 | P2 | Equal Trade сообщает NeoForge placement listeners исходный блок вместо нового | neoEqualTradeProtectionSeesProposedBlock |
| R14 | P2 | Сканирование одного зелья блокирует сканы остальных разновидностей | PotionScanIdentityTest; reviewPotionSubtypesUseDistinctWorldTargets |
| R15 | P2 | completeAspect складывает абсолютный итог RP с текущим запасом | reviewAbsoluteAspectAndExactResearch |
| R16 | P2 | Точечное завершение исследования через API выдаёт также зависимости | reviewAbsoluteAspectAndExactResearch |
| R17 | P2 | После загрузки босс неправильно сохраняет долю здоровья при multiplayer scaling | reviewLoadedBossKeepsHealthRatio |
| R18 | P2 | Угловые декорации лабиринта отбрасываются даже без bedrock | OuterLandsBedrockVisibilityTest |
| R19 | P2 | Пустая или узкая категория исследований аддона роняет браузер | ResearchBrowserBackgroundTest |
| R20 | P2 | Вкладки настроек Research и Tweaks уходят за экран | ConfigTabLayoutTest |
| R21 | P3 | Ведро смерти удаляет заменяемые растения без дропа | reviewDeathBucketHonorsPermissionsSolidsAndDrops |
| R22 | P3 | Комбинация аспектов игнорирует настроенный aspect_total_cap | ResearchCombinationSettingsTest |
| R23 | P3 | Двенадцать исследований отображаются без иконок | ResearchItemIconTest |
| R24 | P3 | Подписи настроек Use core не совпадают с переключателями | Прямая правка локальных координат; кадр не проверен |
| R25 | P3 | Название цвета фильтра голема получает двойное смещение панели | Прямая правка локальных координат; кадр не проверен |
| R26 | P3 | Потоки FX эссенции не стареют после выгрузки endpoint-чанка | EssentiaFlowLifetimeTest |
| R27 | P3 | Частицы ярости Eldritch-боссов никогда не появляются | Сборка и dedicated-server загрузка; кадр не проверен |
| R28 | P3 | Адская печь стирает компоненты зарегистрированного addon-бонуса | reviewFurnaceUsesInputExperienceAndPreservesBonusData |
| R29 | P3 | Максимум рунического щита не синхронизируется при смене экипировки без изменения заряда | reviewRunicMaximumSyncsWithoutRecharge |

## Совместимость и различия с оригиналом

- `ResearchManager.completeAspect` снова принимает абсолютный signed-short total; новый `ThaumcraftApiHelper.addAspectPool` по-прежнему принимает delta. Нулевой total открывает аспект и устанавливает нулевой запас.
- `completeResearch` выдаёт только переданное исследование и его warp. Рекурсивная выдача зависимостей осталась административной командой.
- Обычные item scan markers не изменены. Water/unspecified potion сохраняет прежний plain key; зарегистрированные разновидности зелий получают suffix `[potion=namespace:id]`. Старый generic marker не содержит subtype, поэтому восстановить прежнюю разновидность невозможно: он остаётся water/unspecified marker. Имя, цвет и custom effects не создают отдельный scan key, как исходный NBT. API lookup и thaumometer используют один helper.
- R29 — осознанное исправление также исходного TC4 дефекта: oracle `EventHandlerRunic:144–146` отправлял только изменение charge. Теперь изменение maximum тоже синхронизируется; заряд, затраты vis и recharge timers не менялись.
- Тауматорий не вызывает неизвестные container callbacks при commit: неподдержанные контейнеры используют существующую оплаченную физическую выдачу. Поддержанные полные контейнеры сохраняют прежний preflight/pause.
- Сигнатура `public static void ItemArcaneDoor.placeDoor(...)` сохранена.

## Результаты проверки

Fabric: `:common:test :fabric:build :fabric:runGameTest` — PASS на актуальном общем коде. JUnit: 2779 тестов, 0 ошибок/падений, 5 прежних пропусков без оригинального JAR. GameTest: 528/528 на новом мире, включая 13 новых regression scenarios. Обычные redstone/bow session keys сохранены; callback scan, physical target, hovered row и импорт advanced markers проверяют одну идентичность зелья. Журнал: `.tmp/fix-2026-11-01-fabric-accepted.log`. Python: 143 теста, 0 падений, 9 прежних пропусков без исходных JAR. Проверки assets (`missing_references: 0`) и scope ledger (`stale_review_fingerprints: 0`) проходят. NeoForge: `:neoforge:build :neoforge:runGameTest` — PASS; 516/516 сценариев на свежем мире, включая `neoEqualTradeProtectionSeesProposedBlock` с настоящим Event Bus. Журнал: `.tmp/fix-2026-11-01-neoforge.log`. Сборки обоих loaders прошли проверку отсутствия test harness в production JAR. NeoForge JUnit: 13/13 без пропусков. NeoForge проверялся после завершения Fabric.

## Отдельный ранее известный долг: compaction истории

Очереди `DeferredItemDrops` и `AspectOrbQueue` в этом наборе не изменены. Неограниченное хранение receipts/journals было выделено отдельно от 29 findings и уже отмечено в `2026-10-17-crystallizer.md`. Простое удаление по лимиту или возрасту разрешит stale snapshot повторно выдать зачисленный предмет либо заставит загруженный Bore ошибочно fence корректный `WITNESSED` reference.

Безопасная compaction требует сохранённого issuer namespace/monotonic sequence и exact settlement intervals с незавершёнными gaps и legacy UUID, live high-water при stale SavedData reload, а также согласованного source-BE/queue checkpoint либо эквивалентного аутентифицированного compact witness для terminal Bore journal. UNKNOWN/quarantine/opaque custody должны сохраниться. Нужны отдельные проверки stale snapshots после inventory/pickup, ADD retry, callback saves, независимого порядка source/queue save и unload/restart. Это не заявление о гарантии arbitrary power-loss.

## Границы доказательств

Результат unit/GameTest/сборки не считается осмотром GUI, частиц, multiplayer или длительным survival-прохождением. Клиентские кадры текущих исправлений пока не сняты.
