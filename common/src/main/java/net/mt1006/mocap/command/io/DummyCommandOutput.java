package net.mt1006.mocap.command.io;

import net.minecraft.network.chat.Component;

class DummyCommandOutput implements CommandOutput
{
	@Override public void sendSuccess(String component, Object... args) {}
	@Override public void sendSuccessLiteral(String format, Object... args) {}
	@Override public void sendSuccessComponent(Component component) {}
	@Override public void sendFailure(String component, Object... args) {}
	@Override public void sendFailureWithTip(String component, Object... args) {}
	@Override public void sendException(Exception exception, String component, Object... args) {}
}
