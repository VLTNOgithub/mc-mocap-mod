package com.mt1006.mocap.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mt1006.mocap.mocap.files.RecordingFiles;
import com.mt1006.mocap.mocap.files.SceneFiles;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class InputArgument
{
	private static final int RECORDINGS = 1;
	private static final int SCENES = 2;
	private static final int CURRENTLY_RECORDED = 4;
	private static final int PLAYABLE = RECORDINGS | SCENES | CURRENTLY_RECORDED;

	public static final HashSet<String> inputSet = new HashSet<>();

	private static CompletableFuture<Suggestions> inputArgument(SuggestionsBuilder builder, int suggestionFlags, boolean ignoreFirstChar)
	{
		for (String input : inputSet)
		{
			int type = switch (input.charAt(0))
			{
				case '.' -> SCENES;
				case '-' -> CURRENTLY_RECORDED;
				default -> RECORDINGS;
			};

			String remaining = builder.getRemaining();
			if ((suggestionFlags & type) != 0 && (input.startsWith(remaining)
					|| (ignoreFirstChar && input.substring(1).startsWith(remaining))))
			{
				builder.suggest(input);
			}
		}
		return builder.buildFuture();
	}

	public static CompletableFuture<Suggestions> recordingArgument(CommandContext<?> ctx, SuggestionsBuilder builder)
	{
		return inputArgument(builder, RECORDINGS, false);
	}

	public static CompletableFuture<Suggestions> sceneArgument(CommandContext<?> ctx, SuggestionsBuilder builder)
	{
		return inputArgument(builder, SCENES, true);
	}

	public static CompletableFuture<Suggestions> currentlyRecordedArgument(CommandContext<?> ctx, SuggestionsBuilder builder)
	{
		return inputArgument(builder, CURRENTLY_RECORDED, true);
	}

	public static CompletableFuture<Suggestions> playableArgument(CommandContext<?> ctx, SuggestionsBuilder builder)
	{
		return inputArgument(builder, PLAYABLE, false);
	}

	public static void initServerInputSet()
	{
		inputSet.clear();

		List<String> recordingList = RecordingFiles.list();
		if (recordingList != null) { inputSet.addAll(recordingList); }

		List<String> sceneList = SceneFiles.list();
		if (sceneList != null) { inputSet.addAll(sceneList); }
	}
}
