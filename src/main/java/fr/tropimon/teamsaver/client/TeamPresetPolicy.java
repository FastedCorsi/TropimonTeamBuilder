package fr.tropimon.teamsaver.client;

import fr.tropimon.teamsaver.model.TeamModels.SavedSlot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Keeps explicit presets authoritative over the live state of an associated Pokémon. */
final class TeamPresetPolicy {
    private TeamPresetPolicy() {
    }

    static SetValues saved(SavedSlot slot) {
        return new SetValues(slot.abilityId, slot.moveIds, slot.natureId, slot.evs);
    }

    static SetValues afterAssociation(SavedSlot planned, String actualAbility, List<String> actualMoves,
                                      String actualNature, Map<String, Integer> actualEvs) {
        String ability = isSpecified(planned.abilityId) ? planned.abilityId : actualAbility;
        List<String> moves = planned.moveIds != null && !planned.moveIds.isEmpty()
                ? planned.moveIds : actualMoves;
        String nature = isSpecified(planned.natureId) ? planned.natureId : actualNature;
        Map<String, Integer> evs = hasPlannedEvs(planned.evs) ? planned.evs : actualEvs;
        return new SetValues(ability, moves, nature, evs);
    }

    private static boolean isSpecified(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean hasPlannedEvs(Map<String, Integer> evs) {
        return evs != null && evs.values().stream().anyMatch(value -> value != null && value > 0);
    }

    record SetValues(String abilityId, List<String> moveIds, String natureId,
                     Map<String, Integer> evs) {
        SetValues {
            moveIds = moveIds == null ? List.of() : List.copyOf(moveIds);
            evs = evs == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(evs));
        }
    }
}
