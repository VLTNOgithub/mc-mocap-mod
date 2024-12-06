package com.mt1006.mocap.mocap.playing.modifiers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mt1006.mocap.command.io.CommandInfo;
import com.mt1006.mocap.command.io.CommandOutput;
import com.mt1006.mocap.mocap.playing.skins.CustomServerSkinManager;
import com.mt1006.mocap.mocap.settings.Settings;
import com.mt1006.mocap.utils.Fields;
import com.mt1006.mocap.utils.ProfileUtils;
import com.mt1006.mocap.utils.Utils;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.HttpsURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

public class PlayerData
{
	public static final PlayerData EMPTY = new PlayerData((String)null);
	private static final String MINESKIN_API_URL = "https://api.mineskin.org/get/uuid/";
	public final @Nullable String name;
	public final SkinSource skinSource;
	public final String skinPath;

	public PlayerData(@Nullable String name)
	{
		this.name = name;
		this.skinSource = SkinSource.DEFAULT;
		this.skinPath = Utils.NULL_STR;
	}

	public PlayerData(@Nullable String name, SkinSource skinSource, @Nullable String skinPath)
	{
		this.name = name;
		this.skinSource = skinSource;
		this.skinPath = Utils.toNotNullStr(skinPath);
	}

	public PlayerData(@Nullable JsonObject json)
	{
		if (json == null)
		{
			this.name = null;
			this.skinSource = SkinSource.DEFAULT;
			this.skinPath = Utils.NULL_STR;
			return;
		}

		JsonElement nameElement = json.get("name");
		this.name = nameElement != null ? nameElement.getAsString() : null;

		JsonElement skinSourceElement = json.get("skin_source");
		this.skinSource = skinSourceElement != null ? SkinSource.fromName(skinSourceElement.getAsString()) : SkinSource.DEFAULT;

		JsonElement skinPathElement = json.get("skin_path");
		this.skinPath = skinPathElement != null ? skinPathElement.getAsString() : Utils.NULL_STR;
	}

	public @Nullable JsonObject toJson()
	{
		if (name == null && skinSource == SkinSource.DEFAULT) { return null; }

		JsonObject json = new JsonObject();
		if (name != null) { json.add("name", new JsonPrimitive(name)); }
		if (skinSource != SkinSource.DEFAULT) { json.add("skin_source", new JsonPrimitive(skinSource.getName())); }
		if (!skinPath.equals(Utils.NULL_STR)) { json.add("skin_path", new JsonPrimitive(skinPath)); }

		return json;
	}

	public void addSkinToPropertyMap(CommandInfo commandInfo, PropertyMap propertyMap)
			throws IllegalArgumentException, IllegalAccessException
	{
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

	public PlayerData mergeWithParent(PlayerData parent)
	{
		return (skinSource != SkinSource.DEFAULT)
				? new PlayerData(name != null ? name : parent.name, skinSource, skinPath)
				: new PlayerData(name != null ? name : parent.name, parent.skinSource, parent.skinPath);
	}

	public boolean checkIfProperName(CommandOutput commandOutput)
	{
		if (name == null) { return true; }

		if (name.length() > 16)
		{
			commandOutput.sendFailure("scenes.add_to.failed");
			commandOutput.sendFailure("scenes.add_to.failed.too_long_name");
			return false;
		}

		if (name.contains(" "))
		{
			commandOutput.sendFailure("scenes.add_to.failed");
			commandOutput.sendFailure("scenes.add_to.failed.contain_spaces");
			return false;
		}
		return true;
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

		public static SkinSource fromName(String name)
		{
			try
			{
				return valueOf(name.toUpperCase());
			}
			catch (IllegalArgumentException e) { return DEFAULT; }
		}
	}
}
