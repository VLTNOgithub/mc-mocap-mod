package net.mt1006.mocap.forge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import net.mt1006.mocap.MocapMod;
import net.mt1006.mocap.network.MocapPacketC2S;
import net.mt1006.mocap.network.MocapPacketS2C;
import org.jetbrains.annotations.Nullable;

public class PacketHandler
{
	public static final SimpleChannel INSTANCE =
			ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath(MocapMod.MOD_ID, "forge")).simpleChannel();

	public static void register()
	{
		INSTANCE.messageBuilder(MocapPacketC2S.class, 1, NetworkDirection.PLAY_TO_SERVER)
				.decoder(MocapPacketC2S::new)
				.encoder(MocapPacketC2S::encode)
				.consumerMainThread(PacketHandler::serverReceiver)
				.add();

		INSTANCE.messageBuilder(MocapPacketS2C.class, 0, NetworkDirection.PLAY_TO_CLIENT)
				.decoder(MocapPacketS2C::new)
				.encoder(MocapPacketS2C::encode)
				.consumerMainThread(PacketHandler::clientReceiver)
				.add();
	}

	private static void serverReceiver(MocapPacketC2S packet, CustomPayloadEvent.Context ctx)
	{
		packet.handle(new Client(ctx.getSender()));
	}

	private static void clientReceiver(MocapPacketS2C packet, CustomPayloadEvent.Context ctx)
	{
		packet.handle(new Server());
	}

	public static class Client implements MocapPacketC2S.Client
	{
		private final @Nullable ServerPlayer player;

		public Client(@Nullable ServerPlayer player)
		{
			this.player = player;
		}

		@Override public @Nullable ServerPlayer getPlayer()
		{
			return player;
		}

		@Override public void respond(MocapPacketS2C packet)
		{
			if (player == null) { return; }
			INSTANCE.send(packet, PacketDistributor.PLAYER.with(player));
		}
	}

	public static class Server implements MocapPacketS2C.Server
	{
		@Override public void respond(MocapPacketC2S packet)
		{
			INSTANCE.send(packet, PacketDistributor.SERVER.with(null));
		}
	}
}
