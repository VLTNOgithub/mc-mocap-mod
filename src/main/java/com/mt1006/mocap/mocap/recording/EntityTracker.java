package com.mt1006.mocap.mocap.recording;

import com.mt1006.mocap.mixin.fields.LevelMixin;
import com.mt1006.mocap.mocap.actions.EntityUpdate;
import com.mt1006.mocap.mocap.playing.Playing;
import com.mt1006.mocap.mocap.settings.Settings;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityTracker
{
	private final RecordingContext ctx;
	private final Map<Entity, TrackedEntity> map = new HashMap<>();
	private int counter = 0;
	private Entity playerVehicle = null;

	public EntityTracker(RecordingContext ctx)
	{
		this.ctx = ctx;
	}
	
	public TrackedEntity get(Entity entity)
	{
		return map.get(entity);
	}

	public void onTick()
	{
		updateTracked();
		updateVehicle();
		removeOld();
	}

	private void updateTracked()
	{
		if (Settings.ENTITY_TRACKING_DISTANCE.val == 0.0)
		{
			if (!map.isEmpty())
			{
				map.forEach((uuid, trackedEntity) -> ctx.addAction(EntityUpdate.removeEntity(trackedEntity.id)));
				map.clear();
			}
			return;
		}

		double entityTrackingDist = Settings.ENTITY_TRACKING_DISTANCE.val;
		boolean limitDistance = entityTrackingDist >= 0.0;
		double maxDistanceSqr = entityTrackingDist * entityTrackingDist;

		for (Entity entity : ((LevelMixin)ctx.recordedPlayer.level()).callGetEntities().getAll())
		{
			if ((limitDistance && ctx.recordedPlayer.distanceToSqr(entity) > maxDistanceSqr) || entity instanceof Player
					|| (Settings.PREVENT_TRACKING_PLAYED_ENTITIES.val && entity.getTags().contains(Playing.MOCAP_ENTITY_TAG)))
			{
				continue;
			}

			if (!ctx.entityFilter.isAllowed(entity)) { continue; }
			//if (Settings.DYNAMIC_ENTITY_FILTERS.get())

			TrackedEntity trackedEntity = map.get(entity);
			if (trackedEntity == null)
			{
				//if (!ctx.entityFilter.isAllowed(entity)) { continue; }

				trackedEntity = new TrackedEntity(ctx, counter++, entity);
				map.put(entity, trackedEntity);
				ctx.addAction(EntityUpdate.addEntity(trackedEntity.id, entity));
			}
			trackedEntity.onTick();
		}
	}

	private void updateVehicle()
	{
		Entity newPlayerVehicle = ctx.recordedPlayer.getVehicle();
		if (newPlayerVehicle == null)
		{
			if (playerVehicle != null)
			{
				ctx.addAction(EntityUpdate.playerDismount());
				playerVehicle = null;
			}
			return;
		}

		if (newPlayerVehicle.equals(playerVehicle)) { return; }

		if (playerVehicle != null)
		{
			ctx.addAction(EntityUpdate.playerDismount());
			playerVehicle = null;
		}

		TrackedEntity trackedEntity = map.get(newPlayerVehicle);
		if (trackedEntity != null)
		{
			ctx.addAction(EntityUpdate.playerMount(trackedEntity.id));
			playerVehicle = newPlayerVehicle;
		}
	}

	private void removeOld()
	{
		int tick = ctx.getTick();
		List<Entity> toRemove = new ArrayList<>();

		for (Map.Entry<Entity, TrackedEntity> entry : map.entrySet())
		{
			if (entry.getValue().lastTick == tick) { continue; }

			if (!entry.getValue().dying)
			{
				int entityId = entry.getValue().id;
				EntityUpdate entityUpdate = entry.getKey().getRemovalReason() == Entity.RemovalReason.KILLED
						? EntityUpdate.kill(entityId) : EntityUpdate.removeEntity(entityId);
				ctx.addAction(entityUpdate);
			}
			toRemove.add(entry.getKey());
		}
		toRemove.forEach(map::remove);
	}

	public static class TrackedEntity
	{
		//TODO: change to private
		public final RecordingContext ctx;
		public final int id;
		private final Entity entity;
		private final PositionTracker positionTracker;
		private @Nullable RecordedEntityState previousState = null;
		public boolean dying;
		public int lastTick;

		public TrackedEntity(RecordingContext ctx, int id, Entity entity)
		{
			this.ctx = ctx;
			this.id = id;
			this.entity = entity;
			this.positionTracker = new PositionTracker(entity, true);
		}

		public void onTick()
		{
			RecordedEntityState state = new RecordedEntityState(entity);
			state.saveTrackedEntityDifference(ctx.data.actions, id, previousState);
			previousState = state;

			lastTick = ctx.getTick();

			if (entity instanceof LivingEntity && ((LivingEntity)entity).isDeadOrDying() && !dying)
			{
				ctx.addAction(EntityUpdate.kill(id));
				dying = true;
			}
		}
	}
}
