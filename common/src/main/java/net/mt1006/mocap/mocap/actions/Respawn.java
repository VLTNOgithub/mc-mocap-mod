package net.mt1006.mocap.mocap.actions;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.mt1006.mocap.mixin.fields.EntityFields;
import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;
import net.mt1006.mocap.utils.FakePlayer;

import java.util.List;
import java.util.UUID;

public class Respawn implements Action
{
	/*private final Vec3 pos;
	private final float rotY, rotX;
	private final @Nullable ResourceLocation dimensionId;

	public Respawn(Vec3 pos, float rotY, float rotX, @Nullable ResourceLocation dimensionId)
	{
		this.pos = pos;
		this.rotY = rotY;
		this.rotX = rotX;
		this.dimensionId = dimensionId;
	}

	public Respawn(RecordingFiles.Reader reader)
	{
		pos = reader.readVec3();
		rotY = reader.readFloat();
		rotX = reader.readFloat();

		//TODO: test when bad input
		String dimensionStr = reader.readString();
		dimensionId = dimensionStr.isEmpty() ? null : ResourceLocation.parse(dimensionStr);
	}

	@Override public void write(RecordingFiles.Writer writer)
	{
		writer.addByte(Type.RESPAWN.id);

		writer.addVec3(pos);
		writer.addFloat(rotY);
		writer.addFloat(rotX);
		writer.addString(dimensionId != null ? dimensionId.toString() : "");
	}*/

	public Respawn() {}

	public Respawn(RecordingFiles.Reader reader) {}

	@Override public void write(RecordingFiles.Writer writer)
	{
		writer.addByte(Type.RESPAWN.id);
	}

	@Override public Result execute(ActionContext ctx)
	{
		ctx.entity.setPose(Pose.STANDING);

		if (ctx.entity instanceof LivingEntity)
		{
			LivingEntity entity = (LivingEntity)ctx.entity;
			entity.setHealth(entity.getMaxHealth());
			entity.deathTime = 0;
		}

		if (ctx.entity instanceof FakePlayer)
		{
			UUID uuid = ctx.entity.getUUID();
			ctx.broadcast(new ClientboundPlayerInfoRemovePacket(List.of(uuid)));
			ctx.level.removePlayerImmediately((FakePlayer)ctx.entity, Entity.RemovalReason.KILLED);

			((FakePlayer)ctx.entity).fakeRespawn();
			ctx.level.getServer().getPlayerList()
					.broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, (FakePlayer)ctx.entity));
			ctx.level.addNewPlayer((FakePlayer)ctx.entity);
		}
		else
		{
			((EntityFields)ctx.entity).callUnsetRemoved();
			ctx.level.addFreshEntity(ctx.entity);
		}
		return Result.OK;
	}
}
