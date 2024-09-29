package com.mt1006.mocap.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class TestMixin
{
	//TODO: remove
	@Shadow public float moveDist;
	@Shadow private float nextStep;

	@Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z"))
	public void test(MoverType type, Vec3 pos, CallbackInfo ci)
	{
		//MocapMod.LOGGER.warn("{} {}", moveDist, nextStep);
	}
}
