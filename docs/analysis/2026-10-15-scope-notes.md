# M00: воспроизводимый учёт исходного Java scope

Дата: 15.10.2026. [Scope ledger](../porting/scope-ledger.json) — очередь аудита, а не завершение M00 или оценка процента готовности игры. Начальный снимок содержит **859 original**, **934 legacy**, **1044 union paths**: 749 `shared_path`, 110 `original_only`, 185 `legacy_only`. У всех строк `review.disposition = needs_review`; исходные игровые контракты по всем строкам ещё не проверены.

Скрипт [update_scope_ledger.py](../../scripts/porting/update_scope_ledger.py) читает приватный original oracle за пределами Git, retained `src/main/java` и явно составленные mappings [class-inventory.json](../porting/class-inventory.json). В ledger сохраняются только относительные пути, source aliases, SHA-256, число строк и метаданные. Тело приватного исходника не копируется. Хэши используют UTF-8 без BOM с нормализацией CRLF/CR в LF; содержимое и финальная новая строка сохраняются.

```powershell
py -3 scripts/porting/update_scope_ledger.py --self-test
py -3 scripts/porting/update_migration_inventory.py --write
py -3 scripts/porting/update_scope_ledger.py --write
py -3 scripts/porting/update_scope_ledger.py --check
```

`--original-root` позволяет указать другой приватный oracle. По умолчанию используется соседний `work/thaumcraft-4.2.3.5-oracle`; абсолютный локальный путь в JSON не записывается. `--self-test` использует временные синтетические файлы и не требует приватного oracle. Полная проверка ledger требует доступности обоих источников.

Каждая строка соответствует одному относительному Java path. `shared_path` сообщает лишь совпадение пути, **не совпадение поведения**. Перемещённые классы могут оставаться разными строками; доказанный semantic link следует фиксировать в `review.evidence` и `review.notes`. Нельзя исключать 185 legacy-only классов как TC6 extras или объявлять original-only dead code без исследования регистраций, всех вызовов и условных путей. Скрипт не делает таких выводов.

`native_counterparts` берутся только из `legacy_units[].native_paths` authored class inventory. Его `partial/pending/complete` и `parity_verified` сохранены отдельно как `class_inventory_mapping`: они не становятся verdict о соответствии original. После явного note mapping refresh linked23 строки /28 уникальных native paths; это остаётся только authored mapping. Это число также не измеряет полноту native реализации.

Первоначальные `queue.subsystem_family` и `queue.planned_milestone` — эвристика имени/пути, помеченная `path_name_heuristic_unreviewed`. Это основной маршрут следующего аудита; классы с несколькими ролями требуют уточнения. Рендеры и GUI реализуются с соответствующей gameplay unit, а M14 означает их последующий послойный аудит. M15 — сценарии приёмки, поэтому не обязан иметь отдельный source path. После ручной правки `queue` скрипт сохраняет её; `basis` следует заменить на описание authored решения.

Поля `review` заполняются вручную после аудита:

- `disposition`: `needs_review`, `port`, `adapt`, `replace`, `exclude`;
- `source_semantics`: `unreviewed`, `equivalent`, `different`, `adapted`, `not_applicable`;
- `callsite_audit`: `not_started`, `partial`, `complete`;
- `native_coverage`: `not_assessed`, `missing`, `partial`, `parity_verified`;
- `owner`, `evidence`, `notes` и `reviewed_sha256` с ключами `original`/`legacy` для реально рассмотренных fingerprints.

`exclude` требует явно записанных complete callsite audit и evidence. `native_coverage = parity_verified` требует evidence; скрипт сам его не устанавливает и не проверяет истинность описанного игрового сценария. При изменении источников authored review сохраняется; если он привязан к fingerprints, `review_fingerprints_current` становится false. Null означает отсутствие fingerprint-привязки, а не актуальный подтверждённый verdict.

`--write` обновляет вычисляемые метаданные, сохраняя `review` и `queue`. Он отказывается терять существующую строку или одну из её source sides, принимать дубли путей, dangling native counterparts или устаревший legacy fingerprint в class inventory. Добавленные source paths создают новые `needs_review` строки. `--check` ничего не пишет и возвращает ошибку при расхождении с воспроизводимым результатом. Исправления source layout или схемы выполняются явно с сохранением прежнего решения и evidence.

Выполнено при создании: synthetic `--self-test`, полный `--write`, затем `--check`; наборы путей совпали с ожидаемыми 859/934/1044. Game build/runtime здесь не запускались. Все semantic/callsite verdicts, полный registry/resource scope и реальные acceptance сценарии остаются отдельной работой M00 и последующих этапов.
