package com.mt1006.mocap.fabric.events;

import com.mt1006.mocap.events.ServerTickEvent;
import net.minecraft.server.MinecraftServer;

public class ServerTickFabricEvent
{
	public static void onEndTick(MinecraftServer server)
	{
		ServerTickEvent.onEndTick();
	}
}
