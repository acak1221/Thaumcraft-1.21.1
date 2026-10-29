# Жезлы, части жезлов и фокусы

## Сначала единицы

Жезл хранит вис в **сантивис** (сотых долях вис, как хранил 1.7.10). Ёмкость стержня, стоимость магического рецепта и
`WandHelper.addVis` — в **целых вис**; стоимость фокуса — в **сантивис**. Перепутав их, легко сделать фокус в сто раз
дешевле или стержень в сто раз меньше.

## Стержни, сердцевины посохов и колпачки

`new WandRod(...)`, `new StaffRod(...)` и `new WandCap(...)` — исходные конструкторы; они регистрируют часть:

<!-- snippet:wand-parts -->
```java
// A wand rod and a wand cap, the 1.7.10 constructors: capacity in whole vis, the craft cost of the part, and an
// entity-style texture with the UV layout of the original ModelWand (a 1.7.10 PNG works unchanged). The arcane
// workbench builds wands, staves and sceptres from the part items once ROD_<tag> and CAP_<tag> are researched.
IWandRodOnUpdate regrowth = (wand, player) -> {
    // Server, every tick, for each wand with this rod in the player's inventory: one Ordo every five seconds
    // up to a tenth of the capacity. getVis/getMaxVis are centivis, addVis takes whole vis.
    if (player.tickCount % 100 == 0 && WandHelper.getVis(wand, Aspect.ORDER) < WandHelper.getMaxVis(wand) / 10) {
        WandHelper.addVis(wand, Aspect.ORDER, 1, true);
    }
};
new WandRod("amethyst", 80, new ItemStack(AMETHYST_ROD), 4, regrowth,
        ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_rod_quartz.png"));
WandCap cap = new WandCap("amethyst", 0.95F, java.util.List.of(Aspect.ORDER, Aspect.ENTROPY), 0.85F, new ItemStack(AMETHYST_CAP), 3);
cap.setTexture(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_cap_thaumium.png"));
new ResearchItem("ROD_amethyst", "EXAMPLE", new AspectList().add(Aspect.CRYSTAL, 3).add(Aspect.TOOL, 2), 0, 2, 1,
        new ItemStack(AMETHYST_ROD)).setParents("EXAMPLE_SHARDS").setSecondary()
        .setPages(new ResearchPage("tc4_example.research_page.ROD_amethyst.1")).registerResearchItem();
new ResearchItem("CAP_amethyst", "EXAMPLE", new AspectList().add(Aspect.CRYSTAL, 2).add(Aspect.ORDER, 2), 2, 2, 1,
        new ItemStack(AMETHYST_CAP)).setParents("EXAMPLE_SHARDS").setSecondary()
        .setPages(new ResearchPage("tc4_example.research_page.CAP_amethyst.1")).registerResearchItem();
```
<!-- /snippet -->

- **Стержень:** тег, ёмкость в целых вис, предмет-часть, его стоимость сборки, необязательный `IWandRodOnUpdate` и
  текстура. Текстура в стиле существ с UV-разметкой исходной `ModelWand`, так что PNG из 1.7.10 подходит без изменений.
- **Сердцевина посоха:** `StaffRod` — стержень, зарегистрированный как `<tag>_staff`; жезл из него — посох (длинная
  модель, +6 урона, без слота на магическом верстаке). `setRunes(true)` добавляет руны первичного посоха и одну
  единицу силы фокуса.
- **Колпачок:** тег, множитель скидки для всех аспектов, особые аспекты и их множитель, предмет, стоимость сборки;
  текстуру задаёт `setTexture`.
- Магический верстак собирает жезлы, посохи и скипетры из предметов-частей, когда исследованы `ROD_<tag>` и
  `CAP_<tag>`; эти записи регистрируете вы сами, как в 1.7.10. Стоимость сборки в вис — стоимость стержня × стоимость
  колпачка.
- Имя жезла складывается из `item.Wand.<tag>.rod` и `item.Wand.<tag>.cap`.
- Часть, зарегистрированная под родным тегом, никогда не заменяет родную часть.

### `IWandRodOnUpdate`

Хук стержня аддона выполняется на **сервере**, раз в тик, для каждого жезла, посоха или скипетра с этим стержнем в
инвентаре игрока, с живым стаком (исходный `ItemWandCasting.onUpdate`). Изменение вис стака там сохраняется и
синхронизируется, как любое изменение инвентаря. Восстановление вис в примере — лямбда в начале фрагмента выше.

## Фасад `WandHelper`

`thaumcraft.api.wands.WandHelper` — исходные методы `ItemWandCasting` для кода, у которого есть только стак:

| Вызов | Единица / смысл |
|---|---|
| `isWand`, `isStaff`, `isSceptre` | что это за стак |
| `getRod`, `getCap`, `setRod`, `setCap`, `hasRunes` | части, как их виды API (родные части тоже) |
| `getFocus`, `getFocusItem`, `setFocus` | установленный фокус |
| `getVis(stack, aspect)`, `getAllVis`, `getMaxVis`, `getAspectsWithRoom` | сантивис |
| `addRealVis(stack, aspect, amount, doit)` | сантивис; возвращает то, что не поместилось |
| `addVis(stack, aspect, amount, doit)` | целые вис; возвращает то, что не поместилось |
| `consumeVis(stack, player, aspect, amount, crafting)` | сантивис, один аспект, со скидкой |
| `consumeAllVis(stack, player, aspects, doit, crafting)` | сантивис, всё или ничего, со скидкой |
| `consumeAllVisCrafting(stack, player, aspects, doit)` | целые вис, всё или ничего (магический крафт) |
| `getConsumptionModifier(stack, player, aspect, crafting)` | множитель, с которым идёт оплата |
| `getFocusPotency`, `getFocusTreasure`, `getFocusFrugal`, `getFocusEnlarge`, `getFocusExtend` | уровни улучшений установленного фокуса |

В жезле хранятся только шесть первичных аспектов. Оплата идёт по родным правилам скидки: колпачок, надетое
`IVisDiscountGear`, улучшение «экономия» (только при касте) и истощение вис.

## Фокусы

Любой наследник `ItemFocusBasic` — фокус: он ставится на жезл (его показывают клавиша смены фокуса, F по умолчанию, и
мешочек для фокусов), рисуется на жезле своим цветом и орнаментом, а жезл вызывает его хуки.

<!-- snippet:focus -->
```java
public class SparkFocus extends ItemFocusBasic {
    /** Focus costs are centivis: half a vis of Ignis and a quarter of Aer per cast. */
    private static final AspectList COST = new AspectList().add(Aspect.FIRE, 50).add(Aspect.AIR, 25);

    public SparkFocus(Properties properties) { super(properties); } // stack size one and the rare rarity are set here

    @Override public AspectList getVisCost(ItemStack focus) { return COST.copy(); }
    @Override public int getFocusColor(ItemStack focus) { return 0xFF9A2E; }
    @Override public int getActivationCooldown(ItemStack focus) { return 500; } // milliseconds, as in 1.7.10

    /** The focal manipulator offers these at every rank; frugal is applied by the wand itself. */
    @Override public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.potency};
    }

    @Override public ItemStack onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
        double range = 16 + 4 * WandHelper.getFocusPotency(wand);
        HitResult hit = player.pick(range, 1.0F, false);
        if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) return null; // not handled
        if (level.isClientSide) return wand; // both sides run this; the server pays and acts
        // All or nothing; the cost is discounted by the wand's cap, worn gear and the frugal upgrade.
        if (!WandHelper.consumeAllVis(wand, player, getVisCost(WandHelper.getFocusItem(wand)), true, false)) return null;
        ThaumcraftFX.zap((ServerLevel) level, player.getEyePosition().add(0, -0.2, 0), hit.getLocation());
        BlockPos fire = block.getBlockPos().relative(block.getDirection());
        if (level.isEmptyBlock(fire) && BaseFireBlock.canBePlacedAt(level, fire, block.getDirection())) {
            level.setBlockAndUpdate(fire, BaseFireBlock.getState(level, fire));
        }
        return wand;
    }
}
```
<!-- /snippet -->

| Метод | Контракт |
|---|---|
| `getVisCost(focus)` | сантивис за каст (или за тик, если `isVisCostPerTick` — true); показывается в подсказке |
| `onFocusRightClick(wand, level, player, hand)` | каст, на **обеих** сторонах. Верните стак, который останется в руке (обычно `wand`), если фокус сработал, или `null`, чтобы клик прошёл дальше. Фокус платит сам |
| `onUsingFocusTick(wand, player, count)` | каждый тик, пока жезл удерживается; чтобы туда попасть, вызовите `player.startUsingItem(hand)` в `onFocusRightClick` |
| `onPlayerStoppedUsingFocus(wand, level, player, count)` | когда игрок отпускает клавишу использования |
| `getActivationCooldown(focus)` | миллисекунды между кастами |
| `getFocusColor(focus)` | оттенок фокуса на жезле |
| `getOrnament(focus)`, `getFocusDepthLayerIcon(focus)` | полные пути текстур орнамента и глубинного слоя или `null` |
| `getPossibleUpgradesByRank(focus, rank)` | что предлагает фокусирующий манипулятор на ранге 1–5 |
| `getUpgradeLevel(focus, type)`, `isUpgradedWith(focus, type)` | применённые улучшения |

`FocusUpgradeType` хранит исходные улучшения (`potency`, `frugal`, `treasure`, `enlarge`, `extend`...) и их id.

## Мешочки для фокусов: `IFocusPouch`

Выбор фокуса жезла (клавиша смены фокуса, по умолчанию F) перечисляет фокусы, которые несёт игрок, и фокусы в каждом
найденном мешочке для фокусов. Мешочек — это любой предмет, реализующий `IFocusPouch`: собственный мешочек Thaumcraft
реализует его, и ваш тоже может. Интерфейс — это пара методов 1.7.10 `getInventory` и `setInventory`, которые были у
Bottomless Pouch из Thaumic Tinkerer через наследование от внутреннего `ItemFocusPouch`; теперь внутренний класс не нужен.

<!-- snippet:focus-pouch -->
```java
public class FocusBelt extends Item implements IBauble, IFocusPouch {
    public static final int CELLS = 6;

    public FocusBelt(Properties properties) { super(properties.stacksTo(1)); }

    @Override public BaubleType getBaubleType(ItemStack stack) { return BaubleType.BELT; }

    /** Every cell, in order, with ItemStack.EMPTY where there is nothing; independent copies the selector may change. */
    @Override public ItemStack[] getInventory(ItemStack pouch) {
        List<ItemStack> stored = pouch.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).stream().toList();
        // A payload this belt cannot read: the selector then ignores the belt and does not touch it.
        if (stored.size() > CELLS) throw new IllegalArgumentException("a belt has " + CELLS + " cells");
        ItemStack[] cells = new ItemStack[CELLS];
        Arrays.fill(cells, ItemStack.EMPTY);
        for (int cell = 0; cell < stored.size(); cell++) cells[cell] = stored.get(cell); // stream() hands out copies
        return cells;
    }

    /** Stores all cells into the stack it is given and nothing else; the selector also calls it on a scratch copy. */
    @Override public void setInventory(ItemStack pouch, ItemStack[] contents) {
        if (contents.length != CELLS) throw new IllegalArgumentException("a belt has " + CELLS + " cells");
        pouch.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(Arrays.asList(contents))); // copies the stacks
    }

    /**
     * The belt's own use. A pouch is never put on by a click, so unlike the talisman it needs no equipsOnUse: the Baubles
     * menu puts it on, and the selector lists its foci either way. A belt that holds data it cannot read is left alone, as the
     * selector leaves it alone.
     */
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack belt = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (level.isClientSide) return InteractionResultHolder.success(belt);
        ItemStack[] cells;
        try { cells = getInventory(belt); }
        catch (IllegalArgumentException unreadable) { return InteractionResultHolder.fail(belt); } // foreign data: change nothing
        if (player.isShiftKeyDown()) {
            for (int cell = CELLS - 1; cell >= 0; cell--) {
                if (cells[cell].isEmpty()) continue;
                player.getInventory().placeItemBackInInventory(cells[cell]);
                cells[cell] = ItemStack.EMPTY;
                break;
            }
        } else if (other.getItem() instanceof ItemFocusBasic) {
            for (int cell = 0; cell < CELLS; cell++) {
                if (!cells[cell].isEmpty()) continue;
                cells[cell] = other.split(1);
                break;
            }
        }
        setInventory(belt, cells);
        return InteractionResultHolder.success(belt);
    }
}
```
<!-- /snippet -->

| Метод | Контракт |
|---|---|
| `getInventory(pouch)` | все ячейки по порядку, `ItemStack.EMPTY` для пустой, никогда не `null`; длина — число ячеек и для одного стака не меняется; независимые копии; вызывается на обеих сторонах. Для содержимого, которое вы не можете прочитать, бросьте `IllegalArgumentException`: выбор тогда игнорирует этот мешочек и не трогает его (любое другое `RuntimeException` обрабатывается так же и пишется в журнал один раз, поэтому ваша ошибка не уронит клиент, но это не способ сказать «не читается») |
| `setInventory(pouch, contents)` | заменяет все ячейки переданного стака (длина та же, что у `getInventory`); копируйте то, что оставляете, и больше ничего не меняйте. Поток сервера. Выбор вызывает его на живом стаке, который остаётся в своём слоте, и на черновых копиях, пока строит план. Если он бросает исключение, ничего не теряется: пока выбор строит план, мешочек не отдаёт и не принимает фокус, а исключение на живом стаке отменяет выбор и возвращает все уже записанные стаки на место |

Что выбор делает с мешочком:

- Он смотрит 36 основных слотов инвентаря и четыре слота безделушек, так что мешочек, который ещё и поясная `IBauble`,
  учитывается, пока надет; вторая рука, броня и другие контейнеры не смотрятся. Считается только стак ровно из одного
  мешочка.
- Он предлагает каждую ячейку, в которой лежит фокус (`ItemFocusBasic`). Остальные предметы в ячейках игнорируются и не
  трогаются.
- **Размер.** Выбор — кольцо с одной кнопкой на фокус, а не сетка, поэтому мешочек любого размера перечисляется. Первые
  `IFocusPouch.MAX_SELECTABLE_CELLS` (128) ячеек могут предлагаться и могут принять фокус обратно; у исходного мешочка 18,
  у Bottomless Pouch 117. Ячейки с номера 128 не предлагаются и фокус в них не возвращается, поэтому важные ячейки держите
  в начале.
- Выбор фокуса вынимает его из ячейки (она становится `ItemStack.EMPTY`) и ставит на жезл. Фокус, который был на жезле,
  уходит в первую пустую ячейку первого мешочка, где она есть (сначала надетые, каждый с первой ячейки), затем в свободный
  слот инвентаря. Выбор проходит целиком или не проходит вовсе.
- Правый клик никогда не надевает мешочек, что бы ни отвечал `IBauble.equipsOnUse`: клик идёт в собственный `use`
  мешочка. Поясной мешочек надевается через меню безделушек.

Экран мешочка, правило о том, что в него можно класть, и место хранения ячеек — дело самого аддона.

## Блоки, отвечающие жезлу: `IWandable`

Блок или его блок-сущность с `IWandable` отвечает на клик жезлом раньше собственного поведения жезла, как в 1.7.10:
сначала спрашивается блок, затем блок-сущность.

- `onWandRightClick(level, wand, player, x, y, z, side, event)`: клик по блоку, на обеих сторонах. Отрицательный ответ
  значит «не обработано», и жезл продолжает; иначе клик обработан и больше ничего не происходит (1 — взмах руки, любое
  другое значение — тихий отказ). `event` был метаданными блока и всегда 0.
- `onWandRightClick(level, wand, player)`: клик без цели, задаётся блоку, на который смотрит игрок. Верните стак,
  который останется в руке, или `null`.
- `onUsingWandTick` и `onWandStoppedUsing` этот порт не вызывает: в 1.7.10 они работали только для объекта, который
  аддон отметил внутренним `ItemWandCasting.setObjectInUse`.

Чаша эссенции из примера ([эссенция и вис](essentia-and-vis.md#блок-отвечающий-жезлу)) опустошается кликом жезла и
сгущает Ordo кликом с приседанием.

## Триггеры жезла

`WandTriggerRegistry.registerWandBlockTrigger(manager, event, block, modid)` (или с одним точным `BlockState`)
сохраняет исходный хук для мультиблоков: клик жезлом по зарегистрированному блоку вызывает
`IWandTriggerManager.performTrigger(level, wand, player, x, y, z, side, event)` сразу после `IWandable` и до
собственных структур Thaumcraft. Аргумент метаданных старых форм игнорируется: в 1.21.1 метаданных нет.
