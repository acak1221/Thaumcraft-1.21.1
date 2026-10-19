package thaumcraft.fabric.clienttest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.gui.GuiArcaneWorkbench;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.client.gui.ThaumcraftRecipeDisplay;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftResources;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneCrafting;
import thaumcraft.common.entities.EntitySpecialItem;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.tiles.TileAlchemyFurnace;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileCrucible;
import thaumcraft.common.tiles.TileJar;

/** A bounded native metallurgy/equipment story, not survival acquisition or a second crafting implementation. */
public final class ThaumiumEquipmentClientSequence {
    private enum Step {
        HANDOFF, HANDOFF_WAIT, SETUP, BOOK, BOOK_PAGE, CONVERT, CONVERTED, BUCKET, FILL, BOIL,
        BATCH_INPUTS, ROD_POSITION, ROD_HAND, ROD_DROP, ROD_ACK, ROD_SOUP, ROD_VERIFY,
        IRON_POSITION, IRON_HAND, IRON_DROP, IRON_ACK, METAL_WAIT, METAL_VERIFY, METAL_WORLD,
        METAL_APPROACH, METAL_PICKUP, BATCH_FINISH, BATCH_VERIFIED,
        PICK_CRAFT, CHEST_CRAFT, LEGS_INPUT, LEGS_CRAFT,
        CRAFT_TRAVEL, CRAFT_OPEN, CRAFT_GRID, CRAFT_READY, CRAFT_PREVIEW, CRAFT_TAKE,
        CRAFT_OUTPUT, CRAFT_STORE, CRAFT_STORED, CRAFT_VERIFIED,
        MINE_POSITION, MINE_HAND, MINE_START, MINE, MINE_COMMIT, MINE_WORLD, MINE_APPROACH, MINE_PICKUP, MINE_PICKED,
        ANVIL_POSITION, ANVIL_OPEN, ANVIL_PICK, ANVIL_INGOT, ANVIL_READY, ANVIL_PREVIEW,
        ANVIL_TAKE, ANVIL_OUTPUT, ANVIL_STORE, ANVIL_STORED, ANVIL_VERIFIED,
        EQUIP_CHEST, EQUIP_CHEST_WAIT, EQUIP_LEGS, EQUIP_LEGS_WAIT, EQUIPPED_VERIFY,
        ARMOR_POSITION, ARMOR_WORLD, DONE
    }

    private static final int[] RODS = {8, 8, 6, 2};
    private static final int[] INGOTS = {4, 4, 3, 1};
    private static final List<String> CAPTURES = List.of(
            "61-native-thaumium-ordinary-equipment-recipe-book-en.png",
            "62-native-twelve-thaumium-finite-crucible-last-output-world-en.png",
            "63-native-thaumium-pickaxe-zero-vis-no-wand-workbench-menu-en.png",
            "64-native-thaumium-pickaxe-survival-obsidian-drop-and-wear-world-en.png",
            "65-native-thaumium-pickaxe-earned-ingot-anvil-repair-menu-en.png",
            "66-native-thaumium-chestplate-leggings-two-armor-layers-world-en.png");

    public final Map<String, Object> report = new LinkedHashMap<>();
    private final BlockPos workbench, crucible, anvil, stone, obsidian, oldOven;
    private final List<ItemStack> retainedStacks = new ArrayList<>();
    private final Map<BlockPos, Map<String, Object>> retainedJars = new LinkedHashMap<>();
    private final List<Map<String, Object>> inputs = new ArrayList<>(), batches = new ArrayList<>();
    private final List<Map<String, Object>> crafts = new ArrayList<>(), mining = new ArrayList<>(), crucibleStates = new ArrayList<>();
    private Step step = Step.HANDOFF, afterCraft;
    private CompletableFuture<Void> serverAction;
    private CameraType previousCamera;
    private ItemStack handoff = ItemStack.EMPTY, oldWand, newWand, apple, oldFuel, wornPick, craftedChest, craftedLegs;
    private int handoffSlot = -1, newWandSlot, ticks, totalTicks, turns, batch, rodsSupplied, earned;
    private int handChangedAt = Integer.MIN_VALUE / 2, gridIndex, destination, craftIngotsBefore, mineIndex, mineTicks, damageBefore;
    private int repairLevelsBefore, repairCost, levelsInputBefore;
    private boolean dropCommitted, mineCommitted;
    private long heatedAt, mineStarted;
    private UUID miningDrop;
    private ItemStack miningDropStack;
    private List<UUID> metalDrops = List.of();
    private Map<String, Object> snapshot = Map.of();
    private CraftPlan craft;
    private List<int[]> gridClicks = List.of();

    private record CraftPlan(String recipe, Item output, Item[] grid, String capture) {}

    /** Root supplies research+(224,0,8) and research+(240,0,8); accepted earlier patches are not reused. */
    public ThaumiumEquipmentClientSequence(BlockPos workbench, BlockPos crucible) {
        this.workbench = workbench.immutable();
        this.crucible = crucible.immutable();
        anvil = workbench.east(2);
        stone = workbench.offset(5, 0, 0);
        obsidian = workbench.offset(5, 0, -2);
        oldOven = workbench.west(57); // Accepted M07 center is research+176; its oven is center-9.
        report.put("boundary", "Retains both actual earned wands, all prior inventory item counts/components, book6, greatwood/gold wand7, offhand apple, M07 jars and the old oven's five coal. Declared INPUT fixtures: 24 blaze rods,12 iron ingots,2 sticks,1 water bucket, THAUMIUM research,30 additional XP levels; empty workbench/anvil, empty cauldron, heat-source Nitor, bounded floor including one plain-stone throwing/pickup step, one stone and one obsidian mining target, camera/player positioning. Only AFTER all12 earned ingots are spent, supplies seven separately declared INTERMEDIATE INPUT thaumium ingots for leggings. Not full survival acquisition.");
        report.put("native_action_boundary", "Native inventory SWAP/QUICK_MOVE/PICKUP packets, wand cauldron conversion, water-bucket useItemOn, finite player.drop stacks and real crucible collisions/ticks/output pickup, ordinary 3x3 workbench results with no inserted wand, survival start/continueDestroyBlock, physical mining drops/pickup, native anvil result extraction/XP payment and native inventory armor equipping. Server futures only declare inputs/empty fixtures/camera or inspect. No backend ticks, dissolve/craft calls, output grants, armor setters, tool-damage setters, heat/water/aspect/counter writes, wand top-ups or buffer clearing.");
        report.put("metallurgy_plan", Map.of("raw_rods", 24, "raw_iron", 12, "earned_ingots", 12, "praecantatio_paid", 48,
                "rods_by_batch", List.of(8, 8, 6, 2), "ingots_by_batch", List.of(4, 4, 3, 1),
                "no_decay_pre_catalyst_tag_totals", List.of(48, 80, 100, 100), "no_decay_water_after", 400, "no_decay_ignis_after", 96));
        report.put("decay_policy", "Observe actual water/aspects at each boundary. Pure ignis decay is accounted exactly as one lost ignis and2water per event, including later crafting/mining time; residues are never cleared. Fail closed with the actual snapshot if praecantatio decays before its finite catalyst drop. No compensating aspects, hidden raw top-ups or forced counter reset.");
        report.put("coordinates", Map.of("workbench", workbench.toShortString(), "crucible", crucible.toShortString(), "anvil", anvil.toShortString(),
                "stone_target", stone.toShortString(), "obsidian_target", obsidian.toShortString(), "throwing_step", crucible.west().toShortString(), "retained_M07_oven", oldOven.toShortString()));
        report.put("preparation_bounds", Map.of("workbench_min", workbench.offset(-2, -2, -4).toShortString(), "workbench_max", workbench.offset(7, 4, 7).toShortString(),
                "crucible_min", crucible.offset(-3, -2, -3).toShortString(), "crucible_max", crucible.offset(3, 4, 3).toShortString(), "refuse_any_existing_BE", true));
        report.put("supplied_inputs", inputs);
        report.put("native_metallurgy_batches", batches);
        report.put("actual_crucible_boundaries", crucibleStates);
        report.put("native_ordinary_crafts", crafts);
        report.put("native_mining", mining);
        report.put("capture_names", CAPTURES);
        report.put("capture_count", CAPTURES.size());
        report.put("visual_inspection", "Pending: resource/model probes and capture requests are not proof that either armor layer was visually inspected.");
        report.put("not_covered", List.of("full survival/research/XP acquisition", "crafting all nine equipment items in this client story",
                "ordinary crafting with THAUMIUM research absent (server check, not detached from the earned client research chain)",
                "native incoming armor damage/combat in this client story", "large-damage25% repair boundary", "enchanting/Repair enchantment/runic shielding",
                "thaumium caps/Salis Mundus/auram sources", "TC4 combat timing parity (modern native cooldown is an adaptation)", "NeoForge", "visual/audio parity before exact capture inspection"));
    }

    public boolean tick(Minecraft client, Consumer<String> capture) throws ReflectiveOperationException {
        try {
            return tickSequence(client, capture);
        } catch (ReflectiveOperationException | RuntimeException | Error error) {
            client.options.keyUse.setDown(false);
            client.options.keyShift.setDown(false);
            client.options.keyAttack.setDown(false);
            client.gameMode.stopDestroyBlock();
            if (previousCamera != null) client.options.setCameraType(previousCamera);
            if (error instanceof RuntimeException) throw new IllegalStateException("Thaumium sequence failed at " + step
                    + "; batch=" + batch + "; earned=" + earned + "; recipe=" + (craft == null ? "none" : craft.recipe()), error);
            throw error;
        }
    }

    private boolean tickSequence(Minecraft client, Consumer<String> capture) throws ReflectiveOperationException {
        ++ticks; ++totalTicks;
        if (ticks > 1200 || totalTicks > 12000) throw new IllegalStateException("Thaumium client timeout: " + step + "; hand=" + client.player.getMainHandItem());
        switch (step) {
            case HANDOFF -> {
                previousCamera = client.options.getCameraType();
                client.options.setCameraType(CameraType.FIRST_PERSON);
                client.options.keyUse.setDown(false); client.options.keyShift.setDown(false); client.options.keyAttack.setDown(false);
                client.setScreen(null);
                require(client.player.containerMenu == client.player.inventoryMenu && client.player.containerMenu.getCarried().isEmpty(), "M07 returned its actual native menu/cursor");
                handoff = client.player.getInventory().getItem(8).copy();
                require(isSilverwood(handoff) && ItemWandCasting.getVis(handoff, "aer") >= 6000, "hotbar8 contains the actually earned and charged100vis Silverwood/gold wand, not an empty action slot");
                handoffSlot = emptyMain(client.player);
                click(client, inventorySlot(handoffSlot), 8, ClickType.SWAP);
                next(Step.HANDOFF_WAIT);
            }
            case HANDOFF_WAIT -> {
                if (ticks < 8 || !client.player.getInventory().getItem(8).isEmpty() || !ItemStack.matches(client.player.getInventory().getItem(handoffSlot), handoff)) return false;
                report.put("native_earned_wand_handoff", Map.of("from_hotbar", 8, "to_empty_main_slot", handoffSlot, "action", "native inventory SWAP", "aer_centivis", ItemWandCasting.getVis(handoff, "aer")));
                next(Step.SETUP);
            }
            case SETUP -> {
                select(client, 8);
                onServer(client, () -> prepare(serverPlayer(client)));
                next(Step.BOOK);
            }
            case BOOK -> {
                if (!serverDone() || ticks < 12 || !KnowledgeAccess.get(client.player).isResearchComplete("THAUMIUM")) return false;
                client.setScreen(new GuiResearchRecipe(new GuiResearchBrowser(), ResearchCatalog.get("THAUMIUM")));
                next(Step.BOOK_PAGE);
            }
            case BOOK_PAGE -> {
                if (ticks < 8 || !showRecipe(client)) return false;
                report.put("native_book", Map.of("research", "THAUMIUM", "ordinary_recipe", id("thaumium_pickaxe").toString(), "native_synced_recipe_holder", true,
                        "craft_research_gate_claimed", false, "research_screen_fixture", true));
                next(Step.CONVERT);
                capture.accept(CAPTURES.get(0));
            }
            case CONVERT -> {
                client.setScreen(null);
                select(client, 7);
                use(client, crucible, Direction.UP, Vec3.atCenterOf(crucible).add(0, .5, 0));
                next(Step.CONVERTED);
            }
            case CONVERTED -> {
                if (ticks < 8 || !(client.level.getBlockEntity(crucible) instanceof TileCrucible tile)) return false;
                require(tile.getWaterAmount() == 0 && tile.tagAmount() == 0 && ItemStack.matches(client.player.getInventory().getItem(7), oldWand), "native wand conversion creates an empty crucible for no vis");
                next(Step.BUCKET);
            }
            case BUCKET -> {
                if (!hand(client, stack -> stack.is(Items.WATER_BUCKET))) return false;
                use(client, crucible, Direction.UP, Vec3.atCenterOf(crucible).add(0, .5, 0));
                next(Step.FILL);
            }
            case FILL -> {
                if (ticks < 6 || bowl(client.level).getWaterAmount() != 1000 || count(client.player, Items.WATER_BUCKET) != 0 || count(client.player, Items.BUCKET) != 1) return false;
                onServer(client, () -> heatedAt = serverPlayer(client).serverLevel().getGameTime());
                next(Step.BOIL);
            }
            case BOIL -> {
                if (!serverDone() || bowl(client.level).getHeat() <= 150) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client); var tile = bowl(player.level());
                    require(tile.getHeat() > 150 && tile.tagAmount() == 0 && tile.getWaterAmount() == 1000, "real Nitor-source native ticks heat bucket-filled clean water above150");
                    retained(player);
                    report.put("native_crucible_start", Map.of("native_wand_cauldron_conversion", true, "water_bucket_consumed", 1, "returned_buckets", 1,
                            "water", tile.getWaterAmount(), "heat", tile.getHeat(), "elapsed_server_ticks_since_fill_ack", player.serverLevel().getGameTime() - heatedAt,
                            "heat_fixture", "thaumcraft:nitor (not claimed crafted here)", "backend_heat_or_water_writes", 0));
                });
                next(Step.BATCH_INPUTS);
            }
            case BATCH_INPUTS -> {
                if (!serverDone()) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    require(count(player, ingot()) == earned && count(player, Items.BLAZE_ROD) == 0 && count(player, Items.IRON_INGOT) == 0 && metalEntities(player.level()).isEmpty(), "next finite batch starts ONLY after actual prior craft and physical pickup");
                    inspectCrucible(player, "before batch " + (batch + 1), rodsSupplied, earned, 0);
                    supply(player, Items.BLAZE_ROD, RODS[batch], "raw metallurgy INPUT batch " + (batch + 1));
                    supply(player, Items.IRON_INGOT, INGOTS[batch], "raw native catalyst INPUT batch " + (batch + 1));
                    rodsSupplied += RODS[batch];
                    player.inventoryMenu.broadcastChanges();
                });
                next(Step.ROD_POSITION);
            }
            case ROD_POSITION -> {
                if (!serverDone()) return false;
                standForDrop(client);
                next(Step.ROD_HAND);
            }
            case ROD_HAND -> {
                if (!serverDone()) return false;
                aimDrop(client);
                if (ticks < 10 || !hand(client, stack -> stack.is(Items.BLAZE_ROD))) return false;
                require(client.player.getMainHandItem().getCount() == RODS[batch], "finite native raw rod stack, never a supplied aspect buffer");
                next(Step.ROD_DROP);
            }
            case ROD_DROP -> {
                aimDrop(client);
                if (ticks < 4) return false; // C2S drop carries no rotation; let normal look packets reach the real thrower first.
                require(client.player.drop(true), "native player drops the finite blaze-rod INPUT stack");
                dropCommitted = false;
                next(Step.ROD_ACK);
            }
            case ROD_ACK -> {
                if (!serverDone()) return false;
                if (!dropCommitted) { acknowledgeDrop(client); return false; }
                next(Step.ROD_SOUP);
            }
            case ROD_SOUP -> {
                if (!serverDone() || ticks < 6) return false;
                if (bowl(client.level).getAspects().getAmount(Aspect.MAGIC) != RODS[batch] * 2 && ticks < 100) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    require(rawEntities(player.level(), Items.BLAZE_ROD).isEmpty() && count(player, Items.BLAZE_ROD) == 0, "all dropped raw rods actually dissolved by native collision");
                    inspectCrucible(player, "batch " + (batch + 1) + " dissolved rods / before iron", rodsSupplied, earned, RODS[batch] * 2);
                    require(bowl(player.level()).tagAmount() <= TileCrucible.MAX_TAGS, "finite four-batch soup never exceeds original100tag overflow threshold");
                });
                next(Step.ROD_VERIFY);
            }
            case ROD_VERIFY -> {
                if (!serverDone()) return false;
                next(Step.IRON_POSITION);
            }
            case IRON_POSITION -> {
                standForDrop(client);
                next(Step.IRON_HAND);
            }
            case IRON_HAND -> {
                if (!serverDone()) return false;
                aimDrop(client);
                if (ticks < 10 || !hand(client, stack -> stack.is(Items.IRON_INGOT))) return false;
                require(client.player.getMainHandItem().getCount() == INGOTS[batch] && bowl(client.level).getAspects().getAmount(Aspect.MAGIC) == INGOTS[batch] * 4,
                        "actual native soup funds each finite iron catalyst at4praecantatio before dropping it");
                next(Step.IRON_DROP);
            }
            case IRON_DROP -> {
                aimDrop(client);
                if (ticks < 4) return false;
                require(client.player.drop(true), "native player drops only this funded finite iron catalyst stack");
                dropCommitted = false;
                next(Step.IRON_ACK);
            }
            case IRON_ACK -> {
                if (!serverDone()) return false;
                if (!dropCommitted) { acknowledgeDrop(client); return false; }
                next(Step.METAL_WAIT);
            }
            case METAL_WAIT -> {
                if (ticks < 8) return false;
                int physical = metalEntities(client.level).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
                if ((bowl(client.level).getAspects().getAmount(Aspect.MAGIC) != 0 || physical != INGOTS[batch]) && ticks < 100) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client); var drops = metalEntities(player.level());
                    inspectCrucible(player, "batch " + (batch + 1) + " actual craft", rodsSupplied, earned + INGOTS[batch], 0);
                    require(rawEntities(player.level(), Items.IRON_INGOT).isEmpty() && count(player, Items.IRON_INGOT) == 0 && count(player, ingot()) == earned
                            && drops.stream().mapToInt(entity -> entity.getItem().getCount()).sum() == INGOTS[batch], "native iron collision emits this batch's physical special-item outputs, none granted/picked yet");
                    metalDrops = drops.stream().map(ItemEntity::getUUID).toList();
                    snapshot = Map.of("batch", batch + 1, "raw_rods", RODS[batch], "raw_iron", INGOTS[batch], "praecantatio_paid", INGOTS[batch] * 4,
                            "native_physical_ingots", INGOTS[batch], "native_drop_uuids_at_observation", metalDrops.stream().map(UUID::toString).toList(),
                            "cumulative_earned", earned + INGOTS[batch], "actual_water", bowl(player.level()).getWaterAmount(), "actual_aspects", aspects(bowl(player.level())));
                    retained(player);
                });
                next(Step.METAL_VERIFY);
            }
            case METAL_VERIFY -> {
                if (!serverDone() || ticks < 8) return false;
                next(Step.METAL_WORLD);
            }
            case METAL_WORLD -> {
                lookAt(client, Vec3.atCenterOf(crucible).add(0, .5, 0));
                next(Step.METAL_APPROACH);
                if (batch == 3) {
                    report.put("native_twelve_ingot_capture", Map.of("genuine_ingots_already_picked_up", earned, "genuine_final_physical_ingots", INGOTS[batch], "last_batch", snapshot));
                    capture.accept(CAPTURES.get(1));
                }
            }
            case METAL_APPROACH -> {
                onServer(client, () -> {
                    var player = serverPlayer(client); var drops = metalEntities(player.level());
                    if (!drops.isEmpty()) {
                        var actual = drops.getFirst();
                        require(actual.distanceToSqr(Vec3.atCenterOf(crucible).add(0, .5, 0)) < 4, "approach genuine floating output from the declared adjacent step");
                        // Player's real pickup box reaches the floating items
                        // from this cold step; do not repeatedly burn the player
                        // inside the bowl or heal/replace them between batches.
                        player.teleportTo(crucible.getX() - .4, crucible.getY() + 1, crucible.getZ() + .5);
                    }
                });
                next(Step.METAL_PICKUP);
            }
            case METAL_PICKUP -> {
                if (!serverDone() || ticks < 8) return false;
                if (count(client.player, ingot()) != earned + INGOTS[batch]) {
                    if (ticks >= 20) next(Step.METAL_APPROACH);
                    return false;
                }
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    require(count(player, ingot()) == earned + INGOTS[batch] && metalEntities(player.level()).isEmpty()
                            && metalDrops.stream().allMatch(uuid -> player.serverLevel().getEntity(uuid) == null), "actual physical batch ingots entered native inventory; no replacement outputs");
                    earned += INGOTS[batch];
                    var entry = new LinkedHashMap<String, Object>(snapshot);
                    entry.put("physical_pickup_server_verified", true);
                    batches.add(entry);
                    inspectCrucible(player, "batch " + (batch + 1) + " picked up", rodsSupplied, earned, 0);
                    viewCrucible(player);
                });
                next(Step.BATCH_FINISH);
            }
            case BATCH_FINISH -> {
                if (!serverDone() || ticks < 8) return false;
                if (++batch < INGOTS.length) { next(Step.BATCH_INPUTS); return false; }
                require(earned == 12 && rodsSupplied == 24 && count(client.player, ingot()) == 12, "all12 actually earned ingots precede any equipment/intermediate INPUT fixture");
                report.put("native_earned_metallurgy", Map.of("raw_rods_consumed", 24, "raw_iron_consumed", 12, "praecantatio_paid", 48, "physical_ingots_picked_up", 12,
                        "equipment_or_ingot_output_grants", 0, "backend_heat_water_aspect_counter_writes", 0));
                next(Step.BATCH_VERIFIED);
            }
            case BATCH_VERIFIED -> next(Step.PICK_CRAFT);
            case PICK_CRAFT -> beginCraft(client, new CraftPlan("thaumium_pickaxe", pickaxe(),
                    new Item[] {ingot(), ingot(), ingot(), null, Items.STICK, null, null, Items.STICK, null}, CAPTURES.get(2)), Step.CHEST_CRAFT);
            case CHEST_CRAFT -> beginCraft(client, new CraftPlan("thaumium_chestplate", chestplate(),
                    new Item[] {ingot(), null, ingot(), ingot(), ingot(), ingot(), ingot(), ingot(), ingot()}, null), Step.MINE_POSITION);
            case CRAFT_TRAVEL -> {
                if (!serverDone() || ticks < 10) return false;
                use(client, workbench, Direction.SOUTH, south(workbench));
                next(Step.CRAFT_OPEN);
            }
            case CRAFT_OPEN -> {
                if (ticks < 8 || !(client.screen instanceof GuiArcaneWorkbench)) return false;
                require(arcane(client).getCarried().isEmpty(), "ordinary craft starts with an empty real cursor");
                for (int slot = 1; slot <= 10; slot++) require(arcane(client).getSlot(slot).getItem().isEmpty(), "native workbench has no inserted wand or supplied grid/output");
                craftIngotsBefore = count(client.player, ingot());
                gridClicks = recipeClicks(client);
                gridIndex = 0;
                next(Step.CRAFT_GRID);
            }
            case CRAFT_GRID -> {
                if (ticks < 2) return false;
                if (gridIndex == gridClicks.size()) { next(Step.CRAFT_READY); return false; }
                int[] at = gridClicks.get(gridIndex++);
                click(client, at[0], at[1], ClickType.PICKUP);
                ticks = 0;
            }
            case CRAFT_READY -> {
                var menu = arcane(client);
                if (ticks < 8 || !menu.craftable() || !menu.getSlot(0).getItem().is(craft.output())) return false;
                require(menu.getSlot(0).getItem().getCount() == 1 && menu.getSlot(1).getItem().isEmpty() && menu.getCarried().isEmpty(), "one real ordinary output with no wand/cursor funding");
                verifyGrid(menu, true);
                for (String primal : WandData.PRIMALS) require(menu.requiredVis(primal) == 0, "native ordinary equipment menu requires zero " + primal);
                onServer(client, () -> {
                    var player = serverPlayer(client); var table = (TileArcaneWorkbench) player.level().getBlockEntity(workbench);
                    var match = ArcaneCrafting.find(player, table);
                    require(match != null && match.recipe().id().equals(id(craft.recipe())) && !match.arcane() && match.prices().isEmpty()
                            && match.output().is(craft.output()) && match.output().getCount() == 1 && table.getItem(TileArcaneWorkbench.WAND_SLOT).isEmpty(), "actual native RecipeManager ordinary fallback, no research/vis/wand craft gate");
                    retained(player);
                    snapshot = Map.of("recipe", match.recipe().id().toString(), "ordinary_native_fallback", true, "inserted_wands", 0,
                            "vis_price", match.prices(), "result_count", 1, "ingots_before_grid", craftIngotsBefore);
                });
                next(Step.CRAFT_PREVIEW);
            }
            case CRAFT_PREVIEW -> {
                if (!serverDone()) return false;
                next(Step.CRAFT_TAKE);
                if (craft.capture() != null) capture.accept(craft.capture());
            }
            case CRAFT_TAKE -> {
                click(client, 0, 0, ClickType.PICKUP);
                next(Step.CRAFT_OUTPUT);
            }
            case CRAFT_OUTPUT -> {
                if (ticks < 6 || !arcane(client).getCarried().is(craft.output())) return false;
                require(arcane(client).getCarried().getCount() == 1 && arcane(client).getCarried().getDamageValue() == 0, "native result extraction produces one new undamaged equipment item");
                verifyGrid(arcane(client), false);
                destination = emptyMain(client.player);
                next(Step.CRAFT_STORE);
            }
            case CRAFT_STORE -> {
                click(client, arcaneSlot(destination), 0, ClickType.PICKUP);
                next(Step.CRAFT_STORED);
            }
            case CRAFT_STORED -> {
                if (ticks < 6 || !arcane(client).getCarried().isEmpty() || count(client.player, craft.output()) != 1) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    int consumed = 0; for (Item item : craft.grid()) if (item == ingot()) consumed++;
                    require(count(player, ingot()) == craftIngotsBefore - consumed && ((TileArcaneWorkbench) player.level().getBlockEntity(workbench)).isEmpty()
                            && player.containerMenu.getCarried().isEmpty() && count(player, craft.output()) == 1, "native extraction consumes the exact ordinary grid and empties table/cursor");
                    if (craft.output() == chestplate()) craftedChest = player.getInventory().getItem(itemSlot(player, stack -> stack.is(chestplate()))).copy();
                    if (craft.output() == leggings()) craftedLegs = player.getInventory().getItem(itemSlot(player, stack -> stack.is(leggings()))).copy();
                    retained(player);
                    var entry = new LinkedHashMap<String, Object>(snapshot);
                    entry.put("actual_ingots_consumed", consumed);
                    entry.put("actual_ingots_remaining", count(player, ingot()));
                    entry.put("server_verified", true);
                    crafts.add(entry);
                });
                client.player.closeContainer();
                next(Step.CRAFT_VERIFIED);
            }
            case CRAFT_VERIFIED -> {
                if (!serverDone() || ticks < 8) return false;
                next(afterCraft);
            }
            case MINE_POSITION -> {
                if (!serverDone()) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client); BlockPos target = mineTarget();
                    require(count(player, ingot()) == 1 && count(player, pickaxe()) == 1 && count(player, chestplate()) == 1, "eleven earned ingots crafted pick/chest; the twelfth genuine ingot is reserved for repair");
                    require(player.level().getBlockState(target).is(mineIndex == 0 ? Blocks.STONE : Blocks.OBSIDIAN)
                            && player.level().getBlockState(target.below()).is(Blocks.STONE), "declared actual mining target has a bounded solid floor underneath");
                    require(!player.hasEffect(MobEffects.DIG_SPEED) && !player.hasEffect(MobEffects.DIG_SLOWDOWN), "native mining uses no haste/fatigue fixture");
                    player.teleportTo(target.getX() + .5, target.getY(), target.getZ() + 2.6);
                });
                next(Step.MINE_HAND);
            }
            case MINE_HAND -> {
                if (!serverDone()) return false;
                lookAt(client, south(mineTarget()));
                if (ticks < 10 || !hand(client, stack -> stack.is(pickaxe())) || !client.player.onGround()) return false;
                damageBefore = client.player.getMainHandItem().getDamageValue();
                require(damageBefore == mineIndex, "only previous genuine mining, never a damage setter, wore this pick");
                onServer(client, () -> {
                    var player = serverPlayer(client); var actual = player.getMainHandItem();
                    require(actual.is(pickaxe()) && actual.getMaxDamage() == 400 && actual.getDamageValue() == damageBefore
                            && actual.isCorrectToolForDrops(player.level().getBlockState(mineTarget())) && actual.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 7F
                            && actual.getDestroySpeed(Blocks.OBSIDIAN.defaultBlockState()) == 7F, "actual native400durability/speed7 pick correctly harvests the real stone/obsidian target");
                    require(actual.getItem() instanceof PickaxeItem pick && pick.getTier() == ThaumcraftEquipment.THAUMIUM_TIER && pick.getEnchantmentValue() == 22
                            && actual.getItem().isValidRepairItem(actual, new ItemStack(ingot())), "native Thaumium tier and actual ingot repair ingredient");
                    report.put("actual_pick_stats", Map.of("durability", actual.getMaxDamage(), "stone_speed", actual.getDestroySpeed(Blocks.STONE.defaultBlockState()),
                            "obsidian_correct_drops", actual.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "enchantability", actual.getItem().getEnchantmentValue(),
                            "native_mainhand_attack_damage", player.getAttributeValue(Attributes.ATTACK_DAMAGE), "repair_item", itemId(ingot())));
                    mineStarted = player.serverLevel().getGameTime();
                });
                next(Step.MINE_START);
            }
            case MINE_START -> {
                if (!serverDone() || ticks < 6) return false;
                lookAt(client, south(mineTarget()));
                require(!client.gameMode.hasInfiniteItems() && client.player.canInteractWithBlock(mineTarget(), 0), "genuine survival reach for mining");
                require(client.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(mineTarget()), "real first-person crosshair targets the declared block before held attack");
                client.mouseHandler.grabMouse();
                require(client.mouseHandler.isMouseGrabbed(), "native held mining needs the owned game window/camera grabbed, not a fabricated hit/progress field");
                client.options.keyAttack.setDown(true);
                require(client.gameMode.startDestroyBlock(mineTarget(), Direction.SOUTH), "native survival break starts on the real target");
                mineTicks = 0; mineCommitted = false;
                next(Step.MINE);
            }
            case MINE -> {
                ++mineTicks;
                lookAt(client, south(mineTarget()));
                // Minecraft.continueAttack drives its ONE native
                // continueDestroyBlock per tick from the real held key/ray.
                // A second manual call here would double mining progress; with
                // the key up, vanilla instead aborts a manual break each tick.
                if (!client.level.isEmptyBlock(mineTarget())) return false;
                client.options.keyAttack.setDown(false);
                client.gameMode.stopDestroyBlock();
                next(Step.MINE_COMMIT);
            }
            case MINE_COMMIT -> {
                if (!serverDone()) return false;
                if (!mineCommitted) {
                    onServer(client, () -> inspectMiningDrop(serverPlayer(client)));
                    return false;
                }
                if (ticks < 8 || client.player.getMainHandItem().getDamageValue() != damageBefore + 1) return false;
                if (rawEntities(client.level, mineOutput()).stream().noneMatch(entity -> entity.getUUID().equals(miningDrop))) return false;
                next(Step.MINE_WORLD);
            }
            case MINE_WORLD -> {
                lookAt(client, Vec3.atCenterOf(mineTarget()).add(0, -.2, 0));
                next(Step.MINE_APPROACH);
                if (mineIndex == 1) capture.accept(CAPTURES.get(3));
            }
            case MINE_APPROACH -> {
                onServer(client, () -> {
                    var player = serverPlayer(client); var entity = player.serverLevel().getEntity(miningDrop);
                    require(entity instanceof ItemEntity item && ItemStack.matches(item.getItem(), miningDropStack), "approach the SAME real native mining drop, not a replacement");
                    player.teleportTo(entity.getX(), entity.getY(), entity.getZ());
                });
                next(Step.MINE_PICKUP);
            }
            case MINE_PICKUP -> {
                if (!serverDone() || ticks < 8 || count(client.player, mineOutput()) != 1) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    require(player.serverLevel().getEntity(miningDrop) == null && count(player, mineOutput()) == 1
                            && player.getMainHandItem().getDamageValue() == damageBefore + 1, "native physical drop pickup preserves genuine tool wear");
                    retained(player);
                });
                next(Step.MINE_PICKED);
            }
            case MINE_PICKED -> {
                if (!serverDone()) return false;
                if (++mineIndex < 2) next(Step.MINE_POSITION); else next(Step.ANVIL_POSITION);
            }
            case ANVIL_POSITION -> {
                if (!serverDone()) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    wornPick = player.getMainHandItem().copy();
                    require(wornPick.is(pickaxe()) && wornPick.getDamageValue() == 2 && count(player, ingot()) == 1
                            && count(player, Items.OBSIDIAN) == 1 && count(player, Items.COBBLESTONE) == 1, "only actual stone/obsidian mining caused two wear; one genuine crucible ingot remains");
                    repairLevelsBefore = player.experienceLevel;
                    player.teleportTo(anvil.getX() + .5, anvil.getY(), anvil.getZ() + 2.6);
                });
                next(Step.ANVIL_OPEN);
            }
            case ANVIL_OPEN -> {
                if (!serverDone() || ticks < 10) return false;
                use(client, anvil, Direction.UP, Vec3.atCenterOf(anvil).add(0, .5, 0));
                next(Step.ANVIL_PICK);
            }
            case ANVIL_PICK -> {
                if (ticks < 8 || !(client.screen instanceof AnvilScreen)) return false;
                require(repairMenu(client).getCarried().isEmpty() && repairMenu(client).getSlot(0).getItem().isEmpty() && repairMenu(client).getSlot(1).getItem().isEmpty(), "empty native anvil fixture, never a supplied repaired pick");
                click(client, anvilSlot(itemSlot(client.player, stack -> stack.is(pickaxe()))), 0, ClickType.QUICK_MOVE);
                next(Step.ANVIL_INGOT);
            }
            case ANVIL_INGOT -> {
                if (ticks < 8 || !ItemStack.matches(repairMenu(client).getSlot(0).getItem(), wornPick)) return false;
                int slot = itemSlot(client.player, stack -> stack.is(ingot()));
                require(client.player.getInventory().getItem(slot).getCount() == 1, "the twelfth genuinely earned ingot is the only repair material");
                click(client, anvilSlot(slot), 0, ClickType.QUICK_MOVE);
                next(Step.ANVIL_READY);
            }
            case ANVIL_READY -> {
                var menu = repairMenu(client); var result = menu.getSlot(2).getItem();
                if (ticks < 8 || !result.is(pickaxe()) || result.getDamageValue() != 0 || menu.getCost() <= 0) return false;
                repairCost = menu.getCost();
                require(repairCost == 1 && menu.getSlot(1).getItem().is(ingot()) && menu.getSlot(1).getItem().getCount() == 1, "native one-material repair costs one level for actual two wear");
                onServer(client, () -> {
                    var player = serverPlayer(client); var actual = (AnvilMenu) player.containerMenu;
                    require(ItemStack.matches(actual.getSlot(0).getItem(), wornPick) && actual.getSlot(1).getItem().is(ingot()) && actual.getSlot(1).getItem().getCount() == 1
                            && actual.getSlot(2).getItem().is(pickaxe()) && actual.getSlot(2).getItem().getDamageValue() == 0 && actual.getCost() == repairCost
                            && player.experienceLevel == repairLevelsBefore && count(player, pickaxe()) == 0 && count(player, ingot()) == 0, "authoritative native anvil preview owns the actual worn pick/last earned ingot; extraction has not yet paid XP");
                    snapshot = Map.of("native_menu", "minecraft:anvil", "actual_input_snbt", wornPick.save(player.registryAccess()).toString(), "damage_before", wornPick.getDamageValue(),
                            "preview_damage", actual.getSlot(2).getItem().getDamageValue(), "preview_repair_cost_component", actual.getSlot(2).getItem().getOrDefault(DataComponents.REPAIR_COST, 0),
                            "earned_ingot_input", 1, "level_cost", repairCost, "levels_before", repairLevelsBefore, "large_damage25percent_boundary_claimed", false);
                    retained(player);
                });
                next(Step.ANVIL_PREVIEW);
            }
            case ANVIL_PREVIEW -> {
                if (!serverDone()) return false;
                report.put("native_anvil_preview", snapshot);
                next(Step.ANVIL_TAKE);
                capture.accept(CAPTURES.get(4));
            }
            case ANVIL_TAKE -> {
                click(client, 2, 0, ClickType.PICKUP);
                next(Step.ANVIL_OUTPUT);
            }
            case ANVIL_OUTPUT -> {
                if (ticks < 8 || !repairMenu(client).getCarried().is(pickaxe())) return false;
                require(repairMenu(client).getCarried().getDamageValue() == 0 && repairMenu(client).getCarried().getOrDefault(DataComponents.REPAIR_COST, 0) == 1
                        && repairMenu(client).getSlot(0).getItem().isEmpty() && repairMenu(client).getSlot(1).getItem().isEmpty(), "native result click, not a damage setter, consumes the material and repairs the actual pick");
                destination = emptyMain(client.player);
                next(Step.ANVIL_STORE);
            }
            case ANVIL_STORE -> {
                click(client, anvilSlot(destination), 0, ClickType.PICKUP);
                next(Step.ANVIL_STORED);
            }
            case ANVIL_STORED -> {
                if (ticks < 8 || !repairMenu(client).getCarried().isEmpty() || count(client.player, pickaxe()) != 1) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client); var actual = player.getInventory().getItem(itemSlot(player, stack -> stack.is(pickaxe())));
                    require(actual.getDamageValue() == 0 && actual.getOrDefault(DataComponents.REPAIR_COST, 0) == 1 && player.experienceLevel == repairLevelsBefore - repairCost
                            && count(player, ingot()) == 0 && player.containerMenu.getCarried().isEmpty(), "real anvil extraction paid one earned ingot and the exact native XP cost");
                    for (int slot = 0; slot <= 2; slot++) require(player.containerMenu.getSlot(slot).getItem().isEmpty(), "real anvil inputs/result are empty after extraction");
                    retained(player);
                    report.put("native_anvil_repair", Map.of("server_verified", true, "damage_from_native_mining", 2, "damage_after_native_result", actual.getDamageValue(),
                            "earned_ingots_spent", 1, "levels_paid", repairCost, "levels_after", player.experienceLevel, "actual_output_snbt", actual.save(player.registryAccess()).toString()));
                });
                client.player.closeContainer();
                next(Step.ANVIL_VERIFIED);
            }
            case ANVIL_VERIFIED -> {
                if (!serverDone() || ticks < 8) return false;
                next(Step.LEGS_INPUT);
            }
            case LEGS_INPUT -> {
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    require(count(player, ingot()) == 0 && count(player, leggings()) == 0 && player.containerMenu == player.inventoryMenu, "all12 genuine ingots have already funded pick3+chest8+repair1 BEFORE unrelated leggings INPUT supply");
                    supply(player, ingot(), 7, "DECLARED INTERMEDIATE INPUT solely for ordinary leggings/layer2; NOT earned by this crucible");
                    player.inventoryMenu.broadcastChanges();
                });
                next(Step.LEGS_CRAFT);
            }
            case LEGS_CRAFT -> {
                if (!serverDone()) return false;
                beginCraft(client, new CraftPlan("thaumium_leggings", leggings(),
                        new Item[] {ingot(), ingot(), ingot(), ingot(), null, ingot(), ingot(), null, ingot()}, null), Step.EQUIP_CHEST);
            }
            case EQUIP_CHEST -> {
                require(client.player.containerMenu == client.player.inventoryMenu && client.player.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "native chest armor slot is genuinely empty");
                click(client, inventorySlot(itemSlot(client.player, stack -> stack.is(chestplate()))), 0, ClickType.QUICK_MOVE);
                next(Step.EQUIP_CHEST_WAIT);
            }
            case EQUIP_CHEST_WAIT -> {
                if (ticks < 8 || !ItemStack.matches(client.player.getItemBySlot(EquipmentSlot.CHEST), craftedChest)) return false;
                next(Step.EQUIP_LEGS);
            }
            case EQUIP_LEGS -> {
                require(client.player.getItemBySlot(EquipmentSlot.LEGS).isEmpty(), "native leggings slot is genuinely empty");
                click(client, inventorySlot(itemSlot(client.player, stack -> stack.is(leggings()))), 0, ClickType.QUICK_MOVE);
                next(Step.EQUIP_LEGS_WAIT);
            }
            case EQUIP_LEGS_WAIT -> {
                if (ticks < 8 || !ItemStack.matches(client.player.getItemBySlot(EquipmentSlot.LEGS), craftedLegs) || !hand(client, stack -> stack.is(pickaxe()))) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client); var chest = player.getItemBySlot(EquipmentSlot.CHEST); var legs = player.getItemBySlot(EquipmentSlot.LEGS);
                    require(ItemStack.matches(chest, craftedChest) && ItemStack.matches(legs, craftedLegs) && count(player, chestplate()) == 0 && count(player, leggings()) == 0
                            && count(player, ingot()) == 0 && player.getAttributeValue(Attributes.ARMOR) == 11 && player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 0,
                            "real inventory QUICK_MOVE equips both crafted native items and supplies armor6+5, no shield/toughness ability");
                    require(chest.getItem() instanceof ArmorItem armor && armor.getDefense() == 6 && armor.getEnchantmentValue() == 25 && chest.getMaxDamage() == 400
                            && legs.getItem() instanceof ArmorItem legArmor && legArmor.getDefense() == 5 && legArmor.getEnchantmentValue() == 25 && legs.getMaxDamage() == 375,
                            "actual native armor material/slot stats, not catalog-only placeholders");
                    require(player.getMainHandItem().is(pickaxe()) && player.getMainHandItem().getDamageValue() == 0 && count(player, Items.OBSIDIAN) == 1 && count(player, Items.COBBLESTONE) == 1,
                            "repaired genuine pick and both genuine mining outputs survive equip");
                    require(count(player, Items.BUCKET) == 1 && count(player, Items.WATER_BUCKET) == 0 && count(player, Items.STICK) == 0
                            && count(player, Items.BLAZE_ROD) == 0 && count(player, Items.IRON_INGOT) == 0, "declared finite raw inputs are consumed and the native empty bucket remainder is retained");
                    inspectCrucible(player, "final retained residues (native decay included)", rodsSupplied, earned, 0);
                    retained(player);
                    report.put("native_equipped_armor", Map.of("action", "native inventory QUICK_MOVE, no setItemSlot", "chest_snbt", chest.save(player.registryAccess()).toString(),
                            "leggings_snbt", legs.save(player.registryAccess()).toString(), "actual_armor_attribute", player.getAttributeValue(Attributes.ARMOR),
                            "actual_toughness_attribute", player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), "chest_ingots_genuinely_earned", 8, "leggings_ingots_declared_intermediate_input", 7,
                            "incoming_damage_tested_in_this_client_story", false));
                    report.put("native_net_outputs", Map.of("earned_crucible_ingots", 12, "earned_ingots_used_by_pick", 3, "earned_ingots_used_by_chest", 8,
                            "earned_ingots_used_by_anvil", 1, "separate_intermediate_input_ingots_used_by_leggings", 7,
                            "native_repaired_pickaxes", 1, "native_equipped_chestplates", 1, "native_equipped_leggings", 1, "actual_mining_drops", Map.of("obsidian", 1, "cobblestone", 1), "returned_buckets", 1));
                    player.teleportTo(workbench.getX() + 4.5, workbench.getY(), workbench.getZ() + 2.5);
                });
                next(Step.EQUIPPED_VERIFY);
            }
            case EQUIPPED_VERIFY -> {
                if (!serverDone() || ticks < 10) return false;
                report.put("native_item_models", models(client));
                report.put("native_armor_render_inputs", armorLayers(client));
                client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                next(Step.ARMOR_POSITION);
            }
            case ARMOR_POSITION -> {
                faceArmorCamera(client);
                if (ticks < 15) return false;
                next(Step.ARMOR_WORLD);
            }
            case ARMOR_WORLD -> {
                faceArmorCamera(client);
                require(client.options.getCameraType() == CameraType.THIRD_PERSON_FRONT && client.player.getItemBySlot(EquipmentSlot.CHEST).is(chestplate())
                        && client.player.getItemBySlot(EquipmentSlot.LEGS).is(leggings()) && client.player.getMainHandItem().is(pickaxe()), "final native third-person scene uses actual equipped chest/legs and the repaired earned pick");
                report.put("native_client_ticks", totalTicks);
                report.put("armor_capture", Map.of("capture", CAPTURES.get(5), "camera", "THIRD_PERSON_FRONT", "actual_equipped_slots", List.of("chest", "legs"),
                        "both_layer_visual_inspection", "pending root inspection of exact capture; not inferred from resource existence"));
                next(Step.DONE);
                capture.accept(CAPTURES.get(5));
            }
            case DONE -> {
                client.options.setCameraType(previousCamera);
                return true;
            }
        }
        return false;
    }

    private void prepare(ServerPlayer player) {
        var level = player.serverLevel();
        require(workbench.getY() == crucible.getY() && workbench.getZ() == crucible.getZ() && crucible.getX() - workbench.getX() == 16,
                "root supplies fresh disjoint research+224/+240 origins, not old nodes/machines");
        require(player.getInventory().getItem(8).isEmpty() && ItemStack.matches(player.getInventory().getItem(handoffSlot), handoff), "native Silverwood handoff committed before any fixture supply");
        newWandSlot = handoffSlot;
        newWand = player.getInventory().getItem(newWandSlot).copy();
        oldWand = player.getInventory().getItem(7).copy();
        apple = player.getOffhandItem().copy();
        require(oldWand.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(oldWand).rod().equals("greatwood") && ItemWandCasting.data(oldWand).cap().equals("gold")
                && isSilverwood(newWand) && ItemWandCasting.getVis(newWand, "aer") >= 6000 && count(player, ThaumcraftContent.WAND.get()) == 2,
                "both actual earned wands/capacity/charged components are retained, not reconstructed/top-upped");
        require(player.getInventory().getItem(6).is(ThaumcraftContent.THAUMONOMICON.get()) && apple.is(Items.APPLE) && apple.getCount() == 1
                && count(player, ThaumcraftContent.EMPTY_PHIAL.get()) == 4 && count(player, ThaumcraftTubes.ESSENTIA_TUBE_ITEM.get()) == 6
                && count(player, ThaumcraftContent.MATERIALS.get("quicksilver_drop").get()) == 8 && count(player, ThaumcraftFoci.FIRE_FOCUS.get()) == 1
                && count(player, Items.BLAZE_POWDER) == 1 && count(player, ThaumcraftInfusion.PEDESTAL_ITEM.get()) == 1, "accepted M07/M06 inventory outputs reach this story intact");
        for (ItemStack equipped : player.getArmorSlots()) require(equipped.isEmpty(), "never overwrite prior equipped armor");
        for (ItemStack stack : player.getInventory().items) if (!stack.isEmpty()) retainedStacks.add(stack.copy());
        for (Item item : List.of(Items.BLAZE_ROD, Items.IRON_INGOT, Items.STICK, Items.WATER_BUCKET, Items.BUCKET, Items.OBSIDIAN, Items.COBBLESTONE, ingot()))
            require(count(player, item) == 0, "new INPUT/mining output cannot consume or mask an earlier retained stack: " + itemId(item));
        for (Item item : equipment()) require(count(player, item) == 0, "no equipment outputs are supplied or inherited as a false craft");
        var oven = (TileAlchemyFurnace) level.getBlockEntity(oldOven);
        require(oven != null && oven.getItem(0).isEmpty() && oven.getVis() == 0 && oven.getItem(1).is(Items.COAL) && oven.getItem(1).getCount() == 5,
                "accepted M07 old oven retains its five actual unused coal");
        oldFuel = oven.getItem(1).copy();
        for (int i = 0; i < 8; i++) {
            BlockPos at = workbench.offset(-55 + i * 2, 0, -8);
            require(level.getBlockEntity(at) instanceof TileJar, "accepted M07 actual unused-essentia jars remain outside new patches");
            retainedJars.put(at, jarState((TileJar) level.getBlockEntity(at)));
        }
        require(retainedJars.values().stream().mapToInt(value -> (Integer) value.get("amount")).sum() == 129, "all129actual M07 unused essentia are retained, not used as new magic INPUT");
        patch(level, workbench.offset(-2, -2, -4), workbench.offset(7, 4, 7), workbench.getY());
        patch(level, crucible.offset(-3, -2, -3), crucible.offset(3, 4, 3), crucible.getY());
        level.setBlockAndUpdate(workbench, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState());
        level.setBlockAndUpdate(anvil, Blocks.ANVIL.defaultBlockState());
        level.setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(obsidian, Blocks.OBSIDIAN.defaultBlockState());
        level.setBlockAndUpdate(crucible.west(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(crucible.below(), ThaumcraftResources.NITOR.get().defaultBlockState());
        level.setBlockAndUpdate(crucible, Blocks.CAULDRON.defaultBlockState());
        var rod = ObjectAspectResolver.get(new ItemStack(Items.BLAZE_ROD), level);
        require(rod.visSize() == 6 && rod.getAmount(Aspect.FIRE) == 4 && rod.getAmount(Aspect.MAGIC) == 2, "real resolver establishes original4ignis+2praecantatio per raw blaze rod");
        player.setGameMode(GameType.SURVIVAL);
        player.setShiftKeyDown(false);
        supply(player, Items.WATER_BUCKET, 1, "native crucible water INPUT; returned bucket retained");
        supply(player, Items.STICK, 2, "ordinary pickaxe raw INPUT");
        levelsInputBefore = player.experienceLevel;
        player.giveExperienceLevels(30);
        inputs.add(Map.of("fixture", "additional XP levels INPUT", "added_levels", 30, "levels_before", levelsInputBefore, "levels_after", player.experienceLevel));
        KnowledgeAccess.get(player).addResearch("THAUMIUM");
        KnowledgeEvents.sync(player);
        player.inventoryMenu.broadcastChanges();
        viewCrucible(player);
        report.put("retained_wands_before", Map.of("old_wand_snbt", oldWand.save(player.registryAccess()).toString(), "new100vis_wand_snbt", newWand.save(player.registryAccess()).toString(),
                "old_vis_centivis", ItemWandCasting.data(oldWand).vis(), "new_vis_centivis", ItemWandCasting.data(newWand).vis(), "new_inventory_slot", newWandSlot));
    }

    private static void patch(ServerLevel level, BlockPos min, BlockPos max, int floorY) {
        for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++) for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++) level.getChunk(x, z);
        require(level.getEntitiesOfClass(ItemEntity.class, new AABB(Vec3.atLowerCornerOf(min), Vec3.atLowerCornerOf(max.offset(1, 1, 1)))).isEmpty(), "fresh bounded patch must not erase other physical outputs");
        for (BlockPos at : BlockPos.betweenClosed(min, max)) require(level.getBlockEntity(at) == null
                && !BuiltInRegistries.BLOCK.getKey(level.getBlockState(at).getBlock()).getNamespace().equals("thaumcraft"), "refuse to erase ANY old BE/node/machine/Thaumcraft block at " + at);
        for (BlockPos at : BlockPos.betweenClosed(min, max)) level.setBlockAndUpdate(at, at.getY() < floorY ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
    }

    private void supply(ServerPlayer player, Item item, int amount, String purpose) {
        int slot = emptyMain(player);
        require(amount > 0 && amount <= item.getDefaultInstance().getMaxStackSize(), "finite declared INPUT stack");
        player.getInventory().setItem(slot, new ItemStack(item, amount));
        inputs.add(Map.of("item", itemId(item), "count", amount, "purpose", purpose, "genuinely_empty_main_slot", slot));
    }

    /** Read-only budget check. Natural primal decay is real loss, never silently treated as unchanged96ignis/400water. */
    private void inspectCrucible(ServerPlayer player, String boundary, int rawRods, int paidIngots, int magic) {
        var tile = bowl(player.level());
        int waterLossBeyondCrafts = 1000 - paidIngots * 50 - tile.getWaterAmount();
        var state = new LinkedHashMap<String, Object>();
        state.put("boundary", boundary); state.put("native_game_time", player.serverLevel().getGameTime());
        state.put("raw_rods_dissolved", rawRods); state.put("actual_paid_ingots", paidIngots); state.put("actual_water", tile.getWaterAmount());
        state.put("actual_heat", tile.getHeat()); state.put("actual_aspects", aspects(tile)); state.put("water_loss_beyond_native_crafts", waterLossBeyondCrafts);
        crucibleStates.add(state); // Keep the real state even when a late catalyst/compound decay makes this attempt fail closed.
        require(waterLossBeyondCrafts >= 0 && waterLossBeyondCrafts % 2 == 0, "actual native water budget is50/craft plus2/decay: " + state);
        int decays = waterLossBeyondCrafts / 2;
        state.put("accounted_native_primal_decay_events", decays);
        require(tile.getAspects().getAmount(Aspect.MAGIC) == magic && tile.getAspects().getAmount(Aspect.FIRE) == rawRods * 4 - decays
                && tile.tagAmount() == rawRods * 4 - decays + magic && tile.tagAmount() <= TileCrucible.MAX_TAGS,
                "native finite crucible budget; compound decay/late catalyst fails without injected repairs: " + state);
    }

    private void acknowledgeDrop(Minecraft client) {
        onServer(client, () -> {
            var player = serverPlayer(client);
            // A server Runnable can beat a C2S drop. Move the CAMERA only after
            // the server actually emptied that hand, never move an item/result.
            if (!player.getInventory().getItem(8).isEmpty()) return;
            dropCommitted = true;
            viewCrucible(player);
        });
    }

    private void standForDrop(Minecraft client) { onServer(client, () -> serverPlayer(client).teleportTo(crucible.getX() - .4, crucible.getY() + 1, crucible.getZ() + .5)); }
    private void viewCrucible(ServerPlayer player) { player.teleportTo(crucible.getX() - 1.5, crucible.getY(), crucible.getZ() + 1.5); }

    private void beginCraft(Minecraft client, CraftPlan plan, Step following) {
        require(serverDone(), "ordinary craft cannot overlap a server inspection/input fixture");
        client.setScreen(null);
        craft = plan; afterCraft = following;
        onServer(client, () -> serverPlayer(client).teleportTo(workbench.getX() + .5, workbench.getY(), workbench.getZ() + 2.6));
        next(Step.CRAFT_TRAVEL);
    }

    /** Only literal player menu clicks; actual recipe lookup/payment/output remains in the native container. */
    private List<int[]> recipeClicks(Minecraft client) {
        var clicks = new ArrayList<int[]>();
        var groups = new LinkedHashMap<Item, List<Integer>>();
        for (int i = 0; i < 9; i++) if (craft.grid()[i] != null) groups.computeIfAbsent(craft.grid()[i], ignored -> new ArrayList<>()).add(i + 2);
        for (var group : groups.entrySet()) {
            int needed = group.getValue().size();
            int inventory = itemSlot(client.player, stack -> stack.is(group.getKey()) && stack.getCount() >= needed);
            int source = arcaneSlot(inventory);
            clicks.add(new int[] {source, 0});
            for (int cell : group.getValue()) clicks.add(new int[] {cell, 1});
            if (client.player.getInventory().getItem(inventory).getCount() > needed) clicks.add(new int[] {source, 0});
        }
        return clicks;
    }

    private void verifyGrid(ContainerArcaneWorkbench menu, boolean populated) {
        require(menu.getSlot(1).getItem().isEmpty(), "no inserted wand in an ordinary equipment craft");
        for (int i = 0; i < 9; i++) {
            Item expected = craft.grid()[i]; ItemStack actual = menu.getSlot(i + 2).getItem();
            require(!populated || expected == null ? actual.isEmpty() : actual.is(expected) && actual.getCount() == 1, "actual native ordinary grid cell " + i + " for " + craft.recipe());
        }
    }

    private void inspectMiningDrop(ServerPlayer player) {
        BlockPos target = mineTarget();
        if (!player.level().isEmptyBlock(target) || player.getMainHandItem().getDamageValue() != damageBefore + 1) return;
        var drops = rawEntities(player.level(), mineOutput());
        require(drops.size() == 1 && drops.getFirst().getItem().getCount() == 1 && count(player, mineOutput()) == 0, "native correct-tool survival break creates exactly one real mining drop before pickup");
        var actual = drops.getFirst();
        require(actual.distanceToSqr(Vec3.atCenterOf(target)) < 9, "native mining drop belongs to the real target");
        miningDrop = actual.getUUID(); miningDropStack = actual.getItem().copy(); mineCommitted = true;
        require(mineIndex == 0 || mineTicks >= 200 && player.serverLevel().getGameTime() - mineStarted >= 200,
                "obsidian was genuinely mined over actual client/server time, not gamemode instant-break");
        mining.add(Map.of("target", target.toShortString(), "input_target_fixture", mineIndex == 0 ? "minecraft:stone" : "minecraft:obsidian", "output_item", itemId(mineOutput()),
                "native_drop_uuid", miningDrop.toString(), "native_drop_snbt", miningDropStack.save(player.registryAccess()).toString(), "client_continue_ticks", mineTicks,
                "elapsed_server_ticks_since_aim", player.serverLevel().getGameTime() - mineStarted, "damage_before", damageBefore, "damage_after", player.getMainHandItem().getDamageValue(), "server_verified", true));
    }

    private void retained(Player player) {
        require(ItemStack.matches(player.getInventory().getItem(7), oldWand) && ItemStack.matches(player.getInventory().getItem(newWandSlot), newWand)
                && ItemStack.matches(player.getOffhandItem(), apple), "both actual wand components/vis and offhand apple survive unchanged, without top-ups");
        for (ItemStack original : retainedStacks) {
            int before = retainedStacks.stream().filter(stack -> ItemStack.isSameItemSameComponents(stack, original)).mapToInt(ItemStack::getCount).sum();
            int after = player.getInventory().items.stream().filter(stack -> ItemStack.isSameItemSameComponents(stack, original)).mapToInt(ItemStack::getCount).sum();
            require(after >= before, "retained prior inventory counts/components not overwritten: " + original);
        }
        require(ItemStack.matches(((TileAlchemyFurnace) player.level().getBlockEntity(oldOven)).getItem(1), oldFuel), "old oven's five unused actual coal were not overwritten/consumed");
        for (var jar : retainedJars.entrySet()) require(player.level().getBlockEntity(jar.getKey()) instanceof TileJar actual && jarState(actual).equals(jar.getValue()), "all actual129old essentia/filter states remain outside the new story: " + jar.getKey());
        report.put("retained_prior_outputs", Map.of("server_verified", true, "original_main_inventory_stacks", retainedStacks.size(), "both_wands_components_and_vis_unchanged", true,
                "silverwood_aer_centivis", ItemWandCasting.getVis(player.getInventory().getItem(newWandSlot), "aer"), "old_oven_unused_coal", oldFuel.getCount(), "old_jar_essentia", 129));
    }

    private boolean showRecipe(Minecraft client) throws ReflectiveOperationException {
        int page = (int) BookClientSmoke.field(client.screen, "page");
        var pages = (List<?>) BookClientSmoke.field(client.screen, "pages");
        for (int i = page; i < Math.min(page + 2, pages.size()); i++) {
            Object value = BookClientSmoke.field(pages.get(i), "recipe");
            if (value instanceof ThaumcraftRecipeDisplay display && display.recipe().id().equals(id("thaumium_pickaxe"))) {
                require(display.recipe() == client.level.getRecipeManager().byKey(id("thaumium_pickaxe")).orElseThrow(), "book uses the actual synced ordinary equipment RecipeHolder");
                return true;
            }
        }
        require(++turns <= 24, "bounded native THAUMIUM equipment book navigation");
        var button = client.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(widget -> widget.active && widget.getMessage().getString().equals("→")).findFirst().orElseThrow();
        client.screen.mouseClicked(button.getX() + 5, button.getY() + 5, 0);
        client.screen.mouseReleased(button.getX() + 5, button.getY() + 5, 0);
        ticks = 0;
        return false;
    }

    private List<Map<String, Object>> models(Minecraft client) {
        String[] originalSprites = {"thaumiumsword", "thaumiumpick", "thaumiumaxe", "thaumiumshovel", "thaumiumhoe", "thaumiumhelm", "thaumiumchest", "thaumiumlegs", "thaumiumboots"};
        var result = new ArrayList<Map<String, Object>>();
        var equipment = equipment();
        for (int i = 0; i < equipment.size(); i++) {
            Item item = equipment.get(i);
            // Model-query prototypes only: never put these nine stacks into the world/inventory.
            var model = client.getItemRenderer().getModel(new ItemStack(item), client.level, client.player, 0);
            require(model != client.getModelManager().getMissingModel(), "native item model is not missing: " + itemId(item));
            var sprites = new LinkedHashSet<String>();
            for (var quad : model.getQuads(null, null, RandomSource.create(0))) sprites.add(quad.getSprite().contents().name().toString());
            for (Direction face : Direction.values()) for (var quad : model.getQuads(null, face, RandomSource.create(0))) sprites.add(quad.getSprite().contents().name().toString());
            require(sprites.contains("thaumcraft:items/" + originalSprites[i]) && sprites.stream().noneMatch(sprite -> sprite.contains("missingno")), "native item selects its original sprite, not another equipment/fallback model: " + itemId(item));
            result.add(Map.of("item", itemId(item), "actual_sprites", List.copyOf(sprites), "catalog_model_query_only_not_a_craft_claim", true));
        }
        return result;
    }

    private List<Map<String, Object>> armorLayers(Minecraft client) {
        var result = new ArrayList<Map<String, Object>>();
        for (EquipmentSlot slot : List.of(EquipmentSlot.CHEST, EquipmentSlot.LEGS)) {
            var stack = client.player.getItemBySlot(slot);
            require(stack.getItem() instanceof ArmorItem, "actual equipped native armor item for layer observation");
            var armor = (ArmorItem) stack.getItem(); var layers = armor.getMaterial().value().layers();
            require(layers.size() == 1, "one original native material layer per equipped item");
            boolean inner = slot == EquipmentSlot.LEGS;
            var texture = layers.getFirst().texture(inner);
            require(texture.equals(id("textures/models/armor/thaumium_layer_" + (inner ? 2 : 1) + ".png")) && client.getResourceManager().getResource(texture).isPresent(), "native biped resolves the actual equipped original outer/inner texture, not a missing material layer");
            result.add(Map.of("equipped_slot", slot.getName(), "actual_item", itemId(stack.getItem()), "native_inner_model", inner, "native_texture", texture.toString(),
                    "resource_present", true, "visual_inspection", "pending exact66 screenshot; resolution is not a seen-layer claim"));
        }
        return result;
    }

    private boolean hand(Minecraft client, Predicate<ItemStack> wanted) throws ReflectiveOperationException {
        require(client.player.containerMenu == client.player.inventoryMenu, "native hand SWAP only outside machine menus");
        if (!wanted.test(client.player.getInventory().getItem(8))) {
            int slot = itemSlot(client.player, wanted);
            require(slot != 6 && slot != 7 && slot != newWandSlot, "action hand never displaces either earned wand or the book");
            click(client, inventorySlot(slot), 8, ClickType.SWAP);
            handChangedAt = client.player.tickCount;
            return false;
        }
        if (client.player.tickCount - handChangedAt < 6) return false;
        select(client, 8);
        return true;
    }

    private static void faceArmorCamera(Minecraft client) {
        // The front camera's entire four-block boom stays inside the fresh
        // bounded patch, rather than clipping against uncleared outside terrain.
        client.player.setYRot(0); client.player.yRotO = 0;
        client.player.setXRot(0); client.player.xRotO = 0;
        client.player.yBodyRot = 0; client.player.yBodyRotO = 0;
        client.player.setYHeadRot(0); client.player.yHeadRotO = 0;
    }
    private static Map<String, Object> jarState(TileJar jar) { return Map.of("amount", jar.getAmount(), "aspect", jar.getAspect() == null ? "" : jar.getAspect().getTag(), "filter", jar.getFilter() == null ? "" : jar.getFilter().getTag()); }
    private static Map<String, Integer> aspects(TileCrucible tile) { var result = new LinkedHashMap<String, Integer>(); var tags = tile.getAspects(); for (Aspect aspect : tags.getAspects()) if (tags.getAmount(aspect) > 0) result.put(aspect.getTag(), tags.getAmount(aspect)); return result; }
    private List<EntitySpecialItem> metalEntities(Level level) { return level.getEntitiesOfClass(EntitySpecialItem.class, new AABB(crucible).inflate(4), entity -> entity.getItem().is(ingot())); }
    private List<ItemEntity> rawEntities(Level level, Item item) { return level.getEntitiesOfClass(ItemEntity.class, new AABB(item == Items.BLAZE_ROD || item == Items.IRON_INGOT ? crucible : mineTarget()).inflate(4), entity -> entity.getItem().is(item)); }
    private TileCrucible bowl(Level level) { return (TileCrucible) level.getBlockEntity(crucible); }
    private BlockPos mineTarget() { return mineIndex == 0 ? stone : obsidian; }
    private Item mineOutput() { return mineIndex == 0 ? Items.COBBLESTONE : Items.OBSIDIAN; }
    private static List<Item> equipment() { return List.of(ThaumcraftEquipment.THAUMIUM_SWORD.get(), pickaxe(), ThaumcraftEquipment.THAUMIUM_AXE.get(), ThaumcraftEquipment.THAUMIUM_SHOVEL.get(), ThaumcraftEquipment.THAUMIUM_HOE.get(), ThaumcraftEquipment.THAUMIUM_HELMET.get(), chestplate(), leggings(), ThaumcraftEquipment.THAUMIUM_BOOTS.get()); }
    private static Item ingot() { return ThaumcraftContent.MATERIALS.get("thaumium_ingot").get(); }
    private static Item pickaxe() { return ThaumcraftEquipment.THAUMIUM_PICKAXE.get(); }
    private static Item chestplate() { return ThaumcraftEquipment.THAUMIUM_CHESTPLATE.get(); }
    private static Item leggings() { return ThaumcraftEquipment.THAUMIUM_LEGGINGS.get(); }
    private static boolean isSilverwood(ItemStack stack) { return stack.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(stack).rod().equals("silverwood") && ItemWandCasting.data(stack).cap().equals("gold") && ItemWandCasting.getMaxVis(stack) == 10000; }
    private static int emptyMain(Player player) { for (int slot = 9; slot < 36; slot++) if (player.getInventory().getItem(slot).isEmpty()) return slot; throw new IllegalStateException("No genuinely empty native main-inventory slot"); }
    private static int itemSlot(Player player, Predicate<ItemStack> wanted) { for (int slot = 0; slot < 36; slot++) if (wanted.test(player.getInventory().getItem(slot))) return slot; throw new IllegalStateException("Native Thaumium story lacks its actual stage input/output"); }
    private static int count(Player player, Item item) { return player.getInventory().items.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum(); }
    private static int inventorySlot(int slot) { return slot < 9 ? slot + 36 : slot; }
    private static int arcaneSlot(int slot) { return slot < 9 ? slot + 38 : slot + 2; }
    private static int anvilSlot(int slot) { return slot < 9 ? slot + 30 : slot - 6; }
    private static ContainerArcaneWorkbench arcane(Minecraft client) { return (ContainerArcaneWorkbench) client.player.containerMenu; }
    private static AnvilMenu repairMenu(Minecraft client) { return (AnvilMenu) client.player.containerMenu; }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
    private static String itemId(Item item) { return BuiltInRegistries.ITEM.getKey(item).toString(); }
    private static Vec3 south(BlockPos pos) { return Vec3.atCenterOf(pos).add(0, 0, .5); }
    private static void click(Minecraft client, int slot, int button, ClickType type) { client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, slot, button, type, client.player); }
    private static void select(Minecraft client, int slot) throws ReflectiveOperationException { client.player.getInventory().selected = slot; var ensure = client.gameMode.getClass().getDeclaredMethod("ensureHasSentCarriedItem"); ensure.setAccessible(true); ensure.invoke(client.gameMode); }
    private static void use(Minecraft client, BlockPos pos, Direction face, Vec3 point) { require(!client.gameMode.hasInfiniteItems() && client.player.canInteractWithBlock(pos, 0), "native survival block reach for " + pos); lookAt(client, point); require(client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(point, face, pos, false)).consumesAction(), "native useItemOn accepts " + pos + "/" + face); }
    private static void aimDrop(Minecraft client) {
        // The native throw starts above a solid, cold west step. Its real
        // eastward/downward velocity clears the edge into the bowl; no moving
        // ItemEntity, hovering player or crucible-contact healing fixture.
        client.player.setYRot(-90); client.player.yRotO = -90;
        client.player.setXRot(65); client.player.xRotO = 65;
    }
    private static void lookAt(Minecraft client, Vec3 point) { Vec3 delta = point.subtract(client.player.getEyePosition()); client.player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z))); client.player.yRotO = client.player.getYRot(); client.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)))); client.player.xRotO = client.player.getXRot(); }
    private static ServerPlayer serverPlayer(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private void onServer(Minecraft client, Runnable action) { require(serverAction == null || serverAction.isDone(), "Thaumium fixture/read-only server actions never overlap"); serverAction = CompletableFuture.runAsync(action, client.getSingleplayerServer()); }
    private boolean serverDone() { if (serverAction == null) return true; if (!serverAction.isDone()) return false; serverAction.join(); return true; }
    private void next(Step next) { step = next; ticks = 0; }
}
