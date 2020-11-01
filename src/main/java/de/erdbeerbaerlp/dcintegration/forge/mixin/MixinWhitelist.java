package de.erdbeerbaerlp.dcintegration.forge.mixin;

import com.mojang.authlib.GameProfile;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

@Mixin(PlayerList.class)
public class MixinWhitelist {
    @Inject(method = "canPlayerLogin", at = @At("HEAD"), cancellable = true)
    private void canLogin(SocketAddress address, GameProfile profile, CallbackInfoReturnable<ITextComponent> cir) {
        if (Configuration.instance().linking.whitelistMode && ServerLifecycleHooks.getCurrentServer().isServerInOnlineMode()) {
            try {
                if (!PlayerLinkController.isPlayerLinked(profile.getId())) {
                    cir.setReturnValue(new StringTextComponent(Configuration.instance().localization.notWhitelisted));
                }
            } catch (IllegalStateException e) {
                cir.setReturnValue(new StringTextComponent("Please check LinkedPlayers.json\n\n" + e.toString()));
            }
        }
    }
}
