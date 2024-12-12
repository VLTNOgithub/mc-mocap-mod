package com.mt1006.mocap.forge.events;

import com.mt1006.mocap.MocapMod;
import com.mt1006.mocap.events.EntityEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MocapMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EntityForgeEvent
{
	@SubscribeEvent
	public static void onEntityHurt(LivingAttackEvent attackEvent)
	{
		EntityEvent.onEntityHurt(attackEvent.getEntity());
	}

	@SubscribeEvent
	public static void onEntityDrop(LivingDropsEvent dropsEvent)
	{
		if (EntityEvent.onEntityDrop(dropsEvent.getEntity())) { dropsEvent.setCanceled(true); }
	}
}
