package net.mt1006.mocap.mocap.actions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.mt1006.mocap.mocap.files.RecordingData;
import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;
import net.mt1006.mocap.mocap.playing.playback.PositionTransformer;
import net.mt1006.mocap.mocap.settings.Settings;

import java.util.List;

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

	@Override public void preExecute(Entity entity, PositionTransformer transformer)
	{
		previousBlockState.placeSilently(entity, transformer, blockPos);
	}

	@Override public Result execute(ActionContext ctx)
	{
		List<BlockPos> blocks = ctx.transformer.transformBlockPos(blockPos);
		blocks.forEach((b) -> ctx.level.destroyBlock(b, Settings.DROP_FROM_BLOCKS.val));
		return Result.OK;
	}
}
