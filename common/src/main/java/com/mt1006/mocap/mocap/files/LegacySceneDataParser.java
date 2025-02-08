package com.mt1006.mocap.mocap.files;

import com.mt1006.mocap.command.io.CommandOutput;
import com.mt1006.mocap.mocap.playing.modifiers.*;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.util.Scanner;

public class LegacySceneDataParser
{
	private static final String NULL_STR = "[null]";
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
		catch (Exception e) { commandOutput.sendException(e, "error.failed_to_load_scene"); }
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
		SceneData.Subscene subscene = new SceneData.Subscene(scanner.next(), PlaybackModifiers.empty());
		try
		{
			subscene.modifiers.startDelay = StartDelay.fromSeconds(Double.parseDouble(scanner.next()));
			subscene.modifiers.offset = new Offset(Double.parseDouble(scanner.next()),
					Double.parseDouble(scanner.next()), Double.parseDouble(scanner.next()));
			subscene.modifiers.playerName = parsePlayerName(scanner);
			subscene.modifiers.playerSkin = parsePlayerSkin(scanner);

			String playerAsEntityStr = scanner.next();
			if (playerAsEntityStr.equals(NULL_STR)) { playerAsEntityStr = null; }
			subscene.modifiers.playerAsEntity = new PlayerAsEntity(playerAsEntityStr, null);
		}
		catch (Exception ignore) {}
		return subscene;
	}

	private static @Nullable String parsePlayerName(Scanner scanner)
	{
		try
		{
			String nameStr = scanner.next();
			return (nameStr.length() <= 16 && !nameStr.equals(NULL_STR)) ? nameStr : null;
		}
		catch (Exception e) { return null; }
	}

	private static PlayerSkin parsePlayerSkin(Scanner scanner)
	{
		PlayerSkin.SkinSource skinSource = PlayerSkin.SkinSource.DEFAULT;
		String skinPath = NULL_STR;

		try
		{
			skinPath = scanner.next();

			// Pre-1.3 compatibility
			if (!skinPath.equals(NULL_STR)) { skinSource = PlayerSkin.SkinSource.FROM_MINESKIN; }

			skinSource = PlayerSkin.SkinSource.fromID(Integer.parseInt(scanner.next()));
		}
		catch (Exception ignore) {}

		return new PlayerSkin(skinSource, skinPath.equals(NULL_STR) ? null : skinPath);
	}
}
