package net.mt1006.mocap.mocap.actions;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.mt1006.mocap.mixin.fields.EntityIdFields;
import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.Playing;
import net.mt1006.mocap.mocap.playing.modifiers.EntityFilter;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;
import net.mt1006.mocap.mocap.settings.Settings;
import net.mt1006.mocap.utils.Utils;
import org.jetbrains.annotations.Nullable;

public class EntityUpdate implements Action
{
	private final UpdateType type;
	private final int id;
	private final @Nullable String nbtString;
	private final @Nullable Vec3 position;

	public static EntityUpdate addEntity(int id, Entity entity)
	{
		String nbtString = serializeEntityNBT(entity).toString();
		return new EntityUpdate(UpdateType.ADD, id, nbtString, entity.position());
	}

	public static EntityUpdate removeEntity(int id)
	{
		return new EntityUpdate(UpdateType.REMOVE, id, null, null);
	}

	public static EntityUpdate kill(int id)
	{
		return new EntityUpdate(UpdateType.KILL, id, null, null);
	}

	public static EntityUpdate hurt(int id)
	{
		return new EntityUpdate(UpdateType.HURT, id, null, null);
	}

	public static EntityUpdate playerMount(int id)
	{
		return new EntityUpdate(UpdateType.PLAYER_MOUNT, id, null, null);
	}

	public static EntityUpdate playerDismount()
	{
		return new EntityUpdate(UpdateType.PLAYER_DISMOUNT, 0, null, null);
	}

	private EntityUpdate(UpdateType type, int id, @Nullable String nbtString, @Nullable Vec3 position)
	{
		this.type = type;
		this.id = id;
		this.nbtString = nbtString;
		this.position = position;
	}

	public EntityUpdate(RecordingFiles.Reader reader)
	{
		type = UpdateType.fromId(reader.readByte());
		id = reader.readInt();

		if (type == UpdateType.ADD)
		{
			nbtString = reader.readString();
			position = reader.readVec3();
		}
		else
		{
			nbtString = null;
			position = null;
		}
	}

	public static CompoundTag serializeEntityNBT(Entity entity)
	{
		CompoundTag compoundTag = new CompoundTag();

		String id = ((EntityIdFields)entity).callGetEncodeId();
		compoundTag.putString("id", id != null ? id : "minecraft:cow");

		entity.saveWithoutId(compoundTag);
		compoundTag.remove("UUID");
		compoundTag.remove("Pos");
		compoundTag.remove("Motion");
		return compoundTag;
	}

	@Override public void write(RecordingFiles.Writer writer)
	{
		writer.addByte(Type.ENTITY_UPDATE.id);

		writer.addByte(type.id);
		writer.addInt(id);

		if (type == UpdateType.ADD)
		{
			writer.addString(nbtString != null ? nbtString : "");
			if (position != null)
			{
				writer.addVec3(position);
			}
		}
	}

	@Override public Result execute(ActionContext ctx)
	{
		switch (type)
		{
			case ADD:
				return executeAdd(ctx);

			case PLAYER_DISMOUNT:
				ctx.entity.stopRiding();
				return Result.OK;

			case NONE:
				return Result.IGNORED;
		}

		ActionContext.EntityData entityData = ctx.entityDataMap.get(id);
		if (entityData == null) { return Result.IGNORED; }
		Entity entity = entityData.entity;

		switch (type)
		{
			case REMOVE:
				entity.remove(Entity.RemovalReason.KILLED);
				return Result.OK;

			case KILL:
				entity.invulnerableTime = 0; // for sound effect
				entity.kill();
				return Result.OK;

			case HURT:
				Hurt.hurtEntity(entity);
				return Result.OK;

			case PLAYER_MOUNT:
				ctx.entity.startRiding(entity, true);
				return Result.OK;
		}
		return Result.IGNORED;
	}

	private Result executeAdd(ActionContext ctx)
	{
		EntityFilter filter = ctx.modifiers.entityFilter;
		if (nbtString == null || position == null || ctx.entityDataMap.containsKey(id) || filter.isEmpty()) { return Result.IGNORED; }

		CompoundTag nbt;
		try
		{
			nbt = Utils.nbtFromString(nbtString);
		}
		catch (Exception e)
		{
			Utils.exception(e, "Exception occurred when parsing entity NBT data!");
			return Result.ERROR;
		}

		Entity entity = EntityType.create(nbt, ctx.level).orElse(null);
		if (entity == null || !filter.isAllowed(entity)) { return Result.IGNORED; }

		entity.setPos(ctx.transformer.transformPos(position));
		entity.setDeltaMovement(0.0, 0.0, 0.0);
		entity.setNoGravity(true);
		entity.setInvulnerable(Settings.INVULNERABLE_PLAYBACK.val);
		entity.addTag(Playing.MOCAP_ENTITY_TAG);
		if (entity instanceof Mob) { ((Mob)entity).setNoAi(true); }
		ctx.modifiers.transformations.scale.applyToEntity(entity);

		ctx.level.addFreshEntity(entity);
		ctx.entityDataMap.put(id, new ActionContext.EntityData(entity, position));
		return Result.OK;
	}

	public enum UpdateType
	{
		NONE(0),
		ADD(1),
		REMOVE(2),
		KILL(3),
		HURT(4),
		PLAYER_MOUNT(5),
		PLAYER_DISMOUNT(6);

		private static final UpdateType[] VALUES = values();
		private final byte id;

		UpdateType(int id)
		{
			this.id = (byte)id;
		}

		private static UpdateType fromId(byte id)
		{
			for (UpdateType type : VALUES)
			{
				if (type.id == id) { return type; }
			}
			return NONE;
		}
	}
}
