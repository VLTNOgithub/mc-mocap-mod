package net.mt1006.mocap.mocap.recording;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.mt1006.mocap.command.io.CommandOutput;
import net.mt1006.mocap.mocap.actions.*;
import net.mt1006.mocap.mocap.files.RecordingData;
import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.modifiers.EntityFilter;
import net.mt1006.mocap.mocap.settings.Settings;
import net.mt1006.mocap.mocap.settings.enums.OnDeath;
import net.mt1006.mocap.utils.Utils;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class RecordingContext
{
	public final RecordingId id;
	public ServerPlayer recordedPlayer;
	public final @Nullable ServerPlayer sourcePlayer;
	public final RecordingData data = RecordingData.forWriting();
	public State state = State.WAITING_FOR_ACTION;
	private @Nullable RecordedEntityState entityState = null;
	private final PositionTracker positionTracker;
	private final EntityTracker entityTracker = new EntityTracker(this);
	public final EntityFilter entityFilter;
	private int tick = 0, diedOnTick = 0;
	private boolean died = false;

	public RecordingContext(RecordingId id, ServerPlayer recordedPlayer, @Nullable ServerPlayer sourcePlayer)
	{
		this.id = id;
		this.recordedPlayer = recordedPlayer;
		this.sourcePlayer = sourcePlayer;
		this.positionTracker = new PositionTracker(recordedPlayer, false);
		this.entityFilter = EntityFilter.FOR_RECORDING;

		this.positionTracker.writeToRecordingData(data);

		if (Settings.ASSIGN_DIMENSIONS.val) { data.startDimension = recordedPlayer.level().dimension().location().toString(); }
		if (Settings.ASSIGN_PLAYER_NAME.val) { data.playerName = recordedPlayer.getName().getString(); }
	}

	public void start(boolean sendMessage)
	{
		entityState = null;
		state = State.RECORDING;
		if (sendMessage) { Utils.sendMessage(sourcePlayer, "recording.start.recording_started"); }
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

		if (newEntityState.differs(entityState) || positionTracker.getDelta() != null) { start(true); }
		else { entityState = newEntityState; }
	}

	//TODO: safe saving
	private void onTickRecording()
	{
		tick++;
		if (died)
		{
			int tickDiff = tick - diedOnTick;
			if (Settings.ON_DEATH.val == OnDeath.CONTINUE_SYNCED || tickDiff < 20)
			{
				entityTracker.onTick();
				addTickAction();
			}

			if (tickDiff == 20)
			{
				if (Settings.ON_DEATH.val == OnDeath.END_RECORDING) { stopRecording("recording.stop.stopped"); }
				else if (Settings.ON_DEATH.val != OnDeath.SPLIT_RECORDING) { positionTracker.teleportFarAway(data.actions); }
			}
			return;
		}

		RecordedEntityState newEntityState = new RecordedEntityState(recordedPlayer);
		newEntityState.saveDifference(data.actions, entityState);
		entityState = newEntityState;

		positionTracker.onTick(data.actions, null);
		entityTracker.onTick();

		if (recordedPlayer.isDeadOrDying())
		{
			addAction(new Die());
			died = true;
			diedOnTick = tick;

			if (Settings.ON_DEATH.val != OnDeath.END_RECORDING) { Recording.waitingForRespawn.put(recordedPlayer, this); }
		}
		else if (recordedPlayer.isRemoved())
		{
			stopRecording("recording.stop.stopped");
		}

		addTickAction();
	}

	public void onRespawn(ServerPlayer newPlayer)
	{
		if (Settings.ON_DEATH.val == OnDeath.SPLIT_RECORDING)
		{
			splitRecording(newPlayer);
			return;
		}

		died = false;
		recordedPlayer = newPlayer;
		positionTracker.setEntity(newPlayer);
		addAction(new Respawn());
	}

	public void stopRecording(String message)
	{
		state = State.WAITING_FOR_DECISION;
		Utils.sendMessage(sourcePlayer, message);
	}

	public void splitRecording(ServerPlayer newPlayer)
	{
		stopRecording("recording.stop.split");
		boolean success = Recording.start(newPlayer, sourcePlayer, true, false);
		if (!success) { Utils.sendMessage(sourcePlayer, "recording.stop.split.error"); }
	}

	public void addAction(Action action)
	{
		if (state != State.RECORDING)
		{
			if (state == State.WAITING_FOR_ACTION) { start(true); }
			else { return; }
		}

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
