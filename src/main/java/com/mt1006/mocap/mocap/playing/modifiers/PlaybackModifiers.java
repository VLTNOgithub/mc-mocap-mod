package com.mt1006.mocap.mocap.playing.modifiers;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

public class PlaybackModifiers
{
	public final PlayerData playerData;
	public final PlayerAsEntity playerAsEntity;
	public final Vec3 offset;
	public final Vec3i blockOffset;
	public final int startDelay;
	public EntityFilter entityFilter;

	private PlaybackModifiers(PlayerData playerData, PlayerAsEntity playerAsEntity, Vec3 offset, int startDelay, EntityFilter entityFilter)
	{
		this.playerData = playerData;
		this.playerAsEntity = playerAsEntity;
		this.offset = offset;
		this.blockOffset = calculateBlockOffset(offset);
		this.startDelay = startDelay;
		this.entityFilter = entityFilter;
	}

	public static PlaybackModifiers forRoot(PlayerData playerData)
	{
		return new PlaybackModifiers(playerData, PlayerAsEntity.DISABLED, Vec3.ZERO, 0, EntityFilter.FOR_PLAYBACK);
	}

	public static PlaybackModifiers fromParent(PlaybackModifiers parent, PlayerData playerData, PlayerAsEntity playerAsEntity,
											   double[] offset, double startDelay, EntityFilter entityFilter)
	{
		return new PlaybackModifiers(
				playerData.mergeWithParent(parent.playerData),
				playerAsEntity.isEnabled() ? playerAsEntity : parent.playerAsEntity,
				parent.offset.add(offset[0], offset[1], offset[2]),
				(int)Math.round(startDelay * 20.0),
				entityFilter);
	}

	private static Vec3i calculateBlockOffset(Vec3 offset)
	{
		return new Vec3i((int)Math.round(offset.x), (int)Math.round(offset.y), (int)Math.round(offset.z));
	}
}
