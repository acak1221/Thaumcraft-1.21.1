# Наполнение и Опустошение — Fabric, 19.10.2026

Интегрированы Fill0/Empty1 рядом с Gather2: исходные рецепты/исследования,
реальная установка ядра, маршруты между поддержанными контейнерами, количества
ghost-фильтров, precise/any, сохранение и отзыв. Bell меняет метки настоящего
выбранного голема; F очищает их через server-owned packet. Клиент показывает
метки/количества/правильное ядро. Ни меню, ни настройки не владеют настоящим грузом.

Источник: [контракт оригинала](2026-10-19-golem-fill-empty.md),
[согласованный API](2026-10-19-golem-inventory-api.md).
Передачи: [runtime](2026-10-19-golem-fill-empty-runtime-notes.md),
[data/bell](2026-10-19-core-data-notes.md),
[client](2026-10-19-inventory-core-client-notes.md).

Root-интеграция после передач: Bell use-first против перехвата chest GUI;
редактирование ссылок уже выбранного loaded UUID без повторного entity reach,
но с сохранением отказа unsupported/retiring; локализованный mode label;
точечный lines-buffer flush. Эти изменения новее хешей авторских передач.

## Проверка и ограничения

Фактическая Fabric main-сборка `golem-inventory-build1` **PASS39s**, remap и
production isolation. Private JAR10996795bytes/SHA256
`d27892c805ecc5ae59482ca77a43f1d688a7442de5fea98a4f6304ba5af58f1a`.
Игровой
перенос/GUI/F/сохранение/визуальные проверки пока **не выполнялись**. Новая
сборка не заменяет экспортированный Bore-only пакет.

Пока только конечный набор обычных vanilla/Hungry контейнеров. World-throw ветка
следующим отдельным изменением реализуется, но в этот PASS не входит;
отказ/полный/неподдерживаемый контейнер никогда не разрешает
сброс груза. Нет forced chunk loads, colored/fuzzy/upgrades/advanced или addon API
parity. Оригинальный duplicate-row negative deficit исправлен суммой wanted и
clamp; накладывающиеся Fill ghost controls заменены шестью логическими, без
создания реальных предметов. Mode сохраняется при отзыве — явная native-адаптация.
