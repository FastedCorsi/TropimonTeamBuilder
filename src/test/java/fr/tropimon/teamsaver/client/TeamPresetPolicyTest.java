package fr.tropimon.teamsaver.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import fr.tropimon.teamsaver.TeamJson;
import fr.tropimon.teamsaver.model.TeamModels.SavedSlot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class TeamPresetPolicyTest {
    @Test
    void openingAndSavingAnEditKeepsThePresetJsonIdentical() {
        SavedSlot before = slot("sharpness", List.of("suckerpunch", "ceaselessedge", "aquajet"),
                "adamant", Map.of("atk", 252, "hp", 252, "spe", 4));
        String expected = TeamJson.GSON.toJson(before);

        TeamPresetPolicy.SetValues edit = TeamPresetPolicy.saved(before);
        SavedSlot after = slot(edit.abilityId(), edit.moveIds(), edit.natureId(), edit.evs());

        assertEquals(expected, TeamJson.GSON.toJson(after));
    }

    @Test
    void editingAnAssociatedPokemonKeepsEverySavedPresetValue() {
        SavedSlot saved = slot("torrent", List.of("suckerpunch", "ceaselessedge"),
                "jolly", Map.of("atk", 252, "spe", 252, "hp", 4));

        TeamPresetPolicy.SetValues result = TeamPresetPolicy.saved(saved);

        assertEquals("torrent", result.abilityId());
        assertEquals(List.of("suckerpunch", "ceaselessedge"), result.moveIds());
        assertEquals("jolly", result.natureId());
        assertEquals(Map.of("atk", 252, "spe", 252, "hp", 4), result.evs());
    }

    @Test
    void associationNeverReplacesExplicitMovesNatureOrEvsWithTheLiveSet() {
        SavedSlot planned = slot("sharpness", List.of("suckerpunch", "aquajet"),
                "adamant", Map.of("atk", 252, "hp", 252, "spe", 4));

        TeamPresetPolicy.SetValues result = TeamPresetPolicy.afterAssociation(planned,
                "sharpness", List.of("flipturn", "razorshell"), "jolly",
                Map.of("atk", 100, "spe", 100));

        assertEquals("sharpness", result.abilityId());
        assertEquals(List.of("suckerpunch", "aquajet"), result.moveIds());
        assertEquals("adamant", result.natureId());
        assertEquals(Map.of("atk", 252, "hp", 252, "spe", 4), result.evs());
    }

    @Test
    void associationUsesLiveValuesOnlyForFieldsWithoutAPlannedPreset() {
        SavedSlot unspecified = slot(null, List.of(), null, Map.of());

        TeamPresetPolicy.SetValues result = TeamPresetPolicy.afterAssociation(unspecified,
                "torrent", List.of("flipturn", "razorshell"), "jolly",
                Map.of("atk", 252, "spe", 252));

        assertEquals("torrent", result.abilityId());
        assertEquals(List.of("flipturn", "razorshell"), result.moveIds());
        assertEquals("jolly", result.natureId());
        assertEquals(Map.of("atk", 252, "spe", 252), result.evs());
    }

    @Test
    void copiedPresetCannotBeChangedThroughTheOriginalCollections() {
        List<String> moves = new ArrayList<>(List.of("suckerpunch"));
        Map<String, Integer> evs = new LinkedHashMap<>(Map.of("atk", 252));
        SavedSlot saved = slot("torrent", moves, "jolly", evs);

        TeamPresetPolicy.SetValues result = TeamPresetPolicy.saved(saved);
        saved.moveIds.add("flipturn");
        saved.evs.put("spe", 252);

        assertEquals(List.of("suckerpunch"), result.moveIds());
        assertEquals(Map.of("atk", 252), result.evs());
    }

    private static SavedSlot slot(String ability, List<String> moves, String nature,
                                  Map<String, Integer> evs) {
        return new SavedSlot(null, "cobblemon:samurott", null, "minecraft:air",
                ability, moves, nature, evs);
    }
}
