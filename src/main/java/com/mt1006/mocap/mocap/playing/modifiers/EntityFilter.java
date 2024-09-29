package com.mt1006.mocap.mocap.playing.modifiers;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class EntityFilter
{
	public static final EntityFilter FOR_RECORDING = new EntityFilter(Source.RECORDING_SETTING, null);
	public static final EntityFilter FOR_PLAYBACK = new EntityFilter(Source.PLAYBACK_SETTING, null);

	private static @Nullable EntityFilterInstance recordingSettingInstance, playbackSettingInstance;
	private final Source source;
	private final @Nullable EntityFilterInstance constantInstance;

	private EntityFilter(Source source, @Nullable String str)
	{
		this.source = source;
		if (source != Source.CONSTANT)
		{
			constantInstance = null;
			return;
		}

		if (str == null) { throw new RuntimeException("EntityFilter source is constant but string is null!"); }
		constantInstance = EntityFilterInstance.create(str);

		if (constantInstance == null)
		{
			//TODO: error message
		}
	}

	public static EntityFilter constant(String str)
	{
		return new EntityFilter(Source.CONSTANT, str);
	}

	public boolean isAllowed(Entity entity)
	{
		EntityFilterInstance instance = getInstance();
		return instance != null && instance.isAllowed(entity);
	}

	public boolean isEmpty()
	{
		EntityFilterInstance instance = getInstance();
		return instance == null || instance.isEmpty();
	}

	public boolean isDefaultForPlayback()
	{
		return source == Source.PLAYBACK_SETTING;
	}

	private @Nullable EntityFilterInstance getInstance()
	{
		return switch (source)
		{
			case RECORDING_SETTING -> recordingSettingInstance;
			case PLAYBACK_SETTING -> playbackSettingInstance;
			case CONSTANT -> constantInstance;
		};
	}

	public static void onTrackEntitiesSet(String str)
	{
		recordingSettingInstance = EntityFilterInstance.create(str);
	}

	public static void onPlaybackEntitiesSet(String str)
	{
		playbackSettingInstance = EntityFilterInstance.create(str);
	}

	private enum Source
	{
		RECORDING_SETTING, PLAYBACK_SETTING, CONSTANT
	}
}
