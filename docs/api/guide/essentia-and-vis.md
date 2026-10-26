# Blocks, essentia and vis

This page builds the example's essentia bowl: a block entity that holds essentia, shows it to goggles, gives it to a
tube, and answers a wand.

## A block entity on `TileThaumcraft`

`thaumcraft.api.TileThaumcraft` is the original base tile: `readCustomNBT`/`writeCustomNBT` hold the data that is both
saved and sent to clients.

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

- The custom data is saved with the block entity, sent with the chunk when a client first sees the block, and sent
  again whenever the server calls `level.sendBlockUpdated(pos, state, state, 3)` (1.7.10's `markBlockForUpdate`).
  Call `setChanged()` too (1.7.10's `markDirty`) so the chunk is saved.
- The update always carries the key `TileThaumcraft.UPDATE_KEY` besides your data: a 1.21.1 client ignores an update
  whose tag is empty, so without it a container emptied to nothing would stay full on screen.
- A block entity needs a registered `BlockEntityType` in 1.21.1. On Fabric:

<!-- snippet:block-entity-registration -->
```java
Registry.register(BuiltInRegistries.BLOCK, id("essentia_bowl"), ESSENTIA_BOWL);
Registry.register(BuiltInRegistries.ITEM, id("essentia_bowl"), new BlockItem(ESSENTIA_BOWL, new Item.Properties()));
ESSENTIA_BOWL_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("essentia_bowl"),
        BlockEntityType.Builder.of(EssentiaBowlBlockEntity::new, ESSENTIA_BOWL).build(null));
```
<!-- /snippet -->

  On NeoForge register the same `BlockEntityType` with a `DeferredRegister<BlockEntityType<?>>`.

The block itself is a plain 1.21.1 `BaseEntityBlock` (see `EssentiaBowlBlock.java`); a block entity that does not need
the custom-data hooks can extend `BlockEntity` directly and still implement any of the interfaces below.

## Holding essentia: `IAspectContainer`

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

`addToContainer` returns what did not fit; `takeFromContainer(aspect, amount)` is all or nothing. Goggles of revealing
(any `IGoggles` helmet) show the contents of an `IAspectContainer` block entity, read on the client, which is why the
data must be synchronised. Implement `IAspectSource` (an `IAspectContainer` marker) when the infusion altar and the
essentia mirror may drain the container: they take one unit at a time through `takeFromContainer`.

## Moving essentia: `IEssentiaTransport`

Tubes, jars, buffers, centrifuges, crystallisers and the other essentia machines pull essentia from neighbours that
suck less than they do. A neighbour that is an `IEssentiaTransport` takes part:

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

- `isConnectable(face)`: a tube connects (and draws its connection) to that face. `canInputFrom`/`canOutputTo` say which
  way essentia may go through it.
- Suction: a receiver pulls only from a neighbour whose `getSuctionAmount` is lower than its own, and only when its
  own suction is at least the neighbour's `getMinimumSuction`. An empty jar sucks 32, a tube one less than its
  strongest neighbour. A pure source answers 0 and never pulls; a consumer sets its own suction with `setSuction` and
  pulls in its own tick.
- A receiver asks a source for one unit with `takeEssentia(aspect, 1, face)` and is credited with what it returns. As in
  the original, the source decides; a surplus beyond the unit asked for goes back through its `addEssentia`.
- `getEssentiaType`/`getEssentiaAmount` describe what the face offers; `renderExtendedTube` lengthens the tube's
  connection model to reach a smaller block.

An add-on block that receives essentia pulls from a tube or a jar the same way: in its server tick, call
`takeEssentia(aspect, 1, face)` on the neighbour (through `ThaumcraftApiHelper.getConnectableTile`) and add what it
returns.

## Essentia as items: `IEssentiaContainerItem`

Jars, crystallised essentia, wisp essence and mana beans report their contents with
`IEssentiaContainerItem.getAspects(stack)`. The bowl takes crystallised essentia from the player's hand:

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

Phials do not implement `IEssentiaContainerItem` in this port: an empty and a filled phial are two items, and a filled
phial holds eight of one aspect in its own component.

## A block that answers a wand

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

See [wands and foci](wands-and-foci.md#blocks-that-answer-a-wand-iwandable) for the whole contract.

## Vis from the network and from the aura

- `VisNetHandler.drainVis(level, pos, aspect, amount)`: takes up to `amount` **centivis** of a primal from the vis
  relay network in range of `pos` and returns what it took (the bowl above asks for 100, one whole vis). Server only;
  0 on a client.
- `VisNetHandler.drainVis(level, pos, amount, simulate)`: the aspect-less form of the 1.12 surface, over the six primals.
- `AuraHelper` keeps the 1.12 add-on surface over the TC4 aura, which is the essence of the aura nodes: `getVis` and
  `addVis` read and fill the node at a position, `drainVis` drains the vis network there. TC4 has no per-chunk aura
  and no flux, so the flux accessors of that surface do not exist.

## Effects

`ThaumcraftFX` sends, from the server, the effect packets 1.7.10 add-ons built themselves:

| Call | Original packet |
|---|---|
| `blockSparkle(level, pos, rgb)` (`ThaumcraftFX.RANDOM_GREY` for the grey sparkles) | `PacketFXBlockSparkle` |
| `blockArc(level, source, target)` | `PacketFXBlockArc` |
| `zap(level, from, to)` | `PacketFXBlockZap` |
| `bubbles(level, pos, rgb)` | `PacketFXBlockBubble` |
| `essentiaTrail(level, consumer, source, aspect)` | `PacketFXEssentiaSource` |
| `visDrain(level, consumer, node, aspect)` | `PacketFXVisDrain` |

The client-side particles of `Thaumcraft.proxy` (`sparkle`, `beam`, `wispFX`...) are internals of `thaumcraft.client.fx`
without a stable facade: use vanilla particles or your own.
