package de.erdbeerbaerlp.dcintegration.common.discordCommands.inDMs;

import de.erdbeerbaerlp.dcintegration.common.discordCommands.inChat.DiscordCommand;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import net.dv8tion.jda.api.entities.User;

public abstract class DMCommand extends DiscordCommand {
    protected DMCommand() {
        super("000000000000000000000");
    }

    @Override
    public final boolean adminOnly() {
        return false;
    }

    public boolean requiresLink() {
        return true;
    }

    @Override
    public boolean canUserExecuteCommand(User user) {
        if (user == null) return false;
        return !this.requiresLink() || PlayerLinkController.isDiscordLinked(user.getId());
    }
}
