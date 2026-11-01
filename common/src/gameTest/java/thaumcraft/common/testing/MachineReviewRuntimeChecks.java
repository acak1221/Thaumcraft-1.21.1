package thaumcraft.common.testing;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftLegacyTiles;
import thaumcraft.common.mixin.ExperienceOrbCountAccessor;
import thaumcraft.common.tiles.TileArcaneFurnace;
import thaumcraft.common.tiles.TileJarBrain;

/** Review regressions against real native entities, recipes and output publication.
 * Saved orb multiplicity and the furnace ejection call are declared fixtures. */
public final class MachineReviewRuntimeChecks {
    private MachineReviewRuntimeChecks() {}

    public static void brainJarConservesMergedExperience(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 80, 3);
        try {
            lane.put(lane.origin, ThaumcraftEldritchBlocks.JAR_BRAIN.get().defaultBlockState());
            var jar = (TileJarBrain) lane.level.getBlockEntity(lane.origin);
            var merged = lane.add(new ExperienceOrb(lane.level, lane.origin.getX() + .65,
                    lane.origin.getY() + .5, lane.origin.getZ() + .5, 3));
            // This is the native persisted representation of ten merged 3-XP orbs.
            var savedOrb = new CompoundTag();
            savedOrb.putShort("Health", (short) 5);
            savedOrb.putShort("Value", (short) 3);
            savedOrb.putInt("Count", 10);
            merged.readAdditionalSaveData(savedOrb);
            test.assertTrue(lane.level.addFreshEntity(merged), "the merged XP input is a real server entity");
            TileJarBrain.tick(lane.level, lane.origin, jar.getBlockState(), jar);
            test.assertTrue(merged.isRemoved() && jar.getXp() == 30,
                    "discarding ten merged 3-XP constituents credits all 30 XP");
            TileJarBrain.tick(lane.level, lane.origin, jar.getBlockState(), jar);
            test.assertTrue(jar.getXp() == 30, "a consumed merged entity cannot credit experience twice");

            var single = lane.add(new ExperienceOrb(lane.level, lane.origin.getX() + .65,
                    lane.origin.getY() + .5, lane.origin.getZ() + .5, 7));
            test.assertTrue(lane.level.addFreshEntity(single), "a single unmerged orb is admitted");
            TileJarBrain.tick(lane.level, lane.origin, jar.getBlockState(), jar);
            var restored = new TileJarBrain(lane.origin, jar.getBlockState());
            restored.loadWithComponents(jar.saveWithoutMetadata(lane.level.registryAccess()), lane.level.registryAccess());
            test.assertTrue(single.isRemoved() && jar.getXp() == 37 && restored.getXp() == 37,
                    "ordinary orbs still pay once and the conserved total survives a real save/load");
            test.succeed();
        } finally { lane.clean(); }
    }

    public static void furnaceUsesInputExperienceAndPreservesBonusData(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 110, 3);
        Set<UUID> previousOrbs = lane.level.getEntitiesOfClass(ExperienceOrb.class, lane.area(5)).stream()
                .map(ExperienceOrb::getUUID).collect(Collectors.toSet());
        boolean bonusFixture = false;
        try {
            // Isolate the real ejection/payment implementation. A detached core
            // avoids pretending this fixture formed the 3x3x3 furnace structure.
            var furnace = new TileArcaneFurnace(lane.origin, ThaumcraftLegacyTiles.ARCANE_FURNACE.get().defaultBlockState());
            furnace.setLevel(lane.level);
            furnace.facingX = furnace.facingZ = 0;
            var input = new ItemStack(Items.CACTUS);
            var output = furnace.smeltResult(lane.level, input);
            test.assertTrue(output.is(Items.GREEN_DYE) && furnace.smeltExperience(lane.level, input) == 1F,
                    "the live cactus recipe yields green dye and exactly one experience");
            furnace.ejectItem(lane.level, output.copy(), input);
            int experience = lane.level.getEntitiesOfClass(ExperienceOrb.class, lane.area(5)).stream()
                    .filter(orb -> !previousOrbs.contains(orb.getUUID()))
                    .mapToInt(orb -> orb.getValue() * ((ExperienceOrbCountAccessor) orb).thaumcraft$getOrbCount()).sum();
            test.assertTrue(experience == 1,
                    "ejection pays the input recipe's experience even though green dye has no smelting recipe");

            var bonus = new ItemStack(Items.TIPPED_ARROW, 11);
            bonus.set(DataComponents.CUSTOM_NAME, Component.literal("registered-component-bonus"));
            bonus.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.STRONG_HEALING));
            var data = new CompoundTag(); data.putString("source", "review-smelting-bonus");
            bonus.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            ItemStack before = bonus.copy();
            // Synchronous fixture only; restore the installed production lookup
            // before a world tick can observe this temporary provider.
            TileArcaneFurnace.setSmeltingBonus(stack -> stack.is(Items.CACTUS) ? bonus.copy() : null);
            bonusFixture = true;
            long seed = 0;
            while (true) {
                var random = RandomSource.create(seed);
                // ItemEntity(Level, x, y, z, stack) first takes TWO doubles
                // from the level RNG for sideways launch, before the furnace
                // overwrites that motion and rolls the actual bonus.
                random.nextDouble(); random.nextDouble();
                if (random.nextFloat() < .25F) break;
                ++seed;
            }
            lane.level.random.setSeed(seed); // Explicit successful post-construction no-bellows roll.
            furnace.ejectItem(lane.level, output.copy(), input);
            var bonuses = lane.level.getEntitiesOfClass(ItemEntity.class, lane.area(5), drop -> drop.getItem().is(Items.TIPPED_ARROW));
            test.assertTrue(bonuses.size() == 1 && ItemStack.matches(bonuses.getFirst().getItem(), bonus.copyWithCount(1))
                            && ItemStack.matches(bonus, before),
                    "one rolled bonus preserves registered name, potion and custom data without changing the registered stack; actual="
                            + bonuses.stream().map(drop -> drop.getItem().getCount() + "x " + drop.getItem().getComponents()).toList());
            test.succeed();
        } finally {
            if (bonusFixture) TileArcaneFurnace.setSmeltingBonus(ThaumcraftApi::getSmeltingBonus);
            lane.level.getEntitiesOfClass(ExperienceOrb.class, lane.area(5)).stream()
                    .filter(orb -> !previousOrbs.contains(orb.getUUID())).forEach(ExperienceOrb::discard);
            lane.clean();
        }
    }
}
