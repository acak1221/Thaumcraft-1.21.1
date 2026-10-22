package thaumcraft.client.fx;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 24 independent TC4 oracle call sites, recorded in docs/audit/SOUND_FINDINGS.md. */
class SoundValueParityTest {
    @Test void sampledVolumeAndPitchExpressionsKeepTheOriginalValues() throws Exception {
        String[][] samples = {
            {"common/entities/monster/EntityEldritchGolem", "playSound(sound(\"egattack\"),1.0F,1.0F+level().random.nextFloat()*.1F)"},
            {"common/entities/monster/EntityEldritchGolem", "SoundEvents.IRON_GOLEM_ATTACK,getSoundSource(),1.0F,1.0F,false)"},
            {"common/entities/monster/EntityEldritchGolem", "playSound(SoundEvents.IRON_GOLEM_STEP,1.0F,1.0F)"},
            {"common/entities/monster/EntityEldritchGolem", "sound(\"jacobs\"),getSoundSource(),.8F,1F+(level().random.nextFloat()-level().random.nextFloat())*.05F,false)"},
            {"common/entities/monster/EntityEldritchWarden", "playSound(sound(\"zap\"),1.0F,.9F+level().random.nextFloat()*.1F)"},
            {"common/entities/monster/EntityEldritchWarden", "playSound(sound(\"egattack\"),2.0F,1.0F+level().random.nextFloat()*.1F)"},
            {"common/entities/monster/EntityEldritchWarden", "playSound(sound(\"egscreech\"),4.0F,1.0F+level().random.nextFloat()*.1F)"},
            {"common/entities/monster/EntityEldritchGuardian", "playSound(sound(\"egattack\"),2,1+random.nextFloat()*.1F)"},
            {"common/entities/monster/EntityEldritchGuardian", "playSound(sound(\"egscreech\"),3,1+random.nextFloat()*.1F)"},
            {"common/entities/monster/EntityEldritchCrab", "playSound(sound(\"crabclaw\"),1.0F,.9F+getRandom().nextFloat()*.2F)"},
            {"common/entities/monster/EntityEldritchCrab", "playSound(SoundEvents.SPIDER_STEP,.15F,1.0F)"},
            {"common/entities/monster/EntityWisp", "playSound(sound(\"zap\"),1,1.1F)"},
            {"common/items/wands/foci/ItemFocusHellbat", "ICE,SoundSource.PLAYERS,.2F,.95F+level.random.nextFloat()*.1F)"},
            {"common/items/wands/foci/ItemFocusHellbat", "FAIL,SoundSource.PLAYERS,.1F,.8F+player.level().random.nextFloat()*.1F)"},
            {"common/items/wands/foci/ItemFocusPrimal", "ICE,SoundSource.PLAYERS,.3F,.8F+level.random.nextFloat()*.1F)"},
            {"common/items/wands/foci/ItemFocusPech", "ICE,SoundSource.PLAYERS,.4F,1+level.random.nextFloat()*.1F)"},
            {"common/items/equipment/ItemElementalPickaxe", "FAIL,SoundSource.PLAYERS,0.2F,0.2F+level.random.nextFloat()*0.2F)"},
            {"common/items/equipment/ItemElementalSword", "WIND,SoundSource.PLAYERS,0.5F,0.9F+server.random.nextFloat()*0.2F)"},
            {"common/lib/world/ElementalSwordSweep", "SWING,SoundSource.PLAYERS,1.0F,0.9F+level.random.nextFloat()*0.2F)"},
            {"common/items/ItemLootBag", "COINS,SoundSource.PLAYERS,.75F,1F)"},
            {"common/tiles/TileCrucible", "BUBBLE,SoundSource.BLOCKS,.2F,1+server.random.nextFloat()*.4F)"},
            {"common/entities/projectile/EntityShockOrb", "SoundSource.PLAYERS,1,1+(random.nextFloat()-random.nextFloat())*.2F)"},
            {"client/gui/GuiResearchTable", "sound(\"write\",.2F,1)"},
            {"client/gui/GuiResearchTable", "sound(\"erase\",.2F,1+minecraft.player.getRandom().nextFloat()*.1F)"}
        };
        for (String[] sample : samples) {
            String source = Files.readString(Path.of("src/main/java/thaumcraft/" + sample[0] + ".java"));
            assertTrue(source.replaceAll("\\s+", "").contains(sample[1]), sample[0] + ": " + sample[1]);
        }
    }
}
