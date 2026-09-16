package fr.tropimon.teamsaver.client;

import static org.junit.jupiter.api.Assertions.*;
import fr.tropimon.teamsaver.model.TeamModels.SavedSlot;
import fr.tropimon.teamsaver.model.TeamModels.SavedTeam;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

final class ReadModelCacheTest {
    @Test void unchangedDependenciesComputeOnlyOnce() {
        ReadModelCache<String, Object> cache = new ReadModelCache<>();
        AtomicInteger calls = new AtomicInteger();
        var supplier = (java.util.function.Supplier<Object>) () -> { calls.incrementAndGet(); return new Object(); };
        Object first = cache.get("same", supplier);
        for (int i = 0; i < 500; i++) assertSame(first, cache.get(new String("same"), supplier));
        assertEquals(1, calls.get());
        assertNotSame(first, cache.get("changed", supplier));
        cache.clear();
        cache.get("changed", supplier);
        assertEquals(3, calls.get());
    }

    @Test void failedComputationIsRetriedNotCached() {
        ReadModelCache<String, String> cache = new ReadModelCache<>();
        assertThrows(IllegalStateException.class, () -> cache.get("key", () -> {throw new IllegalStateException();}));
        assertEquals("success", cache.get("key", () -> "success"));
    }

    @Test void everySetFieldAndListOrderInvalidatesTheSnapshot() {
        List<Consumer<SavedSlot>> mutations = List.of(
                s -> s.pokemonId = "other", s -> s.speciesId = "cobblemon:garchomp",
                s -> s.formId = "Hisuian", s -> s.itemId = "cobblemon:air_balloon",
                s -> s.abilityId = "levitate", s -> s.natureId = "bold",
                s -> s.moveIds.add("stealthrock"), s -> s.evs.put("spe", 252));
        for (Consumer<SavedSlot> mutate : mutations) {
            SavedTeam team = team();
            TeamReadKey before = capture(team);
            mutate.accept(team.slots.getFirst());
            assertNotEquals(before, capture(team));
        }
        SavedTeam team = team();
        team.slots.add(new SavedSlot(null, "cobblemon:dragonite", "minecraft:air", List.of("roost")));
        TeamReadKey before = capture(team);
        java.util.Collections.swap(team.slots, 0, 1);
        assertNotEquals(before, capture(team));
        team.slots.removeLast();
        assertNotEquals(before, capture(team));
    }

    @Test void storagePokemonCatalogueAndInventoryAreIndependentDependencies() {
        SavedTeam team = team();
        TeamReadKey before = capture(team);
        assertNotEquals(before, TeamReadKey.capture(team, 2, 1, 1, Map.of()));
        assertNotEquals(before, TeamReadKey.capture(team, 1, 2, 1, Map.of()));
        assertNotEquals(before, TeamReadKey.capture(team, 1, 1, 2, Map.of()));
        assertNotEquals(before, TeamReadKey.capture(team, 1, 1, 1, Map.of("cobblemon:leftovers", 1)));
        team.name = "Name only";
        assertEquals(before, capture(team), "Renaming must not invalidate gameplay validation");
    }

    static SavedTeam team() {
        SavedTeam team = new SavedTeam();
        team.slots.add(new SavedSlot(null, "cobblemon:glimmora", "cobblemon:focus_sash", List.of("powergem")));
        return team;
    }
    private static TeamReadKey capture(SavedTeam team) { return TeamReadKey.capture(team, 1, 1, 1, Map.of()); }
}
