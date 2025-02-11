package com.mt1006.mocap.command.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.datafixers.util.Pair;
import com.mt1006.mocap.command.CommandSuggestions;
import com.mt1006.mocap.command.CommandUtils;
import com.mt1006.mocap.command.io.CommandInfo;
import com.mt1006.mocap.command.io.CommandOutput;
import com.mt1006.mocap.mocap.files.SceneData;
import com.mt1006.mocap.mocap.files.SceneFiles;
import com.mt1006.mocap.mocap.playing.modifiers.PlaybackModifiers;
import com.mt1006.mocap.mocap.playing.modifiers.StartDelay;
import com.mt1006.mocap.utils.Utils;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.List;

public class ScenesCommand
{
	private static final Command<CommandSourceStack> COMMAND_ADD_TO = CommandUtils.command(ScenesCommand::addTo);

	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder(CommandBuildContext buildContext)
	{
		LiteralArgumentBuilder<CommandSourceStack> commandBuilder = Commands.literal("scenes");

		commandBuilder.then(Commands.literal("add").then(CommandUtils.withStringArgument(SceneFiles::add, "name")));
		commandBuilder.then(Commands.literal("copy").then(CommandUtils.withInputAndStringArgument(SceneFiles::copy, CommandSuggestions::scene, "src_name", "dest_name")));
		commandBuilder.then(Commands.literal("rename").then(CommandUtils.withInputAndStringArgument(SceneFiles::rename, CommandSuggestions::scene, "old_name", "new_name")));
		commandBuilder.then(Commands.literal("remove").then(CommandUtils.withInputArgument(SceneFiles::remove, CommandSuggestions::scene, "name")));
		commandBuilder.then(Commands.literal("add_to").
			then(Commands.argument("scene_name", StringArgumentType.string()).suggests(CommandSuggestions::scene).
			then(Commands.argument("to_add", StringArgumentType.string()).suggests(CommandSuggestions::playable).executes(COMMAND_ADD_TO).
			then(Commands.argument("start_delay", DoubleArgumentType.doubleArg(0.0)).executes(COMMAND_ADD_TO).
			then(CommandUtils.playerArguments(buildContext, COMMAND_ADD_TO))))));
		commandBuilder.then(Commands.literal("remove_from").
			then(CommandUtils.withTwoInputArguments(SceneFiles::removeElement, CommandSuggestions::scene, CommandSuggestions::sceneElement, "scene_name", "to_remove")));
		commandBuilder.then(Commands.literal("modify").
			then(Commands.argument("scene_name", StringArgumentType.string()).suggests(CommandSuggestions::scene).
			then(CommandUtils.withModifiers(buildContext, Commands.argument("to_modify", StringArgumentType.string()).
				suggests(CommandSuggestions::sceneElement), CommandUtils.command(ScenesCommand::modify), true))));
		commandBuilder.then(Commands.literal("info").then(CommandUtils.withStringArgument(SceneFiles::info, "scene_name").suggests(CommandSuggestions::scene)).
			then(CommandUtils.withTwoInputArguments(SceneFiles::elementInfo, CommandSuggestions::scene, CommandSuggestions::sceneElement, "scene_name", "element_pos")));
		commandBuilder.then(Commands.literal("list").executes(CommandUtils.command(ScenesCommand::list)).
			then(CommandUtils.withInputArgument(SceneFiles::listElements, CommandSuggestions::scene, "scene_name")));

		return commandBuilder;
	}

	private static boolean addTo(CommandInfo commandInfo)
	{
		try
		{
			String name = commandInfo.getString("scene_name");
			String toAdd = commandInfo.getString("to_add");

			double delay = 0;
			try
			{
				delay = commandInfo.getDouble("start_delay");
			}
			catch (IllegalArgumentException ignore) {}

			PlaybackModifiers modifiers = commandInfo.getSimpleModifiers(commandInfo);
			if (modifiers == null) { return false; }
			modifiers.startDelay = StartDelay.fromSeconds(delay);

			SceneData.Subscene subscene = new SceneData.Subscene(toAdd, modifiers);
			return SceneFiles.addElement(commandInfo, name, subscene);
		}
		catch (IllegalArgumentException e)
		{
			commandInfo.sendException(e, "error.unable_to_get_argument");
			return false;
		}
	}

	private static boolean modify(CommandInfo commandInfo)
	{
		try
		{
			String name = commandInfo.getString("scene_name");
			Pair<Integer, String> posPair = CommandUtils.splitIdStr(commandInfo.getString("to_modify"));
			return SceneFiles.modify(commandInfo, name, posPair.getFirst(), posPair.getSecond());
		}
		catch (IllegalArgumentException e)
		{
			commandInfo.sendException(e, "error.unable_to_get_argument");
			return false;
		}
	}

	public static boolean list(CommandOutput commandOutput)
	{
		StringBuilder scenesListStr = new StringBuilder();
		List<String> scenesList = SceneFiles.list();

		if (scenesList == null)
		{
			scenesListStr.append(" ").append(Utils.stringFromComponent("list.error"));
		}
		else if (!scenesList.isEmpty())
		{
			scenesList.forEach((name) -> scenesListStr.append(" ").append(name));
		}
		else
		{
			scenesListStr.append(" ").append(Utils.stringFromComponent("list.empty"));
		}

		commandOutput.sendSuccess("scenes.list", new String(scenesListStr));
		return true;
	}
}
