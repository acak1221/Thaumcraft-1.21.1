package thaumcraft.common.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.types.Type;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Skips vanilla's data-fixer schema lookup for the port's own entity types.
 *
 * <p>{@code EntityType.Builder.build(String)} asks the vanilla data-fixer schema for the type it
 * is building and throws the answer away; for a mod entity the schema has no entry, so every start
 * logged one {@code ERROR No data fixer registered for thaumcraft:...} line per entity (55 lines).
 * Only {@code thaumcraft:} keys are skipped; the entity type itself, its saving and every other
 * mod's entities are untouched.</p>
 */
@Mixin(EntityType.Builder.class)
abstract class EntityTypeSchemaNoticeMixin {
    @WrapOperation(method = "build", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/Util;fetchChoiceType(Lcom/mojang/datafixers/DSL$TypeReference;Ljava/lang/String;)Lcom/mojang/datafixers/types/Type;"))
    private Type<?> thaumcraft$skipOwnSchemaLookup(DSL.TypeReference reference, String key, Operation<Type<?>> original) {
        return key != null && key.startsWith("thaumcraft:") ? null : original.call(reference, key);
    }
}
