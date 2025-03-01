package net.mt1006.mocap.command.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.mt1006.mocap.MocapMod;
import net.mt1006.mocap.command.CommandUtils;
import net.mt1006.mocap.command.io.CommandInfo;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class MocapCommand
{
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext)
	{
		//TODO: add permission level to config
		LiteralArgumentBuilder<CommandSourceStack> commandBuilder = Commands.literal("mocap").requires(source -> source.hasPermission(2));

		commandBuilder.then(RecordingCommand.getArgumentBuilder());
		commandBuilder.then(PlaybackCommand.getArgumentBuilder(buildContext));
		commandBuilder.then(RecordingsCommand.getArgumentBuilder());
		commandBuilder.then(ScenesCommand.getArgumentBuilder(buildContext));
		commandBuilder.then(SettingsCommand.getArgumentBuilder());
		commandBuilder.then(MiscCommand.getArgumentBuilder());
		commandBuilder.then(Commands.literal("info").executes(CommandUtils.command(MocapCommand::info)));
		commandBuilder.then(Commands.literal("help").executes(CommandUtils.command(MocapCommand::help)));

		dispatcher.register(commandBuilder);
	}

	private static boolean info(CommandInfo commandInfo)
	{
		commandInfo.sendSuccessLiteral(MocapMod.getFullName());
		commandInfo.sendSuccessLiteral("Author: mt1006");
		return true;
	}

	private static boolean help(CommandInfo commandInfo)
	{
		commandInfo.sendSuccess("help", MocapMod.getName());
		return true;
	}
}
