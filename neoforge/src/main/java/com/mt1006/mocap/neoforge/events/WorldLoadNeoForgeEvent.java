package com.mt1006.mocap.neoforge.events;

import com.mt1006.mocap.MocapMod;
import com.mt1006.mocap.events.WorldLoadEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = MocapMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class WorldLoadNeoForgeEvent
{
	@SubscribeEvent
	public static void onWorldLoad(LevelEvent.Load loadEvent)
	{
		//TODO: test if called multiple times
		if (!loadEvent.getLevel().isClientSide()) { WorldLoadEvent.onServerWorldLoad(loadEvent.getLevel().getServer()); }
	}

	@SubscribeEvent
	public static void onWorldUnload(LevelEvent.Unload unloadEvent)
	{
		if (unloadEvent.getLevel().isClientSide()) { WorldLoadEvent.onClientWorldUnload(); }
		else { WorldLoadEvent.onServerWorldUnload(); }
	}
}
