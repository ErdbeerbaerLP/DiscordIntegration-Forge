package de.erdbeerbaerlp.dcintegration.common.addon;

import de.erdbeerbaerlp.dcintegration.common.Discord;

public interface DiscordIntegrationAddon {

    /**
     * Gets called after loading an Addon<br>
     * Use it to register event handlers or semilar
     * @param dc {@link Discord} instance
     */
    void load(final Discord dc);

    /**
     * Gets called when Discord Integration is reloading by the /discord reload command. Can be used to reload configs
     */
    default void reload(){

    }
    /**
     * Gets called before unloading an Addon
     *
     * @param dc {@link Discord} instance
     */
    default void unload(final Discord dc) {
    }
}
