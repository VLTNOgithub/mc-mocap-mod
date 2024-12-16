package com.mt1006.mocap.mocap.playing.modifiers;

import com.mt1006.mocap.command.io.CommandOutput;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class PlaybackModifiers
{
	public static final PlaybackModifiers EMPTY = forRoot(null, PlayerSkin.DEFAULT);
	public final @Nullable String playerName;
	public final PlayerSkin playerSkin;
	public final PlayerAsEntity playerAsEntity;
	public final Vec3 offset;
	public final Vec3i blockOffset;
	public final int startDelay;
	public final EntityFilter entityFilter;

	private PlaybackModifiers(@Nullable String playerName, PlayerSkin playerSkin, PlayerAsEntity playerAsEntity,
							  Vec3 offset, int startDelay, EntityFilter entityFilter)
	{
		this.playerName = playerName;
		this.playerSkin = playerSkin;
		this.playerAsEntity = playerAsEntity;
		this.offset = offset;
		this.blockOffset = calculateBlockOffset(offset);
		this.startDelay = startDelay;
		this.entityFilter = entityFilter;
	}

	public static PlaybackModifiers forRoot(@Nullable String playerName, PlayerSkin playerSkin)
	{
		return new PlaybackModifiers(playerName, playerSkin, PlayerAsEntity.DISABLED, Vec3.ZERO, 0, EntityFilter.FOR_PLAYBACK);
	}

	public static PlaybackModifiers fromParent(PlaybackModifiers parent, @Nullable String playerName, PlayerSkin playerSkin,
											   PlayerAsEntity playerAsEntity, double[] offset, double startDelay, EntityFilter entityFilter)
	{
		return new PlaybackModifiers(
				playerName != null ? playerName : parent.playerName,
				playerSkin.mergeWithParent(parent.playerSkin),
				playerAsEntity.isEnabled() ? playerAsEntity : parent.playerAsEntity,
				parent.offset.add(offset[0], offset[1], offset[2]),
				(int)Math.round(startDelay * 20.0),
				entityFilter);
	}

	public static boolean checkIfProperName(CommandOutput commandOutput, @Nullable String name)
	{
		if (name == null) { return true; }

		if (name.length() > 16)
		{
			commandOutput.sendFailure("scenes.add_to.failed");
			commandOutput.sendFailure("scenes.add_to.failed.too_long_name");
			return false;
		}

		if (name.contains(" "))
		{
			commandOutput.sendFailure("scenes.add_to.failed");
			commandOutput.sendFailure("scenes.add_to.failed.contain_spaces");
			return false;
		}
		return true;
	}

	private static Vec3i calculateBlockOffset(Vec3 offset)
	{
		return new Vec3i((int)Math.round(offset.x), (int)Math.round(offset.y), (int)Math.round(offset.z));
	}
}
