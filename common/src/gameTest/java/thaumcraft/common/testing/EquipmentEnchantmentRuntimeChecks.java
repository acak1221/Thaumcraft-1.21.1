package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.crafting.InfusionEnchantmentRecipe;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.enchantment.EnchantmentHaste;
import thaumcraft.common.lib.enchantment.EnchantmentRepair;
import thaumcraft.common.lib.enchantment.EquipmentEnchantments;
import thaumcraft.common.lib.research.ObjectAspectResolver;

/** Explicit enchanted/worn equipment, connected survival player and finite charged wand are fixtures.
 * Focused movement uses the real effect; repair must occur through ordinary native player ticks. */
public final class EquipmentEnchantmentRuntimeChecks {
    private EquipmentEnchantmentRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        var level = test.getLevel();
        var profile = new GameProfile(UUID.randomUUID(), "enchfx-" + UUID.randomUUID().toString().substring(0, 8));
        var player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        boolean[] finished = {false};
        Runnable finish = () -> {
            if (finished[0]) return; finished[0] = true;
            if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
            player.discard(); channel.finishAndReleaseAll();
        };
        try {
            level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
            player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true); player.setPos(Vec3.atCenterOf(test.absolutePos(new BlockPos(3, 80, 3))));
            var lookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var repair = lookup.getOrThrow(EquipmentEnchantments.REPAIR);
            var haste = lookup.getOrThrow(EquipmentEnchantments.HASTE);
            var unbreaking = lookup.getOrThrow(Enchantments.UNBREAKING);
            test.assertTrue(repair.value().getMaxLevel() == 2 && haste.value().getMaxLevel() == 3 && !Enchantment.areCompatible(repair, unbreaking)
                    && !Enchantment.areCompatible(unbreaking, repair), "native custom enchantments preserve max levels and mutual Repair/Unbreaking conflict");
            var recipes = level.getRecipeManager().getAllRecipesFor(ThaumcraftInfusion.ENCHANTMENT_RECIPE_TYPE.get());
            for (var spec : List.of(Map.entry("repair", 6), Map.entry("haste", 5))) {
                InfusionEnchantmentRecipe recipe = recipes.stream().map(h -> h.value()).filter(r -> r.enchantment().equals(ResourceLocation.fromNamespaceAndPath("thaumcraft", spec.getKey()))).findFirst().orElseThrow();
                ItemStack input = new ItemStack(spec.getKey().equals("repair") ? ThaumcraftEquipment.THAUMIUM_SWORD.get() : Items.LEATHER_BOOTS);
                var physical = recipe.components().stream().map(i -> i.getItems()[0].copyWithCount(1)).toList();
                test.assertTrue(recipe.matches(new InfusionRecipeInput(input, physical), level) && recipe.xpLevels(input, level.registryAccess()) == spec.getValue(), "real custom recipe admits only its actual category with original first-level XP");
                input.enchant(spec.getKey().equals("repair") ? repair : haste, 1);
                test.assertTrue(recipe.xpLevels(input, level.registryAccess()) == spec.getValue() * 2, "the second custom level doubles actual matrix XP cost");
            }
            test.assertTrue(!InfusionEnchantmentRecipe.canApply(new ItemStack(Items.IRON_SWORD), repair)
                    && !InfusionEnchantmentRecipe.canApply(new ItemStack(Items.IRON_CHESTPLATE), haste), "custom target admission preserves IRepairable and boots-only categories");
            ItemStack hasteBoots = new ItemStack(Items.LEATHER_BOOTS); hasteBoots.enchant(haste, 3);
            player.setItemSlot(EquipmentSlot.FEET, hasteBoots); player.setYRot(0); player.zza = 1; player.hasImpulse = false; player.setDeltaMovement(Vec3.ZERO);
            EnchantmentHaste.tick(player);
            test.assertTrue(Math.abs(player.getDeltaMovement().z - (double)Float.intBitsToFloat(0x3d3851eb)) < 1.0E-10 && player.getDeltaMovement().x == 0, "Haste adds original level-three forward acceleration to live velocity");
            player.setDeltaMovement(Vec3.ZERO); player.setYRot(37); EnchantmentHaste.tick(player);
            test.assertTrue(player.getDeltaMovement().x == (double)Float.intBitsToFloat(0xbcddd5bc)
                    && player.getDeltaMovement().z == (double)Float.intBitsToFloat(0x3d13361a),
                    "yaw37 Haste vector matches execution of the official1.7.10 trig helper with original float multiplication");
            player.setDeltaMovement(Vec3.ZERO); player.setYRot(0); player.hasImpulse = true; EnchantmentHaste.tick(player);
            test.assertTrue(Math.abs(player.getDeltaMovement().z - (double)Float.intBitsToFloat(0x3cb851eb)) < 1.0E-10, "original isAirBorne/native hasImpulse halves the actual Haste acceleration");
            player.setDeltaMovement(Vec3.ZERO); player.zza = -1; EnchantmentHaste.tick(player);
            test.assertTrue(player.getDeltaMovement().equals(Vec3.ZERO), "backward movement receives no Haste boost");
            player.zza = 1; player.getAbilities().flying = true; EnchantmentHaste.tick(player);
            test.assertTrue(player.getDeltaMovement().equals(Vec3.ZERO), "flight receives no Haste boost");
            player.getAbilities().flying = false; player.zza = 0; player.hasImpulse = false; player.setDeltaMovement(Vec3.ZERO);
            player.getInventory().clearContent();
            ItemStack sword = new ItemStack(ThaumcraftEquipment.THAUMIUM_SWORD.get()); sword.enchant(repair, 2); sword.setDamageValue(10);
            ItemStack boots = new ItemStack(ThaumcraftEquipment.THAUMIUM_BOOTS.get()); boots.enchant(repair, 1); boots.setDamageValue(10);
            var swordBase = ObjectAspectResolver.baseTags(sword, level);
            test.assertTrue(swordBase.size() == 2 && swordBase.getAmount(Aspect.METAL) == 6 && swordBase.getAmount(Aspect.MAGIC) == 3,
                    "Repair sees original .75-scaled sword recipe tags, before enchantment/equipment bonuses");
            swordBase.add(Aspect.FIRE, 40);
            test.assertTrue(ObjectAspectResolver.baseTags(sword, level).getAmount(Aspect.FIRE) == 0, "base tags are independent copies");
            var cost = EnchantmentRepair.cost(sword, player, 2);
            test.assertTrue(cost.getAmount(Aspect.AIR) == 4 && cost.getAmount(Aspect.EARTH) == 8 && cost.getAmount(Aspect.FIRE) == 4
                    && cost.getAmount(Aspect.ORDER) == 8 && cost.getAmount(Aspect.ENTROPY) == 4 && cost.getAmount(Aspect.WATER) == 0,
                    "sum primal decomposition and floor-square-root payment use raw centivis");
            player.getInventory().items.set(0, sword); player.getInventory().items.set(9, sword.copy());
            player.setItemSlot(EquipmentSlot.OFFHAND, sword.copy()); player.setItemSlot(EquipmentSlot.FEET, boots);
            ItemStack full = new ItemStack(ThaumcraftContent.WAND.get()), partial = new ItemStack(ThaumcraftContent.WAND.get());
            var all = new java.util.LinkedHashMap<String, Integer>(); for (String primal : WandData.PRIMALS) all.put(primal, 100);
            ItemWandCasting.withData(full, new WandData("greatwood", "gold", all));
            all.put("terra", 0); ItemWandCasting.withData(partial, new WandData("greatwood", "gold", all));
            player.getInventory().items.set(34, full); player.getInventory().items.set(35, partial);
            int startTick = player.tickCount;
            test.onEachTick(() -> {
                if (finished[0]) return;
                try {
                    if (sword.getDamageValue() == 10) return;
                    test.assertTrue(sword.getDamageValue() == 8 && boots.getDamageValue() == 9
                            && player.getInventory().items.get(9).getDamageValue() == 10 && player.getOffhandItem().getDamageValue() == 10,
                            "ordinary living ticks repair hotbar and worn armor, excluding main slot9 and offhand");
                    var paid = ItemWandCasting.data(full);
                    test.assertTrue(paid.amount("aer") == 93 && paid.amount("terra") == 86 && paid.amount("ignis") == 93
                            && paid.amount("aqua") == 100 && paid.amount("ordo") == 86 && paid.amount("perditio") == 93,
                            "one real gold wand pays the exact two raw-centivis repair vectors once");
                    test.assertTrue(ItemWandCasting.data(partial).amount("aer") == 100 && ItemWandCasting.data(partial).amount("terra") == 0,
                            "the later-slot insufficient wand loses no partial payment");
                    finish.run(); test.succeed();
                } catch (RuntimeException | Error failure) { finish.run(); throw failure; }
            });
            test.runAfterDelay(90, () -> { if (!finished[0]) { finish.run(); test.fail("native player repair hook did not fire after " + (player.tickCount - startTick) + " player ticks"); } });
        } catch (RuntimeException | Error failure) { finish.run(); throw failure; }
    }
}
