package net.mt1006.mocap.fabric.events;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.mt1006.mocap.events.ChatEvent;

public class ChatFabricEvent
{
	public static void onChatMessage(PlayerChatMessage message, ServerPlayer sender, ChatType.Bound bound)
	{
		ChatEvent.onChatMessage(message.decoratedContent(), sender);
	}
}
