# Thaumonomicon Cheat Sheet — metadata42, контракт TC4

Источник — оригинальный `common/items/relics/ItemThaumonomicon:80–120`,
`ResearchManager.completeResearch:510–527`, `completeAspect:530–542`,
`Thaumcraft.addWarpToPlayer/addStickyWarpToPlayer`.

Metadata42 имеет тот же заголовок книги, EPIC rarity, stack1, оригинальную
`thaumonomiconcheat` и буквальный tooltip `Cheat Sheet`. Отдельный нативный id —
`thaumcraft:thaumonomicon_cheat`, конструкция `ItemThaumonomicon(Properties, boolean)`;
обычная книга остаётся UNCOMMON. Никакого рецепта для чит-книги нет.

Creative visibility проверяет **текущее** `ThaumcraftSettings.current().cheatSheet()`
в `CreativeTabRegistry.modify` при каждой реальной пересборке вкладки. Setting не
захватывается при bootstrap; пустой стек не передаётся API creative output. У item
нет собственного OP/creative ограничения: право выдать все знания определяется
исходным серверным флагом. Удалённый server snapshot на клиенте уже обслуживает R13.

На серверном use при включённом server.cheatSheet только специальный item проходит
по всем `ResearchRegistrationOrder.enabled()` и завершает каждое ещё неизвестное
исследование без prerequisites, notes, RP costs или обхода siblings. Это и есть
исходный `completeResearch` branch, в том числе для lost/hidden/special. Conditional
research, которого нет в текущей зарегистрированной конфигурации, не выдаётся.
За каждый новый ключ warp>0 выдаётся permanent `warp-warp/2`, sticky `warp/2`,
counter становится total. При Wuss эта выдача и изменение counter пропускаются.
Уже завершённое research не выдаёт повторный warp.

Затем каждый **неоткрытый** аспект получает точно50RP через семантику
`completeAspect`. Уже открытые positive/zero/negative pools не пополняются.
Повторное use не пополняет ресурсы; сама книга не расходуется. Native knowledge
snapshot сохраняет/синхронизирует итог перед открытием обычного research browser.

Если сервер запретил cheat sheet, даже уже существующий специальный item выполняет
обычную ветку книги: один live pass sibling unlocks, обычный browser, без массовой
выдачи RP/research. Клиентский флаг не может дать серверу разрешение на completion.

`ResearchCheatSheet` собирает результат на копии player knowledge и публикует его
одним commit перед packet effects. Пакеты Research/Aspects/Warp объединены существующей
нативной KnowledgeSyncPayload; индивидуальные legacy per-key packets не возвращаются.
Оригинальные количественные значения, Wuss и идемпотентность сохранены. Клиентская
инспекция открытия/цвета/tooltip/порядка сообщений остаётся pending.

`CheatSheetRuntimeChecks` — настоящий connected actor, registered item и native
creative tab rebuild. Проверки: disabled special acts normal; live false→true→false
creative setting; точный enabled corpus без prerequisites; unknown50 и сохранение
known positive/zero/negative; исходные warp split/counter; Wuss; повтор без пополнения;
книга не заменена/не расходована. Конфигурация и creative contents восстанавливаются.

**Implemented:** код и модель активированы в разрешённом source window.
Built/game-tested/seen — pending для этого нового среза.
