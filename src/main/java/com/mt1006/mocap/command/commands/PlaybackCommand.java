package com.mt1006.mocap.command.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mt1006.mocap.command.CommandUtils;
import com.mt1006.mocap.command.io.CommandInfo;
import com.mt1006.mocap.mocap.playing.Playing;
import com.mt1006.mocap.mocap.playing.modifiers.PlayerData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class PlaybackCommand
{
	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder()
	{
		LiteralArgumentBuilder<CommandSourceStack> commandBuilder = Commands.literal("playback");

		commandBuilder.then(Commands.literal("start").
			then(Commands.argument("name", StringArgumentType.greedyString()).executes(CommandUtils.command(PlaybackCommand::start)).
			then(CommandUtils.withPlayerArguments(CommandUtils.command(PlaybackCommand::start)))));
		commandBuilder.then(Commands.literal("stop").
			then(Commands.argument("id", IntegerArgumentType.integer()).executes(CommandUtils.command(PlaybackCommand::stop))));
		commandBuilder.then(Commands.literal("stop_all").executes(CommandUtils.command(Playing::stopAll)));
		//commandBuilder.then(Commands.literal("modifiers")); //TODO: finish
		commandBuilder.then(Commands.literal("list").executes(CommandUtils.command(Playing::list)));

		return commandBuilder;
	}

	private static boolean start(CommandInfo commandInfo)
	{
		String name = commandInfo.getNullableString("name");
		PlayerData playerData = commandInfo.getPlayerData();

		if (name == null)
		{
			commandInfo.sendFailure("error.unable_to_get_argument");
			return false;
		}

		try
		{
			return Playing.start(commandInfo, name, playerData);
		}
		catch (Exception exception)
		{
			commandInfo.sendException(exception, "playing.start.error");
			return false;
		}
	}

	private static boolean stop(CommandInfo commandInfo)
	{
		try
		{
			int id = commandInfo.getInteger("id");
			Playing.stop(commandInfo, id);
		}
		catch (IllegalArgumentException exception)
		{
			commandInfo.sendException(exception, "error.unable_to_get_argument");
			return false;
		}
		return true;
	}
}
