package net.mt1006.mocap.mocap.actions;

import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public class SetMainHand implements ComparableAction
{
	private final HumanoidArm mainHand;

	public SetMainHand(Entity entity)
	{
		mainHand = entity instanceof LivingEntity ? ((LivingEntity)entity).getMainArm() : HumanoidArm.RIGHT;
	}

	public SetMainHand(RecordingFiles.Reader reader)
	{
		mainHand = reader.readBoolean() ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
	}

	@Override public boolean differs(ComparableAction previousAction)
	{
		return mainHand != ((SetMainHand)previousAction).mainHand;
	}

	@Override public void write(RecordingFiles.Writer writer)
	{
		writer.addByte(Type.SET_MAIN_HAND.id);
		writer.addBoolean(mainHand == HumanoidArm.RIGHT);
	}

	@Override public Result execute(ActionContext ctx)
	{
		//TODO: test how it affects ghost player RightClickBlock
		if (ctx.entity instanceof Player) { ((Player)ctx.entity).setMainArm(mainHand); }
		else if (ctx.entity instanceof Mob) { ((Mob)ctx.entity).setLeftHanded(mainHand == HumanoidArm.LEFT); }
		else { return Result.IGNORED; }
		return Result.OK;
	}
}
