package fr.tropimon.teamsaver.client;

import com.cobblemon.mod.common.api.storage.party.PartyPosition;
import com.cobblemon.mod.common.api.storage.pc.PCPosition;
import com.cobblemon.mod.common.client.gui.pc.PCGUI;
import com.cobblemon.mod.common.client.storage.ClientPC;
import com.cobblemon.mod.common.client.storage.ClientParty;
import com.cobblemon.mod.common.net.messages.server.storage.SwapPCPartyPokemonPacket;
import com.cobblemon.mod.common.net.messages.server.storage.party.MovePartyPokemonPacket;
import com.cobblemon.mod.common.net.messages.server.storage.party.SwapPartyPokemonPacket;
import com.cobblemon.mod.common.net.messages.server.storage.pc.MovePCPokemonToPartyPacket;
import com.cobblemon.mod.common.net.messages.server.storage.pc.MovePartyPokemonToPCPacket;
import com.cobblemon.mod.common.pokemon.Pokemon;
import fr.tropimon.teamsaver.model.TeamModels.PlayerData;
import fr.tropimon.teamsaver.model.TeamModels.SavedSlot;
import fr.tropimon.teamsaver.model.TeamModels.SavedTeam;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

final class ClientTeamApplier {
    private static final int BOX_SIZE = 30;
    private static final int TIMEOUT_TICKS = 100;
    private static Job active;

    private ClientTeamApplier() {
    }

    static boolean start(TeamManagerScreen screen, PCGUI pcGui, SavedTeam team, PlayerData data) {
        if (active != null) {
            screen.applyFailed(message("error_active"));
            return false;
        }
        if (screen.isStandalone() && requiresPcAccess(pcGui, team)) {
            screen.applyFailed(message("error_pc_required"));
            return false;
        }
        try {
            active = plan(screen, pcGui, team, data);
            return true;
        } catch (PlanException exception) {
            screen.applyFailed(exception.getMessage());
            return false;
        }
    }

    static boolean isActive(TeamManagerScreen screen) {
        return active != null && active.screen == screen;
    }

    static void cancel(TeamManagerScreen screen) {
        Job job = active;
        if (job == null || job.screen != screen) return;
        if (job.current != null) job.current.cancel.run();
        active = null;
        screen.applyCancelled(job.operationStarted);
    }

    static boolean requiresPcAccess(PCGUI gui, SavedTeam team) {
        Set<UUID> desired = new HashSet<>();
        for (SavedSlot slot : team.slots) {
            try {
                UUID id = UUID.fromString(slot.pokemonId);
                desired.add(id);
                if (gui.getParty().findByUUID(id) == null) return true;
            } catch (Exception ignored) {
                return false;
            }
        }
        int currentPartySize = 0;
        for (int slot = 0; slot < 6; slot++) {
            Pokemon pokemon = gui.getParty().get(slot);
            if (pokemon != null) {
                currentPartySize++;
                if (!desired.contains(pokemon.getUuid())) return true;
            }
        }
        return currentPartySize != desired.size();
    }

    static void tick() {
        Job job = active;
        if (job == null) return;
        try {
            tick(job);
        } catch (Exception exception) {
            TropimonTeamSaverClient.LOGGER.error("Échec inattendu pendant l'application d'une team", exception);
            if (job.current != null) job.current.cancel.run();
            failJob(job, message("error_internal"));
            active = null;
        }
    }

    private static void tick(Job job) {
        if (job.current == null) {
            job.current = job.operations.poll();
            job.waited = 0;
            if (job.current == null) {
                if (!job.itemsPlanned) {
                    if (!job.positionsCommitted) {
                        job.data.returnSlots.clear();
                        job.data.returnSlots.putAll(job.plannedReturnSlots);
                        job.positionsCommitted = true;
                        new LocalTeamRepository().save(MinecraftClient.getInstance(), job.data);
                    }
                    job.itemsPlanned = true;
                    job.itemApplier = new ClientItemApplier(job.pcGui, job.team);
                    job.operations.add(new Operation(job.itemApplier::start, () -> {
                        boolean applied = job.itemApplier.tick();
                        if (applied) job.missingItems = job.itemApplier.missingItems();
                        return applied;
                    },
                            job.itemApplier::failure, job.itemApplier::cancel, 1200, Phase.ITEMS));
                } else if (!job.movesPlanned) {
                    job.movesPlanned = true;
                    job.moveApplier = new ClientMoveApplier(job.pcGui, job.team);
                    job.operations.add(new Operation(job.moveApplier::start, job.moveApplier::tick,
                            job.moveApplier::failure, job.moveApplier::cancel, 1200, Phase.MOVES));
                } else {
                    job.screen.applyFinished(job.data, job.missingItems);
                    active = null;
                }
                return;
            }
            job.operationStarted = true;
            publishProgress(job, false);
            job.current.send.run();
            return;
        }
        publishProgress(job, false);
        String operationFailure = job.current.failure.get();
        if (operationFailure != null) {
            job.current.cancel.run();
            failJob(job, operationFailure);
            active = null;
            return;
        }
        if (job.current.applied.getAsBoolean()) {
            if (job.current.phase == Phase.POKEMON) job.completedPokemonOperations++;
            job.current = null;
            return;
        }
        if (++job.waited > job.current.timeoutTicks) {
            if (job.current.phase == Phase.POKEMON && job.current.attempts < 2) {
                job.current.attempts++;
                job.waited = 0;
                publishProgress(job, true);
                job.current.send.run();
                return;
            }
            job.current.cancel.run();
            failJob(job, message("error_timeout"));
            active = null;
        }
    }

    private static void publishProgress(Job job, boolean retry) {
        if (job.current == null) return;
        switch (job.current.phase) {
            case POKEMON -> job.screen.applyProgress("progress_pokemon",
                    Math.min(job.totalPokemonOperations, job.completedPokemonOperations + 1),
                    job.totalPokemonOperations, retry);
            case ITEMS -> job.screen.applyProgress("progress_items",
                    job.itemApplier == null ? 0 : job.itemApplier.completedTargets(),
                    job.itemApplier == null ? job.team.slots.size() : job.itemApplier.totalTargets(), retry);
            case MOVES -> job.screen.applyProgress("progress_moves",
                    job.moveApplier == null ? 0 : job.moveApplier.completedOperations(),
                    job.moveApplier == null ? 0 : job.moveApplier.totalOperations(), retry);
        }
    }

    private static void failJob(Job job, String message) {
        if (job.operationStarted) job.screen.applyFailedAfterChange(message);
        else job.screen.applyFailed(message);
    }

    private static Job plan(TeamManagerScreen screen, PCGUI gui, SavedTeam team, PlayerData data) throws PlanException {
        ClientPC pc = gui.getPc();
        ClientParty party = gui.getParty();
        Map<String, String> plannedReturnSlots = new LinkedHashMap<>(data.returnSlots);
        List<UUID> desired = new ArrayList<>();
        Set<UUID> unique = new HashSet<>();
        for (SavedSlot slot : team.slots) {
            UUID id = parse(slot.pokemonId);
            if (!unique.add(id)) throw new PlanException(message("error_duplicate"));
            if (party.findByUUID(id) == null && pc.findByUUID(id) == null) {
                throw new PlanException(message("error_pokemon_missing"));
            }
            desired.add(id);
        }
        if (desired.isEmpty()) throw new PlanException(message("error_empty"));
        String moveError = ClientMoveApplier.validate(team, party, pc);
        if (moveError != null) throw new PlanException(moveError);

        UUID[] simulatedParty = new UUID[6];
        for (int i = 0; i < 6; i++) {
            Pokemon pokemon = party.get(i);
            simulatedParty[i] = pokemon == null ? null : pokemon.getUuid();
        }
        Map<PCPosition, UUID> simulatedPc = new HashMap<>();
        Map<UUID, PCPosition> positions = new HashMap<>();
        for (int box = 0; box < pc.getBoxes().size(); box++) {
            for (int slot = 0; slot < BOX_SIZE; slot++) {
                PCPosition position = new PCPosition(box, slot);
                Pokemon pokemon = pc.get(position);
                if (pokemon != null) {
                    simulatedPc.put(position, pokemon.getUuid());
                    positions.put(pokemon.getUuid(), position);
                }
            }
        }

        Queue<Operation> operations = new ArrayDeque<>();
        for (int targetSlot = 0; targetSlot < desired.size(); targetSlot++) {
            UUID wanted = desired.get(targetSlot);
            if (wanted.equals(simulatedParty[targetSlot])) continue;
            int partySource = indexOf(simulatedParty, wanted);
            if (partySource >= 0) {
                UUID displaced = simulatedParty[targetSlot];
                if (displaced == null) {
                    int from = partySource;
                    int to = targetSlot;
                    operations.add(new Operation(
                            () -> new MovePartyPokemonPacket(wanted, new PartyPosition(from), new PartyPosition(to)).sendToServer(),
                            () -> hasParty(party, to, wanted)));
                    simulatedParty[to] = wanted;
                    simulatedParty[from] = null;
                } else {
                    int from = partySource;
                    int to = targetSlot;
                    operations.add(new Operation(
                            () -> new SwapPartyPokemonPacket(displaced, new PartyPosition(to), wanted, new PartyPosition(from)).sendToServer(),
                            () -> hasParty(party, to, wanted)));
                    simulatedParty[from] = displaced;
                    simulatedParty[to] = wanted;
                }
                continue;
            }

            PCPosition wantedPosition = positions.get(wanted);
            if (wantedPosition == null) throw new PlanException(message("error_position"));
            plannedReturnSlots.put(wanted.toString(), encode(wantedPosition));
            UUID outgoing = simulatedParty[targetSlot];
            if (outgoing == null) {
                int to = targetSlot;
                operations.add(new Operation(
                        () -> new MovePCPokemonToPartyPacket(wanted, wantedPosition, new PartyPosition(to)).sendToServer(),
                        () -> hasParty(party, to, wanted)));
                simulatedPc.remove(wantedPosition);
                positions.remove(wanted);
                simulatedParty[to] = wanted;
                continue;
            }

            PCPosition preferred = decode(plannedReturnSlots.get(outgoing.toString()));
            if (isValidPcPosition(preferred, pc) && !simulatedPc.containsKey(preferred)) {
                int fromParty = targetSlot;
                operations.add(new Operation(
                        () -> new MovePartyPokemonToPCPacket(outgoing, new PartyPosition(fromParty), preferred).sendToServer(),
                        () -> hasPc(pc, preferred, outgoing)));
                simulatedParty[targetSlot] = null;
                simulatedPc.put(preferred, outgoing);
                positions.put(outgoing, preferred);

                int toParty = targetSlot;
                operations.add(new Operation(
                        () -> new MovePCPokemonToPartyPacket(wanted, wantedPosition, new PartyPosition(toParty)).sendToServer(),
                        () -> hasParty(party, toParty, wanted)));
                simulatedPc.remove(wantedPosition);
                positions.remove(wanted);
                simulatedParty[targetSlot] = wanted;
            } else {
                int partyPosition = targetSlot;
                operations.add(new Operation(
                        () -> new SwapPCPartyPokemonPacket(outgoing, new PartyPosition(partyPosition), wanted, wantedPosition).sendToServer(),
                        () -> hasParty(party, partyPosition, wanted)));
                simulatedPc.put(wantedPosition, outgoing);
                positions.remove(wanted);
                positions.put(outgoing, wantedPosition);
                simulatedParty[targetSlot] = wanted;
                plannedReturnSlots.put(outgoing.toString(), encode(wantedPosition));
            }
        }

        for (int slot = desired.size(); slot < simulatedParty.length; slot++) {
            UUID outgoing = simulatedParty[slot];
            if (outgoing == null) continue;
            PCPosition destination = decode(plannedReturnSlots.get(outgoing.toString()));
            if (!isValidPcPosition(destination, pc) || simulatedPc.containsKey(destination)) {
                destination = firstEmpty(pc, simulatedPc);
            }
            if (destination == null) throw new PlanException(message("error_pc_full"));
            int partySlot = slot;
            PCPosition finalDestination = destination;
            operations.add(new Operation(
                    () -> new MovePartyPokemonToPCPacket(outgoing, new PartyPosition(partySlot), finalDestination).sendToServer(),
                    () -> hasPc(pc, finalDestination, outgoing)));
            simulatedParty[slot] = null;
            simulatedPc.put(destination, outgoing);
            plannedReturnSlots.put(outgoing.toString(), encode(destination));
        }
        return new Job(screen, gui, team, data, operations, plannedReturnSlots);
    }

    private static boolean hasParty(ClientParty party, int slot, UUID id) {
        Pokemon pokemon = party.get(slot);
        return pokemon != null && pokemon.getUuid().equals(id);
    }

    private static boolean hasPc(ClientPC pc, PCPosition position, UUID id) {
        Pokemon pokemon = pc.get(position);
        return pokemon != null && pokemon.getUuid().equals(id);
    }

    private static int indexOf(UUID[] values, UUID id) {
        for (int i = 0; i < values.length; i++) if (id.equals(values[i])) return i;
        return -1;
    }

    private static PCPosition firstEmpty(ClientPC pc, Map<PCPosition, UUID> occupied) {
        for (int box = 0; box < pc.getBoxes().size(); box++) {
            for (int slot = 0; slot < BOX_SIZE; slot++) {
                PCPosition position = new PCPosition(box, slot);
                if (!occupied.containsKey(position)) return position;
            }
        }
        return null;
    }

    private static boolean isValidPcPosition(PCPosition position, ClientPC pc) {
        return position != null
                && position.getBox() >= 0 && position.getBox() < pc.getBoxes().size()
                && position.getSlot() >= 0 && position.getSlot() < BOX_SIZE;
    }

    private static UUID parse(String raw) throws PlanException {
        try { return UUID.fromString(raw); }
        catch (Exception exception) { throw new PlanException(message("error_id")); }
    }

    private static String encode(PCPosition position) {
        return position.getBox() + ":" + position.getSlot();
    }

    private static PCPosition decode(String raw) {
        if (raw == null) return null;
        try {
            String[] parts = raw.split(":", 2);
            return new PCPosition(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } catch (Exception ignored) { return null; }
    }

    private static String message(String key) {
        return Text.translatable("screen.tropimon_team_saver." + key).getString();
    }

    private static final class Operation {
        final Runnable send;
        final BooleanSupplier applied;
        final Supplier<String> failure;
        final Runnable cancel;
        final int timeoutTicks;
        final Phase phase;
        int attempts;

        Operation(Runnable send, BooleanSupplier applied) {
            this(send, applied, () -> null, () -> {}, TIMEOUT_TICKS, Phase.POKEMON);
        }

        Operation(Runnable send, BooleanSupplier applied, Supplier<String> failure,
                  Runnable cancel, int timeoutTicks, Phase phase) {
            this.send = send;
            this.applied = applied;
            this.failure = failure;
            this.cancel = cancel;
            this.timeoutTicks = timeoutTicks;
            this.phase = phase;
        }
    }

    private enum Phase {
        POKEMON, ITEMS, MOVES
    }

    private static final class Job {
        final TeamManagerScreen screen;
        final PCGUI pcGui;
        final SavedTeam team;
        final PlayerData data;
        final Queue<Operation> operations;
        final Map<String, String> plannedReturnSlots;
        Operation current;
        int waited;
        int missingItems;
        boolean itemsPlanned;
        boolean movesPlanned;
        boolean positionsCommitted;
        boolean operationStarted;
        final int totalPokemonOperations;
        int completedPokemonOperations;
        ClientItemApplier itemApplier;
        ClientMoveApplier moveApplier;

        Job(TeamManagerScreen screen, PCGUI pcGui, SavedTeam team, PlayerData data,
            Queue<Operation> operations, Map<String, String> plannedReturnSlots) {
            this.screen = screen;
            this.pcGui = pcGui;
            this.team = team;
            this.data = data;
            this.operations = operations;
            this.totalPokemonOperations = operations.size();
            this.plannedReturnSlots = plannedReturnSlots;
        }
    }

    private static final class PlanException extends Exception {
        private static final long serialVersionUID = 1L;

        PlanException(String message) { super(message); }
    }
}
