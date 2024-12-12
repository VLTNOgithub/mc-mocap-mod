package com.mt1006.mocap.events;

import com.mt1006.mocap.mocap.playing.Playing;
import com.mt1006.mocap.mocap.recording.Recording;

public class ServerTickEvent
{
	public static void onEndTick()
	{
		Recording.onTick();
		Playing.onTick();
	}
}
