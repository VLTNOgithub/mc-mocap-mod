package com.mt1006.mocap.command.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mt1006.mocap.command.CommandUtils;
import com.mt1006.mocap.command.io.CommandInfo;
import com.mt1006.mocap.command.io.CommandOutput;
import com.mt1006.mocap.mocap.files.SceneData;
import com.mt1006.mocap.mocap.files.SceneFiles;
import com.mt1006.mocap.utils.Utils;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.Registries;

import java.util.List;

public class ScenesCommand
{
	private static final Command<CommandSourceStack> COMMAND_ADD_TO = CommandUtils.command(ScenesCommand::addTo);
	private static final Command<CommandSourceStack> COMMAND_MODIFY = CommandUtils.command(ScenesCommand::modify);

	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder(CommandBuildContext buildContext)
	{
		LiteralArgumentBuilder<CommandSourceStack> commandBuilder = Commands.literal("scenes");

		commandBuilder.then(Commands.literal("add").then(CommandUtils.withStringArgument(SceneFiles::add, "name")));
		commandBuilder.then(Commands.literal("copy").then(CommandUtils.withTwoStringArguments(SceneFiles::copy, "src_name", "dest_name")));
		commandBuilder.then(Commands.literal("rename").then(CommandUtils.withTwoStringArguments(SceneFiles::rename, "old_name", "new_name")));
		commandBuilder.then(Commands.literal("remove").then(CommandUtils.withStringArgument(SceneFiles::remove, "name")));
		commandBuilder.then(Commands.literal("add_to").
			then(Commands.argument("scene_name", StringArgumentType.string()).
			then(Commands.argument("to_add", StringArgumentType.string()).executes(COMMAND_ADD_TO).
			then(Commands.argument("start_delay", DoubleArgumentType.doubleArg(0.0)).executes(COMMAND_ADD_TO).
			then(CommandUtils.withPlayerArguments(COMMAND_ADD_TO))))));
		commandBuilder.then(Commands.literal("remove_from").
			then(CommandUtils.withStringAndIntArgument(SceneFiles::removeElement, "scene_name", "to_remove")));
		commandBuilder.then(Commands.literal("modify").
			then(Commands.argument("scene_name", StringArgumentType.string()).
			then(Commands.argument("to_modify", IntegerArgumentType.integer()).
			then(Commands.literal("subscene_name").then(Commands.argument("new_name", StringArgumentType.string()).executes(COMMAND_MODIFY))).
			then(Commands.literal("start_delay").then(Commands.argument("delay", DoubleArgumentType.doubleArg(0.0)).executes(COMMAND_MODIFY))).
			then(Commands.literal("position_offset").
				then(Commands.argument("offset_x", DoubleArgumentType.doubleArg()).
				then(Commands.argument("offset_y", DoubleArgumentType.doubleArg()).
				then(Commands.argument("offset_z", DoubleArgumentType.doubleArg()).executes(COMMAND_MODIFY))))).
			then(Commands.literal("player_info").then(CommandUtils.withPlayerArguments(COMMAND_MODIFY))).
			then(Commands.literal("player_as_entity").
				then(Commands.literal("disabled").executes(COMMAND_MODIFY)).
				then(Commands.literal("enabled").
					then(Commands.argument("entity", ResourceArgument.resource(buildContext, Registries.ENTITY_TYPE)).executes(COMMAND_MODIFY)))))));
		commandBuilder.then(Commands.literal("info").then(CommandUtils.withStringArgument(SceneFiles::info, "scene_name")).
			then(CommandUtils.withStringAndIntArgument(SceneFiles::elementInfo, "scene_name", "element_pos")));
		commandBuilder.then(Commands.literal("list").executes(CommandUtils.command(ScenesCommand::list)).
			then(CommandUtils.withStringArgument(SceneFiles::listElements, "scene_name")));

		return commandBuilder;
	}

	private static boolean addTo(CommandInfo commandInfo)
	{
		try
		{
			String name = commandInfo.getString("scene_name");

			String toAdd = commandInfo.getString("to_add");
			SceneData.Subscene subscene = new SceneData.Subscene(toAdd);

			try
			{
				subscene.startDelay = commandInfo.getDouble("start_delay");
				subscene.playerData = commandInfo.getPlayerData();

				if (subscene.playerData.name != null)
				{
					//TODO: move
					if (subscene.playerData.name.length() > 16)
					{
						commandInfo.sendFailure("scenes.add_to.failed");
						commandInfo.sendFailure("scenes.add_to.failed.too_long_name");
						return false;
					}

					if (subscene.playerData.name.contains(" "))
					{
						commandInfo.sendFailure("scenes.add_to.failed");
						commandInfo.sendFailure("scenes.add_to.failed.contain_spaces");
						return false;
					}
				}
			}
			catch (Exception ignore) {}

			return SceneFiles.addElement(commandInfo, name, subscene.sceneToStr());
		}
		catch (IllegalArgumentException exception)
		{
			commandInfo.sendException(exception, "error.unable_to_get_argument");
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
		catch (IllegalArgumentException exception)
		{
			commandInfo.sendException(exception, "error.unable_to_get_argument");
			return false;
		}
	}

	public static boolean list(CommandOutput commandOutput)
	{
		StringBuilder scenesListStr = new StringBuilder();
		List<String> scenesList = SceneFiles.list(commandOutput);

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
