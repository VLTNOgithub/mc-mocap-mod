package net.mt1006.mocap.mocap.actions;

import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;

public class Die implements Action
{
	public Die() {}

	public Die(RecordingFiles.Reader ignore) {}

	@Override public void write(RecordingFiles.Writer writer)
	{
		writer.addByte(Type.DIE.id);
	}

	@Override public Result execute(ActionContext ctx)
	{
		ctx.entity.kill();
		return Result.OK;
	}
}
