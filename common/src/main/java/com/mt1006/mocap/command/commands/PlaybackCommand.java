package com.mt1006.mocap.command.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mt1006.mocap.command.CommandSuggestions;
import com.mt1006.mocap.command.CommandUtils;
import com.mt1006.mocap.command.CommandsContext;
import com.mt1006.mocap.command.io.CommandInfo;
import com.mt1006.mocap.mocap.playing.Playing;
import com.mt1006.mocap.mocap.playing.modifiers.PlaybackModifiers;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class PlaybackCommand
{
	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder(CommandBuildContext buildContext)
	{
		LiteralArgumentBuilder<CommandSourceStack> commandBuilder = Commands.literal("playback");

		commandBuilder.then(Commands.literal("start").
			then(Commands.argument("name", StringArgumentType.string()).
				suggests(CommandSuggestions::playableArgument).executes(CommandUtils.command(PlaybackCommand::start)).
			then(CommandUtils.playerArguments(buildContext, CommandUtils.command(PlaybackCommand::start)))));
		commandBuilder.then(Commands.literal("stop").
			then(Commands.argument("id", StringArgumentType.string()).
				suggests(CommandSuggestions::playbackIdSuggestions).executes(CommandUtils.command(PlaybackCommand::stop))));
		commandBuilder.then(Commands.literal("stop_all").executes(CommandUtils.command((info) -> PlaybackCommand.stopAll(info, false))).
			then(Commands.literal("including_others").executes(CommandUtils.command((info) -> PlaybackCommand.stopAll(info, true)))).
			then(Commands.literal("excluding_others").executes(CommandUtils.command((info) -> PlaybackCommand.stopAll(info, false)))));
		commandBuilder.then(Commands.literal("modifiers").
			then(CommandUtils.withModifiers(buildContext, Commands.literal("set"), CommandUtils.command(Playing::modifiersSet), false)).
			then(Commands.literal("list").executes(CommandUtils.command(Playing::modifiersList))).
			then(Commands.literal("reset").executes(CommandUtils.command(Playing::modifiersReset))).
			then(Commands.literal("add_to").
				then(Commands.argument("scene_name", StringArgumentType.string()).suggests(CommandSuggestions::sceneSuggestions).
				then(Commands.argument("to_add", StringArgumentType.string()).suggests(CommandSuggestions::playableArgument).
					executes(CommandUtils.command(PlaybackCommand::modifiersAddTo))))));
		commandBuilder.then(Commands.literal("list").executes(CommandUtils.command(Playing::list)));

		return commandBuilder;
	}

	private static boolean start(CommandInfo commandInfo)
	{
		String name = commandInfo.getNullableString("name");
		if (name == null)
		{
			commandInfo.sendFailure("error.unable_to_get_argument");
			return false;
		}

		PlaybackModifiers modifiers = commandInfo.getSimpleModifiers(commandInfo);
		if (modifiers == null) { return false; }

		try
		{
			PlaybackModifiers finalModifiers = CommandsContext.getFinalModifiers(commandInfo.sourcePlayer, modifiers);
			boolean hasDefaultModifiers = CommandsContext.hasDefaultModifiers(commandInfo.sourcePlayer);
			return Playing.start(commandInfo, name, finalModifiers, hasDefaultModifiers);
		}
		catch (Exception e)
		{
			commandInfo.sendException(e, "playback.start.error");
			return false;
		}
	}

	private static boolean stop(CommandInfo commandInfo)
	{
		try
		{
			String idStr = commandInfo.getString("id");
			int dashPos = idStr.indexOf('-');
			int id = Integer.parseInt(dashPos != -1 ? idStr.substring(0, dashPos) : idStr);
			String expectedName = dashPos != -1 ? idStr.substring(dashPos + 1) : null;

			Playing.stop(commandInfo, id, expectedName);
			return true;
		}
		catch (IllegalArgumentException e)
		{
			commandInfo.sendException(e, "error.unable_to_get_argument");
			return false;
		}
	}

	private static boolean stopAll(CommandInfo commandInfo, boolean includeOthers)
	{
		Playing.stopAll(commandInfo, includeOthers ? null : commandInfo.sourcePlayer);
		return true;
	}

	private static boolean modifiersAddTo(CommandInfo commandInfo)
	{
		try
		{
			String name = commandInfo.getString("scene_name");
			String toAdd = commandInfo.getString("to_add");
			return Playing.modifiersAddTo(commandInfo, name, toAdd);
		}
		catch (IllegalArgumentException e)
		{
			commandInfo.sendException(e, "error.unable_to_get_argument");
			return false;
		}
	}
}
