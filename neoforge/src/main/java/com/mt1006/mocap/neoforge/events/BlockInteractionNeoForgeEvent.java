package com.mt1006.mocap.neoforge.events;

import com.mt1006.mocap.MocapMod;
import com.mt1006.mocap.events.BlockInteractionEvent;
import com.mt1006.mocap.mocap.actions.BreakBlock;
import com.mt1006.mocap.mocap.actions.PlaceBlock;
import com.mt1006.mocap.mocap.actions.PlaceBlockSilently;
import com.mt1006.mocap.mocap.actions.RightClickBlock;
import com.mt1006.mocap.mocap.recording.Recording;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = MocapMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class BlockInteractionNeoForgeEvent
{
	@SubscribeEvent
	public static void onBlockBreak(BlockEvent.BreakEvent breakEvent)
	{
		BlockInteractionEvent.onBlockBreak(breakEvent.getPlayer(), breakEvent.getPos(), breakEvent.getState());
	}

	@SubscribeEvent
	public static void onBlockPlace(BlockEvent.EntityPlaceEvent placeEvent)
	{
		Entity entity = placeEvent.getEntity();
		if (!(entity instanceof Player)) { return; }

		BlockInteractionEvent.onBlockPlace((Player)entity,
				placeEvent.getBlockSnapshot().getState(),
				placeEvent.getPlacedBlock(), placeEvent.getPos());
	}

	@SubscribeEvent
	public static void onBlockPlaceSilently(BlockEvent.EntityMultiPlaceEvent placeEvent)
	{
		Entity entity = placeEvent.getEntity();
		if (!(entity instanceof Player)) { return; }

		BlockInteractionEvent.onSilentBlockPlace((Player)entity,
				placeEvent.getBlockSnapshot().getState(),
				placeEvent.getPlacedBlock(), placeEvent.getPos());
	}

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock clickEvent)
	{
		Player player = clickEvent.getEntity();

		BlockInteractionEvent.onRightClickBlock(player, clickEvent.getHand(), clickEvent.getHitVec(),
				player.getMainHandItem().doesSneakBypassUse(player.level(), clickEvent.getPos(), player));
	}
}
