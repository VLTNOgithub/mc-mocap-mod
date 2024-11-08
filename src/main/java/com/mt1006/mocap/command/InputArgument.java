package com.mt1006.mocap.command;

import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Pair;
import com.mt1006.mocap.command.io.CommandOutput;
import com.mt1006.mocap.events.PlayerConnectionEvent;
import com.mt1006.mocap.mocap.files.RecordingFiles;
import com.mt1006.mocap.mocap.files.SceneFiles;
import com.mt1006.mocap.network.MocapPacketS2C;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class InputArgument
{
	private static final int RECORDINGS = 1;
	private static final int SCENES = 2;
	private static final int CURRENTLY_RECORDED = 4;
	private static final int PLAYABLE = RECORDINGS | SCENES | CURRENTLY_RECORDED;

	public static final HashSet<String> serverInputSet = new HashSet<>();
	public static final HashSet<String> clientInputSet = new HashSet<>();

	public static @Nullable CompletableFuture<Suggestions> getSuggestions(CommandContextBuilder<?> rootCtx, String fullCommand, int cursor)
	{
		CommandContextBuilder<?> ctx = CommandUtils.getFinalCommandContext(rootCtx);
		if (ctx == null) { return null; }

		String subcommand1 = CommandUtils.getNode(ctx, 1);
		String subcommand2 = CommandUtils.getNode(ctx, 2);
		if (subcommand1 == null || subcommand2 == null) { return null; }

		String subcommand = String.format("%s/%s", subcommand1, subcommand2);

		List<Pair<Integer, Integer>> args = List.of();

		switch (subcommand)
		{
			case "recording/stop":
				args = List.of(Pair.of(3, CURRENTLY_RECORDED));
				break;

			case "playback/start":
				args = List.of(Pair.of(3, PLAYABLE));
				break;

			case "recordings/copy":
			case "recordings/rename":
			case "recordings/remove":
			case "recordings/info":
				args = List.of(Pair.of(3, RECORDINGS));
				break;

			case "scenes/copy":
			case "scenes/rename":
			case "scenes/remove":
			case "scenes/remove_from":
			case "scenes/list": //TODO: test!
			case "scenes/info":
				args = List.of(Pair.of(3, SCENES));
				break;

			case "scenes/add_to":
				args = List.of(Pair.of(3, SCENES), Pair.of(4, PLAYABLE));
				break;
		}

		if (subcommand.equals("scenes/modify"))
		{
			String paramToModify = CommandUtils.getNode(ctx, 5);
			args = (paramToModify != null && paramToModify.equals("subscene_name"))
					? List.of(Pair.of(3, SCENES), Pair.of(6, PLAYABLE))
					: List.of(Pair.of(3, SCENES));
		}

		int suggestionFlags = 0;
		int suggestionPos = 0;
		String prefix = "";

		for (Pair<Integer, Integer> arg : args)
		{
			StringRange stringRange = getStringRange(ctx, arg.getFirst());

			if (stringRange == null)
			{
				StringRange previousStringRange = getStringRange(ctx, arg.getFirst() - 1);
				if (previousStringRange == null || cursor <= previousStringRange.getEnd()) { continue; }

				suggestionFlags = arg.getSecond();
				suggestionPos = cursor;
				prefix = "";
				break;
			}
			else if (cursor >= stringRange.getStart() && cursor <= stringRange.getEnd())
			{
				suggestionFlags = arg.getSecond();
				suggestionPos = stringRange.getStart();
				prefix = stringRange.get(fullCommand);
				break;
			}
		}

		if (suggestionFlags == 0) { return null; }
		SuggestionsBuilder builder = new SuggestionsBuilder(fullCommand, suggestionPos);

		for (String input : clientInputSet)
		{
			int type = switch (input.charAt(0))
			{
				case '.' -> SCENES;
				case '-' -> CURRENTLY_RECORDED;
				default -> RECORDINGS;
			};
			if ((suggestionFlags & type) != 0 && input.startsWith(prefix)) { builder.suggest(input); }
		}
		return builder.buildFuture();
	}

	public static void initServerInputSet()
	{
		serverInputSet.clear();

		List<String> recordingList = RecordingFiles.list(CommandOutput.DUMMY);
		if (recordingList != null) { serverInputSet.addAll(recordingList); }

		List<String> sceneList = SceneFiles.list(CommandOutput.DUMMY);
		if (sceneList != null) { serverInputSet.addAll(sceneList); }
	}

	public static void addServerInput(String name)
	{
		serverInputSet.add(name);
		PlayerConnectionEvent.players.forEach((player) -> MocapPacketS2C.sendInputSuggestionsAdd(player, List.of(name)));
	}

	public static void removeServerInput(String name)
	{
		serverInputSet.remove(name);
		PlayerConnectionEvent.players.forEach((player) -> MocapPacketS2C.sendInputSuggestionsRemove(player, List.of(name)));
	}

	private static @Nullable StringRange getStringRange(CommandContextBuilder<?> ctx, int pos)
	{
		if (ctx.getNodes().size() <= pos || pos < 0) { return null; }
		return ctx.getNodes().get(pos).getRange();
	}
}
