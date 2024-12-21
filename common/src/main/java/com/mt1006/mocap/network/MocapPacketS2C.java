package com.mt1006.mocap.network;

import com.mojang.datafixers.util.Pair;
import com.mt1006.mocap.MocapMod;
import com.mt1006.mocap.command.InputArgument;
import com.mt1006.mocap.events.PlayerConnectionEvent;
import com.mt1006.mocap.mocap.playing.skins.CustomClientSkinManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class MocapPacketS2C implements CustomPacketPayload
{
	private static final String TYPE_ID = MocapMod.loaderInterface.getLoaderName().toLowerCase() + "_s2c";
	public static final Type<MocapPacketS2C> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MocapMod.MOD_ID, TYPE_ID));
	public static final StreamCodec<FriendlyByteBuf, MocapPacketS2C> CODEC = StreamCodec.of((b, p) -> p.encode(b), MocapPacketS2C::new);

	public static final int ON_LOGIN = 0;
	public static final int NOCOL_PLAYER_ADD = 1;
	public static final int NOCOL_PLAYER_REMOVE = 2;
	public static final int INPUT_SUGGESTIONS_ADD = 3;
	public static final int INPUT_SUGGESTIONS_REMOVE = 4;
	public static final int CUSTOM_SKIN_DATA = 5;

	private final int version;
	private final int op;
	private final Object object;

	public MocapPacketS2C(int op, Object object)
	{
		this.version = MocapMod.NETWORK_PACKETS_VERSION;
		this.op = op;
		this.object = object;
	}

	public MocapPacketS2C(FriendlyByteBuf buf)
	{
		version = buf.readInt();
		op = buf.readInt();

		switch (op)
		{
			case NOCOL_PLAYER_ADD:
			case NOCOL_PLAYER_REMOVE:
				object = buf.readUUID();
				break;

			case INPUT_SUGGESTIONS_ADD:
			case INPUT_SUGGESTIONS_REMOVE:
				int suggestionListSize = buf.readInt();
				Collection<String> suggestionList = new ArrayList<>(suggestionListSize);
				for (int i = 0; i < suggestionListSize; i++)
				{
					suggestionList.add(NetworkUtils.readString(buf));
				}
				object = suggestionList;
				break;

			case CUSTOM_SKIN_DATA:
				String customSkinName = NetworkUtils.readString(buf);
				byte[] customSkinArray = NetworkUtils.readByteArray(buf);
				object = Pair.of(customSkinName, customSkinArray);
				break;

			default:
				object = null;
		}
	}

	@Override public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public void encode(FriendlyByteBuf buf)
	{
		buf.writeInt(version);
		buf.writeInt(op);

		switch (op)
		{
			case NOCOL_PLAYER_ADD:
			case NOCOL_PLAYER_REMOVE:
				if (object instanceof UUID) { buf.writeUUID((UUID)object); }
				break;

			case INPUT_SUGGESTIONS_ADD:
			case INPUT_SUGGESTIONS_REMOVE:
				if (!(object instanceof Collection<?>)) { break; }
				buf.writeInt(((Collection<?>)object).size());
				for (Object str : (Collection<?>)object)
				{
					if (str instanceof String) { NetworkUtils.writeString(buf, (String)str); }
				}
				break;

			case CUSTOM_SKIN_DATA:
				if (!(object instanceof Pair<?,?>)) { break; }
				Pair<String, byte[]> customSkinData = (Pair<String, byte[]>)object;
				NetworkUtils.writeString(buf, customSkinData.getFirst());
				NetworkUtils.writeByteArray(buf, customSkinData.getSecond());
				break;
		}
	}

	public void handle(Server server)
	{
		if (version != MocapMod.NETWORK_PACKETS_VERSION) { return; }

 		switch (op)
		{
			case ON_LOGIN: MocapPacketC2S.sendAcceptServer(server); break;
			case NOCOL_PLAYER_ADD: PlayerConnectionEvent.addNocolPlayer((UUID)object); break;
			case NOCOL_PLAYER_REMOVE: PlayerConnectionEvent.removeNocolPlayer((UUID)object); break;
			case INPUT_SUGGESTIONS_ADD: InputArgument.clientInputSet.addAll((Collection<String>)object); break;
			case INPUT_SUGGESTIONS_REMOVE: InputArgument.clientInputSet.removeAll((Collection<String>)object); break;
			case CUSTOM_SKIN_DATA: CustomClientSkinManager.register((Pair<String, byte[]>)object); break;
		}
	}

	public static void sendOnLogin(MocapPacketC2S.Client client)
	{
		client.respond(new MocapPacketS2C(ON_LOGIN, null));
	}

	public static void sendNocolPlayerAdd(ServerPlayer serverPlayer, UUID playerToAdd)
	{
		send(serverPlayer, NOCOL_PLAYER_ADD, playerToAdd);
	}

	public static void sendNocolPlayerRemove(ServerPlayer serverPlayer, UUID playerToAdd)
	{
		send(serverPlayer, NOCOL_PLAYER_REMOVE, playerToAdd);
	}

	public static void sendInputSuggestionsAdd(ServerPlayer player, Collection<String> strings)
	{
		send(player, INPUT_SUGGESTIONS_ADD, strings);
	}

	public static void sendInputSuggestionsAddOnLogin(MocapPacketC2S.Client client, Collection<String> strings)
	{
		client.respond(new MocapPacketS2C(INPUT_SUGGESTIONS_ADD, strings));
	}

	public static void sendInputSuggestionsRemove(ServerPlayer player, Collection<String> strings)
	{
		send(player, INPUT_SUGGESTIONS_REMOVE, strings);
	}

	public static void sendCustomSkinData(ServerPlayer player, String name, byte[] byteArray)
	{
		send(player, CUSTOM_SKIN_DATA, Pair.of(name, byteArray));
	}

	private static void send(ServerPlayer player, int op, Object object)
	{
		MocapMod.loaderInterface.sendPacketToClient(player, new MocapPacketS2C(op, object));
	}

	public interface Server
	{
		void respond(MocapPacketC2S packet);
	}
}
