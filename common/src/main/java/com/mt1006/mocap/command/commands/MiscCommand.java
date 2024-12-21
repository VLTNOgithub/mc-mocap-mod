package com.mt1006.mocap.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mt1006.mocap.command.CommandUtils;
import com.mt1006.mocap.command.CommandsContext;
import com.mt1006.mocap.command.io.CommandInfo;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class MiscCommand
{
	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder()
	{
		LiteralArgumentBuilder<CommandSourceStack> commandBuilder = Commands.literal("misc");

		commandBuilder.then(Commands.literal("sync").
			then(Commands.literal("enable").executes(CommandUtils.command(MiscCommand::syncEnable))).
			then(Commands.literal("disable").executes(CommandUtils.command(MiscCommand::syncDisable))));
		//TODO: add "refresh", "api"

		return commandBuilder;
	}

	private static boolean syncEnable(CommandInfo commandInfo)
	{
		if (commandInfo.sourcePlayer == null)
		{
			commandInfo.sendFailure("failure.resolve_player");
			return false;
		}

		CommandsContext ctx = CommandsContext.get(commandInfo.sourcePlayer);
		commandInfo.sendSuccess(ctx.setSync(true) ? "misc.sync.enable.not_changed" : "misc.sync.enable.changed");
		return true;
	}

	private static boolean syncDisable(CommandInfo commandInfo)
	{
		if (commandInfo.sourcePlayer == null)
		{
			commandInfo.sendFailure("failure.resolve_player");
			return false;
		}

		CommandsContext ctx = CommandsContext.get(commandInfo.sourcePlayer);
		commandInfo.sendSuccess(ctx.setSync(false) ? "misc.sync.disable.changed" : "misc.sync.disable.not_changed");
		return true;
	}
}
