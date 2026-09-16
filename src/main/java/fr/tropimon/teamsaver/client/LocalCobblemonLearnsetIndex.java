package fr.tropimon.teamsaver.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

/**
 * Restores the complete learnsets that are intentionally not synchronized by
 * Cobblemon to clients. Cobblemon 1.7 only sends level-up moves in its species
 * packet; the egg, tutor and TM lists still exist in the local mod data.
 */
final class LocalCobblemonLearnsetIndex {
    private static final String DEFAULT_FORM = "";

    private final Map<String, SpeciesLearnsets> species = new LinkedHashMap<>();

    static LocalCobblemonLearnsetIndex build() {
        LocalCobblemonLearnsetIndex index = new LocalCobblemonLearnsetIndex();
        // Only the official dependency. Compatibility learnsets belong to this mod,
        // so installing/removing another Tropimon mod cannot change our local catalogue.
        FabricLoader.getInstance().getModContainer("cobblemon").ifPresent(mod -> {
            for (Path root : mod.getRootPaths()) index.scanRoot(root);
        });
        try (var stream = LocalCobblemonLearnsetIndex.class.getResourceAsStream(
                "/assets/tropimon_team_saver/data/learnset_supplements.json")) {
            if (stream == null) throw new IOException("Missing bundled learnset supplements");
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject documents = JsonParser.parseReader(reader).getAsJsonObject();
                documents.entrySet().forEach(entry -> index.addDocument(entry.getKey(), entry.getValue().getAsJsonObject()));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read Team Builder learnsets", exception);
        }
        return index;
    }

    Set<String> moves(Identifier speciesId, String formName) {
        if (speciesId == null) return Set.of();
        SpeciesLearnsets learnsets = species.get(speciesId.toString().toLowerCase(Locale.ROOT));
        if (learnsets == null) return Set.of();
        String normalizedForm = normalizeForm(formName);
        Set<String> exact = normalizedForm.isEmpty() ? null : learnsets.forms.get(normalizedForm);
        if (exact != null && !exact.isEmpty()) return Set.copyOf(exact);
        return Set.copyOf(learnsets.base);
    }

    private void scanRoot(Path root) {
        Path data = root.resolve("data");
        if (!Files.isDirectory(data)) return;
        try (Stream<Path> files = Files.walk(data, 8)) {
            files.filter(Files::isRegularFile)
                    .filter(LocalCobblemonLearnsetIndex::isSpeciesJson)
                    .forEach(path -> readSpeciesFile(data, path));
        } catch (IOException exception) {
            TropimonTeamSaverClient.LOGGER.debug("Impossible de lire les learnsets locaux de {}", root, exception);
        }
    }

    private void readSpeciesFile(Path dataRoot, Path file) {
        Path relative = dataRoot.relativize(file);
        if (relative.getNameCount() < 4) return;
        String namespace = relative.getName(0).toString();
        String collection = relative.getName(1).toString();
        String filename = file.getFileName().toString();
        String fallbackPath = filename.substring(0, filename.length() - ".json".length());
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) return;
            JsonObject document = parsed.getAsJsonObject();
            String speciesId = collection.equals("species_additions")
                    ? string(document, "target") : namespace + ":" + fallbackPath;
            if (speciesId == null || Identifier.tryParse(speciesId) == null) return;
            addDocument(speciesId, document);
        } catch (Exception exception) {
            TropimonTeamSaverClient.LOGGER.debug("Learnset local ignoré: {}", file, exception);
        }
    }

    void addDocument(String speciesId, JsonObject document) {
        if (speciesId == null || document == null) return;
        SpeciesLearnsets learnsets = species.computeIfAbsent(
                speciesId.toLowerCase(Locale.ROOT), ignored -> new SpeciesLearnsets());
        addMoves(document.getAsJsonArray("moves"), learnsets.base);
        JsonArray forms = document.getAsJsonArray("forms");
        if (forms == null) return;
        for (JsonElement element : forms) {
            if (!element.isJsonObject()) continue;
            JsonObject form = element.getAsJsonObject();
            String formName = string(form, "name");
            JsonArray moves = form.getAsJsonArray("moves");
            if (formName == null || moves == null) continue;
            Set<String> formMoves = learnsets.forms.computeIfAbsent(
                    normalizeForm(formName), ignored -> new LinkedHashSet<>());
            addMoves(moves, formMoves);
        }
    }

    private static boolean isSpeciesJson(Path path) {
        if (!path.getFileName().toString().endsWith(".json")) return false;
        List<String> parts = Stream.of(path.toString().replace('\\', '/').split("/"))
                .map(value -> value.toLowerCase(Locale.ROOT)).toList();
        return parts.contains("species") || parts.contains("species_additions");
    }

    private static void addMoves(JsonArray values, Set<String> destination) {
        if (values == null) return;
        for (JsonElement element : values) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) continue;
            String move = parseMoveSpec(element.getAsString());
            if (move != null) destination.add(move);
        }
    }

    static String parseMoveSpec(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.strip();
        int separator = value.indexOf(':');
        if (separator <= 0 || separator + 1 >= value.length()) return null;
        String method = value.substring(0, separator).toLowerCase(Locale.ROOT);
        if (!isObtainableMethod(method)) return null;
        String move = value.substring(separator + 1).strip().toLowerCase(Locale.ROOT);
        int condition = move.indexOf(' ');
        if (condition >= 0) move = move.substring(0, condition);
        if (move.indexOf(':') >= 0) move = move.substring(move.lastIndexOf(':') + 1);
        return move.matches("[a-z0-9_-]+") ? move : null;
    }

    private static boolean isObtainableMethod(String method) {
        if (method.chars().allMatch(Character::isDigit)) return true;
        return method.equals("tm") || method.equals("egg") || method.equals("tutor")
                || method.equals("evolution") || method.equals("form_change");
    }

    private static String string(JsonObject object, String member) {
        JsonElement value = object.get(member);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString() : null;
    }

    private static String normalizeForm(String formName) {
        return formName == null ? DEFAULT_FORM : formName.strip().toLowerCase(Locale.ROOT);
    }

    private static final class SpeciesLearnsets {
        private final Set<String> base = new LinkedHashSet<>();
        private final Map<String, Set<String>> forms = new LinkedHashMap<>();
    }
}
