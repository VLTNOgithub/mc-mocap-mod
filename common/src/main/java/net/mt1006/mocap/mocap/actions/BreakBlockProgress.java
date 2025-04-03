package net.mt1006.mocap.mocap.actions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;
import net.mt1006.mocap.mocap.playing.playback.PositionTransformer;

import java.util.List;

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

	@Override public void preExecute(Entity entity, PositionTransformer transformer) {}

	@Override public Result execute(ActionContext ctx)
	{
		List<BlockPos> blocks = ctx.transformer.transformBlockPos(blockPos);
		if (!blocks.isEmpty()) { ctx.level.destroyBlockProgress(ctx.entity.getId(), blocks.get(0), progress); }
		return Result.OK;
	}
}
