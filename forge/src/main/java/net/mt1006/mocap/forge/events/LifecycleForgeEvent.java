package net.mt1006.mocap.forge.events;

import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mt1006.mocap.MocapMod;
import net.mt1006.mocap.events.LifecycleEvent;

@Mod.EventBusSubscriber(modid = MocapMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LifecycleForgeEvent
{
	@SubscribeEvent
	public static void onServerStart(ServerStartedEvent startedEvent)
	{
		LifecycleEvent.onServerStart(startedEvent.getServer());
	}

	@SubscribeEvent
	public static void onServerStop(ServerStoppingEvent stoppingEvent)
	{
		LifecycleEvent.onServerStop();
	}

	@SubscribeEvent
	public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut loggingOutEvent)
	{
		LifecycleEvent.onClientDisconnect();
	}
}
