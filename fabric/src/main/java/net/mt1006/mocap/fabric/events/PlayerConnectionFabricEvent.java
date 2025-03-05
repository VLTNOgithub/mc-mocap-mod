package net.mt1006.mocap.fabric.events;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.mt1006.mocap.events.PlayerConnectionEvent;
import net.mt1006.mocap.fabric.PacketHandler;

public class PlayerConnectionFabricEvent
{
	public static void onPlayerJoin(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server)
	{
		PlayerConnectionEvent.onPlayerJoin(new PacketHandler.Client(handler.player, sender));
	}

	public static void onPlayerLeave(ServerGamePacketListenerImpl handler, MinecraftServer server)
	{
		PlayerConnectionEvent.onPlayerLeave(handler.player);
	}
}
