package fr.tropimon.teamsaver.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class RankedVariantPlannerTest {
    @Test
    void selectsOnlyUnlockedMembersAndRotatesThem() {
        List<String> roster = List.of("a", "b", "c", "d", "e", "f");
        Set<Integer> locked = Set.of(0, 2, 4);

        assertEquals("b", RankedVariantPlanner.forcedReplacement(roster, locked, 0));
        assertEquals("d", RankedVariantPlanner.forcedReplacement(roster, locked, 1));
        assertEquals("f", RankedVariantPlanner.forcedReplacement(roster, locked, 2));
        assertEquals("b", RankedVariantPlanner.forcedReplacement(roster, locked, 3));
    }

    @Test
    void returnsEmptyWhenEveryMemberIsLocked() {
        assertEquals("", RankedVariantPlanner.forcedReplacement(
                List.of("a", "b"), Set.of(0, 1), 7));
    }
}
