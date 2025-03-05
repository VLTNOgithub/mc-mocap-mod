package net.mt1006.mocap.fabric.events;

import net.minecraft.server.MinecraftServer;
import net.mt1006.mocap.events.ServerTickEvent;

public class ServerTickFabricEvent
{
	public static void onEndTick(MinecraftServer server)
	{
		ServerTickEvent.onEndTick();
	}
}
