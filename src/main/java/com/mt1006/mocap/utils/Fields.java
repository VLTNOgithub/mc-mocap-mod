package com.mt1006.mocap.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import com.mt1006.mocap.MocapMod;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Fields
{
	public static Field gameProfileProperties = null;

	public static void init()
	{
		try
		{
			gameProfileProperties = getField(GameProfile.class, PropertyMap.class);
			if (gameProfileProperties == null) { MocapMod.LOGGER.error("\"gameProfileProperties\" is null!"); }
		}
		catch (Exception exception)
		{
			Utils.exception(exception, "Fields.init() thrown exception!");
		}
	}

	private static @Nullable Field getField(Class<?> declaringClass, Class<?> fieldType)
	{
		Field[] fields = declaringClass.getDeclaredFields();

		for (Field field : fields)
		{
			if (Modifier.isStatic(field.getModifiers())) { continue; }
			if (fieldType.isAssignableFrom(field.getType()))
			{
				field.setAccessible(true);
				return field;
			}
		}
		return null;
	}
}
