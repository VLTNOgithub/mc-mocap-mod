package net.mt1006.mocap.command.io;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.mt1006.mocap.command.CommandUtils;
import net.mt1006.mocap.mocap.playing.modifiers.PlaybackModifiers;
import net.mt1006.mocap.mocap.playing.modifiers.PlayerAsEntity;
import net.mt1006.mocap.mocap.playing.modifiers.PlayerSkin;
import net.mt1006.mocap.utils.Utils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class CommandInfo implements CommandOutput
{
	public final CommandContext<CommandSourceStack> ctx;
	public final CommandSourceStack source;
	public final MinecraftServer server;
	public final ServerLevel level;
	public final @Nullable ServerPlayer sourcePlayer;
	public final @Nullable Entity sourceEntity;

	public CommandInfo(CommandContext<CommandSourceStack> ctx)
	{
		this.ctx = ctx;
		this.source = ctx.getSource();
		this.server = source.getServer();
		this.level = source.getLevel();
		this.sourcePlayer = source.getPlayer();
		this.sourceEntity = source.getEntity();
	}

	@Override public void sendSuccess(String component, Object... args)
	{
		source.sendSuccess(() -> getTranslatableComponent(component, args), false);
	}

	@Override public void sendSuccessLiteral(String format, Object... args)
	{
		source.sendSuccess(() -> Component.literal(String.format(format, args)), false);
	}

	@Override public void sendSuccessComponent(Component component)
	{
		source.sendSuccess(() -> component, false);
	}

	@Override public void sendFailure(String component, Object... args)
	{
		source.sendFailure(getTranslatableComponent(component, args));
	}

	@Override public void sendFailureWithTip(String component, Object... args)
	{
		source.sendFailure(getTranslatableComponent(component, args));
		source.sendFailure(getTranslatableComponent(component + ".tip"));
	}

	@Override public void sendException(Exception exception, String component, Object... args)
	{
		sendFailure(component, args);
		Utils.exception(exception, Utils.stringFromComponent(component, args));
	}

	public MutableComponent getTranslatableComponent(String component, Object... args)
	{
		return Utils.getTranslatableComponent(source.getEntity(), component, args);
	}

	public @Nullable CommandInfo getFinalCommandInfo()
	{
		CommandContext<CommandSourceStack> tempCtx = ctx;
		while (true)
		{
			String command = CommandUtils.getNode(tempCtx.getNodes(), 0);
			if (command != null && (command.equals("mocap") || command.equals("mocap:mocap"))) { return new CommandInfo(tempCtx); }

			tempCtx = ctx.getChild();
			if (tempCtx == null) { return null; }
		}
	}

	public @Nullable String getNode(int pos)
	{
		return CommandUtils.getNode(ctx.getNodes(), pos);
	}

	public int getInteger(String name)
	{
		return IntegerArgumentType.getInteger(ctx, name);
	}

	public double getDouble(String name)
	{
		return DoubleArgumentType.getDouble(ctx, name);
	}

	public boolean getBool(String name)
	{
		return BoolArgumentType.getBool(ctx, name);
	}

	public String getString(String name)
	{
		return StringArgumentType.getString(ctx, name);
	}

	public Collection<GameProfile> getGameProfiles(String name) throws CommandSyntaxException
	{
		return GameProfileArgument.getGameProfiles(ctx, name);
	}

	public @Nullable String getNullableString(String name)
	{
		try { return StringArgumentType.getString(ctx, name); }
		catch (Exception e) { return null; }
	}

	public PlayerSkin getPlayerSkin()
	{
		String fromPlayer = getNullableString("skin_player_name");
		if (fromPlayer != null) { return new PlayerSkin(PlayerSkin.SkinSource.FROM_PLAYER, fromPlayer); }

		String fromFile = getNullableString("skin_filename");
		if (fromFile != null) { return new PlayerSkin(PlayerSkin.SkinSource.FROM_FILE, fromFile); }

		String fromMineskin = getNullableString("mineskin_url");
		if (fromMineskin != null) { return new PlayerSkin(PlayerSkin.SkinSource.FROM_MINESKIN, fromMineskin); }

		return PlayerSkin.DEFAULT;
	}

	public @Nullable PlaybackModifiers getSimpleModifiers(CommandOutput commandOutput)
	{
		String playerName = getNullableString("player_name");
		if (!PlaybackModifiers.checkIfProperName(commandOutput, playerName)) { return null; }

		PlayerSkin playerSkin = getPlayerSkin();
		PlayerAsEntity playerAsEntity = PlayerAsEntity.DISABLED;

		try
		{
			String playerAsEntityId = ResourceArgument.getEntityType(ctx, "entity").key().location().toString();

			Tag tag;
			try { tag = NbtTagArgument.getNbtTag(ctx, "nbt"); }
			catch (Exception e) { tag = null; }
			CompoundTag nbt = (tag instanceof CompoundTag) ? (CompoundTag)tag : null;

			playerAsEntity = new PlayerAsEntity(playerAsEntityId, nbt != null ? nbt.toString() : null);
		}
		catch (Exception ignore) {}

		PlaybackModifiers modifiers = PlaybackModifiers.empty();
		modifiers.playerName = playerName;
		modifiers.playerSkin = playerSkin;
		modifiers.playerAsEntity = playerAsEntity;
		return modifiers;
	}
}
