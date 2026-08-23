package fr.tropimon.teamsaver.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

final class CompetitiveItemRankerTest {
    @Test
    void recommendsLightClayToScreenSet() {
        var profile = new CompetitiveItemRanker.Profile(Set.of("ice"),
                Set.of("auroraveil", "blizzard"), "snowwarning", 73, 67, 75, 81, 100, 109);

        assertTrue(CompetitiveItemRanker.score("cobblemon:light_clay", profile)
                > CompetitiveItemRanker.score("cobblemon:choice_band", profile));
    }

    @Test
    void distinguishesPhysicalAndSpecialChoiceItems() {
        var physical = new CompetitiveItemRanker.Profile(Set.of("fighting"), Set.of("closecombat"),
                "guts", 90, 135, 80, 45, 70, 95);
        var special = new CompetitiveItemRanker.Profile(Set.of("psychic"), Set.of("psychic"),
                "magicguard", 70, 45, 65, 135, 95, 120);

        assertTrue(CompetitiveItemRanker.score("cobblemon:choice_band", physical)
                > CompetitiveItemRanker.score("cobblemon:choice_specs", physical));
        assertTrue(CompetitiveItemRanker.score("cobblemon:choice_specs", special)
                > CompetitiveItemRanker.score("cobblemon:choice_band", special));
    }
}
