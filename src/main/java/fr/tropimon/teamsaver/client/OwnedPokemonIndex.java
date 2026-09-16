package fr.tropimon.teamsaver.client;

import com.cobblemon.mod.common.api.storage.StorePosition;
import com.cobblemon.mod.common.api.storage.party.PartyPosition;
import com.cobblemon.mod.common.api.storage.pc.PCPosition;
import com.cobblemon.mod.common.client.gui.pc.PCGUI;
import com.cobblemon.mod.common.pokemon.Pokemon;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** One-pass index of the party and PC, refreshed only at explicit storage boundaries. */
final class OwnedPokemonIndex {
    private final Map<UUID, Entry> byUuid = new LinkedHashMap<>();
    private final Map<String, List<Entry>> bySpecies = new LinkedHashMap<>();
    private List<Entry> all = List.of();
    private long revision = -1;
    private long pokemonRevision = -1;

    boolean refreshIfChanged(PCGUI gui) {
        if (revision != ClientDataRevision.storage()) {
            refresh(gui);
            return true;
        }
        if (pokemonRevision == ClientDataRevision.pokemon()) return false;
        // Evolution/form updates mutate Pokémon in place. Rebuild the species index
        // from our existing references, without reading every PC box again.
        List<Entry> previous = all;
        byUuid.clear();
        bySpecies.clear();
        List<Entry> collected = new ArrayList<>(previous.size());
        for (Entry entry : previous) add(collected, entry.pokemon, entry.position, entry.party, entry.box);
        finishRefresh(collected);
        return true;
    }

    void refresh(PCGUI gui) {
        byUuid.clear();
        bySpecies.clear();
        List<Entry> collected = new ArrayList<>();
        for (int slot = 0; slot < 6; slot++) {
            Pokemon pokemon = gui.getParty().get(slot);
            if (pokemon != null) add(collected, pokemon, new PartyPosition(slot), true, 0);
        }
        for (int box = 0; box < gui.getPc().getBoxes().size(); box++) {
            List<Pokemon> slots = gui.getPc().getBoxes().get(box).getSlots();
            for (int slot = 0; slot < slots.size(); slot++) {
                Pokemon pokemon = slots.get(slot);
                if (pokemon != null) add(collected, pokemon, new PCPosition(box, slot), false, box);
            }
        }
        revision = ClientDataRevision.storage();
        finishRefresh(collected);
    }

    private void finishRefresh(List<Entry> collected) {
        all = List.copyOf(collected);
        bySpecies.replaceAll((key, value) -> List.copyOf(value));
        pokemonRevision = ClientDataRevision.pokemon();
    }

    private void add(List<Entry> collected, Pokemon pokemon, StorePosition position, boolean party, int box) {
        String speciesId = pokemon.getSpecies().getResourceIdentifier().toString();
        Entry entry = new Entry(pokemon, position, speciesId, party, box);
        collected.add(entry);
        byUuid.put(pokemon.getUuid(), entry);
        bySpecies.computeIfAbsent(speciesId, ignored -> new ArrayList<>()).add(entry);
    }

    Entry byUuid(UUID uuid) {
        return uuid == null ? null : byUuid.get(uuid);
    }

    List<Entry> bySpecies(String speciesId) {
        return speciesId == null ? List.of() : bySpecies.getOrDefault(speciesId, List.of());
    }

    List<Entry> all() {
        return all;
    }

    record Entry(Pokemon pokemon, StorePosition position, String speciesId, boolean party, int box) {
    }
}
