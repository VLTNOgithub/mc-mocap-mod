package net.mt1006.mocap.mocap.actions.deprecated;

import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import net.mt1006.mocap.mixin.fields.EntityFields;
import net.mt1006.mocap.mocap.actions.Action;
import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;

import java.util.Set;

public class MovementLegacy implements Action
{
	//TODO: test with legacy recordings
	private final Vec3 position;
	private final float[] rotation = new float[2];
	private final boolean isOnGround;

	public MovementLegacy(RecordingFiles.Reader reader)
	{
		position = reader.readVec3();

		rotation[0] = reader.readFloat();
		rotation[1] = reader.readFloat();

		isOnGround = reader.readBoolean();
	}

	@Override public void write(RecordingFiles.Writer writer)
	{
		throw new RuntimeException("Trying to save deprecated action!");
	}

	@Override public Result execute(ActionContext ctx)
	{
		Vec3 oldPos = ctx.entity.position();
		
		ctx.changePosition(position, rotation[1], rotation[0], true, true, true);

		ctx.entity.setOnGround(isOnGround);
		ctx.entity.applyEffectsFromBlocks(oldPos, ctx.entity.position());
		ctx.fluentMovement(() -> new ClientboundTeleportEntityPacket(ctx.entity.getId(), PositionMoveRotation.of(ctx.entity), Set.of(), isOnGround));
		return Result.OK;
	}
}
