package net.mt1006.mocap.mixin.fields;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.mt1006.mocap.mocap.actions.Movement;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.Set;

@Mixin(Entity.class)
public interface EntityFields
{
	@Accessor static @Nullable EntityDataAccessor<Byte> getDATA_SHARED_FLAGS_ID() { return null; }
	@Invoker void callRecordMovementThroughBlocks(Vec3 oldPosition, Vec3 position);
	@Invoker void callUnsetRemoved();
}
