package net.mt1006.mocap.mocap.actions;

import net.mt1006.mocap.mocap.files.RecordingData;
import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;

public class EntityAction implements Action
{
	private final int id;
	private final Action action;

	public EntityAction(int id, Action action)
	{
		this.id = id;
		this.action = action;
	}

	public EntityAction(RecordingFiles.Reader reader, RecordingData data)
	{
		id = reader.readInt();
		action = Action.readAction(reader, data);
	}

	@Override public void prepareWrite(RecordingData data)
	{
		action.prepareWrite(data);
	}

	@Override public void write(RecordingFiles.Writer writer)
	{
		RecordingFiles.Writer actionWriter = new RecordingFiles.Writer(writer.parent);
		action.write(actionWriter);
		if (actionWriter.getByteList().isEmpty()) { return; }

		writer.addByte(Type.ENTITY_ACTION.id);
		writer.addInt(id);
		writer.addWriter(actionWriter);
	}

	@Override public Result execute(ActionContext ctx)
	{
		if (!ctx.setContextEntity(id)) { return Result.IGNORED; }
		Result retVal = action.execute(ctx);
		ctx.setMainContextEntity();
		return retVal;
	}
}
