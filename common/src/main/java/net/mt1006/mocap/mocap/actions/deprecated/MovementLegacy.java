package net.mt1006.mocap.mocap.actions.deprecated;

import net.minecraft.world.phys.Vec3;
import net.mt1006.mocap.mixin.fields.EntityFields;
import net.mt1006.mocap.mocap.actions.Action;
import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;

public class MovementLegacy implements Action
{
	//TODO: test with legacy recordings
	private final Vec3 position;
	private final float[] rotation = new float[2];
	private final boolean isOnGround;

	public MovementLegacy(RecordingFiles.Reader reader)
	{
		position = reader.readVec3();

		rotation[0] = reader.readFloat();
		rotation[1] = reader.readFloat();

		isOnGround = reader.readBoolean();
	}

	@Override public void write(RecordingFiles.Writer writer)
	{
		throw new RuntimeException("Trying to save deprecated action!");
	}

	@Override public Result execute(ActionContext ctx)
	{
		ctx.changePosition(position, rotation[1], rotation[0], true, true);

		ctx.entity.setOnGround(isOnGround);
		((EntityFields)ctx.entity).callCheckInsideBlocks();
		ctx.fluentMovement(() -> new ClientboundTeleportEntityPacket(ctx.entity));
		return Result.OK;
	}
}
