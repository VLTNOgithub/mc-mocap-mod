package net.mt1006.mocap.events;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.mt1006.mocap.MocapMod;
import net.mt1006.mocap.command.CommandsContext;
import net.mt1006.mocap.mocap.settings.Settings;
import net.mt1006.mocap.network.MocapPacketC2S;
import net.mt1006.mocap.network.MocapPacketS2C;
import net.mt1006.mocap.utils.Utils;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PlayerConnectionEvent
{
	private static final int MAX_PLAYER_COUNT = 2048;
	private static final int MAX_NOCOL_PLAYER_COUNT = 4096;

	public static final Set<ServerPlayer> players = Collections.newSetFromMap(new IdentityHashMap<>());
	public static final Set<UUID> nocolPlayers = new HashSet<>();

	public static void onPlayerJoin(MocapPacketC2S.Client client)
	{
		MocapPacketS2C.sendOnLogin(client);
	}

	public static void onPlayerLeave(ServerPlayer player)
	{
		players.remove(player);
		CommandsContext.removePlayer(player);
	}

	public static void addPlayer(@Nullable ServerPlayer player)
	{
		if (player == null || players.size() >= MAX_PLAYER_COUNT) { return; }
		players.add(player);
		players.removeIf(Entity::isRemoved);
	}

	public static void addNocolPlayer(UUID uuid)
	{
		if (nocolPlayers.size() >= MAX_NOCOL_PLAYER_COUNT) { return; }
		nocolPlayers.add(uuid);
	}

	public static void removeNocolPlayer(UUID uuid)
	{
		nocolPlayers.remove(uuid);
	}

	public static void experimentalReleaseWarning(ServerPlayer player)
	{
		if (!MocapMod.EXPERIMENTAL || !player.hasPermissions(2) || !Settings.EXPERIMENTAL_RELEASE_WARNING.val) { return; }

		Utils.sendComponent(player, Utils.getTranslatableComponent(player, "warning.experimental")
				.append(Utils.getEventComponent(ClickEvent.Action.OPEN_URL,
						"https://modrinth.com/mod/motion-capture/versions?c=release",
						Utils.getTranslatableComponent(player, "warning.experimental.stable_download")))
				.append(Utils.getEventComponent(ClickEvent.Action.OPEN_URL,
						"https://discord.gg/nzDETZhqur", "§n[Discord]§r "))
				.append(Utils.getEventComponent(ClickEvent.Action.OPEN_URL,
						"https://github.com/mt1006/mc-mocap-mod", "§n[GitHub]§r "))
				.append(Utils.getEventComponent(ClickEvent.Action.SUGGEST_COMMAND,
						"/mocap settings advanced experimental_release_warning false",
						Utils.getTranslatableComponent(player, "warning.experimental.disable_message"))));
	}
}
