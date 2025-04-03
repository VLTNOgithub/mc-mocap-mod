package net.mt1006.mocap.mocap.playing.modifiers;

import net.mt1006.mocap.command.io.CommandInfo;
import net.mt1006.mocap.command.io.CommandOutput;
import net.mt1006.mocap.mocap.files.SceneFiles;
import org.jetbrains.annotations.Nullable;

public class TransformationsConfig
{
	public static final TransformationsConfig DEFAULT = new TransformationsConfig(false, RecordingCenter.AUTO, SceneCenter.DEFAULT, Offset.ZERO);
	public static final TransformationsConfig LEGACY = new TransformationsConfig(true, RecordingCenter.AUTO, SceneCenter.DEFAULT, Offset.ZERO);

	public final boolean roundBlockPos;
	public final RecordingCenter recordingCenter;
	public final SceneCenter sceneCenter;
	public final Offset centerOffset;

	public TransformationsConfig(boolean roundBlockPos, RecordingCenter recordingCenter, SceneCenter sceneCenter, Offset centerOffset)
	{
		this.roundBlockPos = roundBlockPos;
		this.recordingCenter = recordingCenter;
		this.sceneCenter = sceneCenter;
		this.centerOffset = centerOffset;
	}

	public TransformationsConfig(SceneFiles.Reader reader)
	{
		roundBlockPos = reader.readBoolean("round_block_pos", false);
		recordingCenter = reader.readEnum("recording_center", RecordingCenter.AUTO);
		sceneCenter = SceneCenter.fromObject(reader.readObject("scene_center"));
		centerOffset = Offset.fromVec3(reader.readVec3("center_offset"));
	}

	public static TransformationsConfig fromObject(@Nullable SceneFiles.Reader reader)
	{
		return reader != null ? new TransformationsConfig(reader) : DEFAULT;
	}

	public boolean isDefault()
	{
		return !roundBlockPos && recordingCenter == RecordingCenter.AUTO && centerOffset.isZero
				&& sceneCenter.type == SceneCenterType.COMMON_FIRST;
	}

	public @Nullable SceneFiles.Writer save()
	{
		if (isDefault()) { return null; }

		SceneFiles.Writer writer = new SceneFiles.Writer();
		writer.addBoolean("round_block_pos", roundBlockPos, false);
		writer.addEnum("recording_center", recordingCenter, RecordingCenter.AUTO);
		writer.addObject("scene_center", sceneCenter.save());
		writer.addVec3("center_offset", centerOffset.save());

		return writer;
	}

	public void list(CommandOutput commandOutput)
	{
		if (isDefault())
		{
			commandOutput.sendSuccess("scenes.element_info.transformations.default_config");
			return;
		}

		commandOutput.sendSuccess("scenes.element_info.transformations.round_block_pos." + roundBlockPos);
		commandOutput.sendSuccess("scenes.element_info.transformations.recording_center", recordingCenter.name());
		commandOutput.sendSuccess("scenes.element_info.transformations.scene_center", sceneCenter.toString());
		commandOutput.sendSuccess("scenes.element_info.transformations.center_offset", centerOffset.x, centerOffset.y, centerOffset.z);
	}

	public @Nullable TransformationsConfig modify(CommandInfo commandInfo, String propertyName, int propertyNodePosition)
	{
		switch (propertyName)
		{
			case "round_block_pos":
				return new TransformationsConfig(commandInfo.getBool("round"), recordingCenter, sceneCenter, centerOffset);

			case "recording_center":
				String centerPointStr = commandInfo.getNode(propertyNodePosition + 1);
				if (centerPointStr == null) { break; }

				return new TransformationsConfig(roundBlockPos, RecordingCenter.valueOf(centerPointStr.toUpperCase()), sceneCenter, centerOffset);

			case "scene_center":
				String sceneCenterStr = commandInfo.getNode(propertyNodePosition + 1);
				if (sceneCenterStr == null) { break; }

				SceneCenterType centerType = SceneCenterType.valueOf(sceneCenterStr.toUpperCase());
				SceneCenter center = centerType == SceneCenterType.COMMON_SPECIFIC
						? new SceneCenter(centerType, commandInfo.getString("specific_scene_element"))
						: new SceneCenter(centerType, null);

				return new TransformationsConfig(roundBlockPos, recordingCenter, center, centerOffset);

			case "center_offset":
				return new TransformationsConfig(roundBlockPos, recordingCenter, sceneCenter,
						new Offset(commandInfo.getDouble("offset_x"), commandInfo.getDouble("offset_y"), commandInfo.getDouble("offset_z")));
		}
		return null;
	}

	public enum RecordingCenter
	{
		AUTO,
		BLOCK_CENTER,
		BLOCK_CORNER,
		ACTUAL
	}

	public enum SceneCenterType
	{
		COMMON_FIRST,
		COMMON_LAST,
		COMMON_SPECIFIC,
		INDIVIDUAL
	}

	public static class SceneCenter
	{
		public static final SceneCenter DEFAULT = new SceneCenter(SceneCenterType.COMMON_FIRST, null);
		public final SceneCenterType type;
		public final @Nullable String specificStr;

		public SceneCenter(SceneCenterType type, @Nullable String specificStr)
		{
			this.type = type;
			this.specificStr = specificStr;
		}

		public SceneCenter(SceneFiles.Reader reader)
		{
			type = reader.readEnum("type", SceneCenterType.COMMON_FIRST);
			specificStr = (type == SceneCenterType.COMMON_SPECIFIC) ? reader.readString("specific_str") : null;
		}

		public static SceneCenter fromObject(@Nullable SceneFiles.Reader reader)
		{
			return reader != null ? new SceneCenter(reader) : DEFAULT;
		}

		public @Nullable SceneFiles.Writer save()
		{
			if (type == SceneCenterType.COMMON_FIRST) { return null; }

			SceneFiles.Writer writer = new SceneFiles.Writer();
			writer.addEnum("type", type, SceneCenterType.COMMON_FIRST);
			if (type == SceneCenterType.COMMON_SPECIFIC) { writer.addString("specific_str", specificStr); }

			return writer;
		}

		@Override public String toString()
		{
			return (type == SceneCenterType.COMMON_SPECIFIC)
					? String.format("%s [%s]", type.name(), specificStr)
					: type.name();
		}
	}
}
