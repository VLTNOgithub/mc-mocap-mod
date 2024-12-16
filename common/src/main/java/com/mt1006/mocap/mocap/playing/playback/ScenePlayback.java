package com.mt1006.mocap.mocap.playing.playback;

import com.mt1006.mocap.command.io.CommandInfo;
import com.mt1006.mocap.mocap.files.SceneData;
import com.mt1006.mocap.mocap.playing.DataManager;
import com.mt1006.mocap.mocap.playing.modifiers.PlayerSkin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ScenePlayback extends Playback
{
	private final List<Playback> subscenes = new ArrayList<>();

	protected static @Nullable ScenePlayback startRoot(CommandInfo commandInfo, DataManager dataManager,
													   String name, @Nullable String playerName, PlayerSkin playerSkin)
	{
		try { return new ScenePlayback(commandInfo, dataManager, name, playerName, playerSkin, null, null); }
		catch (StartException e) { return null; }
	}

	protected static @Nullable ScenePlayback startSubscene(CommandInfo commandInfo, DataManager dataManager, Playback parent, SceneData.Subscene info)
	{
		try { return new ScenePlayback(commandInfo, dataManager, info.name, null, null, parent, info); }
		catch (StartException e) { return null; }
	}

	private ScenePlayback(CommandInfo commandInfo, DataManager dataManager, String name, @Nullable String rootPlayerName,
						  @Nullable PlayerSkin rootPlayerSkin, @Nullable Playback parent, @Nullable SceneData.Subscene info) throws StartException
	{
		super(parent == null, commandInfo.level, commandInfo.sourcePlayer, rootPlayerName, rootPlayerSkin, parent, info);

		SceneData sceneData = dataManager.getScene(name);
		if (sceneData == null) { return; }

		if (sceneData.subscenes.isEmpty() && root)
		{
			commandInfo.sendFailureWithTip("playback.start.error.empty_scene");
			throw new StartException();
		}

		for (SceneData.Subscene subscene : sceneData.subscenes)
		{
			Playback playback = Playback.start(commandInfo, dataManager, this, subscene);
			if (playback == null) { return; }
			subscenes.add(playback);
		}
	}

	@Override public boolean tick()
	{
		if (finished) { return true; }

		if (shouldExecuteTick())
		{
			finished = true;
			for (Playback scene : subscenes)
			{
				if (!scene.tick()) { finished = false; }
			}
		}

		if (root && finished) { stop(); }

		tickCounter++;
		return finished;
	}

	@Override public void stop()
	{
		subscenes.forEach(Playback::stop);
		finished = true;
	}

	@Override public boolean isFinished()
	{
		return finished;
	}
}
