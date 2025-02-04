package com.mt1006.mocap.mocap.files;

import com.google.gson.*;
import com.mt1006.mocap.MocapMod;
import com.mt1006.mocap.command.CommandSuggestions;
import com.mt1006.mocap.command.io.CommandOutput;
import com.mt1006.mocap.mocap.playing.modifiers.PlaybackModifiers;
import com.mt1006.mocap.mocap.settings.Settings;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SceneData
{
	public final List<Subscene> subscenes = new ArrayList<>();
	public int version = 0;
	public boolean experimentalVersion = false;
	public long fileSize = 0;

	public static SceneData empty()
	{
		SceneData sceneData = new SceneData();
		sceneData.version = SceneFiles.VERSION;
		sceneData.experimentalVersion = MocapMod.EXPERIMENTAL;
		return sceneData;
	}

	public boolean save(CommandOutput commandOutput, File file, String sceneName, String onSuccess, String onError)
	{
		JsonObject json = new JsonObject();
		json.add("version", new JsonPrimitive(experimentalVersion ? (-version) : version));

		JsonArray subscenesArray = new JsonArray();
		subscenes.forEach((s) -> subscenesArray.add(s.toJson()));
		json.add("subscenes", subscenesArray);

		try
		{
			FileWriter writer = new FileWriter(file);
			GsonBuilder gsonBuilder = Settings.PRETTY_SCENE_FILES.val ? new GsonBuilder().setPrettyPrinting() : new GsonBuilder();
			gsonBuilder.create().toJson(json, writer);
			writer.close();

			saveToSceneElementCache(sceneName);
			commandOutput.sendSuccess(onSuccess);
			return true;
		}
		catch (Exception e)
		{
			commandOutput.sendException(e, onError);
			return false;
		}
	}

	public boolean load(CommandOutput commandOutput, String name)
	{
		byte[] data = Files.loadFile(Files.getSceneFile(commandOutput, name));
		return data != null && load(commandOutput, data);
	}

	private boolean load(CommandOutput commandOutput, byte[] scene)
	{
		fileSize = scene.length;

		LegacySceneDataParser legacyParser = new LegacySceneDataParser(this, commandOutput, scene);
		if (legacyParser.isLegacy()) { return legacyParser.wasParsed(); }

		try
		{
			JsonElement jsonElement = new JsonParser().parse(new InputStreamReader(new ByteArrayInputStream(scene)));
			JsonObject json = jsonElement.getAsJsonObject();
			if (json == null) { throw new Exception("Scene file isn't a JSON object!"); }

			JsonElement versionElement = json.get("version");
			if (versionElement == null) { throw new Exception("Scene version not specified!"); }
			if (!setAndVerifyVersion(commandOutput, versionElement.getAsInt())) { return false; }

			JsonElement subsceneArrayElement = json.get("subscenes");
			if (subsceneArrayElement == null) { throw new Exception("Scene subscenes list not found!"); }

			for (JsonElement subsceneElement : subsceneArrayElement.getAsJsonArray())
			{
				JsonObject subsceneObject = subsceneElement.getAsJsonObject();
				if (subsceneObject == null) { throw new Exception("Scene subscene isn't a JSON object!"); }
				subscenes.add(new Subscene(subsceneObject));
			}
			return true;
		}
		catch (Exception e)
		{
			commandOutput.sendException(e, "error.failed_to_load_scene");
			return false;
		}
	}

	public boolean setAndVerifyVersion(CommandOutput commandOutput, int versionNumber)
	{
		version = Math.abs(versionNumber);
		experimentalVersion = (versionNumber < 0);

		if (version > SceneFiles.VERSION)
		{
			commandOutput.sendFailure("error.failed_to_load_scene");
			commandOutput.sendFailure("error.failed_to_load_scene.not_supported");
			return false;
		}
		return true;
	}

	public @Nullable List<String> saveToSceneElementCache(String sceneName)
	{
		List<String> elements = new ArrayList<>(subscenes.size());
		int id = 1;
		for (SceneData.Subscene subscene : subscenes)
		{
			elements.add(String.format("%03d-%s", id, subscene.name));
			id++;
		}

		CommandSuggestions.sceneElementCache.put(sceneName, elements);
		return elements;
	}

	public static class Subscene
	{
		public String name;
		public PlaybackModifiers modifiers;

		public Subscene(String name, PlaybackModifiers modifiers)
		{
			this.name = name;
			this.modifiers = modifiers;
		}

		public Subscene(JsonObject json) throws Exception
		{
			JsonElement nameElement = json.get("name");
			if (nameElement == null) { throw new Exception("JSON \"name\" element not found!"); }

			name = nameElement.getAsString();
			modifiers = new PlaybackModifiers(json);
		}

		public JsonObject toJson()
		{
			JsonObject json = new JsonObject();
			json.add("name", new JsonPrimitive(name));
			modifiers.addToJson(json);
			return json;
		}

		public Subscene copy()
		{
			try
			{
				return new Subscene(toJson());
			}
			catch (Exception e) { throw new RuntimeException("Something went wrong when copying subscene!"); }
		}
	}
}
