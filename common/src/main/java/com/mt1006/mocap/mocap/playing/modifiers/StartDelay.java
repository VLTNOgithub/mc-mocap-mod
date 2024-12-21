package com.mt1006.mocap.mocap.playing.modifiers;

public class StartDelay
{
	public static final StartDelay ZERO = new StartDelay(0.0);
	public final double seconds;
	public final int ticks;

	private StartDelay(double seconds)
	{
		this.seconds = seconds;
		this.ticks = (int)Math.round(seconds * 20.0);
	}

	public static StartDelay fromSeconds(double seconds)
	{
		return seconds != 0.0 ? new StartDelay(seconds) : ZERO;
	}

	public StartDelay add(StartDelay delay)
	{
		return fromSeconds(seconds + delay.seconds);
	}
}
