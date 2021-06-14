package de.erdbeerbaerlp.dcintegration.forge.mixin;


import de.erdbeerbaerlp.dcintegration.forge.DiscordIntegration;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/**
 * Mixin used to detect player timeouts
 */
@Mixin(value = NetHandlerPlayServer.class, priority = 1001)
public abstract class MixinNetHandlerPlayServer
{
    @Shadow
    public EntityPlayerMP player;

    @Inject(method = "disconnect", at = @At("HEAD"))
    private void onDisconnect(final ITextComponent textComponent, CallbackInfo ci) {
        if (textComponent.equals(new TextComponentTranslation("disconnect.timeout"))) DiscordIntegration.timeouts.add(this.player.getUniqueID());
    }


}