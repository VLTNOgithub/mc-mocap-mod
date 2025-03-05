package net.mt1006.mocap.mocap.actions;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;
import net.mt1006.mocap.utils.EntityData;

public class SetLivingEntityFlags implements ComparableAction
{
	private final byte livingEntityFlags;

	public SetLivingEntityFlags(Entity entity)
	{
		livingEntityFlags = (entity instanceof LivingEntity) ? EntityData.LIVING_ENTITY_FLAGS.valOrDef(entity, (byte)0) : 0;
	}

	public SetLivingEntityFlags(RecordingFiles.Reader reader)
	{
		livingEntityFlags = reader.readByte();
	}

	@Override public boolean differs(ComparableAction previousAction)
	{
		return livingEntityFlags != ((SetLivingEntityFlags)previousAction).livingEntityFlags;
	}

	@Override public void write(RecordingFiles.Writer writer)
	{
		writer.addByte(Type.SET_LIVING_ENTITY_FLAGS.id);
		writer.addByte(livingEntityFlags);
	}

	@Override public Result execute(ActionContext ctx)
	{
		if (!(ctx.entity instanceof LivingEntity)) { return Result.IGNORED; }
		EntityData.LIVING_ENTITY_FLAGS.set(ctx.entity, livingEntityFlags);
		return Result.OK;
	}
}
