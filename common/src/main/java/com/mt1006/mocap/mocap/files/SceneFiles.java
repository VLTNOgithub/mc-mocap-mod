package com.mt1006.mocap.mocap.files;

import com.mt1006.mocap.MocapMod;
import com.mt1006.mocap.command.InputArgument;
import com.mt1006.mocap.command.io.CommandInfo;
import com.mt1006.mocap.command.io.CommandOutput;
import com.mt1006.mocap.mocap.playing.modifiers.PlayerAsEntity;
import com.mt1006.mocap.utils.Utils;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
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
		boolean success = sceneData.save(commandOutput, file, "scenes.add.success", "scenes.add.error");
		if (success) { InputArgument.addServerInput(nameWithDot(name)); }
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

		InputArgument.addServerInput(nameWithDot(destName));
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

		InputArgument.removeServerInput(nameWithDot(oldName));
		InputArgument.addServerInput(nameWithDot(newName));
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

		InputArgument.removeServerInput(nameWithDot(name));
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
		return sceneData.save(commandOutput, file, "scenes.add_to.success", "scenes.add_to.error");
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
		return sceneData.save(commandOutput, file, "scenes.remove_from.success", "scenes.remove_from.error");
	}

	public static boolean modify(CommandInfo commandInfo, String name, int pos)
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

		SceneData.Subscene newSubscene = modifySubscene(commandInfo, sceneData.subscenes.get(pos - 1));
		if (newSubscene == null)
		{
			commandInfo.sendFailure("scenes.modify.error");
			return false;
		}

		sceneData.subscenes.set(pos - 1, newSubscene);
		return sceneData.save(commandInfo, file, "scenes.modify.success", "scenes.modify.error");
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

				case "player_name":
					subscene.playerName = commandInfo.getPlayerName();
					return subscene;

				case "player_skin":
					subscene.playerSkin = commandInfo.getPlayerSkin();
					return subscene;

				case "player_as_entity":
					String playerAsEntityStr = commandInfo.getNode(6);
					if (playerAsEntityStr == null) { break; }

					if (playerAsEntityStr.equals("enabled"))
					{
						String playerAsEntityId = ResourceArgument.getEntityType(commandInfo.ctx, "entity").key().location().toString();

						Tag tag;
						try { tag = NbtTagArgument.getNbtTag(commandInfo.ctx, "nbt"); }
						catch (Exception e) { tag = null; }
						CompoundTag nbt = (tag instanceof CompoundTag) ? (CompoundTag)tag : null;

						subscene.playerAsEntity = new PlayerAsEntity(playerAsEntityId, nbt != null ? nbt.toString() : null);
						return subscene;
					}
					else if (playerAsEntityStr.equals("disabled"))
					{
						subscene.playerAsEntity = PlayerAsEntity.DISABLED;
						return subscene;
					}
					break;
			}

			rootCommandInfo.sendFailure("error.unable_to_get_argument");
			return null;
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

		if (subscene.playerName == null) { commandOutput.sendSuccess("scenes.element_info.player_name.default"); }
		else { commandOutput.sendSuccess("scenes.element_info.player_name.custom", subscene.playerName); }

		switch (subscene.playerSkin.skinSource)
		{
			case DEFAULT:
				commandOutput.sendSuccess("scenes.element_info.skin.default");
				break;

			case FROM_PLAYER:
				commandOutput.sendSuccess("scenes.element_info.skin.profile", subscene.playerSkin.skinPath);
				break;

			case FROM_FILE:
				commandOutput.sendSuccess("scenes.element_info.skin.file", subscene.playerSkin.skinPath);
				break;

			case FROM_MINESKIN:
				commandOutput.sendSuccess("scenes.element_info.skin.mineskin");
				Component urlComponent = Utils.getEventComponent(ClickEvent.Action.OPEN_URL,
						subscene.playerSkin.skinPath, String.format("  (§n%s§r)", subscene.playerSkin.skinPath));
				commandOutput.sendSuccessComponent(urlComponent);
				break;
		}

		commandOutput.sendSuccess("scenes.element_info.start_delay", subscene.startDelay, (int)Math.round(subscene.startDelay * 20.0));
		commandOutput.sendSuccess("scenes.element_info.offset", subscene.offset[0], subscene.offset[1], subscene.offset[2]);

		if (!subscene.playerAsEntity.isEnabled()) { commandOutput.sendSuccess("scenes.element_info.player_as_entity.disabled"); }
		else { commandOutput.sendSuccess("scenes.element_info.player_as_entity.enabled", subscene.playerAsEntity.entityId); }
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
			commandOutput.sendSuccessLiteral("[%d] %s <%.3f> [%.3f; %.3f; %.3f] (%s)", i++, element.name,
					element.startDelay, element.offset[0], element.offset[1], element.offset[2], element.playerName);
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
}
