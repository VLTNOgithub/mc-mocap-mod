package com.mt1006.mocap.mocap.playing;

import com.mt1006.mocap.command.io.CommandInfo;
import com.mt1006.mocap.command.io.CommandOutput;
import com.mt1006.mocap.mocap.playing.modifiers.PlayerData;
import com.mt1006.mocap.mocap.playing.playback.Playback;
import com.mt1006.mocap.mocap.recording.Recording;
import com.mt1006.mocap.mocap.recording.RecordingContext;
import com.mt1006.mocap.mocap.settings.Settings;

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

	public static boolean start(CommandInfo commandInfo, String name, PlayerData playerData)
	{
		if (name.charAt(0) == '-')
		{
			List<RecordingContext> contexts = Recording.resolveContexts(commandInfo, name);
			if (contexts == null) { return false; }

			int successes = 0;
			for (RecordingContext ctx : contexts)
			{
				Playback.Root playback = Playback.start(commandInfo, ctx.data, ctx.id.str, playerData, getNextId());
				if (playback == null) { continue; }
				playbacks.add(playback);
				successes++;
			}

			if (successes != 0) { commandInfo.sendSuccess("playback.start.success"); }
			return successes != 0;
		}
		else
		{
			Playback.Root playback = Playback.start(commandInfo, name, playerData, getNextId());
			if (playback == null) { return false; }
			playbacks.add(playback);
			commandInfo.sendSuccess("playback.start.success");
			return true;
		}
	}

	public static void stop(CommandInfo commandInfo, int id)
	{
		for (Playback.Root playback : playbacks)
		{
			if (playback.id == id)
			{
				playback.instance.stop();
				commandInfo.sendSuccess("playback.stop.success");
				return;
			}
		}

		commandInfo.sendFailureWithTip("playback.stop.unable_to_find_scene");
	}

	public static boolean stopAll(CommandOutput commandOutput)
	{
		playbacks.forEach((playback) -> playback.instance.stop());
		commandOutput.sendSuccess("playback.stop_all.success");
		return true;
	}

	public static boolean list(CommandInfo commandInfo)
	{
		commandInfo.sendSuccess("playback.list");

		for (Playback.Root playback : playbacks)
		{
			commandInfo.sendSuccessLiteral("[%d] %s", playback.id, playback.name);
		}
		return true;
	}

	public static void onTick()
	{
		if (!playbacks.isEmpty())
		{
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
