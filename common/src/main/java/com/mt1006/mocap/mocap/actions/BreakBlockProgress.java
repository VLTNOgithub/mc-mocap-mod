package com.mt1006.mocap.mocap.actions;

import com.mt1006.mocap.mocap.files.RecordingFiles;
import com.mt1006.mocap.mocap.playing.modifiers.PlaybackModifiers;
import com.mt1006.mocap.mocap.playing.playback.ActionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

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

	@Override public void preExecute(Entity entity, PlaybackModifiers modifiers, Vec3 startPos) {}

	@Override public Result execute(ActionContext ctx)
	{
		double scale = ctx.modifiers.scale.sceneScale;
		BlockPos shiftedBlockPos = ctx.shiftBlockPos(blockPos);

		if (scale == 1.0)
		{
			ctx.level.destroyBlockProgress(ctx.entity.getId(), shiftedBlockPos, progress);
		}
		else if (BlockStateData.allowScaled(scale))
		{
			BlockStateData.scaledOperation(ctx.entity, shiftedBlockPos, ctx.startPos, scale,
					(entity, pos) -> ctx.level.destroyBlockProgress(entity.getId(), pos, progress));
		}
		return Result.OK;
	}
}
