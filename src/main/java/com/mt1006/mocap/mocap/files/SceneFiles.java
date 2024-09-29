package com.mt1006.mocap.mocap.files;

import com.mt1006.mocap.MocapMod;
import com.mt1006.mocap.command.InputArgument;
import com.mt1006.mocap.command.io.CommandInfo;
import com.mt1006.mocap.command.io.CommandOutput;
import com.mt1006.mocap.utils.Utils;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SceneFiles
{
	public static final int VERSION = MocapMod.SCENE_FORMAT_VERSION;

	public static boolean add(CommandInfo commandInfo, String name)
	{
		File sceneFile = Files.getSceneFile(commandInfo, name);
		if (sceneFile == null) { return false; }

		try
		{
			if (sceneFile.exists())
			{
				commandInfo.sendFailure("scenes.add.already_exists");
				return false;
			}

			PrintWriter printWriter = new PrintWriter(sceneFile);
			printWriter.print(String.format("%d", VERSION));
			printWriter.close();
		}
		catch (IOException exception)
		{
			commandInfo.sendException(exception, "scenes.add.error");
			return false;
		}

		InputArgument.addServerInput(nameWithDot(name));
		commandInfo.sendSuccess("scenes.add.success");
		return true;
	}

	public static boolean copy(CommandInfo commandInfo, String srcName, String destName)
	{
		File srcFile = Files.getSceneFile(commandInfo, srcName);
		if (srcFile == null) { return false; }

		File destFile = Files.getSceneFile(commandInfo, destName);
		if (destFile == null) { return false; }

		try { FileUtils.copyFile(srcFile, destFile); }
		catch (IOException exception)
		{
			commandInfo.sendException(exception, "scenes.copy.failed");
			return false;
		}

		InputArgument.addServerInput(nameWithDot(destName));
		commandInfo.sendSuccess("scenes.copy.success");
		return true;
	}

	public static boolean rename(CommandInfo commandInfo, String oldName, String newName)
	{
		File oldFile = Files.getSceneFile(commandInfo, oldName);
		if (oldFile == null) { return false; }

		File newFile = Files.getSceneFile(commandInfo, newName);
		if (newFile == null) { return false; }

		if (!oldFile.renameTo(newFile))
		{
			commandInfo.sendFailure("scenes.rename.failed");
			return false;
		}

		InputArgument.removeServerInput(nameWithDot(oldName));
		InputArgument.addServerInput(nameWithDot(newName));
		commandInfo.sendSuccess("scenes.rename.success");
		return true;
	}

	public static boolean remove(CommandInfo commandInfo, String name)
	{
		File sceneFile = Files.getSceneFile(commandInfo, name);
		if (sceneFile == null) { return false; }

		if (!sceneFile.delete())
		{
			commandInfo.sendFailure("scenes.remove.failed");
			return false;
		}

		InputArgument.removeServerInput(nameWithDot(name));
		commandInfo.sendSuccess("scenes.remove.success");
		return true;
	}

	//TODO: move saving to SceneData?
	public static boolean addElement(CommandInfo commandInfo, String name, String lineToAdd)
	{
		File sceneFile = Files.getSceneFile(commandInfo, name);
		if (sceneFile == null) { return false; }

		try
		{
			if (!sceneFile.exists())
			{
				commandInfo.sendFailure("scenes.add_to.failed");
				commandInfo.sendFailure("scenes.error.file_not_exists"); //TODO: make it a failure
				return false;
			}

			SceneData sceneData = new SceneData();
			if (!sceneData.load(commandInfo, name)) { return false; }

			PrintWriter printWriter = new PrintWriter(sceneFile);
			printWriter.print(String.format("%d", MocapMod.EXPERIMENTAL ? (-VERSION) : VERSION));
			for (SceneData.Subscene subscene : sceneData.subscenes)
			{
				printWriter.print("\n" + subscene.sceneToStr());
			}
			printWriter.print("\n" + lineToAdd);
			printWriter.close();
		}
		catch (IOException exception)
		{
			commandInfo.sendException(exception, "scenes.add_to.error");
			return false;
		}

		commandInfo.sendSuccess("scenes.add_to.success");
		return true;
	}

	public static boolean removeElement(CommandInfo commandInfo, String name, int pos)
	{
		File sceneFile = Files.getSceneFile(commandInfo, name);
		if (sceneFile == null) { return false; }

		try
		{
			if (!sceneFile.exists())
			{
				//TODO: make it failure, not error
				commandInfo.sendFailure("scenes.remove_from.error");
				commandInfo.sendFailure("scenes.error.file_not_exists");
				return false;
			}

			SceneData sceneData = new SceneData();
			if (!sceneData.load(commandInfo, name)) { return false; }

			if (sceneData.subscenes.size() < pos || pos < 1)
			{
				commandInfo.sendFailure("scenes.remove_from.error");
				commandInfo.sendFailureWithTip("scenes.error.wrong_element_pos");
				return false;
			}

			int i = 1;
			PrintWriter printWriter = new PrintWriter(sceneFile);
			printWriter.print(String.format("%d", VERSION));
			for (SceneData.Subscene subscene : sceneData.subscenes)
			{
				if (i++ != pos) { printWriter.print("\n" + subscene.sceneToStr()); }
			}
			printWriter.close();
		}
		catch (IOException exception)
		{
			commandInfo.sendException(exception, "scenes.remove_from.error");
			return false;
		}

		commandInfo.sendSuccess("scenes.remove_from.success");
		return true;
	}

	public static boolean modify(CommandInfo commandInfo, String name, int pos)
	{
		File sceneFile = Files.getSceneFile(commandInfo, name);
		if (sceneFile == null) { return false; }

		SceneData.Subscene newSubscene = null;
		try
		{
			if (!sceneFile.exists())
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

			int i = 1;
			PrintWriter printWriter = new PrintWriter(sceneFile);
			printWriter.print(String.format("%d", VERSION));
			for (SceneData.Subscene subscene : sceneData.subscenes)
			{
				if (i++ == pos)
				{
					newSubscene = modifySubscene(commandInfo, subscene);
					if (newSubscene != null) { subscene = newSubscene; }
				}
				printWriter.print("\n" + subscene.sceneToStr());
			}
			printWriter.close();
		}
		catch (IOException exception)
		{
			commandInfo.sendException(exception, "scenes.modify.error");
			return false;
		}

		if (newSubscene != null) { commandInfo.sendSuccess("scenes.modify.success"); }
		else { commandInfo.sendFailure("scenes.modify.error"); }
		return newSubscene != null;
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
			switch (propertyName)
			{
				case "subscene_name":
					subscene.name = commandInfo.getString("new_name");
					return subscene;

				case "start_delay":
					subscene.startDelay = commandInfo.getDouble("delay");
					return subscene;

				case "position_offset":
					subscene.offset[0] = commandInfo.getDouble("offset_x");
					subscene.offset[1] = commandInfo.getDouble("offset_y");
					subscene.offset[2] = commandInfo.getDouble("offset_z");
					return subscene;

				case "player_info":
					subscene.playerData = commandInfo.getPlayerData();
					return subscene;

				case "player_as_entity":
					String playerAsEntityStr = commandInfo.getNode(6);
					if (playerAsEntityStr == null) { break; }

					if (playerAsEntityStr.equals("enabled"))
					{
						subscene.playerAsEntityID = ResourceArgument.getEntityType(commandInfo.ctx, "entity").key().location().toString();
						return subscene;
					}
					else if (playerAsEntityStr.equals("disabled"))
					{
						subscene.playerAsEntityID = null;
						return subscene;
					}
					break;
			}

			rootCommandInfo.sendFailure("error.unable_to_get_argument");
			return null;
		}
		catch (Exception exception)
		{
			rootCommandInfo.sendException(exception, "error.unable_to_get_argument");
			return null;
		}
	}

	public static boolean elementInfo(CommandInfo commandInfo, String name, int pos)
	{
		File sceneFile = Files.getSceneFile(commandInfo, name);
		if (sceneFile == null) { return false; }

		if (!sceneFile.exists())
		{
			commandInfo.sendFailure("scenes.element_info.failed");
			commandInfo.sendFailure("scenes.error.file_not_exists");
			return false;
		}

		SceneData sceneData = new SceneData();
		if (!sceneData.load(commandInfo, name)) { return false; }

		if (sceneData.subscenes.size() < pos || pos < 1)
		{
			commandInfo.sendFailure("scenes.element_info.failed");
			commandInfo.sendFailureWithTip("scenes.error.wrong_element_pos");
			return false;
		}

		SceneData.Subscene subscene = sceneData.subscenes.get(pos - 1);

		commandInfo.sendSuccess("scenes.element_info.info");
		commandInfo.sendSuccess("scenes.element_info.id", name, pos);
		commandInfo.sendSuccess("scenes.element_info.name", subscene.name);

		if (subscene.playerData.name == null) { commandInfo.sendSuccess("scenes.element_info.player_name.default"); }
		else { commandInfo.sendSuccess("scenes.element_info.player_name.custom", subscene.playerData.name); }

		switch (subscene.playerData.skinSource)
		{
			case DEFAULT:
				commandInfo.sendSuccess("scenes.element_info.skin.default");
				break;

			case FROM_PLAYER:
				commandInfo.sendSuccess("scenes.element_info.skin.profile", subscene.playerData.skinPath);
				break;

			case FROM_FILE:
				commandInfo.sendSuccess("scenes.element_info.skin.file", subscene.playerData.skinPath);
				break;

			case FROM_MINESKIN:
				commandInfo.sendSuccess("scenes.element_info.skin.mineskin");
				Component urlComponent = Utils.getEventComponent(ClickEvent.Action.OPEN_URL,
						subscene.playerData.skinPath, String.format("  (§n%s§r)", subscene.playerData.skinPath));
				commandInfo.sendSuccessComponent(urlComponent);
				break;
		}

		commandInfo.sendSuccess("scenes.element_info.start_delay", subscene.startDelay, (int)Math.round(subscene.startDelay * 20.0));
		commandInfo.sendSuccess("scenes.element_info.offset", subscene.offset[0], subscene.offset[1], subscene.offset[2]);

		if (subscene.playerAsEntityID == null) { commandInfo.sendSuccess("scenes.element_info.player_as_entity.disabled"); }
		else { commandInfo.sendSuccess("scenes.element_info.player_as_entity.enabled", subscene.playerAsEntityID); }
		return true;
	}

	public static boolean listElements(CommandInfo commandInfo, String name)
	{
		SceneData sceneData = new SceneData();
		if (!sceneData.load(commandInfo, name)) { return false; }

		commandInfo.sendSuccess("scenes.list_elements");

		int i = 1;
		for (SceneData.Subscene element : sceneData.subscenes)
		{
			commandInfo.sendSuccessLiteral("[%d] %s <%.3f> [%.3f; %.3f; %.3f] (%s)", i++, element.name,
					element.startDelay, element.offset[0], element.offset[1], element.offset[2], element.playerData.name);
		}

		commandInfo.sendSuccessLiteral("[id] name <start_delay> [x; y; z] (player_name)");
		return true;
	}

	public static boolean info(CommandInfo commandInfo, String name)
	{
		SceneData sceneData = new SceneData();

		if (!sceneData.load(commandInfo, name) && sceneData.version <= VERSION)
		{
			commandInfo.sendFailure("scenes.info.failed");
			return false;
		}

		commandInfo.sendSuccess("scenes.info.info");
		commandInfo.sendSuccess("file.info.name", name);
		if (!Files.printVersionInfo(commandInfo, VERSION, sceneData.version, sceneData.experimentalVersion)) { return true; }

		commandInfo.sendSuccess("scenes.info.size", String.format("%.2f", sceneData.fileSize / 1024.0), sceneData.subscenes.size());
		return true;
	}

	public static @Nullable List<String> list(CommandOutput commandOutput)
	{
		if (!Files.initDirectories(commandOutput)) { return null; }
		ArrayList<String> scenes = new ArrayList<>();

		String[] fileList = Files.sceneDirectory.list();
		if (fileList == null) { return null; }

		for (String filename : fileList)
		{
			if (Files.isSceneFile(filename))
			{
				scenes.add("." + filename.substring(0, filename.lastIndexOf('.')));
			}
		}

		Collections.sort(scenes);
		return scenes;
	}

	private static String nameWithDot(String name)
	{
		return name.charAt(0) == '.' ? name : ("." + name);
	}
}
