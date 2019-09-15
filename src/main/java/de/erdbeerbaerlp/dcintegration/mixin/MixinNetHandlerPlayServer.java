package de.erdbeerbaerlp.dcintegration.mixin;
/* TODO Wait for mixin 1.14
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.erdbeerbaerlp.dcintegration.DiscordIntegration;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * Mixin used to detect player timeouts
 *//*
@Mixin(value=NetHandlerPlayServer.class, priority = 1001)
public abstract class MixinNetHandlerPlayServer{
	@Shadow
	public EntityPlayerMP player;
	@Inject(method = "disconnect", at = @At("HEAD"))
	private void onDisconnect(final ITextComponent textComponent, CallbackInfo ci) {
		
		if(textComponent.equals(new TextComponentTranslation("disconnect.timeout", new Object[0])))
			DiscordIntegration.lastTimeout = this.player;
	}

	
}
*/