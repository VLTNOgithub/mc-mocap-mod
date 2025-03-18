package net.mt1006.mocap.mocap.actions;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;
import net.mt1006.mocap.mocap.settings.Settings;

public class SetSpectator implements ComparableAction
{
	private final boolean isSpectator;

	public SetSpectator(Entity entity)
	{
		isSpectator = entity instanceof ServerPlayer && (((ServerPlayer)entity).gameMode.getGameModeForPlayer() == GameType.SPECTATOR);
	}

	public SetSpectator(RecordingFiles.Reader reader)
	{
		isSpectator = reader.readBoolean();
	}

	@Override public boolean differs(ComparableAction previousAction)
	{
		return isSpectator != ((SetSpectator)previousAction).isSpectator;
	}

	@Override public void write(RecordingFiles.Writer writer)
	{
		writer.addByte(Type.SET_SPECTATOR.id);
		writer.addBoolean(isSpectator);
	}

	@Override public Result execute(ActionContext ctx)
	{
		if (!(ctx.entity instanceof ServerPlayer)) { return Result.IGNORED; }
		((ServerPlayer)ctx.entity).setGameMode(isSpectator
				? GameType.SPECTATOR
				: (Settings.USE_CREATIVE_GAME_MODE.val ? GameType.CREATIVE : GameType.SURVIVAL));
		return Result.OK;
	}
}
