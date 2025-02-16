package com.mt1006.mocap.command.io;

import net.minecraft.network.chat.Component;

public interface CommandOutput
{
	CommandOutput DUMMY = new DummyCommandOutput();
	CommandOutput LOGS = new LogsCommandOutput();

	void sendSuccess(String component, Object... args);
	void sendSuccessLiteral(String format, Object... args);
	void sendSuccessComponent(Component component);
	void sendFailure(String component, Object... args);
	void sendFailureWithTip(String component, Object... args);
	void sendException(Exception exception, String component, Object... args);
}