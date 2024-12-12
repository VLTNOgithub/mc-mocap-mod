package com.mt1006.mocap.mixin.fabric;

import com.mt1006.mocap.events.EntityEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityFabricMixin
{
	// Based on Forge LivingDropsEvent injection

	@Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
	private void atDropAllDeathLoot(ServerLevel serverLevel, DamageSource damageSource, CallbackInfo ci)
	{
		if (EntityEvent.onEntityDrop((LivingEntity)(Object)this)) { ci.cancel(); }
	}
}
