package net.mt1006.mocap.command;

import net.minecraft.server.level.ServerPlayer;
import net.mt1006.mocap.mocap.playing.modifiers.PlaybackModifiers;
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

		PlaybackModifiers mergedModifiers = simpleModifiers.mergeWithParent(modifiers);

		// This is done to make PositionTransformer center calculating work properly for starting recording (outside of scene)
		// when playback modifiers are enabled. This can be done because simpleModifiers transformations are default transformations.
		mergedModifiers.transformations = modifiers.transformations;
		//TODO: make it in a better way

		return mergedModifiers;
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
