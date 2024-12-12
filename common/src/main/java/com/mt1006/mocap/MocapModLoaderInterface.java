package com.mt1006.mocap;

import com.mt1006.mocap.network.MocapPacketC2S;
import com.mt1006.mocap.network.MocapPacketS2C;
import net.minecraft.server.level.ServerPlayer;

public interface MocapModLoaderInterface
{
	String getLoaderName();
	String getModVersion();

	void sendPacketToClient(ServerPlayer player, MocapPacketS2C packet);
	void sendPacketToServer(MocapPacketC2S packet);
}
