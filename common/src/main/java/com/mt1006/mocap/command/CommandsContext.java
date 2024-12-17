package com.mt1006.mocap.command;

import com.mt1006.mocap.mocap.playing.modifiers.PlaybackModifiers;
import com.mt1006.mocap.mocap.playing.modifiers.PlayerSkin;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class CommandsContext
{
	private static final Map<ServerPlayer, CommandsContext> contexts = new HashMap<>();
	public PlaybackModifiers modifiers = PlaybackModifiers.empty();
	public @Nullable String doubleStart = null;

	public static CommandsContext get(ServerPlayer player)
	{
		CommandsContext ctx = contexts.get(player);
		if (ctx == null)
		{
			ctx = new CommandsContext();
			contexts.put(player, ctx);
		}
		return ctx;
	}

	public static void removePlayer(ServerPlayer player)
	{
		contexts.remove(player);
	}

	public static PlaybackModifiers getFinalModifiers(@Nullable ServerPlayer source, @Nullable String playerName, PlayerSkin playerSkin)
	{
		PlaybackModifiers modifiers = source != null
				? CommandsContext.get(source).modifiers.copy()
				: PlaybackModifiers.empty();

		if (playerName != null) { modifiers.playerName = playerName; }
		if (playerSkin.skinSource != PlayerSkin.SkinSource.DEFAULT) { modifiers.playerSkin = playerSkin; }
		return modifiers;
	}
}
