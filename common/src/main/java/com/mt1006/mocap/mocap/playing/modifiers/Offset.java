package com.mt1006.mocap.mocap.playing.modifiers;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

public class Offset extends Vec3
{
	public static final Offset ZERO = new Offset(0.0, 0.0, 0.0);
	public final Vec3i blockOffset;

	public Offset(double x, double y, double z)
	{
		super(x, y, z);
		blockOffset = new Vec3i((int)Math.round(x), (int)Math.round(y), (int)Math.round(z));
	}

	public Offset shift(Offset val)
	{
		return new Offset(x + val.x, y + val.y, z + val.z);
	}

	public boolean isExactlyZero()
	{
		return x == 0.0 && y == 0.0 && z == 0.0;
	}
}
