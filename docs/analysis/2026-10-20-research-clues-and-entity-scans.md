# R12 — clue triggers и entity scans,20.10.2026

Статус: implementation in progress; compile/runtime не запускались. Original TC4 4.2.3.5 oracle вне repo — ResearchManager.createClue84–117; ScanManager.generateEntityHash76–130/generateEntityAspects183–235/completeScan351–415; ConfigAspects.registerEntityAspects40–114; InventoryUtils.areItemStacksEqual313–343; ItemThaumometer.doScan87–140/onUsingTick158.

## Exact clue contract

- Общая category registration order BASICS→THAUMATURGY→ALCHEMY→ARTIFICE→GOLEMANCY→ELDRITCH и values полного default HashMap каждой категории, как R11. Общий ResearchRegistrationOrder реконструирует source.line corpus без key sorting/presizing/hidden-only insertion.
- Candidate has tags, Hidden или Lost, нет KEY и @KEY. Prerequisites и текущий research difficulty НЕ проверяются; clue не отменяет их при последующей покупке/выдаче/learning.
- Первое совпадение item trigger ИЛИ entity original-name string ИЛИ хотя бы одного положительного фактически награждённого aspect даёт один entry кандидата. Множественные совпавшие triggers не добавляют повторный вес.
- Берётся один world.random.nextInt(pool.size()), только для непустого pool. Добавляется только @KEY. Нет real completion, RP-цены, research warp, material cost или детерминированного dayTime RNG (это другой контракт R11). Duplicate/key/clue gates стоят до RNG.
- Item trigger calls areItemStacksEqual(trigger,scanned,true,true,false): OreDictionary match имеет ранний shortcut, иначе item+metadata, durability/Short.MAX_VALUE wildcard и СТРОГОЕ NBT равенство. Нельзя глобально игнорировать NBT только из-за ignoreDamage=true. Damage в modern component нужен отдельный узкий adapter; поздние native items, ещё отсутствующие в repo, не получают вымышленные bindings.
- В completeScan clues запускаются только после успешного scan award, с aspectsFinal от checkAndSyncAspectKnowledge. Item/entity witness обязателен: original node phenomena clue=null и сами по себе aspect clues не выдают. Обычный thaumometer использует prefix@, поэтому получает original aspect quantities (+2 за первое discovery); prefix# означает отдельный последующий scan consumer и имеет свои +1/no-double-base правила, не путать по misleading variable name scannedByThaumometer.

## Native integration

- ResearchClues.Context — immutable snapshot одного item либо original entity-name string; null для node. ResearchClues.reveal(knowledge,context,actualAwards,IntUnaryOperator) возвращает выбранный key либо null. ScanManager overload/hook до KnowledgeEvents.sync делает root в R13 cap зоне; Существующий old overload сохраняется context=null.
- ItemThaumometer хранит server-observed witness в transient Target и добавляет реальные entity rays; отсутствующий/неподдержанный entity блокирует target вместо получения чужих block rewards. Дубликаты различают original variants (wither skeleton, charged creeper, child/villager, golem material, players).
- EntityAspectBindings сохраняет exact ConfigAspects quantities и original last matching NBT override; player MAN4+три приватных name-seeded draws либо named Easter egg. Внешние/addon scan callbacks, отсутствующие поздние mobs и полный API R15 не считаются готовыми по этим данным.

## Required verification

Pure tests: original candidate order/draw bounds, positive awarded aspects, no parents/difficulty gate, Lost и duplicate clue exclusion, exactly one marker/no payment/warp, zero RNG при отсутствии кандидатов. Native tests: actual thaumometer use20ticks on entity, variants и registry bindings; item damage/components trigger behavior; clue before parents, duplicate no reroll, node no clues, player NBT persist. Поведение клиента/пакетный C2S/полное естественное получение поздних items отдельно pending до фактических запусков.

## Текущие implemented paths и pending bindings

Production classes/resource,8 JUnit methods и два GameTest entry methods написаны; Gradle не запускался. Generator `scripts/porting/gen_entity_aspect_bindings.py --oracle <outside-oracle>` извлекает69 original registration rows, включая byte predicates и dynamic Wisp Type, с normalized source SHA256. Это данные будущих mobs, не свидетельство их наличия/получения в native мире. Native IDs Brainy/Giant/Firebat/Wisp согласованы с агентом mobs; он владеет их gameplay, этот unit только сканированием.

Важный original quirk: JARBRAIN item trigger — itemResource:3, то есть native quick_silver, НЕ zombie_brain. ConfigItems:283 регистрирует nuggetQuicksilver; поэтому именованный quicksilver проходит ранний ore shortcut без NBT сравнения. Для bow/bone/ender_pearl/wool базовые Forge1.7.10 initVanillaEntries и TC4 registers не задают ore shortcut; custom NBT не игнорируется. Forge source reference проверен read-only, сохранён только в ignored .tmp; TC4 oracle остаётся gameplay authority. Modern DAMAGE component нормализуется только для durable-item metadata-правила. Foreign OreDictionary/addon bindings относятся к полному API R15 и пока не заявлены.

Пока не bound поздние item triggers BANNERS (woodenDevice8), ROD_primal_staff (ещё отсутствующий focus primal), OUTERREV (eldritch5/10), PRIMPEARL (eldritchObject3). Ни один отсутствующий item не зарегистрирован лишь ради clue-теста. MIRROR natural ender_pearl/entity Enderman paths работают в implementation; portal/end-portal trigger stacks отдельно pending до подтверждения native source/pick semantics, не подменены придуманным material reward.

Native checks используют настоящий scan receiver для item witness/duplicate/node-null/player NBT и real GameMode.useItem на indexed Enderman с20 реально прошедшими server-world ticks (GameTest вручную вызывает onUseTick для mock-player). Skeleton/Creeper/ZombieVillager проверки используют реальные native instances, split-type/loaded powered-NBT/baby API. Они не являются C2S dispatch или осмотренным клиентом.

Independent read-only review20.10.2026 (`review_reservoir`) не нашёл blockers: сверены quick_silver/nugget shortcut (без ошибочного alias quicksilver_drop), отсутствие research-parent/RP gate, actual awards map, полная EntityUtils geometry, zero-distance overwrite и unsupported-entity occlusion. Это source review; build/runtime остаются pending.

Player witness edge сохранён отдельно: original EntityList.getEntityString(player) возвращал null, хотя generateEntityHash/generateEntityAspects использовали private player_name. EntityAspectBindings.Target теперь содержит nullable clue snapshot; player скан даёт original RP, но не direct/aspect clue. Native check использует actual ServerPlayer descriptor и положительный Flight reward через production handler, проверяя отсутствие @HOVERGIRDLE. Это исправлено до первого snapshot данной группы.

Fabric delegates для обоих R12 checks и R11 включены; compile/GameTest результата ещё нет. Snapshot freeze затрагивает все main/test/gameTest/resources данной группы.

Local prerequisite evidence: JUnit ResearchCluesTest5/5 PASS0.063s и EntityAspectBindingsTest3/3 PASS0.012s, common XML09:31:19UTC. R12 production compiled в том же централизованном прогоне; общий suite619/620 (unrelated Deconstruction failure). Fabric GameTest source/runtime и client для этих final R12 исходников пока не подтверждены; следующая полная проверка идёт через immutable CI checkpoint.
