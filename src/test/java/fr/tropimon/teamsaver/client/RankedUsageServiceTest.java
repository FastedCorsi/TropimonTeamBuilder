package fr.tropimon.teamsaver.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class RankedUsageServiceTest {
    @Test
    void emptyTeamHasNoPreviousPokemonDuringGeneration() {
        assertNull(RankedUsageService.previousDetail(Map.of(), new LinkedHashSet<>()));
    }

    @Test
    void showdownNamesShareAStableKey() {
        assertEquals("goodrahisui", RankedUsageService.key("Goodra-Hisui"));
        assertEquals("goodrahisui", RankedUsageService.key("goodra_hisui"));
        assertEquals("flabebe", RankedUsageService.key("Flabébé"));
    }

    @Test
    void teammateAffinityCanOutrankRawUsage() {
        RankedUsageService.UsageEntry popular = usage("Popular", 30.0, 50.0);
        RankedUsageService.UsageEntry partner = usage("Partner", 15.0, 50.0);
        RankedUsageService.SpeciesStats selected = new RankedUsageService.SpeciesStats();
        selected.teammates = Map.of("Partner", 40.0);

        double popularScore = RankedUsageService.recommendationScore(popular, List.of(selected));
        double partnerScore = RankedUsageService.recommendationScore(partner, List.of(selected));

        assertTrue(partnerScore > popularScore);
    }

    @Test
    void previousPokemonTeammatesDriveTheNextChoice() {
        RankedUsageService.UsageEntry popular = usage("Popular", 40.0, 50.0);
        RankedUsageService.UsageEntry partner = usage("Partner", 12.0, 50.0);
        RankedUsageService.SpeciesStats previous = new RankedUsageService.SpeciesStats();
        previous.teammates = Map.of("Partner", 35.0);
        RankedUsageService.CandidateProfile profile = new RankedUsageService.CandidateProfile(Set.of("water"), "special");

        double popularScore = RankedUsageService.candidateScore(popular, previous, List.of(previous), profile,
                Map.of(), Map.of(), 0L, 1);
        double partnerScore = RankedUsageService.candidateScore(partner, previous, List.of(previous), profile,
                Map.of(), Map.of(), 0L, 1);

        assertTrue(partnerScore > popularScore);
    }

    @Test
    void aThirdSharedTypeIsRejectedWhenDiverseChoicesExist() {
        RankedUsageService.CandidateProfile fire =
                new RankedUsageService.CandidateProfile(Set.of("fire"), "physical");
        RankedUsageService.CandidateProfile water =
                new RankedUsageService.CandidateProfile(Set.of("water"), "special");

        assertTrue(RankedUsageService.exceedsTypeLimit(fire, Map.of("fire", 2)));
        assertFalse(RankedUsageService.exceedsTypeLimit(water, Map.of("fire", 2)));
    }

    @Test
    void monotypeRosterRecognizesOnlyItsSharedType() {
        Map<String, RankedUsageService.CandidateProfile> profiles = Map.of(
                "first", new RankedUsageService.CandidateProfile(Set.of("water", "ground"), "bulky"),
                "second", new RankedUsageService.CandidateProfile(Set.of("water", "flying"), "physical"),
                "third", new RankedUsageService.CandidateProfile(Set.of("water", "fairy"), "special"));

        assertEquals(Set.of("water"), RankedUsageService.sharedTypes(
                List.of("first", "second", "third"), profiles));
        assertEquals(0.0D, RankedUsageService.styleScore(profiles.get("first"),
                RankedUsageService.BuildStyle.MONOTYPE));
        assertEquals(Set.of("first", "second", "third"),
                RankedUsageService.profilesForType(profiles, "Water").keySet());
        assertTrue(RankedUsageService.profilesForType(profiles, "fire").isEmpty());
    }

    @Test
    void generationStyleChangesCandidatePriority() {
        RankedUsageService.CandidateProfile fast =
                new RankedUsageService.CandidateProfile(Set.of("electric"), "fast_offense", 120);
        RankedUsageService.CandidateProfile slow =
                new RankedUsageService.CandidateProfile(Set.of("psychic"), "bulky", 35);

        assertTrue(RankedUsageService.styleScore(fast, RankedUsageService.BuildStyle.OFFENSE)
                > RankedUsageService.styleScore(slow, RankedUsageService.BuildStyle.OFFENSE));
        assertTrue(RankedUsageService.styleScore(slow, RankedUsageService.BuildStyle.TRICK_ROOM)
                > RankedUsageService.styleScore(fast, RankedUsageService.BuildStyle.TRICK_ROOM));
    }

    @Test
    void offenseQuotaFiltersOutPassiveCandidatesUntilItIsMet() {
        RankedUsageService.UsageEntry wall = usage("Wall", 35.0, 50.0);
        RankedUsageService.UsageEntry sweeper = usage("Sweeper", 12.0, 50.0);
        Map<String, RankedUsageService.CandidateProfile> profiles = Map.of(
                "wall", new RankedUsageService.CandidateProfile(Set.of("water"), "bulky", 40),
                "sweeper", new RankedUsageService.CandidateProfile(Set.of("electric"), "fast_offense", 120));

        List<RankedUsageService.UsageEntry> preferred = RankedUsageService.prioritizeForStyle(
                List.of(wall, sweeper), Set.of(), profiles, RankedUsageService.BuildStyle.OFFENSE, 6);

        assertEquals(List.of(sweeper), preferred);
    }

    @Test
    void weatherStyleSelectsASetterBeforeOrdinarySynergy() {
        RankedUsageService.UsageEntry setter = usage("Setter", 8.0, 50.0);
        RankedUsageService.UsageEntry water = usage("Water", 30.0, 50.0);
        Map<String, RankedUsageService.CandidateProfile> profiles = Map.of(
                "setter", new RankedUsageService.CandidateProfile(Set.of("flying"), "balanced", 65,
                        Set.of("drizzle"), Set.of()),
                "water", new RankedUsageService.CandidateProfile(Set.of("water"), "special", 95));

        List<RankedUsageService.UsageEntry> preferred = RankedUsageService.prioritizeForStyle(
                List.of(water, setter), Set.of(), profiles, RankedUsageService.BuildStyle.RAIN, 6);

        assertEquals(List.of(setter), preferred);
    }

    @Test
    void trickRoomStyleSelectsASetterThenSlowPartners() {
        RankedUsageService.UsageEntry setter = usage("Setter", 7.0, 50.0);
        RankedUsageService.UsageEntry secondSetter = usage("Second Setter", 6.0, 50.0);
        RankedUsageService.UsageEntry slow = usage("Slow", 15.0, 50.0);
        RankedUsageService.UsageEntry fast = usage("Fast", 35.0, 50.0);
        Map<String, RankedUsageService.CandidateProfile> profiles = Map.of(
                "setter", new RankedUsageService.CandidateProfile(Set.of("psychic"), "bulky", 45,
                        Set.of(), Set.of("trickroom")),
                "secondsetter", new RankedUsageService.CandidateProfile(Set.of("fairy"), "bulky", 50,
                        Set.of(), Set.of("trickroom")),
                "slow", new RankedUsageService.CandidateProfile(Set.of("ground"), "physical", 30),
                "fast", new RankedUsageService.CandidateProfile(Set.of("electric"), "fast_offense", 130));

        assertEquals(List.of(setter, secondSetter), RankedUsageService.prioritizeForStyle(
                List.of(fast, slow, setter, secondSetter), Set.of(), profiles,
                RankedUsageService.BuildStyle.TRICK_ROOM, 6));
        assertEquals(List.of(secondSetter), RankedUsageService.prioritizeForStyle(
                List.of(fast, slow, secondSetter), Set.of("setter"), profiles,
                RankedUsageService.BuildStyle.TRICK_ROOM, 6));
        assertEquals(List.of(slow), RankedUsageService.prioritizeForStyle(
                List.of(fast, slow), Set.of("setter", "secondsetter"), profiles,
                RankedUsageService.BuildStyle.TRICK_ROOM, 6));
    }

    @Test
    void weatherStyleRequiresTwoBeneficiariesAfterItsSetter() {
        RankedUsageService.UsageEntry setter = usage("Setter", 25.0, 50.0);
        RankedUsageService.UsageEntry swimmer = usage("Swimmer", 8.0, 50.0);
        RankedUsageService.UsageEntry thunder = usage("Thunder", 7.0, 50.0);
        RankedUsageService.UsageEntry ordinary = usage("Ordinary", 35.0, 50.0);
        Map<String, RankedUsageService.CandidateProfile> profiles = Map.of(
                "setter", new RankedUsageService.CandidateProfile(Set.of("flying"), "balanced", 65,
                        Set.of("drizzle"), Set.of()),
                "swimmer", new RankedUsageService.CandidateProfile(Set.of("water"), "physical", 90,
                        Set.of("swiftswim"), Set.of()),
                "thunder", new RankedUsageService.CandidateProfile(Set.of("electric"), "special", 100,
                        Set.of(), Set.of("thunder")),
                "ordinary", new RankedUsageService.CandidateProfile(Set.of("normal"), "fast_offense", 110));

        assertEquals(List.of(swimmer), RankedUsageService.prioritizeForStyle(
                List.of(ordinary, swimmer), Set.of("setter"), profiles, RankedUsageService.BuildStyle.RAIN, 6));
        assertEquals(List.of(thunder), RankedUsageService.prioritizeForStyle(
                List.of(ordinary, thunder), Set.of("setter", "swimmer"), profiles,
                RankedUsageService.BuildStyle.RAIN, 6));
    }

    @Test
    void auroraVeilRequiresSnowThenAVeilUser() {
        RankedUsageService.UsageEntry setter = usage("Snow Setter", 10.0, 50.0);
        RankedUsageService.UsageEntry veil = usage("Veil User", 8.0, 50.0);
        RankedUsageService.UsageEntry attacker = usage("Attacker", 30.0, 50.0);
        Map<String, RankedUsageService.CandidateProfile> profiles = Map.of(
                "snowsetter", new RankedUsageService.CandidateProfile(Set.of("ice"), "balanced", 80,
                        Set.of("snowwarning"), Set.of()),
                "veiluser", new RankedUsageService.CandidateProfile(Set.of("ice"), "fast_offense", 110,
                        Set.of(), Set.of("auroraveil")),
                "attacker", new RankedUsageService.CandidateProfile(Set.of("dragon"), "fast_offense", 120));

        assertEquals(List.of(setter), RankedUsageService.prioritizeForStyle(
                List.of(attacker, veil, setter), Set.of(), profiles,
                RankedUsageService.BuildStyle.AURORA_VEIL, 6));
        assertEquals(List.of(veil), RankedUsageService.prioritizeForStyle(
                List.of(attacker, veil), Set.of("snowsetter"), profiles,
                RankedUsageService.BuildStyle.AURORA_VEIL, 6));
    }

    @Test
    void originalModeAlternatesMetaAnchorsAndLessUsedPartners() {
        RankedUsageService.UsageEntry meta = usage("Meta", 35.0, 50.0);
        RankedUsageService.UsageEntry secondMeta = usage("Second Meta", 25.0, 50.0);
        RankedUsageService.UsageEntry original = usage("Original", 3.0, 50.0);
        Set<String> metaKeys = Set.of("meta", "secondmeta");

        assertEquals(List.of(meta, secondMeta), RankedUsageService.prioritizeForMode(
                List.of(original, meta, secondMeta), Set.of(), metaKeys, 6,
                RankedUsageService.GenerationMode.ORIGINAL));
        assertEquals(List.of(original), RankedUsageService.prioritizeForMode(
                List.of(original, secondMeta), Set.of("meta"), metaKeys, 6,
                RankedUsageService.GenerationMode.ORIGINAL));
        assertEquals(List.of(secondMeta), RankedUsageService.prioritizeForMode(
                List.of(original, secondMeta), Set.of("meta", "original"), metaKeys, 6,
                RankedUsageService.GenerationMode.ORIGINAL));
    }

    @Test
    void missingCompetitiveRoleIsFilledBeforeRawUsage() {
        RankedUsageService.UsageEntry popular = usage("Popular", 35.0, 50.0);
        RankedUsageService.UsageEntry setter = usage("Setter", 8.0, 50.0);
        Map<String, RankedUsageService.CandidateProfile> profiles = Map.of(
                "popular", new RankedUsageService.CandidateProfile(Set.of("normal"), "physical", 100)
                        .normalized(),
                "setter", new RankedUsageService.CandidateProfile(Set.of("ground"), "bulky", 55,
                        Set.of(), Set.of("Stealth Rock")).normalized());

        assertEquals(List.of(setter), RankedUsageService.prioritizeForRoles(
                List.of(popular, setter), Set.of(), profiles, RankedUsageService.BuildStyle.BALANCED));
    }

    @Test
    void defensiveAnalysisPrefersAResistanceToAStackedWeakness() {
        Map<String, RankedUsageService.CandidateProfile> profiles = Map.of(
                "first", new RankedUsageService.CandidateProfile(Set.of("fire", "flying"), "fast_offense"),
                "second", new RankedUsageService.CandidateProfile(Set.of("fire", "bug"), "special"));
        RankedUsageService.CandidateProfile steel =
                new RankedUsageService.CandidateProfile(Set.of("steel"), "bulky");
        RankedUsageService.CandidateProfile ice =
                new RankedUsageService.CandidateProfile(Set.of("ice"), "special");

        double steelScore = RankedUsageService.defensiveSynergyScore(steel, Set.of("first", "second"), profiles);
        double iceScore = RankedUsageService.defensiveSynergyScore(ice, Set.of("first", "second"), profiles);

        assertTrue(steelScore > iceScore);
    }

    @Test
    void mixedModeKeepsThreeMetaAnchors() {
        RankedUsageService.UsageEntry meta = usage("Meta", 30.0, 50.0);
        RankedUsageService.UsageEntry original = usage("Original", 3.0, 50.0);

        assertEquals(List.of(meta), RankedUsageService.prioritizeForMode(
                List.of(original, meta), Set.of("anchorone", "anchortwo", "originala", "originalb", "originalc"),
                Set.of("anchorone", "anchortwo", "meta"), 6,
                RankedUsageService.GenerationMode.MIXED));
    }

    @Test
    void completeCompetitiveRolesBeatSixUnstructuredAttackers() {
        Map<String, RankedUsageService.CandidateProfile> profiles = Map.ofEntries(
                Map.entry("hazards", profile("ground", "bulky", 60, "stealthrock", "recover")),
                Map.entry("removal", profile("flying", "physical", 95, "defog", "uturn")),
                Map.entry("speed", profile("electric", "special", 120, "thunderbolt")),
                Map.entry("physical", profile("fighting", "physical", 85, "closecombat")),
                Map.entry("special", profile("psychic", "special", 90, "psychic")),
                Map.entry("support", profile("fairy", "bulky", 65, "wish")),
                Map.entry("plainone", profile("normal", "bulky", 60, "tackle")),
                Map.entry("plaintwo", profile("rock", "bulky", 60, "rockthrow")),
                Map.entry("plainthree", profile("bug", "bulky", 60, "bugbite")),
                Map.entry("plainfour", profile("ice", "bulky", 60, "iceshard")),
                Map.entry("plainfive", profile("poison", "bulky", 60, "acid")),
                Map.entry("plainsix", profile("dragon", "bulky", 60, "dragonbreath")));

        double structured = RankedUsageService.teamRoleCompletionScore(
                List.of("hazards", "removal", "speed", "physical", "special", "support"),
                profiles, RankedUsageService.BuildStyle.BALANCED, true);
        double unstructured = RankedUsageService.teamRoleCompletionScore(
                List.of("plainone", "plaintwo", "plainthree", "plainfour", "plainfive", "plainsix"),
                profiles, RankedUsageService.BuildStyle.BALANCED, true);

        assertTrue(structured > unstructured);
    }

    @Test
    void futureSightAndPhysicalPressureAreRecognizedAsARealSynergy() {
        Map<String, RankedUsageService.CandidateProfile> profiles = Map.of(
                "seer", profile("psychic", "special", 70, "futuresight", "teleport"),
                "breaker", profile("fighting", "physical", 90, "closecombat"),
                "passive", profile("normal", "bulky", 70, "tackle"));

        double synergy = RankedUsageService.conditionalSynergyScore(
                List.of("seer", "breaker"), profiles, RankedUsageService.BuildStyle.BALANCED);
        double ordinary = RankedUsageService.conditionalSynergyScore(
                List.of("seer", "passive"), profiles, RankedUsageService.BuildStyle.BALANCED);

        assertTrue(synergy > ordinary);
    }

    @Test
    void globalDefensePenalizesAnUnansweredSharedWeakness() {
        Map<String, RankedUsageService.CandidateProfile> profiles = Map.of(
                "fireone", profile("fire", "special", 90, "flamethrower"),
                "firetwo", profile("fire", "physical", 90, "flareblitz"),
                "firethree", profile("fire", "bulky", 70, "willowisp"),
                "water", profile("water", "special", 90, "surf"),
                "grass", profile("grass", "bulky", 70, "gigadrain"),
                "flying", profile("flying", "physical", 90, "bravebird"));

        double stacked = RankedUsageService.defensiveTeamScore(
                List.of("fireone", "firetwo", "firethree"), profiles);
        double balanced = RankedUsageService.defensiveTeamScore(
                List.of("water", "grass", "flying"), profiles);

        assertTrue(balanced > stacked);
    }

    private RankedUsageService.CandidateProfile profile(String type, String archetype, int speed,
                                                         String... moves) {
        return new RankedUsageService.CandidateProfile(Set.of(type), archetype, speed,
                Set.of(), Set.of(moves)).normalized();
    }

    private RankedUsageService.UsageEntry usage(String name, double usage, double winRate) {
        RankedUsageService.UsageEntry result = new RankedUsageService.UsageEntry();
        result.name = name;
        result.usagePercent = usage;
        result.winRate = winRate;
        return result;
    }
}
