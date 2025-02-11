package com.mt1006.mocap.mocap.actions;

import com.mt1006.mocap.mocap.files.RecordingData;
import com.mt1006.mocap.mocap.files.RecordingFiles;
import com.mt1006.mocap.mocap.playing.modifiers.PlaybackModifiers;
import com.mt1006.mocap.mocap.playing.playback.ActionContext;
import com.mt1006.mocap.mocap.settings.Settings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BreakBlock implements BlockAction
{
	private final BlockStateData previousBlockState;
	private final BlockPos blockPos;

	public BreakBlock(BlockState blockState, BlockPos blockPos)
	{
		this.previousBlockState = new BlockStateData(blockState);
		this.blockPos = blockPos;
	}

	public BreakBlock(RecordingFiles.Reader reader)
	{
		previousBlockState = new BlockStateData(reader);
		blockPos = reader.readBlockPos();
	}

	@Override public void prepareWrite(RecordingData data)
	{
		previousBlockState.prepareWrite(data);
	}

	@Override public void write(RecordingFiles.Writer writer)
	{
		writer.addByte(Type.BREAK_BLOCK.id);

		previousBlockState.write(writer);
		writer.addBlockPos(blockPos);
	}

	@Override public void preExecute(Entity entity, PlaybackModifiers modifiers, Vec3 startPos)
	{
		previousBlockState.placeSilently(entity, blockPos.offset(modifiers.offset.blockOffset),
				startPos, modifiers.scale.sceneScale);
	}

	@Override public Result execute(ActionContext ctx)
	{
		double scale = ctx.modifiers.scale.sceneScale;
		BlockPos shiftedBlockPos = ctx.shiftBlockPos(blockPos);

		if (scale == 1.0)
		{
			ctx.level.destroyBlock(shiftedBlockPos, Settings.DROP_FROM_BLOCKS.val);
		}
		else if (BlockStateData.allowScaled(scale))
		{
			BlockStateData.scaledOperation(ctx.entity, shiftedBlockPos, ctx.startPos, scale,
					(entity, pos) -> ctx.level.destroyBlock(pos, Settings.DROP_FROM_BLOCKS.val));
		}
		return Result.OK;
	}
}
