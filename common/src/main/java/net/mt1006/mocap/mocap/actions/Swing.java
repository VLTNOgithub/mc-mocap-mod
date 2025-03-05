package net.mt1006.mocap.mocap.actions;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;

public class Swing implements ComparableAction
{
	private final boolean swinging;
	private final int swingingTime;
	private final InteractionHand hand;

	public Swing(Entity entity)
	{
		if (entity instanceof LivingEntity)
		{
			LivingEntity livingEntity = (LivingEntity)entity;
			swinging = livingEntity.swinging;
			swingingTime = livingEntity.swingTime;
			hand = livingEntity.swingingArm;
		}
		else
		{
			swinging = false;
			swingingTime = 0;
			hand = InteractionHand.MAIN_HAND;
		}
	}

	public Swing(RecordingFiles.Reader reader)
	{
		swinging = true;
		swingingTime = 0;
		hand = reader.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
	}

	@Override public boolean differs(ComparableAction previousAction)
	{
		Swing previousSwing = (Swing)previousAction;
		return swinging && (previousSwing == null || !previousSwing.swinging || previousSwing.swingingTime > swingingTime);
	}

	@Override public boolean shouldBeInitialized()
	{
		return false;
	}

	@Override public void write(RecordingFiles.Writer writer)
	{
		writer.addByte(Type.SWING.id);
		writer.addBoolean(hand == InteractionHand.OFF_HAND);
	}

	@Override public Result execute(ActionContext ctx)
	{
		if (!(ctx.entity instanceof LivingEntity)) { return Result.IGNORED; }
		((LivingEntity)ctx.entity).swing(hand);
		return Result.OK;
	}
}
