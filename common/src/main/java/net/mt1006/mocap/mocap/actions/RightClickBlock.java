package net.mt1006.mocap.mocap.actions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;
import net.mt1006.mocap.mocap.playing.playback.PositionTransformer;

public class RightClickBlock implements BlockAction
{
	private final BlockHitResult blockHitResult;
	private final boolean offHand;

	public RightClickBlock(BlockHitResult blockHitResult, boolean offHand)
	{
		this.blockHitResult = blockHitResult;
		this.offHand = offHand;
	}

	public RightClickBlock(RecordingFiles.Reader reader)
	{
		Vec3 pos = reader.readVec3();
		BlockPos blockPos = reader.readBlockPos();
		Direction direction = directionFromByte(reader.readByte());
		boolean inside = reader.readBoolean();

		blockHitResult = new BlockHitResult(pos, direction, blockPos, inside);
		offHand = reader.readBoolean();
	}

	private Direction directionFromByte(byte val)
	{
		return switch (val)
		{
			case 1 -> Direction.UP;
			case 2 -> Direction.NORTH;
			case 3 -> Direction.SOUTH;
			case 4 -> Direction.WEST;
			case 5 -> Direction.EAST;
			default -> Direction.DOWN;
		};
	}

	private byte directionToByte(Direction direction)
	{
		return switch (direction)
		{
			case UP -> 1;
			case NORTH -> 2;
			case SOUTH -> 3;
			case WEST -> 4;
			case EAST -> 5;
			default -> 0;
		};
	}

	@Override public void write(RecordingFiles.Writer writer)
	{
		writer.addByte(Type.RIGHT_CLICK_BLOCK.id);

		writer.addVec3(blockHitResult.getLocation());
		writer.addBlockPos(blockHitResult.getBlockPos());

		writer.addByte(directionToByte(blockHitResult.getDirection()));
		writer.addBoolean(blockHitResult.isInside());

		writer.addBoolean(offHand);
	}

	@Override public void preExecute(Entity entity, PositionTransformer transformer) {}

	@Override public Result execute(ActionContext ctx)
	{
		//TODO: test!
		Player player = (ctx.entity instanceof Player) ? (Player)ctx.entity : ctx.ghostPlayer;
		if (player == null) { return Result.IGNORED; }

		InteractionHand interactionHand = offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		ItemStack itemStack = player.getItemInHand(interactionHand);

		for (BlockPos blockPos : ctx.transformer.transformBlockPos(blockHitResult.getBlockPos()))
		{
			BlockState blockState = ctx.level.getBlockState(blockPos);
			if (blockState.getBlock() instanceof BedBlock) { continue; }

			ItemInteractionResult result = blockState.useItemOn(itemStack, ctx.level, player, interactionHand, blockHitResult);
			if (result == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION)
			{
				blockState.useWithoutItem(ctx.level, player, blockHitResult);
			}
		}
		return Result.OK;
	}
}
