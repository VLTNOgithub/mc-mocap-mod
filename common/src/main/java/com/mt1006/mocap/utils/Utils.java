package com.mt1006.mocap.utils;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mt1006.mocap.MocapMod;
import com.mt1006.mocap.events.PlayerConnectionEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public class Utils
{
	public static final String NULL_STR = "[null]";

	public static @Nullable String toNullableStr(String str)
	{
		return !str.equals(NULL_STR) ? str : null;
	}

	public static String toNotNullStr(@Nullable String str)
	{
		return str != null ? str : NULL_STR;
	}

	public static void exception(Exception exception, String str)
	{
		MocapMod.LOGGER.error(str);
		exception.printStackTrace();
	}

	public static void sendMessage(@Nullable Player player, String component, Object... args)
	{
		if (player == null) { return; }
		player.sendSystemMessage(getTranslatableComponent(player, component, args));
	}

	public static void sendComponent(@Nullable Player player, Component component)
	{
		if (player == null) { return; }
		player.sendSystemMessage(component);
	}

	public static String stringFromComponent(String component, Object... args)
	{
		return Component.translatable("mocap." + component, args).getString();
	}

	public static MutableComponent getTranslatableComponent(@Nullable Entity entity, String component, Object... args)
	{
		String key = "mocap." + component;
		return supportsTranslatable(entity)
				? Component.translatable(key, args)
				: Component.literal(Component.translatable(key, args).getString());
	}

	public static MutableComponent getEventComponent(ClickEvent.Action event, String eventStr, String visibleStr)
	{
		return getEventComponent(event, eventStr, Component.literal(visibleStr));
	}

	public static MutableComponent getEventComponent(ClickEvent.Action event, String eventStr, MutableComponent component)
	{
		return component.setStyle(Style.EMPTY.withClickEvent(new ClickEvent(event, eventStr)));
	}

	public static CompoundTag nbtFromString(String nbtString) throws CommandSyntaxException
	{
		return new TagParser(new StringReader(nbtString)).readStruct();
	}

	private static boolean supportsTranslatable(@Nullable Entity entity)
	{
		return entity instanceof ServerPlayer && PlayerConnectionEvent.players.contains((ServerPlayer)entity);
	}
}
