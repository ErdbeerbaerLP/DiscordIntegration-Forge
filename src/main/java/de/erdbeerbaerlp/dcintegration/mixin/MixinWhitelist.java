package de.erdbeerbaerlp.dcintegration.mixin;

import com.mojang.authlib.GameProfile;
import de.erdbeerbaerlp.dcintegration.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.storage.PlayerLinks;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

@Mixin(PlayerList.class)
public class MixinWhitelist {
    @Inject(method = "canPlayerLogin", at = @At("HEAD"), cancellable = true)
    private void canLogin(SocketAddress address, GameProfile profile, CallbackInfoReturnable<ITextComponent> cir) {
        if (Configuration.INSTANCE.whitelist.get()) {
            try {
                if (!PlayerLinks.isPlayerLinked(profile.getId())) {
                    cir.setReturnValue(new StringTextComponent(Configuration.INSTANCE.msgNotWhitelisted.get()));
                }
            } catch (IllegalStateException e) {
                cir.setReturnValue(new StringTextComponent("Please check linkedPlayers.json\n\n" + e.toString()));
            }
        }
    }
}
