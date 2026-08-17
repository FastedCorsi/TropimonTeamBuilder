package fr.tropimon.teamsaver.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import fr.tropimon.teamsaver.TeamJson;
import fr.tropimon.teamsaver.model.TeamModels.PlayerData;
import fr.tropimon.teamsaver.model.TeamModels.SavedSlot;
import fr.tropimon.teamsaver.model.TeamModels.SavedTeam;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class TeamModelsTest {
    @Test
    void teamNamesAreSanitizedAndLimited() {
        assertEquals("Team", TeamModels.cleanName("\n\t"));
        assertEquals(24, TeamModels.cleanName("abcdefghijklmnopqrstuvwxyz").length());
        String fullLength = "abcdefghijklmnopqrstuvwx";
        String numbered = TeamModels.numberedName(fullLength, 2);
        assertEquals(24, numbered.length());
        assertNotEquals(fullLength, numbered);
        assertTrue(numbered.endsWith(" 2"));
    }

    @Test
    void corruptedOrOversizedDataIsNormalized() {
        PlayerData data = new PlayerData();
        for (int teamIndex = 0; teamIndex < 30; teamIndex++) {
            SavedTeam team = new SavedTeam();
            team.id = UUID.randomUUID().toString();
            team.name = "Equipe " + teamIndex;
            for (int slot = 0; slot < 9; slot++) {
                team.slots.add(new SavedSlot(UUID.randomUUID().toString(), "minecraft:air"));
            }
            data.teams.add(team);
        }
        data.teams.add(null);

        data = TeamJson.GSON.fromJson(TeamJson.GSON.toJson(data), PlayerData.class);
        data.normalize();

        assertEquals(TeamModels.MAX_TEAMS, data.teams.size());
        assertEquals(TeamModels.MAX_TEAM_SIZE, data.teams.getFirst().slots.size());
    }

    @Test
    void jsonRoundTripKeepsPresetAndReturnSlot() {
        PlayerData original = new PlayerData();
        SavedTeam team = new SavedTeam();
        team.id = UUID.randomUUID().toString();
        team.name = "Rain";
        team.slots = new ArrayList<>();
        team.slots.add(new SavedSlot(UUID.randomUUID().toString(), "minecraft:leftovers"));
        original.teams.add(team);
        original.returnSlots.put(team.slots.getFirst().pokemonId, "3:12");

        PlayerData restored = TeamJson.GSON.fromJson(TeamJson.GSON.toJson(original), PlayerData.class);
        restored.normalize();

        assertNotNull(restored);
        assertEquals("Rain", restored.teams.getFirst().name);
        assertEquals("minecraft:leftovers", restored.teams.getFirst().slots.getFirst().itemId);
        assertEquals("3:12", restored.returnSlots.get(team.slots.getFirst().pokemonId));
    }

    @Test
    void malformedIdsDuplicatesAndReturnSlotsAreRepaired() {
        PlayerData data = new PlayerData();
        UUID teamId = UUID.randomUUID();
        UUID pokemonId = UUID.randomUUID();

        SavedTeam first = new SavedTeam();
        first.id = teamId.toString();
        first.name = "First";
        first.slots.add(new SavedSlot(pokemonId.toString().toUpperCase(), null));
        first.slots.add(new SavedSlot(pokemonId.toString(), "minecraft:leftovers"));
        first.slots.add(new SavedSlot("invalid", "minecraft:leftovers"));

        SavedTeam second = new SavedTeam();
        second.id = teamId.toString().toUpperCase();
        second.name = "Second";
        data.teams.add(first);
        data.teams.add(second);
        data.returnSlots.put(pokemonId.toString().toUpperCase(), "2:29");
        data.returnSlots.put(UUID.randomUUID().toString(), "999:30");

        data.normalize();

        assertNotEquals(data.teams.get(0).id, data.teams.get(1).id);
        assertEquals(1, data.teams.get(0).slots.size());
        assertEquals(pokemonId.toString(), data.teams.get(0).slots.getFirst().pokemonId);
        assertEquals("minecraft:air", data.teams.get(0).slots.getFirst().itemId);
        assertEquals("2:29", data.returnSlots.get(pokemonId.toString()));
        assertEquals(1, data.returnSlots.size());
    }

    @Test
    void interserverMigrationMergesDistinctTeamsWithoutDuplicatingIds() {
        String sharedId = UUID.randomUUID().toString();
        PlayerData firstServer = new PlayerData();
        firstServer.teams.add(team(sharedId, "Rain"));
        firstServer.teams.add(team(UUID.randomUUID().toString(), "Sun"));

        PlayerData secondServer = new PlayerData();
        secondServer.teams.add(team(sharedId, "Old Rain Copy"));
        secondServer.teams.add(team(UUID.randomUUID().toString(), "Sand"));

        PlayerData merged = TeamModels.mergeTeamLists(List.of(firstServer, secondServer));

        assertEquals(3, merged.teams.size());
        assertEquals("Rain", merged.teams.get(0).name);
        assertEquals("Sun", merged.teams.get(1).name);
        assertEquals("Sand", merged.teams.get(2).name);
        assertTrue(merged.returnSlots.isEmpty());
    }

    @Test
    void cataloguePlaceholderAndMovePresetSurviveNormalization() {
        PlayerData data = new PlayerData();
        SavedTeam team = new SavedTeam();
        team.id = UUID.randomUUID().toString();
        team.name = "Draft";
        SavedSlot placeholder = new SavedSlot(null, "cobblemon:dragonite", "minecraft:air",
                List.of("thunderpunch", "roost", "roost", "bad:move", "hurricane", "extremespeed"));
        team.slots.add(placeholder);
        data.teams.add(team);

        data = TeamJson.GSON.fromJson(TeamJson.GSON.toJson(data), PlayerData.class);
        data.normalize();

        SavedSlot restored = data.teams.getFirst().slots.getFirst();
        assertNull(restored.pokemonId);
        assertEquals("cobblemon:dragonite", restored.speciesId);
        assertTrue(restored.requiresOwnedPokemon());
        assertEquals(List.of("thunderpunch", "roost", "hurricane", "extremespeed"), restored.moveIds);
    }

    private SavedTeam team(String id, String name) {
        SavedTeam team = new SavedTeam();
        team.id = id;
        team.name = name;
        team.slots.add(new SavedSlot(UUID.randomUUID().toString(), "minecraft:air"));
        return team;
    }
}
