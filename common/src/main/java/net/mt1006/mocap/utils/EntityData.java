package net.mt1006.mocap.utils;

import net.mt1006.mocap.MocapMod;
import net.mt1006.mocap.mixin.fields.AbstractHorseFields;
import net.mt1006.mocap.mixin.fields.EntityFields;
import net.mt1006.mocap.mixin.fields.LivingEntityFields;
import net.mt1006.mocap.mixin.fields.PlayerFields;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EntityData
{
	public static final DataIndex<Byte> ENTITY_FLAGS =                                    new DataIndex<>(EntityFields.getDATA_SHARED_FLAGS_ID());
	public static final DataIndex<Byte> LIVING_ENTITY_FLAGS =                             new DataIndex<>(LivingEntityFields.getDATA_LIVING_ENTITY_FLAGS());
	public static final DataIndex<List<ParticleOptions>> LIVING_ENTITY_EFFECT_PARTICLES = new DataIndex<>(LivingEntityFields.getDATA_EFFECT_PARTICLES());
	public static final DataIndex<Boolean> LIVING_ENTITY_EFFECT_AMBIENCE =                new DataIndex<>(LivingEntityFields.getDATA_EFFECT_AMBIENCE_ID());
	public static final DataIndex<Byte> PLAYER_SKIN_PARTS =                               new DataIndex<>(PlayerFields.getDATA_PLAYER_MODE_CUSTOMISATION());
	public static final DataIndex<Byte> ABSTRACT_HORSE_FLAGS =                            new DataIndex<>(AbstractHorseFields.getDATA_ID_FLAGS());

	public static class DataIndex<T>
	{
		private final @Nullable EntityDataAccessor<T> accessor;

		public DataIndex(@Nullable EntityDataAccessor<T> accessor)
		{
			this.accessor = accessor;
			if (accessor == null) { MocapMod.LOGGER.error("Failed to initialize one of the data indexes!"); }
		}

		public void set(Entity entity, T val)
		{
			if (accessor != null) { entity.getEntityData().set(accessor, val); }
		}

		public T valOrDef(Entity entity, T defVal)
		{
			return accessor != null ? entity.getEntityData().get(accessor) : defVal;
		}
	}
}
