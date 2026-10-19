# Соломенный голем + Сбор — первая рабочая группа големантии

**Straw/Gather собран19.10; не проверено в игре.** Авторитетные значения
и границы — [original review](2026-10-18-straw-golem-original.md). Не весь M10.

Root интегрировал регистрацию living entity0.4×0.95, четыре предмета, компонент
настройки, меню, четыре исходных рецепта, книгу/иконки и переводы. Соломенный голем
создаётся в тигле из сена, не инфузией. Blank core=100, Gather=2, без core=−1.
Голем, предметы/сохранение и клиентский рендер разрабатываются раздельно; текущие
рабочие файлы авторов не считаются завершённой реализацией до их передачи.

Клиентская часть передана: оригинальная модель, sway/анимация, fitted-core badge,
реальный carried-item render, damage overlay, экран с preview настоящего голема
и has_core predicate. Root согласовал predicate с RegistrySupplier-компонентом.
Компиляция и просмотр в игре на момент этой передачи не выполнялись.
Предметы/data также переданы: guarded placer, blank/Gather core, bell selection/
локальные markers, native component codecs и semantic full-stack equality для
шести ghost-образцов. Root добавил указанные tooltips. Маршрутизация markers,
их рендер и клавиша reset пока не реализованы и прямо отмечены в предмете.

Runtime передан и интегрирован: native AbstractGolem, Gather pickup/deposit/return,
1-item carry, шестислотовый ghost-фильтр, pause/configuration, repair, safe home
return, recall и cargo-only death drops, entity persistence/sync. Root согласовал
импорт современного ContainerGolem и дробный bootup float между entity/моделью;
подбор читает исходный native pickupDelay<5 через небольшой AW. Optional анимация
открытия домашнего сундука, fence-gate path handling и client boot sound отложены.
Фактическая main-сборка Fabric `straw-golem-build1` **PASS46s**, включая remap и
production isolation. Private JAR10945193bytes/SHA256
`5e43b5b9040a418684d4814e215e5669c89e81d94969e1f0ab81177cadfad9fa`.
Игрового запуска не было. Следующая группа —
[Wood/Clay/Stone/Iron](2026-10-19-golem-materials.md); этот PASS не переносится
автоматически на её изменённые исходники.

## Меню и ресурсы

Шесть ghost-образцов не принадлежат реальному инвентарю голема: отдельный menu-only
SimpleContainer, нет hopper-доступа, выдачи/сброса образцов или расхода курсора.
Сохранены исходные координаты слотов/176×166 GUI. Native entity ID дополнен UUID
для привязки меню; конфигурация приостанавливает только этого голема.

Явное исправление старого ContainerGolem: shift-перенос ограничен реальными
слотами игрока. Исходный transferStackInSlot мог превращать ghost-образцы в реальные
предметы либо расходовать настоящие предметы на фильтр. Copy/очистка фильтра
остаются отдельными server-authoritative действиями. У Gather нет цветов/fuzzy
настройки без ещё не перенесённых upgrades.

Порядок новых crucible recipes монотонен по исходным строкам164/172; обе стоят
после ранее реализованных ранних рецептов, Gather раньше будущих Fill/Empty.
Используются исходные цены и реальные native RecipeManager holders. Рецепты
blank/bell сохраняют Nitor/brick/quartz как предметы, stickWood как существующий
native тег. Иконки регистрируются только для Straw0/Gather2/Bell, не будущих variants.

## Границы

Активные авторы завершают живой AI pickup/deposit/return, 1-item carry, фильтры,
recall/placer persistence, предмет колокольчика и world/client presentation.
После передачи — root API-интеграция и фактическая компиляция Fabric. Не запускать
Gradle на их незавершённых файлах. Длинный smoke/новый аудитор не разрабатываются.
Итоговый игровой проход отложен пользователем, не объявлен PASS.

Прочие материалы, остальные cores, advanced/upgrades/decorations, цветная/fuzzy
маршрутизация и полная addon/API parity не входят в эту первую группу. Поставка
бураf70b32... остаётся прежней, миры/существа/зависимости не изменяются.
