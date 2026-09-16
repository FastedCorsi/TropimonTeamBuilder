package fr.tropimon.teamsaver.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.tropimon.teamsaver.TeamJson;
import fr.tropimon.teamsaver.model.TeamModels.PlayerData;
import fr.tropimon.teamsaver.model.TeamModels.SavedTeam;
import java.util.List;
import org.junit.jupiter.api.Test;

final class TeamListDragTest {
    @Test
    void wheelGrabsTheTeamWithoutMovingThePointerAndSupportsBothDirections() {
        TeamListDrag drag = new TeamListDrag();
        SavedTeam selected = team("Held team");
        drag.begin(selected, 15, 61);
        assertFalse(drag.dragging);
        assertEquals(1, drag.scrollPage(-1));
        assertTrue(drag.dragging);
        assertSame(selected, drag.team);
        assertEquals(15, drag.x);
        assertEquals(61, drag.y);
        assertEquals(-1, drag.scrollPage(1));
        assertSame(selected, drag.team);
    }

    @Test
    void wheelWithoutHeldTeamOrVerticalMovementDoesNotStartADrag() {
        TeamListDrag drag = new TeamListDrag();
        assertEquals(0, drag.scrollPage(-1));
        assertFalse(drag.dragging);
        drag.begin(team("Clicked team"), 15, 61);
        assertEquals(0, drag.scrollPage(0));
        assertFalse(drag.dragging);
        drag.clear();
        assertEquals(0, drag.scrollPage(1));
        assertNull(drag.team);
        assertFalse(drag.dragging);
    }

    @Test
    void scrollingAloneLeavesTheStoredOrderUntouchedUntilDrop() {
        PlayerData data = new PlayerData();
        for (int i = 0; i < 20; i++) data.teams.add(team("Team " + i));
        String initial = TeamJson.GSON.toJson(data);
        TeamListDrag drag = new TeamListDrag();
        SavedTeam held = data.teams.getFirst();
        drag.begin(held, 15, 61);
        assertEquals(1, drag.scrollPage(-1));
        assertEquals(initial, TeamJson.GSON.toJson(data));
        int boundaryOnSecondPage = 8 + TeamListDrag.insertionRow(19, 19, 8);
        int target = TeamListDrag.targetIndex(0, boundaryOnSecondPage, data.teams.size());
        drag.clear();
        assertTrue(data.moveTeam(0, target));
        assertSame(held, data.teams.get(8));
    }

    @Test
    void clickAndSmallJitterDoNotStartADrag() {
        TeamListDrag drag = new TeamListDrag();
        SavedTeam team = team("First");
        drag.begin(team, 10, 20);
        drag.update(11, 21);
        assertSame(team, drag.team);
        assertFalse(drag.dragging);
        drag.update(10, 23);
        assertTrue(drag.dragging);
        drag.update(10, 20);
        assertTrue(drag.dragging); // Returning to the press point does not lose the gesture.
    }

    @Test
    void cancelAndNextGestureDoNotReuseThePreviousDrag() {
        TeamListDrag drag = new TeamListDrag();
        drag.begin(team("First"), 10, 20);
        drag.update(30, 70);
        drag.clear();
        assertNull(drag.team);
        assertFalse(drag.dragging);
        drag.update(10, 100);
        assertFalse(drag.dragging);
        SavedTeam second = team("Second");
        drag.begin(second, 40, 80);
        assertSame(second, drag.team);
        assertFalse(drag.dragging);
        assertEquals(40, drag.x);
        assertEquals(80, drag.y);
    }

    @Test
    void insertionUsesRowMidpointsAndClampsToTheVisibleRows() {
        assertEquals(0, TeamListDrag.insertionRow(-4, 19, 8));
        assertEquals(0, TeamListDrag.insertionRow(9, 19, 8));
        assertEquals(1, TeamListDrag.insertionRow(9.5, 19, 8));
        assertEquals(1, TeamListDrag.insertionRow(18, 19, 8));
        assertEquals(8, TeamListDrag.insertionRow(152, 19, 8));
        assertEquals(3, TeamListDrag.insertionRow(152, 19, 3)); // Short last page.
        assertEquals(0, TeamListDrag.insertionRow(10, 19, 0));
    }

    @Test
    void sourceRemovalIsAccountedForInBothDirectionsAndAcrossPages() {
        assertEquals(0, TeamListDrag.targetIndex(9, 0, 20));
        assertEquals(19, TeamListDrag.targetIndex(0, 20, 20));
        assertEquals(8, TeamListDrag.targetIndex(1, 9, 20));
        assertEquals(3, TeamListDrag.targetIndex(17, 3, 20));
        assertEquals(5, TeamListDrag.targetIndex(5, 5, 20));
        assertEquals(5, TeamListDrag.targetIndex(5, 6, 20));
    }

    @Test
    void invalidDropAndStaleSourceCannotProduceATarget() {
        assertEquals(-1, TeamListDrag.targetIndex(1, -1, 3));
        assertEquals(-1, TeamListDrag.targetIndex(1, 4, 3));
        assertEquals(-1, TeamListDrag.targetIndex(-1, 0, 3));
        assertEquals(-1, TeamListDrag.targetIndex(3, 0, 3));
        assertEquals(-1, TeamListDrag.targetIndex(0, 0, 0));
    }

    @Test
    void onlyCommittingTheDropChangesTheListAndItInsertsInsteadOfSwapping() {
        PlayerData data = new PlayerData();
        data.teams.addAll(List.of(team("A"), team("B"), team("C"), team("D")));
        String initial = TeamJson.GSON.toJson(data);
        TeamListDrag drag = new TeamListDrag();
        drag.begin(data.teams.get(1), 10, 20);
        drag.update(10, 100);
        assertEquals(initial, TeamJson.GSON.toJson(data));
        drag.clear();
        assertEquals(initial, TeamJson.GSON.toJson(data));
        assertTrue(data.moveTeam(1, TeamListDrag.targetIndex(1, 4, data.teams.size())));
        assertEquals(List.of("A", "C", "D", "B"), data.teams.stream().map(t -> t.name).toList());
        PlayerData restored = TeamJson.GSON.fromJson(TeamJson.GSON.toJson(data), PlayerData.class);
        restored.normalize();
        assertEquals(List.of("A", "C", "D", "B"), restored.teams.stream().map(t -> t.name).toList());
    }

    private static SavedTeam team(String name) {
        SavedTeam result = new SavedTeam();
        result.name = name;
        return result;
    }
}
