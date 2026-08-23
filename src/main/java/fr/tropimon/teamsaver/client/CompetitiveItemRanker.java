package fr.tropimon.teamsaver.client;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Deterministic Gen 9-oriented relevance ordering for the PvP item catalogue. */
final class CompetitiveItemRanker {
    private static final Set<String> RECOVERY = Set.of(
            "recover", "roost", "slackoff", "softboiled", "synthesis", "moonlight", "morningsun",
            "wish", "rest", "protect", "spikyshield", "banefulbunker");
    private static final Set<String> PIVOT = Set.of("uturn", "voltswitch", "flipturn", "partingshot");
    private static final Set<String> SCREEN = Set.of("reflect", "lightscreen", "auroraveil");
    private static final Set<String> HAZARDS = Set.of("stealthrock", "spikes", "toxicspikes", "stickyweb");
    private static final Set<String> MULTI_HIT = Set.of(
            "bulletseed", "iciclespear", "rockblast", "tailslap", "scaleshot", "populationbomb");

    private CompetitiveItemRanker() {
    }

    static int score(String rawItemId, Profile profile) {
        String item = key(rawItemId);
        Profile value = profile == null ? Profile.empty() : profile;
        int score = switch (item) {
            case "leftovers", "heavydutyboots", "choicescarf", "choiceband", "choicespecs",
                    "focussash", "lifeorb", "assaultvest", "rockyhelmet" -> 45;
            case "sitrusberry", "lumberry", "expertbelt", "clearamulet", "covertcloak",
                    "boosterenergy", "airballoon", "eviolite" -> 30;
            default -> item.endsWith("berry") ? 12 : 8;
        };

        boolean physical = value.attack >= value.specialAttack + 12;
        boolean special = value.specialAttack >= value.attack + 12;
        boolean bulky = value.hp + value.defence + value.specialDefence >= 275;
        boolean fragile = value.hp + value.defence + value.specialDefence <= 215;
        boolean statusMoves = value.moves.stream().anyMatch(move -> RECOVERY.contains(move)
                || SCREEN.contains(move) || HAZARDS.contains(move) || move.equals("toxic")
                || move.equals("willowisp") || move.equals("thunderwave"));

        if (item.equals("choiceband") && physical) score += 90;
        if (item.equals("choicespecs") && special) score += 90;
        if (item.equals("choicescarf") && Math.max(value.attack, value.specialAttack) >= 90
                && value.speed < 105) score += 75;
        if (item.equals("lifeorb") && Math.max(value.attack, value.specialAttack) >= 100) score += 55;
        if (item.equals("leftovers") && (bulky || containsAny(value.moves, RECOVERY))) score += 85;
        if (item.equals("rockyhelmet") && bulky && value.defence >= value.specialDefence) score += 75;
        if (item.equals("assaultvest") && !statusMoves && value.specialDefence >= 75) score += 75;
        if (item.equals("focussash") && (fragile || containsAny(value.moves, HAZARDS))) score += 75;
        if (item.equals("heavydutyboots") && (containsAny(value.moves, PIVOT)
                || value.types.stream().anyMatch(Set.of("fire", "ice", "flying", "bug")::contains))) score += 95;
        if (item.equals("lightclay") && containsAny(value.moves, SCREEN)) score += 130;
        if (item.equals("blacksludge") && value.types.contains("poison")) score += 95;
        if ((item.equals("flameorb") || item.equals("toxicorb"))
                && Set.of("guts", "poisonheal", "quickfeet", "marvelscale").contains(value.ability)) score += 130;
        if (item.equals("powerherb") && value.moves.stream().anyMatch(
                move -> Set.of("meteorbeam", "solarbeam", "solarblade", "geomancy").contains(move))) score += 120;
        if (item.equals("loadeddice") && containsAny(value.moves, MULTI_HIT)) score += 115;
        if (item.equals("throatspray") && value.moves.stream().anyMatch(
                move -> Set.of("boomburst", "hypervoice", "snarl", "alluringsvoice", "torchsong").contains(move))) score += 100;
        if (item.equals("damprock") && value.moves.contains("raindance")) score += 115;
        if (item.equals("heatrock") && value.moves.contains("sunnyday")) score += 115;
        if (item.equals("icyrock") && value.moves.contains("snowscape")) score += 115;
        if (item.equals("smoothrock") && value.moves.contains("sandstorm")) score += 115;
        if (item.equals("terrainextender") && value.moves.stream().anyMatch(move -> move.endsWith("terrain"))) score += 110;
        return score;
    }

    private static boolean containsAny(Set<String> values, Set<String> wanted) {
        return values.stream().anyMatch(wanted::contains);
    }

    private static String key(String raw) {
        if (raw == null) return "";
        String lower = raw.toLowerCase(Locale.ROOT);
        int namespace = lower.indexOf(':');
        if (namespace >= 0) lower = lower.substring(namespace + 1);
        StringBuilder result = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') result.append(c);
        }
        return result.toString();
    }

    record Profile(Set<String> types, Set<String> moves, String ability, int hp, int attack,
                   int defence, int specialAttack, int specialDefence, int speed) {
        Profile {
            types = normalize(types);
            moves = normalize(moves);
            ability = key(ability);
        }

        static Profile empty() {
            return new Profile(Set.of(), Set.of(), "", 0, 0, 0, 0, 0, 0);
        }

        private static Set<String> normalize(Iterable<String> values) {
            Set<String> result = new LinkedHashSet<>();
            if (values != null) for (String value : values) result.add(key(value));
            return Set.copyOf(result);
        }
    }
}
