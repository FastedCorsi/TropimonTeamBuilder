package fr.tropimon.teamsaver.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class TeamDoctorTest {
    @Test
    void detectsRepeatedTypesAndMissingUtility() {
        List<TeamDoctor.Member> team = List.of(
                member(Set.of("water"), "special", 80, List.of()),
                member(Set.of("water"), "physical", 70, List.of()),
                member(Set.of("water"), "mixed", 60, List.of()),
                member(Set.of("grass"), "special", 50, List.of()),
                member(Set.of("steel"), "physical", 40, List.of()),
                member(Set.of("fairy"), "mixed", 30, List.of()));

        List<TeamDoctor.Finding> findings = TeamDoctor.analyze(team);
        assertTrue(findings.stream().anyMatch(finding -> finding.code() == TeamDoctor.Code.TYPE_STACK));
        assertTrue(findings.stream().anyMatch(finding -> finding.code() == TeamDoctor.Code.NO_HAZARDS));
        assertTrue(findings.stream().anyMatch(finding -> finding.code() == TeamDoctor.Code.NO_REMOVAL));
    }

    @Test
    void acceptsACompleteBalancedUtilityCore() {
        List<TeamDoctor.Member> team = List.of(
                member(Set.of("rock"), "physical", 110, List.of("Stealth Rock")),
                member(Set.of("water"), "special", 100, List.of("Rapid Spin", "Recover")),
                member(Set.of("grass"), "mixed", 95, List.of("U-turn")),
                member(Set.of("steel"), "physical", 90, List.of()),
                member(Set.of("fairy"), "special", 85, List.of()),
                member(Set.of("ghost"), "mixed", 80, List.of()));

        List<TeamDoctor.Finding> findings = TeamDoctor.analyze(team);
        assertFalse(findings.stream().anyMatch(finding -> finding.severity() > 0));
        assertTrue(findings.stream().anyMatch(finding -> finding.code() == TeamDoctor.Code.HAZARD_SETTERS));
        assertTrue(findings.stream().anyMatch(finding -> finding.code() == TeamDoctor.Code.HEALTHY));
    }

    @Test
    void recognizesNamespacedHazardAndRemovalMoves() {
        List<TeamDoctor.Member> team = balancedTeamWithUtility(new TeamDoctor.Member(
                Set.of("rock", "poison"), "special", 86,
                List.of("cobblemon:toxic_spikes", "cobblemon:mortal_spin"),
                "cobblemon:glimmora", "cobblemon:corrosion"));

        List<TeamDoctor.Finding> findings = TeamDoctor.analyze(team);
        assertFalse(findings.stream().anyMatch(finding -> finding.code() == TeamDoctor.Code.NO_HAZARDS));
        assertFalse(findings.stream().anyMatch(finding -> finding.code() == TeamDoctor.Code.NO_REMOVAL));
    }

    @Test
    void toxicDebrisSetsHazardsButDoesNotInventRemoval() {
        List<TeamDoctor.Member> team = balancedTeamWithUtility(new TeamDoctor.Member(
                Set.of("rock", "poison"), "special", 86, List.of("powergem"),
                "cobblemon:glimmora", "cobblemon:toxic_debris"));

        List<TeamDoctor.Finding> findings = TeamDoctor.analyze(team);
        assertFalse(findings.stream().anyMatch(finding -> finding.code() == TeamDoctor.Code.NO_HAZARDS));
        assertTrue(findings.stream().anyMatch(finding -> finding.code() == TeamDoctor.Code.NO_REMOVAL));
    }

    @Test
    void magicBounceCountsAsPreventiveHazardControl() {
        List<TeamDoctor.Member> team = balancedTeamWithUtility(new TeamDoctor.Member(
                Set.of("psychic", "fairy"), "special", 110, List.of("stealthrock"),
                "cobblemon:hatterene", "cobblemon:magic_bounce"));

        List<TeamDoctor.Finding> findings = TeamDoctor.analyze(team);
        assertFalse(findings.stream().anyMatch(finding -> finding.code() == TeamDoctor.Code.NO_REMOVAL));
    }

    @Test
    void givesGlimmoraAConcreteMortalSpinRecommendation() {
        TeamDoctor.Member glimmora = new TeamDoctor.Member(
                Set.of("rock", "poison"), "special", 86, List.of("powergem"),
                "cobblemon:glimmora", "cobblemon:toxic_debris", "Glimmora",
                "minecraft:air", "timid", Map.of("spa", 252, "spe", 252),
                List.of(new CompetitiveSetCoherence.MoveInfo("powergem", "special", true)),
                Set.of("mortalspin", "toxicspikes"));

        List<TeamDoctor.Finding> findings = TeamDoctor.analyze(balancedTeamWithUtility(glimmora));
        assertTrue(findings.stream().anyMatch(finding -> finding.code() == TeamDoctor.Code.SUGGEST_REMOVAL_MOVE
                && finding.detail().equals("Glimmora|mortalspin")));
        assertFalse(findings.stream().anyMatch(finding -> finding.code() == TeamDoctor.Code.NO_REMOVAL));
    }

    @Test
    void validatesTheSelectedTrickRoomStyle() {
        List<TeamDoctor.Member> team = balancedTeamWithUtility(member(
                Set.of("psychic"), "special", 30, List.of("trickroom", "recover", "stealthrock", "rapidspin")));

        List<TeamDoctor.Finding> findings = TeamDoctor.analyze(team, RankedUsageService.BuildStyle.TRICK_ROOM);
        assertTrue(findings.stream().anyMatch(finding -> finding.code() == TeamDoctor.Code.STYLE_TRICK_ROOM
                && finding.detail().equals("1")));
    }

    @Test
    void reportsAFullSetItemConflictForThePokemon() {
        TeamDoctor.Member vestUser = new TeamDoctor.Member(
                Set.of("water"), "special", 70, List.of("surf", "recover"),
                "cobblemon:vaporeon", "waterabsorb", "Vaporeon",
                "cobblemon:assault_vest", "modest", Map.of("spa", 252),
                List.of(
                        new CompetitiveSetCoherence.MoveInfo("surf", "special", true),
                        new CompetitiveSetCoherence.MoveInfo("recover", "status", false)),
                Set.of("surf", "recover"));

        List<TeamDoctor.Finding> findings = TeamDoctor.analyze(balancedTeamWithUtility(vestUser));
        assertTrue(findings.stream().anyMatch(finding -> finding.code() == TeamDoctor.Code.SET_CONFLICT
                && finding.detail().contains("ASSAULT_VEST_WITH_STATUS")));
    }

    @Test
    void acceptsAnIntentionalMonotypeWithoutTypeStackWarning() {
        List<TeamDoctor.Member> waterTeam = List.of(
                member(Set.of("water", "ground"), "physical", 110, List.of("stealthrock")),
                member(Set.of("water", "flying"), "special", 100, List.of("defog")),
                member(Set.of("water", "fairy"), "mixed", 95, List.of("recover")),
                member(Set.of("water", "steel"), "physical", 90, List.of("uturn")),
                member(Set.of("water", "dragon"), "special", 85, List.of()),
                member(Set.of("water", "dark"), "mixed", 80, List.of()));

        List<TeamDoctor.Finding> findings = TeamDoctor.analyze(waterTeam);
        assertTrue(findings.stream().anyMatch(finding -> finding.code() == TeamDoctor.Code.MONOTYPE
                && finding.detail().equals("water")));
        assertFalse(findings.stream().anyMatch(finding -> finding.code() == TeamDoctor.Code.TYPE_STACK
                && finding.detail().startsWith("water:")));
    }

    private List<TeamDoctor.Member> balancedTeamWithUtility(TeamDoctor.Member utility) {
        return List.of(
                utility,
                member(Set.of("water"), "physical", 110, List.of("tailwind")),
                member(Set.of("grass"), "special", 105, List.of()),
                member(Set.of("steel"), "physical", 95, List.of()),
                member(Set.of("fairy"), "special", 90, List.of()),
                member(Set.of("ghost"), "mixed", 85, List.of()));
    }

    private TeamDoctor.Member member(Set<String> types, String offence, int speed, List<String> moves) {
        return new TeamDoctor.Member(types, offence, speed, moves);
    }
}
