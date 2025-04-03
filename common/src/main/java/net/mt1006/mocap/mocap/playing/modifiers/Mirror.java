package net.mt1006.mocap.mocap.playing.modifiers;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public enum Mirror
{
	NONE(false, false),
	X(true, false),
	Z(false, true),
	XZ(true, true);

	public final boolean mirrorX, mirrorZ;

	Mirror(boolean mirrorX, boolean mirrorZ)
	{
		this.mirrorX = mirrorX;
		this.mirrorZ = mirrorZ;
	}

	public static Mirror fromString(@Nullable String str)
	{
		try
		{
			return str != null ? Mirror.valueOf(str.toUpperCase()) : NONE;
		}
		catch (IllegalArgumentException e) { return NONE; }
	}

	public static @Nullable Mirror fromStringOrNull(@Nullable String str)
	{
		try
		{
			return str != null ? Mirror.valueOf(str.toUpperCase()) : null;
		}
		catch (IllegalArgumentException e) { return null; }
	}

	public @Nullable String save()
	{
		return this == NONE ? null : name().toLowerCase();
	}

	public Vec3 apply(Vec3 point, Vec3 center)
	{
		if (this != NONE)
		{
			double x = mirrorX ? (-(point.x - center.x) + center.x) : point.x;
			double z = mirrorZ ? (-(point.z - center.z) + center.z) : point.z;
			return new Vec3(x, point.y, z);
		}
		return point;
	}
}
