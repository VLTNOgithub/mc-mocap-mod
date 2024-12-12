package com.mt1006.mocap.fabric.events;

import com.mt1006.mocap.events.PlayerConnectionEvent;
import com.mt1006.mocap.fabric.PacketHandler;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

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
