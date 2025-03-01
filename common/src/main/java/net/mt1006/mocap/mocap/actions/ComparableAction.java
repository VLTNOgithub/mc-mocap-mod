package net.mt1006.mocap.mocap.actions;

import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public interface ComparableAction extends Action
{
	List<Function<Entity, ComparableAction>> REGISTERED = new ArrayList<>();

	boolean differs(ComparableAction previousAction);
	default boolean shouldBeInitialized() { return true; }
}
