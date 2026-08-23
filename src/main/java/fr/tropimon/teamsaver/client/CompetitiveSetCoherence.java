package fr.tropimon.teamsaver.client;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Cross-checks that independently sourced usage fields still form one playable set. */
final class CompetitiveSetCoherence {
    private static final Set<String> PHYSICAL_SETUP = Set.of(
            "bellydrum", "swordsdance", "dragondance", "bulkup", "coil", "howl", "shiftgear");
    private static final Set<String> SPECIAL_SETUP = Set.of(
            "nastyplot", "tailglow", "quiverdance", "geomancy", "calmmind");
    private static final Set<String> MULTI_HIT = Set.of(
            "armthrust", "bonerush", "bulletseed", "doubleslap", "dualwingbeat", "furyattack",
            "furyswipes", "iciclespear", "pinmissile", "populationbomb", "rockblast", "scaleshot",
            "tailslap", "tripleaxel", "watershuriken");
    private static final Set<String> PHYSICAL_NATURES = Set.of("adamant", "jolly", "brave", "naughty");
    private static final Set<String> SPECIAL_NATURES = Set.of("modest", "timid", "quiet", "mild");

    private CompetitiveSetCoherence() {
    }

    static boolean coherent(Profile profile) {
        return profile != null && issues(profile).isEmpty();
    }

    static Set<Issue> issues(Profile profile) {
        if (profile == null) return Set.of(Issue.EMPTY_SET);
        Set<Issue> issues = new LinkedHashSet<>();
        Offence offence = preferredOffence(profile);
        long physical = profile.moves.stream().filter(move -> move.damaging && move.category.equals("physical")).count();
        long special = profile.moves.stream().filter(move -> move.damaging && move.category.equals("special")).count();
        Set<String> moveIds = profile.moves.stream().map(MoveInfo::id).collect(java.util.stream.Collectors.toSet());
        if (offence == Offence.PHYSICAL && physical == 0 && special > 0) issues.add(Issue.PHYSICAL_PLAN_WITHOUT_ATTACK);
        if (offence == Offence.SPECIAL && special == 0 && physical > 0) issues.add(Issue.SPECIAL_PLAN_WITHOUT_ATTACK);
        if (moveIds.contains("bellydrum") && physical == 0) issues.add(Issue.BELLY_DRUM_WITHOUT_PHYSICAL);
        String item = key(profile.item);
        if (item.equals("loadeddice") && moveIds.stream().noneMatch(MULTI_HIT::contains)) {
            issues.add(Issue.LOADED_DICE_WITHOUT_MULTI_HIT);
        }
        if (item.equals("choiceband") && physical == 0) issues.add(Issue.CHOICE_BAND_WITHOUT_PHYSICAL);
        if (item.equals("choicespecs") && special == 0) issues.add(Issue.CHOICE_SPECS_WITHOUT_SPECIAL);
        if (item.equals("assaultvest") && profile.moves.stream().anyMatch(move -> !move.damaging)) {
            issues.add(Issue.ASSAULT_VEST_WITH_STATUS);
        }
        return Set.copyOf(issues);
    }

    static Offence preferredOffence(Profile profile) {
        int attack = ev(profile.evs, "atk");
        int specialAttack = ev(profile.evs, "spa");
        Set<String> ids = profile.moves.stream().map(MoveInfo::id).collect(java.util.stream.Collectors.toSet());
        if (attack >= specialAttack + 80 || PHYSICAL_NATURES.contains(key(profile.nature))
                || ids.stream().anyMatch(PHYSICAL_SETUP::contains)) return Offence.PHYSICAL;
        if (specialAttack >= attack + 80 || SPECIAL_NATURES.contains(key(profile.nature))
                || ids.stream().anyMatch(SPECIAL_SETUP::contains)) return Offence.SPECIAL;
        return Offence.MIXED;
    }

    static boolean supports(MoveInfo move, Offence offence) {
        if (move == null) return false;
        if (move.damaging) return offence == Offence.MIXED || move.category.equals(offence.id);
        if (offence == Offence.PHYSICAL && SPECIAL_SETUP.contains(move.id)) return false;
        if (offence == Offence.SPECIAL && PHYSICAL_SETUP.contains(move.id)) return false;
        return true;
    }

    static boolean multiHit(String moveId) {
        return MULTI_HIT.contains(key(moveId));
    }

    static boolean loadedDice(String itemId) {
        return key(itemId).equals("loadeddice");
    }

    private static int ev(Map<String, Integer> evs, String stat) {
        Integer value = evs == null ? null : evs.get(stat);
        return value == null ? 0 : Math.max(0, value);
    }

    private static String key(String raw) {
        if (raw == null) return "";
        String lower = raw.toLowerCase(Locale.ROOT);
        int namespace = lower.indexOf(':');
        if (namespace >= 0) lower = lower.substring(namespace + 1);
        StringBuilder result = new StringBuilder(lower.length());
        for (int index = 0; index < lower.length(); index++) {
            char character = lower.charAt(index);
            if (character >= 'a' && character <= 'z' || character >= '0' && character <= '9') {
                result.append(character);
            }
        }
        return result.toString();
    }

    enum Offence {
        PHYSICAL("physical"), SPECIAL("special"), MIXED("mixed");
        final String id;
        Offence(String id) { this.id = id; }
    }

    enum Issue {
        EMPTY_SET,
        PHYSICAL_PLAN_WITHOUT_ATTACK,
        SPECIAL_PLAN_WITHOUT_ATTACK,
        BELLY_DRUM_WITHOUT_PHYSICAL,
        LOADED_DICE_WITHOUT_MULTI_HIT,
        CHOICE_BAND_WITHOUT_PHYSICAL,
        CHOICE_SPECS_WITHOUT_SPECIAL,
        ASSAULT_VEST_WITH_STATUS
    }

    record MoveInfo(String id, String category, boolean damaging) {
        MoveInfo {
            id = key(id);
            category = key(category);
        }
    }

    record Profile(String item, String nature, Map<String, Integer> evs, List<MoveInfo> moves) {
        Profile {
            evs = evs == null ? Map.of() : Map.copyOf(evs);
            moves = moves == null ? List.of() : List.copyOf(moves);
        }
    }
}
