package com.mt1006.mocap.events;

import com.mt1006.mocap.MocapMod;
import com.mt1006.mocap.command.InputArgument;
import com.mt1006.mocap.command.io.CommandOutput;
import com.mt1006.mocap.mocap.files.Files;
import com.mt1006.mocap.mocap.playing.Playing;
import com.mt1006.mocap.mocap.playing.skins.CustomClientSkinManager;
import com.mt1006.mocap.mocap.recording.Recording;
import com.mt1006.mocap.mocap.settings.Settings;
import net.minecraft.server.MinecraftServer;

public class WorldLoadEvent
{
	public static void onServerWorldLoad(MinecraftServer server)
	{
		MocapMod.server = server;
		Files.init();
		Settings.load();
		InputArgument.initServerInputSet();
	}

	public static void onServerWorldUnload(MinecraftServer server)
	{
		Playing.stopAll(CommandOutput.DUMMY);
		Settings.unload();
		Files.deinit();
		Recording.onServerStop();
		MocapMod.server = null;
	}

	public static void onClientWorldUnload()
	{
		InputArgument.clientInputSet.clear();
		PlayerConnectionEvent.players.clear();
		PlayerConnectionEvent.nocolPlayers.clear();
		CustomClientSkinManager.clearCache();
	}
}
