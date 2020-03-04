package de.erdbeerbaerlp.dcintegration.mixin;

import de.erdbeerbaerlp.dcintegration.DiscordIntegration;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin used to detect player timeouts
 */
@Mixin(value = ServerPlayNetHandler.class, priority = 1001)
public class MixinNetHandlerPlayServer {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "disconnect", at = @At("HEAD"))
    private void onDisconnect(final ITextComponent textComponent, CallbackInfo ci) {
        System.out.println(textComponent.getUnformattedComponentText());
        if (textComponent.equals(new TranslationTextComponent("disconnect.timeout")))
            DiscordIntegration.timeouts.add(this.player.getUniqueID());
    }
}