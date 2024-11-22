package com.mt1006.mocap.mocap.files;

import com.google.gson.*;
import com.mt1006.mocap.MocapMod;
import com.mt1006.mocap.command.io.CommandOutput;
import com.mt1006.mocap.mocap.playing.modifiers.EntityFilter;
import com.mt1006.mocap.mocap.playing.modifiers.PlayerData;
import com.mt1006.mocap.utils.Utils;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class SceneData
{
	public final ArrayList<Subscene> subscenes = new ArrayList<>();
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

	public boolean save(CommandOutput commandOutput, File file, String onSuccess, String onError)
	{
		JsonObject json = new JsonObject();
		json.add("version", new JsonPrimitive(experimentalVersion ? (-version) : version));

		JsonArray subscenesArray = new JsonArray();
		subscenes.forEach((s) -> subscenesArray.add(s.toJson()));
		json.add("subscenes", subscenesArray);

		try
		{
			FileWriter writer = new FileWriter(file);
			//TODO: add "pretty_scene_files" setting
			new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
			writer.close();

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

	public boolean load(CommandOutput commandOutput, byte[] scene)
	{
		fileSize = scene.length;

		LegacySceneDataParser legacyParser = new LegacySceneDataParser(this, commandOutput, scene);
		if (legacyParser.isLegacy()) { return legacyParser.wasParsed(); }

		try
		{
			JsonElement jsonElement = JsonParser.parseReader(new InputStreamReader(new ByteArrayInputStream(scene)));
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

	public static class Subscene
	{
		public String name;
		public double startDelay = 0.0;
		public double[] offset = new double[3];
		public PlayerData playerData = PlayerData.EMPTY;
		public @Nullable String playerAsEntity = null;
		public EntityFilter entityFilter = EntityFilter.FOR_PLAYBACK; //TODO: implement

		public Subscene(String name)
		{
			this.name = name;
		}

		public Subscene(JsonObject json) throws Exception
		{
			JsonElement nameElement = json.get("name");
			if (nameElement == null) { throw new Exception("JSON \"name\" element not found!"); }
			name = nameElement.getAsString();

			startDelay = getJsonDouble(json, "start_delay");
			offset[0] = getJsonDouble(json, "offset_x");
			offset[1] = getJsonDouble(json, "offset_y");
			offset[2] = getJsonDouble(json, "offset_z");

			JsonElement playerDataElement = json.get("player_data");
			playerData = new PlayerData(playerDataElement != null ? playerDataElement.getAsJsonObject() : null);
			
			JsonElement playerAsEntityElement = json.get("player_as_entity");
			playerAsEntity = playerAsEntityElement != null ? Utils.toNullableStr(playerAsEntityElement.getAsString()) : null;
		}

		public JsonObject toJson()
		{
			JsonObject json = new JsonObject();
			json.add("name", new JsonPrimitive(name));

			addJsonDouble(json, "start_delay", startDelay);
			addJsonDouble(json, "offset_x", offset[0]);
			addJsonDouble(json, "offset_y", offset[1]);
			addJsonDouble(json, "offset_z", offset[2]);

			JsonObject playerDataJson = playerData.toJson();
			if (playerDataJson != null) { json.add("player_data", playerDataJson); }
			if (playerAsEntity != null) { json.add("player_as_entity", new JsonPrimitive(playerAsEntity)); }
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

		private static double getJsonDouble(JsonObject json, String name)
		{
			JsonElement element = json.get(name);
			return element != null ? element.getAsDouble() : 0.0;
		}

		private static void addJsonDouble(JsonObject json, String name, double val)
		{
			if (val != 0.0) { json.add(name, new JsonPrimitive(val)); }
		}
	}
}
