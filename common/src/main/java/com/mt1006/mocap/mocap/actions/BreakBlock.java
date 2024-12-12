package com.mt1006.mocap.mocap.actions;

import com.mt1006.mocap.mocap.files.RecordingData;
import com.mt1006.mocap.mocap.files.RecordingFiles;
import com.mt1006.mocap.mocap.playing.playback.ActionContext;
import com.mt1006.mocap.mocap.settings.Settings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

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

	@Override public void preExecute(Entity entity, Vec3i blockOffset)
	{
		previousBlockState.placeSilently(entity, blockPos.offset(blockOffset));
	}

	@Override public Result execute(ActionContext ctx)
	{
		ctx.level.destroyBlock(ctx.shiftBlockPos(blockPos), Settings.DROP_FROM_BLOCKS.val);
		return Result.OK;
	}
}
