package net.mt1006.mocap.mocap.actions;

import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;
import net.mt1006.mocap.utils.FakePlayer;

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
		if (ctx.entity instanceof FakePlayer) { ((FakePlayer)ctx.entity).fakeKill(); }
		else { ctx.entity.kill(null); }
		return Result.OK;
	}
}
