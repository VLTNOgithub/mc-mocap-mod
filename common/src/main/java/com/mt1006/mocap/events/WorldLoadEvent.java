package com.mt1006.mocap.events;

import com.mt1006.mocap.MocapMod;
import com.mt1006.mocap.command.CommandSuggestions;
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
		CommandSuggestions.initInputSet();
	}

	public static void onServerWorldUnload()
	{
		Playing.stopAll(CommandOutput.DUMMY, null);
		Settings.unload();
		Files.deinit();
		Recording.onServerStop();
		MocapMod.server = null;
	}

	public static void onClientWorldUnload()
	{
		PlayerConnectionEvent.players.clear();
		PlayerConnectionEvent.nocolPlayers.clear();
		CustomClientSkinManager.clearCache();
	}
}
