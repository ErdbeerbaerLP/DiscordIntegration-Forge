package de.erdbeerbaerlp.dcintegration.forge;

import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

public class DCMixinConnector implements IMixinConnector {
    /**
     * Connect to Mixin
     */
    @Override
    public void connect() {
        Mixins.addConfiguration("mixins.dcintegration.json");
    }
}
