package fr.tropimon.teamsaver.client;

import com.cobblemon.mod.common.client.gui.interact.wheel.InteractTypePokemon;
import com.cobblemon.mod.common.client.gui.pc.PCGUI;
import com.cobblemon.mod.common.net.messages.server.SendOutPokemonPacket;
import com.cobblemon.mod.common.net.messages.server.pokemon.interact.InteractPokemonPacket;
import com.cobblemon.mod.common.pokemon.Pokemon;
import fr.tropimon.teamsaver.model.TeamModels.SavedSlot;
import fr.tropimon.teamsaver.model.TeamModels.SavedTeam;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

/** Applies saved held items using only interactions understood by a standard Cobblemon server. */
final class ClientItemApplier {
    private static final int MIN_RECALL_AGE_TICKS = 3;
    private static final int RECALL_RETRY_TICKS = 10;
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final PCGUI gui;
    private final SavedTeam team;
    private int targetSlot;
    private int originalSelectedSlot;
    private State state = State.NEXT_TARGET;
    private String expectedItem = "minecraft:air";
    private Pokemon target;
    private Pokemon actor;
    private int actorPartySlot;
    private boolean actorWasSent;
    private HandSwap handSwap;
    private final Set<UUID> hiddenPokemonIds = new HashSet<>();
    private final Set<Integer> automatedPartySlots = new HashSet<>();
    private final Set<Integer> recallRequestedSlots = new HashSet<>();
    private boolean recallRequested;
    private int recallWaitTicks;
    private int offeredCount;
    private int missingItems;
    private String failure;

    ClientItemApplier(PCGUI gui, SavedTeam team) {
        this.gui = gui;
        this.team = team;
    }

    void start() {
        if (client.player == null) {
            fail("error_item_target");
            return;
        }
        originalSelectedSlot = client.player.getInventory().selectedSlot;
        state = State.NEXT_TARGET;
    }

    boolean tick() {
        if (failure != null || state == State.DONE) return state == State.DONE;
        if (client.player == null || client.world == null) {
            fail("error_item_target");
            return false;
        }

        switch (state) {
            case NEXT_TARGET -> nextTarget();
            case ACQUIRE_ITEM -> acquireExpectedItem();
            case WAIT_EMPTY_HAND -> {
                if (client.player.getMainHandStack().isEmpty()) beginActorInteraction();
            }
            case WAIT_ITEM_HAND -> {
                if (itemId(client.player.getMainHandStack()).equals(expectedItem)) beginGive();
            }
            case WAIT_ACTOR_ACTIVE -> {
                if (actor != null && actor.getEntity() != null) {
                    hideAutomatedEntity(actor);
                    if (readyForItemInteraction(actor)) interactToTake();
                }
            }
            case WAIT_TAKEN -> {
                if (actor != null && actor.getHeldItem$common().isEmpty()) {
                    finishTake();
                }
            }
            case WAIT_TAKE_RECALL -> {
                if (actor == null || actor.getEntity() == null) {
                    automatedPartySlots.remove(actorPartySlot);
                    recallRequestedSlots.remove(actorPartySlot);
                    recallRequested = false;
                    recallWaitTicks = 0;
                    clearHiddenPokemon(actor);
                    completeTake();
                } else {
                    requestRecallWhenReady(actorPartySlot, actor);
                }
            }
            case WAIT_TARGET_ACTIVE -> {
                if (target != null && target.getEntity() != null) {
                    hideAutomatedEntity(target);
                    if (readyForItemInteraction(target)) interactToGive();
                }
            }
            case WAIT_GIVEN -> {
                if (target != null && itemId(target.getHeldItem$common()).equals(expectedItem)
                        && handTransferConfirmed()) {
                    finishGive();
                }
            }
            case WAIT_GIVE_RECALL -> {
                if (target == null || target.getEntity() == null) {
                    automatedPartySlots.remove(targetSlot);
                    recallRequestedSlots.remove(targetSlot);
                    recallRequested = false;
                    recallWaitTicks = 0;
                    clearHiddenPokemon(target);
                    completeTarget();
                } else {
                    requestRecallWhenReady(targetSlot, target);
                }
            }
            case WAIT_SKIP_RECALL -> {
                if (target == null || target.getEntity() == null) {
                    automatedPartySlots.remove(targetSlot);
                    recallRequestedSlots.remove(targetSlot);
                    recallRequested = false;
                    recallWaitTicks = 0;
                    clearHiddenPokemon(target);
                    completeTarget();
                } else {
                    hideAutomatedEntity(target);
                    requestRecallWhenReady(targetSlot, target);
                }
            }
        }
        return state == State.DONE;
    }

    String failure() {
        return failure;
    }

    int missingItems() {
        return missingItems;
    }

    int completedTargets() {
        return Math.min(targetSlot, team.slots.size());
    }

    int totalTargets() {
        return team.slots.size();
    }

    void cancel() {
        recallAutomatedPokemonIfNeeded();
        clearHiddenEntity();
        restoreSelection();
        state = State.DONE;
    }

    private void nextTarget() {
        if (targetSlot >= team.slots.size()) {
            restoreSelection();
            state = State.DONE;
            return;
        }
        SavedSlot saved = team.slots.get(targetSlot);
        UUID wanted;
        try {
            wanted = UUID.fromString(saved.pokemonId);
        } catch (Exception exception) {
            fail("error_item_target");
            return;
        }
        target = gui.getParty().get(targetSlot);
        if (target == null || !target.getUuid().equals(wanted)) {
            fail("error_item_target");
            return;
        }
        expectedItem = normalize(saved.itemId);
        if (itemId(target.getHeldItem$common()).equals(expectedItem)) {
            targetSlot++;
            return;
        }
        if (expectedItem.equals("minecraft:air")) beginTake(target, targetSlot);
        else state = State.ACQUIRE_ITEM;
    }

    private void acquireExpectedItem() {
        if (expectedItem.equals("minecraft:air")) {
            completeTarget();
            return;
        }
        int inventorySlot = findInventoryItem(expectedItem);
        if (inventorySlot >= 0) {
            ItemStack source = client.player.getInventory().getStack(inventorySlot);
            if (!target.getHeldItem$common().isEmpty() && source.getCount() > 1
                    && !hasInventorySpaceFor(target.getHeldItem$common())) {
                fail("error_inventory_full");
                return;
            }
            prepareItemInHand(inventorySlot);
            return;
        }
        skipMissingItem();
    }

    private void beginTake(Pokemon pokemon, int partySlot) {
        actor = pokemon;
        actorPartySlot = partySlot;
        if (!prepareEmptyHand()) return;
        beginActorInteraction();
    }

    private void beginActorInteraction() {
        actorWasSent = automatedPartySlots.contains(actorPartySlot);
        if (actorWasSent) {
            state = State.WAIT_ACTOR_ACTIVE;
        } else if (actor.getEntity() == null) {
            prepareAutomatedSendout(actor, actorPartySlot);
            new SendOutPokemonPacket(actorPartySlot).sendToServer();
            actorWasSent = true;
            state = State.WAIT_ACTOR_ACTIVE;
        } else interactToTake();
    }

    private void interactToTake() {
        if (actor == null || actor.getEntity() == null || !client.player.getMainHandStack().isEmpty()) return;
        if (actorWasSent) hideAutomatedEntity(actor);
        new InteractPokemonPacket(actor.getEntity().getUuid(), InteractTypePokemon.HELD_ITEM).sendToServer();
        state = State.WAIT_TAKEN;
    }

    private void finishTake() {
        restoreHandSwap();
        if (actorWasSent && actor.getEntity() != null) {
            state = State.WAIT_TAKE_RECALL;
        } else completeTake();
    }

    private void completeTake() {
        completeTarget();
    }

    private void prepareItemInHand(int inventorySlot) {
        if (inventorySlot < 9) {
            if (client.player.getInventory().selectedSlot == inventorySlot) {
                beginGive();
                return;
            }
            client.player.getInventory().selectedSlot = inventorySlot;
            state = State.WAIT_ITEM_HAND;
            return;
        }
        int hotbar = firstEmptyHotbar();
        if (hotbar < 0) hotbar = client.player.getInventory().selectedSlot;
        if (!swapInventoryWithHotbar(inventorySlot, hotbar)) return;
        handSwap = new HandSwap(inventorySlot, hotbar);
        client.player.getInventory().selectedSlot = hotbar;
        state = State.WAIT_ITEM_HAND;
    }

    private void beginGive() {
        offeredCount = client.player.getMainHandStack().getCount();
        actorWasSent = automatedPartySlots.contains(targetSlot);
        if (actorWasSent) {
            state = State.WAIT_TARGET_ACTIVE;
        } else if (target.getEntity() == null) {
            prepareAutomatedSendout(target, targetSlot);
            new SendOutPokemonPacket(targetSlot).sendToServer();
            actorWasSent = true;
            state = State.WAIT_TARGET_ACTIVE;
        } else interactToGive();
    }

    private void interactToGive() {
        if (target == null || target.getEntity() == null
                || !itemId(client.player.getMainHandStack()).equals(expectedItem)) return;
        if (actorWasSent) hideAutomatedEntity(target);
        new InteractPokemonPacket(target.getEntity().getUuid(), InteractTypePokemon.HELD_ITEM).sendToServer();
        state = State.WAIT_GIVEN;
    }

    private void finishGive() {
        restoreHandSwap();
        if (actorWasSent && target.getEntity() != null) {
            state = State.WAIT_GIVE_RECALL;
        } else completeTarget();
    }

    private void completeTarget() {
        targetSlot++;
        state = State.NEXT_TARGET;
    }

    private boolean prepareEmptyHand() {
        if (client.player.getMainHandStack().isEmpty()) return true;
        int emptyHotbar = firstEmptyHotbar();
        if (emptyHotbar >= 0) {
            client.player.getInventory().selectedSlot = emptyHotbar;
            state = State.WAIT_EMPTY_HAND;
            return false;
        }
        int emptyInventory = firstEmptyMainInventory();
        if (emptyInventory < 0) {
            fail("error_inventory_full");
            return false;
        }
        int hotbar = client.player.getInventory().selectedSlot;
        if (!swapInventoryWithHotbar(emptyInventory, hotbar)) return false;
        handSwap = new HandSwap(emptyInventory, hotbar);
        state = State.WAIT_EMPTY_HAND;
        return false;
    }

    private void restoreHandSwap() {
        if (handSwap == null || client.player == null) return;
        swapInventoryWithHotbar(handSwap.inventorySlot, handSwap.hotbarSlot);
        handSwap = null;
    }

    private void restoreSelection() {
        restoreHandSwap();
        if (client.player != null) client.player.getInventory().selectedSlot = originalSelectedSlot;
    }

    private boolean swapInventoryWithHotbar(int inventoryIndex, int hotbarIndex) {
        if (client.interactionManager == null || client.player == null) {
            fail("error_item_target");
            return false;
        }
        int screenSlot = inventoryIndex < 9 ? 36 + inventoryIndex : inventoryIndex;
        client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, screenSlot, hotbarIndex,
                SlotActionType.SWAP, client.player);
        return true;
    }

    private int findInventoryItem(String wanted) {
        int fallback = -1;
        for (int slot = 0; slot < Math.min(36, client.player.getInventory().size()); slot++) {
            ItemStack stack = client.player.getInventory().getStack(slot);
            if (itemId(stack).equals(wanted)) {
                if (stack.getCount() == 1) return slot;
                if (fallback < 0) fallback = slot;
            }
        }
        return fallback;
    }

    private boolean hasInventorySpaceFor(ItemStack returnedItem) {
        for (int slot = 0; slot < Math.min(36, client.player.getInventory().size()); slot++) {
            ItemStack stack = client.player.getInventory().getStack(slot);
            if (stack.isEmpty()) return true;
            if (ItemStack.areItemsAndComponentsEqual(stack, returnedItem)
                    && stack.getCount() < stack.getMaxCount()) return true;
        }
        return false;
    }

    private boolean handTransferConfirmed() {
        if (client.player.getAbilities().creativeMode) return true;
        ItemStack hand = client.player.getMainHandStack();
        return !itemId(hand).equals(expectedItem) || hand.getCount() < offeredCount;
    }

    private int firstEmptyHotbar() {
        for (int slot = 0; slot < 9; slot++) {
            if (client.player.getInventory().getStack(slot).isEmpty()) return slot;
        }
        return -1;
    }

    private int firstEmptyMainInventory() {
        for (int slot = 9; slot < 36; slot++) {
            if (client.player.getInventory().getStack(slot).isEmpty()) return slot;
        }
        return -1;
    }

    private void fail(String key) {
        recallAutomatedPokemonIfNeeded();
        clearHiddenEntity();
        restoreSelection();
        failure = Text.translatable("screen.tropimon_team_saver." + key).getString();
    }

    private void skipMissingItem() {
        missingItems++;
        TropimonTeamSaverClient.LOGGER.warn("Objet de team introuvable pendant l'application: {}", expectedItem);
        restoreHandSwap();
        if (automatedPartySlots.contains(targetSlot)) {
            recallRequested = false;
            recallWaitTicks = 0;
            state = State.WAIT_SKIP_RECALL;
        } else {
            completeTarget();
        }
    }

    private void hideAutomatedEntity(Pokemon pokemon) {
        if (pokemon == null || !automatedPartySlots.contains(partySlotOf(pokemon))) return;
        UUID pokemonId = pokemon.getUuid();
        hiddenPokemonIds.add(pokemonId);
        AutomationVisibility.hide(pokemonId);
    }

    private void prepareAutomatedSendout(Pokemon pokemon, int partySlot) {
        if (pokemon == null) return;
        automatedPartySlots.add(partySlot);
        recallRequested = false;
        recallWaitTicks = 0;
        UUID pokemonId = pokemon.getUuid();
        hiddenPokemonIds.add(pokemonId);
        AutomationVisibility.hide(pokemonId);
    }

    private void requestRecallWhenReady(int partySlot, Pokemon pokemon) {
        if (pokemon == null || pokemon.getEntity() == null || pokemon.getEntity().isBusy()) return;
        if (recallRequested) {
            if (++recallWaitTicks < RECALL_RETRY_TICKS) return;
        } else if (pokemon.getEntity().getTicksLived() < MIN_RECALL_AGE_TICKS) {
            return;
        }
        new SendOutPokemonPacket(partySlot).sendToServer();
        recallRequested = true;
        recallWaitTicks = 0;
        recallRequestedSlots.add(partySlot);
        TropimonTeamSaverClient.LOGGER.debug("Rappel automatique demandé pour le slot {}", partySlot);
    }

    private void recallAutomatedPokemonIfNeeded() {
        if (client.player == null) return;
        for (int partySlot : Set.copyOf(automatedPartySlots)) {
            if (recallRequestedSlots.contains(partySlot)) continue;
            Pokemon pokemon = gui.getParty().get(partySlot);
            if (pokemon == null || pokemon.getEntity() == null) continue;
            new SendOutPokemonPacket(partySlot).sendToServer();
            recallRequestedSlots.add(partySlot);
        }
    }

    private void clearHiddenEntity() {
        for (UUID pokemonId : hiddenPokemonIds) AutomationVisibility.show(pokemonId);
        hiddenPokemonIds.clear();
        automatedPartySlots.clear();
        recallRequestedSlots.clear();
    }

    private void clearHiddenPokemon(Pokemon pokemon) {
        if (pokemon == null) return;
        UUID pokemonId = pokemon.getUuid();
        AutomationVisibility.show(pokemonId);
        hiddenPokemonIds.remove(pokemonId);
    }

    private boolean readyForItemInteraction(Pokemon pokemon) {
        return pokemon != null && pokemon.getEntity() != null
                && !pokemon.getEntity().isBusy();
    }

    private int partySlotOf(Pokemon pokemon) {
        if (pokemon == null) return -1;
        for (int slot = 0; slot < 6; slot++) {
            Pokemon partyPokemon = gui.getParty().get(slot);
            if (partyPokemon != null && partyPokemon.getUuid().equals(pokemon.getUuid())) return slot;
        }
        return -1;
    }

    private static String itemId(ItemStack stack) {
        return stack == null || stack.isEmpty() ? "minecraft:air" : Registries.ITEM.getId(stack.getItem()).toString();
    }

    private static String normalize(String raw) {
        return raw == null || raw.isBlank() ? "minecraft:air" : raw;
    }

    private enum State {
        NEXT_TARGET, ACQUIRE_ITEM, WAIT_EMPTY_HAND, WAIT_ITEM_HAND,
        WAIT_ACTOR_ACTIVE, WAIT_TAKEN, WAIT_TAKE_RECALL,
        WAIT_TARGET_ACTIVE, WAIT_GIVEN, WAIT_GIVE_RECALL, WAIT_SKIP_RECALL, DONE
    }

    private record HandSwap(int inventorySlot, int hotbarSlot) {}

}
