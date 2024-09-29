package com.mt1006.mocap.events;

import com.mt1006.mocap.mocap.actions.EntityUpdate;
import com.mt1006.mocap.mocap.actions.Hurt;
import com.mt1006.mocap.mocap.playing.Playing;
import com.mt1006.mocap.mocap.recording.Recording;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class EntityEvent
{
	public static boolean onEntityHurt(LivingEntity entity, DamageSource source, float amount)
	{
		if (Recording.isActive() && entity.level() instanceof ServerLevel)
		{
			Recording.fromRecordedPlayer(entity).forEach((ctx) -> ctx.addAction(new Hurt()));
			Recording.listTrackedEntities(entity).forEach((tracked) -> tracked.ctx.addAction(EntityUpdate.hurt(tracked.id)));
		}
		return true;
	}

	public static boolean onEntityDrop(LivingEntity entity)
	{
		return !Playing.playbacks.isEmpty() && entity.getTags().contains(Playing.MOCAP_ENTITY_TAG);
	}
}
