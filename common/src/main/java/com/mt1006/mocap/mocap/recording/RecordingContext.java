package com.mt1006.mocap.mocap.recording;

import com.mt1006.mocap.command.io.CommandOutput;
import com.mt1006.mocap.mocap.actions.Action;
import com.mt1006.mocap.mocap.actions.BlockAction;
import com.mt1006.mocap.mocap.actions.NextTick;
import com.mt1006.mocap.mocap.actions.SkipTicks;
import com.mt1006.mocap.mocap.files.RecordingData;
import com.mt1006.mocap.mocap.files.RecordingFiles;
import com.mt1006.mocap.mocap.playing.modifiers.EntityFilter;
import com.mt1006.mocap.mocap.settings.Settings;
import com.mt1006.mocap.utils.Utils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class RecordingContext
{
	public final RecordingId id;
	public final ServerPlayer recordedPlayer;
	public final @Nullable ServerPlayer sourcePlayer;
	public final RecordingData data = RecordingData.forWriting();
	public State state = State.WAITING_FOR_ACTION;
	private @Nullable RecordedEntityState entityState = null;
	private final PositionTracker positionTracker;
	private final EntityTracker entityTracker = new EntityTracker(this);
	public final EntityFilter entityFilter;
	private int tick = 0;

	public RecordingContext(RecordingId id, ServerPlayer recordedPlayer, @Nullable ServerPlayer sourcePlayer)
	{
		this.id = id;
		this.recordedPlayer = recordedPlayer;
		this.sourcePlayer = sourcePlayer;
		this.positionTracker = new PositionTracker(recordedPlayer, false);
		this.entityFilter = EntityFilter.FOR_RECORDING;

		this.positionTracker.writeToRecordingData(data);
	}

	public void start()
	{
		entityState = null;
		Utils.sendMessage(sourcePlayer, "recording.start.recording_started");
		state = State.RECORDING;
	}

	public void stop()
	{
		state = switch (state)
		{
			case WAITING_FOR_ACTION -> State.CANCELED;
			case RECORDING -> State.WAITING_FOR_DECISION;
			//case WAITING_FOR_DECISION -> State.DISCARDED; //TODO: ?
			default -> State.UNDEFINED;
		};

		if (state.removed) { Recording.removeContext(this); }
	}

	public void discard()
	{
		state = switch (state)
		{
			case WAITING_FOR_ACTION -> State.CANCELED;
			case WAITING_FOR_DECISION -> State.DISCARDED;
			default -> State.UNDEFINED;
		};

		if (state.removed) { Recording.removeContext(this); }
	}

	public void save(File recordingFile, String name)
	{
		if (state == State.WAITING_FOR_DECISION)
		{
			if (!RecordingFiles.save(CommandOutput.LOGS, recordingFile, name, data)) { return; }
			state = State.SAVED;
		}
		else
		{
			state = State.UNDEFINED;
		}

		if (state.removed) { Recording.removeContext(this); }
	}

	public void onTick()
	{
		switch (state)
		{
			case WAITING_FOR_ACTION -> onTickWaiting();
			case RECORDING -> onTickRecording();
		}
	}

	private void onTickWaiting()
	{
		RecordedEntityState newEntityState = new RecordedEntityState(recordedPlayer);

		if (newEntityState.differs(entityState) || positionTracker.getDelta() != null) { start(); }
		else { entityState = newEntityState; }
	}

	private void onTickRecording()
	{
		RecordedEntityState newEntityState = new RecordedEntityState(recordedPlayer);
		newEntityState.saveDifference(data.actions, entityState);
		entityState = newEntityState;

		positionTracker.onTick(data.actions, null);
		entityTracker.onTick();

		if (recordedPlayer.isDeadOrDying() && Settings.RECORD_PLAYER_DEATH.val)
		{
			//TODO: safe saving
			data.endsWithDeath = true;
			Utils.sendMessage(sourcePlayer, "recording.stop.stopped");
			state = State.WAITING_FOR_DECISION;
			return;
		}

		addTickAction();
		tick++;
	}

	public void addAction(Action action)
	{
		data.actions.add(action);
		if (action instanceof BlockAction) { data.blockActions.add((BlockAction)action); }
	}

	public void addTickAction()
	{
		int lastElementPos = data.actions.size() - 1;
		if (lastElementPos < 0)
		{
			addAction(new NextTick());
			return;
		}

		Action lastElement = data.actions.get(lastElementPos);

		if (lastElement instanceof NextTick)
		{
			data.actions.set(lastElementPos, new SkipTicks(2));
		}
		else if (lastElement instanceof SkipTicks && ((SkipTicks)lastElement).canBeModified())
		{
			data.actions.set(lastElementPos, new SkipTicks(((SkipTicks)lastElement).number + 1));
		}
		else
		{
			addAction(new NextTick());
		}
	}

	public @Nullable EntityTracker.TrackedEntity getTrackedEntity(Entity entity)
	{
		return entityTracker.get(entity);
	}

	public int getTick()
	{
		return tick;
	}

	public enum State
	{
		WAITING_FOR_ACTION(false),
		RECORDING(false),
		WAITING_FOR_DECISION(false),
		CANCELED(true),
		DISCARDED(true),
		SAVED(true),
		UNDEFINED(true);

		public final boolean removed;

		State(boolean removed)
		{
			this.removed = removed;
		}
	}
}
