package com.mt1006.mocap.mocap.files;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mt1006.mocap.MocapMod;
import com.mt1006.mocap.command.CommandSuggestions;
import com.mt1006.mocap.command.io.CommandInfo;
import com.mt1006.mocap.command.io.CommandOutput;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SceneFiles
{
	public static final int VERSION = MocapMod.SCENE_FORMAT_VERSION;

	public static boolean add(CommandOutput commandOutput, String name)
	{
		File file = Files.getSceneFile(commandOutput, name);
		if (file == null) { return false; }
		if (file.exists())
		{
			commandOutput.sendFailure("scenes.add.already_exists");
			return false;
		}

		SceneData sceneData = SceneData.empty();
		boolean success = sceneData.save(commandOutput, file, name, "scenes.add.success", "scenes.add.error");
		if (success) { CommandSuggestions.inputSet.add(nameWithDot(name)); }
		return success;
	}

	public static boolean copy(CommandOutput commandOutput, String srcName, String destName)
	{
		File srcFile = Files.getSceneFile(commandOutput, srcName);
		if (srcFile == null) { return false; }

		File destFile = Files.getSceneFile(commandOutput, destName);
		if (destFile == null) { return false; }

		try { FileUtils.copyFile(srcFile, destFile); }
		catch (IOException e)
		{
			commandOutput.sendException(e, "scenes.copy.failed");
			return false;
		}

		CommandSuggestions.inputSet.add(nameWithDot(destName));
		List<String> elementCache = CommandSuggestions.sceneElementCache.get(nameWithDot(srcName));
		if (elementCache != null)
		{
			CommandSuggestions.sceneElementCache.put(nameWithDot(destName), new ArrayList<>(elementCache));
		}

		commandOutput.sendSuccess("scenes.copy.success");
		return true;
	}

	public static boolean rename(CommandOutput commandOutput, String oldName, String newName)
	{
		File oldFile = Files.getSceneFile(commandOutput, oldName);
		if (oldFile == null) { return false; }

		File newFile = Files.getSceneFile(commandOutput, newName);
		if (newFile == null) { return false; }

		if (!oldFile.renameTo(newFile))
		{
			commandOutput.sendFailure("scenes.rename.failed");
			return false;
		}

		CommandSuggestions.inputSet.remove(nameWithDot(oldName));
		CommandSuggestions.inputSet.add(nameWithDot(newName));
		List<String> elementCache = CommandSuggestions.sceneElementCache.get(nameWithDot(oldName));
		if (elementCache != null)
		{
			CommandSuggestions.sceneElementCache.remove(nameWithDot(oldName));
			CommandSuggestions.sceneElementCache.put(nameWithDot(newName), elementCache);
		}

		commandOutput.sendSuccess("scenes.rename.success");
		return true;
	}

	public static boolean remove(CommandOutput commandOutput, String name)
	{
		File sceneFile = Files.getSceneFile(commandOutput, name);
		if (sceneFile == null) { return false; }

		if (!sceneFile.delete())
		{
			commandOutput.sendFailure("scenes.remove.failed");
			return false;
		}

		CommandSuggestions.inputSet.remove(nameWithDot(name));
		CommandSuggestions.sceneElementCache.remove(nameWithDot(name));
		commandOutput.sendSuccess("scenes.remove.success");
		return true;
	}

	public static boolean addElement(CommandOutput commandOutput, String name, SceneData.Subscene subscene)
	{
		File file = Files.getSceneFile(commandOutput, name);
		if (file == null) { return false; }
		if (!file.exists())
		{
			commandOutput.sendFailure("scenes.add_to.failed");
			commandOutput.sendFailure("scenes.error.file_not_exists"); //TODO: make it a failure
			return false;
		}

		SceneData sceneData = new SceneData();
		if (!sceneData.load(commandOutput, name)) { return false; }

		sceneData.subscenes.add(subscene);
		return sceneData.save(commandOutput, file, name, "scenes.add_to.success", "scenes.add_to.error");
	}

	public static boolean removeElement(CommandOutput commandOutput, String name, int pos)
	{
		File file = Files.getSceneFile(commandOutput, name);
		if (file == null) { return false; }
		if (!file.exists())
		{
			commandOutput.sendFailure("scenes.remove_from.error");
			commandOutput.sendFailure("scenes.error.file_not_exists");
			return false;
		}

		SceneData sceneData = new SceneData();
		if (!sceneData.load(commandOutput, name)) { return false; }
		if (sceneData.subscenes.size() < pos || pos < 1)
		{
			commandOutput.sendFailure("scenes.remove_from.error");
			commandOutput.sendFailureWithTip("scenes.error.wrong_element_pos");
			return false;
		}

		sceneData.subscenes.remove(pos - 1);
		return sceneData.save(commandOutput, file, name, "scenes.remove_from.success", "scenes.remove_from.error");
	}

	public static boolean modify(CommandInfo commandInfo, String name, int pos, @Nullable String expectedName)
	{
		File file = Files.getSceneFile(commandInfo, name);
		if (file == null) { return false; }
		if (!file.exists())
		{
			commandInfo.sendFailure("scenes.modify.error");
			commandInfo.sendFailure("scenes.error.file_not_exists");
			return false;
		}

		SceneData sceneData = new SceneData();
		if (!sceneData.load(commandInfo, name)) { return false; }
		if (sceneData.subscenes.size() < pos || pos < 1)
		{
			commandInfo.sendFailure("scenes.modify.error");
			commandInfo.sendFailureWithTip("scenes.error.wrong_element_pos");
			return false;
		}

		SceneData.Subscene subscene = sceneData.subscenes.get(pos - 1);
		if (expectedName != null && !expectedName.equals(subscene.name))
		{
			commandInfo.sendFailure("scenes.modify.error");
			commandInfo.sendFailure("scenes.error.wrong_subscene_name");
			return false;
		}

		SceneData.Subscene newSubscene = modifySubscene(commandInfo, subscene);
		if (newSubscene == null)
		{
			commandInfo.sendFailure("scenes.modify.error");
			return false;
		}

		sceneData.subscenes.set(pos - 1, newSubscene);
		return sceneData.save(commandInfo, file, name, "scenes.modify.success", "scenes.modify.error");
	}

	private static @Nullable SceneData.Subscene modifySubscene(CommandInfo rootCommandInfo, SceneData.Subscene oldSubscene)
	{
		SceneData.Subscene subscene = oldSubscene.copy();

		CommandInfo commandInfo = rootCommandInfo.getFinalCommandInfo();
		if (commandInfo == null)
		{
			rootCommandInfo.sendFailure("error.unable_to_get_argument");
			return null;
		}

		String propertyName = commandInfo.getNode(5);
		if (propertyName == null)
		{
			rootCommandInfo.sendFailure("error.unable_to_get_argument");
			return null;
		}

		try
		{
			if (propertyName.equals("subscene_name"))
			{
				subscene.name = commandInfo.getString("new_name");
				return subscene;
			}

			boolean success = subscene.modifiers.modify(commandInfo, propertyName, 5);
			if (!success)
			{
				rootCommandInfo.sendFailure("error.generic");
				return null;
			}

			return subscene;
		}
		catch (Exception e)
		{
			rootCommandInfo.sendException(e, "error.unable_to_get_argument");
			return null;
		}
	}

	public static boolean elementInfo(CommandOutput commandOutput, String name, int pos)
	{
		File sceneFile = Files.getSceneFile(commandOutput, name);
		if (sceneFile == null) { return false; }

		if (!sceneFile.exists())
		{
			commandOutput.sendFailure("scenes.element_info.failed");
			commandOutput.sendFailure("scenes.error.file_not_exists");
			return false;
		}

		SceneData sceneData = new SceneData();
		if (!sceneData.load(commandOutput, name)) { return false; }

		if (sceneData.subscenes.size() < pos || pos < 1)
		{
			commandOutput.sendFailure("scenes.element_info.failed");
			commandOutput.sendFailureWithTip("scenes.error.wrong_element_pos");
			return false;
		}

		SceneData.Subscene subscene = sceneData.subscenes.get(pos - 1);

		commandOutput.sendSuccess("scenes.element_info.info");
		commandOutput.sendSuccess("scenes.element_info.id", name, pos);
		commandOutput.sendSuccess("scenes.element_info.name", subscene.name);

		subscene.modifiers.list(commandOutput);
		return true;
	}

	public static boolean listElements(CommandOutput commandOutput, String name)
	{
		SceneData sceneData = new SceneData();
		if (!sceneData.load(commandOutput, name)) { return false; }

		commandOutput.sendSuccess("scenes.list_elements");

		int i = 1;
		for (SceneData.Subscene element : sceneData.subscenes)
		{
			commandOutput.sendSuccessLiteral("[%d] %s <%.3f> [%.3f; %.3f; %.3f] (%s)", i++, element.name, element.modifiers.startDelay.seconds,
					element.modifiers.offset.x, element.modifiers.offset.y, element.modifiers.offset.z, element.modifiers.playerName);
		}

		commandOutput.sendSuccessLiteral("[id] name <start_delay> [x; y; z] (player_name)");
		return true;
	}

	public static boolean info(CommandOutput commandOutput, String name)
	{
		SceneData sceneData = new SceneData();

		if (!sceneData.load(commandOutput, name) && sceneData.version <= VERSION)
		{
			commandOutput.sendFailure("scenes.info.failed");
			return false;
		}

		commandOutput.sendSuccess("scenes.info.info");
		commandOutput.sendSuccess("file.info.name", name);
		if (!Files.printVersionInfo(commandOutput, VERSION, sceneData.version, sceneData.experimentalVersion)) { return true; }

		commandOutput.sendSuccess("scenes.info.size", String.format("%.2f", sceneData.fileSize / 1024.0), sceneData.subscenes.size());
		return true;
	}

	public static @Nullable List<String> list()
	{
		if (!Files.initialized) { return null; }

		String[] fileList = Files.sceneDirectory.list(Files::isSceneFile);
		if (fileList == null) { return null; }

		ArrayList<String> scenes = new ArrayList<>();
		for (String filename : fileList)
		{
			scenes.add("." + filename.substring(0, filename.lastIndexOf('.')));
		}

		Collections.sort(scenes);
		return scenes;
	}

	private static String nameWithDot(String name)
	{
		return name.charAt(0) == '.' ? name : ("." + name);
	}

	public record Writer(JsonObject json)
	{
		public Writer()
		{
			this(new JsonObject());
		}

		public void addDouble(String name, double val, double def)
		{
			if (val != def) { json.add(name, new JsonPrimitive(val)); }
		}

		public void addString(String name, @Nullable String val)
		{
			if (val != null) { json.add(name, new JsonPrimitive(val)); }
		}

		public void addObject(String name, @Nullable Writer object)
		{
			if (object != null) { json.add(name, object.json); }
		}
	}

	public record Reader(JsonObject json)
	{
		public double readDouble(String name, double def)
		{
			JsonElement element = json.get(name);
			return element != null ? element.getAsDouble() : def;
		}

		public @Nullable String readString(String name)
		{
			JsonElement element = json.get(name);
			return element != null ? element.getAsString() : null;
		}

		public @Nullable Reader readObject(String name)
		{
			JsonElement element = json.get(name);
			return element != null ? new Reader(element.getAsJsonObject()) : null;
		}
	}
}
