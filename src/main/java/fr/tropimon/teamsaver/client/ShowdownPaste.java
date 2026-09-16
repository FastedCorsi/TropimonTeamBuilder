package fr.tropimon.teamsaver.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Text-only Showdown interchange parser. Never modifies owned Pokémon or saved teams. */
final class ShowdownPaste {
    static final int MAX_LENGTH = 65_536;
    private ShowdownPaste() { }

    static Parsed parse(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("empty");
        if (raw.length() > MAX_LENGTH) throw new IllegalArgumentException("too_large");
        List<Member> members = new ArrayList<>();
        Set<String> warnings = new LinkedHashSet<>();
        String title = "";
        List<String> block = new ArrayList<>();
        for (String line : (raw.replace("\uFEFF", "").replace("\r", "") + "\n\n").split("\n", -1)) {
            line = line.strip();
            if (line.startsWith("===") && line.endsWith("===")) {
                if (line.length() < 6) throw new IllegalArgumentException("invalid_text");
                if (!block.isEmpty() || !members.isEmpty()) throw new IllegalArgumentException("multiple_teams");
                title = line.substring(3, line.length() - 3).strip().replaceFirst("^\\[[^]]+]\\s*", "");
                continue;
            }
            if (line.isEmpty()) {
                if (!block.isEmpty()) {
                    members.add(parseMember(block, warnings));
                    block.clear();
                    if (members.size() > 6) throw new IllegalArgumentException("too_many");
                }
            } else block.add(line);
        }
        if (members.isEmpty()) throw new IllegalArgumentException("empty");
        return new Parsed(title, List.copyOf(members), List.copyOf(warnings));
    }

    private static Member parseMember(List<String> lines, Set<String> warnings) {
        String header = lines.getFirst();
        String[] held = header.split("\\s+@\\s+", 2);
        String identity = held[0].replaceFirst("\\s+\\([MF]\\)$", "").strip();
        String species = identity;
        int nickname = identity.lastIndexOf(" (");
        if (nickname >= 0 && identity.endsWith(")")) {
            species = identity.substring(nickname + 2, identity.length() - 1);
            warnings.add("Nickname");
        }
        if (species.isBlank() || species.contains(":") || species.startsWith("-") || species.startsWith("<")) {
            throw new IllegalArgumentException("invalid_text");
        }
        String item = held.length == 2 ? held[1].strip() : "";
        String ability = "";
        String nature = "";
        Map<String, Integer> evs = new LinkedHashMap<>();
        List<String> moves = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("Ability:")) ability = line.substring(8).strip();
            else if (line.startsWith("EVs:")) parseEvs(line.substring(4), evs);
            else if (line.endsWith(" Nature")) nature = key(line.substring(0, line.length() - 7));
            else if (line.startsWith("- ") || line.startsWith("~ ")) {
                String move = key(line.substring(2));
                if (move.isEmpty()) throw new IllegalArgumentException("invalid_text");
                if (moves.contains(move)) throw new IllegalArgumentException("duplicate_move");
                moves.add(move);
                if (moves.size() > 4) throw new IllegalArgumentException("too_many_moves");
            } else {
                // Level, gender, IVs, Tera, happiness and cosmetics are not saved by this mod.
                int colon = line.indexOf(':');
                if (colon < 1) throw new IllegalArgumentException("invalid_text");
                warnings.add(line.substring(0, colon));
            }
        }
        return new Member(species, item, ability, nature, Map.copyOf(evs), List.copyOf(moves));
    }

    private static void parseEvs(String raw, Map<String, Integer> evs) {
        for (String part : raw.split("/")) {
            String[] pair = part.strip().split("\\s+", 2);
            if (pair.length != 2) throw new IllegalArgumentException("invalid_evs");
            String stat = key(pair[1]);
            if (!List.of("hp", "atk", "def", "spa", "spd", "spe").contains(stat)) {
                throw new IllegalArgumentException("invalid_evs");
            }
            int value;
            try { value = Integer.parseInt(pair[0]); }
            catch (NumberFormatException exception) { throw new IllegalArgumentException("invalid_evs"); }
            if (value < 0 || value > 252 || evs.putIfAbsent(stat, value) != null) {
                throw new IllegalArgumentException("invalid_evs");
            }
        }
        if (evs.values().stream().mapToInt(Integer::intValue).sum() > 510) {
            throw new IllegalArgumentException("invalid_evs");
        }
    }

    static String key(String raw) {
        return raw == null ? "" : raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    record Member(String species, String item, String ability, String nature,
                  Map<String, Integer> evs, List<String> moves) { }
    record Parsed(String name, List<Member> members, List<String> ignoredFields) { }
}
