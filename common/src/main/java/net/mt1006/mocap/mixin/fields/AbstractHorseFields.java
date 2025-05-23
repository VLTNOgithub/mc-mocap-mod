package net.mt1006.mocap.mixin.fields;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractHorse.class)
public interface AbstractHorseFields
{
	@Accessor static @Nullable EntityDataAccessor<Byte> getDATA_ID_FLAGS() { return null; }
	@Accessor SimpleContainer getInventory();
}
