package de.erdbeerbaerlp.dcintegration.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.erdbeerbaerlp.dcintegration.ModClass;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
@Mixin(NetHandlerPlayServer.class)
public class MixinNetHandlerPlayServer{
	@Shadow
	public EntityPlayerMP player;
	@Inject(method = "disconnect", at = @At("HEAD"))
	private void onDisconnect(CallbackInfo ci) {
		ModClass.lastTimeout = this.player;
	}

	
}
