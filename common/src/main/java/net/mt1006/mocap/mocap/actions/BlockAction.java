package net.mt1006.mocap.mocap.actions;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.mt1006.mocap.mocap.files.RecordingData;
import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.playback.PositionTransformer;

public interface BlockAction extends Action
{
	void preExecute(Entity entity, PositionTransformer transformer);

	class BlockStateData
	{
		private final BlockState blockState;
		private int idToWrite = -1;

		public BlockStateData(BlockState blockState)
		{
			this.blockState = blockState;
		}

		public BlockStateData(RecordingFiles.Reader reader)
		{
			RecordingData recordingData = reader.getParent();
			if (recordingData == null)
			{
				blockState = Blocks.AIR.defaultBlockState();
				return;
			}

			blockState = recordingData.blockStateIdMap.getObject(reader.readInt());
		}

		public void prepareWrite(RecordingData data)
		{
			idToWrite = data.blockStateIdMap.provideId(blockState);
		}

		public void write(RecordingFiles.Writer writer)
		{
			if (idToWrite == -1) { throw new RuntimeException("BlockStateData write wasn't prepared!"); }
			writer.addInt(idToWrite);
		}

		public void place(Entity entity, PositionTransformer transformer, BlockPos blockPos)
		{
			BlockState finBlockState = transformer.transformBlockState(blockState);
			transformer.transformBlockPos(blockPos).forEach((b) -> placeSingle(entity, b, finBlockState));
		}

		public void placeSilently(Entity entity, PositionTransformer transformer, BlockPos blockPos)
		{
			BlockState finBlockState = transformer.transformBlockState(blockState);
			transformer.transformBlockPos(blockPos).forEach((b) -> placeSingleSilently(entity, b, finBlockState));
		}

		private static void placeSingle(Entity entity, BlockPos blockPos, BlockState blockState)
		{
			Level level = entity.level();

			if (blockState.isAir())
			{
				level.destroyBlock(blockPos, true);
			}
			else
			{
				level.setBlock(blockPos, blockState, 3);

				SoundType soundType = blockState.getSoundType();
				level.playSound(entity, blockPos, blockState.getSoundType().getPlaceSound(),
						SoundSource.BLOCKS, (soundType.getVolume() + 1.0f) / 2.0f, soundType.getPitch() * 0.8f);
			}
		}

		private static void placeSingleSilently(Entity entity, BlockPos blockPos, BlockState blockState)
		{
			entity.level().setBlock(blockPos, blockState, 3);
		}
	}
}
