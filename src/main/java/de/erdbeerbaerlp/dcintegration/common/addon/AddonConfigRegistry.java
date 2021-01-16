package de.erdbeerbaerlp.dcintegration.common.addon;


import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import de.erdbeerbaerlp.dcintegration.common.Discord;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class AddonConfigRegistry {

    /**
     * Loads all values from the config file
     * @return The config file with (re-)loaded values
     */
    @Nullable
    public static <T extends AddonConfiguration> T loadConfig(@Nonnull T cfg) {
        if (cfg.getConfigFile() == null) return null;
        if (!cfg.getConfigFile().exists()) {
            saveConfig(cfg);
            return cfg;
        }
        final File cfgFile = cfg.getConfigFile();
        final T conf = (T) new Toml().read(cfgFile).to(cfg.getClass());
        conf.setConfigFile(cfgFile);
        saveConfig(conf); //Re-write the config so new values get added after updates
        return conf;
    }

    /**
     * Saves all values to the config file
     */
    public static void saveConfig(@Nonnull AddonConfiguration cfg) {
        if (cfg.getConfigFile() == null) return;
        try {
            if (!cfg.getConfigFile().exists()) {
                if (!cfg.getConfigFile().getParentFile().exists()) cfg.getConfigFile().getParentFile().mkdirs();
                cfg.getConfigFile().createNewFile();
            }
            final TomlWriter w = new TomlWriter.Builder()
                    .indentValuesBy(2)
                    .indentTablesBy(4)
                    .padArrayDelimitersBy(2)
                    .build();
            w.write(cfg, cfg.getConfigFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates an Instance of your {@link AddonConfiguration} and loads it using {@link AddonConfigRegistry#loadConfig(AddonConfiguration)}<br>
     * Should be called in {@link DiscordIntegrationAddon#load(Discord)}
     * @param cfg  Class of your Configuration
     * @param inst Instance of your Addon
     * @return Configuration file, if existing
     */
    @Nullable
    public static <T extends AddonConfiguration> T registerConfig(@Nonnull Class<T> cfg, @Nonnull DiscordIntegrationAddon inst) {
        try {
            final T conf = cfg.getDeclaredConstructor().newInstance();
            conf.setConfigFile(new File(AddonLoader.getAddonDir(), AddonLoader.getAddonMeta(inst).getName() + ".toml"));
            return loadConfig(conf);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            System.err.println("An exception occurred while loading addon configuration " +cfg.getName() );
            e.printStackTrace();
        }
        return null;
    }
}
