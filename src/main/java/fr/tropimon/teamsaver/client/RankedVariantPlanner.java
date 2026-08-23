package fr.tropimon.teamsaver.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Selects one unlocked member that a single-pass variant must replace. */
final class RankedVariantPlanner {
    private RankedVariantPlanner() {
    }

    static String forcedReplacement(List<String> roster, Set<Integer> lockedSlots, long variation) {
        if (roster == null || roster.isEmpty()) return "";
        Set<Integer> locked = lockedSlots == null ? Set.of() : lockedSlots;
        List<String> candidates = new ArrayList<>();
        for (int index = 0; index < roster.size(); index++) {
            String key = roster.get(index);
            if (!locked.contains(index) && key != null && !key.isBlank()) candidates.add(key);
        }
        if (candidates.isEmpty()) return "";
        return candidates.get(Math.floorMod(variation, candidates.size()));
    }
}
