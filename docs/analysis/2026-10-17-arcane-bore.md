# Arcane Bore — следующая bounded Fabric-группа

Дата17.10.2026. **Обновление18.10: предварительно экспортирован по запросу пользователя,
полная игровая приёмка отложена.** Свежий production build45s, JAR10841529bytes/
SHA256 `f70b32f4c1698bf3247afd1bb1525f69cdc341e045ff605d4b3ed6db8196fa8f`.
Запись: `../porting/bore-export.json`. Непринятые native16–26 не повышены до PASS;
доработка smoke-тестера остановлена, работа продолжена над голодным сундуком.
Ниже сохранена история состояния17.10; её запрет экспорта отменён этим запросом.
Предыдущая принятая поставка остаётся
принятым focal `final7/client14`, SHA256
`0104dc3756199192b56af2956a6f544702b2c6bb5848fc725d96ba4f051e3f10`.
Принятые binary/save/evidence не меняются при разработке этой группы.

## Authority и объём

### Текущая интеграция после prerequisite3

`bore-runtime-validate9` теперь фактически **PASS284JUnit/129requiredFabric/
76Python98.046s**,0failures/errors/skips,3917unchangedinputs. Настоящая native
добыча алмаза деревянной киркой дала ровно один UUID-связанный дроп и износ1,
без XP. Исправлено ложное инвалидирование обычного nonBE lookup: реальная
фабрика BE по-прежнему fenced до вызова. Сохранение/эвакуация/повреждённые
очереди/меню/коллизия/исходные рецепты включены. JAR10,841,529bytes/f70b32f4...
и вся свежая доказательная база в `.tmp/bore-runtime-validated9/`; **не экспорт**.
Последующие native fault/protection cases готовятся в ignored staging; не
выдавать общий PASS за их выполнение. Клиент, точные PNG и stopped audit бура
ещё не выполнены. Accepted focal payload191 и все3distributionJAR неизменны.

Первый общий `bore-runtime-compile1` запущен после freeze всех owners;128native
required cases и284JUnit/76Python — пока authored ожидания, не результат. Помимо
обычной добычи/меню/коллизии добавлены настоящие native callback/retirement и
повреждённый saved-input regression cases. Ревью заранее выявило и исправило
4arg destroyBlock dispatch gap, reserved Bore UUID→legacy downgrade, а также
потерю actual dying contents при неуспешной подготовке эвакуации. Последний путь
теперь передаёт реальные slot objects в ONE unsendable level-queue custody и
очищает старый BE в том же rawstep; стабильная эвакуация остаётся физической.
Ошибочный codec aborts save, сохраняя единственного live owner для retry, а не
пустую диагностику вместо предметов. Все эти изменения ещё не compiled/native
PASS и не full inventory-routing/вакуум/API/power-loss parity. Подробнее в handoff.

Фактически `bore-prereq-validate3`: **284JUnit/116Fabric/74Python103.798sPASS**,
3873unchangedinputs. Четыре поршня реально дали68единиц с полным учетом побочных
аспектов/топлива/банок/флакона, включаяMotus16. Fresh development JAR
10,676,408bytes/c8b756ba... проверен и **не экспортирован**; архив
`.tmp/bore-prereq-validated3/`. Это последний проверенный набор, не новые изменения.

После него начата активная интеграция. Root добавил registry/два исходных рецепта,
alias/icon, wand-before-casting, платформенный native event bridge и ресурсы.
Шесть runtime-классов пока frozen в ignored staging; реальные сервисы добычи,
сохранения output/wear, native-origin/lifecycle guards и GUI/BER пишутся разными
owners. Новые зависимости ещё не сведены и не скомпилированы. Новый SERVER-case
base-payment/top predicates authored,117required — **ожидание**, не результат.
Не запускать Gradle до freeze всех активных owners. Поставку focal и191файл
принятого сохранения не менять; working-Bore/визуальной приёмки пока нет.

Original TC4 4.2.3.5 private oracle — единственная игровая спецификация. Root и
read-only audit адресно сверили pinned OpenTC4
`80aac29c52543ef51766c4b0788fba782abb3b0b`; это справочник, не donorimplementation.
Подробный audit с исходными anchors и hash — `.tmp/arcane-bore-original-audit.md`
(`559f9fbed127df63bac342f1a720fa1cc7880013b06a67330ae42398eac8dd58`).
Root scope decision — `.tmp/arcane-bore-root-scope.md`.

Цель: stationary base+top, настоящее получение, focus/pick slots, rotation/redstone,
исходный spiral scan, slow baseline и настоящий rawPerditio boost, protected native
harvest/output/persistence/GUI/render. Это не мобильная entity/quarry framework.
Пока ни один bore block/recipe не зарегистрирован; работа ниже — prerequisite и
математика, не готовый игровой бур и не закрытый M09.

## Первые независимые source owners

### Фактический prerequisite-прогон (не приёмка бура)

`bore-prereq-compile1` exit0; первый FULL1 остановился на одном новомJUnit тесте
неизменяемых native text siblings (283/1failure, не production ошибка). Root
исправил только тест: допустил native отказ изменить immutable siblings, сохранив
попытки изменения поддерживаемого style/NBT/count и строгую проверку независимых
копий. Failed evidence сохранён, native/JAR в FULL1 ещё не запускались.

Свежий **`bore-prereq-validate2`:283JUnit/115requiredFabric/74Python95.563sPASS**,
0failures/errors/skips, **3872unchangedinputs**. Все113previousnativecases сохранены.
Два новых native cases реально проверили codec/network и selected-session NBT/
замену изношенных инструментов/позднее потребление;45новых pureJUnit проверили
declarations, math и bundle planner. Это synchronous nativefixtures/purechecks,
не получение бура или elapsed-world client evidence.

Fresh development JAR10,675,936bytesSHA256
`90cd9bccf505a279a18133b32f798f3fba785c5f7dcc96a56b0ff0eb481d106a`,
notices/isolation/oneintermediaryAW проверены. Архив `.tmp/bore-prereq-validated2/`.
**Не экспортирован:** в distribution остаётся focal0104dc...,191acceptedparent
files неизменны. Никакой Bore block/recipe/runtime ещё не подключён.

Следующий material prerequisite — original piston complex332(machina2+motus4)
поверх настоящей recipe-derivedbase и native перегонка; это позволит честно
получить Motus для112essentia инфузии. Source author работает только над этой
строкой/целевыми tests; результатов пока нет. Runtime base/top/menu и loader
event bridge пока authored в ignored staging, без active promotion/compile/pass.

1. **Infusion prerequisite:** явный optional damage predicate только для
   объявленного компонента. Будущая bore-инфузия потребляет pristine diamond pick и
   shovel, но допускает дополнительные enchant/name components. Не global rule и
   не рецепт-ID hack. Codec/network/selected-session save/load/позднее потребление
   обязаны сохранять тот же predicate. Existing Silverwood/cap defaults и отдельное
   centerStillMatches-ignore-wear правило не меняются. Owner A меняет только
   Recipe/Matching/Matrix и новые helper/tests; root интегрирует manifest/gates.
2. **Pure math:** owner B пишет только новый ArcaneBoreMath и numericalJUnit source:
   rotation/spiral/projection, MAXfortune/SUMspeed, width5/7/9, slow×4, fractional
   credit/debt/short truncation. Runtime calls/блоки/визуализация не подключаются
   до root review. Это actual formulas, а не predetermined fixture target list.
3. **Protection/output design:** owner C пишет только ignored read-only contract
   note. Root подтвердил наличие installed Fabric FakePlayer и native
   PlayerBlockBreakEvents, но одно наличие API не является harvest доказательством.
   Held Excavation permits/ray/online-player fences нельзя расширять на машину.

Нет активных Gradle/game/Java запусков до freeze всех пишущих owners. Agents не
запускают tests/игру/NeoForge/Git и не меняют accepted records/distribution/saves.
Root единолично выполняет actual compile/tests/PNG/stopped audit/export.

## Исходные контракты, которые нельзя сгладить

- Base: AIR10/ORDER10,4greatwoodplanks/4iron/dispenser, concealed primary ARCANEBORE.
- Top: piston center,2greatwood/2gold/diamondpick/shovel/air+earthshards,
  potentia16/perfodio32/machina32/vacuos16/motus16, instability4.
- Установка above/below и mining direction независимы от nozzle. Wand side-click
  задаёт сторону без vis, redstone включает добычу. Никакого player-online gate.
- Target scan имеет64candidate reads longitudinally+3…+66 и отдельную nearer
  ray-hit коррекцию. Не квадратная quarry/обычный player ray/1…64 substitute.
- Boost unconditional prefill <20: до100RAWPerditio /5F, затем при нужных условиях
  одна Perditio essentia+20. Fraction/debt/overshoot, short cast и select-next-delay
  BEFORE previous-dig debit сохраняются. Frugal не удешевляет бур.
- Original direct break не имел современной защиты. Отдельная machine boundary и
  revalidation target/base/top/tool/focus — явная authority/crash/loss hardening,
  не ложное утверждение об исходной protected-harvest compliance. Никаких XP.
- В первой bounded machine slice разрешён явный отказ target-BE/pre-existing-loose
  vacuum ДО мутации, пока ownership/output branch не реализован. Это safety scope
  limitation, не готовая original vacuum parity. Не удалять чужие items/содержимое.
- Plain Thaumium pick не получает бесплатный Repair; focus Silk/Dowsing/native
  clusters/Arcane Lamp/addon inventories и другие late units остаются pending.

## Сохранения и будущая проверка

Единственный новый accepted parent — stopped focal14
`fabric/run-clienttest/saves/focal-smoke-8fd11823-4346-48f9-97ed-e06dbf9ec4be`.
Root read-only manifest `.tmp/bore-accepted-parent-before-client.json` содержит
**191file hashes** (session.lock исключён), а не старое M09b число187. Только fresh
private copies; failed/исторические focal worlds не использовать как inputs.
Исторические stocks/acquisition/research объявлять как inputs, не выдавать за
полученные заново. Ни XP/vis/essentia/finished outputs, ни RNG/clock/AI resets.

После SOURCEFROZEN: root review, реальные целевые Fabric/JUnit/Python checks,
свежие XML/JAR/input hashes. Никакой compiler-only результат не закрывает gameplay.
Focused client требует настоящий base craft/top112essentia infusion, installed
focus/pick, native target/count/boost/output/payment ledger, точные inspected PNG,
нормальную остановку и независимый NBT/191parent-file audit. Все11named/NoAIactors
и неизрасходованные stocks сохранять; natural node/ambient lifecycle наблюдать,
а не замораживать. Packaged/EULA-server/remote/full survival/API/NeoForge отдельно.

**Игровой приёмки бура ещё нет.** Prerequisite283/115/74 относится только к его
frozen3872inputs, не последующему piston/runtime коду. Accepted focal238/113/74
остаётся отдельным source-bound3865inputs checkpoint и текущей поставкой.
