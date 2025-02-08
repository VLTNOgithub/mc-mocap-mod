package com.mt1006.mocap.mocap.playing.modifiers;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mt1006.mocap.command.io.CommandInfo;
import com.mt1006.mocap.mocap.files.SceneFiles;
import com.mt1006.mocap.mocap.playing.skins.CustomServerSkinManager;
import com.mt1006.mocap.mocap.settings.Settings;
import com.mt1006.mocap.utils.Fields;
import com.mt1006.mocap.utils.ProfileUtils;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.HttpsURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

public class PlayerSkin
{
	public static final PlayerSkin DEFAULT = new PlayerSkin();
	private static final String MINESKIN_API_URL = "https://api.mineskin.org/get/uuid/";
	public final SkinSource skinSource;
	public final @Nullable String skinPath;

	private PlayerSkin()
	{
		this.skinSource = SkinSource.DEFAULT;
		this.skinPath = null;
	}

	public PlayerSkin(SkinSource skinSource, @Nullable String skinPath)
	{
		this.skinSource = skinSource;
		this.skinPath = skinPath;
	}

	public PlayerSkin(@Nullable SceneFiles.Reader reader)
	{
		if (reader == null)
		{
			skinSource = SkinSource.DEFAULT;
			skinPath = null;
			return;
		}

		skinSource = SkinSource.fromName(reader.readString("skin_source"));
		skinPath = reader.readString("skin_path");
	}

	public @Nullable SceneFiles.Writer save()
	{
		if (skinSource == SkinSource.DEFAULT) { return null; }

		SceneFiles.Writer writer = new SceneFiles.Writer();
		writer.addString("skin_source", skinSource.getName());
		writer.addString("skin_path", skinPath);

		return writer;
	}

	public void addSkinToPropertyMap(CommandInfo commandInfo, PropertyMap propertyMap)
			throws IllegalArgumentException, IllegalAccessException
	{
		if (skinPath == null) { return; }

		switch (skinSource)
		{
			case FROM_PLAYER:
				GameProfile tempProfile = ProfileUtils.getGameProfile(commandInfo.server, skinPath);
				PropertyMap tempPropertyMap = (PropertyMap)Fields.gameProfileProperties.get(tempProfile);

				if (!tempPropertyMap.containsKey("textures"))
				{
					commandInfo.sendFailure("playback.start.warning.skin.profile");
					break;
				}

				if (propertyMap.containsKey("textures")) { propertyMap.get("textures").clear(); }
				propertyMap.putAll("textures", tempPropertyMap.get("textures"));
				break;

			case FROM_FILE:
				propertyMap.put(CustomServerSkinManager.PROPERTY_ID, new Property(CustomServerSkinManager.PROPERTY_ID, skinPath));
				break;

			case FROM_MINESKIN:
				if (!Settings.ALLOW_MINESKIN_REQUESTS.val) { return; }
				Property skinProperty = propertyFromMineskinURL(skinPath);

				if (skinProperty == null)
				{
					commandInfo.sendFailure("playback.start.warning.skin.mineskin");
					break;
				}

				if (propertyMap.containsKey("textures")) { propertyMap.get("textures").clear(); }
				propertyMap.put("textures", skinProperty);
				break;
		}
	}

	public PlayerSkin mergeWithParent(PlayerSkin parent)
	{
		return (skinSource != SkinSource.DEFAULT)
				? new PlayerSkin(skinSource, skinPath)
				: new PlayerSkin(parent.skinSource, parent.skinPath);
	}

	private @Nullable Property propertyFromMineskinURL(String mineskinURL)
	{
		String mineskinID = mineskinURL.contains("/") ? mineskinURL.substring(mineskinURL.lastIndexOf('/') + 1) : mineskinURL;
		String mineskinApiURL = MINESKIN_API_URL + mineskinID;

		try
		{
			URL url = new URI(mineskinApiURL).toURL();

			URLConnection connection = url.openConnection();
			if (!(connection instanceof HttpsURLConnection)) { return null; }
			HttpsURLConnection httpsConnection = (HttpsURLConnection)connection;

			httpsConnection.setUseCaches(false);
			httpsConnection.setRequestMethod("GET");

			Scanner scanner = new Scanner(httpsConnection.getInputStream());
			String text = scanner.useDelimiter("\\A").next();

			scanner.close();
			httpsConnection.disconnect();

			String value = text.split("\"value\":\"")[1].split("\"")[0];
			String signature = text.split("\"signature\":\"")[1].split("\"")[0];

			return new Property("textures", value, signature);
		}
		catch (Exception e) { return null; }
	}

	public enum SkinSource
	{
		DEFAULT(0),
		FROM_PLAYER(1),
		FROM_FILE(2),
		FROM_MINESKIN(3);

		private static final SkinSource[] VALUES = values();
		public final int id;

		SkinSource(int id)
		{
			this.id = id;
		}

		public static SkinSource fromID(int id)
		{
			for (SkinSource s : VALUES)
			{
				if (s.id == id) { return s; }
			}
			return DEFAULT;
		}

		public String getName()
		{
			return name().toLowerCase();
		}

		public static SkinSource fromName(@Nullable String name)
		{
			if (name == null) { return DEFAULT; }

			try
			{
				return valueOf(name.toUpperCase());
			}
			catch (IllegalArgumentException e) { return DEFAULT; }
		}
	}
}
