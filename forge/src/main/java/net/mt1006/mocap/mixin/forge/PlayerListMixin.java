package net.mt1006.mocap.mixin.forge;

import net.mt1006.mocap.events.EntityEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
public class PlayerListMixin
{
	@Inject(method = "respawn", at = @At(value = "TAIL"))
	private void atRespawn(ServerPlayer player, boolean keepInventory, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir)
	{
		EntityEvent.onPlayerRespawn(player, cir.getReturnValue());
	}
}
