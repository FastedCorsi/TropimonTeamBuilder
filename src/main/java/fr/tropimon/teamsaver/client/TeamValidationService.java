package fr.tropimon.teamsaver.client;

import com.cobblemon.mod.common.api.moves.BenchedMove;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import fr.tropimon.teamsaver.model.TeamModels.SavedSlot;
import fr.tropimon.teamsaver.model.TeamModels.SavedTeam;
import fr.tropimon.teamsaver.model.TeamModels;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Complete read-only preflight used before saving or applying a team. */
final class TeamValidationService {
    private TeamValidationService() {
    }

    static Report validate(SavedTeam team, OwnedPokemonIndex owned,
                           CobblemonCatalogueCache.Snapshot catalogue,
                           Map<String, Integer> inventoryItems) {
        if (team == null) return Report.empty();
        int unassociated = 0;
        int missingPokemon = 0;
        int missingItems = 0;
        int movesToLearn = 0;
        int abilityMismatches = 0;
        int natureMismatches = 0;
        int evMismatches = 0;
        int duplicates = 0;
        int evOverflow = 0;
        int illegalMoves = 0;
        int invalidAbilities = 0;
        int formMismatches = 0;
        Set<UUID> uuids = new HashSet<>();
        Set<String> species = new HashSet<>();
        int repeatedSpecies = 0;
        Map<String, Integer> availableItems = new LinkedHashMap<>(inventoryItems == null ? Map.of() : inventoryItems);

        for (SavedSlot slot : team.slots) {
            UUID uuid = parse(slot.pokemonId);
            OwnedPokemonIndex.Entry ownedEntry = owned.byUuid(uuid);
            Pokemon pokemon = ownedEntry == null ? null : ownedEntry.pokemon();
            Species expectedSpecies = catalogue.species(slot.speciesId);
            boolean explicitForm = hasExplicitFormId(slot.formId);
            FormData expectedForm = !explicitForm && pokemon != null
                    ? pokemon.getForm() : findForm(expectedSpecies, slot.formId);
            if (expectedSpecies != null && slot.formId != null && !slot.formId.isBlank()
                    && !hasExplicitForm(expectedSpecies, slot.formId)) formMismatches++;
            CobblemonCatalogueCache.Entry catalogueEntry = catalogue.entry(expectedForm);
            if (slot.speciesId != null && !species.add(slot.speciesId)) repeatedSpecies++;

            if (uuid == null) {
                unassociated++;
            } else if (!uuids.add(uuid)) {
                duplicates++;
            }
            if (uuid != null && pokemon == null) missingPokemon++;

            int evTotal = slot.evs == null ? 0 : slot.evs.values().stream()
                    .filter(value -> value != null && value > 0).mapToInt(Integer::intValue).sum();
            if (evTotal > 510 || slot.evs != null && slot.evs.values().stream()
                    .anyMatch(value -> value != null && value > 252)) evOverflow++;

            if (catalogueEntry != null) {
                if (slot.abilityId != null && catalogueEntry.abilities().stream()
                        .noneMatch(ability -> ability.id().equals(slot.abilityId))) invalidAbilities++;
            }

            if (pokemon == null) {
                if (slot.moveIds != null && catalogueEntry != null) {
                    for (String move : slot.moveIds) {
                        if (isIllegalMove(false, catalogueEntry.legalMoveIds().contains(move))) illegalMoves++;
                    }
                }
                continue;
            }
            if (expectedSpecies != null && !pokemon.getSpecies().getResourceIdentifier()
                    .equals(expectedSpecies.getResourceIdentifier())) formMismatches++;
            else if (explicitForm && expectedForm != null && !sameForm(pokemon.getForm(), expectedForm)) {
                formMismatches++;
            }

            String actualAbility = TeamModels.canonicalAbilityId(pokemon.getAbility().getTemplate().getName());
            String wantedAbility = TeamModels.canonicalAbilityId(slot.abilityId);
            if (wantedAbility != null && !wantedAbility.equals(actualAbility)) abilityMismatches++;
            String wantedNature = canonicalId(slot.natureId);
            String actualNature = canonicalId(pokemon.getEffectiveNature().getName().getPath());
            if (wantedNature != null && !wantedNature.equals(actualNature)) natureMismatches++;
            if (hasPlannedEvs(slot.evs) && !matchesEvs(slot.evs, pokemon)) evMismatches++;
            Set<String> learned = new HashSet<>();
            for (Move move : pokemon.getMoveSet().getMoves()) learned.add(move.getTemplate().getName());
            for (BenchedMove move : pokemon.getBenchedMoves()) learned.add(move.getMoveTemplate().getName());
            if (slot.moveIds != null) {
                for (String move : slot.moveIds) {
                    boolean alreadyLearned = learned.contains(move);
                    if (!alreadyLearned) movesToLearn++;
                    if (catalogueEntry != null
                            && isIllegalMove(alreadyLearned, catalogueEntry.legalMoveIds().contains(move))) {
                        illegalMoves++;
                    }
                }
            }

            String wantedItem = normalizeItem(slot.itemId);
            String currentItem = pokemon.getHeldItem$common().isEmpty() ? "minecraft:air"
                    : net.minecraft.registry.Registries.ITEM.getId(pokemon.getHeldItem$common().getItem()).toString();
            if (!wantedItem.equals("minecraft:air") && !wantedItem.equals(currentItem)) {
                int count = availableItems.getOrDefault(wantedItem, 0);
                if (count <= 0) missingItems++;
                else availableItems.put(wantedItem, count - 1);
            }
        }
        return new Report(unassociated, missingPokemon, missingItems, movesToLearn, abilityMismatches,
                natureMismatches, evMismatches, duplicates, repeatedSpecies, evOverflow,
                illegalMoves, invalidAbilities, formMismatches);
    }

    private static boolean hasPlannedEvs(Map<String, Integer> evs) {
        return evs != null && evs.values().stream().anyMatch(value -> value != null && value > 0);
    }

    private static boolean matchesEvs(Map<String, Integer> planned, Pokemon pokemon) {
        Map<String, Stat> stats = Map.of(
                "hp", Stats.HP, "atk", Stats.ATTACK, "def", Stats.DEFENCE,
                "spa", Stats.SPECIAL_ATTACK, "spd", Stats.SPECIAL_DEFENCE, "spe", Stats.SPEED);
        for (Map.Entry<String, Stat> entry : stats.entrySet()) {
            Integer rawExpected = planned.get(entry.getKey());
            int expected = rawExpected == null ? 0 : Math.max(0, rawExpected);
            if (pokemon.getEvs().getOrDefault(entry.getValue()) != expected) return false;
        }
        return true;
    }

    private static String canonicalId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim().toLowerCase(java.util.Locale.ROOT);
        int namespace = value.indexOf(':');
        return namespace >= 0 ? value.substring(namespace + 1) : value;
    }

    private static FormData findForm(Species species, String rawForm) {
        if (species == null) return null;
        if (rawForm == null || rawForm.isBlank()) return species.getStandardForm();
        FormData form = species.getFormByName(rawForm);
        if (form == null) form = species.getFormByShowdownId(rawForm);
        return form == null ? species.getStandardForm() : form;
    }

    private static boolean hasExplicitForm(Species species, String rawForm) {
        return species.getFormByName(rawForm) != null || species.getFormByShowdownId(rawForm) != null;
    }

    private static boolean sameForm(FormData first, FormData second) {
        if (first == second) return true;
        return first != null && second != null && first.getName().equalsIgnoreCase(second.getName())
                && first.getAspects().equals(second.getAspects());
    }

    static boolean hasExplicitFormId(String rawForm) {
        return rawForm != null && !rawForm.isBlank();
    }

    static boolean isIllegalMove(boolean learned, boolean catalogueLegal) {
        return !learned && !catalogueLegal;
    }

    private static UUID parse(String raw) {
        try {
            return raw == null ? null : UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String normalizeItem(String raw) {
        return raw == null || raw.isBlank() ? "minecraft:air" : raw;
    }

    record Report(int unassociated, int missingPokemon, int missingItems, int movesToLearn,
                  int abilityMismatches, int natureMismatches, int evMismatches,
                  int duplicates, int repeatedSpecies, int evOverflow, int illegalMoves,
                  int invalidAbilities, int formMismatches) {
        static Report empty() {
            return new Report(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        boolean readyToEquip() {
            return unassociated == 0 && missingPokemon == 0 && duplicates == 0 && evOverflow == 0
                    && illegalMoves == 0 && invalidAbilities == 0 && formMismatches == 0
                    && abilityMismatches == 0;
        }

        boolean readyToSave() {
            return duplicates == 0 && evOverflow == 0 && illegalMoves == 0
                    && invalidAbilities == 0 && formMismatches == 0 && abilityMismatches == 0;
        }

    }
}
