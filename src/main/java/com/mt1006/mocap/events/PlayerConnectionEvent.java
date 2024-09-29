package com.mt1006.mocap.events;

import com.mt1006.mocap.MocapMod;
import com.mt1006.mocap.mocap.settings.Settings;
import com.mt1006.mocap.network.MocapPacketS2C;
import com.mt1006.mocap.utils.Utils;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PlayerConnectionEvent
{
	private static final int MAX_PLAYER_COUNT = 2048;
	private static final int MAX_NOCOL_PLAYER_COUNT = 4096;

	public static final Set<ServerPlayer> players = Collections.newSetFromMap(new IdentityHashMap<>());
	public static final Set<UUID> nocolPlayers = new HashSet<>();

	public static void onPlayerJoin(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server)
	{
		MocapPacketS2C.sendOnLogin(sender);
	}

	public static void onPlayerLeave(ServerGamePacketListenerImpl handler, MinecraftServer server)
	{
		players.remove(handler.player);
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

		//TODO: use translation keys
		Utils.sendComponent(player, Component.literal(
				"§lWARNING!§r\n" +
				"You're using experimental version of Motion Capture mod!\n" +
				"You need to know that:\n" +
				" -it probably has a lot of issues\n" +
				" -a lot of new features are partially complete and are subject to change\n" +
				" -it should not be used in public modpacks\n" +
				" -future releases MAY NOT be compatible with recordings and scenes from this version!\n" +
				"If you're planning on running a longer project, or if you're expecting stability from this mod, ")
				.append(Utils.getEventComponent(ClickEvent.Action.OPEN_URL,
						"https://modrinth.com/mod/motion-capture/versions?c=release", "§ndownload a stable release§r!\n"))
				.append(Utils.getEventComponent(ClickEvent.Action.OPEN_URL, "https://discord.gg/nzDETZhqur", "§n[Discord]§r "))
				.append(Utils.getEventComponent(ClickEvent.Action.OPEN_URL, "https://github.com/mt1006/mc-mocap-mod", "§n[GitHub]§r "))
				.append(Utils.getEventComponent(ClickEvent.Action.SUGGEST_COMMAND,
						"/mocap settings advanced experimental_release_warning false", "§n[Disable this message]")));
	}
}
