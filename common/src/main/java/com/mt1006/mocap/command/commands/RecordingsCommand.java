package com.mt1006.mocap.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mt1006.mocap.command.CommandSuggestions;
import com.mt1006.mocap.command.CommandUtils;
import com.mt1006.mocap.command.io.CommandOutput;
import com.mt1006.mocap.mocap.files.RecordingFiles;
import com.mt1006.mocap.utils.Utils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.List;

public class RecordingsCommand
{
	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder()
	{
		LiteralArgumentBuilder<CommandSourceStack> commandBuilder = Commands.literal("recordings");

		commandBuilder.then(Commands.literal("copy").then(CommandUtils.withInputAndStringArgument(RecordingFiles::copy, CommandSuggestions::recordingSuggestions, "src_name", "dest_name")));
		commandBuilder.then(Commands.literal("rename").then(CommandUtils.withInputAndStringArgument(RecordingFiles::rename, CommandSuggestions::recordingSuggestions, "old_name", "new_name")));
		commandBuilder.then(Commands.literal("remove").then(CommandUtils.withInputArgument(RecordingFiles::remove, CommandSuggestions::recordingSuggestions, "name")));
		commandBuilder.then(Commands.literal("info").then(CommandUtils.withInputArgument(RecordingFiles::info, CommandSuggestions::recordingSuggestions, "name")));
		commandBuilder.then(Commands.literal("list").executes(CommandUtils.command(RecordingsCommand::list)));

		return commandBuilder;
	}

	public static boolean list(CommandOutput commandOutput)
	{
		StringBuilder recordingsListStr = new StringBuilder();
		List<String> recordingsList = RecordingFiles.list();

		if (recordingsList == null)
		{
			recordingsListStr.append(" ").append(Utils.stringFromComponent("list.error"));
		}
		else if (!recordingsList.isEmpty())
		{
			recordingsList.forEach((name) -> recordingsListStr.append(" ").append(name));
		}
		else
		{
			recordingsListStr.append(" ").append(Utils.stringFromComponent("list.empty"));
		}

		commandOutput.sendSuccess("recordings.list", new String(recordingsListStr));
		return true;
	}
}
