# Исправления второго ревью от 01.11.2026

Базис: `4c38e3f3` с предыдущими 29 исправлениями. По запросу владельца реализованы исправления всех 23 новых дефектов; R01–R29 сохраняются. Оригинальные JAR, oracle, retained legacy `src/` и `thaumcraft_src` не изменялись.

## Область исправлений

| ID | Приоритет | Дефект | Проверки |
|---|---|---|---|
| S01 | P1 | Earthmover удваивает плиты при копировании double slab | review2EarthmoverPaysEveryMaterialUnit; EarthmoverMaterialTest |
| S02 | P2 | Primal Crusher добывает соседние блоки без свойств инструмента | review2CrusherKeepsToolAndProtection |
| S03 | P2 | Save & Quit с Hover Harness может сохранить временный полёт как постоянный | review2HoverGrantUsesPlayerIdentity |
| S04 | P2 | Hand Mirror дублирует или теряет отправляемый стек на spawn callback | review2HandMirrorOwnsOutputAcrossCloseCancelAndRetry; review2HandMirrorMissingDestinationReturnsInputOnce |
| S05 | P2 | Traveling Trunk теряет содержимое при отмене выдачи и допускает повторную выдачу | review2TrunkDeathAndDropsRetainCanceledStock; review2OrderRecallRetriesAndPreservesSlotIndices |
| S06 | P2 | Infernal Furnace уничтожает оплаченный выход при отмене спавна | review2FurnaceCanceledCompletionRetainsOutputs; review2PaidQueuesSaveReceiptsBeforeReplacementTick |
| S07 | P2 | Ограниченные слоты складывают нестакуемые фокусы и чернильницы | review2RestrictedSlotsKeepItemLimits; SlotLimitedNativeLimitTest |
| S08 | P2 | Публичный crafting-vis helper списывает в 100 раз меньше vis | review2CraftingVisPaysWholeUnitsAndCapPrice |
| S09 | P2 | Динамический бонус WEAPON одинаково завышен у всех мечей | review2RegisteredEquipmentKeepsOriginalAspectBonuses; AspectBonusParityTest |
| S10 | P2 | Мотыги получают TOOL вместо исходного HARVEST | review2RegisteredEquipmentKeepsOriginalAspectBonuses; AspectBonusParityTest |
| S11 | P2 | Таблица harvest tiers теряет бонусы VOID и PRIMALVOID | review2RegisteredEquipmentKeepsOriginalAspectBonuses; AspectBonusParityTest |
| S12 | P2 | Кэш getCraftingRecipeKey устаревает после datapack reload | review2RecipeLookupRefreshesAfterSameManagerReload |
| S13 | P2 | Раскрытые tag-регистрации аспектов протекают между наборами item tags | review2TagAspectsFollowBindingAndPreserveOverrides |
| S14 | P2 | NeoForge Decanting golem не видит подходящую жидкость во втором баке | neoGolemAcceptsFluidInLaterTank |
| S15 | P2 | JEI считает разные палочки одним ингредиентом | ItemSubtypesRegression: actual JEI equality/UIDs |
| S16 | P2 | Клиентский кэш аддон-рецептов неограниченно растёт при чтении страницы | AddonRecipePreviewTest |
| S17 | P2 | Перезагрузка ресурсов надолго скрывает актуальный Runic HUD | ClientResourceReloadTest |
| S18 | P2 | Жидкость смерти не имеет своего fluid renderer на обоих загрузчиках | DeathFluidRendererRegression; NeoForgeDeathFluidClientTest |
| S19 | P3 | Ингредиенты рецептов книги не переходят к их исследованию | RecipeItemLinksTest; ResearchReferenceNavigationTest |
| S20 | P3 | Поиск не видит исследования, купленные в уже открытой книге | ResearchSearchCacheTest |
| S21 | P3 | Книга показывает constructor snapshot изменённого аддон-рецепта infusion | AddonRecipePreviewTest |
| S22 | P3 | Order recall уплотняет слоты Traveling Trunk | review2OrderRecallRetriesAndPreservesSlotIndices |
| S23 | P3 | Flux Scrubber дважды отправляет один и тот же sparkle burst | Удалён второй вызов отправки; сборка/dedicated-server smoke; кадр не проверен |

## Основные изменения

- Earthmover оплачивает все единицы составного блока, различает native-варианты и не копирует бесплатную воду из waterlogged state. Crusher добывает соседние блоки через native player game mode с защитой, зачарованиями, XP и одним износом за принятый блок. Hover grants используют weak identity вместо равенства entity ID.
- Зеркало, сундук и печь передают оплаченный результат в сохранённую очередь до ADD/close callbacks. Отменённые предметы и XP повторяются, принятые deliveries получают receipts. Подготовка копий и storage проходит до публикации, source debit/commit — без внешних callbacks. Старые Bore VERSION4 rows сохраняют формат; новый необязательный PortalCooldown отсутствует у старых выдач. Order Trunk сохраняет позиции пустых слотов.
- Vis crafting API принимает целые vis и использует тот же расчёт cap/gear/research modifiers, что workbench. Бонусы sword/hoe/VOID восстановлены. Recipe key cache замечает замену recipes в том же manager; tag-derived object tags снимаются при rebind/выходе мира, explicit registrations сохраняются.
- JEI регистрирует компонентные subtypes. Книга переиспользует стабильные holders, читает live infusion preview, обновляет поиск при изменении знания/каталога/recipes и поддерживает переход по ингредиенту с историей. Resource reload сбрасывает render resources, сохраняя серверный HUD/fog snapshot. Жидкость смерти получает собственный renderer. Scrubber отправляет только исходный BlockSparkle effect.

## Проверка

Fabric: `:common:test :fabric:build :fabric:runGameTest` — PASS на итоговых источниках. JUnit: 2797 тестов, 0 ошибок/падений, 5 прежних пропусков без оригинального JAR. GameTest: 542/542, включая 14 новых сценариев. Лог: `.tmp/fix2-fabric-accepted.log`.

Первый полный прогон прошёл 541/542: все новые проверки прошли, старый strict-sphere тест големов не дождался native entity ticking для дальних чанков. Фикстура теперь ждёт готовности только своих force-loaded chunks и освобождает свои tickets; assertions и gameplay не менялись. Повторный полный прогон зелёный.

NeoForge: `:common:test :neoforge:build :neoforge:runGameTest` — PASS после завершения Fabric. GameTest: 531/531 на свежем мире, включая 14 общих regression scenarios и новый native multi-tank capability сценарий. NeoForge JUnit: 15/15 без пропусков. Лог: `.tmp/fix2-neoforge-accepted.log`.

Python: 143 теста, 0 падений, 9 прежних пропусков без исходных JAR. Assets: `missing_references: 0`; scope ledger: `stale_review_fingerprints: 0`. Оба production JAR прошли проверку отсутствия test harness. Каталог NeoForge согласован с Fabric.

Дополнительные проверки настоящего optional JEI runtime: `ItemSubtypesRegression` — 47 проверок PASS. Fabric `DeathFluidRendererRegression` — PASS: texture atlas lookup, still/flowing sprite, white tint и смена sprite handles при reload. Использованы объявленные atlas/sprite handles без GL; это не осмотр кадра. Логи: `.tmp/fix2-platform/jei-root-rerun.log`, `.tmp/fix2-platform/fabric-root-rerun.log`.

## Границы доказательств

Сборка, unit и dedicated-server GameTest не считаются осмотром GUI/частиц, multiplayer или длительным survival-прохождением. Известный долг compaction receipts/Bore journals остаётся отдельной задачей. Новая XP-очередь также сохраняет историю receipts без compaction. Прежние opaque custody и anti-replay правила Bore сохранены.
