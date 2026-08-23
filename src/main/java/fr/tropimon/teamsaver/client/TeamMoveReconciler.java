package fr.tropimon.teamsaver.client;

import fr.tropimon.teamsaver.model.TeamModels;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Keeps a planned moveset legal for a newly linked, already-owned Pokémon. */
final class TeamMoveReconciler {
    private TeamMoveReconciler() {
    }

    static Result reconcile(List<String> plannedMoves, List<String> learnedMoves,
                            List<String> activeMoves, int activeSlots) {
        List<String> planned = plannedMoves == null ? List.of() : plannedMoves;
        Set<String> learned = new LinkedHashSet<>(learnedMoves == null ? List.of() : learnedMoves);
        List<String> selected = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        int limit = Math.max(1, Math.min(TeamModels.MAX_MOVES, activeSlots));
        for (String move : planned) {
            if (move == null || move.isBlank()) continue;
            if (learned.contains(move)) {
                if (!selected.contains(move) && selected.size() < limit) selected.add(move);
            } else if (!missing.contains(move)) {
                missing.add(move);
            }
        }
        if (activeMoves != null) {
            for (String move : activeMoves) {
                if (selected.size() >= limit) break;
                if (move != null && !move.isBlank() && !selected.contains(move)) selected.add(move);
            }
        }
        return new Result(List.copyOf(selected), List.copyOf(missing));
    }

    record Result(List<String> selected, List<String> missing) {
    }
}
