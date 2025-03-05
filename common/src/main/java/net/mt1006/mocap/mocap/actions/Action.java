package net.mt1006.mocap.mocap.actions;

import net.minecraft.world.entity.Entity;
import net.mt1006.mocap.mocap.actions.deprecated.HeadRotation;
import net.mt1006.mocap.mocap.actions.deprecated.MovementLegacy;
import net.mt1006.mocap.mocap.files.RecordingData;
import net.mt1006.mocap.mocap.files.RecordingFiles;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface Action
{
	List<FromReader> REGISTERED = new ArrayList<>();
	MutableInt lastRegisteredId = new MutableInt(-1);

	default void prepareWrite(RecordingData data) {}
	void write(RecordingFiles.Writer writer);
	Result execute(ActionContext ctx);

	static void init()
	{
		if (REGISTERED.isEmpty())
		{
			for (Type type : Type.values()) { type.init(); }
		}
	}

	static Action readAction(RecordingFiles.Reader reader, RecordingData data)
	{
		FromReader constructor = REGISTERED.get(reader.readByte());
		return constructor != null ? constructor.apply(reader, data) : null;
	}

	enum Type
	{
		NEXT_TICK(0, NextTick::new),
		MOVEMENT_LEGACY(1, MovementLegacy::new), // deprecated
		HEAD_ROTATION(2, HeadRotation::new), // deprecated
		CHANGE_POSE(3, ChangePose::new, ChangePose::new),
		CHANGE_ITEM(4, ChangeItem::new, ChangeItem::new),
		SET_ENTITY_FLAGS(5, SetEntityFlags::new, SetEntityFlags::new),
		SET_LIVING_ENTITY_FLAGS(6, SetLivingEntityFlags::new, SetLivingEntityFlags::new),
		SET_MAIN_HAND(7, SetMainHand::new, SetMainHand::new),
		SWING(8, Swing::new, Swing::new),
		BREAK_BLOCK(9, (FromReaderOnly)BreakBlock::new),
		PLACE_BLOCK(10, PlaceBlock::new),
		RIGHT_CLICK_BLOCK(11, (FromReaderOnly)RightClickBlock::new),
		SET_EFFECT_COLOR(12, SetEffectColor::new, SetEffectColor::new),
		SET_ARROW_COUNT(13, SetArrowCount::new, SetArrowCount::new),
		SLEEP(14, Sleep::new, Sleep::new),
		PLACE_BLOCK_SILENTLY(15, PlaceBlockSilently::new),
		ENTITY_UPDATE(16, EntityUpdate::new),
		ENTITY_ACTION(17, EntityAction::new),
		HURT(18, Hurt::new),
		VEHICLE_DATA(19, VehicleData::new, VehicleData::new),
		BREAK_BLOCK_PROGRESS(20, (FromReaderOnly)BreakBlockProgress::new),
		MOVEMENT(21, Movement::new),
		SKIP_TICKS(22, SkipTicks::new),
		DIE(23, Die::new),
		RESPAWN(24, Respawn::new);

		public final byte id;
		public final FromReader fromReader;
		public final Function<Entity, ComparableAction> fromEntity;

		Type(int id, FromReaderOnly onlyFromReader)
		{
			this(id, (reader, data) -> onlyFromReader.apply(reader));
		}

		Type(int id, FromReaderOnly onlyFromReader, @Nullable FromEntity fromEntity)
		{
			this(id, (reader, data) -> onlyFromReader.apply(reader), fromEntity);
		}

		Type(int id, FromReader fromReader)
		{
			this(id, fromReader, null);
			if (fromReader.apply(RecordingFiles.Reader.DUMMY, RecordingData.DUMMY) instanceof ComparableAction)
			{
				throw new RuntimeException("Tried to register ComparableAction without \"fromEntity\" constructor!");
			}
		}

		Type(int id, FromReader fromReader, @Nullable FromEntity fromEntity)
		{
			this.id = (byte)id;
			this.fromReader = fromReader;
			this.fromEntity = fromEntity;

			if (id > 255) { throw new RuntimeException("Tried to register an Action with ID higher than 255!"); }
			if (id != lastRegisteredId.getValue() + 1) { throw new RuntimeException("Tried to register an Action with id out of order!"); }
			lastRegisteredId.setValue(id);

			if (fromEntity != null) { ComparableAction.REGISTERED.add(fromEntity); }
			REGISTERED.add(fromReader);
		}

		public void init() { /* dummy */ }
	}

	enum Result
	{
		OK(false, false),
		IGNORED(false, false),
		NEXT_TICK(true, false),
		REPEAT(true, false),
		END(true, true),
		ERROR(true, true);

		public final boolean endsTick;
		public final boolean endsPlayback;

		Result(boolean endsTick, boolean endsPlayback)
		{
			this.endsTick = endsTick;
			this.endsPlayback = endsPlayback;
		}
	}

	interface FromReader extends BiFunction<RecordingFiles.Reader, RecordingData, Action> {}
	interface FromReaderOnly extends Function<RecordingFiles.Reader, Action> {}
	interface FromEntity extends Function<Entity, ComparableAction> {}
}
