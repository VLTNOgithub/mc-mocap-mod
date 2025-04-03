package net.mt1006.mocap.mocap.actions.deprecated;

import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.mt1006.mocap.mocap.actions.Action;
import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;

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
		float finHeadRot = ctx.transformer.transformRotation(headRotY);
		ctx.entity.setYHeadRot(finHeadRot);
		ctx.fluentMovement(() -> new ClientboundRotateHeadPacket(ctx.entity, (byte)Math.floor(finHeadRot * 256.0f / 360.0f)));
		return Result.OK;
	}
}
