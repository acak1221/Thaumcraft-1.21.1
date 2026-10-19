# Чернильница: проверенная семантика износа перед R06

Статус: исходный bytecode Minecraft 1.7.10 и соответствующие методы фактического mapped JAR 1.21.1 просмотрены. Это статическая проверка наследуемого поведения, не запуск старой игры и не проверка реализованного R06. Production-код при этом аудите не менялся; Gradle не запускался.

## Источники и воспроизводимость

- Официальный [manifest Mojang](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json) указывает для 1.7.10 на [version JSON](https://piston-meta.mojang.com/v1/packages/ed5d8789ed29872ea2ef1c348302b0c55e3f3468/1.7.10.json). Ожидаемый и фактический SHA1 JSON совпали: `ed5d8789ed29872ea2ef1c348302b0c55e3f3468`.
- [Официальный client.jar из этого JSON](https://launcher.mojang.com/v1/objects/e80d9b3bf5085002218d4be59e668bac718abbc6/client.jar): 5 256 245 байт, ожидаемый и фактический SHA1 `e80d9b3bf5085002218d4be59e668bac718abbc6`. Server JAR не потребовался.
- [Архив MCP SRG с Maven Forge](https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp/1.7.10/mcp-1.7.10-srg.zip), фактический SHA1 `10b95bb342f55d681025bf2d698c893d9c4b4201`: `joined.srg` связывает obfuscated и SRG имена. Читаемые имена дополнительно сверены с `methods.csv`/`fields.csv` pinned OpenTC4.
- [Официальный патч Forge 1.7.10 для ItemStack](https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.7.10/patches/minecraft/net/minecraft/item/ItemStack.java.patch), строки 72–80, независимо подтверждает строгое сравнение `damage > maxDamage`; перенаправление через item callbacks не меняет этот порог.
- Private evidence вне репозитория: `../work/vanilla-1.7.10-reference/`. В `provenance.json` сохранены URL, хеши и путь целевого JAR; `AUDIT_R06.md` индексирует disassembly. Использован `javap -c -p` Java21 и CFR 0.152 только для нужных классов. Game JAR, извлечённые классы и декомпиляция не входят в этот документ или Git.

## Что действительно делал TC4

Original oracle `ResearchManager.java:199–209` выбирает первый `IScribeTools` в основном inventory, у которого damage меньше maximum, затем вызывает vanilla `damageItem(1, player)`. Он возвращает успех и при полностью предотвращённом износе. В оригинальном `ItemInkwell.java:41–44` maximum равен 100, stack size равен 1; предмет не является бронёй.

| Проверенный метод 1.7.10 | Bytecode evidence | Наблюдаемое поведение |
|---|---|---|
| `ItemStack.damageItem`, `add.a(int,sv)`, SRG `func_77972_a` | offsets 0–20 | Creative player возвращается до damageability, зачарований и RNG: расход 0, случайных вызовов 0. |
| Тот же метод | offsets 21–35 | Недамажимый/`Unbreakable` stack не расходуется. Остальные передают в `attemptDamageItem` именно `EntityLivingBase.getRNG()`. |
| `EntityLivingBase.getRNG`, `sv.aI()`, SRG `func_70681_au` | offsets 0–4 | Возвращается `Entity.rand`, не `World.rand`. |
| `ItemStack.attemptDamageItem`, `add.a(int,Random)`, SRG `func_96631_a` | offsets 9–68 | При Unbreaking level 0 случайных вызовов нет. При level > 0 проверяется каждый исходный damage point; предотвращённые точки вычитаются. |
| `EnchantmentDurability.negateDamage`, `afr.a(ItemStack,int,Random)`, SRG `func_92097_a` | offsets 22–37 для не-броневого пути | На одну точку приходится ровно `nextInt(level+1)`; результат 0 расходует точку, ненулевой предотвращает её. |
| `attemptDamageItem` | offsets 69–95 | Сохранившийся damage добавляется; break возвращается только при строгом превышении maximum. |
| `damageItem` | offsets 38–122 | Stack уменьшается и damage сбрасывается только при таком break. |

Mappings, исключающие догадки об именах: `joined.srg:9202–9203` для двух методов ItemStack, `:9481` для negateDamage, `:19994` для getRNG, `:8615/:8596` для `EntityPlayer.capabilities.isCreativeMode`, `:7757` для `Entity.rand`. CSV-имена: `methods.csv:2149/2695/2819/251`, `fields.csv:350/1075/31`.

Следствие для обычной max100 чернильницы: 99→100 сохраняет предмет и count1. Следующий вызов **TC4 consumeInkFromPlayer** не выбирает её из-за предварительного `damage < max` и ничего не расходует. Creative тоже нуждается в пригодной чернильнице: обход износа находится после этого mod-предиката. `Unbreakable` с пригодным damage аналогично удовлетворяет предикату, но vanilla не меняет damage и не использует RNG.

Табличный путь отличается намеренно: original `ResearchManager.java:211–218` устанавливает damage+1 напрямую. Creative и Unbreaking в нём не участвуют. Нельзя объединять оба пути через один современный damage helper.

## Отличие 1.21.1 и границы следующего изменения

Сравнение выполнено с реальным Fabric mapped `minecraft-merged-1.21.1-loom.mappings.1_21_1.layered+hash.40545-v2.jar`, SHA1 `7622e4ff86b4c5f837d284fb0f06ec56720360c4`, из локального `.gradle21`.

- `ItemStack.hurtAndBreak(int,ServerLevel,ServerPlayer,Consumer)` ломает предмет уже при **>= maximum**. Прямой перенос вызова удалил бы опустевшую на100 чернильницу.
- Native `EnchantmentHelper.processDurabilityChange` → `Enchantment.modifyDurabilityChange` → `modifyItemFilteredCount` использует **ServerLevel.getRandom()**. `RemoveBinomial.process` вызывает **nextFloat** на точку. Стандартная вероятность Unbreaking для не-брони совпадает с прежней, но RNG source и тип draw отличаются; современные data packs также могут менять эффекты.
- Предлагаемый ограниченный R06 helper для расхода одной ink: исходный usable-предикат; затем обход износа для creative и `!isDamageableItem()`; lookup native `UNBREAKING` level; только при level>0 один player RNG `nextInt(level+1)`; при прошедшем износе — `setDamageValue(old+1)`. В этой ветке исходный guard делает разрушение недостижимым. Native `hurtAndBreak`/`processDurabilityChange` для неё не подходят.
- Сохранение player RNG вызова не утверждает идентичную эволюцию RNG всего мира между версиями Minecraft: target сохраняет собственный RandomSource и жизненный цикл сущностей.

Минимальные поведенческие проверки R06: survival 99→100 без уменьшения count; повторный расход при 100 отвергается без RNG/мутации; usable creative принимает с нулевым расходом и без draw, depleted creative отвергается; Unbreakable сохраняется; level 0 не делает draw; контролируемые level>0 результаты 0/ненулевой показывают ровно один player draw и соответствующий расход/отмену; табличный +1 не зависит от creative/Unbreaking. Эти проверки здесь **не запускались** и остаются задачей реализации R06.

## Дополнение R06: бумага и полный creative inventory

Дополнительно прочитан `javap -c -p yx` того же официального client JAR; private evidence сохранено как `InventoryPlayer.javap.txt`. `joined.srg:1815` связывает `yx` с InventoryPlayer, `:21657` — `a(Item)` с `consumeInventoryItem`, `:21660` — `a(ItemStack)` с `addItemStackToInventory`.

- `consumeInventoryItem`, bytecode offsets12–38: уменьшает первый найденный stack на1 и освобождает slot при нуле. Проверки creative нет: бумага расходуется и у creative игрока.
- `addItemStackToInventory`, offsets56–83: если предмет не удалось добавить и игрок creative, очищает переданный stack и возвращает true. TC4 `ResearchManager:120–134` вследствие этого не вызывает drop. Для damaged-item ветки аналогичный исход находится на offsets134–153.
- Поэтому полностью занятый creative inventory с paper stack2 даёт paper1, неизменную пригодную чернильницу, ноль записок в inventory и ноль выпавших записок. Если paper stack1 освобождает slot, note занимает его обычным путём. Это сохранённая особенность оригинала, не новое правило экономии материалов.

Для survival native drop подтверждается по появлению фактической ItemEntity в ServerLevel, а не только по ненулевому return. Отказ/null/непоявившаяся entity не должен списывать staged бумагу/чернила; это документированная защита целостности при современном loader callback. Игровые результаты этих веток записываются в MIGRATION_STATUS отдельно после центрального запуска.
