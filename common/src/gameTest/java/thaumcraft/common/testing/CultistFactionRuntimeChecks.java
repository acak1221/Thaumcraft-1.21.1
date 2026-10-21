package thaumcraft.common.testing;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftCultists;
import thaumcraft.common.config.ThaumcraftEldritchItems;
import thaumcraft.common.config.ThaumcraftRunicBaubles;
import thaumcraft.common.entities.monster.EntityCultist;
import thaumcraft.common.entities.monster.EntityCultistLeader;
import thaumcraft.common.lib.cult.CultistRules;

/**
 * The original TC4 cultist faction inside a real dedicated server: the registered numbers of the
 * crimson knight, the crimson cleric and the praetor, the equipment they wear, the resources they
 * drop, the two hundred tick enrage and the eight block regeneration aura of the praetor and the
 * stage machine of the crimson portal that summons the four banners, the loot crates and the
 * cultist minions.
 */
public final class CultistFactionRuntimeChecks {
    private CultistFactionRuntimeChecks() {}

    /** The original cultists keep their numbers, their gear and their loot in a real world. */
    public static void factionNumbersGearAndLoot(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.CREATIVE);
            lane.floor(6);

            var knight = lane.add(ThaumcraftCultists.CULTIST_KNIGHT.get().create(level));
            knight.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + .5D, 0F, 0F);
            // The original cultist knight inherits the champion level of the cultist, so the
            // declared join roll keeps the ordinary numbers of the fixture.
            ChampionJoinRoll.join(level, knight);
            knight.originalSpawnEquipment(level, level.getCurrentDifficultyAt(lane.origin));
            test.assertTrue(knight.getMaxHealth() == 36F && knight.getAttributeValue(Attributes.ATTACK_DAMAGE) == 4F
                            && Math.abs(knight.getAttributeValue(Attributes.MOVEMENT_SPEED) - 0.3D) < 1e-6D
                            && knight.getAttributeValue(Attributes.FOLLOW_RANGE) == 32F,
                    "the crimson knight keeps the original 36 health, 4 damage, 0.3 speed and 32 follow range: "
                            + knight.getMaxHealth() + " " + knight.getAttributeValue(Attributes.ATTACK_DAMAGE) + " "
                            + knight.getAttributeValue(Attributes.MOVEMENT_SPEED) + " " + knight.getAttributeValue(Attributes.FOLLOW_RANGE));
            test.assertTrue(knight.getItemBySlot(EquipmentSlot.HEAD).is(ThaumcraftRunicBaubles.CULTIST_PLATE_HELM.get())
                            && knight.getItemBySlot(EquipmentSlot.CHEST).is(ThaumcraftRunicBaubles.CULTIST_PLATE.get())
                            && knight.getItemBySlot(EquipmentSlot.LEGS).is(ThaumcraftRunicBaubles.CULTIST_PLATE_LEGS.get()),
                    "the crimson knight wears the original crimson plate: " + knight.getItemBySlot(EquipmentSlot.HEAD) + " "
                            + knight.getItemBySlot(EquipmentSlot.CHEST) + " " + knight.getItemBySlot(EquipmentSlot.LEGS));
            test.assertTrue(!knight.getMainHandItem().isEmpty(), "the knight always carries a blade: " + knight.getMainHandItem());

            var cleric = lane.add(ThaumcraftCultists.CULTIST_CLERIC.get().create(level));
            cleric.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + 2.5D, 0F, 0F);
            ChampionJoinRoll.join(level, cleric);
            cleric.originalSpawnEquipment(level, level.getCurrentDifficultyAt(lane.origin));
            test.assertTrue(cleric.getMaxHealth() == 30F && cleric.getAttributeValue(Attributes.ATTACK_DAMAGE) == 4F,
                    "the crimson cleric keeps the original 30 health and 4 damage: " + cleric.getMaxHealth() + " "
                            + cleric.getAttributeValue(Attributes.ATTACK_DAMAGE));
            test.assertTrue(cleric.getItemBySlot(EquipmentSlot.HEAD).is(ThaumcraftRunicBaubles.CULTIST_ROBE_HOOD.get())
                            && cleric.getItemBySlot(EquipmentSlot.CHEST).is(ThaumcraftRunicBaubles.CULTIST_ROBE.get())
                            && cleric.getItemBySlot(EquipmentSlot.LEGS).is(ThaumcraftRunicBaubles.CULTIST_ROBE_LEGS.get()),
                    "the crimson cleric wears the original crimson robe: " + cleric.getItemBySlot(EquipmentSlot.HEAD) + " "
                            + cleric.getItemBySlot(EquipmentSlot.CHEST) + " " + cleric.getItemBySlot(EquipmentSlot.LEGS));

            // The original drop of a cultist is one knowledge fragment, one void seed or one gold coin.
            Item fragment = ThaumcraftContent.MATERIALS.get("knowledge_fragment").get();
            Item voidSeed = ThaumcraftContent.MATERIALS.get("void_seed").get();
            Item coin = ThaumcraftContent.MATERIALS.get("gold_coin").get();
            int coins = 0;
            int foreign = 0;
            for (int index = 0; index < 40; index++) {
                var doomed = lane.add(ThaumcraftCultists.CULTIST_KNIGHT.get().create(level));
                doomed.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + .5D, 0F, 0F);
                ChampionJoinRoll.join(level, doomed);
                doomed.hurt(level.damageSources().playerAttack(player), 1000F);
            }
            for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, lane.area(6))) {
                ItemStack stack = drop.getItem();
                if (stack.is(coin)) coins += stack.getCount();
                if (!stack.is(fragment) && !stack.is(voidSeed) && !stack.is(coin)
                        && !stack.is(ThaumcraftEldritchItems.CRIMSON_RITES.get())) foreign++;
            }
            test.assertTrue(coins > 0, "the cultists drop the original gold coin: " + coins);
            test.assertTrue(foreign == 0, "the cultists never drop a foreign item: " + foreign);
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }

    /** The original praetor keeps its numbers, its enrage and its regeneration aura. */
    public static void praetorBossBehaviour(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.CREATIVE);
            player.setPos(lane.origin.getX() + 40D, lane.origin.getY(), lane.origin.getZ());
            lane.floor(4);

            var praetor = lane.add(ThaumcraftCultists.CULTIST_LEADER.get().create(level));
            praetor.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + .5D, 0F, 0F);
            level.addFreshEntity(praetor);
            praetor.originalSpawnEquipment(level, level.getCurrentDifficultyAt(lane.origin));
            double praetorSpeed = praetor.getChampionModifier() == CultistRules.CHAMPION_BOLD ? 0.32D * 1.3D : 0.32D;
            double praetorDamage = praetor.getChampionModifier() == CultistRules.CHAMPION_MIGHTY ? 5D * 4D : 5D;
            test.assertTrue(praetor.getMaxHealth() == 125F && praetor.getAttributeValue(Attributes.ATTACK_DAMAGE) == praetorDamage
                            && Math.abs(praetor.getAttributeValue(Attributes.MOVEMENT_SPEED) - praetorSpeed) < 1e-6D
                            && praetor.getAttributeValue(Attributes.FOLLOW_RANGE) == 40F
                            && Math.abs(praetor.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) - 0.95D) < 1e-6D,
                    "the praetor keeps the original 125 health, 5 damage, 0.32 speed, 40 follow range and 0.95 knockback resistance: "
                            + praetor.getMaxHealth() + " " + praetor.getAttributeValue(Attributes.ATTACK_DAMAGE) + " "
                            + praetor.getAttributeValue(Attributes.MOVEMENT_SPEED) + " " + praetor.getAttributeValue(Attributes.FOLLOW_RANGE));
            test.assertTrue(java.util.List.of(CultistRules.LEADER_TITLES).contains(praetor.getTitle()),
                    "the praetor takes one of the original fifteen titles: " + praetor.getTitle());
            test.assertTrue(praetor.getChampionModifier() >= 0 && praetor.getChampionModifier() < CultistRules.CHAMPION_COUNT,
                    "the praetor always carries one of the original thirteen champion modifiers: " + praetor.getChampionModifier());
            test.assertTrue(praetor.getItemBySlot(EquipmentSlot.CHEST).is(ThaumcraftRunicBaubles.CULTIST_LEADER.get()),
                    "the praetor wears the original crimson praetor armour: " + praetor.getItemBySlot(EquipmentSlot.CHEST));

            // The original boss base caps a single hit at thirty five and enrages for two hundred ticks.
            praetor.hurt(level.damageSources().playerAttack(player), 100F);
            test.assertTrue(praetor.getAnger() == 200, "a heavy hit enrages the praetor for 200 ticks: " + praetor.getAnger());
            test.assertTrue(praetor.hasEffect(MobEffects.DAMAGE_BOOST) && praetor.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() == 2,
                    "the enrage of the praetor is the original strength two: "
                            + (praetor.hasEffect(MobEffects.DAMAGE_BOOST) ? praetor.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() : -1));

            // The original aura of the praetor feeds every cultist within eight blocks.
            var follower = lane.add(ThaumcraftCultists.CULTIST_CLERIC.get().create(level));
            follower.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + 3.5D, 0F, 0F);
            ChampionJoinRoll.join(level, follower);
            var far = lane.add(ThaumcraftCultists.CULTIST_CLERIC.get().create(level));
            far.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + 12.5D, 0F, 0F);
            ChampionJoinRoll.join(level, far);
            for (int tick = 0; tick < 5; tick++) level.tickNonPassenger(praetor);
            test.assertTrue(follower.hasEffect(MobEffects.REGENERATION)
                            && follower.getEffect(MobEffects.REGENERATION).getAmplifier() == CultistRules.LEADER_AURA_AMPLIFIER,
                    "the praetor feeds a cultist within eight blocks with regeneration two: "
                            + (follower.hasEffect(MobEffects.REGENERATION) ? follower.getEffect(MobEffects.REGENERATION).getAmplifier() : -1));
            test.assertTrue(!far.hasEffect(MobEffects.REGENERATION),
                    "a cultist outside the eight block aura gets nothing: " + far.getEffect(MobEffects.REGENERATION));
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }

    /** The original crimson portal runs its stage machine inside the arena. */
    public static void portalStageMachine(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.CREATIVE);
            player.setPos(lane.origin.getX() + 12D, lane.origin.getY(), lane.origin.getZ());
            lane.floor(9);

            var portal = lane.add(ThaumcraftCultists.CULTIST_PORTAL.get().create(level));
            portal.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + .5D, 0F, 0F);
            level.addFreshEntity(portal);
            test.assertTrue(portal.getMaxHealth() == 500F && portal.getAttributeValue(Attributes.ATTACK_DAMAGE) == 0F
                            && portal.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 1F,
                    "the crimson portal keeps the original 500 health, no attack damage and full knockback resistance: "
                            + portal.getMaxHealth() + " " + portal.getAttributeValue(Attributes.ATTACK_DAMAGE) + " "
                            + portal.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            var woodenDevice = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getOptional(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "wooden_device")).orElse(null);
            var lootCrate = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getOptional(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "loot_crate")).orElse(null);
            test.assertTrue(woodenDevice != null && lootCrate != null,
                    "the banner device and the loot crate of the portal are registered blocks");

            for (int tick = 0; tick < 210; tick++) level.tickNonPassenger(portal);
            test.assertTrue(portal.getStage() >= 1,
                    "the portal advances its stage while a player stands in the arena: stage=" + portal.getStage()
                            + " counter=" + portal.getStageCounter() + " player=" + player.position()
                            + " portal=" + portal.position() + " ticks=" + portal.tickCount);
            int banners = 0;
            int crates = 0;
            for (int dx = -8; dx <= 8; dx++)
                for (int dz = -8; dz <= 8; dz++) {
                    BlockPos at = lane.origin.offset(dx, 0, dz);
                    if (level.getBlockState(at).is(woodenDevice)) banners++;
                    if (level.getBlockState(at).is(lootCrate)) crates++;
                }
            test.assertTrue(banners == 4, "the portal places the original four crimson banners around itself: " + banners);
            test.assertTrue(crates >= 1, "the portal seeds the arena with the original loot crates: " + crates);
            var cultists = level.getEntitiesOfClass(EntityCultist.class, lane.area(10));
            test.assertTrue(!cultists.isEmpty(), "the portal sends its cultist minions into the arena: " + cultists.size());
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }
}
