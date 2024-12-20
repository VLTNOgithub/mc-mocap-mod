package com.mt1006.mocap.mocap.playing;

import com.mt1006.mocap.command.CommandsContext;
import com.mt1006.mocap.command.io.CommandInfo;
import com.mt1006.mocap.command.io.CommandOutput;
import com.mt1006.mocap.mocap.files.SceneData;
import com.mt1006.mocap.mocap.files.SceneFiles;
import com.mt1006.mocap.mocap.playing.modifiers.PlaybackModifiers;
import com.mt1006.mocap.mocap.playing.playback.Playback;
import com.mt1006.mocap.mocap.recording.Recording;
import com.mt1006.mocap.mocap.recording.RecordingContext;
import com.mt1006.mocap.mocap.settings.Settings;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Playing
{
	public static final String MOCAP_ENTITY_TAG = "mocap_entity";
	public static final List<Playback.Root> playbacks = Collections.synchronizedList(new LinkedList<>());
	private static long tickCounter = 0;
	private static double timer = 0.0;
	private static double previousPlaybackSpeed = 0.0;

	public static boolean start(CommandInfo commandInfo, String name, PlaybackModifiers modifiers, boolean defaultModifiers)
	{
		if (name.charAt(0) == '-') { return startCurrentlyRecorded(commandInfo, name, modifiers, defaultModifiers); }

		Playback.Root playback = Playback.start(commandInfo, name, modifiers, getNextId());
		if (playback == null) { return false; }
		playbacks.add(playback);
		commandInfo.sendSuccess(defaultModifiers ? "playback.start.success" : "playback.start.success.modifiers");
		return true;
	}

	private static boolean startCurrentlyRecorded(CommandInfo commandInfo, String name, PlaybackModifiers modifiers, boolean defaultModifiers)
	{
		List<RecordingContext> contexts = Recording.resolveContexts(commandInfo, name);
		if (contexts == null) { return false; }

		int successes = 0;
		for (RecordingContext ctx : contexts)
		{
			Playback.Root playback = Playback.start(commandInfo, ctx.data, ctx.id.str, modifiers, getNextId());
			if (playback == null) { continue; }
			playbacks.add(playback);
			successes++;
		}

		if (successes == 0) { return false; }
		commandInfo.sendSuccess(defaultModifiers ? "playback.start.success" : "playback.start.success.modifiers");
		return true;
	}

	public static void stop(CommandOutput commandOutput, int id)
	{
		for (Playback.Root playback : playbacks)
		{
			if (playback.id == id)
			{
				playback.instance.stop();
				commandOutput.sendSuccess("playback.stop.success");
				return;
			}
		}

		commandOutput.sendFailureWithTip("playback.stop.unable_to_find_scene");
	}

	public static boolean stopAll(CommandOutput commandOutput)
	{
		playbacks.forEach((p) -> p.instance.stop());
		commandOutput.sendSuccess("playback.stop_all.success");
		return true;
	}

	public static boolean modifiersSet(CommandInfo rootCommandInfo)
	{
		ServerPlayer source = rootCommandInfo.sourcePlayer;
		if (source == null)
		{
			rootCommandInfo.sendFailure("failure.resolve_player");
			return false;
		}
		CommandsContext ctx = CommandsContext.get(source);

		CommandInfo commandInfo = rootCommandInfo.getFinalCommandInfo();
		if (commandInfo == null)
		{
			rootCommandInfo.sendFailure("error.unable_to_get_argument");
			return false;
		}

		String propertyName = commandInfo.getNode(4);
		if (propertyName == null)
		{
			rootCommandInfo.sendFailure("error.unable_to_get_argument");
			return false;
		}

		try
		{
			boolean success = ctx.modifiers.modify(commandInfo, propertyName, 4);
			if (!success)
			{
				//TODO: replace with "something went wrong" (alpha-3)
				rootCommandInfo.sendFailure("error.unable_to_get_argument");
				return false;
			}

			rootCommandInfo.sendSuccess("playback.modifiers.set");
			return true;
		}
		catch (Exception e)
		{
			rootCommandInfo.sendException(e, "error.unable_to_get_argument");
			return false;
		}
	}

	public static boolean modifiersList(CommandInfo commandInfo)
	{
		ServerPlayer source = commandInfo.sourcePlayer;
		if (source == null)
		{
			commandInfo.sendFailure("failure.resolve_player");
			return false;
		}

		CommandsContext ctx = CommandsContext.get(source);
		commandInfo.sendSuccess("playback.modifiers.list");
		ctx.modifiers.list(commandInfo);
		return true;
	}

	public static boolean modifiersReset(CommandInfo commandInfo)
	{
		ServerPlayer source = commandInfo.sourcePlayer;
		if (source == null)
		{
			commandInfo.sendFailure("failure.resolve_player");
			return false;
		}

		CommandsContext ctx = CommandsContext.get(source);
		ctx.modifiers = PlaybackModifiers.empty();
		commandInfo.sendSuccess("playback.modifiers.reset");
		return true;
	}

	public static boolean modifiersAddTo(CommandInfo commandInfo, String sceneName, String toAdd)
	{
		ServerPlayer source = commandInfo.sourcePlayer;
		if (source == null)
		{
			commandInfo.sendFailure("failure.resolve_player");
			return false;
		}

		SceneData.Subscene subscene = new SceneData.Subscene(toAdd, CommandsContext.get(source).modifiers);
		return SceneFiles.addElement(commandInfo, sceneName, subscene);
	}

	public static boolean list(CommandOutput commandOutput)
	{
		commandOutput.sendSuccess("playback.list");
		playbacks.forEach((p) -> commandOutput.sendSuccessLiteral("[%d] %s", p.id, p.name));
		return true;
	}

	public static void onTick()
	{
		if (playbacks.isEmpty())
		{
			tickCounter++;
			return;
		}

		if (previousPlaybackSpeed != Settings.PLAYBACK_SPEED.val)
		{
			timer = 0.0;
			previousPlaybackSpeed = Settings.PLAYBACK_SPEED.val;
		}

		if ((long)timer < tickCounter) { timer = tickCounter; }

		while ((long)timer == tickCounter)
		{
			ArrayList<Playback.Root> toRemove = new ArrayList<>();

			for (Playback.Root playback : playbacks)
			{
				if (playback.instance.isFinished()) { toRemove.add(playback); }
				else { playback.instance.tick(); }
			}

			playbacks.removeAll(toRemove);
			if (playbacks.isEmpty()) { break; }

			timer += 1.0 / Settings.PLAYBACK_SPEED.val;
		}
		tickCounter++;
	}

	private static int getNextId()
	{
		int maxInt = 1;
		for (Playback.Root playback : playbacks)
		{
			if (playback.id >= maxInt)
			{
				maxInt = playback.id + 1;
			}
		}
		return maxInt;
	}
}
