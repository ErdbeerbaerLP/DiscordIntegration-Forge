package de.erdbeerbaerlp.dcintegration.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

/**
 * USED TO PREVENT IDLE TIMEOUT, DEBUG STUFF
 */
@Mixin(MinecraftServer.class)
public abstract class MixinNoIdleTimeout {

    private static final File bypassIdleTimeout = new File("./config/dpito.dat");

    @Inject(method = "getMaxPlayerIdleMinutes", at = @At("RETURN"), cancellable = true)
    private void preventIdleTimeout(CallbackInfoReturnable<Integer> cir) {
        if (bypassIdleTimeout.exists()) {
            cir.setReturnValue(0);
        }
    }
}
