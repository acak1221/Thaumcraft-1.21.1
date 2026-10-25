# Исправления по дополнительному списку из 27 замечаний

Дата: 25.10.2026. Проверяемый базис — `c4a8f7a8cc5f56458d6afe7a5551aa31d6957eff`, ветка `work`, с сохранёнными незакоммиченными исправлениями предыдущей волны. `git ls-remote origin refs/heads/main` также вернул этот коммит; локальная tracking-ссылка `origin/main` устарела и не принималась за состояние сервера. Коммит, push, PR, публикация и платный CI не выполнялись.

Список проверен повторно по достижимым вызовам, исходникам используемых зависимостей и приватному oracle TC4 4.2.3.5. Он содержит новые ошибки, уже исправленные пункты, ранее задокументированные ограничения и особенности оригинала. Формулировка «27 новых незадокументированных ошибок» не подтвердилась. Предыдущие отчёты: [первичное ревью](2026-10-25-deep-review-ru.md) и [первая волна исправлений](2026-10-25-review-fixes-ru.md).

## Подтверждённые ошибки и изменения

Причины ошибок ниже подтверждены статически по активным путям и контрактам. Регрессии исполняют исправленный код; отдельный запуск каждого нового теста против старой версии не проводился и не заявляется. Названия проверок обозначают реальные методы тестов; окончательные результаты всей волны приведены в разделе проверок.

| №, приоритет | Условие, достижимый путь и прежнее последствие | Исправление и регрессионная проверка |
|---|---|---|
| 2, P1 | Спавн претора: `finalizeSpawn` выбирал локальный CHAMPION, затем entity-add → `ChampionHooks` выбирал второй. Имя, атрибуты и способности расходились; локальный ID не сохранялся. | `EntityCultistLeader` использует одну сохраняемую и синхронизируемую идентичность общей системы. Идентичность устанавливается до генерации имени, способности исполняются одним hook. `praetorHasOnePersistentChampion` проверяет все 13 модификаторов, имя, native spawn/save/load, атрибуты и shield; `restoredPraetorWarpHitsOnce` — единственный Warp-эффект после загрузки. Направление и oracle независимо проверены другим агентом. |
| 2a, P1, сопутствующее | Общая конверсия атрибутов уже до правки претора трактовала TC4 amount2/op2 как ×2, amount3/op2 как ×3. Реальная операция умножает на `1+amount`: ordinary champion ×3, Mighty дополнительно ×4. | Исправлены множители `ChampionModifier`/`ChampionRules` и операция Bold на `ADD_MULTIPLIED_BASE`. Native/JUnit проверки теперь защищают ×3/×12 для обычного Mighty и ×4 для Mighty-претора, а также сложение Bold с другим base-множителем. Ожидания изменены по независимому oracle, а не ради зелёного теста. |
| 3, P1 | Biome feature → mound/hilltop/ring принимали более широкий набор измерений, чем четвёртый член исходной цепочки. Структуры и их узлы могли появляться вне принятого портом Overworld-контракта. | Все четыре используют общий `permitsStructures`, включая preflight и `placeSelected`; учитывается dimension blacklist. `generationRespectsDimensionPolicies` проверяет ранний отказ без доступа к terrain/RNG. Сохранена ранее документированная адаптация `isOverworld`: vanilla Overworld и измерения vanilla Overworld type; строгая проверка одного resource key не вводилась молча. |
| 5, P1 | Item renderer → `renderWand` обходил публичный depth-hook, рисовал только Primal с постоянной альфой и emissive shell без видимой пульсации. | Все четыре фокуса возвращают depth-иконки. Общий путь использует hook, PNG/.mcmeta animation, альфу .6/.95 и исходный lightmap pulse; cache сбрасывается при reload обоих загрузчиков. `WandFocusRenderingTest` записывает реальные vertices/shaders/UV/light/colors/poses, включая addon hook. Независимое ревью пройдено; физический кадр клиента не исследован. |
| 7, P2 | NeoForge login listener каждый вход печатал `DIAG login` и читал внутреннее состояние сетевой регистрации. | Диагностический listener удалён из `ThaumcraftNeoForge`. Это узкая source-правка, NeoForge не запускался. |
| 8, P2 | Fabric `LootTableEvents.MODIFY` пропускал `DATA_PACK`/`REPLACED` из-за `isBuiltin`; NeoForge `LootTableLoadEvent` охватывает пользовательские datapack-таблицы. | Fabric добавляет исходные TC4 chest rows независимо от source-категории. Сохранены namespace/category filter и единственный существующий primary pool. `LootSourceRegressionChecks` вызывает фактически зарегистрированное событие для всех четырёх source, сравнивает полные serialized tables, проверяет второй pool, чужой namespace и намеренно пустую таблицу. |
| 9, P2 | Аддон → `AspectEventProxy.registerComplexObjectTag(String)` вызывал простую регистрацию без рецептурной базы. Этот дефект уже был описан как D13 в `docs/audit/API_QUALITY.md`. | Добавлены настоящие complex String/TagKey overloads с server lifecycle-контекстом и отдельными копиями добавок для членов тега. Прокси вызывает complex-ветку. Existing `ObjectAspectApiRuntimeChecks` проверяет реальные рецепты OAK/BIRCH_STAIRS, неизменность входа, отсутствие смешивания баз и TC4-ветку уже зарегистрированного предмета. Независимое ревью пройдено. |
| 11, P2 | Конкретный достижимый разрыв: creative/pick-block не создавали tier-варианты, placement терял tier; native rarity отсутствовала, tooltip ссылался на несуществующие vanilla translation keys. Сам worldgen не обязан выдавать контейнер item при разрушении: исходное поведение выдаёт содержимое. | Creative варианты 0/1/2, pick → serialization → survival placement сохраняют tier. Сеттер пишет native RARITY, сохраняет прочие компоненты; tooltip воспроизводит TC4 label. `LootContainerRarityRuntimeChecks` проверяет обе зарегистрированные разновидности по всем трём tier через native placement. |
| 12, P2 | Custom item renderer компенсировал equipped translation обратным сдвигом; кроме того, цепочки item/tile/aspect использовали неверную ось поворота. | Вынесена общая `BannerPose`, восстановлены Y-повороты и действующий equipped offset, slot frame учитывает реальные bounds модели. `BannerPoseParityTest` проверяет матрицы/вершины по исходным цепочкам. Физическая визуальная приёмка остаётся открытой. |
| 13, P2 | Thaumometer → переход на следующую строку аспектов использовал прежнюю вместимость при центрировании. | Порядок сверён по байткоду оригинального JAR, поскольку decompiler скрывал порядок инкремента. Используется вместимость новой строки; сохранена исходная особенность счётчика оставшихся аспектов. `ThaumometerAspectLayoutParityTest` сравнивает координаты многорядной раскладки с буквальным исходным порядком. |
| 14, P2 | Публичные карты blacklist уже существовали, но отсутствовали удобные методы фасада, а отдельные surface paths обходили policy. | Добавлены native-key API add/get dimension/biome blacklist, surface aura/structure gates и чтение biome policy живым tree feature. Оригинальные add-методы находились в ThaumcraftWorldGenerator; размещение convenience-методов на ThaumcraftApi — адаптация порта. Проверяются levels 0/1/2 и неизвестный уровень, rejection до RNG, настоящий native tree pass. При этом biome blacklist TC4 относится к vegetation/ores, а не ко всем узлам; отдельный Nether aura pass обходил surface blacklist и продолжает это делать. `NodeEcologyPolicyTest` и `generationRespectsDimensionPolicies` защищают эти различия. |
| 15, P2 | Player tick → `EnchantmentRepair.tick` → hotbar loop ремонтировал неэкипированный HoverHarness и расходовал VIS. TC4 исключает его только в этом цикле. | Исключён только hotbar Harness; worn armor/direct repair не заблокированы. Existing `EquipmentEnchantmentRuntimeChecks` проверяет нерасходование в хотбаре и починку того же ранца после экипировки. |
| 21, P2 после проверки достижимости | Fabric ArmorRenderer перехватывает HumanoidArmorLayer для LivingEntity. Custom armor → `renderGear` → `copyPose` копировал современную zombie/skeleton animation, обходя особую TC4 arm pose. Поэтому замечание не сводится к наличию мёртвых helper-методов. | Вызов custom armor восстанавливает исходную zombie/skeleton коррекцию после копирования native pose. Public совместимость сохранена. Регрессия использует четыре реально baked модели, ванильную анимацию и буквальные TC4 arm fixtures, с отрицательным контролем игрока. |
| 23, P2 | Native empty BucketItem → mixin → DeathBucketActions удалял источник без claim и проверки результата setBlock; callbacks могли повторить захват или сменить руку до оплаты. Native BucketPickup был вторым несогласованным путём. | Общая capture-транзакция, actor/source claims, input reservation до callback, успешное удаление/postcondition, точный refund и кредит без перезаписи новой руки. `deathBucketConservesInputs` использует native use/pickup, callback/ABA/reentry, component-bearing input и creative-контракт. Отказ записи инъецируется изолированным test-only LevelChunkSection; настоящие LevelChunk/Level возвращают false. Проверяются неизменная read view, число фактических попыток записи и восстановление оригинальной секции; production hooks отсутствуют. |
| 24, P2 | Wand → OculusFormation сначала удалял node/BE, затем мог получить отказ portal placement; восстановление одного blockstate теряло NBT ауры и идентичность. | Прямая замена без промежуточного AIR, full-NBT snapshot и ограниченный rollback без оплаты/открытия при отказе. Чужой новый BE, даже с тем же blockstate, не перезаписывается. `oculusPreservesNodeOnFailure` проверяет инъецированный storage-отказ через настоящие LevelChunk/Level, успешную оплату/первое уведомление, отмену частичной замены и нового владельца. |
| Дополнительное, P3 | `MazeSavedData.save` писал unordered ConcurrentHashMap прямо в ListTag. Одинаковые ячейки после load могли иметь другой порядок сериализации, нарушая строгий roundtrip. Отдельный reproducer на 3042 ключах дал тот же набор ключей и 1895 других порядковых позиций; потеря данных этим не доказана. | Запись упорядочена по packed cell key с сохранением формата и значений. Строгая проверка полного NBT не заменяется сравнением части данных; добавлена регрессия переставленных/коллидирующих ключей. |

## Очистка и уже обработанные пункты

| № | Решение |
|---|---|
| 1 | Исправлен предыдущей волной: `TileMirror.isLinkValid` сравнивает reciprocal target с фактическим измерением текущего зеркала. Native `crossDimensionPairsTransferWithoutLosingStock` защищает межмировую передачу. Повторный патч не нужен. |
| 4 | Предыдущая волна уже подключила CastingWandHud к обоим loader callbacks, focus icon, FocusTrade, cooldown и Shift-цену. Шесть actual draw-operation тестов и hand-path проверки прошли в предыдущей волне; седьмой HUD-тест границы snapshot добавлен сейчас. NeoForge registration по-прежнему проверена только статически. |
| 6 | Предыдущая волна восстановила вызовы виртуальных `checkItemEquals`/`areItemStacksEqual` из активных matches. Проверяются addon subclasses и сохранение caller stacks. |
| 16 | Удалено недоказанное чтение root `Author`, для которого не найден writer даже в вводящем коммите. Recipe-only hint по-прежнему читается с неизвестным автором. Existing native persistence-тест расширен проверкой настоящего `Recipes[].author` UUID roundtrip. Это очистка мёртвой совместимости, а не воспроизведённая потеря автора текущим writer. |
| 17 | Legacy SlotCraftingArcaneWorkbench остаётся неиспользуемым, помечен deprecated с предупреждением предыдущей волной. Активен транзакционный result slot ContainerArcaneWorkbench. Не подключался и не удалялся ради ложной «починки» активного поведения. |
| 20 | Четыре public renderer-класса не зарегистрированы и не имеют callers. Сохранены как compatibility surface; наличие dead code не доказывает отсутствие активного renderer конкретной сущности. Никакие фиктивные регистрации не добавлены. |
| 22 | 31 строка биомных данных вынесена в immutable `OriginalBiomeTypes`; три потребителя используют один источник. Каждая строка и порядок независимо сопоставлены с TC4 Config.registerBiomes. Сохранены mutable compatibility BiomeHandler API и прежние runtime snapshots. |

## Не подтвердившиеся кандидаты

| № | Доказательство и оставшееся ограничение |
|---|---|
| 10 | TC4 HUD также не содержит clamp. Sceptre в этом порте — persistent custom component, а не снимаемое точилом enchantment. Actual GrindstoneMenu проверяет сохранение компонента, capacity и near-full VIS после снятия настоящего Unbreaking. Это declared input для native disenchant, не доказательство survival-получения Unbreaking на посохе. Заявленный путь overflow не подтверждён; произвольное повреждённое NBT/вмешательство аддона не исследовано полностью. |
| 18 | Oracle Config.java:390 буквально использует `resDif.getInt(8)`. Fallback8 сохранён как исходная особенность, документирован и покрыт проверками пути исследований. Новый режим сложности не вводился. |
| 19 | Фактический MC1.21.1 FlowingFluid содержит шесть Fluid.isSame sites в пяти указанных методах: два в getNewLiquid и по одному в остальных. `require=6, allow=6` считает sites и корректен. Сохранены javap-доказательства. |
| 25 | THEORY — read-only проекция TC4 завершённых исследований, а OBSERVATION — изменяемый aspect pool. Ограничение уже описано в NativePlayerKnowledge, API_QUALITY N14 и NULL_POLICY_FIX. Уточнён public interface и добавлена поведенческая проверка; второй TC6-баланс без TC4 спецификации не придуман. |
| 26 | Утверждение «NeoForge только null-check» не соответствует текущему исходнику: проверяются exact FakePlayer class, canonical world/thread/server, UUID/profile/name, connection и идентичность FakePlayerFactory; получение actor также исключает spawned/borrowed/online player. Оставшаяся разница касается класса соединения: Fabric проверяет exact handler class, NeoForge — nonnull, его штатный handler является private nested class. Обход actor identity не продемонстрирован; NeoForge runtime сейчас не проверялся. |
| 27 | Оригинальный ClientTickEventsFML обновляет old-values перед сравнением стрелки, как текущий HUD. Каноническое подавление стрелки на границе snapshot сохранено; добавлена actual draw-operation регрессия этого кадра. |

## Проверки и границы покрытия

Основные места в итоговом дереве; номера строк относятся к этой волне, а не к исходному коммиту:

| № | Файл и строка |
|---|---|
| 2 | `common/src/main/java/thaumcraft/common/entities/monster/EntityCultistLeader.java:171`, `:205`, `:221`; shared persistence — `common/src/main/java/thaumcraft/common/lib/mods/ChampionHooks.java` |
| 2a | `common/src/main/java/thaumcraft/common/lib/mods/ChampionModifier.java:26`, `:30`; `ChampionRules.java:309`, `:318`, `:325` в том же каталоге |
| 3 | `common/src/main/java/thaumcraft/common/lib/world/ThaumcraftTotemFeature.java:182`; `WorldGenMound.java:77`, `WorldGenHilltopStones.java:46`, `EldritchRingFeature.java:48` в том же каталоге вызывают общий gate |
| 5 | `common/src/main/java/thaumcraft/client/renderers/item/PrimalWandFocusRenderer.java:42`, `:51`, `:70`; `common/src/main/java/thaumcraft/client/ThaumcraftGearModelsClient.java:205` |
| 7 | `neoforge/src/main/java/thaumcraft/neoforge/ThaumcraftNeoForge.java:48` — удалённый listener находился перед client setup |
| 8 | `fabric/src/main/java/thaumcraft/fabric/loot/FabricThaumcraftLoot.java:30` |
| 9 | `common/src/main/java/thaumcraft/api/aspects/AspectEventProxy.java:44`; `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:560`, `:576` |
| 11 | `common/src/main/java/thaumcraft/common/blocks/BlockLoot.java:40`; `BlockLootItem.java:41`, `:56` в том же каталоге; `common/src/main/java/thaumcraft/common/config/ThaumcraftEldritchBlocks.java:680` |
| 12 | `common/src/main/java/thaumcraft/client/renderers/BannerPose.java:11`, `:30`, `:40`; вызывается item/tile renderer |
| 13 | `common/src/main/java/thaumcraft/client/renderers/item/ThaumometerReadout.java:153`, `:193` |
| 14 | `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:54`; `common/src/main/java/thaumcraft/common/lib/world/AuraDimensionRules.java:9`, `ThaumcraftTreeFeature.java:51` |
| 15 | `common/src/main/java/thaumcraft/common/lib/enchantment/EnchantmentRepair.java:22` |
| 16 | `common/src/main/java/thaumcraft/common/tiles/TileThaumatorium.java:349` — fallback Recipe-only в методе load, удалена только попытка чтения root Author |
| 21 | `common/src/main/java/thaumcraft/client/renderers/models/gear/ModelGearArmor.java:80`; `common/src/main/java/thaumcraft/client/ThaumcraftGearModelsClient.java:403` |
| 22 | `common/src/main/java/thaumcraft/common/lib/world/biomes/OriginalBiomeTypes.java:13`; consumers — `BiomeHandler`, `MagicalTreeBiomeAdapter`, `NormalNodeBiomeAdapter` |
| 23 | `common/src/main/java/thaumcraft/common/lib/fluid/DeathBucketActions.java:68`, `:97`, `:108`; `common/src/main/java/thaumcraft/common/blocks/BlockFluidDeath.java:145` |
| 24 | `common/src/main/java/thaumcraft/common/lib/eldritch/OculusFormation.java:104`, `:123` |

Окончательные проверки выполнены на одном зафиксированном наборе входов:

| Проверка | Результат |
|---|---|
| `:common:test` | **2561/2561 PASS**,408 suites,0 failures/errors/skips |
| `:fabric:build` | **PASS**, remapped production JAR собран после последней production-правки |
| `verifyTestIsolation` / `verifyProductionIsolation` | **PASS**, test harness отсутствует в production JAR |
| Полный `:fabric:runGameTest` без фильтра, свежий мир | **467/467 required PASS**,0 failures/errors/skips; normal save/stop, единый Gradle command exit0 за3m32s |
| Промежуточный focused9 | **9/9 PASS** после исправления первых fixtures; этот запуск предшествовал последней правке порядка maze save. Финальные467 включают все эти сценарии |
| `TC4_ORACLE=/workspace/work/thaumcraft-4.2.3.5-oracle python3 -m unittest discover -s scripts/porting -p 'test_*.py' -v` | **112/112 PASS**, без skips |
| `python3 scripts/porting/convert_assets.py --verify` | **PASS**, missing references0 |
| `python3 scripts/porting/update_scope_ledger.py --check` | **PASS**,1044 rows,0 stale fingerprints; согласованность учёта не означает полную игровую приёмку |
| `git diff --check` | **PASS** |
| NeoForge build/tests/root build | **Не выполнялись** согласно AGENTS.md |
| Физический клиент/GPU/multiplayer | **Не выполнялись**, inspected frames не заявлены |

Запуск: `JAVA_HOME=<repo>/.tmp/fix-review/jdk21`, `GRADLE_USER_HOME=<repo>/.gradle21`, wrapper8.12.1 с `--no-daemon --offline`. Единый финальный Gradle command: `:common:test :fabric:build :fabric:runGameTest` без gameTestFilter. Sandbox network grant использован для локальных сокетов Gradle, не для удалённого CI.

Manifest `.tmp/fix-followup/final-inputs-before-checks.json`: **6130 source/test/resource/build inputs**, SHA-256 `d3462b5d56513ad68ff21a01f2a0a9edc1415a6a50eb47d3282c5fd52e19efec`. До/после финального запуска manifest byte-identical; root дополнительно пересчитал хеши всех входов и получил0 отличий. От первой волны изменено59 таких файлов, добавлено12, удалено0. Последующие изменения отчётов отдельно отличимы от исполнявшихся входов.

Итоговый локальный JAR: `fabric/build/libs/Thaumcraft-fabric-0.1.0-alpha.5.jar`, **15291266 bytes**, SHA-256 `fd3c05f064aac850b3c65307ac7a82a8c31bd433d0bd38183b86b981a04456db`. Он не опубликован. Прежний прошедший пакет сохранён отдельно. Основные доказательства: `.tmp/fix-followup/gradle-fresh-full-final.log`, `gametest-fresh-final-467.xml`, `junit-fresh-final-summary.json`, `final-results.json`, `build.md`; полные JUnit XML находятся в `junit-fresh-final-xml/`.

Промежуточные красные результаты сохранены, а причины не скрыты:

- Первая компиляция нового loot-теста использовала Holder codec вместо DIRECT_CODEC; исправлен тест по actual mapped API.
- Первые 3 renderer JUnit failures происходили до drawing из-за регистрации Item после freeze. Test-only LauncherSessionListener теперь регистрирует реальные фокусы до freeze; все vertex assertions сохранены.
- Первый focused9 дал 7/9: debug-world fixture скрывал настоящий source/altar под synthetic AIR. Он заменён явной scoped storage fault injection с проверкой исходной read view, числа попыток, строгого refund/NBT и finally restoration. Повторный focused9 прошёл целиком.
- Первый full467 на повторно использованном тестовом мире дал 461/467. Выявлены нестабильный порядок save/load карты, два прежних неверных ожидаемых множителя урона и отсутствующие chunk/index предусловия scan-fixture. Они исправлены с усилением проверок. MazeAudit повторно читал лабиринт с уже удалённым прошлым прогоном замком; assertions лабиринта не менялись. Forest flora assertions также сохранены для проверки на свежем мире.

Полная старая тестовая карта сохранена в `.tmp/fix-followup/reused-gametest-world-before-fresh`; финальный общий прогон начинается без неё. Это тестовый мир из ignored run-каталога, не пользовательское сохранение. Для map-order причины отдельно повторён reproducer на JDK21: `.tmp/fix-followup/maze-map-order-reproduced.txt` фиксирует 3042/3042 keys, sameKeys=true и 1895 отличающихся порядковых позиций.

Финальный fresh467 прошёл также прежние forest flora assertions без изменения их ожиданий и MazeAudit с настоящей генерацией/проходом/открытием замка. Это подтверждает работоспособность этих сценариев на заявленном свежем мире; повторяемость всего набора на уже изменённом мире не заявляется.

Два теста бестиария теперь ожидают исходные 40 attack damage (base10 × Mighty4), а melee голема — 30 (×.75). Значения независимо сверены с оригинальными Golem/Warden и EntityUtils; тесты не ослаблены до диапазона или меньшего набора свойств. Scan-fixture загружает/удерживает все три используемых чанка и явно проверяет `getEntity(id)==entity` до сканирования. Production ScanCallbacks не изменён. Oculus-fixture убирает только собственные новые строки карты с сохранением живого SavedData и чужих записей.

Ресурсный контракт обновлён после чтения конкретного supplier diff: конечные rod/cap/armor expansions не изменились; четыре depth PNG проверяются literal-file verifier. Два удалённых дублирующих champion attribute ID не являются asset paths. Контрольные суммы оригинала, доноров и утверждённых тестовых результатов не заменялись ради зелёного статуса; обновлённая source fingerprint фиксирует просмотренный новый код.

Проверки запускаются одним координатором на Temurin21.0.13+9, wrapper8.12.1, offline dependencies. NeoForge builds/tests/root build не выполняются согласно AGENTS.md; узкие запрошенные loader source-правки проверяются статически. Original/donor JAR и oracle не изменяются.

Реально исследованы активные пути перечисленных замечаний и соответствующие зависимости. Физический клиент, inspected gameplay frames, latency/multiplayer, произвольные аддоны, все сочетания worldgen/ресурс-паков и полное survival-прохождение не проверены. Headless server/vertex tests не означают визуальную приёмку. Не утверждается, что найдены все ошибки или перечитан каждый файл.

Матрица повторной проверки дополняет покрытие первичного отчёта; она не означает повторное полное ревью всех файлов направления:

| Направление | Что действительно проверялось в этой волне | Что осталось вне новой проверки |
|---|---|---|
| Регистрация Fabric/NeoForge | Actual Fabric loot callback/source categories, Fabric armor hook, наличие HUD/reload calls, удаление login probe; NeoForge API sources | NeoForge сборка, startup и runtime |
| HUD/GUI/рендер/ресурсы | HUD snapshot/grindstone hypothesis, четыре depth layers, animation metadata, banner matrices, thaumometer coordinates, четыре armor модели, asset references | Реальные кадры, GPU/shaders, resource-pack reload в живом клиенте |
| API/аддоны | Достижимость virtual comparators, complex tag enrichment, public blacklist API, THEORY contract | Все внешние аддоны и их lifecycle conventions |
| Крафт/инвентарь/расход | Bucket debit/refund/reentry, grindstone, loot pick/place tiers; предыдущие crafting fixes входят в общую регрессию | Полное новое acquisition/survival-прохождение |
| Сеть/синхронизация | Единый champion synced ID и persistence; ранее исправленный HUD callback проверен по регистрации | Два клиента, latency, disconnect/reconnect matrix, NeoForge packets |
| Сохранение/lifecycle/cache | Praetor save/reload, Thaumatorium author roundtrip, Oculus full NBT rollback, renderer cache invalidation registration | Длительная миграция/старые миры (не разрешены), физический reload ресурсов |
| Мир/жидкости/эссенция | Dimension/biome gates, tree RNG admission, Nether exception, death bucket; existing worldgen/fluid suite при общей проверке | Все биомы/сиды/модовые dimension types, filesystem datapack loot reload |
| Сущности/AI/взаимодействия | 13 champion identities, attributes/hit effect, native armor animation primitive; golem actor contract по двум loaders | Полный повторный AI-аудит и игровой осмотр всех мобов |
| Исследования/магия/машины | Repair, Oculus, Sceptre capacity, complex aspects, author persistence, сложность/knowledge contracts | Новое полное исследовательское прохождение и все машины вручную |
| Качество тестов | Проверены новые fixtures/assertions, native callbacks/actors, независимые ревью важных патчей; общий JUnit/Python/GameTest набор | Построчное ревью всего прежнего тестового корпуса |

Полные локальные доказательства этой волны: ignored `.tmp/fix-followup/`, включая заметки шести агентов и независимые проверки champion/API/depth/worldgen/resource transactions. Исторические результаты предыдущей волны не выдаются за результаты новых исходников.

## Финальный Git status и созданные файлы

Полный вывод `git status --short --untracked-files=all`, ветка/HEAD, список12 новых source/test файлов и двух новых отчётных файлов этой волны, а также перечень всех текущих untracked файлов сохранены в [инвентаре файлов](2026-10-25-followup-files.txt). Существующие до этой волны незакоммиченные правки не сбрасывались. Transient логи, caches, test worlds и backup находятся в ignored-каталогах и в список новых исходников не включены.
