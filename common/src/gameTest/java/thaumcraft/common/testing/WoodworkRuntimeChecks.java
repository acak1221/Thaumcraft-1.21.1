package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.config.ThaumcraftWoodwork;

/** Real native placement, redstone and owned sign serialization; no replacement packet/editor protocol. */
public final class WoodworkRuntimeChecks {
    private WoodworkRuntimeChecks() {}

    public static void placementAndSignPersistence(GameTestHelper test) {
        var level = test.getLevel();
        BlockPos origin = test.absolutePos(new BlockPos(4, 2, 4)).atY(160);
        Map<BlockPos, BlockState> old = new LinkedHashMap<>();
        Set<Long> forced = new HashSet<>();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-2,-1,-2), origin.offset(10,3,2))) {
            var fixed = pos.immutable();
            test.assertTrue(level.getBlockEntity(fixed) == null, "woodwork fixture never overwrites a foreign block entity");
            old.put(fixed, level.getBlockState(fixed));
            var chunk = new ChunkPos(fixed);
            if (!level.getForcedChunks().contains(chunk.toLong())) { level.setChunkForced(chunk.x, chunk.z, true); forced.add(chunk.toLong()); }
        }
        GameTestChunkAwait.entityTicking(test, origin);
        var player = test.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(origin.getX()+.5, origin.getY(), origin.getZ()+2.5);
        try {
            for (var family : ThaumcraftWoodwork.FAMILIES.values()) {
                for (var pos : old.keySet()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(origin.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(family.door().get(), 2));
                var result = player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(origin.below()).add(0,.5,0), Direction.UP, origin.below(), false)));
                test.assertTrue(result.consumesAction() && player.getMainHandItem().getCount() == 1
                                && level.getBlockState(origin).is(family.door().get())
                                && level.getBlockState(origin).getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                                && level.getBlockState(origin.above()).is(family.door().get())
                                && level.getBlockState(origin.above()).getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER,
                        "native two-block door placement consumes exactly one owned item: " + family.name());
                var lower = Block.getDrops(level.getBlockState(origin), level, origin, null, player, ItemStack.EMPTY);
                var upper = Block.getDrops(level.getBlockState(origin.above()), level, origin.above(), null, player, ItemStack.EMPTY);
                test.assertTrue(lower.size() == 1 && lower.getFirst().is(family.door().get().asItem()) && upper.isEmpty(),
                        "only the lower half contributes one native door item");
                var gatePos = origin.offset(4,0,0);
                level.setBlock(gatePos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(gatePos, family.fenceGate().get().defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(gatePos.west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                test.assertTrue(level.getBlockState(gatePos).getValue(FenceGateBlock.OPEN), "native gate opens from real adjacent power");
                level.setBlock(gatePos.west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                test.assertTrue(!level.getBlockState(gatePos).getValue(FenceGateBlock.OPEN), "native gate closes after power removal");
                var signPos = origin.offset(8,0,0);
                for (var block : List.of(family.sign(), family.wallSign(), family.hangingSign(), family.wallHangingSign())) {
                    for (Direction side : Direction.values()) level.setBlock(signPos.relative(side), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                    level.setBlock(signPos, block.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                    test.assertTrue(level.getBlockEntity(signPos) instanceof SignBlockEntity, "all sign forms construct a native sign entity");
                    var tile = (SignBlockEntity) level.getBlockEntity(signPos);
                    boolean hanging = block == family.hangingSign() || block == family.wallHangingSign();
                    test.assertTrue((tile instanceof HangingSignBlockEntity) == hanging,
                            "hanging forms retain the native hanging editor and line geometry");
                    tile.setText(tile.getFrontText().setMessage(0, Component.literal(family.name()+" front"))
                            .setColor(DyeColor.CYAN).setHasGlowingText(true), true);
                    tile.setText(tile.getBackText().setMessage(0, Component.literal("back text")), false);
                    tile.setWaxed(true);
                    var saved = tile.saveWithFullMetadata(level.registryAccess());
                    test.assertTrue(saved.getString("id").equals(hanging ? "thaumcraft:woodwork_hanging_sign" : "thaumcraft:woodwork_sign"),
                            "serialized identity belongs to the actual custom sign type");
                    var reloaded = (SignBlockEntity) BlockEntity.loadStatic(signPos, level.getBlockState(signPos), saved, level.registryAccess());
                    test.assertTrue(reloaded != null && reloaded.getType() == tile.getType()
                                    && reloaded.getFrontText().getMessage(0,false).getString().equals(family.name()+" front")
                                    && reloaded.getBackText().getMessage(0,false).getString().equals("back text")
                                    && reloaded.getFrontText().getColor() == DyeColor.CYAN
                                    && reloaded.getFrontText().hasGlowingText() && reloaded.isWaxed(),
                            "native sign save/load keeps both sides, dye, glow and wax");
                }
            }
        } finally {
            for (var entry : old.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), Block.UPDATE_CLIENTS);
            level.getEntitiesOfClass(ItemEntity.class, new AABB(origin).inflate(12)).forEach(entity -> {
                if (entity.getItem().is(ThaumcraftWoodwork.GREATWOOD.door().get().asItem())
                        || entity.getItem().is(ThaumcraftWoodwork.SILVERWOOD.door().get().asItem())) entity.discard();
            });
            for (long chunk : forced) level.setChunkForced(ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false);
            player.discard();
        }
        test.succeed();
    }

    public static void recipesUseRealMaterials(GameTestHelper test) {
        var level = test.getLevel();
        for (var family : ThaumcraftWoodwork.FAMILIES.values()) {
            var planks = family == ThaumcraftWoodwork.GREATWOOD ? ThaumcraftNaturals.GREATWOOD_PLANKS.get() : ThaumcraftNaturals.SILVERWOOD_PLANKS.get();
            var log = family == ThaumcraftWoodwork.GREATWOOD ? ThaumcraftNaturals.GREATWOOD_LOG.get() : ThaumcraftNaturals.SILVERWOOD_LOG.get();
            for (String kind : List.of("door","trapdoor","fence","fence_gate","sign","hanging_sign","button","pressure_plate")) {
                String[] rows = switch (kind) {
                    case "door" -> new String[]{"PP ","PP ","PP "};
                    case "trapdoor" -> new String[]{"PPP","PPP","   "};
                    case "fence" -> new String[]{"PSP","PSP","   "};
                    case "fence_gate" -> new String[]{"SPS","SPS","   "};
                    case "sign" -> new String[]{"PPP","PPP"," S "};
                    case "hanging_sign" -> new String[]{"C C","LLL","LLL"};
                    case "button" -> new String[]{"P  ","   ","   "};
                    default -> new String[]{"PP ","   ","   "};
                };
                List<ItemStack> cells = new ArrayList<>();
                for (String row : rows) for (char token : row.toCharArray()) cells.add(switch (token) {
                    case 'P' -> new ItemStack(planks); case 'L' -> new ItemStack(log);
                    case 'S' -> new ItemStack(Items.STICK); case 'C' -> new ItemStack(Items.CHAIN); default -> ItemStack.EMPTY;
                });
                var id = ResourceLocation.fromNamespaceAndPath("thaumcraft", "woodwork/"+family.name()+"_"+kind);
                var holder = level.getRecipeManager().byKey(id).orElseThrow();
                test.assertTrue(holder.value() instanceof CraftingRecipe, "woodwork uses a loaded native crafting recipe");
                var recipe = (CraftingRecipe) holder.value();
                CraftingInput input = CraftingInput.ofPositioned(3,3,cells).input();
                int expected = switch (kind) { case "door","fence","sign" -> 3; case "trapdoor" -> 2; case "hanging_sign" -> 6; default -> 1; };
                test.assertTrue(recipe.matches(input, level) && recipe.assemble(input, level.registryAccess()).getCount() == expected,
                        "loaded material pattern makes the native quantity: "+id);
                int first = 0; while (cells.get(first).isEmpty()) first++;
                cells.set(first, ItemStack.EMPTY);
                test.assertTrue(!recipe.matches(CraftingInput.ofPositioned(3,3,cells).input(), level),
                        "a missing paid ingredient cannot produce woodwork: "+id);
            }
        }
        test.succeed();
    }
}
