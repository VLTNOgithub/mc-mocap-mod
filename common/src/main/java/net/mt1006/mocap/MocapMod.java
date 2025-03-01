package net.mt1006.mocap;

import net.mt1006.mocap.mocap.actions.Action;
import net.mt1006.mocap.utils.Fields;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class MocapMod
{
	public static final String MOD_ID = "mocap";
	public static final String FOR_VERSION = "1.21.1";
	public static final boolean EXPERIMENTAL = true; //TODO: change it to false

	public static final byte RECORDING_FORMAT_VERSION = 5;
	public static final byte SCENE_FORMAT_VERSION = 4;
	public static final int NETWORK_PACKETS_VERSION = 5;

	public static final Logger LOGGER = LogManager.getLogger();
	public static boolean isDedicatedServer = false;
	public static MocapModLoaderInterface loaderInterface = null;
	public static @Nullable MinecraftServer server = null;

	public static void init(boolean isDedicatedServer, MocapModLoaderInterface loaderInterface)
	{
		MocapMod.isDedicatedServer = isDedicatedServer;
		MocapMod.loaderInterface = loaderInterface;

		Fields.init();
		Action.init();
	}

	public static String getName()
	{
		return String.format("Mocap v%s", loaderInterface.getModVersion());
	}

	public static String getFullName()
	{
		return String.format("Mocap v%s for Minecraft %s [%s]",
				loaderInterface.getModVersion(), FOR_VERSION, loaderInterface.getLoaderName());
	}
}
