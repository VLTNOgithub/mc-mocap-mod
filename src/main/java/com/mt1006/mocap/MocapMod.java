package com.mt1006.mocap;

import com.mt1006.mocap.command.RegisterCommand;
import com.mt1006.mocap.events.*;
import com.mt1006.mocap.mocap.actions.Action;
import com.mt1006.mocap.network.MocapPackets;
import com.mt1006.mocap.utils.Fields;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class MocapMod implements ModInitializer
{
	//TODO: add support for empty player names
	public static final String MOD_ID = "mocap";
	public static final String VERSION = "1.4-alpha-1";
	public static final String FOR_VERSION = "1.21.1";
	public static final String FOR_LOADER = "Fabric";
	public static final boolean EXPERIMENTAL = true;

	public static final byte RECORDING_FORMAT_VERSION = 5;
	public static final byte SCENE_FORMAT_VERSION = 3;

	public static final Logger LOGGER = LogManager.getLogger();
	public static final boolean isDedicatedServer = FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
	public static @Nullable MinecraftServer server = null;

	@Override public void onInitialize()
	{
		ServerTickEvents.END_SERVER_TICK.register(ServerTickEvent::onEndTick);
		ServerLifecycleEvents.SERVER_STARTED.register(WorldLoadEvent::onServerWorldLoad);
		ServerLifecycleEvents.SERVER_STOPPING.register(WorldLoadEvent::onServerWorldUnload);
		PlayerBlockBreakEvents.BEFORE.register(BlockInteractionEvent::onBlockBreak);
		UseBlockCallback.EVENT.register(BlockInteractionEvent::onRightClickBlock);
		ServerPlayConnectionEvents.JOIN.register(PlayerConnectionEvent::onPlayerJoin);
		ServerPlayConnectionEvents.DISCONNECT.register(PlayerConnectionEvent::onPlayerLeave);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(EntityEvent::onEntityHurt);

		RegisterCommand.registerCommands();
		Fields.init();
		MocapPackets.register();
		Action.init();
	}

	public static String getName()
	{
		return "Mocap v" + VERSION;
	}

	public static String getFullName()
	{
		return "Mocap v" + VERSION + " for Minecraft " + FOR_VERSION + " [" + FOR_LOADER + "]";
	}
}
