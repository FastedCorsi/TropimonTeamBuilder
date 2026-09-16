package fr.tropimon.teamsaver.client;

import fr.tropimon.teamsaver.model.TeamModels.SavedTeam;

/** Pointer state only: team data is not modified until a valid drop is committed. */
final class TeamListDrag {
    SavedTeam team;
    double x;
    double y;
    boolean dragging;
    private double startX;
    private double startY;

    void begin(SavedTeam selected, double mouseX, double mouseY) {
        team = selected;
        x = startX = mouseX;
        y = startY = mouseY;
        dragging = false;
    }

    void update(double mouseX, double mouseY) {
        x = mouseX;
        y = mouseY;
        if (team != null && Math.hypot(x - startX, y - startY) >= 3) dragging = true;
    }

    void clear() {
        team = null;
        dragging = false;
    }

    int scrollPage(double amount) {
        if (team == null || amount == 0) return 0;
        // Scrolling with the button held is enough to grab the team; no pointer travel is required.
        dragging = true;
        return amount < 0 ? 1 : -1;
    }

    static int insertionRow(double relativeY, int rowHeight, int visibleRows) {
        return Math.max(0, Math.min(visibleRows, (int) Math.floor((relativeY + rowHeight / 2.0) / rowHeight)));
    }

    /** The insertion boundary still includes the source row, which is removed on commit. */
    static int targetIndex(int source, int boundary, int count) {
        if (source < 0 || source >= count || boundary < 0 || boundary > count) return -1;
        return boundary > source ? boundary - 1 : boundary;
    }
}
