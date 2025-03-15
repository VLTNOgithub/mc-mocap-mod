package net.mt1006.mocap.forge.events;

import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mt1006.mocap.MocapMod;
import net.mt1006.mocap.events.ChatEvent;

@Mod.EventBusSubscriber(modid = MocapMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChatForgeEvent
{
	@SubscribeEvent
	public static void onChatMessage(ServerChatEvent chatEvent)
	{
		ChatEvent.onChatMessage(chatEvent.getMessage(), chatEvent.getPlayer());
	}
}
