package com.mt1006.mocap.mocap.actions;

import com.mt1006.mocap.mocap.files.RecordingData;
import com.mt1006.mocap.mocap.files.RecordingFiles;
import com.mt1006.mocap.mocap.playing.modifiers.PlaybackModifiers;
import com.mt1006.mocap.mocap.settings.Settings;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;

public interface BlockAction extends Action
{
	void preExecute(Entity entity, PlaybackModifiers modifiers, Vec3 startPos);

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

		public void place(Entity entity, BlockPos blockPos, Vec3 startPos, double scale)
		{
			if (scale == 1.0) { placeReal(entity, blockPos); }
			else if (allowScaled(scale)) { scaledOperation(entity, blockPos, startPos, scale, this::placeReal); }
		}

		public void placeSilently(Entity entity, BlockPos blockPos, Vec3 startPos, double scale)
		{
			if (scale == 1.0) { placeRealSilently(entity, blockPos); }
			else if (allowScaled(scale)) { scaledOperation(entity, blockPos, startPos, scale, this::placeRealSilently); }
		}

		private void placeReal(Entity entity, BlockPos blockPos)
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

		private void placeRealSilently(Entity entity, BlockPos blockPos)
		{
			entity.level().setBlock(blockPos, blockState, 3);
		}

		public static void scaledOperation(Entity entity, BlockPos blockPos, Vec3 startPos,
										   double scale, BiConsumer<Entity, BlockPos> func)
		{
			int n = (int)scale;
			BlockPos startBlockPos = new BlockPos(Mth.floor(startPos.x), Mth.floor(startPos.y), Mth.floor(startPos.z));
			BlockPos finalBlockPos = blockPos.subtract(startBlockPos).multiply(n).offset(startBlockPos);

			int bx = finalBlockPos.getX(), by = finalBlockPos.getY(), bz = finalBlockPos.getZ();
			for (int y = by; y < by + n; y++)
			{
				int cornerX = bx - (n / 2);
				for (int x = cornerX; x < cornerX + n; x++)
				{
					int cornerZ = bz - (n / 2);
					for (int z = cornerZ; z < cornerZ + n; z++)
					{
						func.accept(entity, new BlockPos(x, y, z));
					}
				}
			}
		}

		public static boolean allowScaled(double scale)
		{
			return (scale == (int)scale && Settings.BLOCK_ALLOW_SCALED.val);
		}
	}
}
