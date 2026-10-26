# Снаряжение и безделушки

Thaumcraft спрашивает предметы, которые игрок держит и носит, о нескольких интерфейсах; достаточно реализовать нужный,
ничего регистрировать не надо. Все они — исходные интерфейсы `thaumcraft.api` с типами 1.21.1.

## Безделушки: `IBauble`

Thaumcraft 1.7.10 опирался на мод Baubles; в этом порте те же четыре слота свои: амулет, два кольца и пояс. Игрок
открывает их клавишей безделушек (B по умолчанию) или кнопкой рядом с персонажем в инвентаре, а безделушка,
использованная из руки, встаёт в первый свободный слот своего типа.

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

Слоты сохраняются вместе с игроком, выпадают при смерти (если не `keepInventory`) и синхронизируются с клиентом.

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
