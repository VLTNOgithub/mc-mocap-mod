package com.mt1006.mocap.forge.events;

import com.mt1006.mocap.MocapMod;
import com.mt1006.mocap.events.WorldLoadEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MocapMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WorldLoadForgeEvent
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
