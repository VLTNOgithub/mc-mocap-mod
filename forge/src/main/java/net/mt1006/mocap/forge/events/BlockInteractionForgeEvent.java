package net.mt1006.mocap.forge.events;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mt1006.mocap.MocapMod;
import net.mt1006.mocap.events.BlockInteractionEvent;

@Mod.EventBusSubscriber(modid = MocapMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BlockInteractionForgeEvent
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
				placeEvent.getBlockSnapshot().getReplacedBlock(),
				placeEvent.getPlacedBlock(), placeEvent.getPos());
	}

	@SubscribeEvent
	public static void onSilentBlockPlace(BlockEvent.EntityMultiPlaceEvent placeEvent)
	{
		Entity entity = placeEvent.getEntity();
		if (!(entity instanceof Player)) { return; }

		BlockInteractionEvent.onSilentBlockPlace((Player)entity,
				placeEvent.getBlockSnapshot().getReplacedBlock(),
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
