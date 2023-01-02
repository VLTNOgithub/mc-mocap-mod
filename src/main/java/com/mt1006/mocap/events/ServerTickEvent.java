package com.mt1006.mocap.events;

import com.mt1006.mocap.mocap.playing.PlayerState;
import com.mt1006.mocap.mocap.commands.Playing;
import com.mt1006.mocap.mocap.commands.Recording;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public class ServerTickEvent
{
	private static PlayerState oldPlayerState = null;

	public static void onEndTick(MinecraftServer server)
	{
		if (Recording.state == Recording.State.WAITING_FOR_ACTION)
		{
			PlayerState playerState = new PlayerState(Recording.serverPlayer);
			if (!playerState.compare(oldPlayerState))
			{
				Recording.previousPlayerState = null;
				Recording.state = Recording.State.RECORDING;
				Recording.serverPlayer.sendSystemMessage(Component.translatable("mocap.commands.recording.start.recording_started"));
				oldPlayerState = null;
			}
			else
			{
				oldPlayerState = playerState;
			}
		}

		if (Recording.state == Recording.State.RECORDING)
		{
			PlayerState playerState = new PlayerState(Recording.serverPlayer);
			playerState.saveDifference(Recording.recording, Recording.previousPlayerState);
			Recording.previousPlayerState = playerState;
		}

		Playing.onTick();
	}
}
