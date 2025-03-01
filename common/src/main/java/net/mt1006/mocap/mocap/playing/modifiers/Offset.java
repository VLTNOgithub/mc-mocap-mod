package net.mt1006.mocap.mocap.playing.modifiers;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class Offset extends Vec3
{
	public static final Offset ZERO = new Offset(0.0, 0.0, 0.0);
	public final Vec3i blockOffset;

	public Offset(double x, double y, double z)
	{
		super(x, y, z);
		blockOffset = new Vec3i((int)Math.round(x), (int)Math.round(y), (int)Math.round(z));
	}

	public static Offset fromArray(@Nullable JsonArray array)
	{
		return array != null
				? new Offset(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble())
				: ZERO;
	}

	public JsonArray toArray()
	{
		JsonArray array = new JsonArray();
		array.add(new JsonPrimitive(x));
		array.add(new JsonPrimitive(y));
		array.add(new JsonPrimitive(z));
		return array;
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
