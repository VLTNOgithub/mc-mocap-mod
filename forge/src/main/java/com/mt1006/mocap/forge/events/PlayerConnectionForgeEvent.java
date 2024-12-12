package com.mt1006.mocap.forge.events;

import com.mt1006.mocap.MocapMod;
import com.mt1006.mocap.events.PlayerConnectionEvent;
import com.mt1006.mocap.forge.PacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MocapMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerConnectionForgeEvent
{
	@SubscribeEvent
	public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent loggedInEvent)
	{
		Player player = loggedInEvent.getEntity();
		if (!(player instanceof ServerPlayer)) { return; }

		PlayerConnectionEvent.onPlayerJoin(new PacketHandler.Client((ServerPlayer)player));
	}

	@SubscribeEvent
	public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent loggedOutEvent)
	{
		Player player = loggedOutEvent.getEntity();
		if (!(player instanceof ServerPlayer)) { return; }

		PlayerConnectionEvent.onPlayerLeave((ServerPlayer)player);
	}
}
