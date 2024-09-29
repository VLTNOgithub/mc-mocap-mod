package com.mt1006.mocap.mocap.actions;

import com.mt1006.mocap.mocap.files.RecordingData;
import com.mt1006.mocap.mocap.files.RecordingFiles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockAction extends Action
{
	void preExecute(Entity entity, Vec3i blockOffset);

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

		public void place(Entity entity, BlockPos blockPos)
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

		public void placeSilently(Entity entity, BlockPos blockPos)
		{
			entity.level().setBlock(blockPos, blockState, 3);
		}
	}
}
