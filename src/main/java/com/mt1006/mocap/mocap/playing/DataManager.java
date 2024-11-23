package com.mt1006.mocap.mocap.playing;

import com.mt1006.mocap.command.io.CommandInfo;
import com.mt1006.mocap.command.io.CommandOutput;
import com.mt1006.mocap.mocap.files.RecordingData;
import com.mt1006.mocap.mocap.files.SceneData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class DataManager
{
	private final Map<String, RecordingData> recordingMap = new HashMap<>();
	private final Map<String, SceneData> sceneMap = new HashMap<>();
	private final Stack<String> resourceStack = new Stack<>();
	public boolean knownError = false;

	public boolean load(CommandOutput commandOutput, String name)
	{
		if (name.charAt(0) == '.')
		{
			if (resourceStack.contains(name))
			{
				commandOutput.sendFailure("playback.start.error.loop");
				resourceStack.push(name);
				knownError = true;
				return false;
			}

			resourceStack.push(name);

			if (!loadResource(commandOutput, name)) { return false; }

			SceneData scene = getScene(name);
			if (scene == null) { return false; }

			for (SceneData.Subscene subscene : scene.subscenes)
			{
				if (!load(commandOutput, subscene.name)) { return false; }
			}
		}
		else
		{
			resourceStack.push(name);
			if (!loadResource(commandOutput, name)) { return false; }
		}

		resourceStack.pop();
		return true;
	}

	public @Nullable RecordingData getRecording(String name)
	{
		return recordingMap.get(name);
	}

	public @Nullable SceneData getScene(String name)
	{
		return sceneMap.get(name);
	}

	public String getResourcePath()
	{
		StringBuilder retStr = new StringBuilder();
		for (String str : resourceStack)
		{
			retStr.append("/");
			retStr.append(str);
		}
		return new String(retStr);
	}

	private boolean loadResource(CommandOutput commandOutput, String name)
	{
		if (name.charAt(0) == '.')
		{
			if (sceneMap.containsKey(name)) { return true; }
			SceneData sceneData = new SceneData();
			if (!sceneData.load(commandOutput, name)) { return false; }
			sceneMap.put(name, sceneData);
		}
		else
		{
			if (recordingMap.containsKey(name)) { return true; }
			RecordingData recording = new RecordingData();
			if (!recording.load(commandOutput, name)) { return false; }
			recordingMap.put(name, recording);
		}
		return true;
	}
}
