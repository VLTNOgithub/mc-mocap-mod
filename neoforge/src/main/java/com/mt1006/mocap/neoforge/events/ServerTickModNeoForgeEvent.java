package com.mt1006.mocap.neoforge.events;

import com.mt1006.mocap.MocapMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = MocapMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ServerTickModNeoForgeEvent
{
	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post tickEvent)
	{
		com.mt1006.mocap.events.ServerTickEvent.onEndTick();
	}
}
