package com.mt1006.mocap.command.io;

import com.mt1006.mocap.MocapMod;
import com.mt1006.mocap.utils.Utils;
import net.minecraft.network.chat.Component;

class LogsCommandOutput implements CommandOutput
{
	@Override public void sendSuccess(String component, Object... args) {}
	@Override public void sendSuccessWithTip(String component, Object... args) {}
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
