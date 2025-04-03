package net.mt1006.mocap.utils;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.stats.Stat;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.portal.DimensionTransition;
import net.mt1006.mocap.mixin.fields.ServerPlayerFields;
import net.mt1006.mocap.mocap.playing.playback.RecordingPlayback;
import net.mt1006.mocap.mocap.settings.Settings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

// FakePlayer class from Forge
public class FakePlayer extends ServerPlayer
{
	private static final ClientInformation DEFAULT_CLIENT_INFO = ClientInformation.createDefault();
	private final RecordingPlayback playback;
	private final boolean isInvulnerable;
	private int dyingTicks = -1;

	public FakePlayer(ServerLevel level, GameProfile profile, RecordingPlayback playback)
	{
		super(level.getServer(), level, profile, DEFAULT_CLIENT_INFO);
		this.connection = new FakePlayerNetHandler(level.getServer(), this, profile);
		this.playback = playback;
		this.isInvulnerable = Settings.INVULNERABLE_PLAYBACK.val;

		if (isInvulnerable) { setInvulnerable(true); }
		else { ((ServerPlayerFields)this).setSpawnInvulnerableTime(0); }
	}

	@Override public void tick()
	{
		if (!isInvulnerable && invulnerableTime > 0) { invulnerableTime--; }

		if (dyingTicks >= 0)
		{
			dyingTicks--;
			if (dyingTicks == 0) { playback.stop(); }
		}
	}

	@Override public Entity changeDimension(@NotNull DimensionTransition dimensionTransition) { return null; }

	@Override public void displayClientMessage(@NotNull Component chatComponent, boolean actionBar) { }
	@Override public void awardStat(@NotNull Stat stat, int amount) { }
	@Override public void die(@NotNull DamageSource source)
	{
		dyingTicks = 20;
	}

	private static class FakePlayerNetHandler extends ServerGamePacketListenerImpl
	{
		private static final Connection DUMMY_CONNECTION = new DummyConnection(PacketFlow.CLIENTBOUND);

		public FakePlayerNetHandler(MinecraftServer server, ServerPlayer player, GameProfile profile)
		{
			//super(server, DUMMY_CONNECTION, player, new CommonListenerCookie(profile, 0, DEFAULT_CLIENT_INFO));
			super(server, DUMMY_CONNECTION, player, new CommonListenerCookie(profile, 0, DEFAULT_CLIENT_INFO, false));
		}

		@Override public void tick() { }
		@Override public void resetPosition() { }
		@Override public void disconnect(Component message) { }
		@Override public void handlePlayerInput(ServerboundPlayerInputPacket packet) { }
		@Override public void handleMoveVehicle(ServerboundMoveVehiclePacket packet) { }
		@Override public void handleAcceptTeleportPacket(ServerboundAcceptTeleportationPacket packet) { }
		@Override public void handleRecipeBookSeenRecipePacket(ServerboundRecipeBookSeenRecipePacket packet) { }
		@Override public void handleRecipeBookChangeSettingsPacket(ServerboundRecipeBookChangeSettingsPacket packet) { }
		@Override public void handleSeenAdvancements(ServerboundSeenAdvancementsPacket packet) { }
		@Override public void handleCustomCommandSuggestions(ServerboundCommandSuggestionPacket packet) { }
		@Override public void handleSetCommandBlock(ServerboundSetCommandBlockPacket packet) { }
		@Override public void handleSetCommandMinecart(ServerboundSetCommandMinecartPacket packet) { }
		@Override public void handlePickItem(ServerboundPickItemPacket packet) { }
		@Override public void handleRenameItem(ServerboundRenameItemPacket packet) { }
		@Override public void handleSetBeaconPacket(ServerboundSetBeaconPacket packet) { }
		@Override public void handleSetStructureBlock(ServerboundSetStructureBlockPacket packet) { }
		@Override public void handleSetJigsawBlock(ServerboundSetJigsawBlockPacket packet) { }
		@Override public void handleJigsawGenerate(ServerboundJigsawGeneratePacket packet) { }
		@Override public void handleSelectTrade(ServerboundSelectTradePacket packet) { }
		@Override public void handleEditBook(ServerboundEditBookPacket packet) { }
		@Override public void handleEntityTagQuery(ServerboundEntityTagQueryPacket packet) { }
		@Override public void handleBlockEntityTagQuery(ServerboundBlockEntityTagQueryPacket packet) { }
		@Override public void handleMovePlayer(ServerboundMovePlayerPacket packet) { }
		@Override public void teleport(double x, double y, double z, float yaw, float pitch) { }
		@Override public void handlePlayerAction(ServerboundPlayerActionPacket packet) { }
		@Override public void handleUseItemOn(ServerboundUseItemOnPacket packet) { }
		@Override public void handleUseItem(ServerboundUseItemPacket packet) { }
		@Override public void handleTeleportToEntityPacket(ServerboundTeleportToEntityPacket packet) { }
		@Override public void handlePaddleBoat(ServerboundPaddleBoatPacket packet) { }
		@Override public void send(Packet<?> packet) { }
		@Override public void send(Packet<?> packet, @Nullable PacketSendListener sendListener) { }
		@Override public void handleSetCarriedItem(ServerboundSetCarriedItemPacket packet) { }
		@Override public void handleChat(ServerboundChatPacket packet) { }
		@Override public void handleAnimate(ServerboundSwingPacket packet) { }
		@Override public void handlePlayerCommand(ServerboundPlayerCommandPacket packet) { }
		@Override public void handleInteract(ServerboundInteractPacket packet) { }
		@Override public void handleClientCommand(ServerboundClientCommandPacket packet) { }
		@Override public void handleContainerClose(ServerboundContainerClosePacket packet) { }
		@Override public void handleContainerClick(ServerboundContainerClickPacket packet) { }
		@Override public void handlePlaceRecipe(ServerboundPlaceRecipePacket packet) { }
		@Override public void handleContainerButtonClick(ServerboundContainerButtonClickPacket packet) { }
		@Override public void handleSetCreativeModeSlot(ServerboundSetCreativeModeSlotPacket packet) { }
		@Override public void handleSignUpdate(ServerboundSignUpdatePacket packet) { }
		@Override public void handlePlayerAbilities(ServerboundPlayerAbilitiesPacket packet) { }
		@Override public void handleChangeDifficulty(ServerboundChangeDifficultyPacket packet) { }
		@Override public void handleLockDifficulty(ServerboundLockDifficultyPacket packet) { }
		@Override public void teleport(double x, double y, double z, float yaw, float pitch, Set<RelativeMovement> relativeSet) { }
		@Override public void ackBlockChangesUpTo(int sequence) { }
		@Override public void handleChatCommand(ServerboundChatCommandPacket packet) { }
		@Override public void handleChatAck(ServerboundChatAckPacket packet) { }
		@Override public void addPendingMessage(PlayerChatMessage message) { }
		@Override public void sendPlayerChatMessage(PlayerChatMessage message, ChatType.Bound boundChatType) { }
		@Override public void sendDisguisedChatMessage(Component content, ChatType.Bound boundChatType) { }
		@Override public void handleChatSessionUpdate(ServerboundChatSessionUpdatePacket packet) { }
	}

	private static class DummyConnection extends Connection
	{
		public DummyConnection(PacketFlow packetFlow)
		{
			super(packetFlow);
		}
	}
}
