package com.mt1006.mocap.mocap.playing.playback;

import com.mt1006.mocap.command.io.CommandInfo;
import com.mt1006.mocap.mocap.files.RecordingData;
import com.mt1006.mocap.mocap.files.SceneData;
import com.mt1006.mocap.mocap.playing.DataManager;
import com.mt1006.mocap.mocap.playing.modifiers.PlaybackModifiers;
import com.mt1006.mocap.mocap.playing.modifiers.PlayerSkin;
import com.mt1006.mocap.mocap.recording.Recording;
import com.mt1006.mocap.mocap.recording.RecordingContext;
import com.mt1006.mocap.mocap.settings.Settings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public abstract class Playback
{
	//TODO: "playback.start.error.loop" and "playback.start.error.load" as failures, not errors

	protected final boolean root;
	protected final ServerLevel level;
	protected final @Nullable ServerPlayer owner;
	protected boolean finished = false;

	protected final PlaybackModifiers modifiers;
	protected int tickCounter = 0; //TODO: StartContext?

	public static @Nullable Root start(CommandInfo commandInfo, String name, @Nullable String playerName, PlayerSkin playerSkin, int id)
	{
		DataManager dataManager = new DataManager();
		if (!dataManager.load(commandInfo, name))
		{
			if (!dataManager.knownError) { commandInfo.sendFailure("playback.start.error.load"); }
			commandInfo.sendFailure("playback.start.error.load.path", dataManager.getResourcePath());
			return null;
		}

		Playback playback = switch (SceneType.fromName(name))
		{
			case RECORDING -> RecordingPlayback.startRoot(commandInfo, dataManager, name, playerName, playerSkin);
			case SCENE -> ScenePlayback.startRoot(commandInfo, dataManager, name, playerName, playerSkin);
		};
		return playback != null ? new Root(playback, id, name) : null;
	}

	public static @Nullable Root start(CommandInfo commandInfo, RecordingData recordingData, String name,
									   @Nullable String playerName, PlayerSkin playerSkin, int id)
	{
		Playback playback = RecordingPlayback.startRoot(commandInfo, recordingData, playerName, playerSkin);
		return playback != null ? new Root(playback, id, name) : null;
	}

	protected static @Nullable Playback start(CommandInfo commandInfo, DataManager dataManager, Playback parent, SceneData.Subscene info)
	{
		String name = info.name;
		return switch (SceneType.fromName(name))
		{
			case RECORDING -> RecordingPlayback.startSubscene(commandInfo, dataManager, parent, info);
			case SCENE -> ScenePlayback.startSubscene(commandInfo, dataManager, parent, info);
		};
	}

	protected Playback(boolean root, ServerLevel level, @Nullable ServerPlayer owner, @Nullable String rootPlayerName,
					   @Nullable PlayerSkin rootPlayerSkin, @Nullable Playback parent, @Nullable SceneData.Subscene info)
	{
		this.root = root;
		this.level = level;
		this.owner = owner;

		if (root)
		{
			if (rootPlayerSkin == null || parent != null || info != null) { throw new RuntimeException(); }
			this.modifiers = PlaybackModifiers.forRoot(rootPlayerName, rootPlayerSkin);
		}
		else
		{
			if (rootPlayerSkin != null || parent == null || info == null) { throw new RuntimeException(); }
			this.modifiers = PlaybackModifiers.fromParent(parent.modifiers, info.playerName, info.playerSkin,
					info.playerAsEntity, info.offset, info.startDelay, info.entityFilter);
		}
	}

	public abstract boolean tick();

	public abstract void stop();

	public abstract boolean isFinished();

	protected boolean shouldExecuteTick()
	{
		if (tickCounter == 0) { return true; }

		if (modifiers.startDelay <= tickCounter)
		{
			if (!Settings.RECORDING_SYNCHRONIZATION.val) { return true; }

			for (RecordingContext ctx : Recording.getContexts())
			{
				//TODO: add player-specific check
				if (ctx.state == RecordingContext.State.RECORDING) { return true; }
			}
		}
		return false;
	}

	public static class Root
	{
		public final Playback instance;
		public final int id;
		public final String name;

		public Root(Playback instance, int id, String name)
		{
			this.instance = instance;
			this.id = id;
			this.name = name;
		}
	}

	public static class StartException extends Exception {}

	private enum SceneType
	{
		SCENE,
		RECORDING;

		public static SceneType fromName(String name)
		{
			return name.charAt(0) == '.' ? SCENE : RECORDING;
		}
	}
}
