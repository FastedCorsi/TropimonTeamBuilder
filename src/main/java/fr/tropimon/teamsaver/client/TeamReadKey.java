package fr.tropimon.teamsaver.client;

import fr.tropimon.teamsaver.model.TeamModels.SavedSlot;
import fr.tropimon.teamsaver.model.TeamModels.SavedTeam;
import java.util.List;
import java.util.Map;

/** Deep copies: saved slots are mutable, so retaining their references would miss edits. */
record TeamReadKey(List<Slot> slots, long storage, long pokemon, long catalogue,
                   Map<String, Integer> inventory) {
    TeamReadKey {
        slots = List.copyOf(slots);
        inventory = Map.copyOf(inventory);
    }

    static TeamReadKey capture(SavedTeam team, long storage, long pokemon, long catalogue,
                               Map<String, Integer> inventory) {
        return new TeamReadKey(team.slots.stream().map(Slot::capture).toList(),
                storage, pokemon, catalogue, inventory);
    }

    record Slot(String pokemon, String species, String form, String item, String ability,
                String nature, List<String> moves, Map<String, Integer> evs) {
        static Slot capture(SavedSlot slot) {
            return new Slot(slot.pokemonId, slot.speciesId, slot.formId, slot.itemId, slot.abilityId,
                    slot.natureId, slot.moveIds == null ? List.of() : List.copyOf(slot.moveIds),
                    slot.evs == null ? Map.of() : Map.copyOf(slot.evs));
        }
    }
}
