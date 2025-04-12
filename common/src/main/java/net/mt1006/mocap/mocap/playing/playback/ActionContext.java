package net.mt1006.mocap.mocap.playing.playback;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.mt1006.mocap.MocapMod;
import net.mt1006.mocap.events.PlayerConnectionEvent;
import net.mt1006.mocap.mocap.playing.Playing;
import net.mt1006.mocap.mocap.playing.modifiers.PlaybackModifiers;
import net.mt1006.mocap.mocap.settings.Settings;
import net.mt1006.mocap.mocap.settings.enums.EntitiesAfterPlayback;
import net.mt1006.mocap.network.MocapPacketS2C;
import net.mt1006.mocap.utils.FakePlayer;
import net.mt1006.mocap.utils.Utils;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class ActionContext
{
	private final ServerPlayer owner;
	private final PlayerList packetTargets;
	private final EntityData mainEntityData;
	public final Map<Integer, EntityData> entityDataMap = new HashMap<>();
	public final ServerLevel level;
	public final PlaybackModifiers modifiers;
	public final @Nullable FakePlayer ghostPlayer;
	public final PositionTransformer transformer;
	private boolean mainEntityRemoved = false;
	private @Nullable EntityData currentEntityData = null;
	public Entity entity;
	private Vec3 position;
	public int skippingTicks = 0;

	public ActionContext(ServerPlayer owner, PlayerList packetTargets, Entity entity, Vec3 startPos,
						 PlaybackModifiers modifiers, @Nullable FakePlayer ghostPlayer, PositionTransformer transformer)
	{
		if (!(entity.level() instanceof ServerLevel)) { throw new RuntimeException("Failed to get ServerLevel for ActionContext!"); }

		this.owner = owner;
		this.packetTargets = packetTargets;
		this.mainEntityData = new EntityData(entity, startPos);
		this.level = (ServerLevel)entity.level();
		this.modifiers = modifiers;
		this.ghostPlayer = ghostPlayer;
		this.transformer = transformer;

		setMainContextEntity();
	}

	public void setMainContextEntity()
	{
		setContextEntity(mainEntityData);
	}

	public boolean setContextEntity(int id)
	{
		EntityData data = entityDataMap.get(id);
		if (data == null) { return false; }

		setContextEntity(data);
		return true;
	}

	private void setContextEntity(EntityData data)
	{
		if (currentEntityData != null) { currentEntityData.lastPosition = position; }
		currentEntityData = data;

		entity = data.entity;
		position = data.lastPosition;
	}

	public void broadcast(Packet<?> packet)
	{
		packetTargets.broadcastAll(packet);
	}

	public void fluentMovement(Supplier<Packet<?>> packetSupplier)
	{
		double fluentMovements = Settings.FLUENT_MOVEMENTS.val;
		if (fluentMovements == 0.0) { return; }
		Packet<?> packet = packetSupplier.get();

		if (fluentMovements > 0.0)
		{
			Vec3 pos = entity.position();
			double maxDistSqr = fluentMovements * fluentMovements;

			for (ServerPlayer player : packetTargets.getPlayers())
			{
				if (player.distanceToSqr(pos) > maxDistSqr) { continue; }
				player.connection.send(packet);
			}
		}
		else
		{
			packetTargets.broadcastAll(packet);
		}
	}

	public void removeEntities()
	{
		removeMainEntity();
		entityDataMap.values().forEach((data) -> removeEntity(data.entity));
		entityDataMap.clear();
	}

	public void removeMainEntity()
	{
		if (mainEntityRemoved) { return; }
		mainEntityRemoved = true;

		FakePlayer playerToRemove;
		if (entity instanceof Player)
		{
			if (!(entity instanceof FakePlayer))
			{
				Utils.sendMessage(owner, "error.failed_to_remove_fake_player");
				MocapMod.LOGGER.error("Failed to remove fake player!");
				return;
			}
			playerToRemove = (FakePlayer)entity;
		}
		else
		{
			removeEntity(entity);
			if (ghostPlayer == null) { return; }
			playerToRemove = ghostPlayer;
		}

		UUID uuid = playerToRemove.getUUID();
		if (PlayerConnectionEvent.nocolPlayers.contains(uuid))
		{
			for (ServerPlayer player : PlayerConnectionEvent.players)
			{
				MocapPacketS2C.sendNocolPlayerRemove(player, uuid);
				PlayerConnectionEvent.removeNocolPlayer(uuid);
			}
		}
		if (playerToRemove != ghostPlayer) { broadcast(new ClientboundPlayerInfoRemovePacket(List.of(uuid))); }
		playerToRemove.remove(Entity.RemovalReason.KILLED);
		playerToRemove.getAdvancements().stopListening();
	}

	public void changePosition(Vec3 newPos, float rotY, float rotX, boolean shiftXZ, boolean shiftY, boolean transformRot)
	{
		double x = shiftXZ ? (position.x + newPos.x) : newPos.x;
		double y = shiftY ? (position.y + newPos.y) : newPos.y;
		double z = shiftXZ ? (position.z + newPos.z) : newPos.z;
		position = new Vec3(x, y, z);

		Vec3 finPos = transformer.transformPos(position);
		float finRotY = transformRot ? transformer.transformRotation(rotY) : rotY;

		entity.moveTo(finPos, finRotY, rotX);
		if (ghostPlayer != null && entity == mainEntityData.entity) { ghostPlayer.moveTo(finPos, finRotY, rotX); }
	}

	//TODO: restore?
	/*public void changePosition(double x, double y, double z, float rotY, float rotX, @Nullable ResourceLocation dimensionId)
	{
		ServerLevel newLevel = dimensionId != null
				? level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId))
				: null;

		if (newLevel == level || newLevel == null)
		{
			changePosition(x, y, z, rotY, rotX, false, false);
			return;
		}

		position[0] = modifiers.offset.x + x;
		position[1] = modifiers.offset.y + y;
		position[2] = modifiers.offset.z + z;

		DimensionTransition dimensionTransition = new DimensionTransition(
				newLevel, new Vec3(position[0], position[1], position[2]), Vec3.ZERO, rotY, rotX, (ctx) -> {});

		entity.changeDimension(dimensionTransition);
		if (ghostPlayer != null) { ghostPlayer.changeDimension(dimensionTransition); }
	}*/

	private static void removeEntity(Entity entity)
	{
		switch (Settings.ENTITIES_AFTER_PLAYBACK.val)
		{
			case EntitiesAfterPlayback.REMOVE:
				entity.remove(Entity.RemovalReason.KILLED);

			case EntitiesAfterPlayback.KILL:
				entity.invulnerableTime = 0; // for sound effect
				if (entity instanceof FakePlayer) { ((FakePlayer)entity).fakeKill(); }
				else { entity.kill(null); }
				break;

			case EntitiesAfterPlayback.LEFT_UNTOUCHED:
				break;

			case EntitiesAfterPlayback.RELEASE_AS_NORMAL:
				entity.setNoGravity(false);
				entity.setInvulnerable(false);
				entity.removeTag(Playing.MOCAP_ENTITY_TAG);
				if (entity instanceof Mob) { ((Mob)entity).setNoAi(false); }
				break;

			default:
				throw new IllegalStateException("Unexpected value: " + Settings.ENTITIES_AFTER_PLAYBACK.val);
		}
	}

	public static class EntityData
	{
		public final Entity entity;
		public Vec3 lastPosition;

		public EntityData(Entity entity, Vec3 startPos)
		{
			this.entity = entity;
			this.lastPosition = startPos;
		}
	}
}
