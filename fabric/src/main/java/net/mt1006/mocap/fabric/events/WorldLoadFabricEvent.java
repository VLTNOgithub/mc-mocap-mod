package net.mt1006.mocap.fabric.events;

import net.minecraft.server.MinecraftServer;
import net.mt1006.mocap.events.WorldLoadEvent;

public class WorldLoadFabricEvent
{
	public static void onServerWorldLoad(MinecraftServer server)
	{
		WorldLoadEvent.onServerWorldLoad(server);
	}

	public static void onServerWorldUnload(MinecraftServer server)
	{
		WorldLoadEvent.onServerWorldUnload();
	}
}
