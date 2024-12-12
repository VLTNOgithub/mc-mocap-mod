package com.mt1006.mocap.mocap.playing.modifiers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mt1006.mocap.utils.Utils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

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

	public PlayerAsEntity(@Nullable JsonObject json)
	{
		if (json == null)
		{
			this.entityId = null;
			this.entityNbt = null;
			this.entityType = null;
			this.compoundTag = null;
			return;
		}

		JsonElement idElement = json.get("id");
		this.entityId = idElement != null ? idElement.getAsString() : null;

		JsonElement nbtElement = json.get("nbt");
		this.entityNbt = nbtElement != null ? nbtElement.getAsString() : null;

		this.entityType = prepareEntityType(entityId, entityNbt);
		this.compoundTag = prepareCompoundTag(entityId, entityNbt);
	}

	public @Nullable JsonObject toJson()
	{
		if (!isEnabled()) { return null; }

		JsonObject json = new JsonObject();
		if (entityId != null) { json.add("id", new JsonPrimitive(entityId)); }
		if (entityNbt != null) { json.add("nbt", new JsonPrimitive(entityNbt)); }

		return json;
	}

	public boolean isEnabled()
	{
		return entityId != null;
	}

	public @Nullable Entity createEntity(Level level)
	{
		if (entityType == null && compoundTag == null) { return null; }
		return (compoundTag != null)
				? EntityType.create(compoundTag, level).orElse(null)
				: entityType.create(level);
	}

	private static @Nullable EntityType<?> prepareEntityType(@Nullable String entityId, @Nullable String entityNbt)
	{
		if (entityId == null || entityNbt != null) { return null; } // if entityNbt is present, it should be null

		ResourceLocation entityRes = ResourceLocation.parse(entityId);
		EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityRes);
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
