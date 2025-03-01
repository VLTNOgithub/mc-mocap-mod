package net.mt1006.mocap.forge.events;

import net.mt1006.mocap.MocapMod;
import net.mt1006.mocap.events.ServerTickEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MocapMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerTickForgeEvent
{
	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent tickEvent)
	{
		if (tickEvent.phase == TickEvent.Phase.END) { ServerTickEvent.onEndTick(); }
	}
}
