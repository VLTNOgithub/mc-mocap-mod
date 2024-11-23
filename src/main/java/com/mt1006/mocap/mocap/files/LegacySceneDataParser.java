package com.mt1006.mocap.mocap.files;

import com.mt1006.mocap.command.io.CommandOutput;
import com.mt1006.mocap.mocap.playing.modifiers.PlayerAsEntity;
import com.mt1006.mocap.mocap.playing.modifiers.PlayerData;
import com.mt1006.mocap.utils.Utils;

import java.io.ByteArrayInputStream;
import java.util.Scanner;

public class LegacySceneDataParser
{
	private boolean legacy = true, parsed = false;

	public LegacySceneDataParser(SceneData sceneData, CommandOutput commandOutput, byte[] scene)
	{
		try (Scanner scanner = new Scanner(new ByteArrayInputStream(scene)))
		{
			int versionNumber;
			try
			{
				versionNumber = Integer.parseInt(scanner.next());
			}
			catch (NumberFormatException e)
			{
				legacy = false;
				return;
			}

			if (!sceneData.setAndVerifyVersion(commandOutput, versionNumber)) { scanner.close(); }
			if (scanner.hasNextLine()) { scanner.nextLine(); }

			while (scanner.hasNextLine())
			{
				sceneData.subscenes.add(parseSubscene(new Scanner(scanner.nextLine())));
			}
			parsed = true;
		}
		catch (Exception e)
		{
			commandOutput.sendException(e, "error.failed_to_load_scene");
		}
	}

	public boolean isLegacy()
	{
		return legacy;
	}

	public boolean wasParsed()
	{
		return parsed;
	}

	private static SceneData.Subscene parseSubscene(Scanner scanner)
	{
		SceneData.Subscene subscene = new SceneData.Subscene(scanner.next());
		try
		{
			subscene.startDelay = Double.parseDouble(scanner.next());
			subscene.offset[0] = Double.parseDouble(scanner.next());
			subscene.offset[1] = Double.parseDouble(scanner.next());
			subscene.offset[2] = Double.parseDouble(scanner.next());
			subscene.playerData = parsePlayerData(scanner);
			subscene.playerAsEntity = new PlayerAsEntity(Utils.toNullableStr(scanner.next()), null);
		}
		catch (Exception ignore) {}
		return subscene;
	}

	private static PlayerData parsePlayerData(Scanner scanner)
	{
		String name = null;
		PlayerData.SkinSource skinSource = PlayerData.SkinSource.DEFAULT;
		String skinPath = Utils.NULL_STR;

		try
		{
			String nameStr = scanner.next();
			name = nameStr.length() <= 16 ? Utils.toNullableStr(nameStr) : null;

			skinPath = scanner.next();

			// Pre-1.3 compatibility
			if (!skinPath.equals(Utils.NULL_STR)) { skinSource = PlayerData.SkinSource.FROM_MINESKIN; }

			skinSource = PlayerData.SkinSource.fromID(Integer.parseInt(scanner.next()));
		}
		catch (Exception ignore) {}

		return new PlayerData(name, skinSource, skinPath);
	}
}
