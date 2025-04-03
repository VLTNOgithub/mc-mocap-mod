package net.mt1006.mocap.mocap.settings;

import net.mt1006.mocap.command.io.CommandInfo;
import net.mt1006.mocap.mocap.playing.modifiers.EntityFilter;
import net.mt1006.mocap.mocap.playing.modifiers.EntityFilterInstance;
import net.mt1006.mocap.mocap.settings.enums.EntitiesAfterPlayback;
import net.mt1006.mocap.mocap.settings.enums.OnDeath;

import java.util.Collection;

public class Settings
{
	private static final SettingFields fields = new SettingFields();
	private static final SettingGroups groups = new SettingGroups();

	private static final SettingGroups.Group RECORDING = groups.add("recording");
	private static final SettingGroups.Group PLAYBACK = groups.add("playback");
	private static final SettingGroups.Group ADVANCED = groups.add("advanced");

	public static final SettingFields.StringField TRACK_ENTITIES = RECORDING.add(fields.add("track_entities", "@vehicles;@projectiles;@items", EntityFilter::onTrackEntitiesSet, EntityFilterInstance::test));
	public static final SettingFields.BooleanField PREVENT_TRACKING_PLAYED_ENTITIES = RECORDING.add(fields.add("prevent_tracking_played_entities", true));
	public static final SettingFields.DoubleField ENTITY_TRACKING_DISTANCE = RECORDING.add(fields.add("entity_tracking_distance", 128.0));
	//public static final SettingFields.BooleanField ASSIGN_DIMENSIONS = RECORDING.add(fields.add("assign_dimensions", true));
	public static final SettingFields.EnumField<OnDeath> ON_DEATH = RECORDING.add(fields.add("on_death", OnDeath.END_RECORDING));
	//public static final SettingFields.IntegerField ON_CHANGE_DIMENSION = RECORDING.add(fields.add("on_change_dimension", 2)); //TODO: implement
	public static final SettingFields.BooleanField START_INSTANTLY = RECORDING.add(fields.add("start_instantly", false));
	public static final SettingFields.BooleanField ASSIGN_PLAYER_NAME = RECORDING.add(fields.add("assign_player_name", false));
	public static final SettingFields.BooleanField CHAT_RECORDING = RECORDING.add(fields.add("chat_recording", false));

	public static final SettingFields.DoubleField PLAYBACK_SPEED = PLAYBACK.add(fields.add("playback_speed", 1.0));
	public static final SettingFields.StringField PLAY_ENTITIES = PLAYBACK.add(fields.add("play_entities", "*", EntityFilter::onPlaybackEntitiesSet, EntityFilterInstance::test));
	public static final SettingFields.BooleanField CAN_PUSH_ENTITIES = PLAYBACK.add(fields.add("can_push_entities", true));
	public static final SettingFields.EnumField<EntitiesAfterPlayback> ENTITIES_AFTER_PLAYBACK = PLAYBACK.add(fields.add("entities_after_playback", EntitiesAfterPlayback.REMOVE));
	public static final SettingFields.BooleanField BLOCK_ACTIONS_PLAYBACK = PLAYBACK.add(fields.add("block_actions_playback", true));
	public static final SettingFields.BooleanField BLOCK_INITIALIZATION = PLAYBACK.add(fields.add("block_initialization", true));
	public static final SettingFields.BooleanField BLOCK_ALLOW_SCALED = PLAYBACK.add(fields.add("block_allow_scaled", false));
	public static final SettingFields.BooleanField DROP_FROM_BLOCKS = PLAYBACK.add(fields.add("drop_from_blocks", false));
	public static final SettingFields.BooleanField START_AS_RECORDED = PLAYBACK.add(fields.add("start_as_recorded", false));
	public static final SettingFields.BooleanField CHAT_PLAYBACK = PLAYBACK.add(fields.add("chat_playback", true));
	public static final SettingFields.BooleanField INVULNERABLE_PLAYBACK = PLAYBACK.add(fields.add("invulnerable_playback", true));

	public static final SettingFields.DoubleField FLUENT_MOVEMENTS = ADVANCED.add(fields.add("fluent_movements", 32.0));
	//public static final SettingFields.DoubleField MOVEMENT_PRECISION = ADVANCED.add(fields.add("movement_precision", 4096.0));
	//public static final SettingFields.BooleanField PRECISE_ROTATION_RECORDING = ADVANCED.add(fields.add("precise_rotation_recording", true));
	//public static final SettingFields.BooleanField PRECISE_ROTATION_PLAYBACK = ADVANCED.add(fields.add("precise_rotation_playback", false)); //TODO: restore?
	public static final SettingFields.BooleanField ALLOW_MINESKIN_REQUESTS = ADVANCED.add(fields.add("allow_mineskin_requests", true));
	public static final SettingFields.BooleanField PREVENT_SAVING_ENTITIES = ADVANCED.add(fields.add("prevent_saving_entities", true));
	public static final SettingFields.BooleanField USE_CREATIVE_GAME_MODE = ADVANCED.add(fields.add("use_creative_game_mode", false)); //TODO: set to true by default?
	public static final SettingFields.BooleanField ALLOW_GHOSTS = ADVANCED.add(fields.add("allow_ghosts", true));
	public static final SettingFields.BooleanField EXPERIMENTAL_RELEASE_WARNING = ADVANCED.add(fields.add("experimental_release_warning", true));
	//public static final SettingFields.IntegerField SERVER_PERFORMANCE_WARNING = ADVANCED.add(fields.add("server_performance_warning", 45)); //TODO: implement
	public static final SettingFields.BooleanField PRETTY_SCENE_FILES = ADVANCED.add(fields.add("pretty_scene_files", true));
	public static final SettingFields.BooleanField SHOW_TIPS = ADVANCED.add(fields.add("show_tips", true));

	public static void save()
	{
		fields.save();
	}

	public static void load()
	{
		fields.load();
	}

	public static void unload()
	{
		fields.unload();
	}

	public static Collection<SettingFields.Field<?>> getFields()
	{
		return fields.fieldMap.values();
	}

	public static Collection<SettingGroups.Group> getGroups()
	{
		return groups.groupMap.values();
	}

	public static boolean list(CommandInfo commandInfo)
	{
		commandInfo.sendSuccess("settings.list");
		for (SettingFields.Field<?> field : getFields())
		{
			commandInfo.sendSuccessComponent(field.getInfo(commandInfo));
		}
		return true;
	}

	public static boolean info(CommandInfo commandInfo)
	{
		String settingName;
		try
		{
			settingName = commandInfo.ctx.getNodes().get(commandInfo.ctx.getNodes().size() - 1).getNode().getName();
		}
		catch (Exception e)
		{
			commandInfo.sendException(e, "error.unable_to_get_argument");
			return false;
		}

		SettingFields.Field<?> field = fields.fieldMap.get(settingName);
		if (field == null)
		{
			commandInfo.sendFailure("settings.error");
			return false;
		}

		commandInfo.sendSuccess("settings.info.name", settingName);
		commandInfo.sendSuccess("settings.info.about", commandInfo.getTranslatableComponent("settings.info.about." + settingName));
		field.printValues(commandInfo);
		return true;
	}

	public static boolean set(CommandInfo commandInfo)
	{
		String settingName = commandInfo.getNode(-2);
		if (settingName == null)
		{
			commandInfo.sendFailure("error.unable_to_get_argument");
			return false;
		}

		SettingFields.Field<?> field = fields.fieldMap.get(settingName);
		if (field == null)
		{
			commandInfo.sendFailure("settings.error");
			return false;
		}

		String oldValue = field.valToString();
		if (!field.fromCommand(commandInfo)) { return false; }

		String newValue = field.valToString();
		oldValue = oldValue.isEmpty() ? "[empty]" : oldValue;
		newValue = newValue.isEmpty() ? "[empty]" : newValue;

		if (oldValue.equals(newValue)) { commandInfo.sendSuccess("settings.set.success.not_changed", newValue); }
		else { commandInfo.sendSuccess("settings.set.success.changed", oldValue, newValue); }
		save();
		return true;
	}
}
