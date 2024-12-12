package com.mt1006.mocap.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class MiscCommand
{
	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder()
	{
		LiteralArgumentBuilder<CommandSourceStack> commandBuilder = Commands.literal("misc");

		commandBuilder.then(Commands.literal("not_yet_implemented")); //TODO: remove
		//commandBuilder.then(Commands.literal("exception").executes(CommandUtils.command(MiscCommand::exception)));
		//TODO: add "refresh", "api"

		return commandBuilder;
	}
}
