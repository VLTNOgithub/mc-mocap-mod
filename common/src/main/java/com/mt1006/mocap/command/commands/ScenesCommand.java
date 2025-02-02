package com.mt1006.mocap.command.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mt1006.mocap.command.CommandUtils;
import com.mt1006.mocap.command.InputArgument;
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
	private static final Command<CommandSourceStack> COMMAND_MODIFY = CommandUtils.command(ScenesCommand::modify);

	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder(CommandBuildContext buildContext)
	{
		LiteralArgumentBuilder<CommandSourceStack> commandBuilder = Commands.literal("scenes");

		commandBuilder.then(Commands.literal("add").then(CommandUtils.withStringArgument(SceneFiles::add, "name")));
		commandBuilder.then(Commands.literal("copy").then(CommandUtils.withInputAndStringArgument(SceneFiles::copy, InputArgument::sceneArgument, "src_name", "dest_name")));
		commandBuilder.then(Commands.literal("rename").then(CommandUtils.withInputAndStringArgument(SceneFiles::rename, InputArgument::sceneArgument, "old_name", "new_name")));
		commandBuilder.then(Commands.literal("remove").then(CommandUtils.withInputArgument(SceneFiles::remove, InputArgument::sceneArgument, "name")));
		commandBuilder.then(Commands.literal("add_to").
			then(Commands.argument("scene_name", StringArgumentType.string()).suggests(InputArgument::sceneArgument).
			then(Commands.argument("to_add", StringArgumentType.string()).suggests(InputArgument::playableArgument).executes(COMMAND_ADD_TO).
			then(Commands.argument("start_delay", DoubleArgumentType.doubleArg(0.0)).executes(COMMAND_ADD_TO).
			then(CommandUtils.playerArguments(buildContext, COMMAND_ADD_TO))))));
		commandBuilder.then(Commands.literal("remove_from").
			then(CommandUtils.withInputAndIntArgument(SceneFiles::removeElement, InputArgument::sceneArgument, "scene_name", "to_remove")));
		commandBuilder.then(Commands.literal("modify").
			then(Commands.argument("scene_name", StringArgumentType.string()).suggests(InputArgument::sceneArgument).
			then(CommandUtils.withModifiers(buildContext, Commands.argument("to_modify", IntegerArgumentType.integer()), COMMAND_MODIFY, true))));
		commandBuilder.then(Commands.literal("info").then(CommandUtils.withStringArgument(SceneFiles::info, "scene_name")).
			then(CommandUtils.withInputAndIntArgument(SceneFiles::elementInfo, InputArgument::sceneArgument, "scene_name", "element_pos")));
		commandBuilder.then(Commands.literal("list").executes(CommandUtils.command(ScenesCommand::list)).
			then(CommandUtils.withInputArgument(SceneFiles::listElements, InputArgument::sceneArgument, "scene_name")));

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
			int pos = commandInfo.getInteger("to_modify");

			return SceneFiles.modify(commandInfo, name, pos);
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
