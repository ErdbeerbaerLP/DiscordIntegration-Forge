package de.erdbeerbaerlp.dcintegration.forge.mixin;

import com.mojang.authlib.GameProfile;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
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
public class MixinPlayerLogin {
    @Inject(method = "canPlayerLogin", at = @At("HEAD"), cancellable = true)
    private void canLogin(SocketAddress address, GameProfile profile, CallbackInfoReturnable<ITextComponent> cir) {
        if (Configuration.instance().linking.whitelistMode && ServerLifecycleHooks.getCurrentServer().usesAuthentication()) {
            LinkManager.checkGlobalAPI(profile.getId());
            try {
                if (!LinkManager.isPlayerLinked(profile.getId())) {
                    cir.setReturnValue(new StringTextComponent(Localization.instance().linking.notWhitelistedCode.replace("%code%",""+LinkManager.genLinkNumber(profile.getId()))));
                }else if(!DiscordIntegration.INSTANCE.canPlayerJoin(profile.getId())){
                    cir.setReturnValue(new StringTextComponent(Localization.instance().linking.notWhitelistedRole));
                }
            } catch (IllegalStateException e) {
                cir.setReturnValue(new StringTextComponent("An error occured\nPlease check Server Log for more information\n\n" + e));
            }
        }
    }
}
