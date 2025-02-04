package com.mt1006.mocap.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mt1006.mocap.command.io.CommandOutput;
import com.mt1006.mocap.mocap.files.RecordingFiles;
import com.mt1006.mocap.mocap.files.SceneData;
import com.mt1006.mocap.mocap.files.SceneFiles;
import com.mt1006.mocap.mocap.playing.Playing;
import com.mt1006.mocap.mocap.playing.playback.Playback;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CommandSuggestions
{
	private static final int RECORDINGS = 1;
	private static final int SCENES = 2;
	private static final int CURRENTLY_RECORDED = 4;
	private static final int PLAYABLE = RECORDINGS | SCENES | CURRENTLY_RECORDED;

	public static final Set<String> inputSet = new HashSet<>();
	public static final Map<String, List<String>> sceneElementCache = new HashMap<>();

	private static CompletableFuture<Suggestions> inputSuggestions(SuggestionsBuilder builder, int suggestionFlags, boolean ignoreFirstChar)
	{
		String remaining = builder.getRemaining();
		for (String input : inputSet)
		{
			int type = switch (input.charAt(0))
			{
				case '.' -> SCENES;
				case '-' -> CURRENTLY_RECORDED;
				default -> RECORDINGS;
			};

			if ((suggestionFlags & type) != 0 && (input.startsWith(remaining)
					|| (ignoreFirstChar && input.substring(1).startsWith(remaining))))
			{
				builder.suggest(input);
			}
		}
		return builder.buildFuture();
	}

	public static CompletableFuture<Suggestions> recordingSuggestions(CommandContext<?> ctx, SuggestionsBuilder builder)
	{
		return inputSuggestions(builder, RECORDINGS, false);
	}

	public static CompletableFuture<Suggestions> sceneSuggestions(CommandContext<?> ctx, SuggestionsBuilder builder)
	{
		return inputSuggestions(builder, SCENES, true);
	}

	public static CompletableFuture<Suggestions> currentlyRecordedSuggestions(CommandContext<?> ctx, SuggestionsBuilder builder)
	{
		return inputSuggestions(builder, CURRENTLY_RECORDED, true);
	}

	public static CompletableFuture<Suggestions> playableArgument(CommandContext<?> ctx, SuggestionsBuilder builder)
	{
		return inputSuggestions(builder, PLAYABLE, false);
	}

	public static CompletableFuture<Suggestions> playbackIdSuggestions(CommandContext<?> ctx, SuggestionsBuilder builder)
	{
		String remaining = builder.getRemaining();
		for (Playback.Root playback : Playing.playbacks)
		{
			String str = playback.suggestionStr;
			if (str.startsWith(remaining)) { builder.suggest(str); }
		}
		return builder.buildFuture();
	}

	public static CompletableFuture<Suggestions> sceneElementSuggestion(CommandContext<?> ctx, SuggestionsBuilder builder)
	{
		String sceneName = StringArgumentType.getString(ctx, "scene_name");
		if (sceneName.isEmpty()) { return builder.buildFuture(); }
		if (sceneName.charAt(0) != '.') { sceneName = "." + sceneName; }

		if (!inputSet.contains(sceneName)) { return builder.buildFuture(); }
		List<String> elements = sceneElementCache.get(sceneName);

		if (elements == null)
		{
			SceneData sceneData = new SceneData();
			if (!sceneData.load(CommandOutput.LOGS, sceneName)) { builder.buildFuture(); }
			elements = sceneData.saveToSceneElementCache(sceneName);
		}
		if (elements == null) { return builder.buildFuture(); }

		String remaining = builder.getRemaining();
		for (String str : elements)
		{
			if (str.startsWith(remaining)) { builder.suggest(str); }
		}
		return builder.buildFuture();
	}

	public static void initInputSet()
	{
		inputSet.clear();

		List<String> recordingList = RecordingFiles.list();
		if (recordingList != null) { inputSet.addAll(recordingList); }

		List<String> sceneList = SceneFiles.list();
		if (sceneList != null) { inputSet.addAll(sceneList); }
	}
}
