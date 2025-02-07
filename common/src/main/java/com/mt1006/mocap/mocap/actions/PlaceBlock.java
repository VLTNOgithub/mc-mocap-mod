package com.mt1006.mocap.mocap.actions;

import com.mt1006.mocap.mocap.files.RecordingData;
import com.mt1006.mocap.mocap.files.RecordingFiles;
import com.mt1006.mocap.mocap.playing.modifiers.PlaybackModifiers;
import com.mt1006.mocap.mocap.playing.playback.ActionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class PlaceBlock implements BlockAction
{
	private final BlockStateData previousBlockState;
	private final BlockStateData newBlockState;
	private final BlockPos blockPos;

	public PlaceBlock(BlockState previousBlockState, BlockState newBlockState, BlockPos blockPos)
	{
		this.previousBlockState = new BlockStateData(previousBlockState);
		this.newBlockState = new BlockStateData(newBlockState);
		this.blockPos = blockPos;
	}

	public PlaceBlock(RecordingFiles.Reader reader)
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
		writer.addByte(Type.PLACE_BLOCK.id);

		previousBlockState.write(writer);
		newBlockState.write(writer);

		writer.addBlockPos(blockPos);
	}

	@Override public void preExecute(Entity entity, PlaybackModifiers modifiers, Vec3 startPos)
	{
		previousBlockState.placeSilently(entity, blockPos.offset(modifiers.offset.blockOffset),
				startPos, modifiers.scale.sceneScale);
	}

	@Override public Result execute(ActionContext ctx)
	{
		newBlockState.place(ctx.entity, ctx.shiftBlockPos(blockPos),
				ctx.startPos, ctx.modifiers.scale.sceneScale);
		return Result.OK;
	}
}
