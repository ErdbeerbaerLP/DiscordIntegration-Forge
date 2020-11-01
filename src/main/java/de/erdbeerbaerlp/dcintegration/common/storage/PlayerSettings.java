package de.erdbeerbaerlp.dcintegration.common.storage;

public class PlayerSettings {
    public boolean useDiscordNameInChannel = true;
    public boolean ignoreDiscordChatIngame = false;
    //public boolean useDiscordNameIngame = false;

    /**
     * Class used for key descriptions using reflection
     */
    @SuppressWarnings("unused")
    public static final class Descriptions {
        private final String useDiscordNameInChannel = "Should the bot send messages using your discord name and avatar instead";
        private final String ignoreDiscordChatIngame = "Configure if you want to ignore discord chat ingame";
        //private final String useDiscordNameIngame = "Should your Discord (nick-)name be shown instead of your IGN on the server?";
    }
}
