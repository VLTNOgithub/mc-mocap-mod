package net.mt1006.mocap.mocap.playing.modifiers;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.mt1006.mocap.command.io.CommandInfo;
import net.mt1006.mocap.command.io.CommandOutput;
import net.mt1006.mocap.mocap.files.SceneFiles;
import net.mt1006.mocap.utils.Utils;
import org.jetbrains.annotations.Nullable;

public class PlaybackModifiers
{
	public @Nullable String playerName;
	public PlayerSkin playerSkin;
	public PlayerAsEntity playerAsEntity;
	public Offset offset;
	public StartDelay startDelay;
	public EntityFilter entityFilter;
	public Scale scale;

	private PlaybackModifiers(@Nullable String playerName, PlayerSkin playerSkin, PlayerAsEntity playerAsEntity,
							  Offset offset, StartDelay startDelay, EntityFilter entityFilter, Scale scale)
	{
		this.playerName = playerName;
		this.playerSkin = playerSkin;
		this.playerAsEntity = playerAsEntity;
		this.offset = offset;
		this.startDelay = startDelay;
		this.entityFilter = entityFilter;
		this.scale = scale;
	}

	public PlaybackModifiers(SceneFiles.Reader reader)
	{
		startDelay = StartDelay.fromSeconds(reader.readDouble("start_delay", 0.0));
		offset = Offset.fromArray(reader.readArray("offset"));
		playerName = reader.readString("player_name");
		playerSkin = new PlayerSkin(reader.readObject("player_skin"));
		playerAsEntity = new PlayerAsEntity(reader.readObject("player_as_entity"));
		entityFilter = EntityFilter.fromString(reader.readString("entity_filter"));
		scale = new Scale(reader.readObject("scale"));
	}

	public static PlaybackModifiers empty()
	{
		return new PlaybackModifiers(null, PlayerSkin.DEFAULT, PlayerAsEntity.DISABLED,
				Offset.ZERO, StartDelay.ZERO, EntityFilter.FOR_PLAYBACK, Scale.NORMAL);
	}

	public PlaybackModifiers mergeWithParent(PlaybackModifiers parent)
	{
		return new PlaybackModifiers(
				playerName != null ? playerName : parent.playerName,
				playerSkin.mergeWithParent(parent.playerSkin),
				playerAsEntity.isEnabled() ? playerAsEntity : parent.playerAsEntity,
				offset.shift(parent.offset),
				//startDelay.add(parent.startDelay), //TODO: fix how delaying start works?
				startDelay,
				!entityFilter.isDefaultForPlayback() ? entityFilter : parent.entityFilter,
				scale.mergeWithParent(parent.scale));
	}

	public PlaybackModifiers copy()
	{
		return new PlaybackModifiers(playerName, playerSkin, playerAsEntity, offset, startDelay, entityFilter, scale);
	}

	public boolean areDefault()
	{
		return playerName == null && playerSkin.skinSource == PlayerSkin.SkinSource.DEFAULT
				&& !playerAsEntity.isEnabled() && offset.isExactlyZero() && startDelay == StartDelay.ZERO
				&& entityFilter.isDefaultForPlayback() && scale.isNormal();
	}

	public void save(SceneFiles.Writer writer)
	{
		writer.addDouble("start_delay", startDelay.seconds, 0.0);
		writer.addArray("offset", offset.toArray());
		writer.addString("player_name", playerName);
		writer.addObject("player_skin", playerSkin.save());
		writer.addObject("player_as_entity", playerAsEntity.save());
		writer.addString("entity_filter", entityFilter.save());
		writer.addObject("scale", scale.save());
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

		if (entityFilter.isDefaultForPlayback()) { commandOutput.sendSuccess("scenes.element_info.entity_filter.disabled"); }
		else { commandOutput.sendSuccess("scenes.element_info.entity_filter.enabled", entityFilter.save()); }

		if (scale.playerScale == 1.0) { commandOutput.sendSuccess("scenes.element_info.scale.player_scale.normal"); }
		else { commandOutput.sendSuccess("scenes.element_info.scale.player_scale.custom", scale.playerScale); }

		if (scale.sceneScale == 1.0) { commandOutput.sendSuccess("scenes.element_info.scale.scene_scale.normal"); }
		else { commandOutput.sendSuccess("scenes.element_info.scale.scene_scale.custom", scale.sceneScale); }
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
				playerName = commandInfo.getString("player_name");
				return true;

			case "player_skin":
				playerSkin = commandInfo.getPlayerSkin();
				return true;

			case "player_as_entity":
				String playerAsEntityMode = commandInfo.getNode(propertyNodePosition + 1);
				if (playerAsEntityMode == null) { break; }

				if (playerAsEntityMode.equals("enabled"))
				{
					String playerAsEntityId = ResourceArgument.getEntityType(commandInfo.ctx, "entity").key().location().toString();

					Tag tag;
					try { tag = NbtTagArgument.getNbtTag(commandInfo.ctx, "nbt"); }
					catch (Exception e) { tag = null; }
					CompoundTag nbt = (tag instanceof CompoundTag) ? (CompoundTag)tag : null;

					playerAsEntity = new PlayerAsEntity(playerAsEntityId, nbt != null ? nbt.toString() : null);
					return true;
				}
				else if (playerAsEntityMode.equals("disabled"))
				{
					playerAsEntity = PlayerAsEntity.DISABLED;
					return true;
				}
				break;

			case "entity_filter":
				String filterMode = commandInfo.getNode(propertyNodePosition + 1);
				if (filterMode == null) { break; }

				if (filterMode.equals("enabled"))
				{
					String filterStr = commandInfo.getString("entity_filter");
					EntityFilterInstance filterInstance = EntityFilterInstance.create(filterStr);
					if (filterInstance == null)
					{
						commandInfo.sendFailure("failure.entity_filter.failed_to_parse");
						return false;
					}

					entityFilter = new EntityFilter(filterInstance);
					return true;
				}
				else if (filterMode.equals("disabled"))
				{
					entityFilter = EntityFilter.FOR_PLAYBACK;
					return true;
				}
				break;

			case "scale":
				String scaleType = commandInfo.getNode(propertyNodePosition + 1);
				if (scaleType == null) { break; }

				double scaleVal = commandInfo.getDouble("scale");
				if (scaleType.equals("of_player")) { scale = scale.ofPlayer(scaleVal); }
				else if (scaleType.equals("of_scene")) { scale = scale.ofScene(scaleVal); }
				else { break; }
				return true;
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
}
