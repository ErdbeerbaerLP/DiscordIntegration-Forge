package de.erdbeerbaerlp.dcintegration.storage;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import javax.management.openmbean.KeyAlreadyExistsException;
import java.io.*;
import java.util.UUID;

public class PlayerLinks {
    private static final File playerLinkedFile = new File("./linkedPlayers.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final JsonParser parser = new JsonParser();

    public static boolean isPlayerLinked(UUID player) throws IllegalStateException {
        try {
            for (JsonElement e : getJson()) {
                final JsonObject o = e.getAsJsonObject();
                if (o.has("minecraft") && o.get("minecraft").getAsString().equals(player.toString())) {
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
                final JsonObject o = e.getAsJsonObject();
                if (o.has("discord") && o.get("discord").getAsString().equals(discordID)) {
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
                final JsonObject o = e.getAsJsonObject();
                if (o.has("discord") && o.has("minecraft") && o.get("discord").getAsString().equals(discordID)) {
                    return UUID.fromString(o.get("minecraft").getAsString());
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
                final JsonObject o = e.getAsJsonObject();
                if (o.has("discord") && o.has("minecraft") && o.get("minecraft").getAsString().equals(player.toString())) {
                    return o.get("discord").getAsString();
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
            if (PlayerLinks.getNameFromUUID(player) == null) return false;
            final JsonArray a = getJson();
            System.out.println(a);
            final JsonObject link = new JsonObject();
            link.addProperty("discord", discordID);
            link.addProperty("minecraft", player.toString());
            a.add(link);
            System.out.println(a);
            try (Writer writer = new FileWriter(playerLinkedFile)) {
                gson.toJson(a, writer);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean unlinkPlayer(String discordID, UUID player) {
        if (!isDiscordLinked(discordID) && !isPlayerLinked(player)) return false;
        try {
            for (JsonElement e : getJson()) {
                final JsonObject o = e.getAsJsonObject();
                if (o.has("discord") && o.get("discord").getAsString().equals(discordID)) {
                    final JsonArray json = getJson();
                    json.remove(o);
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
