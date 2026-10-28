# Снаряжение и безделушки

Thaumcraft спрашивает предметы, которые игрок держит и носит, о нескольких интерфейсах; достаточно реализовать нужный,
ничего регистрировать не надо. Все они — исходные интерфейсы `thaumcraft.api` с типами 1.21.1.

## Безделушки: `IBauble`

Thaumcraft 1.7.10 опирался на мод Baubles; в этом порте те же четыре слота свои: амулет, два кольца и пояс. Игрок
открывает их клавишей безделушек (B по умолчанию) или кнопкой рядом с персонажем в инвентаре, а безделушка,
использованная из руки, встаёт в первый свободный слот своего типа (предмет может оставить правый клик себе, см.
[ниже](#безделушка-которая-оставляет-правый-клик-себе)).

<!-- snippet:amulet -->
```java
public class OrderAmulet extends Item implements IBauble, IVisDiscountGear, IRunicArmor, IWarpingGear {
    public OrderAmulet(Properties properties) { super(properties.stacksTo(1)); }

    @Override public BaubleType getBaubleType(ItemStack stack) { return BaubleType.AMULET; }

    /** Every tick while worn, on both sides; this one acts on the server once every five seconds. */
    @Override public void onWornTick(ItemStack stack, LivingEntity wearer) {
        if (wearer.level().isClientSide || wearer.tickCount % 100 != 0 || !(wearer instanceof Player player)) return;
        ItemStack wand = player.getMainHandItem();
        if (WandHelper.isWand(wand)) WandHelper.addVis(wand, Aspect.ORDER, 1, true); // whole vis
    }

    /** Percentage points; the aspect is null when a tooltip asks for the general discount. */
    @Override public int getVisDiscount(ItemStack stack, Player player, Aspect aspect) { return aspect == Aspect.ORDER ? 10 : 2; }

    @Override public int getRunicCharge(ItemStack stack) { return 2; }
    @Override public int getWarp(ItemStack stack, Player player) { return 1; }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        tooltip.add(Component.translatable("tc.visdiscount").append(": 2%").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("tc.visdiscount").append(" (" + Aspect.ORDER.getName() + "): 10%").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.warping").append(" 1").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
```
<!-- /snippet -->

| Метод | Контракт |
|---|---|
| `getBaubleType(stack)` | `BaubleType.AMULET`, `RING` или `BELT`: слот, в который она встаёт |
| `onWornTick(stack, wearer)` | каждый тик, пока надета, на обеих сторонах; проверяйте `level().isClientSide` прежде чем что-то менять |
| `onEquipped`, `onUnequipped` | когда попадает в слот или покидает его (через меню, правым кликом, shift-кликом) |
| `canEquip`, `canUnequip` | могут отказать; по умолчанию оба `true` |
| `equipsOnUse(stack, player)` | надевает ли предмет правый клик с ним в руке; по умолчанию `true`. Решает только надевание из руки |

Слоты сохраняются вместе с игроком, выпадают при смерти (если не `keepInventory`) и синхронизируются с клиентом.

### Безделушка, которая оставляет правый клик себе

Надевание из руки — удобство самого порта (в TC4 и в моде Baubles было только меню), и оно срабатывает там, где клик
иначе вызвал бы `Item.use`. Предмет, чей правый клик делает что-то своё, — талисман-переключатель или пояс, открывающий
экран, — отвечает на `equipsOnUse(stack, player)` значением `false`, и клик идёт дальше в `Item.use` (так же он идёт, когда
свободного слота нужного типа нет). По умолчанию `true`, поэтому ни одна существующая безделушка не меняется. Метод решает
только надевание из руки: меню, shift-клик в него, `getBaubleType` и `canEquip` работают как раньше, так что игрок всё
равно надевает такой предмет там.

Клиент спрашивает тоже, для того же клика, поэтому решайте по стаку и по состоянию, которое есть и у клиента (например,
`player.isShiftKeyDown()`), и ничего не меняйте. [Мешочек для фокусов](wands-and-foci.md#мешочки-для-фокусов-ifocuspouch)
кликом не надевается никогда, что бы он ни ответил.

<!-- snippet:talisman -->
```java
public class OrderTalisman extends Item implements IBauble {
    public OrderTalisman(Properties properties) { super(properties.stacksTo(1)); }

    @Override public BaubleType getBaubleType(ItemStack stack) { return BaubleType.RING; }

    /**
     * A click puts the talisman on, as it does every bauble, except while sneaking: that click is the talisman's own and
     * reaches {@link #use}. Only the hand equip is decided here; the Baubles menu still puts it on and takes it off. The
     * client asks as well, for the same click, so the answer depends on state both sides have.
     */
    @Override public boolean equipsOnUse(ItemStack stack, Player player) { return !player.isShiftKeyDown(); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean("awake", !tag.getBoolean("awake")));
            player.displayClientMessage(Component.translatable(isAwake(stack) ? "tc4_example.order_talisman.awake" : "tc4_example.order_talisman.asleep"), true);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override public void onWornTick(ItemStack stack, LivingEntity wearer) {
        if (wearer.level().isClientSide || wearer.tickCount % 100 != 0 || !(wearer instanceof Player player) || !isAwake(stack)) return;
        ItemStack wand = player.getMainHandItem();
        if (!WandHelper.isWand(wand)) return;
        // "Is it worn?": copies of the stacks in the accessory slots, in slot order, without the empty slots.
        boolean paired = BaublesApi.getWorn(player).stream().anyMatch(worn -> worn.is(ExampleAddon.ORDER_AMULET));
        WandHelper.addVis(wand, Aspect.AIR, paired ? 2 : 1, true); // whole vis
    }

    private static boolean isAwake(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("awake");
    }

    @Override public boolean isFoil(ItemStack stack) { return isAwake(stack); }
}
```
<!-- /snippet -->

### Что надето: `BaublesApi.getWorn`

`BaublesApi.getWorn(entity)` отвечает на вопрос «надето ли X». Он возвращает неизменяемый список **копий** стаков из слотов
безделушек в порядке слотов (амулет, кольцо, кольцо, пояс) без пустых слотов. Для `null` и для любой сущности без таких
слотов (то есть для всех, кроме игрока) он пуст; клиент знает только слоты своего игрока. Это снимок, поэтому спросите
заново, если всё могло измениться. Чтобы изменить надетый предмет, используйте стак, который получают его собственные
обратные вызовы (`onWornTick`): он живой. Талисман выше читает список, чтобы работать в паре с амулетом.

## Что может нести надетое и удерживаемое

| Интерфейс | Откуда читается | Действие |
|---|---|---|
| `IVisDiscountGear` | четыре слота брони и четыре слота безделушек | `getVisDiscount(stack, player, aspect)` в **процентных пунктах**, суммируется; `aspect` равен null, когда подсказка спрашивает общую скидку |
| `IRunicArmor` | четыре слота брони и четыре слота безделушек | `getRunicCharge(stack)`: очки рунного щита; строку подсказки «Runic shield +N» игра добавляет сама |
| `IWarpingGear` | основная рука, четыре слота брони и четыре слота безделушек | `getWarp(stack, player)`: варп, который действует, пока предмет в руке или надет, в событиях варпа |
| `IGoggles` | слот головы | `showIngamePopups(stack, wearer)`: всплывающие аспекты над контейнерами и узлами |
| `IRevealer` (`thaumcraft.api.nodes`) | слот головы | `showNodes(stack, wearer)`: узлы ауры рисуются полностью |

Свои строки подсказки (скидку, варп) добавляете вы сами, как в 1.7.10. `ItemRunic(charge)` — исходный простой предмет
с постоянным рунным зарядом. Пакет `thaumcraft.api.items` хранит псевдонимы `IGoggles`, `IRevealer` и
`IVisDiscountGear` из 1.12 для аддонов, собранных под Thaumcraft 6; они наследуют интерфейсы TC4.

## Починка, чернила и фокус архитектора

- `IRepairable`: предмет с чарами «Починка», реализующий его, чинится раз в две секунды, оплачивая вис из жезлов в
  инвентаре (исходный `EventHandlerEntity`). `IRepairableExtended.doRepair(stack, player, level)` может починить
  по-своему или отказать.
- `IScribeTools`: предмет, чья прочность — чернила для стола исследований.
- `IArchitect`: фокус, показывающий наложение архитектора (затрагиваемые блоки и выбор оси) через
  `getArchitectBlocks`, `showAxis` и `getMaxAreaSize`.

## Материалы

`ThaumcraftMaterials` хранит исходные уровни инструментов (`TOOLMAT_THAUMIUM`, `TOOLMAT_VOID`, `TOOLMAT_ELEMENTAL`) и
материалы брони, которые в 1.21.1 — объекты реестра: `armorMatThaumium()`, `armorMatThaumiumFortress()`,
`armorMatVoid()`, `armorMatVoidRobe()`.
