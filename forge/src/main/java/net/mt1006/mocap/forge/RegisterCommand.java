package net.mt1006.mocap.forge;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mt1006.mocap.MocapMod;
import net.mt1006.mocap.command.commands.MocapCommand;

@Mod.EventBusSubscriber(modid = MocapMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RegisterCommand
{
	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event)
	{
		MocapCommand.register(event.getDispatcher(), event.getBuildContext());
	}
}
