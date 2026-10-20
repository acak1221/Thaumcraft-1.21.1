# Инфузия зачарований — original контракт и native реализация, 20.10.2026

Спецификация: original TC4 4.2.3.5, приватная CFR-декомпиляция вне репозитория:
`api/crafting/InfusionEnchantmentRecipe`, `TileInfusionMatrix:278–333,421–509,578–606`,
`ConfigRecipes:297–321`; ItemResource metadata6 — amber, metadata14 — Salis Mundus.
Проверены также реальные vanilla1.7.10 классы из официального клиента Mojang:
SHA-1 `e80d9b3bf5085002218d4be59e668bac718abbc6`. `afv` (EnchantmentHelper), метод
`Map a(add)` строит **LinkedHashMap** в порядке NBT enchantment list, и setter пишет тот же порядок.
Все 22 minEnchantability(1)/maxlevel выполнены reflection-ом исходных классов `aft`/подклассов;
результат удержан вне репозитория в `/workspace/work/minecraft-1.7.10-oracle/enchantment-costs.txt`.

## Original контракт

- Обычные STACK recipes проверяются раньше enchantment recipes. Research — `INFUSIONENCHANTMENT`.
- Центр должен быть поддерживаемым damageable tool/armor; уже наложенное целевое зачарование ниже max,
  каждое другое — взаимно совместимое. Компоненты сравниваются неупорядоченно, без лишних элементов.
- Цель получает +1 уровень. На финише меняется **фактический** центральный предмет; добавленные во время
  ритуала допустимые компоненты/износ не должны теряться из-за замены копией предварительного результата.
- Нестабильность: base + floor(сумма уровней всех зачарований / 2).
- Essentia modifier: уровень цели, затем по каждому другому зачарованию прибавляется `level * 0.1f`;
  каждый аспект оплачивается как `base + (int)(base * modifier)`, а не округлением вверх.
- XP — целые **уровни**, `max(1, original minEnchantability(1) / 3) * (1 + current target level)`.
  Старые first-level minimum costs записаны явно в recipes; современные datapack costs их не заменяют.
- Сначала XP: первый игрок с положительным уровнем в AABB матрицы, расширенном на 10 по каждой оси,
  теряет один уровень за cycle, получает magic damage 0 или 1; следующий интервал — 20 тиков.
  Нулевой баланс у присутствующих игроков с шансом 1/3 добавляет единицу случайного требуемого аспекта;
  отсутствие игроков просто останавливает процесс. После XP интервал возвращается к 10 тикам.
- Затем настоящий забор эссенции и реальные компоненты по прежнему matrix lifecycle.
  XP и enchantment identity сохраняются в незавершённой сессии; transient count/delay при reload сбрасываются.

## Native адаптации и границы

- Enchantment registry holders/resources заменяют числовые id. Vanilla22 targets сохраняют original категории
  предметов через native armor/tool классы, поскольку современные tags сами по себе исключили бы TC-инструменты
  и допустили новые 1.21 категории (например мотыги для efficiency). Совместимость задаётся native exclusive sets.
- Другие уровни складываются по entries native enchantment map; исходный порядок старого NBT list/LinkedHashMap
  в новом native компоненте ItemEnchantments отсутствует. Это явная адаптация формата 1.21.1, не bitwise parity
  порядка float additions со старым сейвом. Импорт миров 1.7 не поддерживается. Float additions не агрегируются;
  на пограничном усечении другая перестановка уровней может изменить surcharge на одну единицу.
- Recipe instability >24 и переполнение стоимости отвергаются до старта: исходные shortage RNG bounds при
  повреждённых/аномально высоких данных иначе были бы неположительными и приводили бы к падению.
- Авторство native-сессии всегда задаётся текущим игроком через UUID: original enchantment branch не обновляла
  recipePlayer и могла использовать имя предыдущего ритуала. Это не даёт прежнему пользователю чужой crafting event.
- Первые 22 vanilla recipes и actual matrix path — текущая группа. Repair/Haste требуют своих native
  enchantment definitions/effects и остаются отдельной следующей группой; Runic Augmentation также отдельно.

## Evidence

**Implemented:** native recipe type/serializer, 22 JSON, original cost policy, XP phase и dynamic actual-centre
result в настоящей matrix, сохранение XP progress/цели, существующий native essentia executor и ingredient path.
Книга разрешает original `InfEnch0..21` к этим же RecipeHolders и показывает cycling I..max, original
component ring, XP и аспекты через методы той же native recipe; base instability как в original GUI.
Это implemented client renderer, а не inspected client evidence.
`InfusionEnchantmentRulesTest` содержит original arithmetic vectors. `EnchantmentInfusionRuntimeChecks` использует
настоящего подключённого игрока, объявленные formed-altar/skull/resource fixtures, реальные жезл/матрицу/банки/
пьедесталы: admission research/type/max/conflict; exact XP before essentia/components; отсутствие игроков;
save/load после одного оплаченного уровня; сохранение актуального wear/CustomData; отсутствие повтора.
Первый focused ритуал вызывает actual matrix ticker синхронно; второй ритуал Protection I на chestplate идёт
**полностью на обычных серверных тиках**, с проверкой точных levels/essentia/components/output.
Никакой build/runtime PASS пока не заявлен: итоговую сборку запускает ведущий агент.
Страница книги/FX в запущенном клиенте, multiplayer/world restart и NeoForge пока не проверены.
