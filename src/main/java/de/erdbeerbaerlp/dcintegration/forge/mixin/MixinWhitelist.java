package de.erdbeerbaerlp.dcintegration.forge.mixin;

import com.mojang.authlib.GameProfile;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import net.minecraft.server.management.PlayerList;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

@Mixin(PlayerList.class)
public class MixinWhitelist {
    @Inject(method = "allowUserToConnect", at = @At("HEAD"), cancellable = true)
    private void canLogin(SocketAddress address, GameProfile profile, CallbackInfoReturnable<String> cir) {
        if (Configuration.instance().linking.whitelistMode &&  FMLCommonHandler.instance().getMinecraftServerInstance().isServerInOnlineMode()) {
            try {
                if (!PlayerLinkController.isPlayerLinked(profile.getId())) {
                    cir.setReturnValue(Localization.instance().linking.notWhitelistedCode.replace("%code%",""+Variables.discord_instance.genLinkNumber(profile.getId())));
                }
            } catch (IllegalStateException e) {
                cir.setReturnValue("Please check " + Variables.discordDataDir + "LinkedPlayers.json\n\n" + e.toString());
            }
        }
    }
}
