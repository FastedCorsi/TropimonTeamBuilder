package fr.tropimon.teamsaver.client;

import com.google.gson.JsonElement;
import fr.tropimon.teamsaver.TeamJson;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.loader.api.FabricLoader;

/** Cached catalogue of coherent competitive sets published by Pokémon Showdown for generation 9. */
final class ShowdownGen9SetService {
    static final ShowdownGen9SetService INSTANCE = new ShowdownGen9SetService();
    private static final String BASE_URL = "https://play.pokemonshowdown.com/data/sets/";
    private static final List<String> FORMATS = List.of(
            "gen9ou", "gen9uu", "gen9ru", "gen9nu", "gen9pu", "gen9zu", "gen9ubers", "gen9nationaldex");
    private static final long FRESH_MS = Duration.ofHours(24).toMillis();
    private static final long STALE_MS = Duration.ofDays(30).toMillis();
    private static final int CACHE_SCHEMA = 2;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private Map<String, List<CompetitiveSet>> sets = Map.of();
    private boolean loaded;

    private ShowdownGen9SetService() {
    }

    synchronized void ensureLoaded() {
        if (loaded) return;
        CacheFile disk = readDisk();
        if (disk != null && disk.schemaVersion != CACHE_SCHEMA) disk = null;
        long now = System.currentTimeMillis();
        if (disk != null && disk.sets != null && now - disk.savedAt <= FRESH_MS) {
            sets = immutable(disk.sets);
            loaded = true;
            return;
        }
        try {
            Map<String, List<CompetitiveSet>> online = fetchAll();
            if (!online.isEmpty()) {
                sets = immutable(online);
                loaded = true;
                writeDisk(new CacheFile(CACHE_SCHEMA, now, sets));
                return;
            }
        } catch (RuntimeException exception) {
            TropimonTeamSaverClient.LOGGER.warn("Catalogue Pokémon Showdown Gen 9 indisponible", exception);
        }
        if (disk != null && disk.sets != null && now - disk.savedAt <= STALE_MS) sets = immutable(disk.sets);
        loaded = true;
    }

    CompetitiveSet bestSet(String showdownId, RankedUsageService.BuildStyle style, String archetype) {
        ensureLoaded();
        List<CompetitiveSet> choices = sets.get(RankedUsageService.key(showdownId));
        if (choices == null || choices.isEmpty()) return null;
        CompetitiveSet best = null;
        int bestScore = Integer.MIN_VALUE;
        for (CompetitiveSet choice : choices) {
            int score = choice.score(style, archetype);
            if (score > bestScore) {
                best = choice;
                bestScore = score;
            }
        }
        return best == null ? null : best.withScore(bestScore);
    }

    private Map<String, List<CompetitiveSet>> fetchAll() {
        List<CompletableFuture<FormatResult>> requests = new ArrayList<>();
        for (String format : FORMATS) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + format + ".json"))
                    .timeout(Duration.ofSeconds(10)).header("Accept", "application/json")
                    .header("User-Agent", "TropimonTeamBuilder").GET().build();
            requests.add(http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenApply(response -> response.statusCode() >= 200 && response.statusCode() < 300
                            ? new FormatResult(format, response.body()) : null)
                    .exceptionally(error -> null));
        }
        CompletableFuture.allOf(requests.toArray(CompletableFuture[]::new)).join();
        Map<String, List<CompetitiveSet>> result = new LinkedHashMap<>();
        for (CompletableFuture<FormatResult> request : requests) {
            FormatResult response = request.join();
            if (response != null) merge(result, response.format, response.body);
        }
        return result;
    }

    private void merge(Map<String, List<CompetitiveSet>> target, String format, String body) {
        Payload payload = TeamJson.GSON.fromJson(body, Payload.class);
        if (payload == null || payload.dex == null) return;
        payload.dex.forEach((species, namedSets) -> {
            String speciesKey = RankedUsageService.key(species);
            if (speciesKey.isBlank() || namedSets == null) return;
            namedSets.forEach((name, raw) -> {
                if (raw == null) return;
                CompetitiveSet set = new CompetitiveSet(speciesKey, format, name,
                        firstString(raw.ability), firstString(raw.item), firstString(raw.nature),
                        allStrings(raw.moves), raw.evs == null ? Map.of() : Map.copyOf(raw.evs), 0);
                if (set.moves.isEmpty()) return;
                List<CompetitiveSet> choices = target.computeIfAbsent(speciesKey, ignored -> new ArrayList<>());
                String signature = set.signature();
                if (choices.stream().noneMatch(existing -> existing.signature().equals(signature))) choices.add(set);
            });
        });
    }

    private static String firstString(JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        if (element.isJsonPrimitive()) return element.getAsString();
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                String value = firstString(child);
                if (value != null && !value.isBlank()) return value;
            }
        }
        return null;
    }

    private static List<String> allStrings(JsonElement element) {
        Set<String> result = new LinkedHashSet<>();
        if (element != null && element.isJsonArray()) {
            for (JsonElement slot : element.getAsJsonArray()) {
                String selected = firstString(slot);
                if (selected != null && !selected.isBlank()) result.add(selected);
            }
        } else {
            String selected = firstString(element);
            if (selected != null && !selected.isBlank()) result.add(selected);
        }
        return List.copyOf(result);
    }

    private CacheFile readDisk() {
        Path path = cachePath();
        if (!Files.isRegularFile(path)) return null;
        try {
            return TeamJson.GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), CacheFile.class);
        } catch (IOException | RuntimeException exception) {
            TropimonTeamSaverClient.LOGGER.debug("Cache Showdown Gen 9 local ignoré", exception);
            return null;
        }
    }

    private void writeDisk(CacheFile cache) {
        Path path = cachePath();
        try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temporary, TeamJson.GSON.toJson(cache), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException unsupportedAtomicMove) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            TropimonTeamSaverClient.LOGGER.warn("Impossible d'écrire le cache Showdown Gen 9", exception);
        }
    }

    private Path cachePath() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("tropimon-team-manager").resolve("showdown-gen9-cache.json");
    }

    private static Map<String, List<CompetitiveSet>> immutable(Map<String, List<CompetitiveSet>> source) {
        Map<String, List<CompetitiveSet>> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, value == null ? List.of() : List.copyOf(value)));
        return Map.copyOf(copy);
    }

    static final class CompetitiveSet {
        String speciesKey;
        String format;
        String name;
        String ability;
        String item;
        String nature;
        List<String> moves = List.of();
        Map<String, Integer> evs = Map.of();
        int matchScore;

        CompetitiveSet() {
        }

        CompetitiveSet(String speciesKey, String format, String name, String ability, String item,
                       String nature, List<String> moves, Map<String, Integer> evs, int matchScore) {
            this.speciesKey = speciesKey;
            this.format = format;
            this.name = name;
            this.ability = ability;
            this.item = item;
            this.nature = nature;
            this.moves = moves == null ? List.of() : List.copyOf(moves);
            this.evs = evs == null ? Map.of() : Map.copyOf(evs);
            this.matchScore = matchScore;
        }

        CompetitiveSet withScore(int score) {
            return new CompetitiveSet(speciesKey, format, name, ability, item, nature, moves, evs, score);
        }

        private String signature() {
            return RankedUsageService.key(name) + '|' + RankedUsageService.key(item) + '|' + moves;
        }

        int score(RankedUsageService.BuildStyle style, String archetype) {
            String label = RankedUsageService.key(name);
            int score = complete() ? 12 : 0;
            if (style == null) style = RankedUsageService.BuildStyle.BALANCED;
            score += switch (style) {
                case AURORA_VEIL -> containsAny(label, "auroraveil") || hasMove("auroraveil") ? 180 : 0;
                case SNOW -> containsAny(label, "snow", "hail", "blizzard")
                        || RankedUsageService.isStyleAbility(ability, style) ? 90 : 0;
                case RAIN -> containsAny(label, "rain") || RankedUsageService.isStyleAbility(ability, style) ? 90 : 0;
                case SUN -> containsAny(label, "sun") || RankedUsageService.isStyleAbility(ability, style) ? 90 : 0;
                case SAND -> containsAny(label, "sand") || RankedUsageService.isStyleAbility(ability, style) ? 90 : 0;
                case TRICK_ROOM -> containsAny(label, "trickroom") || hasMove("trickroom") ? 120 : 0;
                case STALL -> containsAny(label, "defensive", "utility", "wall", "support", "unaware") ? 70 : 0;
                case OFFENSE -> containsAny(label, "sweeper", "attacker", "wallbreaker", "choice", "offensive")
                        ? 65 : 0;
                case BULKY_OFFENSE -> containsAny(label, "bulkyattacker", "bulkysetup", "pivot") ? 70 : 0;
                case BALANCED, MONOTYPE -> containsAny(label, "utility", "pivot", "support") ? 35 : 0;
            };
            String role = RankedUsageService.key(archetype);
            if (role.equals("bulky") && containsAny(label, "bulky", "defensive", "utility", "support")) score += 25;
            if (role.equals("physical") && evs.getOrDefault("atk", 0) > evs.getOrDefault("spa", 0)) score += 20;
            if (role.equals("special") && evs.getOrDefault("spa", 0) > evs.getOrDefault("atk", 0)) score += 20;
            if (role.equals("fastoffense") && evs.getOrDefault("spe", 0) >= 200) score += 18;
            return score;
        }

        private boolean complete() {
            return ability != null && item != null && nature != null && moves.size() >= 4 && !evs.isEmpty();
        }

        private boolean hasMove(String expected) {
            for (String move : moves) if (RankedUsageService.key(move).equals(expected)) return true;
            return false;
        }

        private static boolean containsAny(String value, String... expected) {
            for (String candidate : expected) if (value.contains(candidate)) return true;
            return false;
        }
    }

    private static final class Payload {
        Map<String, Map<String, RawSet>> dex;
    }

    private static final class RawSet {
        JsonElement moves;
        JsonElement ability;
        JsonElement item;
        JsonElement nature;
        Map<String, Integer> evs;
    }

    private static final class CacheFile {
        int schemaVersion;
        long savedAt;
        Map<String, List<CompetitiveSet>> sets;

        CacheFile() {
        }

        CacheFile(int schemaVersion, long savedAt, Map<String, List<CompetitiveSet>> sets) {
            this.schemaVersion = schemaVersion;
            this.savedAt = savedAt;
            this.sets = sets;
        }
    }

    private record FormatResult(String format, String body) {
    }
}
