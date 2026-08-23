package fr.tropimon.teamsaver.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ShowdownGen9SetServiceTest {
    @Test
    void auroraVeilStylePrefersTheDedicatedGen9Set() {
        ShowdownGen9SetService.CompetitiveSet veil = new ShowdownGen9SetService.CompetitiveSet(
                "ninetalesalola", "gen9ou", "Aurora Veil", "Snow Warning", "Light Clay", "Timid",
                List.of("Aurora Veil", "Encore", "Freeze-Dry", "Moonblast"),
                Map.of("hp", 252, "def", 4, "spe", 252), 0);
        ShowdownGen9SetService.CompetitiveSet attacker = new ShowdownGen9SetService.CompetitiveSet(
                "ninetalesalola", "gen9ou", "Nasty Plot", "Snow Warning", "Heavy-Duty Boots", "Timid",
                List.of("Nasty Plot", "Freeze-Dry", "Moonblast", "Encore"),
                Map.of("spa", 252, "spd", 4, "spe", 252), 0);

        assertTrue(veil.score(RankedUsageService.BuildStyle.AURORA_VEIL, "fast_offense")
                > attacker.score(RankedUsageService.BuildStyle.AURORA_VEIL, "fast_offense"));
    }
}
