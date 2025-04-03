package net.mt1006.mocap.mocap.actions;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;
import net.mt1006.mocap.mocap.settings.Settings;

import java.util.List;
import java.util.UUID;

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
		if (message == null) { return Result.IGNORED; }

		UUID senderUUID;
		if (player == ctx.ghostPlayer)
		{
			List<ServerPlayer> playerList = ctx.level.getServer().getPlayerList().getPlayers();
			if (playerList.isEmpty()) { return Result.IGNORED; }
			senderUUID = playerList.get(0).getUUID();
		}
		else
		{
			senderUUID = player.getUUID();
		}

		PlayerChatMessage chatMessage = PlayerChatMessage.unsigned(senderUUID, message.getString()).withUnsignedContent(message);
		server.getPlayerList().broadcastChatMessage(chatMessage, player, ChatType.bind(ChatType.CHAT, player));
		return Result.OK;
	}
}
