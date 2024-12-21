package com.mt1006.mocap.command;

import com.mt1006.mocap.mocap.playing.modifiers.PlaybackModifiers;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class CommandsContext
{
	private static final Map<ServerPlayer, CommandsContext> contexts = new HashMap<>();
	public static int haveSyncEnabled = 0;
	public PlaybackModifiers modifiers = PlaybackModifiers.empty();
	private boolean sync = false;
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
		CommandsContext ctx = contexts.get(player);
		if (ctx == null) { return; }

		if (ctx.sync) { haveSyncEnabled--; }
		contexts.remove(player);
	}

	public static boolean hasDefaultModifiers(@Nullable ServerPlayer source)
	{
		return source == null || CommandsContext.get(source).modifiers.areDefault();
	}

	public static PlaybackModifiers getFinalModifiers(@Nullable ServerPlayer source, PlaybackModifiers simpleModifiers)
	{
		PlaybackModifiers modifiers = source != null
				? CommandsContext.get(source).modifiers.copy()
				: PlaybackModifiers.empty();

		return simpleModifiers.mergeWithParent(modifiers);
	}

	public boolean setSync(boolean sync)
	{
		boolean oldSync = this.sync;
		this.sync = sync;

		if (oldSync != sync)
		{
			if (sync) { haveSyncEnabled++; }
			else { haveSyncEnabled--; }
		}
		return oldSync;
	}

	public boolean getSync()
	{
		return sync;
	}
}
