package net.mt1006.mocap.mocap.recording;

import net.minecraft.world.entity.Entity;
import net.mt1006.mocap.mocap.actions.Action;
import net.mt1006.mocap.mocap.actions.ComparableAction;
import net.mt1006.mocap.mocap.actions.EntityAction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RecordedEntityState
{
	public final List<ComparableAction> actions;

	public RecordedEntityState(Entity entity)
	{
		actions = new ArrayList<>(ComparableAction.REGISTERED.size());
		ComparableAction.REGISTERED.forEach((constructor) -> actions.add(constructor.apply(entity)));
	}

	public boolean differs(@Nullable RecordedEntityState previousActions)
	{
		if (previousActions == null) { return false; }
		for (int i = 0; i < actions.size(); i++)
		{
			if (actions.get(i).differs(previousActions.actions.get(i))) { return true; }
		}
		return false;
	}

	public void saveDifference(List<Action> actionList, @Nullable RecordedEntityState previousActions)
	{
		if (previousActions == null)
		{
			for (ComparableAction action : actions)
			{
				if (action.shouldBeInitialized()) { actionList.add(action); }
			}
			return;
		}

		for (int i = 0; i < actions.size(); i++)
		{
			ComparableAction action = actions.get(i);
			ComparableAction previousAction = previousActions.actions.get(i);

			if (action.differs(previousAction)) { actionList.add(action); }
		}
	}

	public void saveTrackedEntityDifference(List<Action> actionList, int id, @Nullable RecordedEntityState previousActions)
	{
		if (previousActions == null)
		{
			for (ComparableAction action : actions)
			{
				if (action.shouldBeInitialized()) { actionList.add(new EntityAction(id, action)); }
			}
			return;
		}

		for (int i = 0; i < ComparableAction.REGISTERED.size(); i++)
		{
			ComparableAction action = actions.get(i);
			ComparableAction previousAction = previousActions.actions.get(i);

			if (action.differs(previousAction)) { actionList.add(new EntityAction(id, action)); }
		}
	}
}
