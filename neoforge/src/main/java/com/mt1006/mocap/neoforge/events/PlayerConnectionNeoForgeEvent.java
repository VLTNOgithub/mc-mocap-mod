package com.mt1006.mocap.neoforge.events;

import com.mt1006.mocap.MocapMod;
import com.mt1006.mocap.events.PlayerConnectionEvent;
import com.mt1006.mocap.neoforge.PacketHandler;
import com.mt1006.mocap.network.MocapPacketS2C;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@EventBusSubscriber(modid = MocapMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class PlayerConnectionNeoForgeEvent
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
