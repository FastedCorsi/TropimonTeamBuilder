package fr.tropimon.teamsaver.client;

import com.cobblemon.mod.common.api.moves.BenchedMove;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.client.gui.pc.PCGUI;
import com.cobblemon.mod.common.client.storage.ClientPC;
import com.cobblemon.mod.common.client.storage.ClientParty;
import com.cobblemon.mod.common.net.messages.server.BenchMovePacket;
import com.cobblemon.mod.common.net.messages.server.RequestMoveSwapPacket;
import com.cobblemon.mod.common.pokemon.Pokemon;
import fr.tropimon.teamsaver.model.TeamModels.SavedSlot;
import fr.tropimon.teamsaver.model.TeamModels.SavedTeam;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import net.minecraft.text.Text;

/** Applies saved active/benched move presets after the requested party is in place. */
final class ClientMoveApplier {
    private static final int TIMEOUT_TICKS = 120;

    private final PCGUI gui;
    private final SavedTeam team;
    private final Queue<MoveOperation> operations = new ArrayDeque<>();
    private MoveOperation current;
    private int waited;
    private String failure;
    private boolean done;

    ClientMoveApplier(PCGUI gui, SavedTeam team) {
        this.gui = gui;
        this.team = team;
    }

    void start() {
        try {
            plan();
            if (operations.isEmpty()) done = true;
        } catch (Exception exception) {
            TropimonTeamSaverClient.LOGGER.error("Impossible de préparer les attaques de la team", exception);
            failure = message("error_move_internal");
        }
    }

    boolean tick() {
        if (done || failure != null) return done;
        if (current == null) {
            current = operations.poll();
            waited = 0;
            if (current == null) {
                done = true;
                return true;
            }
            current.send.run();
            return false;
        }
        if (current.isApplied()) {
            current = null;
            return false;
        }
        if (++waited > TIMEOUT_TICKS) failure = message("error_move_timeout");
        return false;
    }

    String failure() {
        return failure;
    }

    void cancel() {
        operations.clear();
        current = null;
    }

    static String validate(SavedTeam team, ClientParty party, ClientPC pc) {
        for (SavedSlot slot : team.slots) {
            if (slot.moveIds == null || slot.moveIds.isEmpty()) continue;
            UUID id;
            try {
                id = UUID.fromString(slot.pokemonId);
            } catch (Exception ignored) {
                return message("error_replace_required");
            }
            Pokemon pokemon = party.findByUUID(id);
            if (pokemon == null) pokemon = pc.findByUUID(id);
            if (pokemon == null) return message("error_pokemon_missing");

            List<String> active = activeIds(pokemon);
            if (slot.moveIds.size() != active.size()) {
                return Text.translatable("screen.tropimon_team_saver.error_move_count",
                        pokemon.getDisplayName(false), active.size()).getString();
            }
            Set<String> accessible = new HashSet<>(active);
            for (BenchedMove move : pokemon.getBenchedMoves()) accessible.add(move.getMoveTemplate().getName());
            Set<String> unique = new HashSet<>();
            for (String moveId : slot.moveIds) {
                MoveTemplate template = Moves.getByName(moveId);
                if (template == null || !accessible.contains(moveId) || !unique.add(moveId)) {
                    String moveName = template == null ? moveId : template.getDisplayName().getString();
                    return Text.translatable("screen.tropimon_team_saver.error_move_unavailable",
                            moveName, pokemon.getDisplayName(false)).getString();
                }
            }
        }
        return null;
    }

    private void plan() {
        for (int partySlot = 0; partySlot < team.slots.size(); partySlot++) {
            int targetPartySlot = partySlot;
            SavedSlot saved = team.slots.get(partySlot);
            if (saved.moveIds == null || saved.moveIds.isEmpty()) continue;
            Pokemon pokemon = gui.getParty().get(partySlot);
            if (pokemon == null) continue;
            UUID pokemonId = pokemon.getUuid();
            List<String> desired = new ArrayList<>(saved.moveIds);
            List<String> simulated = activeIds(pokemon);

            for (String incomingId : desired) {
                if (simulated.contains(incomingId)) continue;
                int outgoingIndex = firstUndesired(simulated, desired);
                if (outgoingIndex < 0) continue;
                String outgoingId = simulated.get(outgoingIndex);
                MoveTemplate outgoing = Moves.getByName(outgoingId);
                MoveTemplate incoming = Moves.getByName(incomingId);
                operations.add(new MoveOperation(
                        () -> new BenchMovePacket(true, pokemonId, outgoing, incoming).sendToServer(),
                        () -> {
                            Pokemon currentPokemon = gui.getParty().findByUUID(pokemonId);
                            if (currentPokemon == null) return false;
                            List<String> currentMoves = activeIds(currentPokemon);
                            return currentMoves.contains(incomingId) && !currentMoves.contains(outgoingId);
                        }));
                simulated.set(outgoingIndex, incomingId);
            }

            for (int index = 0; index < desired.size(); index++) {
                if (desired.get(index).equals(simulated.get(index))) continue;
                int other = simulated.indexOf(desired.get(index));
                if (other < 0) continue;
                int first = index;
                int second = other;
                String expectedFirst = simulated.get(second);
                String expectedSecond = simulated.get(first);
                operations.add(new MoveOperation(
                        () -> new RequestMoveSwapPacket(first, second, targetPartySlot).sendToServer(),
                        () -> {
                            Pokemon currentPokemon = gui.getParty().get(targetPartySlot);
                            if (currentPokemon == null || !currentPokemon.getUuid().equals(pokemonId)) return false;
                            List<String> currentMoves = activeIds(currentPokemon);
                            return currentMoves.size() > Math.max(first, second)
                                    && expectedFirst.equals(currentMoves.get(first))
                                    && expectedSecond.equals(currentMoves.get(second));
                        }));
                String temporary = simulated.get(first);
                simulated.set(first, simulated.get(second));
                simulated.set(second, temporary);
            }
        }
    }

    private static int firstUndesired(List<String> active, List<String> desired) {
        for (int i = 0; i < active.size(); i++) if (!desired.contains(active.get(i))) return i;
        return -1;
    }

    private static List<String> activeIds(Pokemon pokemon) {
        List<String> result = new ArrayList<>();
        for (Move move : pokemon.getMoveSet().getMoves()) result.add(move.getTemplate().getName());
        return result;
    }

    private static String message(String key) {
        return Text.translatable("screen.tropimon_team_saver." + key).getString();
    }

    private record MoveOperation(Runnable send, Check applied) {
        boolean isApplied() {
            return applied.get();
        }
    }

    @FunctionalInterface
    private interface Check {
        boolean get();
    }
}
