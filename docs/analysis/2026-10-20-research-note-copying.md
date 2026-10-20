# R10 — копирование открытий и whole-stack lifecycle, 20.10.2026

## Контракт оригинала

Источники: TC4 4.2.3.5, приватный CFR вне репозитория: `TileResearchTable.duplicate` (398–423), `ContainerResearchTable` (button 5), `GuiResearchTable` (completed `RESEARCHDUPE` + solved note), `ResearchManager.updateData` (`copies`), `ItemResearchNotes` (max stack 1). Подробный ранний аудит: [research-notes-spec](2026-10-15-research-notes-spec.md), раздел 6; [research-table-spec](2026-10-15-research-table-spec.md).

Операция копирования требует solved discovery в слоте записки, одну бумагу и один `dye:0` (современный `minecraft:ink_sac`) в основном инвентаре игрока. Цена каждого аспекта — `research.aspect_cost[tag] + copies`, где `copies` берётся **до** операции. Сначала проверяются все цены и ресурсы. После оплаты один общий note component получает `copies+1`, а тот же физический stack — `count+1`. Отдельная новая записка не создаётся. Перо и чернила инструмента стола не участвуют, creative не отменяет явные исходные расходы.

`InventoryUtils.isPlayerCarrying` сравнивает item+damage (`isItemEqual`), а `consumeInventoryItem` — item+metadata0. Поэтому имя/NBT бумаги и чернильного мешка не препятствуют оплате; современная проверка не должна требовать пустые components. Рассматриваются только основные 36 слотов, без брони и offhand. Столовые bonus pools не заменяют личные очки аспектов.

Оригинальный сервер не повторяет клиентский gate `RESEARCHDUPE/complete`; порт обязан проверять его на сервере вместе с реальным открытым menu, session, revision и неизменной физической плиткой. Пакет не передаёт цену, игрока, позицию, счётчик копий или признак solved.

Оригинальное копирование не ограничивает количество в max-stack-one предмете. Придуманный cap 64/99/127 или автоматическое разбиение на отдельные notes меняет его поведение. Нативный положительный int-count сохраняется целиком; прибавление к `Integer.MAX_VALUE` должно быть отказом до расходов, а не integer wrap. Современный persistent codec устраняет потерю count за старой byte-границей, не вводя искусственный лимит 127. Цены считаются в long до проверки — это закрывает original overflow при крайне большом `copies`.

## Обязательная предпосылка до включения natural copy

Vanilla 1.21.1 `ItemStack.CODEC` допускает count лишь до 99. Ранее таблица и долговечная очередь дропа уже хранили count отдельно, но `Inventory.save` игрока всё ещё не мог сохранить discovery100/127 после исходного whole-stack quick move. Это реальный блокер активации копирования, а не повод разрешить его с молчаливой потерей.

Новые адаптеры ограничены классом `ItemResearchNotes`:

- `ResearchNoteStackCodecMixin` оборачивает первый `Codec.lazyInitialized` в `ItemStack.<clinit>` **до** захвата `CODEC` полями `OPTIONAL_CODEC` и их владельцами. Для note count>99 исходный codec сериализует item/components как count1; поверх него записывается полный положительный int-count. При чтении native id/components разбираются до проверки класса предмета, затем восстанавливается исходный count. Другие предметы сохраняют native count validation; itemmax записки остаётся 1. `STRICT_CODEC` продолжает свою дополнительную max-stack проверку.
- `ResearchNoteMenuCountMixin` возвращает исходный whole note remainder в пустую ячейку `AbstractContainerMenu.moveItemStackTo`, как старый `Container.mergeItemStack`, вместо современного split до itemmax1. Путь объединения занятых stack и правила приёма слота не меняются.
- `ResearchNoteContainerCountMixin` предотвращает truncation note в `SimpleContainer`/`BaseContainerBlockEntity` setter после whole-stack transfer (обычный сундук и ender chest). Другие предметы сохраняют native пределы.
- Setter исследовательского стола сохраняет count note выше64 при возврате. Это намеренная адаптация сохранности ресурсов: старый setter clamp64 нельзя применять к уже существующему платному stack100/127. Ограничение64 обычного инструмента остаётся. Один старый тест ожидания note70→64 заменён проверкой сохранности70; он больше не утверждает loss-quirk как требуемое поведение.
- На закрытии исследовательского меню цельный overstack курсора передаётся существующей долговечной очереди дропа вместо modern inventory-placeback, который разбивал бы max-stack-one note. Очередь уже сохраняет все components/count и повторяет отменённый spawn без повторной выдачи.

## Подготовленная проверка предпосылки

`ResearchNoteOverstackRuntimeChecks` использует явные solved-discovery fixtures100/127/**128** с именем и дополнительным component, реальные таблицу/сундук/ender chest и настоящий server-player inventory. Natural copying этим сценарием не вызывается.

Проверяются: `ItemStack.CODEC` и слотный stream codec; table→player whole quick move; `Inventory.save/load`; полный `ServerPlayer.save/load`; оба направления vanilla chest quick move и NBT сундука; оба направления ender chest и его native tag; player→table возврат; обычный pickup на курсор; menu close→durable drop; native player throw; native death-inventory `Inventory.dropAll`; сохранение/загрузка item entity через его настоящий зарегистрированный тип. Каждая передача сохраняет ровно один физический stack, itemmax1 и все components.

Отрицательные проверки сохраняют native отказ disk-count100 для обычного камня и setter-limit64 камня в сундуке. Загрузка обычного `minecraft:stone count127` не должна пользоваться исключением записок.

**Prerequisite built / game-tested:** централизованный `full-port-r10-prereq3` скомпилировал production и исполнил `researchNoteWholeStacksSurviveNativeOwners` (XML: 0,129 с, PASS) для100/127/128. Весь тот прогон **177/178**, единственный провал — прежний `focusUpgradeDataAndCasting` с устаревшим предположением «silk8 не существует» после другой текущей группы. Это не общий зелёный checkpoint; собственная lifecycle-предпосылка R10 выполнена до подключения natural copy.

## Natural-copy реализация после подтверждённой предпосылки

- `ResearchNoteCopying` — pure admission/plan: `RESEARCHDUPE`, solved и известный enabled research key (`isEnabled`, включая легитимные original conditional definitions); границы положительного count/copies; исходные aspect prices плюс старый counter, signed-short query личных pools; staged knowledge и immutable next data. Никакого RNG, shared bonus или scribing tool.
- `ResearchTableCopyPayload` — ограниченный frame с `containerId`, `sessionToken`, `expectedTableRevision`. `ResearchTableCopyNetworking` проверяет actual sender/menu, range/physical identity, session/revision, presentation `DISCOVERY` и solved data, оба основных inventory supplies; только после полного plan платит всё. Поддельные session, stale replay, offhand supplies и отсутствие одного аспекта ничего не меняют.
- `TileResearchTable.commitCopy` обновляет **тот же объект** стека и общий counter/count; копирование не заменяет tool-slot reference и не изменяет bonus pools. Подготовленные owner writes не вызывают inventory setter, dirty updates, сеть или звуки между расходами/выходом. Уведомления и shared-viewer sync идут после commit. Crash-atomic сохранение нескольких файлов не заявляется.
- `ContainerResearchTable.canCopy/requestCopy` — клиентская доступность и запрос с текущими подтверждёнными session/revision; прежний shared `editPending` очищается авторитетным state acknowledgement. GUI вызывает этот метод вместо обычного button5-пакета, который не содержит revision.

Новые meaningful JUnit: две последовательные цены/общий counter, атомарный отказ при нехватке одного аспекта, gates/неизвестный ключ, overflow/short-pool, count99→100 и127→128, возможность копировать уже изученное для другого читателя; payload boundaries/round-trip и malformed framing (9 новых тестов всего).

`ResearchNoteCopyRuntimeChecks` вызывает actual production receiver через настоящий menu/table. Проверяет две реальные оплаты `lux3/ignis1`, затем `lux4/ignis2`; именованные paper/ink-sac; creative расходы; exact same stack object; неизменные exhausted inkwell, feather9, shared bonus99, warp и scans; stale replay и session/range/RESEARCHDUPE/solved/presentation/pool/offhand отказы; natural count99→100 и127→128 с последующим native save/load; отказ int-overflow до оплаты и копирование при полном player inventory. Это прямой вызов server receiver, **не** доказательство C2S transport или осмотренного GUI.

**Implemented:** адаптеры, копирование, payload/menu, GUI original icon24×24 в local37,5, tooltip реальных ресурсов/цен и custom request подключены; bootstrap/Fabric delegate интегрировал ведущий.

**Natural-copy built / game-tested:** централизованный `full-port-integrated2` скомпилировал production и новые test sources; JUnit `ResearchNoteCopyingTest`6/6 и `ResearchTableCopyPayloadTest`3/3 PASS (XML timestamp08:42:35UTC), Fabric `researchTableCopiesDiscoveriesWithExactCosts` PASS0,056с и повторный `researchNoteWholeStacksSurviveNativeOwners` PASS0,072с. Весь integrated2 прогон завершился с5 required GameTest failures в других текущих группах; общий checkpoint пока не принят. Эти точные R10 результаты не являются заявлением о зелёной полной сборке.

**Seen / multiplayer network / actual world restart:** не проверены. NBT-круг реального класса в одном сервере не является перезапуском мира или C2S dispatch.
