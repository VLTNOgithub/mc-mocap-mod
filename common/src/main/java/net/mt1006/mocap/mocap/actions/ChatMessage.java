package net.mt1006.mocap.mocap.actions;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;
import net.mt1006.mocap.mocap.settings.Settings;

public class ChatMessage implements Action
{
	private final String messageJson;

	public ChatMessage(String messageJson)
	{
		this.messageJson = messageJson;
	}

	public ChatMessage(RecordingFiles.Reader reader)
	{
		this.messageJson = reader.readString();
	}

	@Override public void write(RecordingFiles.Writer writer)
	{
		writer.addByte(Type.CHAT_MESSAGE.id);
		writer.addString(messageJson);
	}

	@Override public Result execute(ActionContext ctx)
	{
		if (!Settings.CHAT_PLAYBACK.val) { return Result.IGNORED; }
		ServerPlayer player = (ctx.entity instanceof ServerPlayer) ? (ServerPlayer)ctx.entity : ctx.ghostPlayer;
		if (player == null) { return Result.IGNORED; }

		MinecraftServer server = ctx.level.getServer();
		Component message = Component.Serializer.fromJson(messageJson, server.registryAccess());
		if (message == null) { return Result.IGNORED; } //TODO: non-critical error?

		PlayerChatMessage chatMessage = PlayerChatMessage.unsigned(player.getUUID(), message.getString()).withUnsignedContent(message);
		server.getPlayerList().broadcastChatMessage(chatMessage, player, ChatType.bind(ChatType.CHAT, player));
		return Result.OK;
	}
}
