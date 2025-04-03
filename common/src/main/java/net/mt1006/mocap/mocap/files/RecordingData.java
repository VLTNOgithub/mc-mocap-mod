package net.mt1006.mocap.mocap.files;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.mt1006.mocap.MocapMod;
import net.mt1006.mocap.command.io.CommandOutput;
import net.mt1006.mocap.mocap.actions.Action;
import net.mt1006.mocap.mocap.actions.BlockAction;
import net.mt1006.mocap.mocap.actions.NextTick;
import net.mt1006.mocap.mocap.actions.SkipTicks;
import net.mt1006.mocap.mocap.playing.playback.ActionContext;
import net.mt1006.mocap.mocap.playing.playback.PositionTransformer;
import net.mt1006.mocap.mocap.settings.Settings;
import net.mt1006.mocap.utils.EntityData;
import net.mt1006.mocap.utils.Utils;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecordingData
{
	public static final RecordingData DUMMY = new RecordingData();

	private static final byte FLAGS1_ENDS_WITH_DEATH =           0b00000001;
	private static final byte FLAGS1_NULL_TERMINATED_STRINGS =   0b00000010;
	private static final byte FLAGS1_HAS_ID_MAPS =               0b00000100;
	private static final byte FLAGS1_START_DIMENSION_SPECIFIED = 0b00001000;
	private static final byte FLAGS1_PLAYER_NAME_SPECIFIED =     0b00010000;

	public long fileSize = 0;
	public byte version = 0;
	public boolean experimentalVersion = false;
	public Vec3 startPos = Vec3.ZERO;
	public final float[] startRot = new float[2];
	public boolean endsWithDeath = false; // deprecated
	private boolean usesIdMaps = true;
	public final ItemIdMap itemIdMap = new ItemIdMap(this);
	public final BlockStateIdMap blockStateIdMap = new BlockStateIdMap(this);
	public @Nullable String startDimension = null; //TODO: use it
	public @Nullable String playerName = null;
	public final List<Action> actions = new ArrayList<>();
	public final List<BlockAction> blockActions = new ArrayList<>();
	public long tickCount = 0;

	public static RecordingData forWriting()
	{
		RecordingData data = new RecordingData();
		data.version = RecordingFiles.VERSION;
		data.experimentalVersion = MocapMod.EXPERIMENTAL;
		return data;
	}

	public void save(BufferedOutputStream stream) throws IOException
	{
		if (version != RecordingFiles.VERSION) { throw new RuntimeException("Trying to save recording with read-only version."); }
		actions.forEach((action) -> action.prepareWrite(this));

		RecordingFiles.Writer writer = new RecordingFiles.Writer(this);

		writer.addByte((byte)(experimentalVersion ? (-version) : version));
		saveHeader(writer);
		actions.forEach((action) -> action.write(writer));

		stream.write(writer.toByteArray());
	}

	public boolean load(CommandOutput commandOutput, String name)
	{
		byte[] data = Files.loadFile(Files.getRecordingFile(commandOutput, name));
		return data != null && load(commandOutput, new RecordingFiles.FileReader(this, data, true));
	}

	private boolean load(CommandOutput commandOutput, RecordingFiles.FileReader reader)
	{
		fileSize = reader.getSize();

		byte versionByte = reader.readByte();
		version = (byte)Math.abs(versionByte);
		experimentalVersion = (versionByte < 0);

		if (version > RecordingFiles.VERSION)
		{
			commandOutput.sendFailure("playback.start.error.load_header");
			return false;
		}

		loadHeader(reader, version <= 2); //TODO: test old recordings

		while (reader.canRead())
		{
			Action action = Action.readAction(reader, this);
			if (action == null) { return false; }

			actions.add(action);
			if (action instanceof BlockAction) { blockActions.add((BlockAction)action); }
			else if (action instanceof NextTick) { tickCount++; }
			else if (action instanceof SkipTicks) { tickCount += ((SkipTicks)action).number; }
		}
		return true;
	}

	private void saveHeader(RecordingFiles.Writer writer)
	{
		boolean hasIdMaps = usesIdMaps && (itemIdMap.size() != 0 || blockStateIdMap.size() != 0);

		writer.addVec3(startPos);
		writer.addFloat(startRot[0]);
		writer.addFloat(startRot[1]);

		byte flags1 = 0;
		flags1 |= endsWithDeath ? FLAGS1_ENDS_WITH_DEATH : 0;
		flags1 |= FLAGS1_NULL_TERMINATED_STRINGS;
		flags1 |= hasIdMaps ? FLAGS1_HAS_ID_MAPS : 0;
		flags1 |= startDimension != null ? FLAGS1_START_DIMENSION_SPECIFIED : 0;
		flags1 |= playerName != null ? FLAGS1_PLAYER_NAME_SPECIFIED : 0;
		writer.addByte(flags1);

		if (hasIdMaps)
		{
			itemIdMap.save(writer);
			blockStateIdMap.save(writer);
		}

		if (startDimension != null) { writer.addString(startDimension); }
		if (playerName != null) { writer.addString(playerName); }
	}

	private void loadHeader(RecordingFiles.FileReader reader, boolean legacyHeader)
	{
		startPos = reader.readVec3();
		startRot[0] = reader.readFloat();
		startRot[1] = reader.readFloat();
		if (legacyHeader) { return; }

		byte flags1 = reader.readByte();
		endsWithDeath = (flags1 & FLAGS1_ENDS_WITH_DEATH) != 0;
		reader.setStringMode((flags1 & FLAGS1_NULL_TERMINATED_STRINGS) == 0);
		usesIdMaps = (flags1 & FLAGS1_HAS_ID_MAPS) != 0;
		boolean startDimensionSpecified = (flags1 & FLAGS1_START_DIMENSION_SPECIFIED) != 0;
		boolean playerNameSpecified = (flags1 & FLAGS1_PLAYER_NAME_SPECIFIED) != 0;

		if (usesIdMaps)
		{
			itemIdMap.load(reader);
			blockStateIdMap.load(reader);
		}

		if (startDimensionSpecified) { startDimension = reader.readString(); }
		if (playerNameSpecified) { playerName = reader.readString(); }
	}

	public void initEntityPosition(Entity entity, PositionTransformer transformer)
	{
		float rotY = transformer.transformRotation(startRot[0]);
		entity.moveTo(transformer.transformPos(startPos), rotY, startRot[1]);
		entity.setYHeadRot(rotY);
	}

	public void preExecute(Entity entity, PositionTransformer transformer)
	{
		if (Settings.BLOCK_INITIALIZATION.val)
		{
			for (int i = blockActions.size() - 1; i >= 0; i--)
			{
				blockActions.get(i).preExecute(entity, transformer);
			}
		}
	}

	public Action.Result executeNext(ActionContext ctx, int pos)
	{
		if (pos >= actions.size()) { return Action.Result.END; }
		if (pos == 0) { firstExecute(ctx.entity); }

		try
		{
			Action nextAction = actions.get(pos);
			if (!Settings.BLOCK_ACTIONS_PLAYBACK.val && nextAction instanceof BlockAction) { return Action.Result.OK; }

			return nextAction.execute(ctx);
		}
		catch (Exception e)
		{
			Utils.exception(e, "Exception occurred while executing action!");
			return Action.Result.ERROR;
		}
	}

	private void firstExecute(Entity entity)
	{
		if (entity instanceof Player)
		{
			//TODO: recording skin parts
			EntityData.PLAYER_SKIN_PARTS.set(entity, (byte)0b01111111);
		}
	}

	public static abstract class RefIdMap<T>
	{
		protected final RecordingData parent;
		protected final Reference2IntMap<T> refToId = new Reference2IntOpenHashMap<>();
		protected final List<T> idToRef = new ArrayList<>();

		public RefIdMap(RecordingData parent)
		{
			this.parent = parent;
			init();
		}

		public int size()
		{
			return idToRef.size() - 1;
		}

		protected int provideMappedId(T ref)
		{
			int id = refToId.getOrDefault(ref, -1);
			return id != -1 ? id : put(ref);
		}

		protected T getMappedObject(int id)
		{
			return idToRef.get(id);
		}

		protected int put(T ref)
		{
			int pos = idToRef.size();
			idToRef.add(ref);
			refToId.put(ref, pos);
			return pos;
		}

		protected String resLocToStr(ResourceLocation resLoc)
		{
			//TODO: test with mods
			return resLoc.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)
					? resLoc.getPath()
					: resLoc.toString();
		}

		protected abstract void init();
		public abstract int provideId(T ref);
		public abstract T getObject(int id);
		protected abstract void save(RecordingFiles.Writer writer);
		protected abstract void load(RecordingFiles.Reader reader);
	}

	public static class ItemIdMap extends RefIdMap<Item>
	{
		public ItemIdMap(RecordingData parent) { super(parent); }
		@Override protected void init() { put(Items.AIR); }

		@Override public int provideId(Item item)
		{
			return parent.usesIdMaps ? provideMappedId(item) : Item.getId(item);
		}

		@Override public Item getObject(int id)
		{
			return parent.usesIdMaps ? getMappedObject(id) : Item.byId(id);
		}

		@Override protected void save(RecordingFiles.Writer writer)
		{
			writer.addInt(size());
			idToRef.subList(1, idToRef.size())
					.forEach((item) -> writer.addString(resLocToStr(BuiltInRegistries.ITEM.getKey(item))));
		}

		@Override protected void load(RecordingFiles.Reader reader)
		{
			int size = reader.readInt();

			for (int i = 1; i <= size; i++)
			{
				Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(reader.readString()));
				refToId.put(item, i);
				idToRef.add(item);
			}
		}
	}

	public static class BlockStateIdMap extends RefIdMap<BlockState>
	{
		public BlockStateIdMap(RecordingData parent) { super(parent); }
		@Override protected void init() { put(Blocks.AIR.defaultBlockState()); }

		@Override public int provideId(BlockState blockState)
		{
			return parent.usesIdMaps ? provideMappedId(blockState) : Block.getId(blockState);
		}

		@Override public BlockState getObject(int id)
		{
			return parent.usesIdMaps ? getMappedObject(id) : Block.stateById(id);
		}

		@Override protected void save(RecordingFiles.Writer writer)
		{
			List<Property<?>> properties = new ArrayList<>();
			writer.addInt(size());

			for (BlockState blockState : idToRef.subList(1, idToRef.size()))
			{
				properties.clear();
				properties.addAll(blockState.getProperties());

				if (properties.size() > Short.MAX_VALUE)
				{
					MocapMod.LOGGER.warn("BlockState properties count limit reached ({})!", properties.size());
					properties.clear();
				}

				writer.addString(resLocToStr(BuiltInRegistries.BLOCK.getKey(blockState.getBlock())));
				writer.addShort((short)properties.size());

				for (Property<?> property : properties)
				{
					writer.addString(property.getName());
					writer.addString(propertyValueToStr(blockState, property));
				}
			}
		}

		@Override protected void load(RecordingFiles.Reader reader)
		{
			int size = reader.readInt();

			for (int i = 1; i <= size; i++)
			{
				Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(reader.readString()));
				StateDefinition<Block, BlockState> stateDefinition = block.getStateDefinition();
				BlockState blockState = block.defaultBlockState();

				short propertyCount = reader.readShort();
				for (int j = 0; j < propertyCount; j++)
				{
					Property<?> property = stateDefinition.getProperty(reader.readString());
					blockState = updateBlockState(blockState, property, reader.readString());
				}

				refToId.put(blockState, i);
				idToRef.add(blockState);
			}
		}

		private static <T extends Comparable<T>> String propertyValueToStr(BlockState blockState, Property<T> property)
		{
			return property.getName(blockState.getValue(property));
		}

		private static <T extends Comparable<T>> BlockState updateBlockState(BlockState blockState, @Nullable Property<T> property, String str)
		{
			if (property == null) { return blockState; }
			Optional<T> value = property.getValue(str);
			return value.map((val) -> blockState.setValue(property, val)).orElse(blockState);
		}
	}
}
