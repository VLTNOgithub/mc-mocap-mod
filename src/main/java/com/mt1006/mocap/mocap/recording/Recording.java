package com.mt1006.mocap.mocap.recording;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mt1006.mocap.MocapMod;
import com.mt1006.mocap.command.InputArgument;
import com.mt1006.mocap.command.io.CommandInfo;
import com.mt1006.mocap.mocap.files.Files;
import com.mt1006.mocap.mocap.files.RecordingFiles;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class Recording
{
	//TODO: temporary enum and variable, move it to config
	private enum QuickDiscard
	{
		ALLOW,
		DISALLOW,
		SAFE;

		public boolean allowsSingle(RecordingContext ctx)
		{
			//TODO: test if ends with death
			return this == SAFE || this == ALLOW;
		}
	}

	private static final int DOUBLE_START_LIMIT = 1024;
	private static final QuickDiscard quickDiscard = QuickDiscard.SAFE;

	private static final List<RecordingContext> contexts = new ArrayList<>();
	private static final Multimap<String, RecordingContext> contextsBySource = HashMultimap.create();
	private static final Map<String, String> awaitDoubleStart = new HashMap<>();

	public static boolean start(CommandInfo commandInfo, ServerPlayer recordedPlayer)
	{
		if (!checkDoubleStart(commandInfo, recordedPlayer)) { return false; }

		boolean success = addContext(recordedPlayer, commandInfo.sourcePlayer);
		if (success) { commandInfo.sendSuccess("recording.start.waiting_for_action"); }
		else { commandInfo.sendFailure("recording.start.error"); }

		return success;
	}

	private static boolean checkDoubleStart(CommandInfo commandInfo, ServerPlayer recordedPlayer)
	{
		ServerPlayer sourcePlayer = commandInfo.sourcePlayer;
		if (sourcePlayer == null) { return true; }

		String sourcePlayerName = sourcePlayer.getName().getString();
		String recordedPlayerName = recordedPlayer.getName().getString();

		if (!recordedPlayerName.equals(awaitDoubleStart.getOrDefault(sourcePlayerName, null)))
		{
			for (RecordingContext ctx : contexts)
			{
				if (ctx.sourcePlayer == sourcePlayer && ctx.recordedPlayer == recordedPlayer)
				{
					handleDoubleStart(commandInfo, ctx);
					return false;
				}
			}
		}
		awaitDoubleStart.remove(sourcePlayerName);
		return true;
	}

	private static void handleDoubleStart(CommandInfo commandInfo, RecordingContext ctx)
	{
		boolean addToDoubleStart = false;

		switch (ctx.state)
		{
			case WAITING_FOR_ACTION:
				ctx.start();
				break;

			case RECORDING:
				commandInfo.sendFailureWithTip("recording.start.already_recording");
				addToDoubleStart = true;
				break;

			case WAITING_FOR_DECISION:
				commandInfo.sendFailureWithTip("recording.start.waiting_for_decision");
				addToDoubleStart = true;
				break;

			default:
				MocapMod.LOGGER.error("Undefined recording context state supplied to double start handler!");
				commandInfo.sendFailureWithTip("recording.start.error");
				break;
		}

		if (addToDoubleStart && ctx.sourcePlayer != null)
		{
			if (awaitDoubleStart.size() > DOUBLE_START_LIMIT)
			{
				awaitDoubleStart.clear();
				MocapMod.LOGGER.warn("awaitDoubleStart limit reached, clearing map");
			}
			awaitDoubleStart.put(ctx.sourcePlayer.getName().getString(), ctx.recordedPlayer.getName().getString());
		}
	}

	public static boolean startMultiple(CommandInfo commandInfo, String str)
	{
		//TODO: finish
		return true;
	}

	public static boolean stop(CommandInfo commandInfo, @Nullable String id)
	{
		//TODO: add playback stopping for recordingSync (make it stop recordings started by player) - add more config for sync
		//if (Settings.RECORDING_SYNC.val) { Playing.stopAll(commandInfo); }

		ResolvedContexts resolvedContexts = ResolvedContexts.resolve(commandInfo, id, false);
		if (resolvedContexts == null) { return false; }

		return resolvedContexts.isSingle
				? stopSingle(commandInfo, resolvedContexts.list.get(0))
				: stopMultiple(commandInfo, resolvedContexts.list);
	}

	private static boolean stopSingle(CommandInfo commandInfo, RecordingContext ctx)
	{
		if (ctx.state == RecordingContext.State.WAITING_FOR_DECISION)
		{
			if (!quickDiscard.allowsSingle(ctx))
			{
				//TODO: proper message when blocked by death
				commandInfo.sendFailureWithTip("recording.stop.quick_discard.disabled");
				return false;
			}
			return discardSingle(commandInfo, ctx);
		}

		ctx.stop();

		switch (ctx.state)
		{
			case WAITING_FOR_DECISION:
				commandInfo.sendSuccess("recording.stop.stopped");
				if (quickDiscard.allowsSingle(ctx)) { commandInfo.sendSuccess("recording.stop.stopped.tip_stop"); }
				else { commandInfo.sendSuccess("recording.stop.stopped.tip_discard"); }
				return true;

			case CANCELED:
				commandInfo.sendSuccess("recording.stop.canceled");
				return true;

			default:
				commandInfo.sendFailure("recording.undefined_state", ctx.state.name());
				return false;
		}
	}

	private static boolean stopMultiple(CommandInfo commandInfo, List<RecordingContext> contexts)
	{
		int stopped = 0, cancelled = 0, stillWaiting = 0, unknownState = 0;

		for (RecordingContext ctx : contexts)
		{
			if (ctx.state == RecordingContext.State.WAITING_FOR_DECISION)
			{
				stillWaiting++;
				continue;
			}

			ctx.stop();

			switch (ctx.state)
			{
				case WAITING_FOR_DECISION: stopped++; break;
				case CANCELED: cancelled++; break;
				default: unknownState++;
			}
		}

		if (stopped == 0 && cancelled == 0 && stillWaiting == 0 && unknownState == 0)
		{
			commandInfo.sendSuccess("recording.multiple.results.none");
			return true;
		}

		commandInfo.sendSuccess("recording.multiple.results");
		if (stopped != 0) { commandInfo.sendSuccess("recording.multiple.results.stopped", stopped); }
		if (cancelled != 0) { commandInfo.sendSuccess("recording.multiple.results.cancelled", cancelled); }
		if (stillWaiting != 0) { commandInfo.sendSuccess("recording.multiple.results.still_waiting", stillWaiting); }

		if (unknownState != 0)
		{
			commandInfo.sendSuccess("recording.multiple.results.error", unknownState);
			commandInfo.sendFailure("recording.multiple.undefined_state");
			return false;
		}
		return true;
	}

	public static boolean discard(CommandInfo commandInfo, @Nullable String id)
	{
		ResolvedContexts resolvedContexts = ResolvedContexts.resolve(commandInfo, id, false);
		if (resolvedContexts == null) { return false; }

		if (resolvedContexts.isSingle)
		{
			RecordingContext ctx = resolvedContexts.list.get(0);
			boolean success = discardSingle(commandInfo, ctx);

			if (success && ctx.state == RecordingContext.State.DISCARDED && quickDiscard.allowsSingle(ctx))
			{
				commandInfo.sendSuccess("recording.discard.quick_discard_tip");
			}
			return success;
		}
		else
		{
			return discardMultiple(commandInfo, resolvedContexts.list);
		}
	}

	private static boolean discardSingle(CommandInfo commandInfo, RecordingContext ctx)
	{
		if (ctx.state == RecordingContext.State.RECORDING)
		{
			commandInfo.sendFailure("recording.discard.not_stopped");
			return false;
		}

		ctx.discard();

		switch (ctx.state)
		{
			case DISCARDED:
				commandInfo.sendSuccess("recording.discard.discarded");
				return true;

			case CANCELED:
				commandInfo.sendSuccess("recording.stop.canceled");
				return true;

			default:
				commandInfo.sendFailure("recording.undefined_state", ctx.state.name());
				return false;
		}
	}

	private static boolean discardMultiple(CommandInfo commandInfo, List<RecordingContext> contexts)
	{
		int discarded = 0, cancelled = 0, stillRecording = 0, unknownState = 0;

		for (RecordingContext ctx : contexts)
		{
			if (ctx.state == RecordingContext.State.RECORDING)
			{
				stillRecording++;
				continue;
			}

			ctx.discard();

			switch (ctx.state)
			{
				case DISCARDED: discarded++; break;
				case CANCELED: cancelled++; break;
				default: unknownState++;
			}
		}

		if (discarded == 0 && cancelled == 0 && stillRecording == 0 && unknownState == 0)
		{
			commandInfo.sendSuccess("recording.multiple.results.none");
			return true;
		}

		commandInfo.sendSuccess("recording.multiple.results");
		if (discarded != 0) { commandInfo.sendSuccess("recording.multiple.results.discarded", discarded); }
		if (cancelled != 0) { commandInfo.sendSuccess("recording.multiple.results.cancelled", cancelled); }
		if (stillRecording != 0) { commandInfo.sendSuccess("recording.multiple.results.still_recording", stillRecording); }

		if (unknownState != 0)
		{
			commandInfo.sendSuccess("recording.multiple.results.error", unknownState);
			commandInfo.sendFailure("recording.multiple.undefined_state");
			return false;
		}
		return true;
	}

	public static boolean save(CommandInfo commandInfo, @Nullable String id, String name)
	{
		ResolvedContexts resolvedContexts = ResolvedContexts.resolve(commandInfo, id, false);
		if (resolvedContexts == null) { return false; }

		return resolvedContexts.isSingle
				? saveSingle(commandInfo, resolvedContexts.list.get(0), name)
				: saveMultiple(commandInfo, resolvedContexts.list, name);
	}

	private static boolean saveSingle(CommandInfo commandInfo, RecordingContext ctx, String name)
	{
		if (ctx.state == RecordingContext.State.RECORDING)
		{
			commandInfo.sendFailure("recording.save.not_stopped");
			return false;
		}

		File recordingFile = Files.getRecordingFile(commandInfo, name);
		if (recordingFile == null) { return false; }

		if (recordingFile.exists())
		{
			commandInfo.sendFailure("recording.save.already_exists");

			String alternativeName = RecordingFiles.findAlternativeName(name);
			if (alternativeName != null)
			{
				commandInfo.sendFailure("recording.save.already_exists.alternative", alternativeName);
			}
			return false;
		}

		ctx.save(recordingFile, name);

		switch (ctx.state)
		{
			case SAVED:
				commandInfo.sendSuccess("recording.save.saved");
				return true;

			case WAITING_FOR_DECISION:
				commandInfo.sendFailure("recording.save.error");
				return false;

			default:
				commandInfo.sendFailure("recording.undefined_state", ctx.state.name());
				return false;
		}
	}

	private static boolean saveMultiple(CommandInfo commandInfo, List<RecordingContext> contexts, String namePrefix)
	{
		List<RecordingContext> stopped = new ArrayList<>();
		for (RecordingContext ctx : contexts)
		{
			if (ctx.state == RecordingContext.State.WAITING_FOR_DECISION) { stopped.add(ctx); }
		}

		List<String> filenames = new ArrayList<>();
		List<File> files = new ArrayList<>();
		for (int i = 1; i <= stopped.size(); i++)
		{
			String filename = String.format("%s%d", namePrefix, i);
			File recordingFile = Files.getRecordingFile(commandInfo, filename);
			if (recordingFile == null) { return false; }

			if (recordingFile.exists())
			{
				commandInfo.sendFailure("recording.save.multiple.already_exists", filename);
				return false;
			}

			filenames.add(filename);
			files.add(recordingFile);
		}

		boolean somethingFailed = false;
		for (int i = 0; i < stopped.size(); i++)
		{
			RecordingContext ctx = stopped.get(i);
			ctx.save(files.get(i), filenames.get(i));

			switch (ctx.state)
			{
				case SAVED -> commandInfo.sendSuccess("recording.save.multiple.saved", ctx.id.str, filenames.get(i));
				case WAITING_FOR_DECISION -> commandInfo.sendFailure("recording.save.multiple.failed", ctx.id.str, filenames.get(i));
				default -> commandInfo.sendFailure("recording.save.multiple.unknown_state", ctx.id.str, filenames.get(i), ctx.state.name());
			}
			if (ctx.state != RecordingContext.State.SAVED) { somethingFailed = true; }
		}

		if (somethingFailed) { commandInfo.sendFailure("recording.save.multiple.error"); }
		return !somethingFailed;
	}

	public static boolean list(CommandInfo commandInfo, @Nullable String id)
	{
		ResolvedContexts resolvedContexts = ResolvedContexts.resolve(commandInfo, id, true);
		if (resolvedContexts == null) { return false; }

		if (resolvedContexts.isSingle)
		{
			RecordingContext ctx = resolvedContexts.list.get(0);
			commandInfo.sendSuccess("recording.list.state", resolvedContexts.fullId.str, ctx.state.name());
			return true;
		}

		ArrayList<String> waitingForAction = new ArrayList<>(), recording = new ArrayList<>(),
				waitingForDecision = new ArrayList<>(), error = new ArrayList<>();

		for (RecordingContext ctx : resolvedContexts.list)
		{
			ArrayList<String> list = switch (ctx.state)
			{
				case WAITING_FOR_ACTION -> waitingForAction;
				case RECORDING -> recording;
				case WAITING_FOR_DECISION -> waitingForDecision;
				default -> error;
			};

			list.add(ctx.id.str);
		}

		Collections.sort(waitingForAction);
		Collections.sort(recording);
		Collections.sort(waitingForDecision);
		Collections.sort(error);

		commandInfo.sendSuccess("recording.list.list", resolvedContexts.fullId.str);

		if (waitingForAction.isEmpty()) { commandInfo.sendSuccess("recording.list.waiting_for_action.none"); }
		else { commandInfo.sendSuccess("recording.list.waiting_for_action", StringUtils.join(waitingForAction, " ")); }

		if (recording.isEmpty()) { commandInfo.sendSuccess("recording.list.recording.none"); }
		else { commandInfo.sendSuccess("recording.list.recording", StringUtils.join(recording, " ")); }

		if (waitingForDecision.isEmpty()) { commandInfo.sendSuccess("recording.list.waiting_for_decision.none"); }
		else { commandInfo.sendSuccess("recording.list.waiting_for_decision", StringUtils.join(waitingForDecision, " ")); }

		if (!error.isEmpty())
		{
			commandInfo.sendSuccess("recording.list.unknown_state", StringUtils.join(error, " "));
			commandInfo.sendSuccess("recording.list.unknown_state.error");
		}
		return true;
	}

	public static void onTick()
	{
		contexts.forEach(RecordingContext::onTick);
	}

	public static boolean isRecordedPlayer(@Nullable Entity entity)
	{
		if (contexts.isEmpty() || !(entity instanceof ServerPlayer)) { return false; }

		for (RecordingContext ctx : contexts)
		{
			if (entity.equals(ctx.recordedPlayer)) { return true; }
		}
		return false;
	}

	public static boolean isActive()
	{
		return !contexts.isEmpty();
	}

	public static List<RecordingContext> fromRecordedPlayer(int id)
	{
		if (contexts.size() == 1) { return (contexts.get(0).recordedPlayer.getId() == id) ? List.of(contexts.get(0)) : List.of(); }

		List<RecordingContext> list = new ArrayList<>();
		for (RecordingContext ctx : contexts)
		{
			if (ctx.recordedPlayer.getId() == id) { list.add(ctx); }
		}
		return list;
	}

	public static List<RecordingContext> fromRecordedPlayer(Entity entity)
	{
		if (!(entity instanceof Player)) { return List.of(); }
		if (contexts.size() == 1) { return (contexts.get(0).recordedPlayer == entity) ? List.of(contexts.get(0)) : List.of(); }

		List<RecordingContext> list = new ArrayList<>();
		for (RecordingContext ctx : contexts)
		{
			if (ctx.recordedPlayer == entity) { list.add(ctx); }
		}
		return list;
	}

	public static List<RecordingContext> getContexts()
	{
		return contexts;
	}

	public static @Nullable List<RecordingContext> resolveContexts(CommandInfo commandInfo, String id)
	{
		ResolvedContexts resolvedContexts = ResolvedContexts.resolve(commandInfo, id, false);
		return resolvedContexts != null ? resolvedContexts.list : null;
	}

	public static List<EntityTracker.TrackedEntity> listTrackedEntities(Entity entity)
	{
		if (contexts.size() == 1)
		{
			EntityTracker.TrackedEntity trackedEntity = contexts.get(0).getTrackedEntity(entity);
			return trackedEntity != null ? List.of(trackedEntity) : List.of();
		}

		List<EntityTracker.TrackedEntity> list = new ArrayList<>();
		for (RecordingContext ctx : contexts)
		{
			EntityTracker.TrackedEntity trackedEntity = ctx.getTrackedEntity(entity);
			if (trackedEntity != null) { list.add(trackedEntity); }
		}
		return list;
	}

	private static boolean addContext(ServerPlayer recordedPlayer, @Nullable ServerPlayer sourcePlayer)
	{
		RecordingId id = new RecordingId(contexts, recordedPlayer, sourcePlayer);
		if (!id.isProper()) { return false; }

		RecordingContext ctx = new RecordingContext(id, recordedPlayer, sourcePlayer);
		contexts.add(ctx);
		if (sourcePlayer != null) { contextsBySource.put(sourcePlayer.getName().getString(), ctx); }

		InputArgument.addServerInput(id.str);
		return true;
	}

	public static void removeContext(RecordingContext ctx)
	{
		contexts.remove(ctx);
		if (ctx.sourcePlayer != null) { contextsBySource.remove(ctx.sourcePlayer.getName().getString(), ctx); }
		InputArgument.removeServerInput(ctx.id.str);
	}

	public static void onServerStop()
	{
		contexts.clear();
		contextsBySource.clear();
		awaitDoubleStart.clear();
	}

	private record ResolvedContexts(List<RecordingContext> list, boolean isSingle, RecordingId fullId)
	{
		public static @Nullable ResolvedContexts resolve(CommandInfo commandInfo, @Nullable String idStr, boolean listMode)
		{
			//TODO: test when empty
			if (idStr == null)
			{
				if (listMode) { return new ResolvedContexts(contexts, false, RecordingId.ALL); }
				RecordingContext ctx = resolveEmpty(commandInfo);
				return ctx != null ? new ResolvedContexts(List.of(ctx), true, ctx.id) : null;
			}

			RecordingId id = new RecordingId(idStr, commandInfo.sourcePlayer);

			if (!id.isProper())
			{
				if (idStr.contains("_"))
				{
					commandInfo.sendFailureWithTip("recording.resolve.improper_group_structure");
				}
				else
				{
					commandInfo.sendFailure("recording.resolve.improper_structure");
					if (!listMode) { commandInfo.sendFailure("recording.resolve.list_tip"); }
				}
				return null;
			}

			List<RecordingContext> matchingContexts = new ArrayList<>();
			for (RecordingContext ctx : contexts)
			{
				if (ctx.id.matches(id)) { matchingContexts.add(ctx); }
			}

			boolean isSingle = id.isSingle();
			if (isSingle)
			{
				//TODO: improve unexpected error info
				if (matchingContexts.size() > 1) { MocapMod.LOGGER.error("Multiple recording contexts are matching single id!"); }
				if (matchingContexts.isEmpty())
				{
					commandInfo.sendFailure("recording.resolve.not_found");
					if (!listMode) { commandInfo.sendFailure("recording.resolve.list_tip"); }
					return null;
				}
			}

			return new ResolvedContexts(matchingContexts, isSingle, id);
		}

		private static @Nullable RecordingContext resolveEmpty(CommandInfo commandInfo)
		{
			ServerPlayer source = commandInfo.sourcePlayer;
			if (source == null)
			{
				//TODO: error message
				return null;
			}

			Collection<RecordingContext> sourceContexts = contextsBySource.get(source.getName().getString());

			if (sourceContexts.size() > 1)
			{
				commandInfo.sendFailureWithTip("recording.resolve.multiple_recordings");
				return null;
			}
			else if (sourceContexts.isEmpty())
			{
				if (contexts.isEmpty()) { commandInfo.sendFailure("recording.resolve.server_not_recording"); }
				else { commandInfo.sendFailureWithTip("recording.resolve.player_not_recording"); }
				return null;
			}

			return sourceContexts.iterator().next();
		}
	}
}
