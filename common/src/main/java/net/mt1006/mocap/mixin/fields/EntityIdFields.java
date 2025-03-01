package net.mt1006.mocap.mixin.fields;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityIdFields
{
	@Invoker @Nullable String callGetEncodeId();
}
