package com.mt1006.mocap.mocap.files;

import com.mt1006.mocap.command.io.CommandInfo;
import com.mt1006.mocap.mocap.playing.modifiers.EntityFilter;
import com.mt1006.mocap.mocap.playing.modifiers.PlayerData;
import com.mt1006.mocap.utils.Utils;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

public class SceneData
{
	public final ArrayList<Subscene> subscenes = new ArrayList<>();
	public int version = 0;
	public boolean experimentalVersion = false;
	public long fileSize = 0;

	public boolean load(CommandInfo commandInfo, String name)
	{
		byte[] data = Files.loadFile(Files.getSceneFile(commandInfo, name));
		return data != null && load(commandInfo, data);
	}

	public boolean load(CommandInfo commandInfo, byte[] scene)
	{
		fileSize = scene.length;

		try (Scanner scanner = new Scanner(new ByteArrayInputStream(scene)))
		{
			int versionNumber = Integer.parseInt(scanner.next());
			version = Math.abs(versionNumber);
			experimentalVersion = (versionNumber < 0);

			if (version > SceneFiles.VERSION)
			{
				commandInfo.sendFailure("error.failed_to_load_scene");
				commandInfo.sendFailure("error.failed_to_load_scene.not_supported");
				scanner.close();
				return false;
			}

			if (scanner.hasNextLine()) { scanner.nextLine(); }

			while (scanner.hasNextLine())
			{
				subscenes.add(new Subscene(new Scanner(scanner.nextLine())));
			}
			return true;
		}
		catch (Exception exception)
		{
			commandInfo.sendException(exception, "error.failed_to_load_scene");
			return false;
		}
	}

	public static class Subscene
	{
		public String name;
		public double startDelay = 0.0;
		public double[] offset = new double[3];
		public PlayerData playerData = PlayerData.EMPTY;
		public @Nullable String playerAsEntityID = null;
		public EntityFilter entityFilter = EntityFilter.FOR_PLAYBACK;

		public Subscene(String name)
		{
			this.name = name;
			offset[0] = 0.0;
			offset[1] = 0.0;
			offset[2] = 0.0;
		}

		public Subscene(Scanner scanner)
		{
			name = scanner.next();
			try
			{
				startDelay = Double.parseDouble(scanner.next());
				offset[0] = Double.parseDouble(scanner.next());
				offset[1] = Double.parseDouble(scanner.next());
				offset[2] = Double.parseDouble(scanner.next());
				playerData = new PlayerData(scanner);
				playerAsEntityID = Utils.toNullableStr(scanner.next());
			}
			catch (Exception ignore) {}
		}

		public Subscene copy()
		{
			return new Subscene(new Scanner(sceneToStr()));
		}

		public String sceneToStr()
		{
			return String.format(Locale.US, "%s %f %f %f %f %s %s", name, startDelay,
					offset[0], offset[1], offset[2], playerData.dataToStr(),
					Utils.toNotNullStr(playerAsEntityID));
		}
	}
}
