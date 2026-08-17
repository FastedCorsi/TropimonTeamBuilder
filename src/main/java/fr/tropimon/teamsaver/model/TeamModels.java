package fr.tropimon.teamsaver.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TeamModels {
    public static final int MAX_TEAMS = 24;
    public static final int MAX_TEAM_SIZE = 6;
    public static final int MAX_MOVES = 4;
    private static final Pattern RESOURCE_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_/.-]+");

    private TeamModels() {
    }

    public static final class PlayerData {
        public List<SavedTeam> teams = new ArrayList<>();
        /** Pokémon UUID -> "box:slot". */
        public LinkedHashMap<String, String> returnSlots = new LinkedHashMap<>();

        public void normalize() {
            if (teams == null) teams = new ArrayList<>();
            if (returnSlots == null) returnSlots = new LinkedHashMap<>();
            teams.removeIf(team -> team == null);
            if (teams.size() > MAX_TEAMS) {
                teams = new ArrayList<>(teams.subList(0, MAX_TEAMS));
            }
            Set<String> teamIds = new HashSet<>();
            for (SavedTeam team : teams) {
                String canonicalId = canonicalUuid(team.id);
                if (canonicalId == null || !teamIds.add(canonicalId)) {
                    do {
                        canonicalId = UUID.randomUUID().toString();
                    } while (!teamIds.add(canonicalId));
                }
                team.id = canonicalId;
                team.normalize();
            }
            LinkedHashMap<String, String> normalizedReturnSlots = new LinkedHashMap<>();
            for (var entry : returnSlots.entrySet()) {
                String pokemonId = canonicalUuid(entry.getKey());
                if (pokemonId != null && isPcSlot(entry.getValue())) {
                    normalizedReturnSlots.put(pokemonId, entry.getValue());
                }
            }
            returnSlots = normalizedReturnSlots;
        }
    }

    public static final class SavedTeam {
        public String id;
        public String name;
        public List<SavedSlot> slots = new ArrayList<>();

        public void normalize() {
            name = cleanName(name);
            if (slots == null) slots = new ArrayList<>();
            Set<String> pokemonIds = new HashSet<>();
            List<SavedSlot> normalizedSlots = new ArrayList<>();
            for (SavedSlot slot : slots) {
                if (slot == null) continue;
                String pokemonId = canonicalUuid(slot.pokemonId);
                String speciesId = canonicalResourceId(slot.speciesId);
                if (pokemonId == null && speciesId == null) continue;
                if (pokemonId != null && !pokemonIds.add(pokemonId)) continue;
                slot.pokemonId = pokemonId;
                slot.speciesId = speciesId;
                if (slot.itemId == null || slot.itemId.isBlank()) slot.itemId = "minecraft:air";
                if (slot.moveIds == null) slot.moveIds = new ArrayList<>();
                List<String> normalizedMoves = new ArrayList<>();
                Set<String> uniqueMoves = new HashSet<>();
                for (String moveId : slot.moveIds) {
                    String normalizedMove = canonicalMoveId(moveId);
                    if (normalizedMove != null && uniqueMoves.add(normalizedMove)) normalizedMoves.add(normalizedMove);
                    if (normalizedMoves.size() == MAX_MOVES) break;
                }
                slot.moveIds = normalizedMoves;
                normalizedSlots.add(slot);
                if (normalizedSlots.size() == MAX_TEAM_SIZE) break;
            }
            slots = normalizedSlots;
        }
    }

    public static final class SavedSlot {
        public String pokemonId;
        /** Cobblemon species resource id; also identifies unresolved catalogue slots. */
        public String speciesId;
        public String itemId;
        /** Ordered Showdown move ids. Empty keeps legacy behaviour and does not modify moves. */
        public List<String> moveIds = new ArrayList<>();

        public SavedSlot() {
        }

        public SavedSlot(String pokemonId, String itemId) {
            this(pokemonId, null, itemId, List.of());
        }

        public SavedSlot(String pokemonId, String speciesId, String itemId, List<String> moveIds) {
            this.pokemonId = pokemonId;
            this.speciesId = speciesId;
            this.itemId = itemId == null ? "minecraft:air" : itemId;
            this.moveIds = moveIds == null ? new ArrayList<>() : new ArrayList<>(moveIds);
        }

        public boolean requiresOwnedPokemon() {
            return canonicalUuid(pokemonId) == null;
        }
    }

    public static final class SaveRequest {
        public String name;
        public List<SavedSlot> slots = new ArrayList<>();
    }

    public static String cleanName(String value) {
        String cleaned = value == null ? "" : value.strip().replaceAll("[\\p{Cntrl}]", "");
        if (cleaned.length() > 24) cleaned = cleaned.substring(0, 24);
        return cleaned.isBlank() ? "Team" : cleaned;
    }

    public static String numberedName(String baseName, int number) {
        String cleanBase = cleanName(baseName);
        String ending = " " + Math.max(2, number);
        int prefixLength = Math.max(0, 24 - ending.length());
        String prefix = cleanBase.substring(0, Math.min(cleanBase.length(), prefixLength)).stripTrailing();
        return cleanName(prefix + ending);
    }

    public static PlayerData mergeTeamLists(List<PlayerData> sources) {
        PlayerData merged = new PlayerData();
        Set<String> teamIds = new HashSet<>();
        if (sources == null) return merged;
        for (PlayerData source : sources) {
            if (source == null) continue;
            source.normalize();
            for (SavedTeam team : source.teams) {
                if (merged.teams.size() >= MAX_TEAMS) return merged;
                if (teamIds.add(team.id)) merged.teams.add(team);
            }
        }
        return merged;
    }

    private static String canonicalUuid(String value) {
        if (value == null) return null;
        try {
            return UUID.fromString(value).toString();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String canonicalResourceId(String value) {
        if (value == null) return null;
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        return RESOURCE_ID.matcher(normalized).matches() ? normalized : null;
    }

    private static String canonicalMoveId(String value) {
        if (value == null) return null;
        String normalized = value.strip().toLowerCase(Locale.ROOT).replace(" ", "");
        return normalized.matches("[a-z0-9_.-]+") ? normalized : null;
    }

    private static boolean isPcSlot(String value) {
        if (value == null) return false;
        try {
            String[] parts = value.split(":", 2);
            int box = Integer.parseInt(parts[0]);
            int slot = Integer.parseInt(parts[1]);
            return box >= 0 && slot >= 0 && slot < 30;
        } catch (Exception ignored) {
            return false;
        }
    }
}
