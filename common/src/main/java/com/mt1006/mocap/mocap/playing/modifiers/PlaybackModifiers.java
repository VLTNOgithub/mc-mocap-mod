package com.mt1006.mocap.mocap.playing.modifiers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mt1006.mocap.command.io.CommandInfo;
import com.mt1006.mocap.command.io.CommandOutput;
import com.mt1006.mocap.utils.Utils;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class PlaybackModifiers
{
	public @Nullable String playerName;
	public PlayerSkin playerSkin;
	public PlayerAsEntity playerAsEntity;
	public Offset offset;
	public StartDelay startDelay;
	public EntityFilter entityFilter; //TODO: implement

	private PlaybackModifiers(@Nullable String playerName, PlayerSkin playerSkin, PlayerAsEntity playerAsEntity,
							  Offset offset, StartDelay startDelay, EntityFilter entityFilter)
	{
		this.playerName = playerName;
		this.playerSkin = playerSkin;
		this.playerAsEntity = playerAsEntity;
		this.offset = offset;
		this.startDelay = startDelay;
		this.entityFilter = entityFilter;
	}

	public PlaybackModifiers(JsonObject json)
	{
		startDelay = StartDelay.fromSeconds(getJsonDouble(json, "start_delay"));
		offset = new Offset(getJsonDouble(json, "offset_x"), getJsonDouble(json, "offset_y"), getJsonDouble(json, "offset_z"));

		JsonElement playerNameElement = json.get("player_name");
		playerName = playerNameElement != null ? playerNameElement.getAsString() : null;

		JsonElement playerSkinElement = json.get("player_skin");
		playerSkin = new PlayerSkin(playerSkinElement != null ? playerSkinElement.getAsJsonObject() : null);

		JsonElement playerAsEntityElement = json.get("player_as_entity");
		playerAsEntity = new PlayerAsEntity(playerAsEntityElement != null ? playerAsEntityElement.getAsJsonObject() : null);
	}

	public static PlaybackModifiers empty()
	{
		return forRoot(null, PlayerSkin.DEFAULT);
	}

	public static PlaybackModifiers forRoot(@Nullable String playerName, PlayerSkin playerSkin)
	{
		return new PlaybackModifiers(playerName, playerSkin, PlayerAsEntity.DISABLED, Offset.ZERO, StartDelay.ZERO, EntityFilter.FOR_PLAYBACK);
	}

	public PlaybackModifiers mergeWithParent(PlaybackModifiers parent)
	{
		return new PlaybackModifiers(
				playerName != null ? playerName : parent.playerName,
				playerSkin.mergeWithParent(parent.playerSkin),
				playerAsEntity.isEnabled() ? playerAsEntity : parent.playerAsEntity,
				parent.offset.shift(offset),
				startDelay,
				entityFilter);
	}

	public PlaybackModifiers copy()
	{
		return new PlaybackModifiers(playerName, playerSkin, playerAsEntity, offset, startDelay, entityFilter);
	}

	public boolean areDefault(@Nullable String commandPlayerName, @Nullable PlayerSkin commandPlayerSkin)
	{
		return (playerName == null || playerName.equals(commandPlayerName))
				&& (playerSkin.skinSource == PlayerSkin.SkinSource.DEFAULT || playerSkin == commandPlayerSkin)
				&& !playerAsEntity.isEnabled() && offset.isExactlyZero() && startDelay == StartDelay.ZERO;
	}

	public void addToJson(JsonObject json)
	{
		addJsonDouble(json, "start_delay", startDelay.seconds);
		addJsonDouble(json, "offset_x", offset.x);
		addJsonDouble(json, "offset_y", offset.y);
		addJsonDouble(json, "offset_z", offset.z);

		if (playerName != null) { json.add("player_name", new JsonPrimitive(playerName)); }

		JsonObject playerDataJson = playerSkin.toJson();
		if (playerDataJson != null) { json.add("player_skin", playerDataJson); }

		JsonObject playerAsEntityJson = playerAsEntity.toJson();
		if (playerAsEntityJson != null) { json.add("player_as_entity", playerAsEntityJson); }
	}

	public void list(CommandOutput commandOutput)
	{
		if (playerName == null) { commandOutput.sendSuccess("scenes.element_info.player_name.default"); }
		else { commandOutput.sendSuccess("scenes.element_info.player_name.custom", playerName); }

		switch (playerSkin.skinSource)
		{
			case DEFAULT:
				commandOutput.sendSuccess("scenes.element_info.skin.default");
				break;

			case FROM_PLAYER:
				commandOutput.sendSuccess("scenes.element_info.skin.profile", playerSkin.skinPath);
				break;

			case FROM_FILE:
				commandOutput.sendSuccess("scenes.element_info.skin.file", playerSkin.skinPath);
				break;

			case FROM_MINESKIN:
				commandOutput.sendSuccess("scenes.element_info.skin.mineskin");
				Component urlComponent = Utils.getEventComponent(ClickEvent.Action.OPEN_URL,
						playerSkin.skinPath, String.format("  (§n%s§r)", playerSkin.skinPath));
				commandOutput.sendSuccessComponent(urlComponent);
				break;
		}

		commandOutput.sendSuccess("scenes.element_info.start_delay", startDelay.seconds, startDelay.ticks);
		commandOutput.sendSuccess("scenes.element_info.offset", offset.x, offset.y, offset.z);

		if (!playerAsEntity.isEnabled()) { commandOutput.sendSuccess("scenes.element_info.player_as_entity.disabled"); }
		else { commandOutput.sendSuccess("scenes.element_info.player_as_entity.enabled", playerAsEntity.entityId); }
	}

	public boolean modify(CommandInfo commandInfo, String propertyName, int propertyNodePosition) throws CommandSyntaxException
	{
		switch (propertyName)
		{
			case "start_delay":
				startDelay = StartDelay.fromSeconds(commandInfo.getDouble("delay"));
				return true;

			case "position_offset":
				offset = new Offset(commandInfo.getDouble("offset_x"), commandInfo.getDouble("offset_y"), commandInfo.getDouble("offset_z"));
				return true;

			case "player_name":
				playerName = commandInfo.getPlayerName();
				return true;

			case "player_skin":
				playerSkin = commandInfo.getPlayerSkin();
				return true;

			case "player_as_entity":
				String playerAsEntityStr = commandInfo.getNode(propertyNodePosition + 1);
				if (playerAsEntityStr == null) { break; }

				if (playerAsEntityStr.equals("enabled"))
				{
					String playerAsEntityId = ResourceArgument.getEntityType(commandInfo.ctx, "entity").key().location().toString();

					Tag tag;
					try { tag = NbtTagArgument.getNbtTag(commandInfo.ctx, "nbt"); }
					catch (Exception e) { tag = null; }
					CompoundTag nbt = (tag instanceof CompoundTag) ? (CompoundTag)tag : null;

					playerAsEntity = new PlayerAsEntity(playerAsEntityId, nbt != null ? nbt.toString() : null);
					return true;
				}
				else if (playerAsEntityStr.equals("disabled"))
				{
					playerAsEntity = PlayerAsEntity.DISABLED;
					return true;
				}
				break;
		}
		return false;
	}

	public static boolean checkIfProperName(CommandOutput commandOutput, @Nullable String name)
	{
		if (name == null) { return true; }

		if (name.length() > 16)
		{
			commandOutput.sendFailure("failure.improper_player_name");
			commandOutput.sendFailure("failure.improper_player_name.too_long");
			return false;
		}

		if (name.contains(" "))
		{
			commandOutput.sendFailure("failure.improper_player_name");
			commandOutput.sendFailure("failure.improper_player_name.contains_spaces");
			return false;
		}
		return true;
	}

	private static double getJsonDouble(JsonObject json, String name)
	{
		JsonElement element = json.get(name);
		return element != null ? element.getAsDouble() : 0.0;
	}

	private static void addJsonDouble(JsonObject json, String name, double val)
	{
		if (val != 0.0) { json.add(name, new JsonPrimitive(val)); }
	}
}
