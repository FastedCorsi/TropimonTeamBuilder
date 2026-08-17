package fr.tropimon.teamsaver.client;

import fr.tropimon.teamsaver.TeamJson;
import fr.tropimon.teamsaver.model.TeamModels;
import fr.tropimon.teamsaver.model.TeamModels.PlayerData;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.WorldSavePath;

final class LocalTeamRepository {
    private static final Path ROOT = FabricLoader.getInstance().getConfigDir().resolve("tropimon-team-saver");
    private static final String PLAYERS_DIRECTORY = "players";
    private static final String RETURN_SLOTS_DIRECTORY = "return-slots";

    PlayerData load(MinecraftClient client) {
        Path globalFile = globalFile(client);
        if (globalFile == null) return new PlayerData();

        try {
            PlayerData data;
            if (Files.exists(globalFile)) {
                data = readPlayerData(globalFile);
            } else {
                data = migrateLegacyData(client);
            }
            data.returnSlots = loadReturnSlots(client);
            data.normalize();
            return data;
        } catch (Exception exception) {
            TropimonTeamSaverClient.LOGGER.error("Impossible de lire les teams interserveur", exception);
            return new PlayerData();
        }
    }

    boolean save(MinecraftClient client, PlayerData data) {
        Path globalFile = globalFile(client);
        Path returnFile = returnSlotsFile(client);
        if (globalFile == null || returnFile == null) return false;

        try {
            data.normalize();

            PlayerData globalData = new PlayerData();
            globalData.teams = data.teams;
            globalData.returnSlots.clear();
            writeJsonAtomically(globalFile, globalData);

            ReturnSlotData positions = new ReturnSlotData();
            positions.returnSlots.putAll(data.returnSlots);
            writeJsonAtomically(returnFile, positions);
            return true;
        } catch (Exception exception) {
            TropimonTeamSaverClient.LOGGER.error("Impossible de sauvegarder les teams interserveur", exception);
            return false;
        }
    }

    Path file(MinecraftClient client) {
        return globalFile(client);
    }

    private PlayerData migrateLegacyData(MinecraftClient client) throws Exception {
        String playerId = client.player.getUuidAsString();
        String currentServerKey = serverKey(client);
        List<PlayerData> legacySources = new ArrayList<>();

        for (Path legacyFile : legacyFiles(client, playerId)) {
            PlayerData legacy;
            try {
                legacy = readPlayerData(legacyFile);
            } catch (Exception exception) {
                TropimonTeamSaverClient.LOGGER.warn("Sauvegarde historique ignorée pendant la migration: {}",
                        legacyFile, exception);
                continue;
            }

            legacySources.add(legacy);

            String legacyServerKey = legacyFile.getParent().getFileName().toString();
            saveMigratedReturnSlots(client, legacyServerKey, legacy.returnSlots);
            if (legacyFile.equals(legacyCurrentServerFile(client))
                    || legacyFile.equals(legacySingleplayerFile(client))) {
                saveMigratedReturnSlots(client, currentServerKey, legacy.returnSlots);
            }
        }

        PlayerData merged = TeamModels.mergeTeamLists(legacySources);
        writeJsonAtomically(globalFile(client), merged);
        return merged;
    }

    private List<Path> legacyFiles(MinecraftClient client, String playerId) throws Exception {
        if (!Files.isDirectory(ROOT)) return List.of();
        List<Path> files = new ArrayList<>();
        Path current = legacyCurrentServerFile(client);
        Path oldSingleplayer = legacySingleplayerFile(client);
        if (current != null && Files.isRegularFile(current)) files.add(current);
        if (oldSingleplayer != null && Files.isRegularFile(oldSingleplayer) && !files.contains(oldSingleplayer)) {
            files.add(oldSingleplayer);
        }

        try (var directories = Files.list(ROOT)) {
            directories.filter(Files::isDirectory)
                    .filter(path -> !path.getFileName().toString().equals(PLAYERS_DIRECTORY))
                    .filter(path -> !path.getFileName().toString().equals(RETURN_SLOTS_DIRECTORY))
                    .map(path -> path.resolve(playerId + ".json"))
                    .filter(Files::isRegularFile)
                    .filter(path -> !files.contains(path))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(files::add);
        }
        return files;
    }

    private LinkedHashMap<String, String> loadReturnSlots(MinecraftClient client) throws Exception {
        Path returnFile = returnSlotsFile(client);
        if (returnFile != null && Files.exists(returnFile)) return readReturnSlots(returnFile);

        Path legacy = legacyCurrentServerFile(client);
        if (legacy == null || !Files.exists(legacy)) legacy = legacySingleplayerFile(client);
        if (legacy != null && Files.exists(legacy)) {
            LinkedHashMap<String, String> positions = readPlayerData(legacy).returnSlots;
            saveMigratedReturnSlots(client, serverKey(client), positions);
            return positions;
        }
        return new LinkedHashMap<>();
    }

    private PlayerData readPlayerData(Path file) throws Exception {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            PlayerData data = TeamJson.GSON.fromJson(reader, PlayerData.class);
            if (data == null) data = new PlayerData();
            data.normalize();
            return data;
        }
    }

    private LinkedHashMap<String, String> readReturnSlots(Path file) throws Exception {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            ReturnSlotData positions = TeamJson.GSON.fromJson(reader, ReturnSlotData.class);
            if (positions == null || positions.returnSlots == null) return new LinkedHashMap<>();
            PlayerData normalized = new PlayerData();
            normalized.returnSlots.putAll(positions.returnSlots);
            normalized.normalize();
            return normalized.returnSlots;
        }
    }

    private void saveMigratedReturnSlots(MinecraftClient client, String key,
                                         LinkedHashMap<String, String> returnSlots) throws Exception {
        String playerId = playerKey(client);
        if (playerId == null || returnSlots == null || returnSlots.isEmpty()) return;
        Path file = ROOT.resolve(RETURN_SLOTS_DIRECTORY).resolve(key)
                .resolve(playerId + ".json");
        if (Files.exists(file)) return;
        ReturnSlotData positions = new ReturnSlotData();
        positions.returnSlots.putAll(returnSlots);
        writeJsonAtomically(file, positions);
    }

    private void writeJsonAtomically(Path file, Object value) throws Exception {
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            TeamJson.GSON.toJson(value, writer);
        }
        try {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception unsupportedAtomicMove) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path globalFile(MinecraftClient client) {
        String playerId = playerKey(client);
        if (playerId == null) return null;
        return ROOT.resolve(PLAYERS_DIRECTORY).resolve(playerId + ".json");
    }

    private Path returnSlotsFile(MinecraftClient client) {
        String playerId = playerKey(client);
        if (playerId == null) return null;
        return ROOT.resolve(RETURN_SLOTS_DIRECTORY).resolve(serverKey(client))
                .resolve(playerId + ".json");
    }

    private Path legacyCurrentServerFile(MinecraftClient client) {
        if (client.player == null) return null;
        return ROOT.resolve(serverKey(client)).resolve(client.player.getUuidAsString() + ".json");
    }

    private String serverKey(MinecraftClient client) {
        String source;
        if (client.getCurrentServerEntry() != null) source = client.getCurrentServerEntry().address;
        else if (client.isInSingleplayer() && client.getServer() != null) {
            source = "singleplayer:" + client.getServer().getSavePath(WorldSavePath.ROOT)
                    .toAbsolutePath().normalize();
        } else if (client.isInSingleplayer()) source = "singleplayer:unknown";
        else source = "local";
        return hashKey(source);
    }

    private Path legacySingleplayerFile(MinecraftClient client) {
        if (!client.isInSingleplayer() || client.player == null) return null;
        return ROOT.resolve(hashKey("singleplayer")).resolve(client.player.getUuidAsString() + ".json");
    }

    private String playerKey(MinecraftClient client) {
        if (client.getSession().getUuidOrNull() != null) {
            return client.getSession().getUuidOrNull().toString();
        }
        return client.player == null ? null : client.player.getUuidAsString();
    }

    private String hashKey(String source) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (Exception ignored) {
            return Integer.toHexString(source.hashCode());
        }
    }

    private static final class ReturnSlotData {
        LinkedHashMap<String, String> returnSlots = new LinkedHashMap<>();
    }
}
