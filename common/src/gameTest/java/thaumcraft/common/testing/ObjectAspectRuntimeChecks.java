package thaumcraft.common.testing;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantments;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.items.ItemJarLabel;
import thaumcraft.common.lib.research.ObjectAspectResolver;

/** Real bundled ingredients/recipes and native item components; no invented test recipes. */
public final class ObjectAspectRuntimeChecks {
    private ObjectAspectRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        var level = test.getLevel();
        for (var item : new net.minecraft.world.item.Item[]{Items.COAL, Items.CHARCOAL}) {
            var coal = ObjectAspectResolver.get(new ItemStack(item), level);
            test.assertTrue(coal.getAmount(Aspect.ENERGY) == 2 && coal.getAmount(Aspect.FIRE) == 2,
                    "original wildcard coal tags override the incomplete legacy fallback");
        }
        var torch = ObjectAspectResolver.get(new ItemStack(Items.TORCH), level);
        test.assertTrue(torch.getAmount(Aspect.LIGHT) == 1 && torch.getAmount(Aspect.ENERGY) == 0,
                "original torch is direct lux1, not a recipe-derived fuel binding");
        var dust = ObjectAspectResolver.get(new ItemStack(Items.GLOWSTONE_DUST), level);
        test.assertTrue(dust.getAmount(Aspect.LIGHT) == 2 && dust.getAmount(Aspect.SENSES) == 1,
                "original dustGlowstone dictionary binding has real modern native items");
        var nitor = ObjectAspectResolver.get(new ItemStack(ThaumcraftContent.MATERIALS.get("nitor").get()), level);
        test.assertTrue(nitor.getAmount(Aspect.ENERGY) == 1 && nitor.getAmount(Aspect.FIRE) == 1
                && nitor.getAmount(Aspect.LIGHT) == 3 && nitor.getAmount(Aspect.SENSES) == 1,
                "same native Nitor recipe derives catalyst tags plus truncated square-root aspect costs");
        test.assertTrue(nitor.aspects.equals(ObjectAspectResolver.get(new ItemStack(ThaumcraftContent.MATERIALS.get("nitor").get(), 17), level).aspects),
                "dissolution tags describe one item, never the entire stack");
        var phial = ObjectAspectResolver.get(ItemEssence.filled("ignis"), level);
        test.assertTrue(phial.getAmount(Aspect.FIRE) == 8, "real phial component contributes its eight stored essentia");
        var blankLabel = ObjectAspectResolver.get(ItemJarLabel.blank(), level);
        var boundLabel = ObjectAspectResolver.get(ItemJarLabel.bound("ignis"), level);
        test.assertTrue(boundLabel.getAmount(Aspect.FIRE) == blankLabel.getAmount(Aspect.FIRE) + 1,
                "original bound resource13 label adds exactly one contained aspect, unlike eight-unit phials");
        var bow = new ItemStack(Items.BOW);
        bow.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.POWER), 3);
        var enchanted = ObjectAspectResolver.get(bow, level);
        test.assertTrue(enchanted.getAmount(Aspect.MAGIC) == 3 && enchanted.getAmount(Aspect.WEAPON) == 3
                && enchanted.getAmount(Aspect.FLIGHT) == 1 && enchanted.size() <= 6,
                "native gear/enchantment bonuses and original six-tag culling are applied to the actual stack");
        var potion = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.STRONG_HEALING);
        var potionTags = ObjectAspectResolver.get(potion, level);
        test.assertTrue(potionTags.getAmount(Aspect.WATER) == 1 && potionTags.getAmount(Aspect.ENTROPY) == 2
                && potionTags.getAmount(Aspect.MAGIC) == 4 && potionTags.getAmount(Aspect.HEAL) == 6,
                "actual native splash-potion component maps original amplifier and potion aspects");
        test.assertTrue(ObjectAspectResolver.get(new ItemStack(Items.BARRIER), level).size() == 0,
                "unknown recipe-less items remain unknown instead of receiving fabricated resources");
        var crucible = ObjectAspectResolver.get(new ItemStack(ThaumcraftAlchemy.CRUCIBLE_ITEM.get()), level);
        test.assertTrue(crucible.getAmount(Aspect.CRAFT) == 4 && crucible.getAmount(Aspect.MAGIC) == 4,
                "converted crucible uses original cauldron-derived direct base plus fabrico4/praecantatio4");
        var log = ObjectAspectResolver.get(new ItemStack(ThaumcraftNaturals.SILVERWOOD_LOG.get()), level);
        test.assertTrue(log.getAmount(Aspect.TREE) == 3 && log.getAmount(Aspect.MAGIC) == 1 && log.getAmount(Aspect.ORDER) == 1,
                "real silverwood log overrides the earlier generic logWood registration");
        for (var item : new net.minecraft.world.item.Item[]{ThaumcraftDistillation.ALCHEMY_FURNACE_ITEM.get(),
                ThaumcraftAlembics.ALEMBIC_ITEM.get(), ThaumcraftContent.MATERIALS.get("vis_filter").get()}) {
            test.assertTrue(ObjectAspectResolver.get(new ItemStack(item), level).visSize() > 0,
                    "native distillation prerequisites derive aspects from their actual registered recipes");
        }
        var upgraded = new ItemStack(ThaumcraftContent.WAND.get());
        upgraded.set(ThaumcraftContent.WAND_DATA.get(), new WandData("greatwood", "gold", java.util.Map.of()));
        var wandTags = ObjectAspectResolver.get(upgraded, level);
        test.assertTrue(wandTags.getAmount(Aspect.MAGIC) >= 3 && wandTags.getAmount(Aspect.TOOL) >= 2,
                "actual assembled wand component contributes original sum-of-material-cost bonuses, not fixed wood/iron tags");
        var valveTags = ObjectAspectResolver.get(new ItemStack(ThaumcraftTubes.ESSENTIA_VALVE_ITEM.get()), level);
        test.assertTrue(valveTags.aspects.equals(java.util.Map.of(Aspect.WATER, 2, Aspect.ORDER, 2)),
                "genuine shapeless valve derives original truncated ingredients plus sqrt5 aqua/ordo from the registered recipe, not an empty unsupported-recipe result");
        var resonatorTags = ObjectAspectResolver.get(new ItemStack(ThaumcraftContent.RESONATOR.get()), level);
        test.assertTrue(resonatorTags.aspects.equals(java.util.Map.of(Aspect.METAL, 12, Aspect.WATER, 2, Aspect.AIR, 2)),
                "original four iron/quartz/stick resonator derives metallum12 and two whole units of each primal vis cost");
        var restrictedTags = ObjectAspectResolver.get(new ItemStack(ThaumcraftTubes.ESSENTIA_RESTRICT_ITEM.get()), level);
        test.assertTrue(restrictedTags.aspects.equals(java.util.Map.of(Aspect.WATER, 2, Aspect.EARTH, 5)),
                "real shapeless restricted tube derives original stone2 times.75 plus sqrt16terra4 and sqrt5aqua2");
        var filterTags = ObjectAspectResolver.get(new ItemStack(ThaumcraftTubes.ESSENTIA_FILTER_ITEM.get()), level);
        test.assertTrue(filterTags.getAmount(Aspect.WATER) >= 2 && filterTags.getAmount(Aspect.ORDER) >= 4,
                "native filter tube participates in shared recipe-derived dissolution including its exact base primal cost contribution");
        var onewayTags = ObjectAspectResolver.get(new ItemStack(ThaumcraftTubes.ESSENTIA_ONEWAY_ITEM.get()), level);
        test.assertTrue(onewayTags.getAmount(Aspect.WATER) >= 2 && onewayTags.getAmount(Aspect.ORDER) >= 2 && onewayTags.getAmount(Aspect.ENTROPY) >= 2,
                "native directional tube resolves its real lapis/tag ingredient and truncated square-root aqua/ordo/perditio cost");
        test.succeed();
    }
}
