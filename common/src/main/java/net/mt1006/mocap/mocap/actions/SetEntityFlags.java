package net.mt1006.mocap.mocap.actions;

import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;
import net.mt1006.mocap.utils.EntityData;
import net.minecraft.world.entity.Entity;

public class SetEntityFlags implements ComparableAction
{
	private final byte entityFlags;

	public SetEntityFlags(Entity entity)
	{
		//TODO: test fire
		this.entityFlags = EntityData.ENTITY_FLAGS.valOrDef(entity, (byte)0);
	}

	public SetEntityFlags(RecordingFiles.Reader reader)
	{
		entityFlags = reader.readByte();
	}

	@Override public boolean differs(ComparableAction previousAction)
	{
		return entityFlags != ((SetEntityFlags)previousAction).entityFlags;
	}

	public void write(RecordingFiles.Writer writer)
	{
		writer.addByte(Type.SET_ENTITY_FLAGS.id);
		writer.addByte(entityFlags);
	}

	@Override public Result execute(ActionContext ctx)
	{
		EntityData.ENTITY_FLAGS.set(ctx.entity, entityFlags);
		return Result.OK;
	}
}
