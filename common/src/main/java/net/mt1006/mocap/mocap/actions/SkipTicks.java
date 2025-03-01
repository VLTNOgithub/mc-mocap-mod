package net.mt1006.mocap.mocap.actions;

import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;

public class SkipTicks implements Action
{
	public final int number;

	public SkipTicks(int number)
	{
		if (number > 255) { throw new RuntimeException("Trying to skip more than 255 ticks!"); }
		this.number = number;
	}

	public SkipTicks(RecordingFiles.Reader reader)
	{
		this.number = Byte.toUnsignedInt(reader.readByte());
	}

	public boolean canBeModified()
	{
		return number < 255;
	}

	@Override public void write(RecordingFiles.Writer writer)
	{
		writer.addByte(Type.SKIP_TICKS.id);
		writer.addByte((byte)number);
		//TODO: test
	}

	@Override public Result execute(ActionContext ctx)
	{
		if (ctx.skippingTicks == number)
		{
			ctx.skippingTicks = 0;
			return Result.OK;
		}

		//MocapMod.LOGGER.warn("SKIP TICK (ST/{})", number); //TODO: remove
		ctx.skippingTicks++;
		return Result.REPEAT;
	}
}
