package com.mt1006.mocap.mocap.recording;

import com.mojang.datafixers.util.Pair;
import com.mt1006.mocap.MocapMod;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RecordingId
{
	public static final RecordingId ALL = new RecordingId("_._._", null);
	public final @Nullable String str;
	private final @Nullable String source;
	private final @Nullable String recorded;
	private final @Nullable String name;

	public RecordingId(String id, @Nullable ServerPlayer sourcePlayer)
	{
		if (id.startsWith("-")) { id = id.substring(1); }

		String[] parts = id.split("\\.");

		if (!verifyParts(parts))
		{
			this.str = null;
			this.source = null;
			this.recorded = null;
			this.name = null;
			return;
		}

		switch (parts.length)
		{
			case 1:
				this.source = getSourceName(sourcePlayer);
				this.recorded = getNullablePart(parts[0]);
				this.name = parts[0].equals("_") ? null : "1";
				break;

			case 2:
				this.source = getSourceName(sourcePlayer);
				this.recorded = getNullablePart(parts[0]);
				this.name = getNullablePart(parts[1]);
				break;

			case 3:
				this.source = getNullablePart(parts[0]);
				this.recorded = getNullablePart(parts[1]);
				this.name = getNullablePart(parts[2]);
				break;

			default:
				throw new RuntimeException();
		}

		this.str = String.format("-%s.%s.%s", getNotNullPart(source), getNotNullPart(recorded), getNotNullPart(name));
	}

	public RecordingId(List<RecordingContext> contexts, ServerPlayer recordedPlayer, @Nullable ServerPlayer sourcePlayer)
	{
		String source = getSourceName(sourcePlayer);
		String recorded = recordedPlayer.getName().getString();

		long maxId = 0;
		for (RecordingContext ctx : contexts)
		{
			if (ctx.id.source == null || ctx.id.recorded == null)
			{
				MocapMod.LOGGER.warn("Found context with non-single id");
				continue;
			}

			if (!ctx.id.source.equals(source) || !ctx.id.recorded.equals(recorded)) { continue; }

			long num = -1;
			if (ctx.id.name != null)
			{
				try { num = Long.parseLong(ctx.id.name); }
				catch (Exception ignore) {}
			}

			if (num > maxId) { maxId = num; }
		}

		String name = Long.toString(maxId + 1);
		String finalId = String.format("-%s.%s.%s", source, recorded, name);

		for (RecordingContext ctx : contexts)
		{
			if (ctx.id.str == null)
			{
				MocapMod.LOGGER.warn("Found context with improper id");
				continue;
			}

			if (ctx.id.str.equals(finalId))
			{
				MocapMod.LOGGER.error("Tried to create context with already existing id!");
				this.str = null;
				this.source = null;
				this.recorded = null;
				this.name = null;
				return;
			}
		}

		this.str = finalId;
		this.source = source;
		this.recorded = recorded;
		this.name = name;
	}

	public boolean isSingle()
	{
		return source != null && recorded != null && name != null;
	}

	public boolean isProper()
	{
		return str != null;
	}

	public boolean matches(RecordingId id)
	{
		return (id.source == null || id.source.equals(source))
				&& (id.recorded == null || id.recorded.equals(recorded))
				&& (id.name == null || id.name.equals(name));
	}

	//TODO: find use for modifiers or remove them
	//TODO: allow multiple flags (Pair<String, String[]>)
	public static Pair<String, String> separateFlags(@Nullable String input)
	{
		if (input == null) { return Pair.of("", ""); }
		int flagsPos = input.indexOf("!");
		return flagsPos != -1 ? Pair.of(input.substring(0, flagsPos), input.substring(flagsPos)) : Pair.of(input, "");
	}

	private static String getNotNullPart(@Nullable String part)
	{
		return part != null ? part : "_";
	}

	private static @Nullable String getNullablePart(String part)
	{
		return part.equals("_") ? null : part;
	}

	private static String getSourceName(@Nullable ServerPlayer sourcePlayer)
	{
		return sourcePlayer != null ? sourcePlayer.getName().getString() : "+mc";
	}

	private static boolean verifyParts(String[] parts)
	{
		if (parts.length > 3 || parts.length == 0) { return false; }

		for (String part : parts)
		{
			if (!isProperPartName(part)) { return false; }
		}

		boolean shouldBeGroup = parts[parts.length - 1].equals("_");
		if (!shouldBeGroup)
		{
			for (String part : parts)
			{
				if (part.equals("_")) { return false; }
			}
		}
		return true;
	}

	private static boolean isProperPartName(String part)
	{
		if (part.isEmpty()) { return false; }
		if (part.equals("_")) { return true; }

		for (char c : part.toCharArray())
		{
			if (!isAllowedPartChar(c)) { return false; }
		}
		return true;
	}

	private static boolean isAllowedPartChar(char c)
	{
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '+';
	}
}
