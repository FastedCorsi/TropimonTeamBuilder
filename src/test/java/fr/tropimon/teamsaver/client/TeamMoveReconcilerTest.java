package fr.tropimon.teamsaver.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class TeamMoveReconcilerTest {
    @Test
    void fallbackAtEquipTimeDoesNotRewriteTheSavedPreset() {
        List<String> saved = new ArrayList<>(List.of("suckerpunch"));

        TeamMoveReconciler.Result result = TeamMoveReconciler.reconcile(
                saved, List.of("flipturn"), List.of("flipturn"), 1);

        assertEquals(List.of("flipturn"), result.selected());
        assertEquals(List.of("suckerpunch"), result.missing());
        assertEquals(List.of("suckerpunch"), saved);
    }

    @Test
    void keepsOwnedPresetMovesAndFillsWithCurrentMoves() {
        TeamMoveReconciler.Result result = TeamMoveReconciler.reconcile(
                List.of("roost", "defog", "bravebird", "uturn"),
                List.of("roost", "bravebird", "ironhead", "hurricane"),
                List.of("bravebird", "ironhead", "hurricane", "roost"), 4);

        assertEquals(List.of("roost", "bravebird", "ironhead", "hurricane"), result.selected());
        assertEquals(List.of("defog", "uturn"), result.missing());
    }

    @Test
    void neverSelectsAnUnlearnedMove() {
        TeamMoveReconciler.Result result = TeamMoveReconciler.reconcile(
                List.of("stealthrock"), List.of("tackle"), List.of("tackle"), 1);

        assertEquals(List.of("tackle"), result.selected());
        assertEquals(List.of("stealthrock"), result.missing());
    }

    @Test
    void neverCreatesMoreThanFourActiveMoves() {
        TeamMoveReconciler.Result result = TeamMoveReconciler.reconcile(
                List.of("move1", "move2", "move3", "move4", "move5"),
                List.of("move1", "move2", "move3", "move4", "move5"),
                List.of(), 99);

        assertEquals(List.of("move1", "move2", "move3", "move4"), result.selected());
    }
}
