package com.mt1006.mocap.mocap.playing.playback;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import com.mt1006.mocap.MocapMod;
import com.mt1006.mocap.command.io.CommandInfo;
import com.mt1006.mocap.events.PlayerConnectionEvent;
import com.mt1006.mocap.mocap.actions.Action;
import com.mt1006.mocap.mocap.files.RecordingData;
import com.mt1006.mocap.mocap.files.SceneData;
import com.mt1006.mocap.mocap.playing.DataManager;
import com.mt1006.mocap.mocap.playing.modifiers.PlayerSkin;
import com.mt1006.mocap.mocap.settings.Settings;
import com.mt1006.mocap.network.MocapPacketS2C;
import com.mt1006.mocap.utils.*;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class RecordingPlayback extends Playback
{
	private final RecordingData recording;
	private final ActionContext ctx;
	private int pos = 0;
	private int dyingTicks = 0;

	protected static @Nullable RecordingPlayback startRoot(CommandInfo commandInfo, DataManager dataManager,
														   String name, @Nullable String playerName, PlayerSkin playerSkin)
	{
		try { return new RecordingPlayback(commandInfo, dataManager.getRecording(name), playerName, playerSkin, null, null); }
		catch (StartException e) { return null; }
	}

	protected static @Nullable RecordingPlayback startRoot(CommandInfo commandInfo, @Nullable RecordingData recording,
														   @Nullable String playerName, PlayerSkin playerSkin)
	{
		try { return new RecordingPlayback(commandInfo, recording, playerName, playerSkin, null, null); }
		catch (StartException e) { return null; }
	}

	protected static @Nullable RecordingPlayback startSubscene(CommandInfo commandInfo, DataManager dataManager, Playback parent, SceneData.Subscene info)
	{
		try { return new RecordingPlayback(commandInfo, dataManager.getRecording(info.name), null, null, parent, info); }
		catch (StartException e) { return null; }
	}

	private RecordingPlayback(CommandInfo commandInfo, @Nullable RecordingData recording, @Nullable String rootPlayerName,
							  @Nullable PlayerSkin rootPlayerSkin, @Nullable Playback parent, @Nullable SceneData.Subscene info) throws StartException
	{
		super(parent == null, commandInfo.level, commandInfo.sourcePlayer, rootPlayerName, rootPlayerSkin, parent, info);

		if (recording == null) { throw new StartException(); } //TODO: test if gives error message (especially as subscene)
		this.recording = recording;

		GameProfile profile = getGameProfile(commandInfo);
		if (profile == null)
		{
			commandInfo.sendFailure("playback.start.error");
			commandInfo.sendFailure("playback.start.error.profile");
			throw new StartException();
		}

		GameProfile newProfile = new GameProfile(UUID.randomUUID(), profile.getName());
		try
		{
			PropertyMap oldPropertyMap = (PropertyMap)Fields.gameProfileProperties.get(profile);
			PropertyMap newPropertyMap = (PropertyMap)Fields.gameProfileProperties.get(newProfile);

			newPropertyMap.putAll(oldPropertyMap);
			modifiers.playerSkin.addSkinToPropertyMap(commandInfo, newPropertyMap);
		}
		catch (Exception ignore) {}

		PlayerList packetTargets = level.getServer().getPlayerList();
		Entity entity;
		FakePlayer ghost = null;

		if (!modifiers.playerAsEntity.isEnabled())
		{
			FakePlayer fakePlayer = new FakePlayer(level, newProfile);
			entity = fakePlayer;

			EntityData.PLAYER_SKIN_PARTS.set(fakePlayer, (byte)0b01111111);
			fakePlayer.gameMode.changeGameModeForPlayer(Settings.USE_CREATIVE_GAME_MODE.val ? GameType.CREATIVE : GameType.SURVIVAL);
			recording.initEntityPosition(fakePlayer, modifiers.offset);
			recording.preExecute(fakePlayer, modifiers.blockOffset);

			packetTargets.broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, fakePlayer));
			level.addNewPlayer(fakePlayer);

			if (!Settings.CAN_PUSH_ENTITIES.val)
			{
				for (ServerPlayer player : PlayerConnectionEvent.players)
				{
					MocapPacketS2C.sendNocolPlayerAdd(player, fakePlayer.getUUID());
					PlayerConnectionEvent.addNocolPlayer(fakePlayer.getUUID());
				}
			}

			EntityData.PLAYER_SKIN_PARTS.set(fakePlayer, (byte)0b01111111);
		}
		else
		{
			entity = modifiers.playerAsEntity.createEntity(level);

			if (entity == null)
			{
				commandInfo.sendFailure("playback.start.warning.unknown_entity", modifiers.playerAsEntity.entityId);
				throw new StartException();
			}

			recording.initEntityPosition(entity, modifiers.offset);
			entity.setDeltaMovement(0.0, 0.0, 0.0);
			entity.setInvulnerable(true);
			entity.setNoGravity(true);
			if (entity instanceof Mob) { ((Mob)entity).setNoAi(true); }

			level.addFreshEntity(entity);
			recording.preExecute(entity, modifiers.blockOffset);

			if (Settings.ALLOW_GHOSTS.val)
			{
				ghost = new FakePlayer(level, newProfile);
				ghost.gameMode.changeGameModeForPlayer(Settings.USE_CREATIVE_GAME_MODE.val ? GameType.CREATIVE : GameType.SURVIVAL);
				recording.initEntityPosition(ghost, modifiers.offset);
				level.addNewPlayer(ghost);
			}
		}

		this.ctx = new ActionContext(owner, packetTargets, entity, modifiers, ghost);
	}

	@Override public boolean tick()
	{
		if (dyingTicks > 0)
		{
			dyingTicks--;
			if (dyingTicks == 0)
			{
				ctx.removeMainEntity();
				stop();
				return true;
			}
			return false;
		}
		if (finished) { return true; }

		if (shouldExecuteTick())
		{
			while (true)
			{
				Action.Result result = recording.executeNext(ctx, pos++);

				if (result.endsPlayback)
				{
					if (result == Action.Result.ERROR)
					{
						Utils.sendMessage(owner, "error.playback_error");
						MocapMod.LOGGER.error("Something went wrong during playback!");
					}
					else if (recording.endsWithDeath)
					{
						ctx.entity.kill();
						if (ctx.entity instanceof LivingEntity) { dyingTicks = 20; }
					}
					finished = true;
				}

				if (result == Action.Result.REPEAT) { pos--; }
				if (result.endsTick) { break; }
			}
		}

		if (root && finished && dyingTicks == 0) { stop(); }

		tickCounter++;
		return finished && dyingTicks == 0;
	}

	@Override public void stop()
	{
		ctx.removeEntities();
		finished = true;
	}

	@Override public boolean isFinished()
	{
		return finished && dyingTicks == 0;
	}

	private @Nullable GameProfile getGameProfile(CommandInfo commandInfo)
	{
		String profileName = modifiers.playerName;
		Entity entity = commandInfo.sourceEntity;
		Level level = commandInfo.level;

		if (profileName == null)
		{
			if (entity instanceof ServerPlayer) { profileName = ((ServerPlayer)entity).getGameProfile().getName(); }
			else if (!level.players().isEmpty()) { profileName = level.players().get(0).getGameProfile().getName(); }
			else { profileName = "Player"; }
		}

		return ProfileUtils.getGameProfile(commandInfo.server, profileName);
	}
}
