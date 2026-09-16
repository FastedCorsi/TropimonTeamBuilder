package fr.tropimon.teamsaver.client;

import com.cobblemon.mod.common.api.abilities.PotentialAbility;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.pokemon.abilities.HiddenAbility;
import fr.tropimon.teamsaver.model.TeamModels;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/** Immutable, lazily built view of the Cobblemon catalogue for the current game session. */
final class CobblemonCatalogueCache {
    static final CobblemonCatalogueCache INSTANCE = new CobblemonCatalogueCache();

    private volatile Snapshot snapshot;
    private volatile long revision = -1;

    private CobblemonCatalogueCache() {
    }

    Snapshot snapshot() {
        Snapshot current = snapshot;
        if (current != null && revision == ClientDataRevision.catalogue()) return current;
        synchronized (this) {
            if (snapshot == null || revision != ClientDataRevision.catalogue()) {
                long buildingRevision = ClientDataRevision.catalogue();
                snapshot = build();
                revision = buildingRevision;
            }
            return snapshot;
        }
    }

    private Snapshot build() {
        LocalCobblemonLearnsetIndex localLearnsets = LocalCobblemonLearnsetIndex.build();
        List<Entry> entries = new ArrayList<>();
        Map<Identifier, Species> speciesById = new LinkedHashMap<>();
        Map<FormData, Entry> byForm = new IdentityHashMap<>();
        for (Species species : PokemonSpecies.getImplemented()) {
            if (species == null) continue;
            speciesById.put(species.getResourceIdentifier(), species);
            Set<String> seenForms = new HashSet<>();
            add(entries, byForm, localLearnsets, species, species.getStandardForm(), seenForms);
            for (FormData form : species.getForms()) {
                add(entries, byForm, localLearnsets, species, form, seenForms);
            }
        }

        Map<String, String> itemByRankedKey = new LinkedHashMap<>();
        for (Identifier id : Registries.ITEM.getIds()) {
            itemByRankedKey.putIfAbsent(RankedUsageService.key(id.getPath()), id.toString());
        }
        return new Snapshot(List.copyOf(entries), Map.copyOf(speciesById),
                Collections.unmodifiableMap(new IdentityHashMap<>(byForm)), Map.copyOf(itemByRankedKey));
    }

    private void add(List<Entry> entries, Map<FormData, Entry> byForm,
                     LocalCobblemonLearnsetIndex localLearnsets, Species species,
                     FormData form, Set<String> seenForms) {
        if (form == null) return;
        String identity = form.getName().toLowerCase(Locale.ROOT) + "|" + String.join(",", form.getAspects());
        if (!seenForms.add(identity)) return;

        Set<String> types = new LinkedHashSet<>();
        types.add(form.getPrimaryType().getName());
        if (form.getSecondaryType() != null) types.add(form.getSecondaryType().getName());

        Map<String, Boolean> uniqueAbilities = new LinkedHashMap<>();
        for (PotentialAbility potential : form.getAbilities()) {
            String id = TeamModels.canonicalAbilityId(potential.getTemplate().getName());
            if (id != null) uniqueAbilities.merge(id, potential instanceof HiddenAbility, Boolean::logicalOr);
        }
        boolean multipleAbilities = uniqueAbilities.size() > 1;
        List<AbilitySpec> abilities = new ArrayList<>();
        uniqueAbilities.forEach((id, hidden) -> abilities.add(new AbilitySpec(id, multipleAbilities && hidden)));

        List<MoveTemplate> moves = new ArrayList<>();
        Set<String> moveIds = new LinkedHashSet<>();
        for (MoveTemplate move : form.getMoves().getAllLegalMoves()) {
            if (move != null && moveIds.add(move.getName())) moves.add(move);
        }
        for (String moveId : localLearnsets.moves(species.getResourceIdentifier(), form.getName())) {
            MoveTemplate move = Moves.getByName(moveId);
            if (move != null && moveIds.add(move.getName())) moves.add(move);
        }
        Entry entry = new Entry(species, form, Set.copyOf(types), Map.copyOf(form.getBaseStats()),
                List.copyOf(abilities), List.copyOf(moves), Set.copyOf(moveIds));
        entries.add(entry);
        byForm.put(form, entry);
    }

    record AbilitySpec(String id, boolean hidden) {
    }

    record Entry(Species species, FormData form, Set<String> types, Map<Stat, Integer> baseStats,
                 List<AbilitySpec> abilities, List<MoveTemplate> legalMoves, Set<String> legalMoveIds) {
    }

    record Snapshot(List<Entry> entries, Map<Identifier, Species> speciesById,
                    Map<FormData, Entry> byForm, Map<String, String> itemByRankedKey) {
        Entry entry(FormData form) {
            return form == null ? null : byForm.get(form);
        }

        Species species(String rawId) {
            Identifier id = Identifier.tryParse(rawId);
            return id == null ? null : speciesById.get(id);
        }
    }
}
