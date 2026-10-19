# Проверка checkpoint без устаревшего evidence

Успешная сборка не означает полного переноса TC4. Проверяются отдельно pure tests,
настоящие серверы обоих loaders, содержимое JAR и просмотренные клиентские сцены.

**С16.10 текущая разработка Fabric-first по решению пользователя.** Обычные
devscripts выполняют только Fabric. Recorder по умолчанию выбирает`--loaders fabric`:
свежие FabricXML/JAR обязательны, NeoForgeXML/JAR не читаются и не выдаются за PASS.
`loaders_checked`/`loaders_not_checked` фиксируют эту границу в отчёте. Исторические
двухloader-checkpoints сохранены; NeoForge возобновляется после полного Fabric.
Описание двухloader-режима ниже относится к отложенной фазе, не текущему требованию.

`record_verification.py` теперь требует снимок входов **до** запуска проверок.
Он включает все активные source sets (`main`, `test`, `gameTest`, `clientTest`),
ресурсы, исходные ресурсы конвертера, Gradle wrapper/configuration и скрипты.
Используются реальные байты рабочего дерева, включая незакоммиченные файлы.
Приватная декомпиляция, старые Java-исходники, миры и caches в снимок не входят.

## Воспроизводимый локальный запуск

1. Завершить изменения во всех source sets; не запускать два Gradle/Loom процесса.
2. Создать **новый** snapshot, не переиспользуя имя старого запуска:

   ```sh
   python -B scripts/porting/record_verification.py --begin-run .tmp/my-run-inputs.json
   ```

3. Запустить Python tests с сохранением stdout/stderr в `.tmp/my-run-assets.log`:

   ```sh
   python -B -m unittest discover -s scripts/porting -p 'test_*.py'
   ```

4. С Java21 выполнить `./scripts/dev.sh validate --rerun-tasks` либо
   `./scripts/dev.ps1 -Task validate -RerunTasks`. Сохранить полный вывод в
   `.tmp/my-run-validate.log` и **реальный** exit code в
   `.tmp/my-run-exit.json` (`{"code":0}` допустимо только после успешного процесса).
   Повторное исполнение требуется, потому что up-to-date XML/JAR не являются
   свежими результатами нового запуска. В `validate` также компилируется
   изолированный Fabric `clientTest`, но сам GUI этим не запускается.
5. Записать результат:

   ```sh
   python -B scripts/porting/record_verification.py \
      --loaders fabric --run-snapshot .tmp/my-run-inputs.json \
     --build-exit-json .tmp/my-run-exit.json \
     --build-log .tmp/my-run-validate.log \
     --asset-test-log .tmp/my-run-assets.log
   ```

Recorder отклоняет изменившиеся/добавленные/удалённые входы, старые либо пустые
отчёты и артефакты, failures/errors/skips/disabled, дубли, отсутствие отчёта любого
common `*Test.java` и отсутствие обязательных GameTests. Исходные 41 case IDs закреплены в
`scripts/porting/required-gametests.json` и проверяются **для каждого loader**;
посторонний новый тест не заменяет пропавшую регрессию.

Проверяются hashes XML/JAR/logs и актуальные notices. В production JAR недопустимы
test helpers/wrappers/client harnesses, test templates, test-mod metadata,
`fabric-gametest` entrypoint и ссылки production-классов на тестовую поддержку.
Отдельный `--check-artifacts` (старое имя `--check-notices` также работает)
проверяет упаковку без заявления о новом runtime.

## Что остаётся отдельной приёмкой

- GameTests — dev dedicated runtime, а не запуск remapped JAR в обычной установке.
- Компиляция `clientTest` — не исполнение сценария и не осмотр картинки.
- Старые просмотренные сцены сохраняют историческую ценность. При передаче их
  recorder сравнивает записанные source fingerprints с текущими и явно отмечает
  несовпадение; старый client PASS не подтверждает новые исходники/сборку.
- Сохранение всей TC4 механики, реальные два игрока, survival progression,
  visual/audio parity и незавершённые M00–M15 не выводятся из зелёных тестов.

GitHub workflow использует тот же протокол со свежими входами и результатами.
Редактирование workflow не означает его удалённого исполнения или публикации.
