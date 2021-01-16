package de.erdbeerbaerlp.dcintegration.common.addon;

import com.moandjiezana.toml.TomlIgnore;

import java.io.File;

public abstract class AddonConfiguration {
    @TomlIgnore
    private File configFile = null;

    public File getConfigFile() {
        return configFile;
    }
    void setConfigFile(File file){
        configFile = file;
    }


}
