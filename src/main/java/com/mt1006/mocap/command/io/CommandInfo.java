package com.mt1006.mocap.command.io;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mt1006.mocap.command.CommandUtils;
import com.mt1006.mocap.mocap.playing.modifiers.PlayerData;
import com.mt1006.mocap.utils.Utils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class CommandInfo implements CommandOutput
{
	public final CommandContext<CommandSourceStack> ctx;
	public final CommandSourceStack source;
	public final MinecraftServer server;
	public final ServerLevel level;
	public final @Nullable ServerPlayer sourcePlayer;
	public final @Nullable Entity sourceEntity;

	public CommandInfo(CommandContext<CommandSourceStack> ctx)
	{
		//TODO: test if it doesn't crash (command block?)
		this.ctx = ctx;
		this.source = ctx.getSource();
		this.server = source.getServer();
		this.level = source.getLevel();
		this.sourcePlayer = source.getPlayer();
		this.sourceEntity = source.getEntity();
	}

	@Override public void sendSuccess(String component, Object... args)
	{
		source.sendSuccess(() -> getTranslatableComponent(component, args), false);
	}

	@Override public void sendSuccessWithTip(String component, Object... args)
	{
		source.sendSuccess(() -> getTranslatableComponent(component, args), false);
		source.sendSuccess(() -> getTranslatableComponent(component + ".tip", args), false);
	}

	@Override public void sendSuccessLiteral(String format, Object... args)
	{
		source.sendSuccess(() -> Component.literal(String.format(format, args)), false);
	}

	@Override public void sendSuccessComponent(Component component)
	{
		source.sendSuccess(() -> component, false);
	}

	@Override public void sendFailure(String component, Object... args)
	{
		source.sendFailure(getTranslatableComponent(component, args));
	}

	@Override public void sendFailureWithTip(String component, Object... args)
	{
		source.sendFailure(getTranslatableComponent(component, args));
		source.sendFailure(getTranslatableComponent(component + ".tip"));
	}

	@Override public void sendException(Exception exception, String component, Object... args)
	{
		sendFailure(component, args);
		Utils.exception(exception, Utils.stringFromComponent(component, args));
	}

	public MutableComponent getTranslatableComponent(String component, Object... args)
	{
		return Utils.getTranslatableComponent(source.getEntity(), component, args);
	}

	public @Nullable CommandInfo getFinalCommandInfo()
	{
		CommandContext<CommandSourceStack> tempCtx = ctx;
		while (true)
		{
			String command = CommandUtils.getNode(tempCtx.getNodes(), 0);
			if (command != null && (command.equals("mocap") || command.equals("mocap:mocap"))) { return new CommandInfo(tempCtx); }

			tempCtx = ctx.getChild();
			if (tempCtx == null) { return null; }
		}
	}

	public @Nullable String getNode(int pos)
	{
		return CommandUtils.getNode(ctx.getNodes(), pos);
	}

	public int getInteger(String name)
	{
		return IntegerArgumentType.getInteger(ctx, name);
	}

	public double getDouble(String name)
	{
		return DoubleArgumentType.getDouble(ctx, name);
	}

	public boolean getBool(String name)
	{
		return BoolArgumentType.getBool(ctx, name);
	}

	public String getString(String name)
	{
		return StringArgumentType.getString(ctx, name);
	}

	public Collection<GameProfile> getGameProfiles(String name) throws CommandSyntaxException
	{
		return GameProfileArgument.getGameProfiles(ctx, name);
	}

	public @Nullable String getNullableString(String name)
	{
		try { return StringArgumentType.getString(ctx, name); }
		catch (Exception exception) { return null; }
	}

	public PlayerData getPlayerData()
	{
		String playerName = getNullableString("player_name");
		if (playerName == null) { return new PlayerData((String)null); }

		String fromPlayer = getNullableString("skin_player_name");
		if (fromPlayer != null) { return new PlayerData(playerName, PlayerData.SkinSource.FROM_PLAYER, fromPlayer); }

		String fromFile = getNullableString("skin_filename");
		if (fromFile != null) { return new PlayerData(playerName, PlayerData.SkinSource.FROM_FILE, fromFile); }

		String fromMineskin = getNullableString("mineskin_url");
		if (fromMineskin != null) { return new PlayerData(playerName, PlayerData.SkinSource.FROM_MINESKIN, fromMineskin); }

		return new PlayerData(playerName);
	}
}
