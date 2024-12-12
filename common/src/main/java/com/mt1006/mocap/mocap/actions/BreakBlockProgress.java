package com.mt1006.mocap.mocap.actions;

import com.mt1006.mocap.mocap.files.RecordingFiles;
import com.mt1006.mocap.mocap.playing.playback.ActionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;

public class BreakBlockProgress implements BlockAction
{
	private final BlockPos blockPos;
	private final int progress;

	public BreakBlockProgress(BlockPos blockPos, int progress)
	{
		this.blockPos = blockPos;
		this.progress = progress;
	}

	public BreakBlockProgress(RecordingFiles.Reader reader)
	{
		this.blockPos = reader.readBlockPos();
		this.progress = reader.readInt();
	}

	@Override public void write(RecordingFiles.Writer writer)
	{
		writer.addByte(Type.BREAK_BLOCK_PROGRESS.id);

		writer.addBlockPos(blockPos);
		writer.addInt(progress);
	}

	@Override public void preExecute(Entity entity, Vec3i blockOffset) {}

	@Override public Result execute(ActionContext ctx)
	{
		ctx.level.destroyBlockProgress(ctx.entity.getId(), ctx.shiftBlockPos(blockPos), progress);
		return Result.OK;
	}
}
