package de.erdbeerbaerlp.dcintegration.common.storage;

public class PlayerSettings {
    public boolean useDiscordNameInChannel = Configuration.instance().linking.personalSettingsDefaults.default_useDiscordNameInChannel;
    public boolean ignoreDiscordChatIngame = false;
    public boolean ignoreReactions = Configuration.instance().linking.personalSettingsDefaults.default_ignoreReactions;
    public boolean pingSound = Configuration.instance().linking.personalSettingsDefaults.default_pingSound;

    /**
     * Class used for key descriptions using reflection
     */
    @SuppressWarnings("unused")
    public static final class Descriptions {
        private final String useDiscordNameInChannel = Configuration.instance().localization.personalSettings.descriptons.useDiscordNameInChannel;
        private final String ignoreDiscordChatIngame = Configuration.instance().localization.personalSettings.descriptons.ignoreDiscordChatIngame;
        private final String ignoreReactions = Configuration.instance().localization.personalSettings.descriptons.ignoreReactions;
        private final String pingSound = Configuration.instance().localization.personalSettings.descriptons.pingSound;
    }
}
