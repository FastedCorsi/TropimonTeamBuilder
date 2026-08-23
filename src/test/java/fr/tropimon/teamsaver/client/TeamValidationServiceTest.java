package fr.tropimon.teamsaver.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class TeamValidationServiceTest {
    @Test
    void missingItemsMovesNatureAndEvsRemainWarnings() {
        TeamValidationService.Report report = new TeamValidationService.Report(
                0, 0, 2, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0);

        assertTrue(report.readyToEquip());
        assertTrue(report.readyToSave());
    }

    @Test
    void unresolvedIllegalOrDuplicateSlotsBlockEquipment() {
        assertFalse(new TeamValidationService.Report(
                1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0).readyToEquip());
        assertFalse(new TeamValidationService.Report(
                0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0).readyToSave());
        assertFalse(new TeamValidationService.Report(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0).readyToEquip());
        assertFalse(new TeamValidationService.Report(
                0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0).readyToEquip());
    }

    @Test
    void legacyLearnedMovesAndUnspecifiedFormsStayCompatible() {
        assertFalse(TeamValidationService.hasExplicitFormId(null));
        assertFalse(TeamValidationService.hasExplicitFormId(""));
        assertFalse(TeamValidationService.isIllegalMove(true, false));
        assertTrue(TeamValidationService.isIllegalMove(false, false));
    }
}
