package fr.tropimon.teamsaver.client;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class TeamDoctorCacheTest {
    @Test void cacheIsEquivalentForEveryStyleAndPreservesMembersAcrossStyleChanges() {
        TeamDoctorCache cache = new TeamDoctorCache();
        AtomicInteger builds = new AtomicInteger();
        var members = List.of(new TeamDoctor.Member(Set.of("rock", "poison"), "special", 86,
                List.of("stealthrock", "mortalspin"), "cobblemon:glimmora", "toxicdebris",
                "Floréclat", "cobblemon:focus_sash", "timid", Map.of("spa", 252),
                List.of(new CompetitiveSetCoherence.MoveInfo("powergem", "special", true)),
                Set.of("stealthrock", "mortalspin", "powergem")));
        var input = TeamDoctorCache.Input.capture(ReadModelCacheTest.team(), List.of("Floréclat"), 1, "fr_fr");
        for (RankedUsageService.BuildStyle style : RankedUsageService.BuildStyle.values()) {
            var result = cache.analyze(input, style, () -> { builds.incrementAndGet(); return members; });
            assertEquals(TeamDoctor.analyze(members, style), result);
            assertSame(result, cache.analyze(input, style, () -> { fail("Cache miss on unchanged render"); return members; }));
        }
        assertEquals(1, builds.get());
    }

    @Test void catalogueLanguageNicknameAndDraftChangesInvalidateMembers() {
        TeamDoctorCache cache = new TeamDoctorCache();
        AtomicInteger calls = new AtomicInteger();
        var team = ReadModelCacheTest.team();
        var base = TeamDoctorCache.Input.capture(team, List.of("Glimmora"), 1, "en_us");
        var inputs = new java.util.ArrayList<>(List.of(base,
                TeamDoctorCache.Input.capture(team, List.of("Nickname"), 1, "en_us"),
                TeamDoctorCache.Input.capture(team, List.of("Nickname"), 2, "en_us"),
                TeamDoctorCache.Input.capture(team, List.of("Floréclat"), 2, "fr_fr")));
        team.slots.getFirst().moveIds.add("mortalspin");
        inputs.add(TeamDoctorCache.Input.capture(team, List.of("Floréclat"), 2, "fr_fr"));
        for (var input : inputs) cache.analyze(input, RankedUsageService.BuildStyle.BALANCED,
                () -> { calls.incrementAndGet(); return List.of(); });
        assertEquals(5, calls.get());
    }
}
