package com.mt1006.mocap.fabric.events;

import com.mt1006.mocap.events.EntityEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class EntityFabricEvent
{
	public static boolean onEntityHurt(LivingEntity entity, DamageSource source, float amount)
	{
		EntityEvent.onEntityHurt(entity);
		return true;
	}
}
