# Закрытие остатков клиентского слоя TC4 4.2.3.5 → Minecraft 1.21.1 (Fabric)

Дата: 2026-10-21. Ветка: `port/clientfx`. Домен: `client_rendering_fx_input_and_gui`.
Источник истины: `/home/Acak/work/thaumcraft-4.2.3.5-oracle` (декомпиляция `Thaumcraft-1.7.10-4.2.3.5.jar`).
Второй источник: сам оригинальный JAR (только ассеты шейдеров — в декомпиляции оракула ассетов нет).
Нативный порт: `common/src/main/java/thaumcraft/`, `common/src/main/resources/assets/`.
Референс 1.7.10: `/home/Acak/work/mc1710-src` (только для чтения vanilla 1.7.10).
Инструмент SRG: `python3 -B scripts/porting/resolve_srg.py <srg-имя>`.

Метод: для каждой единицы прочитан оригинальный класс целиком, найден нативный аналог **по смыслу**
(поиск по графу вызовов в 859 классах оракула, а не по имени файла), сверены числа и порядок
вызовов. Каждая правка в ledger ссылается на `файл:строка` оракула. Клиентская визуальная
приёмка **не заявляется**: ни один из закрытых эффектов не смотрелся в игре.

---

## 1. Главное: цепочки пост-эффектов warp — gap был ложным, он закрыт

Прошлая волна закрыла все четыре warp-пост-эффекта как «непереносимый gap» с формулировкой
*«vanilla 1.21.1 не поставляет ни одной программы пост-эффектов, цепочки не грузятся»*
(`docs/analysis/2026-10-21-audit-a1-client.md:289-293, 355-356`). Обе посылки неверны.

**1а. Vanilla 1.21.1 поставляет программы пост-эффектов.** В `assets/minecraft/shaders/program/`
ванильного клиента 1.21.1 есть `blit`, `box_blur`, `color_convolve`, `blur`, `invert`, `bits`,
`entity_outline`, `spiderclip`, `transparency` и вершинные `sobel`, `screenquad`, `blit`, `blur`,
`rotscale` (проверено по `minecraft-client.jar` из loom-кэша, `assets/minecraft/shaders/**`).

**1б. Настоящая причина — правило разрешения имён.** `net.minecraft.client.renderer.EffectInstance`
(декомпиляция 1.21.1) резолвит программу поста только в **default namespace**:
`EFFECT_SHADER_PATH = "shaders/program/"` + `ResourceLocation.withDefaultNamespace(...)`, а вершинный
и фрагментный исходники — с тем же default namespace и расширениями `.vsh`/`.fsh`.
Пасс с именем `minecraft:box_blur` или `thaumcraft:bloom2` ищется как
`minecraft:shaders/program/minecraft:box_blur.json` и **не может быть найден никогда**.
Именно так и были записаны четыре нативные цепочки (`assets/thaumcraft/shaders/post/warp_*.json`),
поэтому `PostChain` ронял цепочку при загрузке.

**Что сделано:**

| действие | детали |
|---|---|
| цепочки переименованы в резолвимые имена | `warp_death_gaze` → `color_convolve` + `blit`; `warp_hunger` → `thaumcraft_color_convolve2` + `blit`; `warp_sun_scorned` → `thaumcraft_bloom_color` + `thaumcraft_bloom2`; `warp_blurred` → два `box_blur` |
| три собственных программы TC4 портированы в GLSL 150 | `assets/minecraft/shaders/program/thaumcraft_{color_convolve2,bloom_color,bloom2}.json|.fsh` + `thaumcraft_bloom_vertex_base.vsh`; исходники — `assets/minecraft/shaders/program/*` оригинального JAR (1.7.10 TC4) |
| числа и порядок оригинала сохранены | `post/desaturatetc.json` (Saturation 0.8), `post/blurtc.json` (Radius 10, BlurDir 1,0 затем 0,1), `post/hunger.json` (ColorScale 1,0.8,0.8, Saturation 1.1), `post/sunscorned.json` (порядок bloomColor → bloom2) |
| отклонение по `blurtc` задокументировано | OptiFine-программы `blur` в 1.21.1 нет; ближайшая ванильная — `box_blur`, Radius и двухосевая схема проходов сохранены |
| оригинальный выключатель восстановлен | `Config.shaders` (оракул `thaumcraft/common/config/Config.java:410-412`, дефолт `true` в статике :847) → `ThaumcraftSettings.shaders()`, ключ `shaders` в файле настроек |
| мёртвые файлы удалены | `assets/thaumcraft/shaders/program/*` (namespace, который 1.21.1 не резолвит) удалены скриптом-конвертером |
| регрессия закрыта тестом | `common/src/test/java/thaumcraft/client/WarpPostChainTest.java` падает, если пасс называет программу, которую не поставляют ни порт, ни 1.21.1, или если в имени есть namespace |

Конвертер `scripts/porting/convert_warp_shaders.py` переписан: он читает ассеты **из оригинального
JAR** (он tracked в репозитории) и порождает нативные цепочки и программы детерминированно.

---

## 2. Таблица: единица → вердикт → что сделано

Вердикты — это `review.disposition` / `review.source_semantics` / `review.native_coverage` в
`docs/porting/scope-ledger.json` после этой волны.

| единица (ledger) | вердикт | что сделано |
|---|---|---|
| `thaumcraft/client/fx/particles/FXSmokeSpiral.java` | `port` / `adapted` / `partial` | **Портировано.** `common/src/main/java/thaumcraft/client/fx/particles/FXSmokeSpiral.java`: поворот на 720°, полярный угол 90…−90°, радиус, квад со стороной 0.15 от масштаба частицы, альфа 0.66 от затухания, первые пять ячеек листа частиц, зеркальный порядок UV оригинала (оракул `FXSmokeSpiral.java:19-152`). Вызывающие стороны тоже портированы: `ItemElementalSword.onUseTick` (оракул `ItemElementalSword.java:128-141`, пять спиралей за тик + ванильный «smoke» при подкрадывании) и `EntityEldritchWarden` (оракул `EntityEldritchWarden.java:186-194`, 33 спирали за тик анимации появления). Числа закреплены тестом `ClientEffectsParityTest`. |
| `thaumcraft/client/fx/beams/FXArc.java` | `adapt` / `adapted` / `partial` | **Вердикт исправлен: «missing» было ложным.** Нативный `common/src/main/java/thaumcraft/client/fx/particles/FXArc.java` — это и есть порт оригинальной ленты (оракул `beams/FXArc.java:37-144`): та же дуга (гравитация 0.115, шум 0.25, максимум 50 шагов), та же лента из `textures/misc/beamh.png`, полуширина 0.25, альфа 0.8, аддитивный бленд 770,1, тот же UV «индекс точки / длина дуги». Пакет отличается, потому что в 1.21.1 нет `EntityFX`. Связь добавлена в `class-inventory.json`. |
| `thaumcraft/client/fx/beams/FXBeamPower.java` | `adapt` / `adapted` / `partial` | **Проверено по оракулу и достроено.** Нативный релей-луч (`TileVisRelayRenderer.java:32-40`, `VisNetRender.java:81-97`) уже повторял оригинал: полуширина 0.105 (0.15 от размера 0.7), скролл текстуры, пол 0.3, спад 0.025/тик, импульс 0.8 и десятикратное усиление для носителя `IRevealer` (оракул `FXBeamPower.java:105-158`). Добавлена недостающая **аддитивная вспышка** на листе частиц у цели (оракул `FXBeamPower.java:186-218`): полуразмер 0.66 от непрозрачности, ячейка по возрасту, альфа ×0.2 (×1.0 у ревилера). |
| `thaumcraft/client/fx/beams/FXBeam.java` | `exclude` / `not_applicable` / `missing` | **Мёртвый код в оригинале.** `PacketFXBeamPulse` зарегистрирован в `PacketHandler.java:95`, но **ни один класс** TC4 4.2.3.5 не конструирует и не отправляет это сообщение — в 4.2.3.5 этот луч не показывался. Нативных аналогов не требуется; `VisNetRender.beam` — это луч релея VisNet (FXBeamPower), его нельзя считать этим юнитом. |
| `thaumcraft/client/fx/beams/FXBeamGolemBoss.java` | `exclude` / `not_applicable` / `missing` | **Исправлен неверный вердикт прошлой волны** («видимый луч оригинала отсутствует»). `PacketFXBeamPulseGolemBoss` зарегистрирован в `PacketHandler.java:96`, но отправителей в оракуле нет — оригинал этот луч не показывал. Боевая механика безголового голема совпадает: оригинал стреляет `EntityGolemOrb` (оракул `EntityEldritchGolem.java:226-243`), нативный порт — тем же. |
| `thaumcraft/client/fx/particles/FXScorch.java` | `exclude` / `not_applicable` / `missing` | **Уточнена формулировка.** Единственная инстанциация — `UtilsFX.shootFire` (оракул `UtilsFX.java:139`), а у `shootFire` **ноль вызывающих** в оракуле: эффект был недостижим и в оригинале. Фокус огня 1.21.1 не нуждается в аналоге. |
| `thaumcraft/client/fx/particles/FXSmokeTrail.java` | `exclude` / `not_applicable` / `missing` | **Мёртвый код в оригинале.** Единственная инстанциация — `ClientProxy.soulTrail` (оракул `ClientProxy.java:892`), у которого ноль вызывающих. |
| `thaumcraft/client/fx/particles/FXBubbleAlt.java` | `exclude` / `not_applicable` / `missing` | **Мёртвый код в оригинале** (ни одной точки конструирования в оракуле). |
| `thaumcraft/client/fx/ITCParticle.java` | `exclude` / `not_applicable` / `missing` | Артефакт только 1.12-референса, в оригинале 4.2.3.5 такого класса нет. Нативные частицы 1.21.1 выбирают бленд и атлас явно. |
| `thaumcraft/client/fx/particles/FXSmokeDrift.java` | `exclude` / `not_applicable` / `missing` | Артефакт только 1.12-референса, в оракуле такого файла нет. |
| `thaumcraft/client/fx/beams/WandEffectOrigin.java` | `exclude` / `not_applicable` / `missing` | Вспомогательный класс 1.12-референса, в оракуле такого файла нет; нативные фокусы считают свою точку отброса сами. |
| `thaumcraft/client/gui/ThaumcraftGuiConfig.java` | `adapt` / `adapted` / `partial` | **Экран настроек портирован.** 1.21.1 не имеет Forge-экрана конфигурации, поэтому в порту свой: `common/src/main/java/thaumcraft/client/gui/ThaumcraftConfigScreen.java` с пятью оригинальными категориями (`general`, `Monster_Spawning`, `World_Generation`, `World_Regeneration`, `Research` — оракул `Config.java:104-111`) и оригинальными ключами/дефолтами (`Config.java:369-500`); каталог — `common/src/main/java/thaumcraft/common/config/ThaumcraftConfigStore.java`. Экран правит тот же файл `config/thaumcraft.properties`, что читает порт, и сохраняет чужие ключи и заголовок. Категория `World_Regeneration` в порту пуста (порту нечего регенерировать). |
| `thaumcraft/client/ThaumcraftGuiFactory.java` | `adapt` / `adapted` / `partial` | Точка входа заменена: в 1.21.1 нет списка модов и FML-фабрики, поэтому `common/src/main/java/thaumcraft/client/ThaumcraftConfigClient.java` открывает свой экран по клавише (по умолчанию не назначена, назначается в ванильном экране управления). |
| `thaumcraft/client/gui/GuiFactory.java` | `exclude` / `not_applicable` / `missing` | Артефакт только 1.12-референса (Forge `GuiFactory`), в оракуле такого файла нет; в 1.21.1/Architectury нет FML-хука. |
| `thaumcraft/client/lib/ItemAspectTooltipHandler.java` | `exclude` / `not_applicable` / `missing` | Артефакт только 1.12-референса. **Уточнено, что оригинальный аналог тоже не перенесён и почему:** `ClientTickEventsFML.renderAspectsInGui` (оракул :660-724) рисует ряд аспектов над наведённым слотом, но только для **просканированных** предметов, а реестр просканированных объектов (`Thaumcraft.proxy.getScannedObjects()`, оракул `ScanManager.java:290-336`) в порте 1.21.1 отсутствует. Ряд аспектов в порте не рисуется. |
| `thaumcraft/client/integration/jei/JeiRecipeData.java` | `exclude` / `not_applicable` / `missing` | **Не переносимо без внешнего мода — задокументировано.** В оригинале TC4 4.2.3.5 интеграции JEI нет вообще: в JAR нет ни одного класса JEI, единственная связанная строка — `ClientProxy.java:444` `Loader.isModLoaded("NotEnoughItems")`, которая ставит `Thaumcraft.aspectShift` (сдвиг иконок аспектов в GUI из-за NEI/OptiFine). В 1.21.1 JEI — отдельный мод со своим API-артефактом (`maven.blamejared.com`) и своей точкой входа; порт не объявляет зависимость JEI и не регистрирует `jei`-энтпоинт. Перенос плагина = новая фича и новая рантайм-зависимость от внешнего мода, а не порт оригинала. |
| `thaumcraft/client/integration/jei/ResearchVisibility.java` | `exclude` / `not_applicable` / `missing` | То же решение (см. выше). |
| `thaumcraft/client/integration/jei/ResearchVisibilityTicker.java` | `exclude` / `not_applicable` / `missing` | То же решение (см. выше). |
| `thaumcraft/client/integration/jei/ThaumcraftJeiPlugin.java` | `exclude` / `not_applicable` / `missing` | То же решение (см. выше). |
| `thaumcraft/client/integration/jei/ThaumcraftRecipeCategory.java` | `exclude` / `not_applicable` / `missing` | То же решение (см. выше). |
| `thaumcraft/client/integration/jei/ThaumcraftRecipeWrapper.java` | `exclude` / `not_applicable` / `missing` | То же решение (см. выше). |

Итог по строкам ledger: в таблице 23 единицы, изменено 19 из них; 5 переведены из «нет нативного аналога» в
рабочий/частичный нативный код (`FXSmokeSpiral`, `FXArc`, `FXBeamPower`, `ThaumcraftGuiConfig`,
`ThaumcraftGuiFactory`), 3 строки переведены в «мёртвый код в оригинале» (`FXBeam`,
`FXBeamGolemBoss`, `FXScorch`/`FXSmokeTrail`), 11 строк остались `exclude` с честным обоснованием
(нативного аналога нет и он не требуется).

---

## 3. Что осталось пробелом (обоснование)

1. **Оптимизация внешних программ в 1.21.1.** Собственные программы TC4 (`bloomColor`, `bloom2`,
   `color_convolve2`) переписаны в GLSL 150 построчно; математика оригинала сохранена, включая
   оригинальную особенность `bloomColor` (деление на `delta` без защиты → NaN на серых пикселях).
   Это поведение оригинала, а не порок порта.
2. **`blurtc` (размытие).** Оптиминовской программы `blur` в 1.21.1 нет; взята ванильная
   `box_blur`. Числа (Radius 10) и порядок проходов сохранены, алгоритм отличается (прямоугольное
   размытие вместо гауссова).
3. **Аспект-иконки в GUI контейнера.** Требуют реестра просканированных объектов оригинала
   (`Thaumcraft.proxy.getScannedObjects()`), которого в порте нет; это отдельная подсистема
   сканирования, не входящая в клиентскую волну.
4. **JEI.** См. §2 — требует внешнего мода JEI и его API-артефакта; в оригинале TC4 4.2.3.5
   интеграции нет.
5. **`Config.shaders` как пользовательская настройка.** Перенесён как локальный ключ файла
   настроек (в оригинале это тоже клиентский файл конфигурации). На выделенном сервере файл
   настроек серверный, поэтому клиент читает свой.
6. **Визуальная приёмка.** Ни один из перенесённых эффектов (спираль дыма, вспышка релея, луч
   релея, пост-эффекты warp) не наблюдался в игре. Все вердикты по визуальной части — `partial`,
   не `parity_verified`.

## 4. Вне моей области (не переаудировано)

`ParticleEngine`, `WRMat4`, `WRVector3`, `FXBeamWand`, `FXBeamBore`, `FXLightningBolt*`,
`ClientTickEventsFML`, `RenderEventHandler` и остальные строки домена
`client_rendering_fx_input_and_gui` не переаудировались — они принадлежат другим волнам
(`audit-a1-client`, `audit-a3-devices`, `audit-a9-core`). Их вердикты в ledger я не менял.

---

## 5. Реальный вывод проверок

Команда (как в задании):

```
export JAVA_HOME=/home/Acak/.jdk21; export PATH=$JAVA_HOME/bin:$PATH
export GRADLE_USER_HOME=/home/Acak/gradle-homes/clientfx
./gradlew --no-daemon --console=plain :common:test :fabric:compileClientJava
```

Результат:

```
BUILD SUCCESSFUL in 27s
9 actionable tasks: 4 executed, 5 up-to-date
EXIT=0
```

Тесты `:common:test`: **256 классов тестов, 1781 тест, 0 падений и 0 ошибок**
(`common/build/test-results/test/*.xml`, сумма `tests`/`failures`/`errors`).

Новые тесты этой волны:

* `thaumcraft.client.WarpPostChainTest` — 3 теста: каждая цепочка warp называет резолвимую
  программу (без namespace), порядок пассов и uniform-ы совпадают с оригинальными JSON, у
  портированных программ есть вершинный и фрагментный исходники.
* `thaumcraft.client.ClientEffectsParityTest` — 5 тестов: числа спирали дыма (720°, 90…−90°,
  радиус), пять оригинальных категорий экрана настроек, ключи экрана принадлежат оригинальному
  `Config`, файл настроек сохраняет заголовок и чужие ключи при записи.

Сборка клиента: `:fabric:compileClientJava` — успешно (в логе только штатные предупреждения
`deprecation`/`unchecked` и одно предупреждение маппинга `@At(INVOKE)` в чужом
`ResearchNoteStackCodecMixin`).

Инвентаризация и ledger после правок:

```
python3 -B scripts/porting/update_migration_inventory.py   # check: legacy_units 934, partial 42, pending 892, native java 1312
python3 -B scripts/porting/update_scope_ledger.py --original-root /home/Acak/work/thaumcraft-4.2.3.5-oracle --check
  # union_rows 1044, native_linked_rows 37, unique_native_counterparts 57, stale_review_fingerprints 0
```

---

## 6. Коммиты

* `00f54e9` — Load the four warp post chains in 1.21.1 and port the smoke spiral
  (цепочки warp, программы постов в default namespace, `Config.shaders`, тест цепочек,
  нативный `FXSmokeSpiral` + вызовы меча и стража).
* следующий коммит — экран настроек, вспышка луча релея, правки ledger/инвентаризации и этот отчёт.
