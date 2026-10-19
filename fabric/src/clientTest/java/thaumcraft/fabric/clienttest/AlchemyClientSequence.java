package thaumcraft.fabric.clienttest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftResources;
import thaumcraft.common.entities.EntitySpecialItem;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.tiles.TileCrucible;

/** Supplied materials/camera fixture, but native wand/bucket/drop/collision/craft/pickup/place actions. */
final class AlchemyClientSequence {
    private enum Step {
        SETUP, CONVERT, FILL, BOIL, COAL_CAMERA, COAL_DROP, COAL_WAIT, TORCH_DROP, TORCH_WAIT,
        SOUP, GLOW_CAMERA, GLOW_DROP, GLOW_ACK, OUTPUT_CAMERA, OUTPUT, PICKUP, PLACE, PLACED,
        BOOK, BOOK_PAGE, FLUX_SETUP, FLUX_SCENE, DONE
    }
    final Map<String, Object> evidence = new LinkedHashMap<>();
    private final BlockPos bowl;
    private Step step = Step.SETUP;
    private int ticks;
    private int boilingSince = -1;
    private boolean catalystDropAcknowledged;
    private CompletableFuture<Void> serverAction;

    AlchemyClientSequence(BlockPos research) {
        bowl = research.offset(4, 0, 2);
        evidence.put("boundary", "Supplied vanilla materials and NITOR research; server teleports are camera fixtures, not a full survival run. No aspects/heat/results are injected.");
    }

    boolean tick(Minecraft client, Consumer<String> capture) throws ReflectiveOperationException {
        ticks++;
        if (ticks > 600) throw new IllegalStateException("Alchemy client step timed out: " + step
                + "; water=" + (client.level.getBlockEntity(bowl) instanceof TileCrucible tile ? tile.getWaterAmount() : -1)
                + "; slot4=" + client.player.getInventory().getItem(4)
                + "; nearby=" + client.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(bowl).inflate(7)).stream()
                .map(entity -> entity.getItem() + "@" + entity.position()).toList());
        switch (step) {
            case SETUP -> {
                client.setScreen(null);
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    var level = player.serverLevel();
                    for (int x = -2; x <= 7; x++) for (int z = -2; z <= 3; z++) {
                        level.setBlockAndUpdate(bowl.offset(x, -2, z), Blocks.STONE.defaultBlockState());
                        for (int y = -1; y <= 3; y++) level.setBlockAndUpdate(bowl.offset(x, y, z), Blocks.AIR.defaultBlockState());
                        level.setBlockAndUpdate(bowl.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                    }
                    level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(bowl).inflate(7)).forEach(entity -> entity.discard());
                    level.setBlockAndUpdate(bowl.below(), Blocks.FIRE.defaultBlockState());
                    level.setBlockAndUpdate(bowl, Blocks.CAULDRON.defaultBlockState());
                    player.setGameMode(GameType.SURVIVAL);
                    player.setHealth(20);
                    player.getInventory().clearContent();
                    player.getInventory().setItem(0, new ItemStack(ThaumcraftContent.WAND.get()));
                    player.getInventory().setItem(1, new ItemStack(Items.WATER_BUCKET));
                    player.getInventory().setItem(2, new ItemStack(Items.COAL, 2));
                    player.getInventory().setItem(3, new ItemStack(Items.TORCH, 3));
                    player.getInventory().setItem(4, new ItemStack(Items.GLOWSTONE_DUST));
                    player.getInventory().selected = 0;
                    KnowledgeAccess.get(player).addResearch("NITOR");
                    KnowledgeEvents.sync(player);
                    player.inventoryMenu.broadcastChanges();
                    player.teleportTo(bowl.getX() - 1.5, bowl.getY(), bowl.getZ() + 1.5);
                });
                next(Step.CONVERT);
            }
            case CONVERT -> {
                if (!serverDone() || ticks < 15 || !client.player.getMainHandItem().is(ThaumcraftContent.WAND.get())) return false;
                lookAt(client, Vec3.atCenterOf(bowl));
                useBlock(client, bowl);
                next(Step.FILL);
            }
            case FILL -> {
                if (!(client.level.getBlockEntity(bowl) instanceof TileCrucible) || ticks < 10) return false;
                require(client.player.getInventory().getItem(0).is(ThaumcraftContent.WAND.get()), "cauldron conversion retains its wand");
                client.player.getInventory().selected = 1;
                if (ticks < 14) return false;
                useBlock(client, bowl);
                next(Step.BOIL);
            }
            case BOIL -> {
                lookAt(client, Vec3.atCenterOf(bowl).add(0, .2, 0));
                var tile = tile(client);
                if (tile.getWaterAmount() != 1000 || tile.getHeat() <= 150) return false;
                if (boilingSince < 0) boilingSince = ticks;
                if (ticks - boilingSince < 12) return false;
                require(client.player.getInventory().getItem(1).is(Items.BUCKET) && tile.tagAmount() == 0,
                        "actual bucket fills exactly 1000 and native heat ticks boil clean water");
                evidence.put("wand_and_bucket", "Native useItemOn converted a cauldron, retained the empty wand, consumed water bucket, then natural heat crossed150");
                next(Step.COAL_CAMERA);
                capture.accept("27-crucible-boiling-clean-water-en.png");
            }
            case COAL_CAMERA -> {
                standOnRim(client);
                next(Step.COAL_DROP);
            }
            case COAL_DROP -> {
                if (!serverDone() || ticks < 10) return false;
                client.player.setXRot(90); client.player.xRotO = 90;
                client.player.getInventory().selected = 2;
                if (ticks < 14) return false;
                require(client.player.drop(true), "native client drops the supplied coal stack");
                next(Step.COAL_WAIT);
            }
            case COAL_WAIT -> {
                if (tile(client).getAspects().getAmount(Aspect.ENERGY) != 4 || tile(client).getAspects().getAmount(Aspect.FIRE) != 4) return false;
                next(Step.TORCH_DROP);
            }
            case TORCH_DROP -> {
                client.player.getInventory().selected = 3;
                if (ticks < 5) return false;
                require(client.player.drop(true), "native client drops three original-lux torches");
                next(Step.TORCH_WAIT);
            }
            case TORCH_WAIT -> {
                if (tile(client).getAspects().getAmount(Aspect.LIGHT) != 3) return false;
                onServer(client, () -> serverPlayer(client).teleportTo(bowl.getX() - 1.5, bowl.getY(), bowl.getZ() + 1.5));
                next(Step.SOUP);
            }
            case SOUP -> {
                if (!serverDone() || ticks < 10) return false;
                var tile = tile(client);
                require(tile.getWaterAmount() == 1000 && tile.tagAmount() == 11, "real coal and torch collisions supplied4potentia/4ignis/3lux without consuming water");
                evidence.put("physical_dissolution", Map.of("coal", 2, "torches", 3, "potentia", 4, "ignis", 4, "lux", 3, "water", 1000));
                lookAt(client, Vec3.atCenterOf(bowl).add(0, .2, 0));
                next(Step.GLOW_CAMERA);
                capture.accept("28-crucible-dissolved-coal-and-torches-en.png");
            }
            case GLOW_CAMERA -> {
                standOnRim(client);
                next(Step.GLOW_DROP);
            }
            case GLOW_DROP -> {
                if (!serverDone() || ticks < 10) return false;
                client.player.setXRot(90); client.player.xRotO = 90;
                client.player.getInventory().selected = 4;
                if (ticks < 14) return false;
                require(client.player.drop(true), "native client drops actual glowstone catalyst with server-owned thrower UUID");
                acknowledgeCatalystDrop(client);
                next(Step.GLOW_ACK);
            }
            case GLOW_ACK -> {
                if (!serverDone() || ticks < 2) return false;
                if (!catalystDropAcknowledged) { acknowledgeCatalystDrop(client); return false; }
                next(Step.OUTPUT_CAMERA);
            }
            case OUTPUT_CAMERA -> {
                if (!serverDone() || ticks < 10) return false;
                lookAt(client, Vec3.atCenterOf(bowl).add(0, .45, 0));
                next(Step.OUTPUT);
            }
            case OUTPUT -> {
                var tile = tile(client);
                var outputs = client.level.getEntitiesOfClass(EntitySpecialItem.class, new AABB(bowl).inflate(3),
                        entity -> entity.getItem().is(ThaumcraftContent.MATERIALS.get("nitor").get()));
                if (tile.getWaterAmount() != 950 || outputs.size() != 1) return false;
                require(tile.tagAmount() == 2 && tile.getAspects().getAmount(Aspect.ENERGY) == 1 && tile.getAspects().getAmount(Aspect.FIRE) == 1
                        && outputs.getFirst().getItem().getCount() == 1, "physical Nitor craft consumes exactly50water and3ofeach required aspect");
                evidence.put("physical_nitor", Map.of("result_count", 1, "water_after", 950, "potentia_after", 1, "ignis_after", 1,
                        "entity_type", "thaumcraft:special_item", "thrower_checked", true));
                next(Step.PICKUP);
                capture.accept("29-crucible-physical-floating-nitor-en.png");
            }
            case PICKUP -> {
                if (ticks == 1) onServer(client, () -> {
                    var player = serverPlayer(client);
                    var item = player.serverLevel().getEntitiesOfClass(EntitySpecialItem.class, new AABB(bowl).inflate(3),
                            entity -> entity.getItem().is(ThaumcraftContent.MATERIALS.get("nitor").get())).getFirst();
                    player.teleportTo(item.getX(), item.getY(), item.getZ());
                });
                if (!serverDone() || ticks < 10) return false;
                int slot = nitorSlot(client);
                if (slot < 0) return false;
                client.player.getInventory().selected = slot;
                onServer(client, () -> serverPlayer(client).teleportTo(bowl.getX() - 1.5, bowl.getY(), bowl.getZ() + 1.5));
                next(Step.PLACE);
            }
            case PLACE -> {
                if (!serverDone() || ticks < 10) return false;
                require(client.player.getMainHandItem().is(ThaumcraftContent.MATERIALS.get("nitor").get()), "picked-up real crafted output is held, not a supplied replacement");
                useBlock(client, bowl.below(2));
                next(Step.PLACED);
            }
            case PLACED -> {
                if (!client.level.getBlockState(bowl.below()).is(ThaumcraftResources.NITOR.get()) || ticks < 15) return false;
                require(nitorSlot(client) < 0 && client.level.getBlockState(bowl.below()).getLightEmission() == 15,
                        "actual ItemNitor placement consumes the crafted item and creates its light15 heat-source block");
                evidence.put("crafted_nitor_placed", Map.of("light_emission", 15, "item_consumed", true, "below_crucible", true));
                lookAt(client, Vec3.atCenterOf(bowl).add(0, -.3, 0));
                next(Step.BOOK);
                capture.accept("30-crafted-nitor-heats-crucible-world-en.png");
            }
            case BOOK -> {
                client.setScreen(new GuiResearchRecipe(new GuiResearchBrowser(), ResearchCatalog.get("NITOR")));
                next(Step.BOOK_PAGE);
            }
            case BOOK_PAGE -> {
                if (ticks < 10) return false;
                int page = (int) BookClientSmoke.field(client.screen, "page");
                var pages = (List<?>) BookClientSmoke.field(client.screen, "pages");
                boolean visible = false;
                for (int i = page; i < Math.min(page + 2, pages.size()); i++) visible |= BookClientSmoke.field(pages.get(i), "recipe") != null;
                if (!visible) {
                    var button = client.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                            .filter(widget -> widget.getMessage().getString().equals("→") && widget.active).findFirst().orElseThrow();
                    client.screen.mouseClicked(button.getX() + 5, button.getY() + 5, 0);
                    client.screen.mouseReleased(button.getX() + 5, button.getY() + 5, 0);
                    ticks = 0;
                    return false;
                }
                evidence.put("native_nitor_recipe_page_visible", true);
                next(Step.FLUX_SETUP);
                capture.accept("31-nitor-crucible-recipe-book-en.png");
            }
            case FLUX_SETUP -> {
                client.setScreen(null);
                onServer(client, () -> {
                    var level = serverPlayer(client).serverLevel();
                    for (int i = 0; i < 2; i++) {
                        BlockPos center = bowl.offset(3 + i * 2, 0, 0);
                        for (Direction direction : Direction.values()) level.setBlockAndUpdate(center.relative(direction), Blocks.GLASS.defaultBlockState());
                        level.setBlockAndUpdate(center, fluxState(i == 0 ? "flux_gas" : "flux_goo"));
                    }
                    serverPlayer(client).teleportTo(bowl.getX() + 4.5, bowl.getY(), bowl.getZ() + 3.5);
                });
                next(Step.FLUX_SCENE);
            }
            case FLUX_SCENE -> {
                if (!serverDone() || ticks < 15) return false;
                lookAt(client, Vec3.atCenterOf(bowl.offset(4, 0, 0)));
                evidence.put("flux_visual_fixture", "Supplied full eight-quanta gas/goo in separate sealed glass cells; runtime tests, not this picture, check spill/exposure/finite movement");
                next(Step.DONE);
                capture.accept("32-flux-gas-and-goo-native-world-en.png");
            }
            case DONE -> { return true; }
        }
        return false;
    }

    private static BlockState fluxState(String id) {
        BlockState state = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", id)).defaultBlockState();
        require(!state.isAir(), "registered flux block is present");
        for (var property : state.getProperties()) if (property.getName().equals("level") && property instanceof IntegerProperty level) return state.setValue(level, 7);
        throw new IllegalStateException("Flux must expose its finite level0..7");
    }
    private void standOnRim(Minecraft client) {
        onServer(client, () -> serverPlayer(client).teleportTo(bowl.getX() + .4, bowl.getY() + 1, bowl.getZ() + .5));
    }
    private void acknowledgeCatalystDrop(Minecraft client) {
        onServer(client, () -> {
            var player = serverPlayer(client);
            // The C2S drop packet and an integrated-server Runnable are NOT an
            // ordered channel. A camera teleport before that packet moves the
            // actual catalyst drop away from the crucible. Observe commitment,
            // never move the item or supply a substitute catalyst/result.
            if (!player.getInventory().getItem(4).isEmpty()) return;
            catalystDropAcknowledged = true;
            player.teleportTo(bowl.getX() - 1.5, bowl.getY(), bowl.getZ() + 1.5);
        });
    }
    private TileCrucible tile(Minecraft client) { return (TileCrucible) client.level.getBlockEntity(bowl); }
    private static int nitorSlot(Minecraft client) {
        for (int slot = 0; slot < 9; slot++) if (client.player.getInventory().getItem(slot).is(ThaumcraftContent.MATERIALS.get("nitor").get())) return slot;
        return -1;
    }
    private static void useBlock(Minecraft client, BlockPos pos) {
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), Direction.UP, pos, false));
    }
    private static void lookAt(Minecraft client, Vec3 target) {
        Vec3 delta = target.subtract(client.player.getEyePosition());
        client.player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        client.player.yRotO = client.player.getYRot();
        client.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))));
        client.player.xRotO = client.player.getXRot();
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private static net.minecraft.server.level.ServerPlayer serverPlayer(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private void onServer(Minecraft client, Runnable action) { serverAction = CompletableFuture.runAsync(action, client.getSingleplayerServer()); }
    private boolean serverDone() { if (!serverAction.isDone()) return false; serverAction.join(); return true; }
    private void next(Step next) { step = next; ticks = 0; }
}
