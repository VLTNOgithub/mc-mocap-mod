package net.mt1006.mocap.mocap.actions.deprecated;

import net.mt1006.mocap.mocap.actions.Action;
import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;

public class HeadRotation implements Action
{
	private final float headRotY;

	public HeadRotation(RecordingFiles.Reader reader)
	{
		headRotY = reader.readFloat();
	}

	@Override public void write(RecordingFiles.Writer writer)
	{
		throw new RuntimeException("Trying to save deprecated action!");
	}

	@Override public Result execute(ActionContext ctx)
	{
		ctx.entity.setYHeadRot(headRotY);
		ctx.fluentMovement(() -> new ClientboundRotateHeadPacket(ctx.entity, (byte)Math.floor(headRotY * 256.0f / 360.0f)));
		return Result.OK;
	}
}
