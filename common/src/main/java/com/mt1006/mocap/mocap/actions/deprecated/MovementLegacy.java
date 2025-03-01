package com.mt1006.mocap.mocap.actions.deprecated;

import com.mt1006.mocap.mixin.fields.EntityFields;
import com.mt1006.mocap.mocap.actions.Action;
import com.mt1006.mocap.mocap.files.RecordingFiles;
import com.mt1006.mocap.mocap.playing.playback.ActionContext;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;

public class MovementLegacy implements Action
{
	//TODO: test with legacy recordings
	private final double[] position = new double[3];
	private final float[] rotation = new float[2];
	private final boolean isOnGround;

	public MovementLegacy(RecordingFiles.Reader reader)
	{
		position[0] = reader.readDouble();
		position[1] = reader.readDouble();
		position[2] = reader.readDouble();

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
		ctx.changePosition(position[0], position[1], position[2], rotation[1], rotation[0], true, true);

		ctx.entity.setOnGround(isOnGround);
		((EntityFields)ctx.entity).callCheckInsideBlocks();
		ctx.fluentMovement(() -> new ClientboundTeleportEntityPacket(ctx.entity));
		return Result.OK;
	}
}
