package de.erdbeerbaerlp.dcintegration.storage;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import javax.management.openmbean.KeyAlreadyExistsException;
import java.io.*;
import java.util.UUID;

public class PlayerLinkController {
    private static final File playerLinkedFile = new File("./linkedPlayers.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final JsonParser parser = new JsonParser();

    public static boolean isPlayerLinked(UUID player) throws IllegalStateException {
        try {
            for (JsonElement e : getJson()) {
                final PlayerLink o = gson.fromJson(e, PlayerLink.class);
                if (!o.mcPlayerUUID.isEmpty() && o.mcPlayerUUID.equals(player.toString())) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isDiscordLinked(String discordID) {
        try {
            for (JsonElement e : getJson()) {
                final PlayerLink o = gson.fromJson(e, PlayerLink.class);
                if (!o.discordID.isEmpty() && o.discordID.equals(discordID)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Nullable
    public static UUID getPlayerFromDiscord(String discordID) {
        try {
            for (JsonElement e : getJson()) {
                final PlayerLink o = gson.fromJson(e, PlayerLink.class);
                if (!o.discordID.isEmpty() && !o.mcPlayerUUID.isEmpty() && o.discordID.equals(discordID)) {
                    return UUID.fromString(o.mcPlayerUUID);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    public static String getDiscordFromPlayer(UUID player) {
        try {
            for (JsonElement e : getJson()) {
                final PlayerLink o = gson.fromJson(e, PlayerLink.class);
                if (!o.discordID.isEmpty() && !o.mcPlayerUUID.isEmpty() && o.mcPlayerUUID.equals(player.toString())) {
                    return o.discordID;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static PlayerSettings getSettings(String discordID, UUID player) {
        if (player == null && discordID == null) throw new NullPointerException();
        else if (discordID == null) discordID = getDiscordFromPlayer(player);
        else if (player == null) player = getPlayerFromDiscord(discordID);
        if (player == null || discordID == null) throw new NullPointerException();
        try {
            for (JsonElement e : getJson()) {
                final PlayerLink o = gson.fromJson(e, PlayerLink.class);
                if (!o.discordID.isEmpty() && !o.mcPlayerUUID.isEmpty() && o.discordID.equals(discordID) && o.mcPlayerUUID.equals(player.toString())) {
                    return o.settings;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean linkPlayer(String discordID, UUID player) throws KeyAlreadyExistsException {
        if (isDiscordLinked(discordID) || isPlayerLinked(player)) throw new KeyAlreadyExistsException();
        try {
            if (PlayerLinkController.getNameFromUUID(player) == null) return false;
            final JsonArray a = getJson();
            final PlayerLink link = new PlayerLink();
            link.discordID = discordID;
            link.mcPlayerUUID = player.toString();
            a.add(gson.toJsonTree(link));
            saveJSON(a);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean updatePlayerSettings(String discordID, UUID player, PlayerSettings s) {
        if (player == null && discordID == null) throw new NullPointerException();
        else if (discordID == null) discordID = getDiscordFromPlayer(player);
        else if (player == null) player = getPlayerFromDiscord(discordID);
        if (player == null || discordID == null) throw new NullPointerException();
        if (isDiscordLinked(discordID) && isPlayerLinked(player))
            try {
                final JsonArray a = getJson();
                final PlayerLink link = getUser(discordID, player);
                if (link == null) return false;
                a.remove(gson.toJsonTree(link));
                link.settings = s;
                a.add(gson.toJsonTree(link));
                saveJSON(a);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        return false;
    }

    private static void saveJSON(JsonArray a) throws IOException {
        try (Writer writer = new FileWriter(playerLinkedFile)) {
            gson.toJson(a, writer);
        }
    }

    public static boolean unlinkPlayer(String discordID, UUID player) {
        if (!isDiscordLinked(discordID) && !isPlayerLinked(player)) return false;
        try {
            for (JsonElement e : getJson()) {
                final PlayerLink o = gson.fromJson(e, PlayerLink.class);
                if (o.discordID != null && o.discordID.equals(discordID)) {
                    final JsonArray json = getJson();
                    json.remove(e);
                    try (Writer writer = new FileWriter(playerLinkedFile)) {
                        gson.toJson(json, writer);
                    }
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static PlayerLink getUser(String discordID, UUID player) throws IOException {
        final JsonArray a = getJson();
        for (JsonElement e : a) {
            final PlayerLink l = gson.fromJson(e, PlayerLink.class);
            if (l.discordID.equals(discordID) && l.mcPlayerUUID.equals(player.toString()))
                return l;
        }
        return null;
    }

    private static JsonArray getJson() throws IOException, IllegalStateException {
        if (!playerLinkedFile.exists()) {
            playerLinkedFile.createNewFile();
            try (Writer writer = new FileWriter(playerLinkedFile)) {
                gson.toJson(new JsonArray(), writer);
            }
            return new JsonArray();
        }
        final FileReader is = new FileReader(playerLinkedFile);
        final JsonArray a = parser.parse(is).getAsJsonArray();
        is.close();
        return a;
    }

    @Nullable
    public static String getNameFromUUID(UUID uuid) {
        final String name = ServerLifecycleHooks.getCurrentServer().getMinecraftSessionService().fillProfileProperties(new GameProfile(uuid, ""), false).getName();
        return name.isEmpty() ? null : name;
    }

}
