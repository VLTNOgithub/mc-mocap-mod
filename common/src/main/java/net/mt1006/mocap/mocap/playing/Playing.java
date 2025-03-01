package net.mt1006.mocap.mocap.playing;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.mt1006.mocap.command.CommandsContext;
import net.mt1006.mocap.command.io.CommandInfo;
import net.mt1006.mocap.command.io.CommandOutput;
import net.mt1006.mocap.mocap.files.SceneData;
import net.mt1006.mocap.mocap.files.SceneFiles;
import net.mt1006.mocap.mocap.playing.modifiers.PlaybackModifiers;
import net.mt1006.mocap.mocap.playing.playback.Playback;
import net.mt1006.mocap.mocap.recording.Recording;
import net.mt1006.mocap.mocap.recording.RecordingContext;
import net.mt1006.mocap.mocap.settings.Settings;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Playing
{
	public static final String MOCAP_ENTITY_TAG = "mocap_entity";
	public static final Multimap<String, Playback.Root> playbacksByOwner = HashMultimap.create();
	public static final Collection<Playback.Root> playbacks = playbacksByOwner.values();
	private static long tickCounter = 0;
	private static double timer = 0.0;
	private static double previousPlaybackSpeed = 0.0;

	public static boolean start(CommandInfo commandInfo, String name, PlaybackModifiers modifiers, boolean defaultModifiers)
	{
		if (name.charAt(0) == '-') { return startCurrentlyRecorded(commandInfo, name, modifiers, defaultModifiers); }

		Playback.Root playback = Playback.start(commandInfo, name, modifiers, getMaxId() + 1);
		if (playback == null) { return false; }
		addPlayback(playback);
		sendStartMessage(commandInfo, defaultModifiers);
		return true;
	}

	private static boolean startCurrentlyRecorded(CommandInfo commandInfo, String name, PlaybackModifiers modifiers, boolean defaultModifiers)
	{
		Collection<RecordingContext> contexts = Recording.resolveContexts(commandInfo, name);
		if (contexts == null) { return false; }

		int successes = 0;
		for (RecordingContext ctx : contexts)
		{
			PlaybackModifiers modifiersToApply = modifiers;
			if (Settings.START_AS_RECORDED.val)
			{
				PlaybackModifiers playerNameModifier = PlaybackModifiers.empty();
				playerNameModifier.playerName = ctx.recordedPlayer.getName().getString();
				modifiersToApply = modifiers.mergeWithParent(playerNameModifier);
			}

			Playback.Root playback = Playback.start(commandInfo, ctx.data, ctx.id.str, modifiersToApply, getMaxId() + 1);
			if (playback == null) { continue; }
			addPlayback(playback);
			successes++;
		}

		if (successes == 0) { return false; }
		sendStartMessage(commandInfo, defaultModifiers);
		return true;
	}

	private static void sendStartMessage(CommandInfo commandInfo, boolean defaultModifiers)
	{
		String key = "playback.start.success";
		if (!defaultModifiers) { key += ".modifiers"; }

		if (commandInfo.sourcePlayer != null)
		{
			CommandsContext commandsContext = CommandsContext.get(commandInfo.sourcePlayer);
			if (commandsContext.getSync()) { key += ".sync"; }
		}

		commandInfo.sendSuccess(key);
	}

	public static void stop(CommandOutput commandOutput, int id, @Nullable String expectedName)
	{
		for (Playback.Root playback : playbacks)
		{
			if (playback.id == id)
			{
				if (expectedName != null && !expectedName.equals(playback.name))
				{
					commandOutput.sendFailure("playback.stop.wrong_playback_name");
					return;
				}

				playback.instance.stop();
				commandOutput.sendSuccess("playback.stop.success");
				return;
			}
		}

		commandOutput.sendFailureWithTip("playback.stop.unable_to_find_playback");
	}

	public static void stopAll(CommandOutput commandOutput, @Nullable ServerPlayer player)
	{
		if (player == null)
		{
			playbacks.forEach((p) -> p.instance.stop());
			commandOutput.sendSuccess(playbacks.isEmpty() ? "playback.stop_all.empty": "playback.stop_all.all");
		}
		else
		{
			Collection<Playback.Root> playerPlaybacks = playbacksByOwner.get(player.getName().getString());
			playerPlaybacks.forEach((p) -> p.instance.stop());

			if (playerPlaybacks.isEmpty())
			{
				commandOutput.sendSuccess(playbacks.isEmpty()
						? "playback.stop_all.empty"
						: "playback.stop_all.own.empty");
			}
			else
			{
				commandOutput.sendSuccess(playerPlaybacks.size() == playbacks.size()
						? "playback.stop_all.own.all"
						: "playback.stop_all.own.not_all");
			}

			if (playerPlaybacks.size() != playbacks.size()) { commandOutput.sendSuccess("playback.stop_all.own.tip"); }
		}
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
				rootCommandInfo.sendFailure("error.generic");
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
			List<Playback.Root> toRemove = new ArrayList<>();

			for (Playback.Root playback : playbacks)
			{
				if (playback.instance.isFinished()) { toRemove.add(playback); }
				else { playback.instance.tick(); }
			}

			removePlaybacks(toRemove);
			if (playbacks.isEmpty()) { break; }

			timer += 1.0 / Settings.PLAYBACK_SPEED.val;
		}
		tickCounter++;
	}

	private static void addPlayback(Playback.Root playback)
	{
		ServerPlayer owner = playback.instance.owner;
		playbacksByOwner.put(owner != null ? owner.getName().getString() : "", playback);
	}

	private static void removePlaybacks(Collection<Playback.Root> toRemove)
	{
		for (Playback.Root playback : toRemove)
		{
			ServerPlayer owner = playback.instance.owner;
			playbacksByOwner.remove(owner != null ? owner.getName().getString() : "", playback);
		}
	}

	public static int getMaxId()
	{
		int maxInt = 0;
		for (Playback.Root playback : playbacks)
		{
			if (playback.id > maxInt)
			{
				maxInt = playback.id;
			}
		}
		return maxInt;
	}
}
