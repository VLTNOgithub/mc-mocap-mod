package net.mt1006.mocap.command.io;

import net.minecraft.network.chat.Component;
import net.mt1006.mocap.MocapMod;
import net.mt1006.mocap.utils.Utils;

class LogsCommandOutput implements CommandOutput
{
	@Override public void sendSuccess(String component, Object... args) {}
	@Override public void sendSuccessLiteral(String format, Object... args) {}
	@Override public void sendSuccessComponent(Component component) {}

	@Override public void sendFailure(String component, Object... args)
	{
		MocapMod.LOGGER.error(Utils.stringFromComponent(component, args));
	}

	@Override public void sendFailureWithTip(String component, Object... args)
	{
		MocapMod.LOGGER.error(Utils.stringFromComponent(component, args));
	}

	@Override public void sendException(Exception exception, String component, Object... args)
	{
		Utils.exception(exception, Utils.stringFromComponent(component, args));
	}
}
