package com.mt1006.mocap.mocap.playing.playback;

import com.mt1006.mocap.MocapMod;
import com.mt1006.mocap.events.PlayerConnectionEvent;
import com.mt1006.mocap.mocap.playing.Playing;
import com.mt1006.mocap.mocap.playing.modifiers.PlaybackModifiers;
import com.mt1006.mocap.mocap.settings.Settings;
import com.mt1006.mocap.network.MocapPacketS2C;
import com.mt1006.mocap.utils.FakePlayer;
import com.mt1006.mocap.utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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
	public final Level level;
	public final PlaybackModifiers modifiers;
	public final @Nullable FakePlayer ghostPlayer;
	public final Vec3 startPos;
	private boolean mainEntityRemoved = false;
	public Entity entity;
	private double[] position;
	public int skippingTicks = 0;

	public ActionContext(ServerPlayer owner, PlayerList packetTargets, Entity entity,
						 PlaybackModifiers modifiers, @Nullable FakePlayer ghostPlayer, double[] startPos)
	{
		this.owner = owner;
		this.packetTargets = packetTargets;
		this.mainEntityData = new EntityData(entity);
		this.level = entity.level();
		this.modifiers = modifiers;
		this.ghostPlayer = ghostPlayer;
		this.startPos = modifiers.offset.add(startPos[0], startPos[1], startPos[2]);

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
		entity = data.entity;
		position = data.position;
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
	}

	public void changePosition(double x, double y, double z, float rotY, float rotX, boolean shiftXZ, boolean shiftY)
	{
		position[0] = shiftXZ ? (position[0] + x) : (modifiers.offset.x + x);
		position[1] = shiftY ? (position[1] + y) : (modifiers.offset.y + y);
		position[2] = shiftXZ ? (position[2] + z) : (modifiers.offset.z + z);

		double scale = modifiers.scale.sceneScale;
		double[] moveToPos = position;
		if (scale != 1.0)
		{
			moveToPos = new double[3];
			moveToPos[0] = ((position[0] - startPos.x) * scale) + startPos.x;
			moveToPos[1] = ((position[1] - startPos.y) * scale) + startPos.y;
			moveToPos[2] = ((position[2] - startPos.z) * scale) + startPos.z;
		}

		entity.moveTo(moveToPos[0], moveToPos[1], moveToPos[2], rotY, rotX);
		if (ghostPlayer != null) { ghostPlayer.moveTo(moveToPos[0], moveToPos[1], moveToPos[2], rotY, rotX); }
	}

	private static void removeEntity(Entity entity)
	{
		switch (Settings.ENTITIES_AFTER_PLAYBACK.val)
		{
			case -1:
				entity.setNoGravity(false);
				entity.setInvulnerable(false);
				entity.removeTag(Playing.MOCAP_ENTITY_TAG);
				if (entity instanceof Mob) { ((Mob)entity).setNoAi(false); }
				break;

			case 0:
				break;

			case 2:
				entity.invulnerableTime = 0; // for sound effect
				entity.kill();
				break;

			default: // case 1
				entity.remove(Entity.RemovalReason.KILLED);
		}
	}

	public BlockPos shiftBlockPos(BlockPos blockPos)
	{
		return blockPos.offset(modifiers.offset.blockOffset);
	}

	public static class EntityData
	{
		public final Entity entity;
		public final double[] position = new double[3];

		public EntityData(Entity entity)
		{
			this.entity = entity;

			Vec3 posVec = entity.position();
			this.position[0] = posVec.x;
			this.position[1] = posVec.y;
			this.position[2] = posVec.z;
		}
	}
}
