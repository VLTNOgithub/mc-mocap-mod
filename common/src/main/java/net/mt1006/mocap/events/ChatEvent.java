package net.mt1006.mocap.events;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.mt1006.mocap.mocap.actions.ChatMessage;
import net.mt1006.mocap.mocap.recording.Recording;
import net.mt1006.mocap.mocap.settings.Settings;

public class ChatEvent
{
	public static void onChatMessage(Component message, ServerPlayer sender)
	{
		if (Recording.isActive() && Settings.CHAT_RECORDING.val)
		{
			Recording.byRecordedPlayer(sender).forEach((ctx) -> ctx.addAction(
					new ChatMessage(Component.Serializer.toJson(message, sender.server.registryAccess()))));
		}
	}
}
