package net.mt1006.mocap.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.Registries;
import net.mt1006.mocap.command.io.CommandInfo;
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

	public static ArgumentBuilder<CommandSourceStack, ?> playerArguments(CommandBuildContext buildContext, Command<CommandSourceStack> command)
	{
		return withModelArguments(buildContext, playerNameArgument(command), command, true);
	}

	public static ArgumentBuilder<CommandSourceStack, ?> withModelArguments(CommandBuildContext buildContext, ArgumentBuilder<CommandSourceStack, ?> builder,
																			Command<CommandSourceStack> command, boolean addPlayerAsEntity)
	{
		builder.then(Commands.literal("skin_from_player").then(Commands.argument("skin_player_name", StringArgumentType.greedyString()).executes(command)));
		builder.then(Commands.literal("skin_from_file").then(Commands.argument("skin_filename", StringArgumentType.greedyString()).executes(command)));
		builder.then(Commands.literal("skin_from_mineskin").then(Commands.argument("mineskin_url", StringArgumentType.greedyString()).executes(command)));
		if (addPlayerAsEntity)
		{
			builder.then(Commands.literal("player_as_entity").
				then(Commands.argument("entity", ResourceArgument.resource(buildContext, Registries.ENTITY_TYPE)).executes(command).
				then(Commands.argument("nbt", NbtTagArgument.nbtTag()).executes(command))));
		}
		return builder;
	}

	public static ArgumentBuilder<CommandSourceStack, ?> withModifiers(CommandBuildContext buildContext,
																	   ArgumentBuilder<CommandSourceStack, ?> builder,
																	   Command<CommandSourceStack> command, boolean isScene)
	{
		builder.then(Commands.literal("start_delay").then(Commands.argument("delay", DoubleArgumentType.doubleArg(0.0)).executes(command)));
		builder.then(Commands.literal("transformations").
			then(Commands.literal("rotation").
				then(Commands.argument("deg", DoubleArgumentType.doubleArg()).executes(command))).
			then(Commands.literal("mirror").
				then(Commands.literal("none").executes(command)).
				then(Commands.literal("x").executes(command)).
				then(Commands.literal("z").executes(command)).
				then(Commands.literal("xz").executes(command))).
			then(Commands.literal("scale").
				then(Commands.literal("of_player").
					then(Commands.argument("scale", DoubleArgumentType.doubleArg(0.0)).executes(command))).
				then(Commands.literal("of_scene").
					then(Commands.argument("scale", DoubleArgumentType.doubleArg(0.0)).executes(command)))).
			then(Commands.literal("offset").
				then(Commands.argument("offset_x", DoubleArgumentType.doubleArg()).
				then(Commands.argument("offset_y", DoubleArgumentType.doubleArg()).
				then(Commands.argument("offset_z", DoubleArgumentType.doubleArg()).executes(command))))).
			then(Commands.literal("config").
				then(Commands.literal("round_block_pos").
					then(Commands.argument("round", BoolArgumentType.bool()).executes(command))).
				then(Commands.literal("recording_center").
					then(Commands.literal("auto").executes(command)).
					then(Commands.literal("block_center").executes(command)).
					then(Commands.literal("block_corner").executes(command)).
					then(Commands.literal("actual").executes(command))).
				then(Commands.literal("scene_center").
					then(Commands.literal("common_first").executes(command)).
					then(Commands.literal("common_last").executes(command)).
					then(Commands.literal("common_specific").
						then(Commands.argument("specific_scene_element", StringArgumentType.string()).executes(command))). //TODO: add suggestions
					then(Commands.literal("individual").executes(command))).
				then(Commands.literal("center_offset").
					then(Commands.argument("offset_x", DoubleArgumentType.doubleArg()).
					then(Commands.argument("offset_y", DoubleArgumentType.doubleArg()).
					then(Commands.argument("offset_z", DoubleArgumentType.doubleArg()).executes(command)))))));
		builder.then(Commands.literal("player_name").then(playerNameArgument(command)));
		builder.then(withModelArguments(buildContext, Commands.literal("player_skin"), command, false));
		builder.then(Commands.literal("player_as_entity").
			then(Commands.literal("disabled").executes(command)).
			then(Commands.literal("enabled").
				then(Commands.argument("entity", ResourceArgument.resource(buildContext, Registries.ENTITY_TYPE)).executes(command).
				then(Commands.argument("nbt", NbtTagArgument.nbtTag()).executes(command)))));
		builder.then(Commands.literal("entity_filter").
			then(Commands.literal("disabled").executes(command)).
			then(Commands.literal("enabled").
				then(Commands.argument("entity_filter", StringArgumentType.greedyString()).executes(command))));

		if (isScene)
		{
			builder.then(Commands.literal("subscene_name")
					.then(Commands.argument("new_name", StringArgumentType.string())
							.suggests(CommandSuggestions::playable).executes(command)));
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

	public static RequiredArgumentBuilder<CommandSourceStack, String> withInputArgument(BiFunction<CommandInfo, String, Boolean> function,SuggestionProvider<CommandSourceStack> suggestions, String arg)
	{
		return withStringArgument(function, arg).suggests(suggestions);
	}

	public static RequiredArgumentBuilder<CommandSourceStack, String> withInputAndStringArgument(TriFunction<CommandInfo, String, String, Boolean> function,
																								 SuggestionProvider<CommandSourceStack> suggestions, String arg1, String arg2)
	{
		return Commands.argument(arg1, StringArgumentType.string())
				.suggests(suggestions)
				.then(Commands.argument(arg2, StringArgumentType.string())
				.executes((ctx) -> twoStringCommand(function, ctx, arg1, arg2)));
	}

	public static RequiredArgumentBuilder<CommandSourceStack, String> withTwoInputArguments(TriFunction<CommandInfo, String, String, Boolean> function,
																							SuggestionProvider<CommandSourceStack> suggestions1,
																							SuggestionProvider<CommandSourceStack> suggestions2,
																							String arg1, String arg2)
	{
		return Commands.argument(arg1, StringArgumentType.string())
				.suggests(suggestions1)
				.then(Commands.argument(arg2, StringArgumentType.string())
				.suggests(suggestions2)
				.executes((ctx) -> twoStringCommand(function, ctx, arg1, arg2)));
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

	public static Pair<Integer, @Nullable String> splitIdStr(String str)
	{
		int dashPos = str.indexOf('-');
		int pos = Integer.parseInt(dashPos != -1 ? str.substring(0, dashPos) : str);
		String expectedName = dashPos != -1 ? str.substring(dashPos + 1) : null;
		return Pair.of(pos, expectedName);
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
