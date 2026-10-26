# Блоки, эссенция и вис

На этой странице собирается чаша эссенции из примера: блок-сущность, которая хранит эссенцию, показывает её очкам,
отдаёт трубе и отвечает жезлу.

## Блок-сущность на `TileThaumcraft`

`thaumcraft.api.TileThaumcraft` — исходная базовая плитка: `readCustomNBT`/`writeCustomNBT` хранят данные, которые и
сохраняются, и отправляются клиентам.

<!-- snippet:bowl-entity -->
```java
public class EssentiaBowlBlockEntity extends TileThaumcraft implements IAspectContainer, IEssentiaTransport, IWandable {
    public static final int CAPACITY = 32;
    private Aspect aspect;
    private int amount;

    public EssentiaBowlBlockEntity(BlockPos pos, BlockState state) { super(ExampleAddon.ESSENTIA_BOWL_TYPE, pos, state); }

    // TileThaumcraft saves this custom data, sends it with the chunk and in the update packet.
    @Override public void readCustomNBT(CompoundTag tag) {
        aspect = Aspect.getAspect(tag.getString("Aspect"));
        amount = aspect == null ? 0 : tag.getShort("Amount");
    }

    @Override public void writeCustomNBT(CompoundTag tag) {
        if (aspect == null) return;
        tag.putString("Aspect", aspect.getTag());
        tag.putShort("Amount", (short) amount);
    }

    /** 1.7.10 markDirty + markBlockForUpdate: save the chunk and resend the custom data to the clients. */
    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }
```
<!-- /snippet -->

- Собственные данные сохраняются вместе с блок-сущностью, отправляются с чанком, когда клиент впервые видит блок, и
  отправляются снова каждый раз, когда сервер вызывает `level.sendBlockUpdated(pos, state, state, 3)`
  (`markBlockForUpdate` из 1.7.10). Вызывайте и `setChanged()` (`markDirty` из 1.7.10), чтобы чанк сохранился.
- Обновление всегда несёт ключ `TileThaumcraft.UPDATE_KEY` помимо ваших данных: клиент 1.21.1 пропускает обновление с
  пустым тегом, и без ключа опустевший контейнер оставался бы полным на экране.
- Блок-сущности в 1.21.1 нужен зарегистрированный `BlockEntityType`. На Fabric:

<!-- snippet:block-entity-registration -->
```java
Registry.register(BuiltInRegistries.BLOCK, id("essentia_bowl"), ESSENTIA_BOWL);
Registry.register(BuiltInRegistries.ITEM, id("essentia_bowl"), new BlockItem(ESSENTIA_BOWL, new Item.Properties()));
ESSENTIA_BOWL_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("essentia_bowl"),
        BlockEntityType.Builder.of(EssentiaBowlBlockEntity::new, ESSENTIA_BOWL).build(null));
```
<!-- /snippet -->

  На NeoForge тот же `BlockEntityType` регистрируется через `DeferredRegister<BlockEntityType<?>>`.

Сам блок — обычный `BaseEntityBlock` 1.21.1 (см. `EssentiaBowlBlock.java`); блок-сущность, которой не нужны хуки
собственных данных, может наследовать `BlockEntity` напрямую и всё равно реализовывать интерфейсы ниже.

## Хранение эссенции: `IAspectContainer`

<!-- snippet:bowl-container -->
```java
@Override public AspectList getAspects() { return aspect == null ? new AspectList() : new AspectList().add(aspect, amount); }

@Override public void setAspects(AspectList aspects) {
    Aspect[] list = aspects == null ? new Aspect[0] : aspects.getAspects();
    aspect = list.length == 0 ? null : list[0];
    amount = aspect == null ? 0 : Math.min(CAPACITY, aspects.getAmount(aspect));
    changed();
}

@Override public boolean doesContainerAccept(Aspect tag) { return tag != null && (aspect == null || aspect == tag); }

/** Returns what did not fit. */
@Override public int addToContainer(Aspect tag, int add) {
    if (!doesContainerAccept(tag) || add <= 0) return add;
    int taken = Math.min(add, CAPACITY - amount);
    if (taken > 0) {
        aspect = tag;
        amount += taken;
        changed();
    }
    return add - taken;
}

@Override public boolean takeFromContainer(Aspect tag, int take) {
    if (tag == null || tag != aspect || take <= 0 || amount < take) return false;
    amount -= take;
    if (amount == 0) aspect = null;
    changed();
    return true;
}

@Override @Deprecated public boolean takeFromContainer(AspectList aspects) { return false; }
@Override public boolean doesContainerContainAmount(Aspect tag, int need) { return tag != null && tag == aspect && amount >= need; }
@Override @Deprecated public boolean doesContainerContain(AspectList aspects) { return false; }
@Override public int containerContains(Aspect tag) { return tag != null && tag == aspect ? amount : 0; }
```
<!-- /snippet -->

`addToContainer` возвращает то, что не поместилось; `takeFromContainer(aspect, amount)` — всё или ничего. Очки
откровения (любой шлем `IGoggles`) показывают содержимое блок-сущности `IAspectContainer`, прочитанное на клиенте,
поэтому данные должны синхронизироваться. Реализуйте `IAspectSource` (маркер-наследник `IAspectContainer`), если
алтарь наполнения и зеркало эссенции могут забирать из контейнера: они берут по одной единице через
`takeFromContainer`.

## Перенос эссенции: `IEssentiaTransport`

Трубы, банки, буферы, центрифуги, кристаллизаторы и другие машины эссенции тянут эссенцию из соседей, у которых
всасывание слабее. Сосед, реализующий `IEssentiaTransport`, участвует в этом:

<!-- snippet:bowl-transport -->
```java
// A source like the alembic: it connects downwards only, never pulls, and gives its essentia to a tube that sucks.
@Override public boolean isConnectable(Direction face) { return face == Direction.DOWN; }
@Override public boolean canInputFrom(Direction face) { return false; }
@Override public boolean canOutputTo(Direction face) { return face == Direction.DOWN; }
@Override public void setSuction(Aspect tag, int suction) {}
@Override public Aspect getSuctionType(Direction face) { return null; }
@Override public int getSuctionAmount(Direction face) { return 0; }
@Override public Aspect getEssentiaType(Direction face) { return aspect; }
@Override public int getEssentiaAmount(Direction face) { return amount; }

@Override public int takeEssentia(Aspect tag, int take, Direction face) {
    return canOutputTo(face) && takeFromContainer(tag, take) ? take : 0;
}

@Override public int addEssentia(Aspect tag, int add, Direction face) { return 0; }
@Override public int getMinimumSuction() { return 0; }
@Override public boolean renderExtendedTube() { return true; }
```
<!-- /snippet -->

- `isConnectable(face)`: труба подключается (и рисует подключение) к этой стороне. `canInputFrom`/`canOutputTo`
  говорят, куда эссенция может идти через неё.
- Всасывание: приёмник тянет только из соседа, у которого `getSuctionAmount` меньше его собственного, и только если
  его всасывание не меньше `getMinimumSuction` соседа. Пустая банка всасывает 32, труба — на единицу меньше самого
  сильного соседа. Чистый источник отвечает 0 и никогда не тянет; потребитель задаёт своё всасывание через
  `setSuction` и тянет в своём тике.
- Приёмник просит у источника одну единицу через `takeEssentia(aspect, 1, face)` и получает то, что тот вернул. Как в
  оригинале, решает источник; излишек сверх запрошенной единицы возвращается ему через `addEssentia`.
- `getEssentiaType`/`getEssentiaAmount` описывают, что предлагает сторона; `renderExtendedTube` удлиняет модель
  подключения трубы, чтобы та дотянулась до меньшего блока.

Блок аддона, который принимает эссенцию, тянет из трубы или банки так же: в серверном тике вызовите
`takeEssentia(aspect, 1, face)` у соседа (через `ThaumcraftApiHelper.getConnectableTile`) и добавьте то, что он вернул.

## Эссенция в предметах: `IEssentiaContainerItem`

Банки, кристаллизованная эссенция, эссенция блуждающего огонька и бобы маны сообщают содержимое через
`IEssentiaContainerItem.getAspects(stack)`. Чаша берёт кристаллизованную эссенцию из руки игрока:

<!-- snippet:essentia-item -->
```java
/** Crystallized essentia is an IEssentiaContainerItem: its aspect is read through the API, then one crystal is used up. */
@Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                                    InteractionHand hand, BlockHitResult hit) {
    if (!stack.is(ItemsTC.require("crystal_essence")) || !(stack.getItem() instanceof IEssentiaContainerItem container)
            || !(level.getBlockEntity(pos) instanceof EssentiaBowlBlockEntity bowl)) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION; // a wand goes on to IWandable
    }
    AspectList contents = container.getAspects(stack);
    if (contents == null || contents.size() != 1) return ItemInteractionResult.FAIL;
    Aspect aspect = contents.getAspects()[0];
    if (!bowl.doesContainerAccept(aspect) || bowl.containerContains(aspect) + contents.getAmount(aspect) > EssentiaBowlBlockEntity.CAPACITY) {
        return ItemInteractionResult.FAIL;
    }
    if (!level.isClientSide) {
        bowl.addToContainer(aspect, contents.getAmount(aspect));
        stack.consume(1, player);
    }
    return ItemInteractionResult.sidedSuccess(level.isClientSide);
}
```
<!-- /snippet -->

Фиалы в этом порте не реализуют `IEssentiaContainerItem`: пустой и полный фиал — два разных предмета, и полный фиал
хранит восемь единиц одного аспекта в своём компоненте.

## Блок, отвечающий жезлу

<!-- snippet:bowl-wandable -->
```java
/** A negative answer lets the wand go on; 1 handles the click and swings the arm. */
@Override public int onWandRightClick(Level world, ItemStack wand, Player player, int x, int y, int z, int side, int event) {
    if (player.isShiftKeyDown()) {
        if (world.isClientSide) return 1;
        // One whole vis of Ordo from the vis network (the network counts in centivis) makes one Ordo essentia.
        if (doesContainerAccept(Aspect.ORDER) && amount < CAPACITY && VisNetHandler.drainVis(world, worldPosition, Aspect.ORDER, 100) == 100) {
            addToContainer(Aspect.ORDER, 1);
            ThaumcraftFX.blockSparkle((ServerLevel) world, worldPosition, Aspect.ORDER.getColor());
        }
        return 1;
    }
    if (aspect == null) return -1;
    if (!world.isClientSide) {
        player.displayClientMessage(Component.translatable("tc4_example.essentia_bowl.emptied", amount, aspect.getName()), true);
        ThaumcraftFX.bubbles((ServerLevel) world, worldPosition, aspect.getColor());
        setAspects(null);
    }
    return 1;
}

/** The click without a target: not used by the bowl. */
@Override public ItemStack onWandRightClick(Level world, ItemStack wand, Player player) { return null; }
@Override public void onUsingWandTick(ItemStack wand, Player player, int ticks) {}
@Override public void onWandStoppedUsing(ItemStack wand, Level world, Player player, int ticks) {}
```
<!-- /snippet -->

Весь контракт — в [жезлах и фокусах](wands-and-foci.md#блоки-отвечающие-жезлу-iwandable).

## Вис из сети и из ауры

- `VisNetHandler.drainVis(level, pos, aspect, amount)`: забирает до `amount` **сантивис** первичного аспекта из сети
  ретрансляторов вис в радиусе `pos` и возвращает, сколько взял (чаша выше просит 100, один целый вис). Только сервер;
  на клиенте 0.
- `VisNetHandler.drainVis(level, pos, amount, simulate)`: форма без аспекта из API 1.12, по шести первичным.
- `AuraHelper` сохраняет поверхность API 1.12 над аурой TC4 — эссенцией узлов ауры: `getVis` и `addVis` читают и
  наполняют узел в позиции, `drainVis` забирает из сети вис. В TC4 нет ауры по чанкам и нет флюкса, поэтому методов
  флюкса из той поверхности нет.

## Эффекты

`ThaumcraftFX` отправляет с сервера пакеты эффектов, которые аддоны 1.7.10 собирали сами:

| Вызов | Исходный пакет |
|---|---|
| `blockSparkle(level, pos, rgb)` (`ThaumcraftFX.RANDOM_GREY` — серые искры) | `PacketFXBlockSparkle` |
| `blockArc(level, source, target)` | `PacketFXBlockArc` |
| `zap(level, from, to)` | `PacketFXBlockZap` |
| `bubbles(level, pos, rgb)` | `PacketFXBlockBubble` |
| `essentiaTrail(level, consumer, source, aspect)` | `PacketFXEssentiaSource` |
| `visDrain(level, consumer, node, aspect)` | `PacketFXVisDrain` |

Клиентские частицы `Thaumcraft.proxy` (`sparkle`, `beam`, `wispFX`...) — внутренности `thaumcraft.client.fx` без
стабильного фасада: используйте частицы ванили или свои.
