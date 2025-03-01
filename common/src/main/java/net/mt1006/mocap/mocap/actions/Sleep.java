package net.mt1006.mocap.mocap.actions;

import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class Sleep implements ComparableAction
{
	private final @Nullable BlockPos bedPostion;

	public Sleep(Entity entity)
	{
		bedPostion = (entity instanceof LivingEntity) ? ((LivingEntity)entity).getSleepingPos().orElse(null) : null;
	}

	public Sleep(RecordingFiles.Reader reader)
	{
		bedPostion = reader.readBoolean() ? reader.readBlockPos() : null;
	}

	@Override public boolean differs(ComparableAction previousAction)
	{
		if (bedPostion == null && ((Sleep)previousAction).bedPostion == null) { return false; }
		if ((bedPostion == null) != (((Sleep)previousAction).bedPostion == null)) { return true; }
		return bedPostion != null && !bedPostion.equals(((Sleep)previousAction).bedPostion);
	}

	public void write(RecordingFiles.Writer writer)
	{
		writer.addByte(Type.SLEEP.id);

		if (bedPostion != null)
		{
			writer.addBoolean(true);
			writer.addBlockPos(bedPostion);
		}
		else
		{
			writer.addBoolean(false);
		}
	}

	@Override public Result execute(ActionContext ctx)
	{
		if (!(ctx.entity instanceof LivingEntity)) { return Result.IGNORED; }

		if (bedPostion != null) { ((LivingEntity)ctx.entity).setSleepingPos(bedPostion); }
		else { ((LivingEntity)ctx.entity).clearSleepingPos(); }
		return Result.OK;
	}
}
