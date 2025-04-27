package net.mt1006.mocap.utils;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.mt1006.mocap.MocapMod;
import net.mt1006.mocap.events.PlayerConnectionEvent;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;

public class Utils
{
	public static void exception(Exception exception, String str)
	{
		MocapMod.LOGGER.error(str);
		exception.printStackTrace();
	}

	public static void sendMessage(@Nullable Player player, String component, Object... args)
	{
		if (player == null) { return; }
		player.displayClientMessage(Component.literal(component), false);
	}

	public static void sendComponent(@Nullable Player player, Component component)
	{
		if (player == null) { return; }
		player.displayClientMessage(Component.literal(component.getString()), false);
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
		ClickEvent clickEvent;
		switch (event) {
			case OPEN_URL:
				try {
					clickEvent = new ClickEvent.OpenUrl(new URI(eventStr));
				} catch (URISyntaxException e) {
					MocapMod.LOGGER.error("Failed to create URI for ClickEvent: {}", eventStr, e);
					return component;
				}
				break;
			case OPEN_FILE:
				clickEvent = new ClickEvent.OpenFile(eventStr);
				break;
			case RUN_COMMAND:
				clickEvent = new ClickEvent.RunCommand(eventStr);
				break;
			case SUGGEST_COMMAND:
				clickEvent = new ClickEvent.SuggestCommand(eventStr);
				break;
			case CHANGE_PAGE:
				try {
					clickEvent = new ClickEvent.ChangePage(Integer.parseInt(eventStr));
				} catch (NumberFormatException e) {
					MocapMod.LOGGER.error("Failed to parse page number for ClickEvent: {}", eventStr, e);
					return component;
				}
				break;
			case COPY_TO_CLIPBOARD:
				clickEvent = new ClickEvent.CopyToClipboard(eventStr);
				break;
			default:
				MocapMod.LOGGER.error("Unsupported ClickEvent.Action type: {}", event);
				return component;
		}
		return component.setStyle(Style.EMPTY.withClickEvent(clickEvent));
	}

	public static CompoundTag nbtFromString(String nbtString) throws CommandSyntaxException
	{
		return TagParser.parseCompoundFully(nbtString);
	}

	private static boolean supportsTranslatable(@Nullable Entity entity)
	{
		return entity instanceof ServerPlayer && PlayerConnectionEvent.players.contains((ServerPlayer)entity);
	}
}
