package net.mt1006.mocap.fabric.events;

import net.minecraft.server.MinecraftServer;
import net.mt1006.mocap.events.LifecycleEvent;

public class LifecycleFabricEvent
{
	public static void onServerStart(MinecraftServer server)
	{
		LifecycleEvent.onServerStart(server);
	}

	public static void onServerStop(MinecraftServer server)
	{
		LifecycleEvent.onServerStop();
	}
}
