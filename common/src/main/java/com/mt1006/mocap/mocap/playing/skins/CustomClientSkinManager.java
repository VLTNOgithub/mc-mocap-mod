package com.mt1006.mocap.mocap.playing.skins;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Pair;
import com.mt1006.mocap.MocapMod;
import com.mt1006.mocap.mocap.files.Files;
import com.mt1006.mocap.network.MocapPacketC2S;
import com.mt1006.mocap.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CustomClientSkinManager
{
	private static final int MAX_CLIENT_CACHE_SIZE = 4096;
	private static final String SKIN_RES_PREFIX = "custom_skin/";
	private static final String SLIM_SKIN_RES_PREFIX = SKIN_RES_PREFIX + Files.SLIM_SKIN_PREFIX;
	private static final ConcurrentMap<String, Boolean> skinCache = new ConcurrentHashMap<>();
	private static boolean clientWarned = false;

	public static @Nullable ResourceLocation get(@Nullable String name)
	{
		if (name == null) { return null; }
		Boolean accessible = skinCache.get(name);

		if (accessible == null)
		{
			loadClientSkin(name);
			return null;
		}
		return accessible ? resFromName(name) : null;
	}

	public static void loadClientSkin(String name)
	{
		if (skinCache.size() > MAX_CLIENT_CACHE_SIZE)
		{
			if (clientWarned) { return; }

			Player player = Minecraft.getInstance().player;
			if (player == null) { return; }

			Utils.sendMessage(player, "warning.custom_skin_cache_limit");
			Utils.sendMessage(player, "warning.custom_skin_cache_limit.tip");
			clientWarned = true;
			return;
		}

		skinCache.put(name, false);
		MocapPacketC2S.sendRequestCustomSkin(name);
	}

	public static void register(Pair<String, byte[]> customSkinData)
	{
		//TODO: test!!!
		String name = customSkinData.getFirst();
		byte[] array = customSkinData.getSecond();

		Boolean accessible = skinCache.get(name);
		if (accessible == null || accessible) { return; }

		try
		{
			NativeImage nativeImage;

			try
			{
				nativeImage = NativeImage.read(array);
			}
			catch (IOException e)
			{
				Utils.exception(e, "Failed to load skin texture into buffer!");
				return;
			}

			if (nativeImage.getWidth() > 4096 || nativeImage.getHeight() > 4096)
			{
				MocapMod.LOGGER.error("Skin texture too big!");
				return;
			}

			Minecraft.getInstance().getTextureManager().register(resFromName(name), new DynamicTexture(nativeImage));
			skinCache.put(name, true);
		}
		catch (Exception e) { Utils.exception(e, "Failed to read skin texture!"); }
	}

	public static void clearCache()
	{
		TextureManager textureManager = Minecraft.getInstance().getTextureManager();
		for (Map.Entry<String, Boolean> entry : skinCache.entrySet())
		{
			Boolean val = entry.getValue();
			if (val != null && val) { textureManager.release(resFromName(entry.getKey())); }
		}
		skinCache.clear();
		clientWarned = false;
	}

	public static boolean isSlimSkin(ResourceLocation res)
	{
		return res.getPath().startsWith(SLIM_SKIN_RES_PREFIX);
	}

	private static ResourceLocation resFromName(String name)
	{
		return ResourceLocation.fromNamespaceAndPath(MocapMod.MOD_ID, SKIN_RES_PREFIX + name);
	}
}
