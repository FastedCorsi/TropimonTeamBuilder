package fr.tropimon.teamsaver.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Pure, deterministic competitive checks used by the in-game Team Doctor panel. */
final class TeamDoctor {
    private static final Set<String> HAZARDS = Set.of(
            "stealthrock", "spikes", "toxicspikes", "stickyweb", "stoneaxe", "ceaselessedge",
            "gmaxsteelsurge");
    private static final Set<String> REMOVAL = Set.of(
            "defog", "rapidspin", "mortalspin", "tidyup", "courtchange", "gmaxwindrage");
    private static final Set<String> HAZARD_ABILITIES = Set.of("toxicdebris");
    private static final Set<String> HAZARD_CONTROL_ABILITIES = Set.of("magicbounce");
    private static final Set<String> SPEED_CONTROL = Set.of(
            "tailwind", "trickroom", "icywind", "electroweb", "thunderwave", "glare", "nuzzle");
    private static final Set<String> RECOVERY = Set.of(
            "recover", "roost", "slackoff", "softboiled", "milkdrink", "shoreup", "synthesis",
            "moonlight", "morningsun", "strengthsap", "wish", "rest", "lifedew", "junglehealing");
    private static final Set<String> PIVOT = Set.of(
            "uturn", "voltswitch", "flipturn", "partingshot", "teleport", "chillyreception", "batonpass");
    private static final Set<String> SETUP = Set.of(
            "swordsdance", "nastyplot", "dragondance", "quiverdance", "calmmind", "bulkup", "bellydrum",
            "shellsmash", "shiftgear", "coil", "agility", "irondefense", "curse", "tailglow", "geomancy");
    private static final Set<String> GROUND_IMMUNITIES = Set.of("levitate", "eartheater");
    private static final Set<String> WATER_IMMUNITIES = Set.of("waterabsorb", "stormdrain", "dryskin");
    private static final Set<String> FIRE_IMMUNITIES = Set.of("flashfire", "wellbakedbody");
    private static final Set<String> ELECTRIC_IMMUNITIES = Set.of("voltabsorb", "lightningrod", "motordrive");
    private static final Set<String> GRASS_IMMUNITIES = Set.of("sapsipper");
    private static final Map<String, Set<String>> SUPER_EFFECTIVE = Map.ofEntries(
            Map.entry("fire", Set.of("grass", "ice", "bug", "steel")),
            Map.entry("water", Set.of("fire", "ground", "rock")),
            Map.entry("electric", Set.of("water", "flying")),
            Map.entry("grass", Set.of("water", "ground", "rock")),
            Map.entry("ice", Set.of("grass", "ground", "flying", "dragon")),
            Map.entry("fighting", Set.of("normal", "ice", "rock", "dark", "steel")),
            Map.entry("poison", Set.of("grass", "fairy")),
            Map.entry("ground", Set.of("fire", "electric", "poison", "rock", "steel")),
            Map.entry("flying", Set.of("grass", "fighting", "bug")),
            Map.entry("psychic", Set.of("fighting", "poison")),
            Map.entry("bug", Set.of("grass", "psychic", "dark")),
            Map.entry("rock", Set.of("fire", "ice", "flying", "bug")),
            Map.entry("ghost", Set.of("psychic", "ghost")),
            Map.entry("dragon", Set.of("dragon")),
            Map.entry("dark", Set.of("psychic", "ghost")),
            Map.entry("steel", Set.of("ice", "rock", "fairy")),
            Map.entry("fairy", Set.of("fighting", "dragon", "dark")));
    private static final Map<String, Set<String>> RESISTED = Map.ofEntries(
            Map.entry("normal", Set.of("rock", "steel")),
            Map.entry("fire", Set.of("fire", "water", "rock", "dragon")),
            Map.entry("water", Set.of("water", "grass", "dragon")),
            Map.entry("electric", Set.of("electric", "grass", "dragon")),
            Map.entry("grass", Set.of("fire", "grass", "poison", "flying", "bug", "dragon", "steel")),
            Map.entry("ice", Set.of("fire", "water", "ice", "steel")),
            Map.entry("fighting", Set.of("poison", "flying", "psychic", "bug", "fairy")),
            Map.entry("poison", Set.of("poison", "ground", "rock", "ghost")),
            Map.entry("ground", Set.of("grass", "bug")),
            Map.entry("flying", Set.of("electric", "rock", "steel")),
            Map.entry("psychic", Set.of("psychic", "steel")),
            Map.entry("bug", Set.of("fire", "fighting", "poison", "flying", "ghost", "steel", "fairy")),
            Map.entry("rock", Set.of("fighting", "ground", "steel")),
            Map.entry("ghost", Set.of("dark")),
            Map.entry("dragon", Set.of("steel")),
            Map.entry("dark", Set.of("fighting", "dark", "fairy")),
            Map.entry("steel", Set.of("fire", "water", "electric", "steel")),
            Map.entry("fairy", Set.of("fire", "poison", "steel")));
    private static final Map<String, Set<String>> IMMUNE = Map.of(
            "normal", Set.of("ghost"),
            "electric", Set.of("ground"),
            "fighting", Set.of("ghost"),
            "poison", Set.of("steel"),
            "ground", Set.of("flying"),
            "psychic", Set.of("dark"),
            "ghost", Set.of("normal"),
            "dragon", Set.of("fairy"));

    private TeamDoctor() {
    }

    static List<Finding> analyze(List<Member> members) {
        return analyze(members, RankedUsageService.BuildStyle.BALANCED);
    }

    static List<Finding> analyze(List<Member> members, RankedUsageService.BuildStyle requestedStyle) {
        List<Member> team = members == null ? List.of() : members.stream().filter(member -> member != null).toList();
        RankedUsageService.BuildStyle style = requestedStyle == null
                ? RankedUsageService.BuildStyle.BALANCED : requestedStyle;
        List<Finding> findings = new ArrayList<>();
        if (team.size() < 6) findings.add(new Finding(Code.INCOMPLETE, String.valueOf(team.size()), 1));

        Map<String, Integer> typeCounts = new LinkedHashMap<>();
        boolean hazards = false;
        boolean removal = false;
        boolean speedControl = false;
        boolean physical = false;
        boolean special = false;
        int recoveryUsers = 0;
        int pivotUsers = 0;
        int setupUsers = 0;
        int trickRoomSetters = 0;
        int weatherSetters = 0;
        int weatherAbusers = 0;
        boolean auroraVeil = false;
        List<String> hazardSetters = new ArrayList<>();
        List<String> hazardControllers = new ArrayList<>();
        int fastest = 0;
        for (Member member : team) {
            for (String type : member.types) typeCounts.merge(normalize(type), 1, Integer::sum);
            Set<String> memberMoves = new LinkedHashSet<>();
            for (String move : member.moves) {
                String id = canonicalRoleMove(move);
                memberMoves.add(id);
                speedControl |= SPEED_CONTROL.contains(id);
            }
            String ability = normalizeIdentifier(member.abilityId);
            boolean memberHazards = intersects(memberMoves, HAZARDS) || HAZARD_ABILITIES.contains(ability);
            boolean memberRemoval = intersects(memberMoves, REMOVAL) || HAZARD_CONTROL_ABILITIES.contains(ability);
            hazards |= memberHazards;
            removal |= memberRemoval;
            if (memberHazards) hazardSetters.add(member.name());
            if (memberRemoval) hazardControllers.add(member.name());
            recoveryUsers += intersects(memberMoves, RECOVERY) ? 1 : 0;
            pivotUsers += intersects(memberMoves, PIVOT) ? 1 : 0;
            setupUsers += intersects(memberMoves, SETUP) ? 1 : 0;
            trickRoomSetters += memberMoves.contains("trickroom") ? 1 : 0;
            auroraVeil |= memberMoves.contains("auroraveil");
            if (isWeatherStyle(style)) {
                weatherSetters += weatherSetter(style, memberMoves, ability) ? 1 : 0;
                weatherAbusers += weatherAbuser(style, member, memberMoves, ability) ? 1 : 0;
            }
            boolean hasCategorizedDamage = member.moveInfo.stream().anyMatch(CompetitiveSetCoherence.MoveInfo::damaging);
            physical |= member.moveInfo.stream().anyMatch(move -> move.damaging() && move.category().equals("physical"));
            special |= member.moveInfo.stream().anyMatch(move -> move.damaging() && move.category().equals("special"));
            if (!hasCategorizedDamage) {
                physical |= member.offence.equals("physical") || member.offence.equals("mixed");
                special |= member.offence.equals("special") || member.offence.equals("mixed");
            }
            addSetFinding(findings, member);
            fastest = Math.max(fastest, member.baseSpeed);
        }

        if (!hazardSetters.isEmpty()) {
            findings.add(new Finding(Code.HAZARD_SETTERS, String.join(", ", hazardSetters), 0));
        }
        if (!hazardControllers.isEmpty()) {
            findings.add(new Finding(Code.HAZARD_CONTROLLERS, String.join(", ", hazardControllers), 0));
        }
        Set<String> monotypeTypes = commonTypes(team);
        if (!monotypeTypes.isEmpty()) {
            findings.add(new Finding(Code.MONOTYPE, monotypeTypes.iterator().next(), 0));
        }

        if (!team.isEmpty() && !hazards) {
            MoveSuggestion suggestion = suggestMove(team, HAZARDS);
            findings.add(suggestion == null ? new Finding(Code.NO_HAZARDS, "", 1)
                    : new Finding(Code.SUGGEST_HAZARD_MOVE, suggestion.detail(), 1));
        }
        if (!team.isEmpty() && !removal) {
            MoveSuggestion suggestion = suggestMove(team, REMOVAL);
            findings.add(suggestion == null ? new Finding(Code.NO_REMOVAL, "", 2)
                    : new Finding(Code.SUGGEST_REMOVAL_MOVE, suggestion.detail(), 1));
        }
        addStyleFindings(findings, style, recoveryUsers, pivotUsers, setupUsers,
                trickRoomSetters, weatherSetters, weatherAbusers, auroraVeil);

        typeCounts.entrySet().stream()
                .filter(entry -> entry.getValue() >= 3)
                .filter(entry -> !monotypeTypes.contains(entry.getKey()))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> findings.add(new Finding(Code.TYPE_STACK,
                        entry.getKey() + ":" + entry.getValue(), 2)));
        Map<String, Integer> weaknessCounts = new LinkedHashMap<>();
        for (String attackType : SUPER_EFFECTIVE.keySet()) {
            int weakMembers = 0;
            for (Member member : team) if (effectiveness(attackType, member) > 1.0D) weakMembers++;
            int warningThreshold = monotypeTypes.isEmpty() ? 3 : 4;
            if (weakMembers >= warningThreshold) weaknessCounts.put(attackType, weakMembers);
        }
        weaknessCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> findings.add(new Finding(Code.SHARED_WEAKNESS,
                        entry.getKey() + ":" + entry.getValue(), 2)));
        if (!team.isEmpty() && fastest < 100 && !speedControl) {
            findings.add(new Finding(Code.LOW_SPEED, String.valueOf(fastest), 1));
        }
        if (!team.isEmpty() && !physical) findings.add(new Finding(Code.NO_PHYSICAL, "", 1));
        if (!team.isEmpty() && !special) findings.add(new Finding(Code.NO_SPECIAL, "", 1));
        if (findings.stream().noneMatch(finding -> finding.severity > 0)) {
            findings.add(new Finding(Code.HEALTHY, "", 0));
        }
        return List.copyOf(findings);
    }

    private static String canonicalRoleMove(String raw) {
        String id = normalizeIdentifier(raw);
        return switch (id) {
            case "piegederoc", "piegesderoc", "stealthrocks" -> "stealthrock";
            case "picotstoxik", "toxicspike" -> "toxicspikes";
            case "toilegluante" -> "stickyweb";
            case "antibrume" -> "defog";
            case "tourrapide" -> "rapidspin";
            case "toupieeclat" -> "mortalspin";
            case "changecote" -> "courtchange";
            case "grandnettoyage", "grandmenage" -> "tidyup";
            default -> id;
        };
    }

    private static Set<String> commonTypes(List<Member> team) {
        if (team.size() < 3) return Set.of();
        Set<String> common = new LinkedHashSet<>();
        for (String type : team.getFirst().types) common.add(normalize(type));
        for (int index = 1; index < team.size() && !common.isEmpty(); index++) {
            Set<String> memberTypes = new LinkedHashSet<>();
            for (String type : team.get(index).types) memberTypes.add(normalize(type));
            common.retainAll(memberTypes);
        }
        return Set.copyOf(common);
    }

    private static void addSetFinding(List<Finding> findings, Member member) {
        if (member.moveInfo.isEmpty()) return;
        CompetitiveSetCoherence.Profile profile = new CompetitiveSetCoherence.Profile(
                member.itemId, member.natureId, member.evs, member.moveInfo);
        Set<CompetitiveSetCoherence.Issue> issues = CompetitiveSetCoherence.issues(profile);
        CompetitiveSetCoherence.Issue issue = issues.stream()
                .filter(value -> value != CompetitiveSetCoherence.Issue.EMPTY_SET)
                .findFirst().orElse(null);
        if (issue == null) return;
        findings.add(new Finding(Code.SET_CONFLICT, member.name() + "|" + issue.name(), 2));
    }

    private static MoveSuggestion suggestMove(List<Member> team, Set<String> roleMoves) {
        for (Member member : team) {
            if (normalizeIdentifier(member.speciesId).equals("glimmora")
                    && roleMoves == REMOVAL && member.legalMoves.contains("mortalspin")) {
                return new MoveSuggestion(member.name(), "mortalspin");
            }
        }
        for (Member member : team) {
            for (String preferred : roleMoves) {
                if (member.legalMoves.contains(preferred)) return new MoveSuggestion(member.name(), preferred);
            }
        }
        return null;
    }

    private static void addStyleFindings(List<Finding> findings, RankedUsageService.BuildStyle style,
                                         int recoveryUsers, int pivotUsers, int setupUsers,
                                         int trickRoomSetters, int weatherSetters, int weatherAbusers,
                                         boolean auroraVeil) {
        if (style == RankedUsageService.BuildStyle.TRICK_ROOM && trickRoomSetters < 2) {
            findings.add(new Finding(Code.STYLE_TRICK_ROOM, String.valueOf(trickRoomSetters), 2));
        }
        if (isWeatherStyle(style)) {
            if (weatherSetters < 1 || weatherAbusers < 2) {
                findings.add(new Finding(Code.STYLE_WEATHER,
                        style.name() + "|" + weatherSetters + "|" + weatherAbusers, 2));
            }
            if (style == RankedUsageService.BuildStyle.AURORA_VEIL && !auroraVeil) {
                findings.add(new Finding(Code.STYLE_AURORA_VEIL, "", 2));
            }
        }
        int expectedRecovery = style == RankedUsageService.BuildStyle.STALL ? 3
                : style == RankedUsageService.BuildStyle.BULKY_OFFENSE ? 2
                : style == RankedUsageService.BuildStyle.BALANCED ? 1 : 0;
        if (recoveryUsers < expectedRecovery) {
            findings.add(new Finding(Code.STYLE_RECOVERY,
                    recoveryUsers + "|" + expectedRecovery, style == RankedUsageService.BuildStyle.STALL ? 2 : 1));
        }
        if ((style == RankedUsageService.BuildStyle.BALANCED
                || style == RankedUsageService.BuildStyle.BULKY_OFFENSE
                || style == RankedUsageService.BuildStyle.OFFENSE) && pivotUsers == 0) {
            findings.add(new Finding(Code.STYLE_PIVOT, "", 1));
        }
        if ((style == RankedUsageService.BuildStyle.OFFENSE
                || style == RankedUsageService.BuildStyle.BULKY_OFFENSE) && setupUsers == 0) {
            findings.add(new Finding(Code.STYLE_WIN_CONDITION, "", 1));
        }
    }

    private static boolean isWeatherStyle(RankedUsageService.BuildStyle style) {
        return style == RankedUsageService.BuildStyle.RAIN || style == RankedUsageService.BuildStyle.SUN
                || style == RankedUsageService.BuildStyle.SAND || style == RankedUsageService.BuildStyle.SNOW
                || style == RankedUsageService.BuildStyle.AURORA_VEIL;
    }

    private static boolean weatherSetter(RankedUsageService.BuildStyle style, Set<String> moves, String ability) {
        return switch (style) {
            case RAIN -> Set.of("drizzle", "primordialsea").contains(ability) || moves.contains("raindance");
            case SUN -> Set.of("drought", "orichalcumpulse", "desolateland").contains(ability)
                    || moves.contains("sunnyday");
            case SAND -> Set.of("sandstream", "sandspit").contains(ability) || moves.contains("sandstorm");
            case SNOW, AURORA_VEIL -> ability.equals("snowwarning")
                    || moves.contains("snowscape") || moves.contains("hail");
            default -> false;
        };
    }

    private static boolean weatherAbuser(RankedUsageService.BuildStyle style, Member member,
                                          Set<String> moves, String ability) {
        return switch (style) {
            case RAIN -> Set.of("swiftswim", "raindish", "hydration", "dryskin").contains(ability)
                    || intersects(moves, Set.of("thunder", "hurricane", "weatherball"));
            case SUN -> Set.of("chlorophyll", "solarpower", "harvest", "protosynthesis").contains(ability)
                    || intersects(moves, Set.of("solarbeam", "solarblade", "weatherball"));
            case SAND -> Set.of("sandrush", "sandforce", "sandveil").contains(ability)
                    || member.types.contains("rock");
            case SNOW, AURORA_VEIL -> Set.of("slushrush", "icebody", "snowcloak").contains(ability)
                    || member.types.contains("ice");
            default -> false;
        };
    }

    private static boolean intersects(Set<String> values, Set<String> expected) {
        for (String value : values) if (expected.contains(value)) return true;
        return false;
    }

    private static String normalize(String raw) {
        if (raw == null) return "";
        String lower = raw.toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(lower.length());
        for (int index = 0; index < lower.length(); index++) {
            char character = lower.charAt(index);
            if (character >= 'a' && character <= 'z' || character >= '0' && character <= '9') {
                result.append(character);
            }
        }
        return result.toString();
    }

    private static String normalizeIdentifier(String raw) {
        if (raw == null) return "";
        String value = raw.strip();
        int namespace = value.lastIndexOf(':');
        if (namespace >= 0 && namespace + 1 < value.length()) value = value.substring(namespace + 1);
        return normalize(value);
    }

    private static double effectiveness(String attackType, Member member) {
        String ability = normalizeIdentifier(member.abilityId);
        String item = normalizeIdentifier(member.itemId);
        if (attackType.equals("ground") && (GROUND_IMMUNITIES.contains(ability) || item.equals("airballoon"))) return 0.0D;
        if (attackType.equals("water") && WATER_IMMUNITIES.contains(ability)) return 0.0D;
        if (attackType.equals("fire") && FIRE_IMMUNITIES.contains(ability)) return 0.0D;
        if (attackType.equals("electric") && ELECTRIC_IMMUNITIES.contains(ability)) return 0.0D;
        if (attackType.equals("grass") && GRASS_IMMUNITIES.contains(ability)) return 0.0D;
        double multiplier = 1.0D;
        for (String rawType : member.types) {
            String defendingType = normalize(rawType);
            if (IMMUNE.getOrDefault(attackType, Set.of()).contains(defendingType)) return 0.0D;
            if (SUPER_EFFECTIVE.getOrDefault(attackType, Set.of()).contains(defendingType)) multiplier *= 2.0D;
            if (RESISTED.getOrDefault(attackType, Set.of()).contains(defendingType)) multiplier *= 0.5D;
        }
        if (ability.equals("thickfat") && (attackType.equals("fire") || attackType.equals("ice"))) multiplier *= 0.5D;
        return multiplier;
    }

    enum Code {
        INCOMPLETE, MONOTYPE, TYPE_STACK, SHARED_WEAKNESS, HAZARD_SETTERS, HAZARD_CONTROLLERS,
        NO_HAZARDS, NO_REMOVAL,
        SUGGEST_HAZARD_MOVE, SUGGEST_REMOVAL_MOVE, SET_CONFLICT,
        STYLE_TRICK_ROOM, STYLE_WEATHER, STYLE_AURORA_VEIL, STYLE_RECOVERY,
        STYLE_PIVOT, STYLE_WIN_CONDITION, LOW_SPEED, NO_PHYSICAL, NO_SPECIAL, HEALTHY
    }

    record Member(Set<String> types, String offence, int baseSpeed, List<String> moves,
                  String speciesId, String abilityId, String displayName, String itemId,
                  String natureId, Map<String, Integer> evs,
                  List<CompetitiveSetCoherence.MoveInfo> moveInfo, Set<String> legalMoves) {
        Member(Set<String> types, String offence, int baseSpeed, List<String> moves) {
            this(types, offence, baseSpeed, moves, "", "", "", "", "", Map.of(), List.of(), Set.of());
        }

        Member(Set<String> types, String offence, int baseSpeed, List<String> moves,
               String speciesId, String abilityId) {
            this(types, offence, baseSpeed, moves, speciesId, abilityId,
                    speciesId, "", "", Map.of(), List.of(), Set.of());
        }

        Member {
            types = types == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(types));
            offence = normalize(offence);
            baseSpeed = Math.max(0, baseSpeed);
            moves = moves == null ? List.of() : List.copyOf(moves);
            speciesId = speciesId == null ? "" : speciesId;
            abilityId = abilityId == null ? "" : abilityId;
            displayName = displayName == null ? "" : displayName;
            itemId = itemId == null ? "" : itemId;
            natureId = natureId == null ? "" : natureId;
            evs = evs == null ? Map.of() : Map.copyOf(evs);
            moveInfo = moveInfo == null ? List.of() : List.copyOf(moveInfo);
            Set<String> normalizedLegalMoves = new LinkedHashSet<>();
            if (legalMoves != null) for (String move : legalMoves) normalizedLegalMoves.add(normalizeIdentifier(move));
            legalMoves = Set.copyOf(normalizedLegalMoves);
        }

        String name() {
            return displayName.isBlank() ? normalizeIdentifier(speciesId) : displayName;
        }
    }

    record Finding(Code code, String detail, int severity) {
    }

    private record MoveSuggestion(String pokemon, String move) {
        String detail() {
            return pokemon + "|" + move;
        }
    }
}
