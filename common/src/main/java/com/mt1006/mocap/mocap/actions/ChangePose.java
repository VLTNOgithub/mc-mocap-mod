package com.mt1006.mocap.mocap.actions;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mt1006.mocap.mocap.files.RecordingFiles;
import com.mt1006.mocap.mocap.playing.playback.ActionContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;

import java.util.EnumMap;

public class ChangePose implements ComparableAction
{
	private static final BiMap<Integer, Pose> poseMap;
	private static final BiMap<Pose, Integer> poseIdMap;
	static
	{
		EnumMap<Pose, Integer> enumMap = new EnumMap<>(Pose.class);
		enumMap.put(Pose.STANDING, 1);
		enumMap.put(Pose.FALL_FLYING, 2);
		enumMap.put(Pose.SLEEPING, 3);
		enumMap.put(Pose.SWIMMING, 4);
		enumMap.put(Pose.SPIN_ATTACK, 5);
		enumMap.put(Pose.CROUCHING, 6);
		enumMap.put(Pose.DYING, 7);
		enumMap.put(Pose.LONG_JUMPING, 8);
		enumMap.put(Pose.CROAKING, 9);
		enumMap.put(Pose.USING_TONGUE, 10);
		enumMap.put(Pose.SITTING, 11);
		enumMap.put(Pose.ROARING, 12);
		enumMap.put(Pose.SNIFFING, 13);
		enumMap.put(Pose.EMERGING, 14);
		enumMap.put(Pose.DIGGING, 15);
		enumMap.put(Pose.SLIDING, 16);
		enumMap.put(Pose.SHOOTING, 17);
		enumMap.put(Pose.INHALING, 18);

		poseIdMap = HashBiMap.create(enumMap);
		poseMap = poseIdMap.inverse();
	}

	private final Pose pose;

	public ChangePose(Entity entity)
	{
		pose = entity.getPose();
	}

	public ChangePose(RecordingFiles.Reader reader)
	{
		pose = poseMap.getOrDefault(reader.readInt(), Pose.STANDING);
	}

	@Override public boolean differs(ComparableAction previousAction)
	{
		return pose != ((ChangePose)previousAction).pose;
	}

	@Override public void write(RecordingFiles.Writer writer)
	{
		writer.addByte(Type.CHANGE_POSE.id);
		writer.addInt(poseIdMap.getOrDefault(pose, 0));
	}

	@Override public Result execute(ActionContext ctx)
	{
		ctx.entity.setPose(pose);
		return Result.OK;
	}
}
