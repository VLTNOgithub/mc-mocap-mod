package net.mt1006.mocap.mocap.actions;

import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class SetArrowCount implements ComparableAction
{
	private final int arrowCount;
	private final int beeStingerCount;

	public SetArrowCount(Entity entity)
	{
		if (entity instanceof LivingEntity)
		{
			arrowCount = ((LivingEntity)entity).getArrowCount();
			beeStingerCount = ((LivingEntity)entity).getStingerCount();
		}
		else
		{
			arrowCount = 0;
			beeStingerCount = 0;
		}
	}

	public SetArrowCount(RecordingFiles.Reader reader)
	{
		arrowCount = reader.readInt();
		beeStingerCount = reader.readInt();
	}

	@Override public boolean differs(ComparableAction previousAction)
	{
		return arrowCount != ((SetArrowCount)previousAction).arrowCount
				|| beeStingerCount != ((SetArrowCount)previousAction).beeStingerCount;
	}

	@Override public void write(RecordingFiles.Writer writer)
	{
		writer.addByte(Type.SET_ARROW_COUNT.id);

		writer.addInt(arrowCount);
		writer.addInt(beeStingerCount);
	}

	@Override public Result execute(ActionContext ctx)
	{
		if (!(ctx.entity instanceof LivingEntity)) { return Result.IGNORED; }
		((LivingEntity)ctx.entity).setArrowCount(arrowCount);
		((LivingEntity)ctx.entity).setStingerCount(beeStingerCount);
		return Result.OK;
	}
}
