package com.mt1006.mocap.mocap.playing.modifiers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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

	public Scale(@Nullable JsonObject json)
	{
		if (json == null)
		{
			playerScale = 1.0;
			sceneScale = 1.0;
			return;
		}

		JsonElement playerScaleElement = json.get("player_scale");
		playerScale = playerScaleElement != null ? playerScaleElement.getAsDouble() : 1.0;

		JsonElement sceneScaleElement = json.get("scene_scale");
		sceneScale = sceneScaleElement != null ? sceneScaleElement.getAsDouble() : 1.0;
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

	public @Nullable JsonObject toJson()
	{
		if (isNormal()) { return null; }

		JsonObject json = new JsonObject();
		if (playerScale != 1.0) { json.add("player_scale", new JsonPrimitive(playerScale)); }
		if (sceneScale != 1.0) { json.add("scene_scale", new JsonPrimitive(sceneScale)); }

		return json;
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
