package net.mt1006.mocap.mocap.playing.modifiers;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class Offset extends Vec3
{
	public static final Offset ZERO = new Offset(0.0, 0.0, 0.0);
	public final boolean isZero, isInt;

	public Offset(double x, double y, double z)
	{
		super(x, y, z);
		isZero = (x == 0.0 && y == 0.0 && z == 0.0);
		isInt = (isZero || (x == (int)x && y == (int)y && z == (int)z));
	}

	public static Offset fromVec3(@Nullable Vec3 vec)
	{
		return vec != null ? new Offset(vec.x, vec.y, vec.z) : ZERO;
	}

	public @Nullable Vec3 save()
	{
		return isZero ? null : this;
	}

	public Vec3 apply(Vec3 point)
	{
		return new Vec3(x + point.x, y + point.y, z + point.z);
	}
}
