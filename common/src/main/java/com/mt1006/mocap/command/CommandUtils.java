package com.mt1006.mocap.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mt1006.mocap.command.io.CommandInfo;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.Registries;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CommandUtils
{
	public static ArgumentBuilder<CommandSourceStack, ?> playerNameArgument(Command<CommandSourceStack> command)
	{
		return Commands.argument("player_name", StringArgumentType.string()).executes(command);
	}

	public static ArgumentBuilder<CommandSourceStack, ?> playerArguments(Command<CommandSourceStack> command)
	{
		return withSkinArguments(playerNameArgument(command), command);
	}

	public static ArgumentBuilder<CommandSourceStack, ?> withSkinArguments(ArgumentBuilder<CommandSourceStack, ?> builder,
																		   Command<CommandSourceStack> command)
	{
		builder.then(Commands.literal("from_player").then(Commands.argument("skin_player_name", StringArgumentType.greedyString()).executes(command)));
		builder.then(Commands.literal("from_file").then(Commands.argument("skin_filename", StringArgumentType.greedyString()).executes(command)));
		builder.then(Commands.literal("from_mineskin").then(Commands.argument("mineskin_url", StringArgumentType.greedyString()).executes(command)));
		return builder;
	}

	public static ArgumentBuilder<CommandSourceStack, ?> withModifiers(CommandBuildContext buildContext,
																	   ArgumentBuilder<CommandSourceStack, ?> builder,
																	   Command<CommandSourceStack> command, boolean isScene)
	{
		builder.then(Commands.literal("start_delay").then(Commands.argument("delay", DoubleArgumentType.doubleArg(0.0)).executes(command)));
		builder.then(Commands.literal("position_offset").
			then(Commands.argument("offset_x", DoubleArgumentType.doubleArg()).
			then(Commands.argument("offset_y", DoubleArgumentType.doubleArg()).
			then(Commands.argument("offset_z", DoubleArgumentType.doubleArg()).executes(command)))));
		builder.then(Commands.literal("player_name").then(playerNameArgument(command)));
		builder.then(withSkinArguments(Commands.literal("player_skin"), command));
		builder.then(Commands.literal("player_as_entity").
			then(Commands.literal("disabled").executes(command)).
			then(Commands.literal("enabled").
				then(Commands.argument("entity", ResourceArgument.resource(buildContext, Registries.ENTITY_TYPE)).executes(command).
				then(Commands.argument("nbt", NbtTagArgument.nbtTag()).executes(command)))));

		if (isScene)
		{
			builder.then(Commands.literal("subscene_name").then(Commands.argument("new_name", StringArgumentType.string()).executes(command)));
		}
		return builder;
	}

	public static Command<CommandSourceStack> command(Function<CommandInfo, Boolean> function)
	{
		return (ctx) -> (function.apply(new CommandInfo(ctx)) ? 1 : 0);
	}

	public static RequiredArgumentBuilder<CommandSourceStack, String> withStringArgument(BiFunction<CommandInfo, String, Boolean> function, String arg)
	{
		return Commands.argument(arg, StringArgumentType.string()).executes((ctx) -> stringCommand(function, ctx, arg, false));
	}

	public static RequiredArgumentBuilder<CommandSourceStack, String> withTwoStringArguments(TriFunction<CommandInfo, String, String, Boolean> function, String arg1, String arg2)
	{
		return Commands.argument(arg1, StringArgumentType.string())
				.then(Commands.argument(arg2, StringArgumentType.string())
				.executes((ctx) -> twoStringCommand(function, ctx, arg1, arg2)));
	}

	public static RequiredArgumentBuilder<CommandSourceStack, String> withStringAndIntArgument(TriFunction<CommandInfo, String, Integer, Boolean> function, String arg1, String arg2)
	{
		return Commands.argument(arg1, StringArgumentType.string())
				.then(Commands.argument(arg2, IntegerArgumentType.integer())
				.executes((ctx) -> stringAndIntCommand(function, ctx, arg1, arg2)));
	}

	private static int stringCommand(BiFunction<CommandInfo, String, Boolean> function, CommandContext<CommandSourceStack> ctx, String arg, boolean nullable)
	{
		CommandInfo commandInfo = new CommandInfo(ctx);
		try
		{
			String str = commandInfo.getString(arg);
			return function.apply(commandInfo, str) ? 1 : 0;
		}
		catch (IllegalArgumentException e)
		{
			if (nullable)
			{
				return function.apply(commandInfo, null) ? 1 : 0;
			}
			else
			{
				commandInfo.sendException(e, "error.unable_to_get_argument");
				return 0;
			}
		}
	}

	private static int stringAndIntCommand(TriFunction<CommandInfo, String, Integer, Boolean> function, CommandContext<CommandSourceStack> ctx, String arg1, String arg2)
	{
		CommandInfo commandInfo = new CommandInfo(ctx);
		try
		{
			String str = commandInfo.getString(arg1);
			int intVal = commandInfo.getInteger(arg2);
			return function.apply(commandInfo, str, intVal) ? 1 : 0;
		}
		catch (IllegalArgumentException e)
		{
			commandInfo.sendException(e, "error.unable_to_get_argument");
			return 0;
		}
	}

	private static int twoStringCommand(TriFunction<CommandInfo, String, String, Boolean> function, CommandContext<CommandSourceStack> ctx, String arg1, String arg2)
	{
		CommandInfo commandInfo = new CommandInfo(ctx);
		try
		{
			String str1 = commandInfo.getString(arg1);
			String str2 = commandInfo.getString(arg2);
			return function.apply(commandInfo, str1, str2) ? 1 : 0;
		}
		catch (IllegalArgumentException e)
		{
			commandInfo.sendException(e, "error.unable_to_get_argument");
			return 0;
		}
	}

	public static <T> @Nullable CommandContextBuilder<T> getFinalCommandContext(CommandContextBuilder<T> ctx)
	{
		while (true)
		{
			String command = getNode(ctx, 0);
			if (command != null && (command.equals("mocap") || command.equals("mocap:mocap"))) { return ctx; }

			ctx = ctx.getChild();
			if (ctx == null) { return null; }
		}
	}

	public static @Nullable String getNode(CommandContextBuilder<?> ctx, int pos)
	{
		return getNode(ctx.getNodes(), pos);
	}

	public static @Nullable String getNode(List<? extends ParsedCommandNode<?>> nodes, int pos)
	{
		int size = nodes.size();
		if (pos < 0) { pos += size; }
		if (pos >= size || pos < 0) { return null; }
		return nodes.get(pos).getNode().getName();
	}

	public interface TriFunction<T, U, V, R>
	{
		R apply(T t, U u, V v);
	}
}
