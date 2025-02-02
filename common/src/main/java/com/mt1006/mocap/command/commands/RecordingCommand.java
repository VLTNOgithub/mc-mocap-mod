package com.mt1006.mocap.command.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mt1006.mocap.command.CommandSuggestions;
import com.mt1006.mocap.command.CommandUtils;
import com.mt1006.mocap.command.io.CommandInfo;
import com.mt1006.mocap.mocap.recording.Recording;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Collection;

public class RecordingCommand
{
	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder()
	{
		LiteralArgumentBuilder<CommandSourceStack> commandBuilder = Commands.literal("recording");

		commandBuilder.then(Commands.literal("start").executes(CommandUtils.command(RecordingCommand::start)).
			then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(CommandUtils.command(RecordingCommand::start))));
		//commandBuilder.then(Commands.literal("start_multiple").then(CommandUtils.withStringArgument(Recording::startMultiple, "players")));
		commandBuilder.then(Commands.literal("stop").executes(CommandUtils.command(RecordingCommand::stop)).
			then(Commands.argument("id", StringArgumentType.string()).
				suggests(CommandSuggestions::currentlyRecordedSuggestions).executes(CommandUtils.command(RecordingCommand::stop))));
		commandBuilder.then(Commands.literal("discard").executes(CommandUtils.command(RecordingCommand::discard)).
			then(Commands.argument("id", StringArgumentType.string()).executes(CommandUtils.command(RecordingCommand::discard))));
		commandBuilder.then(Commands.literal("save").then(CommandUtils.withStringArgument(RecordingCommand::saveAuto, "name").
			then(Commands.argument("id", StringArgumentType.string()).executes(CommandUtils.command(RecordingCommand::saveSpecific)))));
		commandBuilder.then(Commands.literal("list").executes(CommandUtils.command(RecordingCommand::list)).
			then(Commands.argument("id", StringArgumentType.string()).executes(CommandUtils.command(RecordingCommand::list))));

		return commandBuilder;
	}

	private static boolean start(CommandInfo commandInfo)
	{
		ServerPlayer player = null;

		try
		{
			Collection<GameProfile> gameProfiles = commandInfo.getGameProfiles("player");

			if (gameProfiles.size() == 1)
			{
				String nickname = gameProfiles.iterator().next().getName();
				player = commandInfo.server.getPlayerList().getPlayerByName(nickname);
			}

			if (player == null)
			{
				commandInfo.sendFailure("recording.start.player_not_found");
				return false;
			}
		}
		catch (Exception e)
		{
			Entity entity = commandInfo.sourceEntity;

			if (!(entity instanceof ServerPlayer))
			{
				commandInfo.sendFailureWithTip("recording.start.player_not_specified");
				return false;
			}

			player = (ServerPlayer)entity;
		}

		return Recording.start(commandInfo, player);
	}

	private static boolean stop(CommandInfo commandInfo)
	{
		return Recording.stop(commandInfo, commandInfo.getNullableString("id"));
	}

	private static boolean discard(CommandInfo commandInfo)
	{
		return Recording.discard(commandInfo, commandInfo.getNullableString("id"));
	}

	private static boolean saveAuto(CommandInfo commandInfo, String name)
	{
		return Recording.save(commandInfo, null, name);
	}

	private static boolean saveSpecific(CommandInfo commandInfo)
	{
		String name = commandInfo.getNullableString("name");
		if (name == null)
		{
			commandInfo.sendFailure("error.unable_to_get_argument");
			return false;
		}

		return Recording.save(commandInfo, commandInfo.getNullableString("id"), name);
	}

	private static boolean list(CommandInfo commandInfo)
	{
		return Recording.list(commandInfo, commandInfo.getNullableString("id"));
	}
}
