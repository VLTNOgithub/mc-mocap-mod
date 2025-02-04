package com.mt1006.mocap.mocap.playing.skins;

import com.mt1006.mocap.command.io.CommandOutput;
import com.mt1006.mocap.mocap.files.Files;
import com.mt1006.mocap.network.MocapPacketS2C;
import net.minecraft.Util;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CustomServerSkinManager
{
	public static final String PROPERTY_ID = "mocap:skin_from_file";
	private static final ConcurrentMap<String, byte[]> skinCache = new ConcurrentHashMap<>();

	public static void sendSkinToClient(ServerPlayer player, String name)
	{
		byte[] image = skinCache.get(name);
		if (image != null) { MocapPacketS2C.sendCustomSkinData(player, name, image); }
		else { Util.backgroundExecutor().execute(() -> sendSkinToClientThread(player, name)); }
	}

	public static void sendSkinToClientThread(ServerPlayer player, String name)
	{
		if (!checkIfProperName(CommandOutput.DUMMY, name)) { return; }
		byte[] array = Files.loadFile(Files.getSkinFile(name));

		if (array != null)
		{
			skinCache.put(name, array);
			MocapPacketS2C.sendCustomSkinData(player, name, array);
		}
	}

	public static boolean checkIfProperName(CommandOutput commandOutput, String name)
	{
		return Files.checkIfProperName(commandOutput, name.startsWith(Files.SLIM_SKIN_PREFIX) ? name.substring(5) : name);
	}

	public static void clearCache()
	{
		skinCache.clear();
	}
}
