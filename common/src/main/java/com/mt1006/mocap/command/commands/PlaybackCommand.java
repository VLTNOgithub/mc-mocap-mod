package com.mt1006.mocap.command.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mt1006.mocap.command.CommandUtils;
import com.mt1006.mocap.command.io.CommandInfo;
import com.mt1006.mocap.mocap.files.SceneData;
import com.mt1006.mocap.mocap.playing.Playing;
import com.mt1006.mocap.mocap.playing.modifiers.PlayerSkin;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.jetbrains.annotations.Nullable;

public class PlaybackCommand
{
	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder(CommandBuildContext buildContext)
	{
		LiteralArgumentBuilder<CommandSourceStack> commandBuilder = Commands.literal("playback");

		commandBuilder.then(Commands.literal("start").
			then(Commands.argument("name", StringArgumentType.string()).executes(CommandUtils.command(PlaybackCommand::start)).
			then(CommandUtils.playerArguments(CommandUtils.command(PlaybackCommand::start)))));
		commandBuilder.then(Commands.literal("stop").
			then(Commands.argument("id", IntegerArgumentType.integer()).executes(CommandUtils.command(PlaybackCommand::stop))));
		commandBuilder.then(Commands.literal("stop_all").executes(CommandUtils.command(Playing::stopAll)));
		commandBuilder.then(Commands.literal("modifiers").
			then(CommandUtils.withModifiers(buildContext, Commands.literal("set"), CommandUtils.command(Playing::modifiersSet), false)).
			then(Commands.literal("list").executes(CommandUtils.command(Playing::modifiersList))).
			then(Commands.literal("reset").executes(CommandUtils.command(Playing::modifiersReset))).
			then(Commands.literal("add_to").
				then(Commands.argument("scene_name", StringArgumentType.string()).
				then(Commands.argument("to_add", StringArgumentType.string()).executes(CommandUtils.command(PlaybackCommand::modifiersAddTo))))));
		commandBuilder.then(Commands.literal("list").executes(CommandUtils.command(Playing::list)));

		return commandBuilder;
	}

	private static boolean start(CommandInfo commandInfo)
	{
		String name = commandInfo.getNullableString("name");
		@Nullable String playerName = commandInfo.getPlayerName();
		PlayerSkin playerSkin = commandInfo.getPlayerSkin();

		if (name == null)
		{
			commandInfo.sendFailure("error.unable_to_get_argument");
			return false;
		}

		try
		{
			return Playing.start(commandInfo, name, playerName, playerSkin);
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
			int id = commandInfo.getInteger("id");
			Playing.stop(commandInfo, id);
		}
		catch (IllegalArgumentException e)
		{
			commandInfo.sendException(e, "error.unable_to_get_argument");
			return false;
		}
		return true;
	}

	private static boolean modifiersAddTo(CommandInfo commandInfo)
	{
		try
		{
			String name = commandInfo.getString("scene_name");

			String toAdd = commandInfo.getString("to_add");
			SceneData.Subscene subscene = new SceneData.Subscene(toAdd);

			return Playing.modifiersAddTo(commandInfo, name, subscene);
		}
		catch (IllegalArgumentException e)
		{
			commandInfo.sendException(e, "error.unable_to_get_argument");
			return false;
		}
	}
}
