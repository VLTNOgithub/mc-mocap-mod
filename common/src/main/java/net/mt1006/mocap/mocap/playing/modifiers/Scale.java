package net.mt1006.mocap.mocap.playing.modifiers;

import net.mt1006.mocap.mocap.files.SceneFiles;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.Nullable;

public class Scale
{
	public static final Scale NORMAL = new Scale(1.0, 1.0);
	private static final @Nullable ResourceLocation SCALE_ID = BuiltInRegistries.ATTRIBUTE.getKey(Attributes.SCALE.value());
	public final double playerScale;
	public final double sceneScale;

	public Scale(@Nullable SceneFiles.Reader reader)
	{
		if (reader == null)
		{
			playerScale = 1.0;
			sceneScale = 1.0;
			return;
		}

		playerScale = reader.readDouble("player_scale", 1.0);
		sceneScale = reader.readDouble("scene_scale", 1.0);
	}

	private Scale(double playerScale, double sceneScale)
	{
		this.playerScale = playerScale;
		this.sceneScale = sceneScale;
	}

	public Scale ofPlayer(double scale)
	{
		return new Scale(scale, sceneScale);
	}

	public Scale ofScene(double scale)
	{
		return new Scale(playerScale, scale);
	}

	public @Nullable SceneFiles.Writer save()
	{
		if (isNormal()) { return null; }

		SceneFiles.Writer writer = new SceneFiles.Writer();
		writer.addDouble("player_scale", playerScale, 1.0);
		writer.addDouble("scene_scale", sceneScale, 1.0);

		return writer;
	}

	public boolean isNormal()
	{
		return (playerScale == 1.0 && sceneScale == 1.0);
	}

	public Scale mergeWithParent(Scale parent)
	{
		return new Scale(playerScale * parent.playerScale, sceneScale * parent.sceneScale);
	}

	public void applyToPlayer(Entity player)
	{
		applyToEntity(player, playerScale * sceneScale);
	}

	public void applyToEntity(Entity entity)
	{
		applyToEntity(entity, sceneScale);
	}

	private void applyToEntity(Entity entity, double scale)
	{
		if (scale == 1.0 || !(entity instanceof LivingEntity) || SCALE_ID == null) { return; }

		AttributeModifier modifier = new AttributeModifier(SCALE_ID, scale - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		AttributeInstance instance = ((LivingEntity)entity).getAttributes().getInstance(Attributes.SCALE);
		if (instance != null) { instance.addPermanentModifier(modifier); }
	}
}
