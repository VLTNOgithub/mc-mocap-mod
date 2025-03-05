package net.mt1006.mocap.mocap.playing.playback;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.mt1006.mocap.MocapMod;
import net.mt1006.mocap.command.io.CommandInfo;
import net.mt1006.mocap.events.PlayerConnectionEvent;
import net.mt1006.mocap.mocap.actions.Action;
import net.mt1006.mocap.mocap.files.RecordingData;
import net.mt1006.mocap.mocap.files.SceneData;
import net.mt1006.mocap.mocap.playing.DataManager;
import net.mt1006.mocap.mocap.playing.modifiers.PlaybackModifiers;
import net.mt1006.mocap.mocap.settings.Settings;
import net.mt1006.mocap.network.MocapPacketS2C;
import net.mt1006.mocap.utils.*;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class RecordingPlayback extends Playback
{
	private final RecordingData recording;
	private final ActionContext ctx;
	private int pos = 0;
	private int dyingTicks = 0;

	protected static @Nullable RecordingPlayback startRoot(CommandInfo commandInfo, @Nullable RecordingData recording, PlaybackModifiers modifiers)
	{
		try { return new RecordingPlayback(commandInfo, recording, modifiers, null); }
		catch (StartException e) { return null; }
	}

	protected static @Nullable RecordingPlayback startSubscene(CommandInfo commandInfo, DataManager dataManager, Playback parent, SceneData.Subscene info)
	{
		try { return new RecordingPlayback(commandInfo, dataManager.getRecording(info.name), parent.modifiers, info); }
		catch (StartException e) { return null; }
	}

	private RecordingPlayback(CommandInfo commandInfo, @Nullable RecordingData recording,
							  PlaybackModifiers parentModifiers, @Nullable SceneData.Subscene subscene) throws StartException
	{
		super(subscene == null, commandInfo.level, commandInfo.sourcePlayer, parentModifiers, subscene);

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
			Vec3 startPos = recording.initEntityPosition(fakePlayer, modifiers.offset);
			recording.preExecute(fakePlayer, modifiers, startPos);
			modifiers.scale.applyToPlayer(fakePlayer);

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

			Vec3 startPos = recording.initEntityPosition(entity, modifiers.offset);
			entity.setDeltaMovement(0.0, 0.0, 0.0);
			entity.setInvulnerable(true);
			entity.setNoGravity(true);
			if (entity instanceof Mob) { ((Mob)entity).setNoAi(true); }
			modifiers.scale.applyToPlayer(entity);

			level.addFreshEntity(entity);
			recording.preExecute(entity, modifiers, startPos);

			if (Settings.ALLOW_GHOSTS.val)
			{
				ghost = new FakePlayer(level, newProfile);
				ghost.gameMode.changeGameModeForPlayer(Settings.USE_CREATIVE_GAME_MODE.val ? GameType.CREATIVE : GameType.SURVIVAL);
				recording.initEntityPosition(ghost, modifiers.offset);
				level.addNewPlayer(ghost);
			}
		}

		this.ctx = new ActionContext(owner, packetTargets, entity, modifiers, ghost, recording.startPos);
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
