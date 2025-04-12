package net.mt1006.mocap.mocap.playing.modifiers;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.mt1006.mocap.mocap.files.SceneFiles;
import net.mt1006.mocap.utils.Utils;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PlayerAsEntity
{
	public static final PlayerAsEntity DISABLED = new PlayerAsEntity(null, null);

	public final @Nullable String entityId;
	public final @Nullable String entityNbt;
	private final @Nullable EntityType<?> entityType;
	private final @Nullable CompoundTag compoundTag;

	public PlayerAsEntity(@Nullable String entityId, @Nullable String entityNbt)
	{
		this.entityId = entityId;
		this.entityNbt = entityNbt;
		this.entityType = prepareEntityType(entityId, entityNbt);
		this.compoundTag = prepareCompoundTag(entityId, entityNbt);
	}

	public PlayerAsEntity(@Nullable SceneFiles.Reader reader)
	{
		if (reader == null)
		{
			entityId = null;
			entityNbt = null;
			entityType = null;
			compoundTag = null;
			return;
		}

		entityId = reader.readString("id");
		entityNbt = reader.readString("nbt");
		this.entityType = prepareEntityType(entityId, entityNbt);
		this.compoundTag = prepareCompoundTag(entityId, entityNbt);
	}

	public @Nullable SceneFiles.Writer save()
	{
		if (!isEnabled()) { return null; }

		SceneFiles.Writer writer = new SceneFiles.Writer();
		writer.addString("id", entityId);
		writer.addString("nbt", entityNbt);

		return writer;
	}

	public boolean isEnabled()
	{
		return entityId != null;
	}

	public @Nullable Entity createEntity(Level level)
	{
		if (entityType == null && compoundTag == null) { return null; }
		return entityType.create(level, EntitySpawnReason.MOB_SUMMONED);
	}

	private static @Nullable EntityType<?> prepareEntityType(@Nullable String entityId, @Nullable String entityNbt)
	{
		if (entityId == null || entityNbt != null) { return null; } // if entityNbt is present, it should be null

		ResourceLocation entityRes = ResourceLocation.parse(entityId);
		Optional<Holder.Reference<EntityType<?>>> entityTypeHolder = BuiltInRegistries.ENTITY_TYPE.get(entityRes);
		EntityType entityType = entityTypeHolder.get().value();
		return BuiltInRegistries.ENTITY_TYPE.containsKey(entityRes) ? entityType : null;
	}

	private static @Nullable CompoundTag prepareCompoundTag(@Nullable String entityId, @Nullable String entityNbt)
	{
		if (entityId == null || entityNbt == null) { return null; }

		try
		{
			CompoundTag nbt = Utils.nbtFromString(entityNbt);
			nbt.putString("id", entityId);
			return nbt;
		}
		catch (CommandSyntaxException e) { return null; }
	}
}
