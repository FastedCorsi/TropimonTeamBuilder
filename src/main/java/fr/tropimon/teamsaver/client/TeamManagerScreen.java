package fr.tropimon.teamsaver.client;

import com.cobblemon.mod.common.api.abilities.PotentialAbility;
import com.cobblemon.mod.common.api.moves.BenchedMove;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.storage.StorePosition;
import com.cobblemon.mod.common.api.storage.party.PartyPosition;
import com.cobblemon.mod.common.api.storage.pc.PCPosition;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.client.gui.pc.PCGUI;
import com.cobblemon.mod.common.client.render.gui.PCBoxWallpaperRepository;
import com.cobblemon.mod.common.client.gui.summary.widgets.ModelWidget;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.pokemon.abilities.HiddenAbility;
import fr.tropimon.teamsaver.model.TeamModels;
import fr.tropimon.teamsaver.model.TeamModels.PlayerData;
import fr.tropimon.teamsaver.model.TeamModels.SavedSlot;
import fr.tropimon.teamsaver.model.TeamModels.SavedTeam;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import kotlin.Triple;
import kotlin.Unit;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class TeamManagerScreen extends Screen {
    private static final Identifier PC_BASE = Identifier.of("cobblemon", "textures/gui/pc/pc_base.png");
    private static final Identifier SCREEN_OVERLAY = Identifier.of("cobblemon", "textures/gui/pc/pc_screen_overlay.png");
    private static final int PANEL_WIDTH = PCGUI.BASE_WIDTH;
    private static final int PANEL_HEIGHT = PCGUI.BASE_HEIGHT;
    private static final int PAGE_SIZE = 5;
    private static final int LIST_X = 6;
    private static final int SCREEN_X = 86;
    private static final int PARTY_X = 263;
    private static final int CARD_START_X = SCREEN_X + 7;
    private static final int CARD_START_Y = 49;
    private static final int CARD_WIDTH = 50;
    private static final int CARD_HEIGHT = 46;
    private static final int PREVIEW_CARD_WIDTH = 50;
    private static final int PREVIEW_CARD_HEIGHT = 46;
    private static final int ITEM_SLOT_SIZE = 18;
    private static final int PREVIEW_START_X = SCREEN_X + 7;
    private static final int PREVIEW_START_Y = 49;
    private static final int COLOR_OK = 0xFF57E391;
    private static final int COLOR_ITEM = 0xFFFFD15C;
    private static final int COLOR_MISSING = 0xFFFF6B6B;
    private static final TagKey<Item> PVP_ITEM_TAG = TagKey.of(RegistryKeys.ITEM,
            Identifier.of("cobblemon", "held/is_held_item"));
    private static final TagKey<Item> PVP_BERRY_TAG = TagKey.of(RegistryKeys.ITEM,
            Identifier.of("cobblemon", "berries"));
    private static final int ITEM_PICKER_COLUMNS = 8;
    private static final int ITEM_PICKER_ROWS = 4;
    private static final int ITEM_PICKER_PAGE_SIZE = ITEM_PICKER_COLUMNS * ITEM_PICKER_ROWS;
    private static final int POKEMON_PICKER_ROWS = 5;
    private static final int POKEMON_PICKER_ROW_HEIGHT = 23;
    private static final int MOVE_PICKER_ROWS = 6;
    private static final List<Stat> DISPLAY_STATS = List.of(
            Stats.HP, Stats.ATTACK, Stats.DEFENCE, Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED);

    private final PCGUI parent;
    private final boolean standalone;
    private String autoApplyTeamId;
    private final LocalTeamRepository repository = new LocalTeamRepository();
    private final List<DraftSlot> draft = new ArrayList<>();
    private PlayerData data;
    private int selectedTeam;
    private int page;
    private boolean creating;
    private boolean confirmDelete;
    private int selectedDraftSlot = -1;
    private int selectedPreviewSlot = -1;
    private int draggedDraftSlot = -1;
    private int pickerReplaceIndex = -1;
    private String editingTeamId;
    private String draftName = "";
    private String status = "";
    private boolean statusError;
    private TextFieldWidget nameField;
    private Runnable undoAction;
    private String undoDescription = "";
    private SavedTeam pendingPartyUndo;
    private boolean restoringParty;
    private boolean itemPickerOpen;
    private int itemPickerSlot = -1;
    private int itemPickerPage;
    private final List<ItemChoice> itemChoices = new ArrayList<>();
    private boolean pokemonPickerOpen;
    private int pokemonPickerPage;
    private String pokemonPickerQuery = "";
    private TextFieldWidget pokemonSearchField;
    private boolean pokemonPickerNeedsRebuild;
    private final Set<UUID> pokemonPickerExcluded = new HashSet<>();
    private final List<PokemonChoice> pokemonChoices = new ArrayList<>();
    private final List<PokemonChoice> filteredPokemonChoices = new ArrayList<>();
    private boolean pokemonPickerAllSpecies;
    private Stat pokemonPickerSortStat;
    private boolean pokemonPickerSortDescending = true;
    private boolean movePickerOpen;
    private int movePickerSlot = -1;
    private int movePickerSelectedIndex;
    private int movePickerPage;
    private final List<MoveChoice> moveChoices = new ArrayList<>();

    TeamManagerScreen(PCGUI parent) {
        this(parent, false, null);
    }

    TeamManagerScreen(PCGUI parent, boolean standalone) {
        this(parent, standalone, null);
    }

    TeamManagerScreen(PCGUI parent, boolean standalone, String autoApplyTeamId) {
        super(Text.translatable("screen.tropimon_team_saver.title"));
        this.parent = parent;
        this.standalone = standalone;
        this.autoApplyTeamId = autoApplyTeamId;
        this.data = repository.load(MinecraftClient.getInstance());
    }

    @Override
    protected void init() {
        selectedTeam = Math.max(0, Math.min(selectedTeam, Math.max(0, data.teams.size() - 1)));
        page = Math.max(0, Math.min(page, Math.max(0, (data.teams.size() - 1) / PAGE_SIZE)));
        if (pokemonPickerOpen) {
            initPokemonPicker();
            return;
        }
        if (movePickerOpen) return;
        if (itemPickerOpen) return;
        initTabs(left(), top());
        if (creating) initEditor(left(), top());
        else initBrowser(left(), top());
        if (autoApplyTeamId != null && client != null) {
            String teamId = autoApplyTeamId;
            autoApplyTeamId = null;
            client.execute(() -> applyTeamById(teamId));
        }
    }

    private void initTabs(int left, int top) {
        int y = Math.max(1, top - 12);
        PcStyleButton boxes = new PcStyleButton(left + 263, y, 40, 16,
                standalone ? ui("close") : ui("boxes"), button -> openBoxes());
        boxes.setTooltip(Tooltip.of(standalone ? ui("tooltip_close") : ui("tooltip_boxes")));
        addDrawableChild(boxes);
        addDrawableChild(new PcStyleButton(left + 305, y, 40, 16, ui("teams"), button -> {}, PcStyleButton.Style.SELECTED));
    }

    private void initBrowser(int left, int top) {
        addDrawableChild(new PcStyleButton(left + LIST_X + 4, top + 40, 66, 16,
                ui("new"), button -> startCreating()));

        int start = page * PAGE_SIZE;
        for (int row = 0; row < PAGE_SIZE && start + row < data.teams.size(); row++) {
            int index = start + row;
            SavedTeam team = data.teams.get(index);
            PcStyleButton.Style style = index == selectedTeam ? PcStyleButton.Style.SELECTED : PcStyleButton.Style.NORMAL;
            PcStyleButton entry = new PcStyleButton(left + LIST_X + 4, top + 61 + row * 19, 66, 16,
                    Text.literal(team.name), button -> selectTeam(index), style).withScrollingText();
            entry.setTooltip(Tooltip.of(ui("tooltip_team_count", team.name, team.slots.size())));
            addDrawableChild(entry);
        }

        if (page > 0) {
            addDrawableChild(new PcStyleButton(left + 8, top + 169, 22, 14, Text.literal("‹"), button -> changePage(-1)));
        }
        if ((page + 1) * PAGE_SIZE < data.teams.size()) {
            addDrawableChild(new PcStyleButton(left + 58, top + 169, 22, 14, Text.literal("›"), button -> changePage(1)));
        }

        if (data.teams.isEmpty()) {
            addDrawableChild(new PcStyleButton(left + SCREEN_X + 51, top + 123, 72, 16,
                    ui("create"), button -> startCreating()));
        } else {
            SavedTeam team = selectedTeam();
            addBrowserModels(team, left, top);

            PcStyleButton equip = new PcStyleButton(left + SCREEN_X + 5, top + 161, 52, 16,
                    ui("apply"), button -> applyTeam(team));
            equip.active = !hasRequiredReplacements(team);
            equip.setTooltip(Tooltip.of(ui("tooltip_apply")));
            addDrawableChild(equip);

            PcStyleButton edit = new PcStyleButton(left + SCREEN_X + 61, top + 161, 52, 16,
                    ui("edit"), button -> startEditing(team));
            edit.setTooltip(Tooltip.of(ui("tooltip_edit")));
            addDrawableChild(edit);

            PcStyleButton duplicate = new PcStyleButton(left + SCREEN_X + 117, top + 161, 52, 16,
                    ui("duplicate"), button -> duplicateTeam(team));
            duplicate.setTooltip(Tooltip.of(ui("tooltip_duplicate")));
            addDrawableChild(duplicate);

            PcStyleButton delete = new PcStyleButton(left + PARTY_X + 6, top + 169, 70, 16,
                    confirmDelete ? ui("confirm") : ui("delete_team"),
                    button -> requestDelete(team), PcStyleButton.Style.DANGER).withScrollingText();
            delete.setTooltip(Tooltip.of(ui("tooltip_delete")));
            addDrawableChild(delete);
        }

        PcStyleButton saveCurrent = new PcStyleButton(left + PARTY_X + 6, top + 150, 70, 16,
                ui("save_current"), button -> saveCurrentParty()).withScrollingText();
        saveCurrent.active = !parent.getParty().isEmpty();
        saveCurrent.setTooltip(Tooltip.of(ui("tooltip_save_current")));
        addDrawableChild(saveCurrent);

        if (undoAction != null) {
            PcStyleButton undo = new PcStyleButton(left + PARTY_X + 6, top + 131, 70, 16,
                    ui("undo"), button -> undoLast());
            undo.setTooltip(Tooltip.of(Text.literal(undoDescription)));
            addDrawableChild(undo);
        }

    }

    private void initEditor(int left, int top) {
        nameField = new TextFieldWidget(textRenderer, left + SCREEN_X + 6, top + 28, 162, 16, ui("name"));
        nameField.setMaxLength(24);
        nameField.setPlaceholder(ui("name"));
        nameField.setText(draftName);
        nameField.setChangedListener(value -> draftName = value);
        addDrawableChild(nameField);

        addDraftModels(left + CARD_START_X, top + CARD_START_Y);

        if (selectedDraftSlot >= 0 && selectedDraftSlot < draft.size()) {
            addDrawableChild(new PcStyleButton(left + PARTY_X + 6, top + 43, 70, 16,
                    ui("replace"), button -> openPicker(selectedDraftSlot)));
            addDrawableChild(new PcStyleButton(left + PARTY_X + 6, top + 62, 70, 16,
                    ui("remove"), button -> removeSelectedDraft(), PcStyleButton.Style.DANGER));
            DraftSlot selected = draft.get(selectedDraftSlot);
            PcStyleButton moves = new PcStyleButton(left + PARTY_X + 6, top + 81, 70, 16,
                    ui("moves"), button -> openMovePicker(selectedDraftSlot));
            moves.active = selected.pokemon != null;
            moves.setTooltip(Tooltip.of(ui(selected.pokemon == null
                    ? "tooltip_moves_replace" : "tooltip_moves")));
            addDrawableChild(moves);
        }

        PcStyleButton save = new PcStyleButton(left + PARTY_X + 6, top + 150, 70, 16,
                editingTeamId == null ? ui("save") : ui("update"), button -> saveDraft());
        save.active = !draft.isEmpty();
        addDrawableChild(save);
        addDrawableChild(new PcStyleButton(left + PARTY_X + 6, top + 169, 70, 16,
                ui("cancel"), button -> cancelEditor()));
    }

    private void selectTeam(int index) {
        selectedTeam = index;
        selectedPreviewSlot = -1;
        page = selectedTeam / PAGE_SIZE;
        confirmDelete = false;
        status = "";
        clearAndInit();
    }

    private void changePage(int direction) {
        page += direction;
        selectedTeam = Math.min(data.teams.size() - 1, page * PAGE_SIZE);
        selectedPreviewSlot = -1;
        confirmDelete = false;
        clearAndInit();
    }

    private void startCreating() {
        creating = true;
        selectedPreviewSlot = -1;
        editingTeamId = null;
        confirmDelete = false;
        selectedDraftSlot = -1;
        draft.clear();
        draftName = "";
        status = "";
        clearAndInit();
    }

    private void startEditing(SavedTeam team) {
        creating = true;
        selectedPreviewSlot = -1;
        editingTeamId = team.id;
        selectedDraftSlot = -1;
        draft.clear();
        draftName = team.name;
        for (SavedSlot slot : team.slots) {
            Pokemon pokemon = findPokemon(slot.pokemonId);
            String speciesId = slot.speciesId == null && pokemon != null ? speciesId(pokemon) : slot.speciesId;
            draft.add(new DraftSlot(pokemon, slot.pokemonId, speciesId, normalizedItemId(slot.itemId),
                    savedMoves(slot, pokemon), pokemon == null ? uiText("source_catalogue") : currentSource(pokemon)));
        }
        status = uiText("status_edit");
        statusError = false;
        clearAndInit();
    }

    private void cancelEditor() {
        creating = false;
        editingTeamId = null;
        selectedDraftSlot = -1;
        draggedDraftSlot = -1;
        pickerReplaceIndex = -1;
        draft.clear();
        draftName = "";
        status = uiText("status_edit_cancelled");
        statusError = false;
        clearAndInit();
    }

    private void removeSelectedDraft() {
        if (selectedDraftSlot < 0 || selectedDraftSlot >= draft.size()) return;
        DraftSlot removed = draft.remove(selectedDraftSlot);
        selectedDraftSlot = Math.min(selectedDraftSlot, draft.size() - 1);
        status = uiText("status_removed", displayName(removed));
        statusError = false;
        clearAndInit();
    }

    private void openPicker(int replaceIndex) {
        if (client == null || replaceIndex < -1 || replaceIndex >= draft.size()) return;
        if (replaceIndex == -1 && draft.size() >= TeamModels.MAX_TEAM_SIZE) return;
        pickerReplaceIndex = replaceIndex;

        pokemonPickerExcluded.clear();
        for (int i = 0; i < draft.size(); i++) {
            if (i == replaceIndex) continue;
            UUID id = parseUuid(draft.get(i).pokemonId);
            if (id != null) pokemonPickerExcluded.add(id);
        }
        pokemonPickerPage = 0;
        pokemonPickerAllSpecies = false;
        pokemonPickerSortStat = null;
        pokemonPickerSortDescending = true;
        pokemonPickerOpen = true;
        scanPokemonChoices();
        clearAndInit();
    }

    private void initPokemonPicker() {
        int left = left();
        int top = top();
        addDrawableChild(new PcStyleButton(left + 8, top + 27, 57, 17, ui("pokemon_picker_owned"),
                button -> switchPokemonPickerMode(false), pokemonPickerAllSpecies
                ? PcStyleButton.Style.NORMAL : PcStyleButton.Style.SELECTED));
        addDrawableChild(new PcStyleButton(left + 67, top + 27, 45, 17, ui("pokemon_picker_all"),
                button -> switchPokemonPickerMode(true), pokemonPickerAllSpecies
                ? PcStyleButton.Style.SELECTED : PcStyleButton.Style.NORMAL));
        pokemonSearchField = new TextFieldWidget(textRenderer, left + 115, top + 28, 225, 16,
                ui("pokemon_picker_search"));
        pokemonSearchField.setMaxLength(40);
        pokemonSearchField.setPlaceholder(ui("pokemon_picker_search"));
        pokemonSearchField.setText(pokemonPickerQuery);
        pokemonSearchField.setChangedListener(value -> {
            pokemonPickerQuery = value;
            pokemonPickerPage = 0;
            filterPokemonChoices();
            pokemonPickerNeedsRebuild = true;
        });
        addDrawableChild(pokemonSearchField);
        setInitialFocus(pokemonSearchField);

        int start = pokemonPickerPage * POKEMON_PICKER_ROWS;
        for (int row = 0; row < POKEMON_PICKER_ROWS; row++) {
            int index = start + row;
            if (index >= filteredPokemonChoices.size()) break;
            PokemonChoice choice = filteredPokemonChoices.get(index);
            if (choice.pokemon != null) {
                addPokemonModel(choice.pokemon, left + 4, top + 57 + row * POKEMON_PICKER_ROW_HEIGHT,
                        34, 21, 0.62F);
            } else {
                addPokemonModel(choice.species, Set.of(), left + 4,
                        top + 57 + row * POKEMON_PICKER_ROW_HEIGHT, 34, 21, 0.62F);
            }
        }
    }

    private void scanPokemonChoices() {
        pokemonChoices.clear();
        Map<String, PokemonChoice> firstOwnedBySpecies = new LinkedHashMap<>();
        for (int slot = 0; slot < TeamModels.MAX_TEAM_SIZE; slot++) {
            Pokemon pokemon = parent.getParty().get(slot);
            if (pokemon != null && !pokemonPickerExcluded.contains(pokemon.getUuid())) {
                PokemonChoice choice = ownedChoice(pokemon, new PartyPosition(slot), uiText("source_party"));
                pokemonChoices.add(choice);
                firstOwnedBySpecies.putIfAbsent(choice.species.getResourceIdentifier().toString(), choice);
            }
        }
        for (int box = 0; box < parent.getPc().getBoxes().size(); box++) {
            List<Pokemon> slots = parent.getPc().getBoxes().get(box).getSlots();
            for (int slot = 0; slot < slots.size(); slot++) {
                Pokemon pokemon = slots.get(slot);
                if (pokemon != null && !pokemonPickerExcluded.contains(pokemon.getUuid())) {
                    PokemonChoice choice = ownedChoice(pokemon, new PCPosition(box, slot), uiText("source_box", box + 1));
                    pokemonChoices.add(choice);
                    firstOwnedBySpecies.putIfAbsent(choice.species.getResourceIdentifier().toString(), choice);
                }
            }
        }
        if (pokemonPickerAllSpecies) {
            pokemonChoices.clear();
            for (Species species : PokemonSpecies.getImplemented()) {
                PokemonChoice owned = firstOwnedBySpecies.get(species.getResourceIdentifier().toString());
                pokemonChoices.add(owned != null ? owned : new PokemonChoice(null, species, null,
                        uiText("source_catalogue"), false));
            }
        }
        filterPokemonChoices();
    }

    private PokemonChoice ownedChoice(Pokemon pokemon, StorePosition position, String source) {
        return new PokemonChoice(pokemon, pokemon.getSpecies(), position, source, true);
    }

    private void switchPokemonPickerMode(boolean allSpecies) {
        if (pokemonPickerAllSpecies == allSpecies) return;
        pokemonPickerAllSpecies = allSpecies;
        pokemonPickerPage = 0;
        scanPokemonChoices();
        clearAndInit();
    }

    private void filterPokemonChoices() {
        filteredPokemonChoices.clear();
        String query = pokemonPickerQuery == null ? "" : pokemonPickerQuery.strip().toLowerCase(Locale.ROOT);
        for (PokemonChoice choice : pokemonChoices) {
            if (query.isEmpty() || pokemonSearchText(choice).contains(query)) filteredPokemonChoices.add(choice);
        }
        filteredPokemonChoices.sort(pokemonChoiceComparator());
        int pages = pokemonPickerPages();
        pokemonPickerPage = Math.max(0, Math.min(pokemonPickerPage, pages - 1));
    }

    private Comparator<PokemonChoice> pokemonChoiceComparator() {
        Comparator<PokemonChoice> byName = Comparator.comparing(
                choice -> speciesName(choice.species), String.CASE_INSENSITIVE_ORDER);
        Comparator<PokemonChoice> byLevel = Comparator.comparingInt(this::pokemonChoiceLevel).reversed();
        if (pokemonPickerSortStat == null) return byLevel.thenComparing(byName);

        Comparator<PokemonChoice> byStat = Comparator.comparingInt(
                choice -> pokemonChoiceBaseStat(choice, pokemonPickerSortStat));
        if (pokemonPickerSortDescending) byStat = byStat.reversed();
        return byStat.thenComparing(byLevel).thenComparing(byName);
    }

    private int pokemonChoiceLevel(PokemonChoice choice) {
        return choice.pokemon == null ? -1 : choice.pokemon.getLevel();
    }

    private int pokemonChoiceBaseStat(PokemonChoice choice, Stat stat) {
        return (choice.pokemon == null ? choice.species.getStandardForm() : choice.pokemon.getForm())
                .getBaseStats().getOrDefault(stat, 0);
    }

    private String pokemonSearchText(PokemonChoice choice) {
        Pokemon pokemon = choice.pokemon;
        Species species = choice.species;
        String secondary = species.getSecondaryType() == null ? "" : species.getSecondaryType().getDisplayName().getString();
        String ability = pokemon == null ? "" : abilityName(pokemon).getString();
        return (speciesName(species) + " " + species.getName() + " "
                + species.getPrimaryType().getDisplayName().getString() + " " + secondary + " "
                + ability + " " + choice.source).toLowerCase(Locale.ROOT);
    }

    private int pokemonPickerPages() {
        return Math.max(1, (filteredPokemonChoices.size() + POKEMON_PICKER_ROWS - 1) / POKEMON_PICKER_ROWS);
    }

    private void closePokemonPicker(boolean rebuild) {
        pokemonPickerOpen = false;
        pokemonPickerPage = 0;
        pokemonPickerNeedsRebuild = false;
        pokemonSearchField = null;
        pokemonPickerExcluded.clear();
        pokemonChoices.clear();
        filteredPokemonChoices.clear();
        if (rebuild) clearAndInit();
    }

    private Unit selectPokemon(PokemonChoice choice) {
        int target = pickerReplaceIndex;
        Pokemon pokemon = choice.pokemon;
        String pokemonId = pokemon == null ? null : pokemon.getUuid().toString();
        String itemId = pokemon == null ? "minecraft:air" : itemId(pokemon.getHeldItem$common());
        List<String> moves = pokemon == null ? List.of() : activeMoveIds(pokemon);
        DraftSlot replacement = new DraftSlot(pokemon, pokemonId, choice.species.getResourceIdentifier().toString(),
                itemId, moves, choice.source);
        if (target >= 0 && target < draft.size()) {
            draft.set(target, replacement);
            selectedDraftSlot = target;
            status = uiText("status_replaced", target + 1, displayName(replacement));
        } else if (draft.size() < TeamModels.MAX_TEAM_SIZE) {
            draft.add(replacement);
            selectedDraftSlot = draft.size() - 1;
            status = uiText("status_added", displayName(replacement), choice.source);
        }
        statusError = false;
        pickerReplaceIndex = -1;
        closePokemonPicker(false);
        clearAndInit();
        return Unit.INSTANCE;
    }

    private void saveDraft() {
        if (draft.isEmpty()) return;
        List<SavedSlot> slots = new ArrayList<>();
        for (DraftSlot slot : draft) {
            slots.add(new SavedSlot(slot.pokemonId, slot.speciesId, slot.itemId, slot.moveIds));
        }
        String name = TeamModels.cleanName(nameField == null ? draftName : nameField.getText());

        if (editingTeamId != null) {
            int index = findTeamIndex(editingTeamId);
            if (index < 0) {
                applyFailed(uiText("error_missing_team"));
                return;
            }
            SavedTeam before = copyTeam(data.teams.get(index));
            SavedTeam edited = data.teams.get(index);
            edited.name = name;
            edited.slots = slots;
            if (!repository.save(client, data)) {
                data.teams.set(index, before);
                applyFailed(uiText("error_save"));
                return;
            }
            String editedId = edited.id;
            setUndo(uiText("undo_edit", name), () -> restoreTeam(editedId, before));
            selectedTeam = index;
            status = uiText("status_updated", name);
        } else {
            if (data.teams.size() >= TeamModels.MAX_TEAMS) {
                applyFailed(uiText("error_limit"));
                return;
            }
            SavedTeam created = new SavedTeam();
            created.id = UUID.randomUUID().toString();
            created.name = name;
            created.slots = slots;
            data.teams.add(created);
            if (!repository.save(client, data)) {
                data.teams.remove(created);
                applyFailed(uiText("error_save"));
                return;
            }
            String createdId = created.id;
            setUndo(uiText("undo_create", name), () -> removeTeamById(createdId));
            selectedTeam = data.teams.size() - 1;
            status = uiText("status_saved", name);
        }

        creating = false;
        editingTeamId = null;
        draft.clear();
        draftName = "";
        selectedDraftSlot = -1;
        selectedPreviewSlot = -1;
        page = selectedTeam / PAGE_SIZE;
        statusError = false;
        clearAndInit();
    }

    private void saveCurrentParty() {
        if (parent.getParty().isEmpty()) {
            applyFailed(uiText("error_empty"));
            return;
        }
        if (data.teams.size() >= TeamModels.MAX_TEAMS) {
            applyFailed(uiText("error_limit"));
            return;
        }
        SavedTeam created = snapshotCurrentParty();
        created.id = UUID.randomUUID().toString();
        created.name = uniqueTeamName(uiText("current_team_name"));
        data.teams.add(created);
        if (!repository.save(client, data)) {
            data.teams.remove(created);
            applyFailed(uiText("error_save"));
            return;
        }
        String createdId = created.id;
        setUndo(uiText("undo_create", created.name), () -> removeTeamById(createdId));
        selectedTeam = data.teams.size() - 1;
        page = selectedTeam / PAGE_SIZE;
        selectedPreviewSlot = -1;
        status = uiText("status_current_saved", created.name);
        statusError = false;
        clearAndInit();
    }

    private String uniqueTeamName(String baseName) {
        String cleanBase = TeamModels.cleanName(baseName);
        String candidate = cleanBase;
        int suffix = 2;
        while (teamNameExists(candidate)) {
            candidate = TeamModels.numberedName(cleanBase, suffix++);
        }
        return candidate;
    }

    private boolean teamNameExists(String name) {
        for (SavedTeam team : data.teams) {
            if (team.name.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private void duplicateTeam(SavedTeam source) {
        if (data.teams.size() >= TeamModels.MAX_TEAMS) {
            applyFailed(uiText("error_limit"));
            return;
        }
        SavedTeam copy = copyTeam(source);
        copy.id = UUID.randomUUID().toString();
        copy.name = uniqueTeamName(uiText("copy_name", source.name));
        data.teams.add(copy);
        if (!repository.save(client, data)) {
            data.teams.remove(copy);
            applyFailed(uiText("error_copy"));
            return;
        }
        String copyId = copy.id;
        setUndo(uiText("undo_duplicate", source.name), () -> removeTeamById(copyId));
        selectedTeam = data.teams.size() - 1;
        selectedPreviewSlot = -1;
        page = selectedTeam / PAGE_SIZE;
        status = uiText("status_duplicated", copy.name);
        statusError = false;
        clearAndInit();
    }

    private void requestDelete(SavedTeam team) {
        if (!confirmDelete) {
            confirmDelete = true;
            status = uiText("status_confirm_delete");
            statusError = true;
            clearAndInit();
            return;
        }
        int index = data.teams.indexOf(team);
        SavedTeam removed = copyTeam(team);
        data.teams.remove(team);
        if (!repository.save(client, data)) {
            data.teams.add(Math.max(0, index), removed);
            applyFailed(uiText("error_delete"));
            return;
        }
        setUndo(uiText("undo_delete", removed.name), () -> restoreDeletedTeam(index, removed));
        selectedTeam = Math.min(selectedTeam, Math.max(0, data.teams.size() - 1));
        selectedPreviewSlot = -1;
        page = selectedTeam / PAGE_SIZE;
        confirmDelete = false;
        status = uiText("status_deleted");
        statusError = false;
        clearAndInit();
    }

    private void applyTeam(SavedTeam team) {
        if (hasRequiredReplacements(team)) {
            applyFailed(uiText("error_replace_required"));
            return;
        }
        if (standalone && ClientTeamApplier.requiresPcAccess(parent, team)) {
            status = uiText("status_opening_remote_pc");
            statusError = false;
            if (!TropimonTeamSaverClient.requestRemotePcForApply(team.id)) {
                applyFailed(uiText("error_remote_pc_failed"));
            }
            return;
        }
        pendingPartyUndo = snapshotCurrentParty();
        status = uiText("status_applying", analysisSummary(analysis(team)));
        statusError = false;
        if (!ClientTeamApplier.start(this, parent, team, data)) pendingPartyUndo = null;
    }

    private void applyTeamById(String teamId) {
        int index = findTeamIndex(teamId);
        if (index < 0) {
            applyFailed(uiText("error_missing_team"));
            return;
        }
        selectedTeam = index;
        page = selectedTeam / PAGE_SIZE;
        selectedPreviewSlot = -1;
        clearAndInit();
        applyTeam(data.teams.get(index));
    }

    boolean isStandalone() {
        return standalone;
    }

    void applyFinished(PlayerData updated, int itemDifferences) {
        boolean positionsSaved = repository.save(client, updated);
        if (restoringParty) {
            restoringParty = false;
            status = positionsSaved ? uiText("status_restored") : uiText("error_save");
            statusError = !positionsSaved;
            clearAndInit();
            return;
        }
        boolean canUndo = installPartyUndo();
        String undoSuffix = canUndo ? uiText("status_undo_available") : ".";
        status = itemDifferences == 0 ? uiText("status_equipped", undoSuffix) :
                uiText("status_equipped_items", itemDifferences, undoSuffix);
        statusError = !positionsSaved;
        if (!positionsSaved) status = uiText("error_save");
        clearAndInit();
    }

    void applyFailed(String message) {
        pendingPartyUndo = null;
        restoringParty = false;
        status = message;
        statusError = true;
        clearAndInit();
    }

    void applyFailedAfterChange(String message) {
        restoringParty = false;
        boolean canUndo = installPartyUndo();
        status = canUndo ? uiText("error_partial_undo", message) : message;
        statusError = true;
        clearAndInit();
    }

    private boolean installPartyUndo() {
        if (pendingPartyUndo == null) return false;
        SavedTeam previousParty = pendingPartyUndo;
        pendingPartyUndo = null;
        setUndo(uiText("undo_party"), () -> {
            status = uiText("status_restoring");
            statusError = false;
            restoringParty = true;
            if (!ClientTeamApplier.start(this, parent, previousParty, data)) restoringParty = false;
        });
        return true;
    }

    private void undoLast() {
        if (undoAction == null) return;
        Runnable action = undoAction;
        undoAction = null;
        undoDescription = "";
        action.run();
    }

    private void setUndo(String description, Runnable action) {
        undoDescription = description;
        undoAction = action;
    }

    private void restoreDeletedTeam(int index, SavedTeam team) {
        int restoredIndex = Math.min(Math.max(0, index), data.teams.size());
        SavedTeam restored = copyTeam(team);
        data.teams.add(restoredIndex, restored);
        if (!repository.save(client, data)) {
            data.teams.remove(restored);
            applyFailed(uiText("error_save"));
            return;
        }
        selectedTeam = Math.min(index, data.teams.size() - 1);
        page = selectedTeam / PAGE_SIZE;
        status = uiText("status_restored");
        statusError = false;
        clearAndInit();
    }

    private void restoreTeam(String id, SavedTeam previous) {
        int index = findTeamIndex(id);
        if (index < 0) {
            applyFailed(uiText("error_missing_team"));
            return;
        }
        SavedTeam current = data.teams.set(index, copyTeam(previous));
        if (!repository.save(client, data)) {
            data.teams.set(index, current);
            applyFailed(uiText("error_save"));
            return;
        }
        selectedTeam = Math.max(0, index);
        page = selectedTeam / PAGE_SIZE;
        status = uiText("status_action_undone");
        statusError = false;
        clearAndInit();
    }

    private void removeTeamById(String id) {
        int index = findTeamIndex(id);
        if (index < 0) {
            applyFailed(uiText("error_missing_team"));
            return;
        }
        SavedTeam removed = data.teams.remove(index);
        if (!repository.save(client, data)) {
            data.teams.add(index, removed);
            applyFailed(uiText("error_save"));
            return;
        }
        selectedTeam = Math.min(selectedTeam, Math.max(0, data.teams.size() - 1));
        page = selectedTeam / PAGE_SIZE;
        status = uiText("status_action_undone");
        statusError = false;
        clearAndInit();
    }

    private SavedTeam snapshotCurrentParty() {
        SavedTeam snapshot = new SavedTeam();
        snapshot.id = "undo-" + UUID.randomUUID();
        snapshot.name = uiText("previous_party");
        for (int i = 0; i < TeamModels.MAX_TEAM_SIZE; i++) {
            Pokemon pokemon = parent.getParty().get(i);
            if (pokemon != null) snapshot.slots.add(new SavedSlot(pokemon.getUuid().toString(), speciesId(pokemon),
                    itemId(pokemon.getHeldItem$common()), activeMoveIds(pokemon)));
        }
        return snapshot;
    }

    private SavedTeam copyTeam(SavedTeam source) {
        SavedTeam copy = new SavedTeam();
        copy.id = source.id;
        copy.name = source.name;
        for (SavedSlot slot : source.slots) copy.slots.add(new SavedSlot(slot.pokemonId, slot.speciesId,
                normalizedItemId(slot.itemId), slot.moveIds));
        return copy;
    }

    private int findTeamIndex(String id) {
        for (int i = 0; i < data.teams.size(); i++) if (data.teams.get(i).id.equals(id)) return i;
        return -1;
    }

    private SavedTeam selectedTeam() {
        return data.teams.get(selectedTeam);
    }

    private void addBrowserModels(SavedTeam team, int left, int top) {
        for (int i = 0; i < team.slots.size(); i++) {
            SavedSlot slot = team.slots.get(i);
            Pokemon pokemon = findPokemon(slot.pokemonId);
            int x = previewCardX(left + PREVIEW_START_X, i);
            int y = previewCardY(top + PREVIEW_START_Y, i);
            if (pokemon != null) {
                addPokemonModel(pokemon, x + 6, y + 2, 38, 38);
            } else {
                Species species = findSpecies(slot.speciesId);
                if (species != null) addPokemonModel(species, Set.of(), x + 6, y + 2, 38, 38);
            }
        }
    }

    private void addDraftModels(int x, int y) {
        for (int i = 0; i < draft.size(); i++) {
            DraftSlot slot = draft.get(i);
            if (slot.pokemon != null) addPokemonModel(slot.pokemon, cardX(x, i) + 6, cardY(y, i) + 2, 38, 38);
            else {
                Species species = findSpecies(slot.speciesId);
                if (species != null) addPokemonModel(species, Set.of(), cardX(x, i) + 6, cardY(y, i) + 2, 38, 38);
            }
        }
    }

    private void addPokemonModel(Pokemon pokemon, int x, int y, int width, int height) {
        addPokemonModel(pokemon, x, y, width, height, 0.8F);
    }

    private void addPokemonModel(Pokemon pokemon, int x, int y, int width, int height, float scale) {
        RenderablePokemon renderable = new RenderablePokemon(pokemon.getSpecies(), pokemon.getAspects(), ItemStack.EMPTY);
        addDrawableChild(new ModelWidget(x, y, width, height, renderable, scale, 35.0F, 1.0, false, false));
    }

    private void addPokemonModel(Species species, Set<String> aspects, int x, int y, int width, int height) {
        addPokemonModel(species, aspects, x, y, width, height, 0.8F);
    }

    private void addPokemonModel(Species species, Set<String> aspects, int x, int y,
                                 int width, int height, float scale) {
        RenderablePokemon renderable = new RenderablePokemon(species, aspects, ItemStack.EMPTY);
        addDrawableChild(new ModelWidget(x, y, width, height, renderable, scale, 35.0F, 1.0, false, false));
    }

    private Pokemon findPokemon(String rawId) {
        UUID id = parseUuid(rawId);
        if (id == null) return null;
        Pokemon partyPokemon = parent.getParty().findByUUID(id);
        return partyPokemon != null ? partyPokemon : parent.getPc().findByUUID(id);
    }

    private Species findSpecies(String rawId) {
        Identifier id = Identifier.tryParse(rawId);
        return id == null ? null : PokemonSpecies.getByIdentifier(id);
    }

    private String speciesId(Pokemon pokemon) {
        return pokemon.getSpecies().getResourceIdentifier().toString();
    }

    private String speciesName(Species species) {
        return species == null ? uiText("not_found") : species.getTranslatedName().getString();
    }

    private List<String> activeMoveIds(Pokemon pokemon) {
        List<String> moves = new ArrayList<>();
        for (Move move : pokemon.getMoveSet().getMoves()) moves.add(move.getTemplate().getName());
        return moves;
    }

    private List<String> savedMoves(SavedSlot slot, Pokemon pokemon) {
        return slot.moveIds == null || slot.moveIds.isEmpty()
                ? pokemon == null ? new ArrayList<>() : activeMoveIds(pokemon)
                : new ArrayList<>(slot.moveIds);
    }

    private boolean hasRequiredReplacements(SavedTeam team) {
        for (SavedSlot slot : team.slots) {
            if (slot.pokemonId == null || findPokemon(slot.pokemonId) == null) return true;
        }
        return false;
    }

    private Identifier activePcWallpaper() {
        Identifier fallback = PCBoxWallpaperRepository.INSTANCE.getDefaultWallpaper();
        try {
            int boxIndex = currentPcBox();
            if (parent.getPc().getBoxes().isEmpty()) return fallback;
            boxIndex = Math.max(0, Math.min(boxIndex, parent.getPc().getBoxes().size() - 1));
            Identifier selected = parent.getPc().getBoxes().get(boxIndex).getWallpaper();
            for (Triple<Identifier, Identifier, Identifier> wallpaper
                    : PCBoxWallpaperRepository.INSTANCE.getAllWallpapers()) {
                if (selected.equals(wallpaper.getFirst()) || selected.equals(wallpaper.getSecond())) return selected;
            }
        } catch (Exception ignored) {
            // Le wallpaper par défaut du PC reste toujours une ressource valide.
        }
        return fallback;
    }

    /**
     * A standalone team screen owns a lightweight PCGUI that has never been
     * initialized as a screen. Its Kotlin storageWidget is therefore unavailable.
     * The constructor's openOnBox value is the authoritative fallback in that mode.
     */
    private int currentPcBox() {
        if (standalone) return parent.getOpenOnBox();
        try {
            return parent.getStorage().getBox();
        } catch (RuntimeException ignored) {
            return parent.getOpenOnBox();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int left = left();
        int top = top();
        context.drawTexture(PC_BASE, left, top, 349, 205, 0, 0, 349, 205, 349, 205);
        context.fill(left + 1, top + 22, left + 82, top + 190, 0xFF2E363A);
        context.drawTexture(activePcWallpaper(), left + SCREEN_X, top + 25, 174, 155,
                0, 0, 174, 155, 174, 155);
        context.drawTexture(SCREEN_OVERLAY, left + SCREEN_X, top + 25, 174, 155, 0, 0, 174, 155, 174, 155);
        context.fill(left + PARTY_X + 4, top + 24, left + PARTY_X + 78, top + 188, 0xFF343A3D);
        context.fill(left + PARTY_X + 5, top + 25, left + PARTY_X + 77, top + 26, 0xFF626A6D);
        context.fill(left + PARTY_X + 5, top + 187, left + PARTY_X + 77, top + 188, 0xFF1C2022);
        context.drawCenteredTextWithShadow(textRenderer, title, left + 174, top + 15, 0xFFFFFF);

        if (pokemonPickerOpen) {
            renderPokemonPickerBase(context, mouseX, mouseY, left, top);
            super.render(context, mouseX, mouseY, delta);
            renderPokemonPickerOverlay(context, mouseX, mouseY, left, top);
            return;
        }

        if (movePickerOpen) {
            super.render(context, mouseX, mouseY, delta);
            renderMovePicker(context, mouseX, mouseY, left, top);
            return;
        }

        if (itemPickerOpen) {
            super.render(context, mouseX, mouseY, delta);
            renderItemPicker(context, mouseX, mouseY, left, top);
            return;
        }

        if (creating) renderEditor(context, left, top, mouseX, mouseY);
        else renderBrowser(context, left, top, mouseX, mouseY);
        renderStatus(context, left, top);
        super.render(context, mouseX, mouseY, delta);

        if (creating) renderDraftCardOverlay(context, left, top, mouseX, mouseY);
        else if (!data.teams.isEmpty()) renderBrowserCardOverlay(context, selectedTeam(), left, top, mouseX, mouseY);
        renderHoverTooltip(context, mouseX, mouseY);
    }

    private void renderMovePicker(DrawContext context, int mouseX, int mouseY, int left, int top) {
        int panelX = left + 24;
        int panelY = top + 21;
        int panelWidth = 301;
        int panelHeight = 169;
        context.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0x98000000);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF283238);
        context.fill(panelX + 2, panelY + 2, panelX + panelWidth - 2, panelY + 18, 0xFF3B464B);
        drawOutline(context, panelX, panelY, panelWidth, panelHeight, 0xFF9BA5A9);
        drawOutline(context, panelX + 2, panelY + 2, panelWidth - 4, panelHeight - 4, 0xFF111719);
        DraftSlot slot = movePickerSlot >= 0 && movePickerSlot < draft.size() ? draft.get(movePickerSlot) : null;
        context.drawCenteredTextWithShadow(textRenderer,
                slot == null ? ui("move_picker_title") : ui("move_picker_title_pokemon", displayName(slot)),
                panelX + panelWidth / 2, panelY + 6, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, ui("move_picker_preset"), panelX + 9, panelY + 24, 0xFFA7D6E2);
        context.drawTextWithShadow(textRenderer, ui("move_picker_available"), panelX + 142, panelY + 24, 0xFFA7D6E2);

        if (slot != null) {
            for (int i = 0; i < 4; i++) {
                int x = panelX + 8;
                int y = panelY + 36 + i * 25;
                boolean hovered = isInside(mouseX, mouseY, x, y, 124, 22);
                boolean selected = movePickerSelectedIndex == i;
                context.fill(x, y, x + 124, y + 22, selected ? 0xFF356D7A : hovered ? 0xFF3C5158 : 0xFF1A252A);
                drawOutline(context, x, y, 124, 22, selected ? 0xFFA7F1FF : 0xFF607980);
                String moveId = i < slot.moveIds.size() ? slot.moveIds.get(i) : null;
                MoveTemplate move = moveId == null ? null : com.cobblemon.mod.common.api.moves.Moves.getByName(moveId);
                context.drawTextWithShadow(textRenderer, Text.literal(String.valueOf(i + 1)), x + 5, y + 7, 0xFF8ECED9);
                context.drawTextWithShadow(textRenderer,
                        move == null ? ui("move_picker_empty") : move.getDisplayName(), x + 19, y + 7,
                        move == null ? 0xFF87989D : 0xFFFFFFFF);
            }
        }

        int start = movePickerPage * MOVE_PICKER_ROWS;
        for (int row = 0; row < MOVE_PICKER_ROWS; row++) {
            int index = start + row;
            if (index >= moveChoices.size()) break;
            MoveChoice choice = moveChoices.get(index);
            int x = panelX + 140;
            int y = panelY + 36 + row * 20;
            boolean hovered = isInside(mouseX, mouseY, x, y, 153, 18);
            context.fill(x, y, x + 153, y + 18, hovered ? 0xFF3F6F79 : 0xFF1A252A);
            drawOutline(context, x, y, 153, 18, hovered ? 0xFFB7F5FF : 0xFF607980);
            context.fill(x + 2, y + 2, x + 5, y + 16, typeColor(choice.template.getElementalType().getName()));
            context.drawText(textRenderer, Text.literal(fitText(choice.template.getDisplayName().getString(), 92)),
                    x + 9, y + 5, 0xFFFFFFFF, false);
            String info = choice.active ? uiText("move_active") : uiText("move_benched");
            int badgeX = x + 105;
            int badgeColor = choice.active ? 0xFF245D46 : 0xFF66531D;
            int badgeBorder = choice.active ? 0xFF57E391 : 0xFFFFD15C;
            context.fill(badgeX, y + 3, badgeX + 46, y + 15, badgeColor);
            drawOutline(context, badgeX, y + 3, 46, 12, badgeBorder);
            drawCenteredPlainText(context, Text.literal(fitText(info, 42)), badgeX + 23, y + 5,
                    choice.active ? 0xFFB8FFD4 : 0xFFFFE59A);
        }

        context.drawCenteredTextWithShadow(textRenderer,
                ui("move_picker_page", movePickerPage + 1, movePickerPages()),
                panelX + 216, panelY + 159, 0xFF91AEB5);
        context.drawCenteredTextWithShadow(textRenderer, ui("move_picker_close"),
                panelX + 67, panelY + 146, 0xFF91AEB5);
    }

    private void renderPokemonPickerBase(DrawContext context, int mouseX, int mouseY, int left, int top) {
        int panelX = left + 5;
        int panelY = top + 24;
        int panelWidth = 339;
        context.fill(panelX, panelY, panelX + panelWidth, top + 189, 0xFFE7EDF3);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 2, 0xFF8B969E);
        context.fill(panelX, top + 46, panelX + panelWidth, top + 59, 0xFFD1DAE3);
        int sortedStat = pokemonPickerSortStat == null ? -1 : DISPLAY_STATS.indexOf(pokemonPickerSortStat);
        if (sortedStat >= 0) {
            int statX = panelX + 231 + sortedStat * 18;
            context.fill(statX, top + 46, statX + 18, top + 59, 0xFFB8DFE9);
            int markerY = pokemonPickerSortDescending ? top + 57 : top + 46;
            context.fill(statX, markerY, statX + 18, markerY + 2, 0xFF147D96);
        }
        drawOutline(context, panelX, panelY, panelWidth, 165, 0xFF20282D);

        int start = pokemonPickerPage * POKEMON_PICKER_ROWS;
        for (int row = 0; row < POKEMON_PICKER_ROWS; row++) {
            int y = top + 59 + row * POKEMON_PICKER_ROW_HEIGHT;
            int index = start + row;
            boolean hovered = index < filteredPokemonChoices.size()
                    && isInside(mouseX, mouseY, panelX + 2, y, panelWidth - 4, POKEMON_PICKER_ROW_HEIGHT);
            int color = hovered ? 0xFFC7E8F2 : row % 2 == 0 ? 0xFFF3F7FA : 0xFFE5ECF2;
            context.fill(panelX + 2, y, panelX + panelWidth - 2, y + POKEMON_PICKER_ROW_HEIGHT, color);
            context.fill(panelX + 2, y + POKEMON_PICKER_ROW_HEIGHT - 1,
                    panelX + panelWidth - 2, y + POKEMON_PICKER_ROW_HEIGHT, 0xFFCCD6DE);
            if (hovered) context.fill(panelX + 2, y, panelX + 5, y + POKEMON_PICKER_ROW_HEIGHT, 0xFF3BA4BC);
        }
    }

    private void renderPokemonPickerOverlay(DrawContext context, int mouseX, int mouseY, int left, int top) {
        int panelX = left + 5;
        context.drawText(textRenderer, ui("pokemon_picker_name"), panelX + 34, top + 48, 0xFF17242B, false);
        context.drawText(textRenderer, ui("pokemon_picker_types"), panelX + 112, top + 48, 0xFF17242B, false);
        context.drawText(textRenderer, ui("pokemon_picker_ability"), panelX + 159, top + 48, 0xFF17242B, false);
        for (int i = 0; i < DISPLAY_STATS.size(); i++) {
            boolean selected = DISPLAY_STATS.get(i) == pokemonPickerSortStat;
            drawCenteredPlainText(context, compactStatHeader(DISPLAY_STATS.get(i)),
                    panelX + 240 + i * 18, top + 48,
                    selected ? 0xFF07566A : 0xFF17242B);
        }

        int start = pokemonPickerPage * POKEMON_PICKER_ROWS;
        for (int row = 0; row < POKEMON_PICKER_ROWS; row++) {
            int index = start + row;
            if (index >= filteredPokemonChoices.size()) break;
            PokemonChoice choice = filteredPokemonChoices.get(index);
            Pokemon pokemon = choice.pokemon;
            Species species = choice.species;
            var primaryType = pokemon == null ? species.getPrimaryType() : pokemon.getPrimaryType();
            var secondaryType = pokemon == null ? species.getSecondaryType() : pokemon.getSecondaryType();
            int y = top + 59 + row * POKEMON_PICKER_ROW_HEIGHT;
            context.drawText(textRenderer, Text.literal(fitText(speciesName(species), 76)), panelX + 34, y + 2,
                    choice.owned ? 0xFF172126 : 0xFF9A3030, false);
            String detail = pokemonChoiceCompactDetail(choice);
            context.drawText(textRenderer, Text.literal(fitText(detail, 76)), panelX + 34, y + 13,
                    choice.owned ? 0xFF455760 : 0xFFB44848, false);
            drawTypeLabel(context, primaryType.getDisplayName(), primaryType.getName(),
                    panelX + 112, y + 1);
            if (secondaryType != null) {
                drawTypeLabel(context, secondaryType.getDisplayName(), secondaryType.getName(),
                        panelX + 112, y + 12);
            }
            Text ability = pokemon == null ? ui("pokemon_picker_replace") : abilityName(pokemon);
            drawMarqueeText(context, ability, panelX + 159, y + 7, 70,
                    pokemon == null ? 0xFFC13E3E : isHiddenAbility(pokemon) ? 0xFFA95C00 : 0xFF17242B);
            for (int stat = 0; stat < DISPLAY_STATS.size(); stat++) {
                int value = (pokemon == null ? species.getStandardForm() : pokemon.getForm())
                        .getBaseStats().getOrDefault(DISPLAY_STATS.get(stat), 0);
                drawCenteredPlainText(context, Text.literal(String.valueOf(value)),
                        panelX + 240 + stat * 18, y + 7, 0xFF17242B);
            }
        }

        if (filteredPokemonChoices.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, ui("pokemon_picker_empty"), left + 174, top + 105,
                    0xFF596A73);
        }
        context.drawText(textRenderer, Text.literal("‹"), panelX + 8, top + 178,
                pokemonPickerPage > 0 ? 0xFF1D7187 : 0xFF9AA5AB, false);
        context.drawText(textRenderer, Text.literal("›"), panelX + 325, top + 178,
                pokemonPickerPage + 1 < pokemonPickerPages() ? 0xFF1D7187 : 0xFF9AA5AB, false);
        drawCenteredPlainText(context, ui("pokemon_picker_page", pokemonPickerPage + 1, pokemonPickerPages()),
                left + 174, top + 178, 0xFF34434B);

        int hovered = pokemonChoiceIndexAt(mouseX, mouseY);
        if (hovered >= 0 && hovered < filteredPokemonChoices.size()) {
            PokemonChoice choice = filteredPokemonChoices.get(hovered);
            Pokemon pokemon = choice.pokemon;
            List<Text> lines = new ArrayList<>();
            lines.add(choice.species.getTranslatedName());
            lines.add(Text.literal(pokemon == null ? choice.source : choice.source + " · Nv." + pokemon.getLevel()));
            if (pokemon != null) lines.add(abilityName(pokemon));
            lines.add(ui(pokemon == null ? "pokemon_picker_select_placeholder" : "pokemon_picker_select"));
            context.drawTooltip(textRenderer, lines, Optional.empty(), mouseX, mouseY);
        } else {
            int hoveredStat = pokemonStatHeaderIndexAt(mouseX, mouseY);
            if (hoveredStat >= 0) {
                Stat stat = DISPLAY_STATS.get(hoveredStat);
                boolean descending = stat != pokemonPickerSortStat || pokemonPickerSortDescending;
                context.drawTooltip(textRenderer, List.of(
                        ui(descending ? "pokemon_picker_sort_desc" : "pokemon_picker_sort_asc", statHeader(stat)),
                        ui("pokemon_picker_sort_hint")), Optional.empty(), mouseX, mouseY);
            }
        }
    }

    private void renderBrowser(DrawContext context, int left, int top, int mouseX, int mouseY) {
        drawCompactLeftHeader(context, ui("my_teams"), left, top);
        if (data.teams.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, ui("empty"), left + SCREEN_X + 87, top + 84, 0xE4F7FB);
            context.drawCenteredTextWithShadow(textRenderer, ui("empty_hint"), left + SCREEN_X + 87, top + 100, 0xA7D6E2);
            return;
        }

        SavedTeam team = selectedTeam();
        TeamAnalysis analysis = analysis(team);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(trim(team.name, 22)), left + SCREEN_X + 87, top + 29, 0xFFFFFF);
        for (int i = 0; i < TeamModels.MAX_TEAM_SIZE; i++) {
            drawBrowserCardBase(context, team, i, left, top, mouseX, mouseY);
        }
        if (status.isBlank()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(analysisSummary(analysis)),
                    left + SCREEN_X + 87, top + 150,
                    analysis.missing > 0 ? COLOR_MISSING : analysis.itemDifferences > 0 ? COLOR_ITEM : COLOR_OK);
        }
        if (selectedPreviewSlot >= 0 && selectedPreviewSlot < team.slots.size()) {
            renderPokemonInfoPanel(context, team.slots.get(selectedPreviewSlot), left, top);
        }
    }

    private void drawBrowserCardBase(DrawContext context, SavedTeam team, int index, int left, int top,
                                     int mouseX, int mouseY) {
        int x = previewCardX(left + PREVIEW_START_X, index);
        int y = previewCardY(top + PREVIEW_START_Y, index);
        boolean hovered = isInside(mouseX, mouseY, x, y, PREVIEW_CARD_WIDTH, PREVIEW_CARD_HEIGHT);
        SavedSlot slot = index < team.slots.size() ? team.slots.get(index) : null;
        if (slot == null) return;
        Pokemon pokemon = slot == null ? null : findPokemon(slot.pokemonId);
        SlotState state = slotState(slot, pokemon);
        context.fill(x, y, x + PREVIEW_CARD_WIDTH, y + PREVIEW_CARD_HEIGHT,
                hovered ? 0xAA4B565B : 0x88373F43);
        context.fill(x + 1, y + 1, x + PREVIEW_CARD_WIDTH - 1, y + 2, 0x886F7B80);
        if (pokemon == null && findSpecies(slot.speciesId) == null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("?"), x + 25, y + 18, COLOR_MISSING);
        }
        context.fill(x + 2, y + PREVIEW_CARD_HEIGHT - 3, x + PREVIEW_CARD_WIDTH - 2,
                y + PREVIEW_CARD_HEIGHT - 1, state.color);
        if (hovered) drawOutline(context, x, y, PREVIEW_CARD_WIDTH, PREVIEW_CARD_HEIGHT, 0xFFC1F7FF);
        if (selectedPreviewSlot == index) drawOutline(context, x, y, PREVIEW_CARD_WIDTH,
                PREVIEW_CARD_HEIGHT, 0xFFFFFFFF);
    }

    private void renderBrowserCardOverlay(DrawContext context, SavedTeam team, int left, int top,
                                          int mouseX, int mouseY) {
        for (int i = 0; i < TeamModels.MAX_TEAM_SIZE; i++) {
            int x = previewCardX(left + PREVIEW_START_X, i);
            int y = previewCardY(top + PREVIEW_START_Y, i);
            if (i >= team.slots.size()) {
                continue;
            }
            SavedSlot slot = team.slots.get(i);
            SlotState state = slotState(slot, findPokemon(slot.pokemonId));
            context.fill(x + PREVIEW_CARD_WIDTH - 5, y + 3, x + PREVIEW_CARD_WIDTH - 2, y + 6, state.color);
            drawSlotNumber(context, x, y, i);
            drawItemSlot(context, x, y, PREVIEW_CARD_WIDTH, PREVIEW_CARD_HEIGHT,
                    itemStack(slot.itemId), mouseX, mouseY);
            if (selectedPreviewSlot == i) drawOutline(context, x, y, PREVIEW_CARD_WIDTH, PREVIEW_CARD_HEIGHT, 0xFFFFFFFF);
        }
    }

    private void renderDraftCardOverlay(DrawContext context, int left, int top, int mouseX, int mouseY) {
        for (int i = 0; i < draft.size(); i++) {
            int x = cardX(left + CARD_START_X, i);
            int y = cardY(top + CARD_START_Y, i);
            drawSlotNumber(context, x, y, i);
            drawItemSlot(context, x, y, CARD_WIDTH, CARD_HEIGHT,
                    itemStack(draft.get(i).itemId), mouseX, mouseY);
        }
    }

    private void drawSlotNumber(DrawContext context, int x, int y, int index) {
        context.drawTextWithShadow(textRenderer, Text.literal(String.valueOf(index + 1)),
                x + 4, y + 4, 0xFFEAFBFF);
        context.fill(x + 3, y + 14, x + 11, y + 15, 0xCC63D7E8);
    }

    private void drawItemSlot(DrawContext context, int cardX, int cardY, int cardWidth, int cardHeight,
                              ItemStack item, int mouseX, int mouseY) {
        int x = cardX + cardWidth - ITEM_SLOT_SIZE - 2;
        int y = cardY + cardHeight - ITEM_SLOT_SIZE - 4;
        boolean hovered = isInside(mouseX, mouseY, x, y, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
        context.fill(x, y, x + ITEM_SLOT_SIZE, y + ITEM_SLOT_SIZE,
                hovered ? 0xDD34474E : 0xBB273338);
        drawOutline(context, x, y, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE,
                hovered ? 0xFFD1F8FF : 0xFF748A91);
        context.fill(x + 1, y + 1, x + ITEM_SLOT_SIZE - 1, y + 2,
                hovered ? 0xFF8ECED9 : 0xAA60757B);
        if (item.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("+"),
                    x + ITEM_SLOT_SIZE / 2, y + 5, hovered ? 0xFFFFFFFF : 0xFFB7D8DE);
        } else {
            context.drawItem(item, x + 1, y + 1);
        }
    }

    private void renderPokemonInfoPanel(DrawContext context, SavedSlot slot, int left, int top) {
        Pokemon pokemon = findPokemon(slot.pokemonId);
        if (pokemon == null) {
            Species species = findSpecies(slot.speciesId);
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(trim(speciesName(species), 12)), left + PARTY_X + 41, top + 49, 0xFFFFFFFF);
            context.drawTextWrapped(textRenderer, ui("replace_required"),
                    left + PARTY_X + 9, top + 67, 64, COLOR_MISSING);
            return;
        }
        int center = left + PARTY_X + 41;
        int x = left + PARTY_X + 9;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(trim(pokemonName(pokemon), 12)), center, top + 31, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, natureName(pokemon), center, top + 44, 0xFFBFEFFF);
        boolean hiddenAbility = isHiddenAbility(pokemon);
        context.drawCenteredTextWithShadow(textRenderer, abilityName(pokemon), center, top + 56,
                hiddenAbility ? 0xFFFFD15C : 0xFFFFFFFF);

        context.drawTextWithShadow(textRenderer, ui("iv"), x + 29, top + 67, COLOR_OK);
        context.drawTextWithShadow(textRenderer, ui("ev"), x + 48, top + 67, 0xFF72C9EA);
        drawStatRow(context, pokemon, Stats.HP, "stat_hp", x, top + 78);
        drawStatRow(context, pokemon, Stats.ATTACK, "stat_attack", x, top + 87);
        drawStatRow(context, pokemon, Stats.DEFENCE, "stat_defence", x, top + 96);
        drawStatRow(context, pokemon, Stats.SPECIAL_ATTACK, "stat_special_attack", x, top + 105);
        drawStatRow(context, pokemon, Stats.SPECIAL_DEFENCE, "stat_special_defence", x, top + 114);
        drawStatRow(context, pokemon, Stats.SPEED, "stat_speed", x, top + 123);
    }

    private void drawStatRow(DrawContext context, Pokemon pokemon, Stat stat, String labelKey, int x, int y) {
        int iv = pokemon.getIvs().getOrDefault(stat);
        int ev = pokemon.getEvs().getOrDefault(stat);
        context.drawTextWithShadow(textRenderer, ui(labelKey), x, y, 0xFFB9C9CD);
        context.drawTextWithShadow(textRenderer, Text.literal(String.valueOf(iv)), x + 29, y, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal(String.valueOf(ev)), x + 48, y, 0xFFFFFFFF);
    }

    private void renderEditor(DrawContext context, int left, int top, int mouseX, int mouseY) {
        drawCompactLeftHeader(context, editingTeamId == null ? ui("editor_new") : ui("editor_edit"), left, top);
        drawStep(context, left + 9, top + 55, "1", uiText("step_name"), !draftName.isBlank());
        drawStep(context, left + 9, top + 87, "2", uiText("step_pokemon"), !draft.isEmpty());
        drawStep(context, left + 9, top + 119, "3", uiText("step_save"), false);
        context.drawTextWrapped(textRenderer, ui("reorder_hint"),
                left + 9, top + 150, 68, 0xFFA7D6E2);
        drawDraftCards(context, left + CARD_START_X, top + CARD_START_Y, mouseX, mouseY);
    }

    private void drawStep(DrawContext context, int x, int y, String number, String label, boolean complete) {
        int color = complete ? COLOR_OK : 0xFF317C91;
        context.fill(x, y, x + 13, y + 13, color);
        context.fill(x + 1, y + 1, x + 12, y + 2, complete ? 0xFFB5FFD1 : 0xFF77C7D8);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(complete ? "✓" : number), x + 6, y + 3, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal(label), x + 17, y + 3, 0xFFE5F7FA);
    }

    private void drawCompactLeftHeader(DrawContext context, Text label, int left, int top) {
        float scale = 0.8F;
        float centerX = left + 40.0F;
        float y = top + 11.0F;
        context.enableScissor(left + 5, top + 2, left + 75, top + 20);
        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, 1.0F);
        context.drawCenteredTextWithShadow(textRenderer, label,
                Math.round(centerX / scale), Math.round(y / scale), 0xFFFFFFFF);
        context.getMatrices().pop();
        context.disableScissor();
    }

    private void drawDraftCards(DrawContext context, int x, int y, int mouseX, int mouseY) {
        for (int i = 0; i < TeamModels.MAX_TEAM_SIZE; i++) {
            int px = cardX(x, i);
            int py = cardY(y, i);
            boolean hovered = isInside(mouseX, mouseY, px, py, CARD_WIDTH, CARD_HEIGHT);
            if (i >= draft.size()) {
                drawEmptyCard(context, px, py, i, hovered);
                continue;
            }
            DraftSlot slot = draft.get(i);
            drawDraftCard(context, px, py, slot, i, selectedDraftSlot == i || draggedDraftSlot == i, hovered);
        }
    }

    private void drawEmptyCard(DrawContext context, int x, int y, int index, boolean hovered) {
        PcStyleButton.drawFrame(context, x, y, CARD_WIDTH, CARD_HEIGHT,
                PcStyleButton.Style.NORMAL, hovered, true);
        context.fill(x + 3, y + 3, x + CARD_WIDTH - 3, y + CARD_HEIGHT - 3,
                hovered ? 0x663D6872 : 0x443A454A);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("+"), x + CARD_WIDTH / 2, y + 11,
                hovered ? 0xFFFFFFFF : 0xFFD9E5E8);
        context.drawCenteredTextWithShadow(textRenderer, ui("slot", index + 1), x + CARD_WIDTH / 2, y + 25,
                hovered ? 0xFFFFFFFF : 0xFFE4ECEE);
    }

    private void drawDraftCard(DrawContext context, int x, int y, DraftSlot slot, int index, boolean selected, boolean hovered) {
        boolean missing = slot.pokemon == null;
        context.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT,
                selected ? 0xAA4D6269 : hovered ? 0xAA4B565B : 0x88373F43);
        context.fill(x + 1, y + 1, x + CARD_WIDTH - 1, y + 2, 0x886F7B80);
        context.fill(x + 2, y + CARD_HEIGHT - 3, x + CARD_WIDTH - 2, y + CARD_HEIGHT - 1,
                missing ? COLOR_MISSING : COLOR_OK);
        if (hovered) drawOutline(context, x, y, CARD_WIDTH, CARD_HEIGHT, 0xFFC1F7FF);
        if (selected) drawOutline(context, x, y, CARD_WIDTH, CARD_HEIGHT, 0xFFFFFFFF);
    }

    private void renderStatus(DrawContext context, int left, int top) {
        if (status.isBlank()) return;
        int x = left + SCREEN_X + 8;
        int y = top + 150;
        int availableWidth = 158;
        int textWidth = textRenderer.getWidth(status);
        int color = statusError ? 0xFFFF8D8D : 0xFF8DFFB5;
        context.enableScissor(x, top + 147, x + availableWidth, top + 160);
        if (textWidth <= availableWidth) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(status), x + availableWidth / 2, y, color);
        } else {
            int travel = textWidth - availableWidth;
            long phase = (System.currentTimeMillis() / 45L) % Math.max(1L, (travel + 28L) * 2L);
            int offset = (int) (phase <= travel + 28L ? Math.max(0L, phase - 14L)
                    : Math.max(0L, (travel + 28L) * 2L - phase - 14L));
            context.drawTextWithShadow(textRenderer, Text.literal(status), x - Math.min(travel, offset), y, color);
        }
        context.disableScissor();
    }

    private void renderHoverTooltip(DrawContext context, int mouseX, int mouseY) {
        if (creating) {
            int index = cardIndexAt(mouseX, mouseY);
            if (index >= 0 && index < draft.size()) {
                DraftSlot slot = draft.get(index);
                List<Text> lines = new ArrayList<>();
                lines.add(Text.literal(displayName(slot)));
                lines.add(ui("tooltip_item_saved", itemName(slot.itemId)));
                lines.add(ui("tooltip_origin", slot.source));
                if (slot.pokemon == null) lines.add(ui("replace_required"));
                else lines.add(ui("tooltip_moves_count", slot.moveIds.size()));
                lines.add(ui("tooltip_reorder"));
                context.drawTooltip(textRenderer, lines, Optional.empty(), mouseX, mouseY);
            }
            return;
        }
        if (data.teams.isEmpty()) return;
        SavedTeam team = selectedTeam();
        int index = previewCardIndexAt(mouseX, mouseY);
        if (index >= 0 && index < team.slots.size()) {
            SavedSlot slot = team.slots.get(index);
            Pokemon pokemon = findPokemon(slot.pokemonId);
            Pokemon current = parent.getParty().get(index);
            SlotState state = slotState(slot, pokemon);
            List<Text> lines = new ArrayList<>();
            lines.add(ui("tooltip_slot", index + 1, savedSlotName(slot, pokemon)));
            lines.add(ui("tooltip_expected", itemName(slot.itemId)));
            lines.add(ui("tooltip_current", heldItemName(pokemon)));
            lines.add(Text.literal(stateText(state)));
            lines.add(ui("tooltip_action", actionLabel(current, slot, pokemon, state)));
            context.drawTooltip(textRenderer, lines, Optional.empty(), mouseX, mouseY);
        }
    }

    private void renderItemPicker(DrawContext context, int mouseX, int mouseY, int left, int top) {
        int panelX = left + 68;
        int panelY = top + 24;
        int panelWidth = 214;
        int panelHeight = 160;
        context.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0x98000000);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF283238);
        context.fill(panelX + 2, panelY + 2, panelX + panelWidth - 2, panelY + 17, 0xFF3B464B);
        drawOutline(context, panelX, panelY, panelWidth, panelHeight, 0xFF9BA5A9);
        drawOutline(context, panelX + 2, panelY + 2, panelWidth - 4, panelHeight - 4, 0xFF111719);
        context.drawCenteredTextWithShadow(textRenderer, ui("item_picker_title"), panelX + panelWidth / 2,
                panelY + 5, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, ui("item_picker_hint"), panelX + panelWidth / 2,
                panelY + 20, 0xFFA7D6E2);

        int start = itemPickerPage * ITEM_PICKER_PAGE_SIZE;
        for (int cell = 0; cell < ITEM_PICKER_PAGE_SIZE; cell++) {
            int choiceIndex = start + cell;
            if (choiceIndex >= itemChoices.size()) break;
            int x = panelX + 11 + (cell % ITEM_PICKER_COLUMNS) * 24;
            int y = panelY + 34 + (cell / ITEM_PICKER_COLUMNS) * 24;
            ItemChoice choice = itemChoices.get(choiceIndex);
            boolean hovered = isInside(mouseX, mouseY, x, y, 22, 22);
            context.fill(x, y, x + 22, y + 22, hovered ? 0xFF3F6F79 : 0xFF1A252A);
            drawOutline(context, x, y, 22, 22, hovered ? 0xFFB7F5FF : 0xFF607980);
            if (choice.stack.isEmpty()) {
                context.drawCenteredTextWithShadow(textRenderer, Text.literal("×"), x + 11, y + 7, 0xFFFF7777);
            } else {
                int itemX = x + 3;
                int itemY = y + 3;
                context.drawItem(choice.stack, itemX, itemY);
                if (choice.count > 1) {
                    String count = choice.count > 99 ? "99+" : String.valueOf(choice.count);
                    context.drawItemInSlot(textRenderer, choice.stack, itemX, itemY, count);
                }
            }
        }

        int pages = Math.max(1, (itemChoices.size() + ITEM_PICKER_PAGE_SIZE - 1) / ITEM_PICKER_PAGE_SIZE);
        int footerY = panelY + 137;
        context.drawCenteredTextWithShadow(textRenderer, ui("item_picker_page", itemPickerPage + 1, pages),
                panelX + panelWidth / 2, footerY + 3, 0xFFFFFFFF);
        if (itemPickerPage > 0) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("‹"), panelX + 14, footerY + 3, 0xFFFFFFFF);
        }
        if (itemPickerPage + 1 < pages) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("›"), panelX + panelWidth - 14,
                    footerY + 3, 0xFFFFFFFF);
        }
        context.drawCenteredTextWithShadow(textRenderer, ui("item_picker_close"), panelX + panelWidth / 2,
                panelY + 150, 0xFF91AEB5);

        int hoveredChoice = itemChoiceIndexAt(mouseX, mouseY, left, top);
        if (hoveredChoice >= 0 && hoveredChoice < itemChoices.size()) {
            ItemChoice choice = itemChoices.get(hoveredChoice);
            List<Text> lines = new ArrayList<>();
            lines.add(choice.stack.isEmpty() ? ui("item_none") : choice.stack.getName());
            if (!choice.stack.isEmpty()) lines.add(ui("item_available", choice.count));
            int shown = 0;
            for (String source : choice.sources) {
                if (shown++ == 6) {
                    lines.add(ui("item_more_sources", choice.sources.size() - 6));
                    break;
                }
                lines.add(Text.literal("• " + source));
            }
            context.drawTooltip(textRenderer, lines, Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (pokemonPickerOpen) return handlePokemonPickerClick(mouseX, mouseY, button);
        if (movePickerOpen) return handleMovePickerClick(mouseX, mouseY, button);
        if (itemPickerOpen) return handleItemPickerClick(mouseX, mouseY, button);
        if (!creating && button == 0 && !data.teams.isEmpty()) {
            int preview = previewCardIndexAt(mouseX, mouseY);
            if (preview >= 0 && preview < selectedTeam().slots.size()) {
                int cardX = previewCardX(left() + PREVIEW_START_X, preview);
                int cardY = previewCardY(top() + PREVIEW_START_Y, preview);
                if (isInside(mouseX, mouseY, cardX + PREVIEW_CARD_WIDTH - ITEM_SLOT_SIZE - 2,
                        cardY + PREVIEW_CARD_HEIGHT - ITEM_SLOT_SIZE - 4, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE)) {
                    openItemPicker(preview);
                    return true;
                }
                selectedPreviewSlot = selectedPreviewSlot == preview ? -1 : preview;
                clearAndInit();
                return true;
            }
        }
        if (creating && button == 0) {
            int index = cardIndexAt(mouseX, mouseY);
            if (index >= 0) {
                if (index < draft.size()) {
                    int cardX = cardX(left() + CARD_START_X, index);
                    int cardY = cardY(top() + CARD_START_Y, index);
                    if (isInside(mouseX, mouseY, cardX + CARD_WIDTH - ITEM_SLOT_SIZE - 2,
                            cardY + CARD_HEIGHT - ITEM_SLOT_SIZE - 4, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE)) {
                        openItemPicker(index);
                        return true;
                    }
                    draggedDraftSlot = index;
                }
                else if (draft.size() < TeamModels.MAX_TEAM_SIZE) openPicker(-1);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handlePokemonPickerClick(double mouseX, double mouseY, int button) {
        if (button != 0) return true;
        int statIndex = pokemonStatHeaderIndexAt(mouseX, mouseY);
        if (statIndex >= 0) {
            Stat stat = DISPLAY_STATS.get(statIndex);
            if (pokemonPickerSortStat != stat) {
                pokemonPickerSortStat = stat;
                pokemonPickerSortDescending = true;
            } else if (pokemonPickerSortDescending) {
                pokemonPickerSortDescending = false;
            } else {
                pokemonPickerSortStat = null;
                pokemonPickerSortDescending = true;
            }
            pokemonPickerPage = 0;
            filterPokemonChoices();
            clearAndInit();
            return true;
        }
        int index = pokemonChoiceIndexAt(mouseX, mouseY);
        if (index >= 0 && index < filteredPokemonChoices.size()) {
            PokemonChoice choice = filteredPokemonChoices.get(index);
            selectPokemon(choice);
            return true;
        }
        int panelX = left() + 5;
        if (isInside(mouseX, mouseY, panelX + 2, top() + 175, 24, 14) && pokemonPickerPage > 0) {
            pokemonPickerPage--;
            clearAndInit();
            return true;
        }
        if (isInside(mouseX, mouseY, panelX + 313, top() + 175, 24, 14)
                && pokemonPickerPage + 1 < pokemonPickerPages()) {
            pokemonPickerPage++;
            clearAndInit();
            return true;
        }
        if (!isInside(mouseX, mouseY, panelX, top() + 24, 339, 165)) {
            pickerReplaceIndex = -1;
            closePokemonPicker(true);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleMovePickerClick(double mouseX, double mouseY, int button) {
        if (button != 0) return true;
        int panelX = left() + 24;
        int panelY = top() + 21;
        for (int i = 0; i < 4; i++) {
            if (isInside(mouseX, mouseY, panelX + 8, panelY + 36 + i * 25, 124, 22)) {
                movePickerSelectedIndex = i;
                return true;
            }
        }
        for (int row = 0; row < MOVE_PICKER_ROWS; row++) {
            int index = movePickerPage * MOVE_PICKER_ROWS + row;
            if (index >= moveChoices.size()) break;
            if (isInside(mouseX, mouseY, panelX + 140, panelY + 36 + row * 20, 153, 18)) {
                chooseMove(moveChoices.get(index));
                return true;
            }
        }
        if (!isInside(mouseX, mouseY, panelX, panelY, 301, 169)) closeMovePicker(true);
        return true;
    }

    private void openMovePicker(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= draft.size() || draft.get(slotIndex).pokemon == null) return;
        movePickerSlot = slotIndex;
        movePickerSelectedIndex = 0;
        movePickerPage = 0;
        movePickerOpen = true;
        scanMoveChoices(draft.get(slotIndex).pokemon);
        clearAndInit();
    }

    private void closeMovePicker(boolean rebuild) {
        movePickerOpen = false;
        movePickerSlot = -1;
        movePickerPage = 0;
        moveChoices.clear();
        if (rebuild) clearAndInit();
    }

    private void scanMoveChoices(Pokemon pokemon) {
        moveChoices.clear();
        Set<String> seen = new HashSet<>();
        for (Move move : pokemon.getMoveSet().getMoves()) {
            if (seen.add(move.getTemplate().getName())) moveChoices.add(new MoveChoice(move.getTemplate(), true));
        }
        for (BenchedMove move : pokemon.getBenchedMoves()) {
            MoveTemplate template = move.getMoveTemplate();
            if (seen.add(template.getName())) moveChoices.add(new MoveChoice(template, false));
        }
        moveChoices.sort((first, second) -> {
            if (first.active != second.active) return first.active ? -1 : 1;
            return first.template.getDisplayName().getString()
                    .compareToIgnoreCase(second.template.getDisplayName().getString());
        });
    }

    private void chooseMove(MoveChoice choice) {
        if (movePickerSlot < 0 || movePickerSlot >= draft.size()) return;
        DraftSlot previous = draft.get(movePickerSlot);
        List<String> moves = new ArrayList<>(previous.moveIds);
        int activeSlots = Math.max(1, previous.pokemon.getMoveSet().getMoves().size());
        while (moves.size() < activeSlots) moves.add("");
        int target = Math.min(movePickerSelectedIndex, activeSlots - 1);
        int existing = moves.indexOf(choice.template.getName());
        if (existing >= 0 && existing != target) Collections.swap(moves, existing, target);
        else moves.set(target, choice.template.getName());
        moves.removeIf(String::isBlank);
        draft.set(movePickerSlot, new DraftSlot(previous.pokemon, previous.pokemonId, previous.speciesId,
                previous.itemId, moves, previous.source));
        movePickerSelectedIndex = Math.min(target + 1, activeSlots - 1);
        status = uiText("status_moves_updated", displayName(previous));
        statusError = false;
    }

    private int movePickerPages() {
        return Math.max(1, (moveChoices.size() + MOVE_PICKER_ROWS - 1) / MOVE_PICKER_ROWS);
    }

    private boolean handleItemPickerClick(double mouseX, double mouseY, int button) {
        if (button != 0) return true;
        int choiceIndex = itemChoiceIndexAt(mouseX, mouseY, left(), top());
        if (choiceIndex >= 0 && choiceIndex < itemChoices.size()) {
            chooseItem(itemChoices.get(choiceIndex));
            return true;
        }
        int panelX = left() + 68;
        int panelY = top() + 24;
        int pages = Math.max(1, (itemChoices.size() + ITEM_PICKER_PAGE_SIZE - 1) / ITEM_PICKER_PAGE_SIZE);
        if (isInside(mouseX, mouseY, panelX + 3, panelY + 135, 24, 18) && itemPickerPage > 0) {
            itemPickerPage--;
            return true;
        }
        if (isInside(mouseX, mouseY, panelX + 187, panelY + 135, 24, 18) && itemPickerPage + 1 < pages) {
            itemPickerPage++;
            return true;
        }
        if (!isInside(mouseX, mouseY, panelX, panelY, 214, 160)
                || isInside(mouseX, mouseY, panelX + 55, panelY + 147, 104, 13)) {
            closeItemPicker();
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (pokemonPickerOpen) {
            if (verticalAmount < 0 && pokemonPickerPage + 1 < pokemonPickerPages()) {
                pokemonPickerPage++;
                clearAndInit();
            } else if (verticalAmount > 0 && pokemonPickerPage > 0) {
                pokemonPickerPage--;
                clearAndInit();
            }
            return true;
        }
        if (movePickerOpen) {
            if (verticalAmount < 0 && movePickerPage + 1 < movePickerPages()) movePickerPage++;
            else if (verticalAmount > 0 && movePickerPage > 0) movePickerPage--;
            return true;
        }
        if (itemPickerOpen) {
            int pages = Math.max(1, (itemChoices.size() + ITEM_PICKER_PAGE_SIZE - 1) / ITEM_PICKER_PAGE_SIZE);
            if (verticalAmount < 0 && itemPickerPage + 1 < pages) itemPickerPage++;
            else if (verticalAmount > 0 && itemPickerPage > 0) itemPickerPage--;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (pokemonPickerOpen && keyCode == 256) {
            pickerReplaceIndex = -1;
            closePokemonPicker(true);
            return true;
        }
        if (movePickerOpen && keyCode == 256) {
            closeMovePicker(true);
            return true;
        }
        if (itemPickerOpen && keyCode == 256) {
            closeItemPicker();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        if (pokemonPickerNeedsRebuild && pokemonPickerOpen) {
            pokemonPickerNeedsRebuild = false;
            clearAndInit();
            if (pokemonSearchField != null) setFocused(pokemonSearchField);
        }
    }

    private void openItemPicker(int slot) {
        itemPickerSlot = slot;
        itemPickerPage = 0;
        itemPickerOpen = true;
        scanAvailableItems();
        clearAndInit();
    }

    private void closeItemPicker() {
        boolean rebuild = itemPickerOpen;
        itemPickerOpen = false;
        itemPickerSlot = -1;
        itemPickerPage = 0;
        itemChoices.clear();
        if (rebuild) clearAndInit();
    }

    private void scanAvailableItems() {
        itemChoices.clear();
        itemChoices.add(new ItemChoice("minecraft:air", ItemStack.EMPTY, 0));
        if (client == null || client.player == null) return;

        for (int slot = 0; slot < Math.min(36, client.player.getInventory().size()); slot++) {
            ItemStack stack = client.player.getInventory().getStack(slot);
            if (stack.isEmpty()) continue;
            addItemChoice(stack, stack.getCount(), uiText("item_source_inventory"));
        }

        if (itemChoices.size() > 2) {
            itemChoices.subList(1, itemChoices.size()).sort((first, second) ->
                    first.stack.getName().getString().compareToIgnoreCase(second.stack.getName().getString()));
        }
    }

    private void addItemChoice(ItemStack stack, int count, String source) {
        if (stack.isEmpty() || (!stack.isIn(PVP_ITEM_TAG) && !stack.isIn(PVP_BERRY_TAG))) return;
        String id = itemId(stack);
        for (ItemChoice choice : itemChoices) {
            if (choice.itemId.equals(id)) {
                choice.count += Math.max(1, count);
                choice.sources.add(source);
                return;
            }
        }
        ItemStack icon = stack.copy();
        icon.setCount(1);
        ItemChoice choice = new ItemChoice(id, icon, Math.max(1, count));
        choice.sources.add(source);
        itemChoices.add(choice);
    }

    private void chooseItem(ItemChoice choice) {
        if (itemPickerSlot < 0) {
            closeItemPicker();
            return;
        }
        String selectedItemId = choice.itemId;
        String selectedName = choice.stack.isEmpty() ? uiText("item_none") : choice.stack.getName().getString();
        if (creating && itemPickerSlot < draft.size()) {
            DraftSlot previous = draft.get(itemPickerSlot);
            draft.set(itemPickerSlot, new DraftSlot(previous.pokemon, previous.pokemonId, previous.speciesId,
                    selectedItemId, previous.moveIds, previous.source));
            status = uiText("status_item_selected", selectedName);
            statusError = false;
        } else if (!creating && !data.teams.isEmpty() && itemPickerSlot < selectedTeam().slots.size()) {
            SavedTeam team = selectedTeam();
            SavedTeam before = copyTeam(team);
            team.slots.get(itemPickerSlot).itemId = selectedItemId;
            if (repository.save(client, data)) {
                String teamId = team.id;
                setUndo(uiText("undo_item", team.name), () -> restoreTeam(teamId, before));
                status = uiText("status_item_selected", selectedName);
                statusError = false;
            } else {
                team.slots.get(itemPickerSlot).itemId = before.slots.get(itemPickerSlot).itemId;
                status = uiText("error_save");
                statusError = true;
            }
        }
        closeItemPicker();
    }

    private int itemChoiceIndexAt(double mouseX, double mouseY, int left, int top) {
        int gridX = left + 79;
        int gridY = top + 58;
        for (int cell = 0; cell < ITEM_PICKER_PAGE_SIZE; cell++) {
            int index = itemPickerPage * ITEM_PICKER_PAGE_SIZE + cell;
            if (index >= itemChoices.size()) break;
            int x = gridX + (cell % ITEM_PICKER_COLUMNS) * 24;
            int y = gridY + (cell / ITEM_PICKER_COLUMNS) * 24;
            if (isInside(mouseX, mouseY, x, y, 22, 22)) return index;
        }
        return -1;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (creating && button == 0 && draggedDraftSlot >= 0) {
            int source = draggedDraftSlot;
            int target = cardIndexAt(mouseX, mouseY);
            draggedDraftSlot = -1;
            if (target >= 0 && target < draft.size()) {
                if (target != source) {
                    Collections.swap(draft, source, target);
                    selectedDraftSlot = target;
                    status = uiText("status_order", source + 1, target + 1);
                } else if (selectedDraftSlot >= 0 && selectedDraftSlot != target) {
                    int previous = selectedDraftSlot;
                    Collections.swap(draft, previous, target);
                    selectedDraftSlot = target;
                    status = uiText("status_order", previous + 1, target + 1);
                } else {
                    selectedDraftSlot = selectedDraftSlot == target ? -1 : target;
                    status = selectedDraftSlot < 0 ? uiText("status_deselected") : uiText("status_selected", target + 1);
                }
                statusError = false;
                clearAndInit();
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private TeamAnalysis analysis(SavedTeam team) {
        int changes = 0;
        int itemDifferences = 0;
        int moveDifferences = 0;
        int missing = 0;
        for (int i = 0; i < TeamModels.MAX_TEAM_SIZE; i++) {
            Pokemon current = parent.getParty().get(i);
            String currentId = current == null ? null : current.getUuid().toString();
            SavedSlot target = i < team.slots.size() ? team.slots.get(i) : null;
            String targetId = target == null ? null : target.pokemonId;
            if (currentId == null ? targetId != null : !currentId.equals(targetId)) changes++;
            if (target != null) {
                Pokemon pokemon = findPokemon(target.pokemonId);
                SlotState state = slotState(target, pokemon);
                if (state == SlotState.MISSING) missing++;
                else if (state == SlotState.ITEM_MISMATCH) itemDifferences++;
                if (pokemon != null && target.moveIds != null && !target.moveIds.isEmpty()
                        && !target.moveIds.equals(activeMoveIds(pokemon))) moveDifferences++;
            }
        }
        return new TeamAnalysis(changes, itemDifferences, moveDifferences, missing);
    }

    private String analysisSummary(TeamAnalysis analysis) {
        if (analysis.missing > 0) return uiText("summary_missing", analysis.missing, analysis.changes);
        if (analysis.changes == 0 && analysis.itemDifferences == 0 && analysis.moveDifferences == 0) {
            return uiText("summary_ready");
        }
        return uiText("summary_changes_moves", analysis.changes, analysis.itemDifferences, analysis.moveDifferences);
    }

    private SlotState slotState(SavedSlot slot, Pokemon pokemon) {
        if (pokemon == null) return SlotState.MISSING;
        return normalizedItemId(slot.itemId).equals(itemId(pokemon.getHeldItem$common())) ? SlotState.MATCH : SlotState.ITEM_MISMATCH;
    }

    private String stateText(SlotState state) {
        return switch (state) {
            case MATCH -> uiText("state_match");
            case ITEM_MISMATCH -> uiText("state_item");
            case MISSING -> uiText("state_missing");
            case EMPTY -> uiText("state_empty");
        };
    }

    private String actionLabel(Pokemon current, SavedSlot targetSlot, Pokemon target, SlotState state) {
        if (targetSlot == null) return current == null ? uiText("action_already_empty") : uiText("action_to_pc");
        if (state == SlotState.MISSING) return uiText("action_missing");
        if (current != null && current.getUuid().toString().equals(targetSlot.pokemonId)) {
            return state == SlotState.ITEM_MISMATCH ? uiText("action_item") : uiText("action_ready");
        }
        if (target != null && parent.getParty().findByUUID(target.getUuid()) != null) return uiText("action_swap");
        return currentSource(target);
    }

    private int cardIndexAt(double mouseX, double mouseY) {
        int x = left() + CARD_START_X;
        int y = top() + CARD_START_Y;
        for (int i = 0; i < TeamModels.MAX_TEAM_SIZE; i++) {
            if (isInside(mouseX, mouseY, cardX(x, i), cardY(y, i), CARD_WIDTH, CARD_HEIGHT)) return i;
        }
        return -1;
    }

    private int previewCardIndexAt(double mouseX, double mouseY) {
        for (int i = 0; i < TeamModels.MAX_TEAM_SIZE; i++) {
            int x = previewCardX(left() + PREVIEW_START_X, i);
            int y = previewCardY(top() + PREVIEW_START_Y, i);
            if (isInside(mouseX, mouseY, x, y, PREVIEW_CARD_WIDTH, PREVIEW_CARD_HEIGHT)) return i;
        }
        return -1;
    }

    private int previewCardX(int x, int index) {
        return x + (index % 3) * 53;
    }

    private int previewCardY(int y, int index) {
        return y + (index / 3) * 49;
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private String currentSource(Pokemon pokemon) {
        if (pokemon == null) return uiText("not_found");
        if (parent.getParty().findByUUID(pokemon.getUuid()) != null) return uiText("source_party");
        PCPosition position = parent.getPc().getPosition(pokemon);
        return position == null ? uiText("source_pc") : uiText("source_box", position.getBox() + 1);
    }

    private String pokemonName(Pokemon pokemon) {
        return pokemon == null ? uiText("state_empty") : pokemon.getDisplayName(false).getString();
    }

    private String savedSlotName(SavedSlot slot, Pokemon pokemon) {
        if (pokemon != null) return pokemonName(pokemon);
        return speciesName(findSpecies(slot.speciesId));
    }

    private String displayName(DraftSlot slot) {
        return slot.pokemon == null ? speciesName(findSpecies(slot.speciesId)) : pokemonName(slot.pokemon);
    }

    private String heldItemName(Pokemon pokemon) {
        return pokemon == null || pokemon.getHeldItem$common().isEmpty() ? uiText("no_item") : pokemon.getHeldItem$common().getName().getString();
    }

    private Text natureName(Pokemon pokemon) {
        String path = pokemon.getEffectiveNature().getName().getPath();
        return Text.translatable("cobblemon.nature." + path);
    }

    private Text abilityName(Pokemon pokemon) {
        return Text.translatable("cobblemon.ability." + pokemon.getAbility().getName());
    }

    private boolean isHiddenAbility(Pokemon pokemon) {
        String currentAbility = pokemon.getAbility().getTemplate().getName();
        for (PotentialAbility potential : pokemon.getForm().getAbilities()) {
            if (potential instanceof HiddenAbility
                    && potential.getTemplate().getName().equalsIgnoreCase(currentAbility)) {
                return true;
            }
        }
        return false;
    }

    private String itemName(String rawId) {
        ItemStack stack = itemStack(rawId);
        return stack.isEmpty() ? uiText("no_item") : stack.getName().getString();
    }

    private ItemStack itemStack(String rawId) {
        Identifier id = Identifier.tryParse(normalizedItemId(rawId));
        if (id == null || !Registries.ITEM.containsId(id) || id.equals(Identifier.of("minecraft", "air"))) return ItemStack.EMPTY;
        return new ItemStack(Registries.ITEM.get(id));
    }

    private String itemId(ItemStack stack) {
        return stack == null || stack.isEmpty() ? "minecraft:air" : Registries.ITEM.getId(stack.getItem()).toString();
    }

    private String normalizedItemId(String rawId) {
        return rawId == null || rawId.isBlank() ? "minecraft:air" : rawId;
    }

    private int pokemonChoiceIndexAt(double mouseX, double mouseY) {
        int panelX = left() + 5;
        int firstY = top() + 59;
        if (!isInside(mouseX, mouseY, panelX + 2, firstY, 335,
                POKEMON_PICKER_ROWS * POKEMON_PICKER_ROW_HEIGHT)) return -1;
        int row = ((int) mouseY - firstY) / POKEMON_PICKER_ROW_HEIGHT;
        int index = pokemonPickerPage * POKEMON_PICKER_ROWS + row;
        return index < filteredPokemonChoices.size() ? index : -1;
    }

    private int pokemonStatHeaderIndexAt(double mouseX, double mouseY) {
        int panelX = left() + 5;
        if (!isInside(mouseX, mouseY, panelX + 231, top() + 46, 108, 13)) return -1;
        return Math.min(DISPLAY_STATS.size() - 1, ((int) mouseX - panelX - 231) / 18);
    }

    private Text statHeader(Stat stat) {
        if (stat == Stats.HP) return ui("stat_hp");
        if (stat == Stats.ATTACK) return ui("stat_attack");
        if (stat == Stats.DEFENCE) return ui("stat_defence");
        if (stat == Stats.SPECIAL_ATTACK) return ui("stat_special_attack");
        if (stat == Stats.SPECIAL_DEFENCE) return ui("stat_special_defence");
        return ui("stat_speed");
    }

    private Text compactStatHeader(Stat stat) {
        if (stat == Stats.HP) return ui("pokemon_picker_stat_hp");
        if (stat == Stats.ATTACK) return ui("pokemon_picker_stat_attack");
        if (stat == Stats.DEFENCE) return ui("pokemon_picker_stat_defence");
        if (stat == Stats.SPECIAL_ATTACK) return ui("pokemon_picker_stat_special_attack");
        if (stat == Stats.SPECIAL_DEFENCE) return ui("pokemon_picker_stat_special_defence");
        return ui("pokemon_picker_stat_speed");
    }

    private String pokemonChoiceCompactDetail(PokemonChoice choice) {
        if (choice.pokemon == null) return choice.source;
        String source;
        if (choice.position instanceof PCPosition pcPosition) {
            source = uiText("pokemon_picker_source_box_compact", pcPosition.getBox() + 1);
        } else if (choice.position instanceof PartyPosition) {
            source = uiText("pokemon_picker_source_party_compact");
        } else {
            source = choice.source;
        }
        return source + " · " + uiText("pokemon_picker_level_compact", choice.pokemon.getLevel());
    }

    private void drawTypeLabel(DrawContext context, Text label, String typeName, int x, int y) {
        int width = 44;
        int color = typeColor(typeName);
        context.fill(x, y, x + width, y + 10, color);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, lighten(color));
        drawCenteredPlainText(context, Text.literal(fitText(label.getString(), width - 4)),
                x + width / 2, y + 1, 0xFFFFFFFF);
    }

    private int typeColor(String typeName) {
        return switch (typeName.toLowerCase(Locale.ROOT)) {
            case "normal" -> 0xFFA0A29F;
            case "fire" -> 0xFFF08030;
            case "water" -> 0xFF4C91D7;
            case "electric" -> 0xFFE0B629;
            case "grass" -> 0xFF5FAE58;
            case "ice" -> 0xFF63B6B8;
            case "fighting" -> 0xFFC45544;
            case "poison" -> 0xFF9B5AA3;
            case "ground" -> 0xFFC99B55;
            case "flying" -> 0xFF7D91CC;
            case "psychic" -> 0xFFE36D91;
            case "bug" -> 0xFF92A63B;
            case "rock" -> 0xFFA58D57;
            case "ghost" -> 0xFF6B638F;
            case "dragon" -> 0xFF5D66B6;
            case "dark" -> 0xFF625B58;
            case "steel" -> 0xFF6F8C98;
            case "fairy" -> 0xFFD986B5;
            default -> 0xFF687A82;
        };
    }

    private int lighten(int color) {
        int red = Math.min(255, ((color >> 16) & 255) + 38);
        int green = Math.min(255, ((color >> 8) & 255) + 38);
        int blue = Math.min(255, (color & 255) + 38);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private void drawScaledText(DrawContext context, Text text, int x, int y, int color, float scale) {
        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, 1.0F);
        context.drawText(textRenderer, text, Math.round(x / scale), Math.round(y / scale), color, false);
        context.getMatrices().pop();
    }

    private void drawScaledCenteredText(DrawContext context, Text text, int centerX, int y, int color, float scale) {
        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, 1.0F);
        context.drawCenteredTextWithShadow(textRenderer, text, Math.round(centerX / scale),
                Math.round(y / scale), color);
        context.getMatrices().pop();
    }

    private void drawScaledCenteredPlainText(DrawContext context, Text text, int centerX, int y,
                                             int color, float scale) {
        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, 1.0F);
        int scaledCenterX = Math.round(centerX / scale);
        int drawX = scaledCenterX - textRenderer.getWidth(text) / 2;
        context.drawText(textRenderer, text, drawX, Math.round(y / scale), color, false);
        context.getMatrices().pop();
    }

    private void drawCenteredPlainText(DrawContext context, Text text, int centerX, int y, int color) {
        context.drawText(textRenderer, text, centerX - textRenderer.getWidth(text) / 2, y, color, false);
    }

    private void drawMarqueeText(DrawContext context, Text text, int x, int y, int width, int color) {
        int textWidth = textRenderer.getWidth(text);
        if (textWidth <= width) {
            context.drawText(textRenderer, text, x, y, color, false);
            return;
        }

        int overflow = textWidth - width;
        int pause = 16;
        int cycle = pause * 2 + overflow * 2;
        int phase = (int) ((net.minecraft.util.Util.getMeasuringTimeMs() / 75L) % cycle);
        int offset;
        if (phase < pause) offset = 0;
        else if (phase < pause + overflow) offset = phase - pause;
        else if (phase < pause * 2 + overflow) offset = overflow;
        else offset = overflow - (phase - pause * 2 - overflow);

        context.enableScissor(x, y - 1, x + width, y + 10);
        context.drawText(textRenderer, text, x - offset, y, color, false);
        context.disableScissor();
    }

    private UUID parseUuid(String raw) {
        try { return UUID.fromString(raw); }
        catch (Exception ignored) { return null; }
    }

    private int cardX(int x, int index) {
        return x + (index % 3) * 53;
    }

    private int cardY(int y, int index) {
        return y + (index / 3) * 49;
    }

    private void drawOutline(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    private int left() {
        return (width - PANEL_WIDTH) / 2;
    }

    private int top() {
        return (height - PANEL_HEIGHT) / 2;
    }

    private String trim(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…";
    }

    private String fitText(String value, int maxWidth) {
        if (value == null || value.isEmpty() || textRenderer.getWidth(value) <= maxWidth) return value == null ? "" : value;
        String suffix = "…";
        int suffixWidth = textRenderer.getWidth(suffix);
        int end = value.length();
        while (end > 0 && textRenderer.getWidth(value.substring(0, end)) + suffixWidth > maxWidth) end--;
        return value.substring(0, end) + suffix;
    }

    private Text ui(String key, Object... arguments) {
        return Text.translatable("screen.tropimon_team_saver." + key, arguments);
    }

    private String uiText(String key, Object... arguments) {
        return ui(key, arguments).getString();
    }

    private void openBoxes() {
        if (client != null) client.setScreen(standalone ? null : parent);
    }

    @Override
    public void close() {
        openBoxes();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private enum SlotState {
        MATCH(COLOR_OK), ITEM_MISMATCH(COLOR_ITEM), MISSING(COLOR_MISSING), EMPTY(0xFF657C82);
        final int color;
        SlotState(int color) { this.color = color; }
    }

    private record TeamAnalysis(int changes, int itemDifferences, int moveDifferences, int missing) {
    }

    private record DraftSlot(Pokemon pokemon, String pokemonId, String speciesId, String itemId,
                             List<String> moveIds, String source) {
    }

    private record PokemonChoice(Pokemon pokemon, Species species, StorePosition position, String source,
                                 boolean owned) {
    }

    private record MoveChoice(MoveTemplate template, boolean active) {
    }

    private static final class ItemChoice {
        private final String itemId;
        private final ItemStack stack;
        private int count;
        private final Set<String> sources = new LinkedHashSet<>();

        private ItemChoice(String itemId, ItemStack stack, int count) {
            this.itemId = itemId;
            this.stack = stack;
            this.count = count;
        }
    }
}
