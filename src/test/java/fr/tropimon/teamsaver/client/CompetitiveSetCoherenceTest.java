package fr.tropimon.teamsaver.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class CompetitiveSetCoherenceTest {
    @Test
    void rejectsPhysicalBellyDrumSetWithOnlySpecialAttacksAndUselessLoadedDice() {
        var set = new CompetitiveSetCoherence.Profile("cobblemon:loaded_dice", "jolly",
                Map.of("atk", 252, "spe", 252), List.of(
                move("bellydrum", "status", false),
                move("clangingscales", "special", true),
                move("flamethrower", "special", true),
                move("flashcannon", "special", true)));

        assertFalse(CompetitiveSetCoherence.coherent(set));
    }

    @Test
    void acceptsPhysicalLoadedDiceSetWhenScaleShotActuallyUsesTheItem() {
        var set = new CompetitiveSetCoherence.Profile("cobblemon:loaded_dice", "jolly",
                Map.of("atk", 252, "spe", 252), List.of(
                move("scaleshot", "physical", true),
                move("closecombat", "physical", true),
                move("ironhead", "physical", true),
                move("swordsdance", "status", false)));

        assertTrue(CompetitiveSetCoherence.coherent(set));
    }

    @Test
    void rejectsAssaultVestWithStatusMove() {
        var set = new CompetitiveSetCoherence.Profile("cobblemon:assault_vest", "modest",
                Map.of("spa", 252), List.of(
                move("recover", "status", false), move("surf", "special", true)));

        assertFalse(CompetitiveSetCoherence.coherent(set));
        assertEquals(java.util.Set.of(CompetitiveSetCoherence.Issue.ASSAULT_VEST_WITH_STATUS),
                CompetitiveSetCoherence.issues(set));
    }

    private static CompetitiveSetCoherence.MoveInfo move(String id, String category, boolean damaging) {
        return new CompetitiveSetCoherence.MoveInfo(id, category, damaging);
    }
}
