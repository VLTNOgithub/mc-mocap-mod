package net.mt1006.mocap.mocap.actions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.mt1006.mocap.mocap.files.RecordingData;
import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;
import net.mt1006.mocap.mocap.playing.playback.PositionTransformer;

public class PlaceBlockSilently implements BlockAction
{
	private final BlockStateData previousBlockState;
	private final BlockStateData newBlockState;
	private final BlockPos blockPos;

	public PlaceBlockSilently(BlockState previousBlockState, BlockState newBlockState, BlockPos blockPos)
	{
		this.previousBlockState = new BlockStateData(previousBlockState);
		this.newBlockState = new BlockStateData(newBlockState);
		this.blockPos = blockPos;
	}

	public PlaceBlockSilently(RecordingFiles.Reader reader)
	{
		previousBlockState = new BlockStateData(reader);
		newBlockState = new BlockStateData(reader);
		blockPos = reader.readBlockPos();
	}

	@Override public void prepareWrite(RecordingData data)
	{
		previousBlockState.prepareWrite(data);
		newBlockState.prepareWrite(data);
	}

	@Override public void write(RecordingFiles.Writer writer)
	{
		writer.addByte(Type.PLACE_BLOCK_SILENTLY.id);

		previousBlockState.write(writer);
		newBlockState.write(writer);

		writer.addBlockPos(blockPos);
	}

	@Override public void preExecute(Entity entity, PositionTransformer transformer)
	{
		previousBlockState.placeSilently(entity, transformer, blockPos);
	}

	@Override public Result execute(ActionContext ctx)
	{
		newBlockState.placeSilently(ctx.entity, ctx.transformer, blockPos);
		return Result.OK;
	}
}
