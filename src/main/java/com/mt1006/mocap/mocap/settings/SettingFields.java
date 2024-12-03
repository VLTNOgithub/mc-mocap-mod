package com.mt1006.mocap.mocap.settings;

import com.mojang.brigadier.arguments.*;
import com.mt1006.mocap.command.io.CommandInfo;
import com.mt1006.mocap.mocap.files.Files;
import com.mt1006.mocap.mocap.playing.modifiers.EntityFilterInstance;
import com.mt1006.mocap.utils.Utils;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Function;

public class SettingFields
{
	final Map<String, Field<?>> fieldMap = new HashMap<>();

	//TODO: remove?
	/*public SettingFields(String filename)
	{
		this.file = new File(Minecraft.getInstance().gameDirectory, "config/" + filename);
	}*/

	public IntegerField add(String name, int val)
	{
		IntegerField field = new IntegerField(name, val);
		addField(field, name);
		return field;
	}

	public BooleanField add(String name, boolean val)
	{
		BooleanField field = new BooleanField(name, val);
		addField(field, name);
		return field;
	}

	public DoubleField add(String name, double val)
	{
		DoubleField field = new DoubleField(name, val);
		addField(field, name);
		return field;
	}

	public StringField add(String name, String val, @Nullable Consumer<String> onSet,
						   @Nullable Function<String, Boolean> testCommandInput)
	{
		StringField field = new StringField(name, val, onSet, testCommandInput);
		addField(field, name);
		return field;
	}

	private void addField(Field<?> field, String name)
	{
		if (fieldMap.put(name, field) != null) { throw new RuntimeException("Duplicate field names!"); };
	}

	public void save()
	{
		try
		{
			File settingsFile = Files.getSettingsFile();
			if (settingsFile == null) { return; }
			PrintWriter printWriter = new PrintWriter(settingsFile);

			for (Field<?> setting : fieldMap.values())
			{
				String str = setting.getString();
				if (str != null) { printWriter.print(str); }
			}

			printWriter.close();
		}
		catch (Exception ignore) {}
	}

	public void load()
	{
		//TODO: improve loading settings?
		try
		{
			File settingsFile = Files.getSettingsFile();
			if (settingsFile == null) { return; }

			Scanner fileScanner = new Scanner(settingsFile);
			while (fileScanner.hasNextLine())
			{
				String line = fileScanner.nextLine();
				if (line.isEmpty()) { continue; }
				String[] parts = line.split("=");

				if (parts.length != 2)
				{
					fileScanner.close();
					return;
				}

				Field<?> field = fieldMap.get(parts[0]);
				if (field == null) { continue; }

				field.fromString(parts[1]);
			}
			fileScanner.close();
		}
		catch (Exception ignore) {}
	}

	public void unload()
	{
		fieldMap.values().forEach(Field::reset);
	}

	public abstract static class Field<T>
	{
		public final String name;
		private final @Nullable Consumer<T> onSet;
		protected final T defVal;
		public T val;

		public Field(String name, T defVal, @Nullable Consumer<T> onSet)
		{
			this.name = name;
			this.defVal = defVal;
			this.val = defVal;
			this.onSet = onSet;

			if (onSet != null) { onSet.accept(val); }
		}

		public void set(T val)
		{
			this.val = val;
			if (onSet != null) { onSet.accept(val); }
		}

		private void reset()
		{
			set(defVal);
		}

		public Component getInfo(CommandInfo commandInfo)
		{
			String key = val.equals(defVal) ? "settings.list.info_def" : "settings.list.info";
			return commandInfo.getTranslatableComponent(key, name, val.toString());
		}

		public void printValues(CommandInfo commandInfo)
		{
			commandInfo.sendSuccess("settings.info.current_value", val.toString());
			commandInfo.sendSuccess("settings.info.default_value", defVal.toString());
		}

		public String getString()
		{
			return name + "=" + val.toString() + "\n";
		}

		public void fromString(String str)
		{
			try
			{
				set(parseFromString(str));
			}
			catch (Exception e)
			{
				Utils.exception(e, "Failed to load settings from string");
				reset();
			}
		}

		public boolean fromCommand(CommandInfo commandInfo)
		{
			try
			{
				T newVal = parseFromCommand(commandInfo);
				if (newVal == null)
				{
					commandInfo.sendFailure("settings.set.invalid_value");
					return false;
				}

				set(newVal);
				return true;
			}
			catch (Exception e)
			{
				commandInfo.sendException(e, "settings.set.error");
				reset();
				return false;
			}
		}

		public abstract T parseFromString(String str);
		public abstract @Nullable T parseFromCommand(CommandInfo commandInfo);
		public abstract ArgumentType<?> getArgumentType();
	}

	public static class IntegerField extends Field<Integer>
	{
		public IntegerField(String name, Integer val)
		{
			super(name, val, null);
		}

		@Override public Integer parseFromString(String str)
		{
			return Integer.valueOf(str);
		}

		@Override public Integer parseFromCommand(CommandInfo commandInfo)
		{
			return commandInfo.getInteger("new_value");
		}

		@Override public ArgumentType<?> getArgumentType()
		{
			return IntegerArgumentType.integer();
		}
	}

	public static class BooleanField extends Field<Boolean>
	{
		public BooleanField(String name, Boolean val)
		{
			super(name, val, null);
		}

		@Override public Boolean parseFromString(String str)
		{
			return Boolean.valueOf(str);
		}

		@Override public Boolean parseFromCommand(CommandInfo commandInfo)
		{
			return commandInfo.getBool("new_value");
		}

		@Override public ArgumentType<?> getArgumentType()
		{
			return BoolArgumentType.bool();
		}
	}

	public static class DoubleField extends Field<Double>
	{
		public DoubleField(String name, Double val)
		{
			super(name, val, null);
		}

		@Override public Double parseFromString(String str)
		{
			return Double.valueOf(str);
		}

		@Override public Double parseFromCommand(CommandInfo commandInfo)
		{
			return commandInfo.getDouble("new_value");
		}

		@Override public ArgumentType<?> getArgumentType()
		{
			return DoubleArgumentType.doubleArg();
		}
	}

	public static class StringField extends Field<String>
	{
		private final @Nullable Function<String, Boolean> testCommandInput;

		public StringField(String name, String val, @Nullable Consumer<String> onSet,
						   @Nullable Function<String, Boolean> testCommandInput)
		{
			super(name, val, onSet);
			this.testCommandInput = testCommandInput;
		}

		@Override public String parseFromString(String str)
		{
			return EntityFilterInstance.test(str) ? str : defVal;
		}

		@Override public @Nullable String parseFromCommand(CommandInfo commandInfo)
		{
			String newValue = commandInfo.getString("new_value");
			return (testCommandInput != null && testCommandInput.apply(newValue)) ? newValue : null;
		}

		@Override public ArgumentType<?> getArgumentType()
		{
			return StringArgumentType.greedyString();
		}

		@Override public void printValues(CommandInfo commandInfo)
		{
			String valStr = val, defValStr = defVal;

			commandInfo.sendSuccess("settings.info.string_value",
					commandInfo.getTranslatableComponent("settings.info.current_value", valStr),
					createButton(commandInfo, valStr));

			commandInfo.sendSuccess("settings.info.string_value",
					commandInfo.getTranslatableComponent("settings.info.default_value", defValStr),
					createButton(commandInfo, defValStr));
		}

		private static Component createButton(CommandInfo commandInfo, String textToCopy)
		{
			ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, textToCopy);
			HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
					commandInfo.getTranslatableComponent("settings.info.copy_button_info"));

			return commandInfo.getTranslatableComponent("settings.info.copy_button")
					.setStyle(Style.EMPTY.withClickEvent(clickEvent).withHoverEvent(hoverEvent)); //TODO: test it on a dedicated server
		}
	}
}
