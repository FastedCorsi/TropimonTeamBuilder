package fr.tropimon.teamsaver.client;

import com.cobblemon.mod.common.api.abilities.PotentialAbility;
import com.cobblemon.mod.common.api.moves.BenchedMove;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.storage.StorePosition;
import com.cobblemon.mod.common.api.storage.party.PartyPosition;
import com.cobblemon.mod.common.api.storage.pc.PCPosition;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.client.gui.pc.PCGUI;
import com.cobblemon.mod.common.client.render.gui.PCBoxWallpaperRepository;
import com.cobblemon.mod.common.client.gui.summary.widgets.ModelWidget;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.pokemon.FormData;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
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
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class TeamManagerScreen extends FittedScreen {
    private static final Identifier PC_BASE = Identifier.of("cobblemon", "textures/gui/pc/pc_base.png");
    private static final Identifier SCREEN_OVERLAY = Identifier.of("cobblemon", "textures/gui/pc/pc_screen_overlay.png");
    private static final Identifier TYPE_ICONS = Identifier.of("cobblemon", "textures/gui/types.png");
    private static final Identifier SUMMARY_PARTY_SLOT = Identifier.of("cobblemon",
            "textures/gui/summary/summary_party_slot_empty.png");
    private static final Identifier SUMMARY_SIDE_SPACER = Identifier.of("cobblemon",
            "textures/gui/summary/summary_side_spacer.png");
    private static final int PANEL_WIDTH = 465;
    private static final int PANEL_HEIGHT = 255;
    private static final int PAGE_SIZE = 8;
    private static final int LIST_X = 6;
    private static final int LIST_START_Y = 61;
    private static final int LIST_ROW_HEIGHT = 19;
    private static final int LEFT_CONTROL_WIDTH = 70;
    private static final int SIDEBAR_NAV_WIDTH = 18;
    private static final int SIDEBAR_NAV_LEFT_X = LIST_X + 2;
    private static final int SIDEBAR_NAV_RIGHT_X = LIST_X + LEFT_CONTROL_WIDTH - SIDEBAR_NAV_WIDTH;
    private static final int SIDEBAR_CENTER_X = LIST_X + LEFT_CONTROL_WIDTH / 2;
    private static final int SCREEN_X = 86;
    private static final int SCREEN_WIDTH = 290;
    private static final int SCREEN_HEIGHT = 205;
    private static final int SCREEN_CENTER_X = SCREEN_X + SCREEN_WIDTH / 2;
    private static final int PARTY_X = SCREEN_X + SCREEN_WIDTH + 3;
    private static final int CARD_START_X = SCREEN_X + 9;
    private static final int CARD_START_Y = 49;
    private static final int CARD_WIDTH = 88;
    private static final int CARD_HEIGHT = 70;
    private static final int CARD_GAP_X = 4;
    private static final int CARD_GAP_Y = 4;
    private static final int PREVIEW_CARD_WIDTH = CARD_WIDTH;
    private static final int PREVIEW_CARD_HEIGHT = CARD_HEIGHT;
    private static final int ITEM_SLOT_SIZE = 18;
    private static final int PREVIEW_START_X = SCREEN_X + 9;
    private static final int PREVIEW_START_Y = 49;
    private static final int STATUS_Y = 197;
    private static final int MAIN_ACTION_Y = 214;
    private static final int MAIN_ACTION_WIDTH = 88;
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
    private static final int POKEMON_PICKER_MIN_ROWS = 5;
    private static final int POKEMON_PICKER_ROW_HEIGHT = 23;
    private static final int POKEMON_PICKER_MAX_WIDTH = 430;
    private static final int POKEMON_PICKER_MAX_HEIGHT = 276;
    private static final int MOVE_PICKER_ROWS = 8;
    private static final int MOVE_PICKER_WIDTH = 385;
    private static final int MOVE_PICKER_HEIGHT = 225;
    private static final int MOVE_PICKER_PRESET_WIDTH = 148;
    private static final int MOVE_PICKER_LIST_X = 164;
    private static final int MOVE_PICKER_LIST_WIDTH = 213;
    private static final long MARQUEE_PAUSE_MS = 1_200L;
    private static final long MARQUEE_TRAVEL_MS = 2_800L;
    private static final long DOUBLE_CLICK_MS = 350L;
    private static final double SLOT_DRAG_THRESHOLD_SQUARED = 9.0;
    private static final int RANKED_VARIATION_ATTEMPTS = 8;
    private static final AtomicLong RANKED_VARIATION = new AtomicLong();
    private static boolean rememberedPokemonPickerAllSpecies;
    private static final List<String> NATURE_IDS = List.of(
            "hardy", "lonely", "brave", "adamant", "naughty",
            "bold", "docile", "relaxed", "impish", "lax",
            "timid", "hasty", "serious", "jolly", "naive",
            "modest", "mild", "quiet", "bashful", "rash",
            "calm", "gentle", "sassy", "careful", "quirky");
    private static final List<String> EV_KEYS = List.of("hp", "atk", "def", "spa", "spd", "spe");
    private static final List<String> MONOTYPE_TYPES = List.of(
            "normal", "fire", "water", "electric", "grass", "ice", "fighting", "poison", "ground",
            "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark", "steel", "fairy");
    private static final List<Stat> DISPLAY_STATS = List.of(
            Stats.HP, Stats.ATTACK, Stats.DEFENCE, Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED);

    private final PCGUI parent;
    private final boolean standalone;
    private String autoApplyTeamId;
    private final LocalTeamRepository repository = new LocalTeamRepository();
    private final OwnedPokemonIndex ownedPokemon = new OwnedPokemonIndex();
    private final CobblemonCatalogueCache catalogue = CobblemonCatalogueCache.INSTANCE;
    private final List<DraftSlot> draft = new ArrayList<>();
    private PlayerData data;
    private int selectedTeam;
    private int page;
    private boolean reorderingTeams;
    private final TeamListDrag teamDrag = new TeamListDrag();
    private int teamDragEdgeTicks;
    private int teamDragEdgeDirection;
    private boolean creating;
    private boolean confirmDelete;
    private int selectedDraftSlot = -1;
    private int selectedPreviewSlot = -1;
    private int draggedDraftSlot = -1;
    private double draftPressX;
    private double draftPressY;
    private double draftDragX;
    private double draftDragY;
    private boolean draftDragging;
    private int draggedPreviewSlot = -1;
    private double previewPressX;
    private double previewPressY;
    private double previewDragX;
    private double previewDragY;
    private boolean previewDragging;
    private int lastPreviewClickSlot = -1;
    private long lastPreviewClickTime;
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
    private boolean setEditorOpen;
    private int setEditorSlot = -1;
    private final Map<String, TextFieldWidget> evInputFields = new LinkedHashMap<>();
    private boolean syncingEvInputFields;
    private String heldEvStat;
    private int heldEvDirection;
    private int heldEvTicks;
    private boolean returnToSetEditor;
    private boolean pokemonPickerOpen;
    private int pokemonPickerPage;
    private String pokemonPickerQuery = "";
    private TextFieldWidget pokemonSearchField;
    private final Set<UUID> pokemonPickerExcluded = new HashSet<>();
    private final List<PokemonChoice> pokemonChoices = new ArrayList<>();
    private final List<PokemonChoice> filteredPokemonChoices = new ArrayList<>();
    private final List<ModelWidget> pokemonPickerModels = new ArrayList<>();
    private final Map<ModelWidget, RenderIdentity> pickerModelIdentities = new java.util.IdentityHashMap<>();
    private boolean pokemonPickerAllSpecies = rememberedPokemonPickerAllSpecies;
    private boolean pokemonPickerAssociationOnly;
    private PokemonChoice pendingAbilityPokemon;
    private int abilityEditSlot = -1;
    private Stat pokemonPickerSortStat;
    private boolean pokemonPickerSortDescending = true;
    private boolean movePickerOpen;
    private int movePickerSlot = -1;
    private int movePickerSelectedIndex;
    private int movePickerPage;
    private final List<MoveChoice> moveChoices = new ArrayList<>();
    private final Map<String, Map<String, Double>> rankedMoveUsage = new LinkedHashMap<>();
    private int rankedMoveUsageRequest;
    private CompletableFuture<?> rankedMoveUsageFuture;
    private final Map<String, RankedUsageService.UsageEntry> rankedUsage = new LinkedHashMap<>();
    private final List<String> rankedSeasons = new ArrayList<>();
    private String rankedSeason = "";
    private boolean rankedSeasonMenuOpen;
    private boolean rankedUsageLoading;
    private int rankedUsageRequest;
    private CompletableFuture<?> rankedUsageFuture;
    private boolean rankedHelperRunning;
    private int rankedHelperRequest;
    private CompletableFuture<?> rankedHelperFuture;
    private boolean rankedTeamGenerated;
    private final List<DraftSlot> rankedGenerationCore = new ArrayList<>();
    private final List<DraftSlot> rankedRegenerationSnapshot = new ArrayList<>();
    private final Set<Integer> rankedLockedSlots = new LinkedHashSet<>();
    private final Map<String, RankedReason> rankedReasons = new LinkedHashMap<>();
    private RankedUsageService.BuildStyle rankedBuildStyle = RankedUsageService.BuildStyle.BALANCED;
    private String rankedMonotypeType = "water";
    private RankedUsageService.GenerationMode rankedGenerationMode = RankedUsageService.GenerationMode.MIXED;
    private RankedUsageService.Recommendation rankedProposal;
    private boolean rankedProposalReplacesDraft;
    private Map<String, RankedLocalTemplate> rankedTemplateCache;
    private Map<String, RankedUsageService.CandidateProfile> rankedProfileCache;
    private boolean teamDoctorOpen;
    private String lastApplyTeamId;
    private boolean applyInBackground;
    private final ReadModelCache<TeamReadKey, TeamAnalysis> browserAnalysisCache = new ReadModelCache<>();
    private final ReadModelCache<TeamReadKey, TeamValidationService.Report> draftValidationCache = new ReadModelCache<>();
    private final TeamDoctorCache teamDoctorCache = new TeamDoctorCache();
    private List<TeamDoctor.Finding> doctorFindings = List.of();
    private TeamAnalysis browserAnalysis;
    private String browserSummary = "";
    private String browserSummaryLanguage = "";
    private TeamValidationService.Report displayedDraftValidation;
    private String draftSummaryLanguage = "";
    private PcStyleButton equipButton;
    private PcStyleButton saveButton;
    private final Map<PokemonChoice, String> pickerSearchText = new java.util.IdentityHashMap<>();
    private final Map<PokemonChoice, String> pickerNames = new java.util.IdentityHashMap<>();
    private final Map<PokemonChoice, String> pickerDetails = new java.util.IdentityHashMap<>();
    private final Map<PokemonChoice, Text> pickerAbilities = new java.util.IdentityHashMap<>();
    private Map<String, Integer> itemPickerInventory = Map.of();
    private long observedPokemonRevision = -1;
    private long observedCatalogueRevision = -1;
    private String observedLanguage = "";
    private boolean pickerRefreshPending;

    TeamManagerScreen(PCGUI parent) {
        this(parent, false, null);
    }

    TeamManagerScreen(PCGUI parent, boolean standalone) {
        this(parent, standalone, null);
    }

    TeamManagerScreen(PCGUI parent, boolean standalone, String autoApplyTeamId) {
        super(Text.translatable("screen.tropimon_team_saver.title"), 473, 263);
        this.parent = parent;
        this.standalone = standalone;
        this.autoApplyTeamId = autoApplyTeamId;
        this.data = repository.load(MinecraftClient.getInstance());
        this.ownedPokemon.refresh(parent);
    }

    @Override
    protected void initContent() {
        ownedPokemon.refreshIfChanged(parent);
        equipButton = null;
        saveButton = null;
        selectedTeam = Math.max(0, Math.min(selectedTeam, Math.max(0, data.teams.size() - 1)));
        page = Math.max(0, Math.min(page, Math.max(0, (data.teams.size() - 1) / PAGE_SIZE)));
        if (pokemonPickerOpen) {
            initPokemonPicker();
            return;
        }
        if (movePickerOpen) return;
        if (itemPickerOpen) return;
        if (setEditorOpen) {
            initSetEditorInputs();
            return;
        }
        if (teamDoctorOpen) {
            refreshDoctor();
            return;
        }
        initTabs(left(), top());
        if (creating) initEditor(left(), top());
        else initBrowser(left(), top());
        if (autoApplyTeamId != null && client != null) {
            String teamId = autoApplyTeamId;
            autoApplyTeamId = null;
            client.execute(() -> {
                applyTeamById(teamId);
                if (ClientTeamApplier.isActive(this)) continueApplyInBackground();
            });
        }
    }

    private void initTabs(int left, int top) {
        int y = Math.max(1, top - 12);
        PcStyleButton boxes = new PcStyleButton(left + PARTY_X, y, 40, 16,
                standalone ? ui("close") : ui("boxes"), button -> openBoxes());
        boolean applying = ClientTeamApplier.isActive(this);
        boxes.active = !applying;
        boxes.setTooltip(Tooltip.of(applying ? ui("tooltip_apply_locked")
                : standalone ? ui("tooltip_close") : ui("tooltip_boxes")));
        addDrawableChild(boxes);
        PcStyleButton teams = new PcStyleButton(left + PARTY_X + 42, y, 40, 16, ui("teams"), button -> {},
                PcStyleButton.Style.SELECTED);
        teams.active = !applying;
        addDrawableChild(teams);
    }

    private void initBrowser(int left, int top) {
        boolean applying = ClientTeamApplier.isActive(this);
        PcStyleButton create = new PcStyleButton(left + LIST_X, top + 40, LEFT_CONTROL_WIDTH, 16,
                ui("new"), button -> startCreating());
        create.active = !applying;
        addDrawableChild(create);

        PcStyleButton paste = new PcStyleButton(left + PARTY_X + 6, top + 143, 70, 16,
                ui("paste_import"), button -> client.setScreen(new PasteImportScreen(this))).withScrollingText();
        paste.active = !applying;
        addDrawableChild(paste);

        PcStyleButton reorder = new PcStyleButton(left + PARTY_X + 6, top + 162, 70, 16,
                ui(reorderingTeams ? "finish_reordering" : "reorder_teams"), button -> toggleTeamReordering(),
                reorderingTeams ? PcStyleButton.Style.SELECTED : PcStyleButton.Style.NORMAL).withScrollingText();
        reorder.active = !applying && (reorderingTeams || data.teams.size() > 1);
        reorder.setTooltip(Tooltip.of(ui("tooltip_reorder_teams")));
        addDrawableChild(reorder);

        int start = page * PAGE_SIZE;
        for (int row = 0; row < PAGE_SIZE && start + row < data.teams.size(); row++) {
            int index = start + row;
            SavedTeam team = data.teams.get(index);
            PcStyleButton.Style style = index == selectedTeam ? PcStyleButton.Style.SELECTED : PcStyleButton.Style.NORMAL;
            PcStyleButton entry = new PcStyleButton(left + LIST_X, top + LIST_START_Y + row * LIST_ROW_HEIGHT, LEFT_CONTROL_WIDTH, 16,
                    Text.literal(team.name), button -> selectTeam(index), style).withScrollingText();
            entry.active = !applying;
            if (!reorderingTeams) entry.setTooltip(Tooltip.of(ui("tooltip_team_count", team.name, team.slots.size())));
            addDrawableChild(entry);
        }

        if (page > 0) {
            PcStyleButton previous = new PcStyleButton(left + SIDEBAR_NAV_LEFT_X, top + 219,
                    SIDEBAR_NAV_WIDTH, 14, Text.literal("‹"),
                    button -> changePage(-1));
            previous.active = !applying;
            addDrawableChild(previous);
        }
        if ((page + 1) * PAGE_SIZE < data.teams.size()) {
            PcStyleButton next = new PcStyleButton(left + SIDEBAR_NAV_RIGHT_X, top + 219,
                    SIDEBAR_NAV_WIDTH, 14, Text.literal("›"),
                    button -> changePage(1));
            next.active = !applying;
            addDrawableChild(next);
        }

        if (data.teams.isEmpty()) {
            addDrawableChild(new PcStyleButton(left + SCREEN_X + (SCREEN_WIDTH - 88) / 2, top + 154, 88, 16,
                    ui("create"), button -> startCreating()));
        } else {
            SavedTeam team = selectedTeam();
            addBrowserModels(team, left, top);
            TeamAnalysis teamAnalysis = cachedBrowserAnalysis(team);

            boolean retry = !applying && statusError && team.id.equals(lastApplyTeamId);
            PcStyleButton equip = new PcStyleButton(left + SCREEN_X + 6, top + MAIN_ACTION_Y,
                    MAIN_ACTION_WIDTH, 16,
                    ui(applying ? "cancel_apply" : retry ? "retry_apply" : "apply"),
                    button -> {
                        if (ClientTeamApplier.isActive(this)) ClientTeamApplier.cancel(this);
                        else applyTeam(team);
                    }, applying ? PcStyleButton.Style.DANGER : PcStyleButton.Style.NORMAL);
            equip.active = applying || teamAnalysis.validation.readyToEquip();
            equip.setTooltip(Tooltip.of(Text.literal(analysisSummary(teamAnalysis))));
            equipButton = equip;
            addDrawableChild(equip);

            PcStyleButton edit = new PcStyleButton(left + SCREEN_X + 101, top + MAIN_ACTION_Y,
                    MAIN_ACTION_WIDTH, 16,
                    ui("edit"), button -> startEditing(team));
            edit.active = !applying;
            edit.setTooltip(Tooltip.of(ui("tooltip_edit")));
            addDrawableChild(edit);

            PcStyleButton duplicate = new PcStyleButton(left + SCREEN_X + 196, top + MAIN_ACTION_Y,
                    MAIN_ACTION_WIDTH, 16,
                    ui("duplicate"), button -> duplicateTeam(team));
            duplicate.active = !applying;
            duplicate.setTooltip(Tooltip.of(ui("tooltip_duplicate")));
            addDrawableChild(duplicate);

            PcStyleButton delete = new PcStyleButton(left + PARTY_X + 6, top + 219, 70, 16,
                    confirmDelete ? ui("confirm") : ui("delete_team"),
                    button -> requestDelete(team), PcStyleButton.Style.DANGER).withScrollingText();
            delete.active = !applying;
            delete.setTooltip(Tooltip.of(ui("tooltip_delete")));
            addDrawableChild(delete);
        }

        PcStyleButton saveCurrent = new PcStyleButton(left + PARTY_X + 6, top + 200, 70, 16,
                ui("save_current"), button -> saveCurrentParty()).withScrollingText();
        saveCurrent.active = !applying && !parent.getParty().isEmpty();
        saveCurrent.setTooltip(Tooltip.of(ui("tooltip_save_current")));
        addDrawableChild(saveCurrent);

        if (undoAction != null) {
            PcStyleButton undo = new PcStyleButton(left + PARTY_X + 6, top + 181, 70, 16,
                    ui("undo"), button -> undoLast());
            undo.active = !applying;
            undo.setTooltip(Tooltip.of(Text.literal(undoDescription)));
            addDrawableChild(undo);
        }

    }

    private void initEditor(int left, int top) {
        nameField = new TextFieldWidget(textRenderer, left + SCREEN_X + 6, top + 28,
                SCREEN_WIDTH - 12, 16, ui("name"));
        nameField.setMaxLength(24);
        nameField.setPlaceholder(ui("name"));
        nameField.setText(draftName);
        nameField.setChangedListener(value -> draftName = value);
        addDrawableChild(nameField);

        addDraftModels(left + CARD_START_X, top + CARD_START_Y);

        boolean unresolved = draft.stream().anyMatch(slot -> slot.pokemon == null && slot.speciesId != null);
        int toolStartY = unresolved ? 181 : 189;
        PcStyleButton style = new PcStyleButton(left + LIST_X, top + toolStartY + (unresolved ? 16 : 0),
                LEFT_CONTROL_WIDTH, 14, Text.literal(rankedStyleName()), button -> cycleRankedStyle(),
                PcStyleButton.Style.SELECTED).withScrollingText();
        style.active = !rankedHelperRunning;
        style.setTooltip(Tooltip.of(ui("tooltip_ranked_style")));
        addDrawableChild(style);
        int styleY = toolStartY + (unresolved ? 16 : 0);
        if (rankedBuildStyle == RankedUsageService.BuildStyle.MONOTYPE) {
            PcStyleButton monotype = new PcStyleButton(left + LIST_X, top + styleY + 16,
                    LEFT_CONTROL_WIDTH, 14, ui("ranked_monotype_button", rankedMonotypeTypeName()),
                    button -> cycleRankedMonotypeType(), PcStyleButton.Style.SELECTED).withScrollingText();
            monotype.active = !rankedHelperRunning;
            monotype.setTooltip(Tooltip.of(ui("tooltip_ranked_monotype")));
            addDrawableChild(monotype);
        }
        int doctorY = styleY + (rankedBuildStyle == RankedUsageService.BuildStyle.MONOTYPE ? 32 : 18);
        PcStyleButton doctor = new PcStyleButton(left + LIST_X, top + doctorY,
                LEFT_CONTROL_WIDTH, 14,
                ui("team_doctor"), button -> openTeamDoctor()).withScrollingText();
        doctor.active = !draft.isEmpty();
        doctor.setTooltip(Tooltip.of(ui("tooltip_team_doctor")));
        addDrawableChild(doctor);
        if (unresolved) {
            PcStyleButton associateAll = new PcStyleButton(left + LIST_X, top + toolStartY,
                    LEFT_CONTROL_WIDTH, 14, ui("associate_all"), button -> autoAssociateAll(),
                    PcStyleButton.Style.NORMAL).withScrollingText();
            associateAll.setTooltip(Tooltip.of(ui("tooltip_associate_all")));
            addDrawableChild(associateAll);
        }

        if (selectedDraftSlot >= 0 && selectedDraftSlot < draft.size()) {
            DraftSlot selected = draft.get(selectedDraftSlot);
            boolean capturedMatch = selected.pokemon == null && !compatibleOwnedChoices(selected).isEmpty();
            addDrawableChild(new PcStyleButton(left + PARTY_X + 6, top + 43, 70, 16,
                    ui(capturedMatch ? "associate" : "replace"),
                    button -> replaceOrAssociate(selectedDraftSlot)).withScrollingText());
            addDrawableChild(new PcStyleButton(left + PARTY_X + 6, top + 62, 70, 16,
                    ui("remove"), button -> removeSelectedDraft(), PcStyleButton.Style.DANGER));
            PcStyleButton setEditor = new PcStyleButton(left + PARTY_X + 6, top + 81, 70, 16,
                    ui("set_editor"), button -> openSetEditor(selectedDraftSlot));
            setEditor.active = selected.pokemon != null || findSpecies(selected.speciesId) != null;
            setEditor.setTooltip(Tooltip.of(ui("tooltip_set_editor")));
            addDrawableChild(setEditor);
            if (rankedTeamGenerated && !rankedLockedSlots.contains(selectedDraftSlot)) {
                PcStyleButton regenerateSlot = new PcStyleButton(left + PARTY_X + 6, top + 100, 70, 16,
                        ui("ranked_regenerate_slot"), button -> regenerateRankedSlot(selectedDraftSlot))
                        .withScrollingText();
                regenerateSlot.active = !rankedHelperRunning;
                regenerateSlot.setTooltip(Tooltip.of(ui("tooltip_ranked_slot_regenerate")));
                addDrawableChild(regenerateSlot);
            }
        }

        PcStyleButton generationMode = new PcStyleButton(left + PARTY_X + 6, top + 146, 70, 16,
                ui("ranked_mode_" + rankedGenerationMode.name().toLowerCase(Locale.ROOT)),
                button -> cycleRankedGenerationMode(), rankedGenerationMode == RankedUsageService.GenerationMode.META
                ? PcStyleButton.Style.NORMAL : PcStyleButton.Style.SELECTED).withScrollingText();
        generationMode.active = !rankedHelperRunning;
        generationMode.setTooltip(Tooltip.of(ui("tooltip_ranked_mode")));
        addDrawableChild(generationMode);

        PcStyleButton save = new PcStyleButton(left + PARTY_X + 6, top + 200, 70, 16,
                editingTeamId == null ? ui("save") : ui("update"), button -> saveDraft());
        TeamValidationService.Report draftValidation = cachedDraftValidation();
        save.active = !draft.isEmpty() && draftValidation.readyToSave();
        save.setTooltip(Tooltip.of(Text.literal(validationSummary(draftValidation))));
        saveButton = save;
        addDrawableChild(save);
        addDrawableChild(new PcStyleButton(left + PARTY_X + 6, top + 219, 70, 16,
                ui("cancel"), button -> cancelEditor()));
        PcStyleButton helper = new PcStyleButton(left + PARTY_X + 6, top + 181, 70, 16,
                ui(rankedHelperLabel()),
                button -> {
                    if (canRegenerateRankedTeam()) regenerateRankedTeam();
                    else startRankedHelper();
                }).withScrollingText();
        helper.active = !rankedHelperRunning;
        helper.setTooltip(Tooltip.of(ui(rankedHelperTooltip())));
        addDrawableChild(helper);

    }

    private String rankedHelperLabel() {
        if (canRegenerateRankedTeam()) return "ranked_regenerate";
        if (draft.size() >= TeamModels.MAX_TEAM_SIZE) return "ranked_sets";
        if (draft.isEmpty()) return "ranked_generate";
        if (draft.size() <= 2) return "ranked_around_core";
        return "ranked_complete";
    }

    private String rankedHelperTooltip() {
        if (canRegenerateRankedTeam()) return "tooltip_ranked_regenerate";
        if (draft.isEmpty()) return "tooltip_ranked_generate";
        if (draft.size() <= 2) return "tooltip_ranked_core";
        return "tooltip_ranked_helper";
    }

    private void selectTeam(int index) {
        if (ClientTeamApplier.isActive(this)) return;
        cancelSlotGestures();
        selectedTeam = index;
        selectedPreviewSlot = -1;
        page = selectedTeam / PAGE_SIZE;
        confirmDelete = false;
        status = "";
        clearAndInit();
    }

    private void toggleTeamReordering() {
        if (ClientTeamApplier.isActive(this)) return;
        teamDrag.clear();
        teamDragEdgeTicks = 0;
        reorderingTeams = !reorderingTeams;
        confirmDelete = false;
        status = reorderingTeams ? uiText("tooltip_reorder_teams") : "";
        statusError = false;
        clearAndInit();
    }

    private void moveTeamTo(int from, int to) {
        if (!reorderingTeams || creating || ClientTeamApplier.isActive(this)) return;
        if (!data.moveTeam(from, to)) return;
        if (!repository.save(client, data)) {
            data.moveTeam(to, from);
            status = uiText("error_save");
            statusError = true;
            clearAndInit();
            return;
        }
        selectedTeam = to;
        page = to / PAGE_SIZE;
        confirmDelete = false;
        status = uiText("status_team_moved", selectedTeam().name, to + 1, data.teams.size());
        statusError = false;
        clearAndInit();
    }

    private int teamListIndexAt(double mouseX, double mouseY) {
        if (!isInside(mouseX, mouseY, left() + LIST_X, top() + LIST_START_Y,
                LEFT_CONTROL_WIDTH, PAGE_SIZE * LIST_ROW_HEIGHT)) return -1;
        double relativeY = mouseY - top() - LIST_START_Y;
        int row = (int) (relativeY / LIST_ROW_HEIGHT);
        int index = page * PAGE_SIZE + row;
        return relativeY % LIST_ROW_HEIGHT < 16 && index < data.teams.size() ? index : -1;
    }

    private int teamDropBoundary(double mouseX, double mouseY) {
        if (!isInside(mouseX, mouseY, left() + LIST_X, top() + LIST_START_Y - 4,
                LEFT_CONTROL_WIDTH, PAGE_SIZE * LIST_ROW_HEIGHT + 8)) return -1;
        int start = page * PAGE_SIZE;
        int rows = Math.min(PAGE_SIZE, data.teams.size() - start);
        return start + TeamListDrag.insertionRow(mouseY - top() - LIST_START_Y, LIST_ROW_HEIGHT, rows);
    }

    private void cancelTeamDrag() {
        teamDrag.clear();
        teamDragEdgeTicks = 0;
        page = selectedTeam / PAGE_SIZE;
        clearAndInit();
    }

    private void changeTeamDragPage(int direction) {
        int next = Math.max(0, Math.min(teamPages() - 1, page + direction));
        if (next == page) return;
        page = next; // Keep the grabbed team selected, even on another page.
        clearAndInit();
    }

    private void tickTeamDrag() {
        if (teamDrag.team == null) return;
        if (!client.isWindowFocused() || creating || !reorderingTeams || ClientTeamApplier.isActive(this)) {
            cancelTeamDrag();
            return;
        }
        int edge = 0;
        if (teamDrag.dragging && teamDrag.x >= left() + LIST_X
                && teamDrag.x < left() + LIST_X + LEFT_CONTROL_WIDTH) {
            double y = teamDrag.y - top();
            if (y >= LIST_START_Y - 8 && y < LIST_START_Y + 4) edge = -1;
            if (y >= LIST_START_Y + PAGE_SIZE * LIST_ROW_HEIGHT - 4 && y < 235) edge = 1;
        }
        if (edge != teamDragEdgeDirection || edge == 0) teamDragEdgeTicks = 0;
        teamDragEdgeDirection = edge;
        if (edge != 0 && ++teamDragEdgeTicks >= 12) {
            teamDragEdgeTicks = 0;
            changeTeamDragPage(edge);
        }
    }

    private void changePage(int direction) {
        if (ClientTeamApplier.isActive(this)) return;
        int nextPage = Math.max(0, Math.min(teamPages() - 1, page + direction));
        if (nextPage == page) return;
        page = nextPage;
        selectedTeam = Math.min(data.teams.size() - 1, page * PAGE_SIZE);
        selectedPreviewSlot = -1;
        confirmDelete = false;
        clearAndInit();
    }

    private int teamPages() {
        return Math.max(1, (data.teams.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private void startCreating() {
        if (ClientTeamApplier.isActive(this)) return;
        cancelSlotGestures();
        reorderingTeams = false;
        creating = true;
        selectedPreviewSlot = -1;
        editingTeamId = null;
        confirmDelete = false;
        selectedDraftSlot = -1;
        draft.clear();
        rankedGenerationCore.clear();
        rankedRegenerationSnapshot.clear();
        rankedLockedSlots.clear();
        rankedReasons.clear();
        rankedProposal = null;
        rankedTeamGenerated = false;
        draftName = "";
        status = "";
        clearAndInit();
    }

    private void startEditing(SavedTeam team) {
        if (ClientTeamApplier.isActive(this)) return;
        cancelSlotGestures();
        reorderingTeams = false;
        creating = true;
        selectedPreviewSlot = -1;
        editingTeamId = team.id;
        selectedDraftSlot = -1;
        draft.clear();
        rankedGenerationCore.clear();
        rankedRegenerationSnapshot.clear();
        rankedLockedSlots.clear();
        rankedReasons.clear();
        rankedProposal = null;
        rankedTeamGenerated = false;
        draftName = team.name;
        for (SavedSlot slot : team.slots) {
            Pokemon pokemon = findPokemon(slot.pokemonId);
            String speciesId = slot.speciesId == null && pokemon != null ? speciesId(pokemon) : slot.speciesId;
            String formId = slot.formId == null && pokemon != null ? formId(pokemon) : slot.formId;
            TeamPresetPolicy.SetValues preset = TeamPresetPolicy.saved(slot);
            draft.add(new DraftSlot(pokemon, slot.pokemonId, speciesId, formId, normalizedItemId(slot.itemId),
                    preset.abilityId(), preset.moveIds(), preset.natureId(), preset.evs(),
                    pokemon == null ? uiText("source_catalogue") : currentSource(pokemon)));
        }
        long capturedMatches = draft.stream()
                .filter(slot -> slot.pokemon == null && !compatibleOwnedChoices(slot).isEmpty()).count();
        status = capturedMatches > 0 ? uiText("status_captured_matches", capturedMatches) : uiText("status_edit");
        statusError = false;
        clearAndInit();
    }

    void importPaste(ShowdownPaste.Parsed paste) {
        if (ClientTeamApplier.isActive(this)) return;
        // Resolve the WHOLE paste before touching the existing editor or repository.
        Map<String, CobblemonCatalogueCache.Entry> forms = new LinkedHashMap<>();
        for (CobblemonCatalogueCache.Entry entry : catalogue.snapshot().entries()) {
            forms.putIfAbsent(rankedKey(entry.species(), entry.form()), entry);
        }
        List<DraftSlot> imported = new ArrayList<>();
        int illegalMoves = 0;
        for (ShowdownPaste.Member member : paste.members()) {
            CobblemonCatalogueCache.Entry entry = forms.get(ShowdownPaste.key(member.species()));
            if (entry == null) throw new IllegalArgumentException("unknown_species");
            String item = member.item().isBlank() ? "minecraft:air" : rankedItemId(member.item());
            if (item == null) throw new IllegalArgumentException("unknown_item");
            String ability = member.ability().isBlank() ? null : rankedAbilityId(entry.form(), member.ability());
            if (!member.ability().isBlank() && ability == null) throw new IllegalArgumentException("unknown_ability");
            String nature = member.nature().isBlank() ? null : member.nature();
            if (nature != null && !NATURE_IDS.contains(nature)) throw new IllegalArgumentException("unknown_nature");
            for (String id : member.moves()) {
                if (com.cobblemon.mod.common.api.moves.Moves.getByName(id) == null) {
                    throw new IllegalArgumentException("unknown_move");
                }
                if (!entry.legalMoveIds().contains(id)) illegalMoves++;
            }
            imported.add(new DraftSlot(null, null, entry.species().getResourceIdentifier().toString(),
                    formId(entry.form(), entry.species()), item, ability, member.moves(), nature,
                    member.evs(), uiText("source_catalogue")));
        }
        cancelRankedHelperRequest();
        startCreating();
        draft.addAll(imported);
        draftName = TeamModels.cleanName(paste.name().isBlank() ? uiText("paste_default_name") : paste.name());
        status = uiText("paste_imported", imported.size());
        if (!paste.ignoredFields().isEmpty()) status += " · " + uiText("paste_ignored", String.join(", ", paste.ignoredFields()));
        if (illegalMoves > 0) status += " · " + uiText("validation_illegal_moves", illegalMoves);
        statusError = illegalMoves > 0 || !paste.ignoredFields().isEmpty();
        clearAndInit();
    }

    private void cancelEditor() {
        cancelRankedHelperRequest();
        creating = false;
        editingTeamId = null;
        selectedDraftSlot = -1;
        draggedDraftSlot = -1;
        pickerReplaceIndex = -1;
        draft.clear();
        rankedGenerationCore.clear();
        rankedRegenerationSnapshot.clear();
        rankedLockedSlots.clear();
        rankedReasons.clear();
        rankedProposal = null;
        rankedTeamGenerated = false;
        draftName = "";
        status = uiText("status_edit_cancelled");
        statusError = false;
        clearAndInit();
    }

    private void removeSelectedDraft() {
        if (selectedDraftSlot < 0 || selectedDraftSlot >= draft.size()) return;
        int removedIndex = selectedDraftSlot;
        DraftSlot removed = draft.remove(removedIndex);
        shiftRankedLocksAfterRemoval(removedIndex);
        selectedDraftSlot = Math.min(selectedDraftSlot, draft.size() - 1);
        status = uiText("status_removed", displayName(removed));
        statusError = false;
        clearAndInit();
    }

    private void openPicker(int replaceIndex) {
        openPicker(replaceIndex, false);
    }

    private void openPicker(int replaceIndex, boolean associationOnly) {
        if (client == null || replaceIndex < -1 || replaceIndex >= draft.size()) return;
        if (replaceIndex == -1 && draft.size() >= TeamModels.MAX_TEAM_SIZE) return;
        pickerReplaceIndex = replaceIndex;
        pokemonPickerAssociationOnly = associationOnly;
        pokemonPickerAllSpecies = associationOnly ? false : rememberedPokemonPickerAllSpecies;
        ownedPokemon.refresh(parent);

        pokemonPickerExcluded.clear();
        for (int i = 0; i < draft.size(); i++) {
            UUID id = parseUuid(draft.get(i).pokemonId);
            if (id != null) pokemonPickerExcluded.add(id);
        }
        pokemonPickerPage = 0;
        pendingAbilityPokemon = null;
        pokemonPickerSortStat = null;
        pokemonPickerSortDescending = true;
        rankedSeasonMenuOpen = false;
        pokemonPickerOpen = true;
        scanPokemonChoices();
        if (rankedUsage.isEmpty() && !rankedUsageLoading) loadRankedUsage();
        clearAndInit();
    }

    private void replaceOrAssociate(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= draft.size()) return;
        DraftSlot slot = draft.get(slotIndex);
        List<PokemonChoice> compatible = compatibleOwnedChoices(slot);
        if (slot.pokemon == null && compatible.size() == 1) {
            pickerReplaceIndex = slotIndex;
            selectPokemon(compatible.get(0));
            return;
        }
        openPicker(slotIndex, slot.pokemon == null && !compatible.isEmpty());
        if (slot.pokemon == null && !compatible.isEmpty()) {
            Species species = findSpecies(slot.speciesId);
            pokemonPickerQuery = species == null ? "" : speciesName(species);
            filterPokemonChoices();
            clearAndInit();
        }
    }

    private void quickReplaceFromBrowser(int slotIndex) {
        if (data.teams.isEmpty() || slotIndex < 0 || slotIndex >= selectedTeam().slots.size()) return;
        SavedTeam team = selectedTeam();
        startEditing(team);
        selectedDraftSlot = slotIndex;
        replaceOrAssociate(slotIndex);
    }

    private void swapSavedSlots(int source, int target) {
        if (data.teams.isEmpty() || source == target) return;
        SavedTeam team = selectedTeam();
        if (source < 0 || target < 0 || source >= team.slots.size() || target >= team.slots.size()) return;
        SavedTeam before = copyTeam(team);
        if (!team.swapSlots(source, target)) return;
        if (!repository.save(client, data)) {
            data.teams.set(selectedTeam, before);
            status = uiText("error_save");
            statusError = true;
            clearAndInit();
            return;
        }
        String teamId = team.id;
        setUndo(uiText("undo_slot_order", team.name), () -> restoreTeam(teamId, before));
        selectedPreviewSlot = target;
        status = uiText("status_order", source + 1, target + 1);
        statusError = false;
        clearAndInit();
    }

    private void cancelSlotGestures() {
        draggedDraftSlot = -1;
        draftDragging = false;
        draggedPreviewSlot = -1;
        previewDragging = false;
        lastPreviewClickSlot = -1;
    }

    private void autoAssociateAll() {
        ownedPokemon.refresh(parent);
        int associated = 0;
        int manual = 0;
        int warnings = 0;
        for (int index = 0; index < draft.size(); index++) {
            DraftSlot slot = draft.get(index);
            if (slot.pokemon != null || slot.speciesId == null) continue;
            List<PokemonChoice> compatible = compatibleOwnedChoices(slot);
            if (compatible.isEmpty()) {
                manual++;
                continue;
            }
            AssociationOutcome outcome = associateCapturedPokemon(index, compatible.get(0));
            associated++;
            warnings += outcome.warningCount();
            notifyAssociationWarnings(outcome);
        }
        status = uiText("status_associate_all", associated, manual, warnings);
        statusError = warnings > 0 || manual > 0;
        clearAndInit();
    }

    private List<PokemonChoice> compatibleOwnedChoices(DraftSlot target) {
        if (target == null || target.pokemon != null) return List.of();
        String targetKey = rankedKey(target);
        if (targetKey.isBlank()) return List.of();
        Set<UUID> used = new HashSet<>();
        for (DraftSlot slot : draft) {
            UUID id = parseUuid(slot.pokemonId);
            if (id != null) used.add(id);
        }
        List<PokemonChoice> exact = new ArrayList<>();
        List<PokemonChoice> speciesFallback = new ArrayList<>();
        for (OwnedPokemonIndex.Entry entry : ownedPokemon.bySpecies(target.speciesId)) {
            if (!used.contains(entry.pokemon().getUuid())) {
                addCompatibleOwnedChoice(target, targetKey, ownedChoice(entry), exact, speciesFallback);
            }
        }
        List<PokemonChoice> result = exact.isEmpty() ? speciesFallback : exact;
        result.sort(Comparator
                .comparing((PokemonChoice choice) -> associationNatureMatches(target, choice)).reversed()
                .thenComparing(Comparator.comparingInt(this::pokemonIvScore).reversed())
                .thenComparing(Comparator.comparingInt(
                        (PokemonChoice choice) -> learnedPlannedMoveCount(target, choice)).reversed())
                .thenComparing(Comparator.comparingInt(this::pokemonChoiceLevel).reversed()));
        return List.copyOf(result);
    }

    private void addCompatibleOwnedChoice(DraftSlot target, String targetKey, PokemonChoice choice,
                                          List<PokemonChoice> exact, List<PokemonChoice> speciesFallback) {
        if (!plannedAbilityMatches(target, choice.pokemon)) return;
        if (rankedKey(choice).equals(targetKey)) {
            exact.add(choice);
            return;
        }
        Identifier targetSpecies = Identifier.tryParse(target.speciesId);
        if (targetSpecies != null && targetSpecies.equals(choice.species.getResourceIdentifier())) {
            speciesFallback.add(choice);
        }
    }

    private boolean associationNatureMatches(DraftSlot target, PokemonChoice choice) {
        if (target == null || target.natureId == null || target.natureId.isBlank()) return true;
        if (choice == null || choice.pokemon == null) return false;
        return RankedUsageService.key(target.natureId).equals(
                RankedUsageService.key(choice.pokemon.getEffectiveNature().getName().getPath()));
    }

    private int pokemonIvScore(PokemonChoice choice) {
        if (choice == null || choice.pokemon == null) return -1;
        int total = 0;
        for (Stat stat : DISPLAY_STATS) total += choice.pokemon.getIvs().getOrDefault(stat);
        return total;
    }

    private int learnedPlannedMoveCount(DraftSlot target, PokemonChoice choice) {
        if (target == null || choice == null || choice.pokemon == null) return 0;
        Set<String> learned = new HashSet<>(activeMoveIds(choice.pokemon));
        for (BenchedMove move : choice.pokemon.getBenchedMoves()) learned.add(move.getMoveTemplate().getName());
        int count = 0;
        for (String move : target.moveIds) if (learned.contains(move)) count++;
        return count;
    }

    private void initPokemonPicker() {
        int left = pickerLeft();
        int top = pickerTop();
        if (pendingAbilityPokemon != null) return;
        addDrawableChild(new PcStyleButton(left + 8, top + 27, 57, 17, ui("pokemon_picker_owned"),
                button -> switchPokemonPickerMode(false), pokemonPickerAllSpecies
                ? PcStyleButton.Style.NORMAL : PcStyleButton.Style.SELECTED));
        PcStyleButton all = new PcStyleButton(left + 67, top + 27, 45, 17, ui("pokemon_picker_all"),
                button -> switchPokemonPickerMode(true), pokemonPickerAllSpecies
                ? PcStyleButton.Style.SELECTED : PcStyleButton.Style.NORMAL);
        all.active = !pokemonPickerAssociationOnly;
        if (pokemonPickerAssociationOnly) all.setTooltip(Tooltip.of(ui("tooltip_association_owned_only")));
        addDrawableChild(all);
        String seasonLabel = rankedSeason.isBlank() ? uiText("pokemon_picker_season") : rankedSeason;
        PcStyleButton season = new PcStyleButton(left + 115, top + 27, 86, 17,
                Text.literal(fitText(seasonLabel, 72)), button -> rankedSeasonMenuOpen = !rankedSeasonMenuOpen,
                rankedSeasonMenuOpen ? PcStyleButton.Style.SELECTED : PcStyleButton.Style.NORMAL);
        season.setTooltip(Tooltip.of(ui("pokemon_picker_season_tooltip", seasonLabel)));
        addDrawableChild(season);
        int searchX = left + 203;
        int searchWidth = pickerWidth() - 212;
        pokemonSearchField = new TextFieldWidget(textRenderer, searchX, top + 28, searchWidth, 16,
                ui("pokemon_picker_search"));
        pokemonSearchField.setMaxLength(40);
        pokemonSearchField.setPlaceholder(ui("pokemon_picker_search"));
        pokemonSearchField.setText(pokemonPickerQuery);
        pokemonSearchField.setChangedListener(value -> {
            pokemonPickerQuery = value;
            pokemonPickerPage = 0;
            filterPokemonChoices();
            refreshPokemonPickerModels();
        });
        addDrawableChild(pokemonSearchField);
        setInitialFocus(pokemonSearchField);

        pokemonPickerModels.clear();
        pickerModelIdentities.clear();
        if (catalogue.snapshot().entries().isEmpty()) return;
        int rows = pokemonPickerRows();
        for (int row = 0; row < rows; row++) {
            ModelWidget model = createModelWidget(left + 4,
                    top + 57 + row * POKEMON_PICKER_ROW_HEIGHT,
                    34, 21, placeholderRenderable(), 0.62F, 35.0F, 1.0, false, false);
            pokemonPickerModels.add(model);
            addDrawableChild(model);
        }
        refreshPokemonPickerModels();
    }

    private void scanPokemonChoices() {
        pickerSearchText.clear();
        pickerNames.clear();
        pickerDetails.clear();
        pickerAbilities.clear();
        pokemonChoices.clear();
        if (pokemonPickerAllSpecies) {
            for (CobblemonCatalogueCache.Entry entry : catalogue.snapshot().entries()) addCatalogueChoice(entry);
        } else {
            for (OwnedPokemonIndex.Entry entry : ownedPokemon.all()) {
                if (!pokemonPickerExcluded.contains(entry.pokemon().getUuid())) pokemonChoices.add(ownedChoice(entry));
            }
        }
        filterPokemonChoices();
    }

    private PokemonChoice ownedChoice(OwnedPokemonIndex.Entry entry) {
        String source = entry.party() ? uiText("source_party") : uiText("source_box", entry.box() + 1);
        return ownedChoice(entry.pokemon(), entry.position(), source);
    }

    private PokemonChoice ownedChoice(Pokemon pokemon, StorePosition position, String source) {
        return new PokemonChoice(pokemon, pokemon.getSpecies(), pokemon.getForm(), position, source, true,
                List.of(new AbilityChoice(abilityId(pokemon), isHiddenAbility(pokemon))));
    }

    private void addCatalogueChoice(CobblemonCatalogueCache.Entry entry) {
        List<AbilityChoice> abilities = entry.abilities().stream()
                .map(ability -> new AbilityChoice(ability.id(), ability.hidden())).toList();
        pokemonChoices.add(new PokemonChoice(null, entry.species(), entry.form(), null,
                uiText("source_catalogue"), false, abilities));
    }

    private void switchPokemonPickerMode(boolean allSpecies) {
        if (pokemonPickerAssociationOnly && allSpecies) return;
        if (pokemonPickerAllSpecies == allSpecies) return;
        pokemonPickerAllSpecies = allSpecies;
        rememberedPokemonPickerAllSpecies = allSpecies;
        pendingAbilityPokemon = null;
        rankedSeasonMenuOpen = false;
        pokemonPickerPage = 0;
        scanPokemonChoices();
        if (allSpecies) loadRankedUsage();
        clearAndInit();
    }

    private void loadRankedUsage() {
        loadRankedUsage(rankedSeason);
    }

    private void loadRankedUsage(String requestedSeason) {
        int request = ++rankedUsageRequest;
        if (rankedUsageFuture != null) rankedUsageFuture.cancel(true);
        rankedUsageLoading = true;
        RankedUsageService.UsageIndex cached = RankedUsageService.INSTANCE.cachedUsageIndex(requestedSeason);
        if (cached != null) applyRankedUsageIndex(cached, false);
        else rankedUsage.clear();
        filterPokemonChoices();
        CompletableFuture<RankedUsageService.UsageIndex> loading = cached == null
                ? RankedUsageService.INSTANCE.loadUsageIndex(requestedSeason)
                : RankedUsageService.INSTANCE.refreshUsageIndex(requestedSeason);
        rankedUsageFuture = loading;
        loading.whenComplete((index, error) -> {
            if (client == null) return;
            client.execute(() -> {
                if (request != rankedUsageRequest) return;
                rankedUsageLoading = false;
                if (error != null || index == null) {
                    TropimonTeamSaverClient.LOGGER.warn("Impossible de charger les usages Ranked Tropimon", error);
                    return;
                }
                applyRankedUsageIndex(index, true);
            });
        });
    }

    private void applyRankedUsageIndex(RankedUsageService.UsageIndex index, boolean rebuild) {
        if (!rankedSeason.equals(index.season())) rankedMoveUsage.clear();
        rankedSeason = index.season();
        rankedSeasons.clear();
        for (RankedUsageService.SeasonInfo season : index.seasons()) rankedSeasons.add(season.name());
        rankedUsage.clear();
        for (RankedUsageService.UsageEntry entry : index.entries()) {
            rankedUsage.putIfAbsent(RankedUsageService.key(entry.name), entry);
        }
        if (pokemonPickerOpen) {
            filterPokemonChoices();
            if (rebuild) clearAndInit();
            else refreshPokemonPickerModels();
        }
    }

    private void selectRankedSeason(String season) {
        rankedSeasonMenuOpen = false;
        if (season == null || season.isBlank() || season.equals(rankedSeason)) return;
        rankedSeason = season;
        rankedMoveUsage.clear();
        cancelRankedHelperRequest();
        pokemonPickerPage = 0;
        loadRankedUsage(season);
        clearAndInit();
    }

    private void cancelRankedHelperRequest() {
        rankedHelperRequest++;
        rankedHelperRunning = false;
        if (rankedHelperFuture != null) rankedHelperFuture.cancel(true);
        rankedHelperFuture = null;
    }

    private void startRankedHelper() {
        if (rankedHelperRunning || client == null) return;
        rankedGenerationCore.clear();
        rankedGenerationCore.addAll(draft);
        rankedLockedSlots.clear();
        for (int index = 0; index < draft.size(); index++) rankedLockedSlots.add(index);
        rankedTeamGenerated = false;
        beginRankedRecommendation(rankedGenerationCore, false);
    }

    private boolean canRegenerateRankedTeam() {
        return rankedTeamGenerated && draft.size() == TeamModels.MAX_TEAM_SIZE
                && rankedLockedSlots.size() < draft.size();
    }

    private void regenerateRankedTeam() {
        if (!canRegenerateRankedTeam() || rankedHelperRunning || client == null) return;
        rankedRegenerationSnapshot.clear();
        rankedRegenerationSnapshot.addAll(draft);
        rankedGenerationCore.clear();
        for (int index = 0; index < draft.size(); index++) {
            if (rankedLockedSlots.contains(index)) rankedGenerationCore.add(draft.get(index));
        }
        beginRankedRecommendation(rankedGenerationCore, true);
    }

    private void beginRankedRecommendation(List<DraftSlot> core, boolean replaceDraftOnSuccess) {
        Map<String, RankedLocalTemplate> templates = rankedTemplates();
        Map<String, RankedUsageService.CandidateProfile> profiles = rankedProfiles(templates);
        if (rankedBuildStyle == RankedUsageService.BuildStyle.MONOTYPE) {
            for (DraftSlot slot : core) {
                if (!draftHasType(slot, rankedMonotypeType)) {
                    status = uiText("error_ranked_monotype_core", rankedMonotypeTypeName());
                    statusError = true;
                    clearAndInit();
                    return;
                }
            }
            profiles = RankedUsageService.profilesForType(profiles, rankedMonotypeType);
            if (profiles.size() < TeamModels.MAX_TEAM_SIZE) {
                status = uiText("error_ranked_monotype_pool", rankedMonotypeTypeName());
                statusError = true;
                clearAndInit();
                return;
            }
        }
        List<String> existing = core.stream().map(this::rankedKey).filter(key -> !key.isBlank()).toList();
        Set<String> excludedSpecies = replaceDraftOnSuccess
                ? rankedSpeciesIds(rankedRegenerationSnapshot) : rankedSpeciesIds(core);
        profiles = excludeRankedSpeciesVariants(profiles, templates, excludedSpecies,
                new LinkedHashSet<>(existing));
        long variation = RANKED_VARIATION.getAndIncrement() + (long) data.teams.size() * 17L;
        if (replaceDraftOnSuccess) {
            List<String> previousRoster = rankedRegenerationSnapshot.stream().map(this::rankedKey).toList();
            String forcedReplacement = RankedVariantPlanner.forcedReplacement(
                    previousRoster, rankedLockedSlots, variation);
            if (!forcedReplacement.isBlank() && profiles.containsKey(forcedReplacement)) {
                profiles = new LinkedHashMap<>(profiles);
                profiles.remove(forcedReplacement);
            }
        }
        rankedHelperRunning = true;
        rankedProposal = null;
        rankedProposalReplacesDraft = replaceDraftOnSuccess;
        int request = ++rankedHelperRequest;
        if (rankedHelperFuture != null) rankedHelperFuture.cancel(true);
        status = uiText(replaceDraftOnSuccess ? "status_ranked_regenerating" : "status_ranked_loading");
        statusError = false;
        clearAndInit();
        requestRankedRecommendation(existing, templates, profiles, variation, replaceDraftOnSuccess, request);
    }

    private void requestRankedRecommendation(List<String> existing,
                                             Map<String, RankedLocalTemplate> templates,
                                             Map<String, RankedUsageService.CandidateProfile> profiles,
                                             long variation, boolean replaceDraftOnSuccess,
                                             int request) {
        CompletableFuture<RankedUsageService.Recommendation> loading =
                RankedUsageService.INSTANCE.recommend(existing, profiles,
                        TeamModels.MAX_TEAM_SIZE, rankedSeason, variation, rankedBuildStyle,
                        rankedGenerationMode);
        rankedHelperFuture = loading;
        loading.whenComplete((recommendation, error) -> {
                    if (client == null) return;
                    client.execute(() -> {
                        if (request != rankedHelperRequest) return;
                        if (error != null || recommendation == null) {
                            rankedHelperRunning = false;
                            TropimonTeamSaverClient.LOGGER.warn("Assistant Ranked Tropimon indisponible", error);
                            status = uiText("error_ranked_unavailable");
                            statusError = true;
                            clearAndInit();
                            return;
                        }
                        rankedHelperRunning = false;
                        rankedProposal = recommendation;
                        rankedProposalReplacesDraft = replaceDraftOnSuccess;
                        showRankedProposal(templates);
                    });
                });
    }

    private void showRankedProposal(Map<String, RankedLocalTemplate> templates) {
        RankedUsageService.Recommendation recommendation = rankedProposal;
        if (recommendation == null) return;
        if (rankedProposalReplacesDraft) {
            applyRegeneratedRecommendation(recommendation, templates);
        } else {
            draft.clear();
            draft.addAll(rankedGenerationCore);
            applyRankedRecommendation(recommendation, templates);
        }
    }

    private Set<String> rankedDraftRoster() {
        Set<String> result = new LinkedHashSet<>();
        for (DraftSlot slot : draft) {
            String key = rankedKey(slot);
            if (!key.isBlank()) result.add(key);
        }
        return Set.copyOf(result);
    }

    private Set<String> rankedSpeciesIds(Iterable<DraftSlot> slots) {
        Set<String> result = new LinkedHashSet<>();
        if (slots == null) return result;
        for (DraftSlot slot : slots) {
            String speciesId = rankedSpeciesId(slot);
            if (!speciesId.isBlank()) result.add(speciesId);
        }
        return result;
    }

    private String rankedSpeciesId(DraftSlot slot) {
        if (slot == null) return "";
        Species species = slot.pokemon == null ? findSpecies(slot.speciesId) : slot.pokemon.getSpecies();
        return species == null ? "" : species.getResourceIdentifier().toString();
    }

    private String rankedSpeciesId(RankedLocalTemplate template) {
        return template == null || template.species == null
                ? "" : template.species.getResourceIdentifier().toString();
    }

    private Map<String, RankedUsageService.CandidateProfile> excludeRankedSpeciesVariants(
            Map<String, RankedUsageService.CandidateProfile> profiles,
            Map<String, RankedLocalTemplate> templates, Set<String> excludedSpecies,
            Set<String> preservedKeys) {
        if (excludedSpecies.isEmpty()) return profiles;
        Map<String, RankedUsageService.CandidateProfile> filtered = new LinkedHashMap<>();
        profiles.forEach((key, profile) -> {
            String speciesId = rankedSpeciesId(templates.get(key));
            if (!excludedSpecies.contains(speciesId) || preservedKeys.contains(key)) filtered.put(key, profile);
        });
        return filtered;
    }

    private void applyRankedRecommendation(RankedUsageService.Recommendation recommendation,
                                           Map<String, RankedLocalTemplate> templates) {
        int updated = 0;
        Set<String> selected = new LinkedHashSet<>();
        Set<String> selectedSpecies = new LinkedHashSet<>();
        for (int index = 0; index < draft.size(); index++) {
            DraftSlot previous = draft.get(index);
            String key = rankedKey(previous);
            selected.add(key);
            selectedSpecies.add(rankedSpeciesId(previous));
            RankedUsageService.SpeciesStats stats = recommendation.details().get(key);
            if (stats != null) {
                draft.set(index, rankedSet(previous, stats, recommendation.season()));
                updated++;
            }
        }
        int added = 0;
        for (RankedUsageService.SpeciesStats stats : recommendation.additions()) {
            if (draft.size() >= TeamModels.MAX_TEAM_SIZE) break;
            String key = RankedUsageService.key(stats.name);
            RankedLocalTemplate template = templates.get(key);
            if (template == null || !selected.add(key) || !selectedSpecies.add(rankedSpeciesId(template))) continue;
            DraftSlot empty = new DraftSlot(null, null, template.species.getResourceIdentifier().toString(),
                    formId(template.form, template.species), "minecraft:air", null, List.of(),
                    null, Map.of(), uiText("source_ranked", recommendation.season()));
            draft.add(rankedSet(empty, stats, recommendation.season()));
            added++;
        }
        enforceGeneratedStyleSets();
        if (draftName.isBlank()) draftName = TeamModels.cleanName("Ranked " + recommendation.season());
        selectedDraftSlot = draft.isEmpty() ? -1 : draft.size() - 1;
        rankedSeason = recommendation.season();
        Set<String> generatedRoster = rankedDraftRoster();
        rankedReasons.clear();
        rememberRankedReasons(recommendation);
        rankedTeamGenerated = added > 0 && generatedRoster.size() == TeamModels.MAX_TEAM_SIZE;
        status = uiText("status_ranked_ready", added, updated, recommendation.season());
        statusError = false;
        clearAndInit();
    }

    private void applyRegeneratedRecommendation(RankedUsageService.Recommendation recommendation,
                                                Map<String, RankedLocalTemplate> templates) {
        List<DraftSlot> rebuilt = new ArrayList<>();
        Set<String> selectedSpecies = new LinkedHashSet<>();
        for (int locked : rankedLockedSlots) {
            if (locked >= 0 && locked < rankedRegenerationSnapshot.size()) {
                selectedSpecies.add(rankedSpeciesId(rankedRegenerationSnapshot.get(locked)));
            }
        }
        int additionIndex = 0;
        boolean incomplete = false;
        for (int index = 0; index < TeamModels.MAX_TEAM_SIZE; index++) {
            if (rankedLockedSlots.contains(index) && index < rankedRegenerationSnapshot.size()) {
                DraftSlot locked = rankedRegenerationSnapshot.get(index);
                RankedUsageService.SpeciesStats stats = recommendation.details().get(rankedKey(locked));
                rebuilt.add(stats == null ? locked : rankedSet(locked, stats, recommendation.season()));
                continue;
            }
            DraftSlot generated = null;
            while (additionIndex < recommendation.additions().size() && generated == null) {
                RankedUsageService.SpeciesStats stats = recommendation.additions().get(additionIndex++);
                DraftSlot candidate = newRankedSlot(stats, templates, recommendation.season());
                if (candidate != null && selectedSpecies.add(rankedSpeciesId(candidate))) generated = candidate;
            }
            if (generated != null) rebuilt.add(generated);
            else {
                incomplete = true;
                break;
            }
        }
        if (incomplete || rebuilt.size() != TeamModels.MAX_TEAM_SIZE) {
            draft.clear();
            draft.addAll(rankedRegenerationSnapshot);
            rankedHelperRunning = false;
            status = uiText("error_ranked_unavailable");
            statusError = true;
            clearAndInit();
            return;
        }
        draft.clear();
        draft.addAll(rebuilt);
        enforceGeneratedStyleSets();
        rankedSeason = recommendation.season();
        selectedDraftSlot = draft.isEmpty() ? -1 : Math.min(selectedDraftSlot, draft.size() - 1);
        rankedTeamGenerated = draft.size() == TeamModels.MAX_TEAM_SIZE;
        rankedReasons.clear();
        rememberRankedReasons(recommendation);
        status = uiText("status_ranked_regenerated", recommendation.season());
        statusError = false;
        clearAndInit();
    }

    private DraftSlot newRankedSlot(RankedUsageService.SpeciesStats stats,
                                    Map<String, RankedLocalTemplate> templates, String season) {
        if (stats == null) return null;
        RankedLocalTemplate template = templates.get(RankedUsageService.key(stats.name));
        if (template == null) return null;
        DraftSlot empty = new DraftSlot(null, null, template.species.getResourceIdentifier().toString(),
                formId(template.form, template.species), "minecraft:air", null, List.of(),
                null, Map.of(), uiText("source_ranked", season));
        return rankedSet(empty, stats, season);
    }

    private void toggleRankedLock(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= draft.size() || !rankedTeamGenerated) return;
        if (!rankedLockedSlots.remove(slotIndex)) rankedLockedSlots.add(slotIndex);
        status = uiText(rankedLockedSlots.contains(slotIndex) ? "status_ranked_locked" : "status_ranked_unlocked",
                displayName(draft.get(slotIndex)));
        statusError = false;
        clearAndInit();
    }

    private void shiftRankedLocksAfterRemoval(int removedIndex) {
        Set<Integer> shifted = new LinkedHashSet<>();
        for (int locked : rankedLockedSlots) {
            if (locked < removedIndex) shifted.add(locked);
            else if (locked > removedIndex) shifted.add(locked - 1);
        }
        rankedLockedSlots.clear();
        rankedLockedSlots.addAll(shifted);
    }

    private void swapRankedLocks(int first, int second) {
        boolean firstLocked = rankedLockedSlots.remove(first);
        boolean secondLocked = rankedLockedSlots.remove(second);
        if (firstLocked) rankedLockedSlots.add(second);
        if (secondLocked) rankedLockedSlots.add(first);
    }

    private void regenerateRankedSlot(int slotIndex) {
        if (!rankedTeamGenerated || rankedHelperRunning || client == null
                || slotIndex < 0 || slotIndex >= draft.size() || rankedLockedSlots.contains(slotIndex)) return;
        Map<String, RankedLocalTemplate> templates = rankedTemplates();
        Map<String, RankedUsageService.CandidateProfile> profiles = rankedProfiles(templates);
        List<String> existing = new ArrayList<>();
        for (int index = 0; index < draft.size(); index++) {
            if (index != slotIndex) existing.add(rankedKey(draft.get(index)));
        }
        String previousKey = rankedKey(draft.get(slotIndex));
        Set<String> excludedSpecies = rankedSpeciesIds(draft);
        profiles = excludeRankedSpeciesVariants(profiles, templates, excludedSpecies,
                new LinkedHashSet<>(existing));
        long variation = RANKED_VARIATION.getAndIncrement() + (long) slotIndex * 31L;
        rankedHelperRunning = true;
        int request = ++rankedHelperRequest;
        if (rankedHelperFuture != null) rankedHelperFuture.cancel(true);
        status = uiText("status_ranked_slot_loading", slotIndex + 1);
        statusError = false;
        clearAndInit();
        requestRankedSlot(existing, previousKey, excludedSpecies, slotIndex,
                templates, profiles, variation, 0, request);
    }

    private void requestRankedSlot(List<String> existing, String previousKey, Set<String> excludedSpecies,
                                   int slotIndex,
                                   Map<String, RankedLocalTemplate> templates,
                                   Map<String, RankedUsageService.CandidateProfile> profiles,
                                   long variation, int attempt, int request) {
        CompletableFuture<RankedUsageService.Recommendation> loading =
                RankedUsageService.INSTANCE.recommend(existing, profiles, TeamModels.MAX_TEAM_SIZE, rankedSeason,
                        variation, rankedBuildStyle, rankedGenerationMode);
        rankedHelperFuture = loading;
        loading.whenComplete((recommendation, error) -> {
                    if (client == null) return;
                    client.execute(() -> {
                        if (request != rankedHelperRequest) return;
                        RankedUsageService.SpeciesStats stats = recommendation == null
                                || recommendation.additions().isEmpty() ? null : recommendation.additions().get(0);
                        String replacementKey = stats == null ? "" : RankedUsageService.key(stats.name);
                        String replacementSpecies = rankedSpeciesId(templates.get(replacementKey));
                        boolean repeatedPokemon = replacementKey.equals(previousKey)
                                || excludedSpecies.contains(replacementSpecies);
                        if (error == null && repeatedPokemon
                                && attempt + 1 < RANKED_VARIATION_ATTEMPTS) {
                            requestRankedSlot(existing, previousKey, excludedSpecies, slotIndex, templates, profiles,
                                    variation + 1L, attempt + 1, request);
                            return;
                        }
                        rankedHelperRunning = false;
                        DraftSlot replacement = newRankedSlot(stats, templates,
                                recommendation == null ? rankedSeason : recommendation.season());
                        if (error != null || repeatedPokemon || replacement == null || slotIndex >= draft.size()) {
                            TropimonTeamSaverClient.LOGGER.warn("Régénération Ranked du slot indisponible", error);
                            status = uiText("error_ranked_unavailable");
                            statusError = true;
                            clearAndInit();
                            return;
                        }
                        draft.set(slotIndex, replacement);
                        enforceGeneratedStyleSets();
                        rankedReasons.remove(previousKey);
                        rankedSeason = recommendation.season();
                        rememberRankedReasons(recommendation);
                        status = uiText("status_ranked_slot_ready", slotIndex + 1, displayName(replacement));
                        statusError = false;
                        clearAndInit();
                    });
                });
    }

    private void enforceGeneratedStyleSets() {
        if (rankedBuildStyle != RankedUsageService.BuildStyle.TRICK_ROOM) return;
        String requiredMove = "trickroom";
        int setters = 0;
        for (DraftSlot slot : draft) {
            if (slot.moveIds.stream().map(RankedUsageService::key).anyMatch(requiredMove::equals)) setters++;
        }
        for (int index = 0; index < draft.size() && setters < 2; index++) {
            if (rankedLockedSlots.contains(index)) continue;
            DraftSlot slot = draft.get(index);
            Species species = findSpecies(slot.speciesId);
            FormData form = findForm(species, slot.formId);
            List<String> legal = rankedMoveIds(form, List.of(requiredMove));
            if (legal.isEmpty()) continue;
            String canonicalMove = legal.getFirst();
            List<String> moves = new ArrayList<>(slot.moveIds);
            if (moves.stream().map(RankedUsageService::key).anyMatch(requiredMove::equals)) continue;
            if (moves.size() >= TeamModels.MAX_MOVES) moves.set(moves.size() - 1, canonicalMove);
            else moves.add(canonicalMove);
            draft.set(index, new DraftSlot(slot.pokemon, slot.pokemonId, slot.speciesId, slot.formId,
                    slot.itemId, slot.abilityId, List.copyOf(moves), slot.natureId, slot.evs, slot.source));
            setters++;
        }
    }

    private void cycleRankedStyle() {
        if (rankedHelperRunning) return;
        RankedUsageService.BuildStyle[] styles = RankedUsageService.BuildStyle.values();
        rankedBuildStyle = styles[(rankedBuildStyle.ordinal() + 1) % styles.length];
        discardRankedProposals();
        status = uiText("status_ranked_style", rankedStyleName());
        statusError = false;
        clearAndInit();
    }

    private void cycleRankedMonotypeType() {
        if (rankedHelperRunning) return;
        int current = MONOTYPE_TYPES.indexOf(rankedMonotypeType);
        rankedMonotypeType = MONOTYPE_TYPES.get(Math.floorMod(current + 1, MONOTYPE_TYPES.size()));
        discardRankedProposals();
        status = uiText("status_ranked_monotype_type", rankedMonotypeTypeName());
        statusError = false;
        clearAndInit();
    }

    private void cycleRankedGenerationMode() {
        if (rankedHelperRunning) return;
        RankedUsageService.GenerationMode[] modes = RankedUsageService.GenerationMode.values();
        rankedGenerationMode = modes[(rankedGenerationMode.ordinal() + 1) % modes.length];
        discardRankedProposals();
        status = uiText("status_ranked_mode", uiText("ranked_mode_"
                + rankedGenerationMode.name().toLowerCase(Locale.ROOT)));
        statusError = false;
        clearAndInit();
    }

    private void discardRankedProposals() {
        rankedProposal = null;
        rankedReasons.clear();
    }

    private String rankedStyleName() {
        return uiText("ranked_style_" + rankedBuildStyle.name().toLowerCase(Locale.ROOT));
    }

    private String rankedMonotypeTypeName() {
        return uiText("ranked_monotype_" + rankedMonotypeType);
    }

    private boolean draftHasType(DraftSlot slot, String expectedType) {
        Species species = findSpecies(slot.speciesId);
        FormData form = findForm(species, slot.formId);
        if (form == null || expectedType == null) return false;
        if (RankedUsageService.key(form.getPrimaryType().getName()).equals(expectedType)) return true;
        return form.getSecondaryType() != null
                && RankedUsageService.key(form.getSecondaryType().getName()).equals(expectedType);
    }

    private void rememberRankedReasons(RankedUsageService.Recommendation recommendation) {
        if (recommendation == null) return;
        for (Map.Entry<String, RankedUsageService.SpeciesStats> entry : recommendation.details().entrySet()) {
            RankedUsageService.SpeciesStats stats = entry.getValue();
            if (stats == null) continue;
            String bestPartner = "";
            double bestAffinity = 0.0D;
            for (RankedUsageService.SpeciesStats source : recommendation.details().values()) {
                if (source == null || source == stats || source.teammates == null) continue;
                for (Map.Entry<String, Double> teammate : source.teammates.entrySet()) {
                    if (RankedUsageService.key(teammate.getKey()).equals(entry.getKey())
                            && teammate.getValue() > bestAffinity) {
                        bestPartner = source.name;
                        bestAffinity = teammate.getValue();
                    }
                }
            }
            rankedReasons.put(entry.getKey(), new RankedReason(bestPartner, bestAffinity,
                    stats.usagePercent, rankedStyleName()));
        }
    }

    private DraftSlot rankedSet(DraftSlot previous, RankedUsageService.SpeciesStats stats, String season) {
        Species species = findSpecies(previous.speciesId);
        FormData form = findForm(species, previous.formId);
        Map<Stat, Integer> baseStats = form == null ? Map.of() : form.getBaseStats();
        String archetype = rankedArchetype(baseStats);
        String rankedItem = rankedItemId(stats.items);
        String rankedAbility = rankedAbilityId(form, stats.abilities, rankedBuildStyle);
        List<String> rankedMoves = rankedMoveIds(form, rankedMoveOrder(stats.moves, rankedBuildStyle));
        String rankedNature = validNature(RankedUsageService.key(stats.mostUsedNature()));
        Map<String, Integer> rankedSpread = rankedEvs(stats.mostUsedSpread());

        ShowdownGen9SetService.CompetitiveSet showdown = ShowdownGen9SetService.INSTANCE
                .bestSet(rankedKey(previous), rankedBuildStyle, archetype);
        List<String> showdownMoves = showdown == null ? List.of() : rankedMoveIds(form, showdown.moves);
        String showdownItem = showdown == null ? null : rankedItemId(showdown.item);
        String showdownNature = showdown == null ? null : validNature(RankedUsageService.key(showdown.nature));
        Map<String, Integer> showdownSpread = showdown == null ? Map.of() : normalizedEvs(showdown.evs);
        boolean rankedIncomplete = rankedItem == null || rankedAbility == null || rankedMoves.size() < 4
                || rankedNature == null || rankedSpread.isEmpty();
        boolean rankedCoherent = coherentCompetitiveSet(rankedItem, rankedNature, rankedSpread, rankedMoves);
        boolean showdownCoherent = showdown != null && showdownMoves.size() >= 3
                && coherentCompetitiveSet(showdownItem, showdownNature, showdownSpread, showdownMoves);
        boolean useShowdown = showdownCoherent && (rankedIncomplete || !rankedCoherent);

        String item = useShowdown ? showdownItem : rankedItem;
        String ability = useShowdown ? rankedAbilityId(form, showdown.ability) : rankedAbility;
        List<String> moves = useShowdown ? showdownMoves : rankedMoves;
        String nature = useShowdown ? showdownNature : rankedNature;
        Map<String, Integer> evs = useShowdown ? showdownSpread : rankedSpread;
        if (item == null) item = rankedItem != null ? rankedItem : fallbackCompetitiveItem(archetype);
        if (ability == null) ability = rankedAbility != null ? rankedAbility : fallbackCompetitiveAbility(form);
        if (moves.size() < TeamModels.MAX_MOVES) {
            moves = fillCompetitiveMoves(form, moves, archetype, rankedBuildStyle);
        }
        if (nature == null) nature = rankedNature != null ? rankedNature : fallbackCompetitiveNature(baseStats);
        if (evs.isEmpty()) evs = rankedSpread.isEmpty() ? fallbackCompetitiveEvs(baseStats) : rankedSpread;
        if (!coherentCompetitiveSet(item, nature, evs, moves)) {
            CompetitiveSetCoherence.Offence offence = CompetitiveSetCoherence.preferredOffence(
                    competitiveSetProfile(item, nature, evs, moves));
            moves = repairCompetitiveMoves(form, moves, offence, item, archetype);
            if (!coherentCompetitiveSet(item, nature, evs, moves)) {
                item = coherentFallbackItem(moves, archetype);
            }
        }
        String source = useShowdown ? uiText("source_showdown_gen9", season) : uiText("source_ranked", season);
        return new DraftSlot(previous.pokemon, previous.pokemonId, previous.speciesId, previous.formId,
                item == null ? previous.itemId : item,
                ability == null ? previous.abilityId : ability,
                moves.isEmpty() ? previous.moveIds : moves,
                nature == null || nature.isBlank() ? previous.natureId : nature,
                evs.isEmpty() ? previous.evs : evs,
                source);
    }

    private boolean coherentCompetitiveSet(String item, String nature, Map<String, Integer> evs,
                                             List<String> moves) {
        return CompetitiveSetCoherence.coherent(competitiveSetProfile(item, nature, evs, moves));
    }

    private CompetitiveSetCoherence.Profile competitiveSetProfile(String item, String nature,
                                                                    Map<String, Integer> evs,
                                                                    List<String> moves) {
        List<CompetitiveSetCoherence.MoveInfo> moveInfo = new ArrayList<>();
        if (moves != null) for (String moveId : moves) {
            MoveTemplate move = com.cobblemon.mod.common.api.moves.Moves.getByName(moveId);
            if (move == null) continue;
            String category = RankedUsageService.key(move.getDamageCategory().getName());
            moveInfo.add(new CompetitiveSetCoherence.MoveInfo(move.getName(), category,
                    move.getPower() > 0.0D && !category.equals("status")));
        }
        return new CompetitiveSetCoherence.Profile(item, nature, evs, moveInfo);
    }

    private List<String> repairCompetitiveMoves(FormData form, List<String> original,
                                                CompetitiveSetCoherence.Offence offence,
                                                String item, String archetype) {
        List<String> compatible = new ArrayList<>();
        if (original != null) for (String moveId : original) {
            MoveTemplate move = com.cobblemon.mod.common.api.moves.Moves.getByName(moveId);
            if (move == null) continue;
            String category = RankedUsageService.key(move.getDamageCategory().getName());
            CompetitiveSetCoherence.MoveInfo info = new CompetitiveSetCoherence.MoveInfo(
                    move.getName(), category, move.getPower() > 0.0D && !category.equals("status"));
            if (CompetitiveSetCoherence.supports(info, offence) && !compatible.contains(move.getName())) {
                compatible.add(move.getName());
            }
        }
        String role = offence == CompetitiveSetCoherence.Offence.MIXED ? archetype : offence.id;
        List<String> repaired = new ArrayList<>(fillCompetitiveMoves(
                form, compatible, role, rankedBuildStyle));
        if (CompetitiveSetCoherence.loadedDice(item)
                && repaired.stream().noneMatch(CompetitiveSetCoherence::multiHit)) {
            MoveTemplate multiHit = bestLegalMultiHit(form, offence, repaired, role);
            if (multiHit != null) {
                if (repaired.size() < TeamModels.MAX_MOVES) repaired.add(multiHit.getName());
                else repaired.set(repaired.size() - 1, multiHit.getName());
            }
        }
        return List.copyOf(repaired);
    }

    private MoveTemplate bestLegalMultiHit(FormData form, CompetitiveSetCoherence.Offence offence,
                                           List<String> selected, String archetype) {
        if (form == null) return null;
        CobblemonCatalogueCache.Entry cached = catalogue.snapshot().entry(form);
        Iterable<MoveTemplate> legal = cached == null ? form.getMoves().getAllLegalMoves() : cached.legalMoves();
        MoveTemplate best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (MoveTemplate move : legal) {
            if (!CompetitiveSetCoherence.multiHit(move.getName())) continue;
            String category = RankedUsageService.key(move.getDamageCategory().getName());
            CompetitiveSetCoherence.MoveInfo info = new CompetitiveSetCoherence.MoveInfo(
                    move.getName(), category, move.getPower() > 0.0D && !category.equals("status"));
            if (!CompetitiveSetCoherence.supports(info, offence)) continue;
            double score = competitiveMoveScore(move, form, selected, archetype, rankedBuildStyle);
            if (score > bestScore) {
                best = move;
                bestScore = score;
            }
        }
        return best;
    }

    private String coherentFallbackItem(List<String> moves, String archetype) {
        boolean bellyDrum = moves != null && moves.stream()
                .map(RankedUsageService::key).anyMatch("bellydrum"::equals);
        if (bellyDrum) {
            String sitrus = rankedItemId("Sitrus Berry");
            if (sitrus != null) return sitrus;
        }
        return fallbackCompetitiveItem(archetype);
    }

    private String fallbackCompetitiveItem(String archetype) {
        if (rankedBuildStyle == RankedUsageService.BuildStyle.AURORA_VEIL) {
            String lightClay = rankedItemId("Light Clay");
            if (lightClay != null) return lightClay;
        }
        if (rankedBuildStyle == RankedUsageService.BuildStyle.STALL || "bulky".equals(archetype)) {
            String leftovers = rankedItemId("Leftovers");
            if (leftovers != null) return leftovers;
        }
        String lifeOrb = rankedItemId("Life Orb");
        return lifeOrb != null ? lifeOrb : rankedItemId("Leftovers");
    }

    private String fallbackCompetitiveAbility(FormData form) {
        List<AbilityChoice> abilities = cachedAbilityChoices(form);
        for (AbilityChoice ability : abilities) {
            if (RankedUsageService.isStyleAbility(ability.id, rankedBuildStyle)) return ability.id;
        }
        return abilities.isEmpty() ? null : abilities.getFirst().id;
    }

    private String fallbackCompetitiveNature(Map<Stat, Integer> stats) {
        int attack = stats.getOrDefault(Stats.ATTACK, 0);
        int specialAttack = stats.getOrDefault(Stats.SPECIAL_ATTACK, 0);
        int defence = stats.getOrDefault(Stats.DEFENCE, 0);
        int specialDefence = stats.getOrDefault(Stats.SPECIAL_DEFENCE, 0);
        boolean physical = attack >= specialAttack;
        if (rankedBuildStyle == RankedUsageService.BuildStyle.TRICK_ROOM) return physical ? "brave" : "quiet";
        if (rankedBuildStyle == RankedUsageService.BuildStyle.STALL || rankedArchetype(stats).equals("bulky")) {
            return defence <= specialDefence ? "bold" : "calm";
        }
        int speed = stats.getOrDefault(Stats.SPEED, 0);
        if (speed >= 85) return physical ? "jolly" : "timid";
        return physical ? "adamant" : "modest";
    }

    private Map<String, Integer> fallbackCompetitiveEvs(Map<Stat, Integer> stats) {
        Map<String, Integer> result = new LinkedHashMap<>();
        int attack = stats.getOrDefault(Stats.ATTACK, 0);
        int specialAttack = stats.getOrDefault(Stats.SPECIAL_ATTACK, 0);
        int defence = stats.getOrDefault(Stats.DEFENCE, 0);
        int specialDefence = stats.getOrDefault(Stats.SPECIAL_DEFENCE, 0);
        boolean bulky = rankedBuildStyle == RankedUsageService.BuildStyle.STALL
                || rankedArchetype(stats).equals("bulky");
        if (bulky) {
            result.put("hp", 252);
            result.put(defence <= specialDefence ? "def" : "spd", 252);
            result.put(defence <= specialDefence ? "spd" : "def", 4);
        } else {
            result.put(attack >= specialAttack ? "atk" : "spa", 252);
            result.put("spe", 252);
            result.put("hp", 4);
        }
        return Map.copyOf(result);
    }

    private Map<String, Integer> normalizedEvs(Map<String, Integer> raw) {
        if (raw == null || raw.isEmpty()) return Map.of();
        Map<String, Integer> result = new LinkedHashMap<>();
        int total = 0;
        for (String key : EV_KEYS) {
            int value = Math.max(0, Math.min(252, raw.getOrDefault(key, 0)));
            value = Math.min(value, Math.max(0, 510 - total));
            if (value > 0) {
                result.put(key, value);
                total += value;
            }
        }
        return Map.copyOf(result);
    }

    private String validNature(String nature) {
        return nature != null && NATURE_IDS.contains(nature) ? nature : null;
    }

    private List<String> fillCompetitiveMoves(FormData form, List<String> initial, String archetype,
                                              RankedUsageService.BuildStyle style) {
        if (form == null) return initial == null ? List.of() : List.copyOf(initial);
        Map<String, MoveTemplate> legal = new LinkedHashMap<>();
        CobblemonCatalogueCache.Entry cached = catalogue.snapshot().entry(form);
        Iterable<MoveTemplate> templates = cached == null ? form.getMoves().getAllLegalMoves() : cached.legalMoves();
        for (MoveTemplate move : templates) legal.putIfAbsent(RankedUsageService.key(move.getName()), move);
        List<String> result = new ArrayList<>();
        if (initial != null) {
            for (String move : initial) {
                MoveTemplate template = legal.get(RankedUsageService.key(move));
                if (template != null && !result.contains(template.getName())) result.add(template.getName());
                if (result.size() == TeamModels.MAX_MOVES) return List.copyOf(result);
            }
        }
        List<MoveTemplate> candidates = new ArrayList<>(legal.values());
        while (result.size() < TeamModels.MAX_MOVES && !candidates.isEmpty()) {
            MoveTemplate best = candidates.stream().max(Comparator.comparingDouble(
                    move -> competitiveMoveScore(move, form, result, archetype, style))).orElse(null);
            if (best == null) break;
            result.add(best.getName());
            candidates.remove(best);
        }
        return List.copyOf(result);
    }

    private double competitiveMoveScore(MoveTemplate move, FormData form, List<String> selected,
                                        String archetype, RankedUsageService.BuildStyle style) {
        String id = RankedUsageService.key(move.getName());
        String category = RankedUsageService.key(move.getDamageCategory().getName());
        String role = RankedUsageService.key(archetype);
        boolean statusMove = move.getPower() <= 0.0D || category.equals("status");
        double score = RankedUsageService.isStyleMove(id, style) ? 500.0D : 0.0D;
        if (statusMove) {
            if (Set.of("bellydrum", "swordsdance", "dragondance", "bulkup", "coil", "howl", "shiftgear")
                    .contains(id)) score += role.equals("physical") ? 220.0D : role.equals("special") ? -300.0D : 40.0D;
            if (Set.of("nastyplot", "tailglow", "quiverdance", "geomancy", "calmmind")
                    .contains(id)) score += role.equals("special") ? 220.0D : role.equals("physical") ? -300.0D : 40.0D;
            if (Set.of("protect", "stealthrock", "spikes", "toxicspikes", "stickyweb", "defog",
                    "rapidspin", "taunt", "toxic", "thunderwave", "willowisp", "encore",
                    "swordsdance", "nastyplot", "calmmind", "dragondance", "bulkup")
                    .contains(id)) score += 150.0D;
            if (Set.of("recover", "roost", "softboiled", "slackoff", "synthesis", "moonlight",
                    "morningsun", "wish", "rest", "strengthsap", "shoreup").contains(id)) {
                score += style == RankedUsageService.BuildStyle.STALL || archetype.equals("bulky") ? 320.0D : 90.0D;
            }
            return score;
        }
        score += Math.min(150.0D, move.getPower());
        String moveType = RankedUsageService.key(move.getElementalType().getName());
        if (RankedUsageService.key(form.getPrimaryType().getName()).equals(moveType)
                || form.getSecondaryType() != null
                && RankedUsageService.key(form.getSecondaryType().getName()).equals(moveType)) score += 65.0D;
        if (role.equals("physical") && category.equals("physical")) score += 55.0D;
        if (role.equals("special") && category.equals("special")) score += 55.0D;
        if (role.equals("fastoffense")) score += 25.0D;
        if (move.getAccuracy() > 0.0D && move.getAccuracy() < 80.0D) score -= 25.0D;
        for (String chosen : selected) {
            MoveTemplate existing = com.cobblemon.mod.common.api.moves.Moves.getByName(chosen);
            if (existing != null && RankedUsageService.key(existing.getElementalType().getName()).equals(moveType)) {
                score -= 45.0D;
            }
        }
        return score;
    }

    private Map<String, RankedLocalTemplate> rankedTemplates() {
        if (rankedTemplateCache != null) return rankedTemplateCache;
        Map<String, RankedLocalTemplate> result = new LinkedHashMap<>();
        for (CobblemonCatalogueCache.Entry entry : catalogue.snapshot().entries()) {
            addRankedTemplate(result, entry.species(), entry.form());
        }
        rankedTemplateCache = Map.copyOf(result);
        return rankedTemplateCache;
    }

    private Map<String, RankedUsageService.CandidateProfile> rankedProfiles(
            Map<String, RankedLocalTemplate> templates) {
        if (rankedProfileCache != null) return rankedProfileCache;
        Map<String, RankedUsageService.CandidateProfile> result = new LinkedHashMap<>();
        for (Map.Entry<String, RankedLocalTemplate> entry : templates.entrySet()) {
            FormData form = entry.getValue().form;
            CobblemonCatalogueCache.Entry cached = catalogue.snapshot().entry(form);
            Set<String> types = cached == null ? Set.of() : cached.types();
            Map<Stat, Integer> stats = cached == null ? form.getBaseStats() : cached.baseStats();
            Set<String> abilities = cached == null ? Set.of() : cached.abilities().stream()
                    .map(CobblemonCatalogueCache.AbilitySpec::id).collect(java.util.stream.Collectors.toSet());
            Set<String> moves = cached == null ? Set.of() : cached.legalMoveIds();
            result.put(entry.getKey(), new RankedUsageService.CandidateProfile(types,
                    rankedArchetype(stats), stats.getOrDefault(Stats.SPEED, 0), abilities, moves));
        }
        rankedProfileCache = Map.copyOf(result);
        return rankedProfileCache;
    }

    private String rankedArchetype(Map<Stat, Integer> stats) {
        int hp = stats.getOrDefault(Stats.HP, 0);
        int attack = stats.getOrDefault(Stats.ATTACK, 0);
        int defence = stats.getOrDefault(Stats.DEFENCE, 0);
        int specialAttack = stats.getOrDefault(Stats.SPECIAL_ATTACK, 0);
        int specialDefence = stats.getOrDefault(Stats.SPECIAL_DEFENCE, 0);
        int speed = stats.getOrDefault(Stats.SPEED, 0);
        if (Math.max(attack, specialAttack) >= 105 && speed >= 90) return "fast_offense";
        if (hp + defence + specialDefence >= 300) return "bulky";
        if (attack >= specialAttack + 20) return "physical";
        if (specialAttack >= attack + 20) return "special";
        return "balanced";
    }

    private void addRankedTemplate(Map<String, RankedLocalTemplate> result, Species species, FormData form) {
        if (species == null || form == null) return;
        String key = rankedKey(species, form);
        if (!key.isBlank()) result.putIfAbsent(key, new RankedLocalTemplate(species, form));
    }

    private String rankedKey(PokemonChoice choice) {
        return choice == null ? "" : rankedKey(choice.species, choice.form);
    }

    private String rankedKey(DraftSlot slot) {
        if (slot == null) return "";
        Species species = slot.pokemon == null ? findSpecies(slot.speciesId) : slot.pokemon.getSpecies();
        FormData form = slot.pokemon == null ? findForm(species, slot.formId) : slot.pokemon.getForm();
        return rankedKey(species, form);
    }

    private String rankedKey(SavedSlot slot) {
        if (slot == null) return "";
        Species species = findSpecies(slot.speciesId);
        FormData form = findForm(species, slot.formId);
        return rankedKey(species, form);
    }

    private String rankedKey(Species species, FormData form) {
        if (species == null || form == null) return "";
        FormData standard = species.getStandardForm();
        boolean isStandard = form == standard || standard != null
                && form.getName().equals(standard.getName())
                && form.getAspects().equals(standard.getAspects());
        if (isStandard) return RankedUsageService.key(species.showdownId());
        String showdown = RankedUsageService.key(form.showdownId());
        return showdown.isBlank() ? RankedUsageService.key(species.showdownId()) : showdown;
    }

    private String rankedItemId(Map<String, Double> rankedItems) {
        if (rankedItems == null) return null;
        return rankedItems.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(entry -> rankedItemId(entry.getKey()))
                .filter(java.util.Objects::nonNull)
                .findFirst().orElse(null);
    }

    private String rankedItemId(String rankedName) {
        String key = RankedUsageService.key(rankedName);
        if (key.isBlank()) return null;
        return catalogue.snapshot().itemByRankedKey().get(key);
    }

    private String rankedAbilityId(FormData form, String rankedName) {
        String key = RankedUsageService.key(rankedName);
        if (form == null || key.isBlank()) return null;
        for (AbilityChoice choice : cachedAbilityChoices(form)) {
            if (RankedUsageService.key(choice.id).equals(key)) return choice.id;
        }
        return null;
    }

    private String rankedAbilityId(FormData form, Map<String, Double> rankedAbilities,
                                   RankedUsageService.BuildStyle style) {
        if (rankedAbilities == null) return null;
        return rankedAbilities.entrySet().stream()
                .sorted(Comparator
                        .<Map.Entry<String, Double>, Boolean>comparing(
                                entry -> RankedUsageService.isStyleAbility(entry.getKey(), style)).reversed()
                        .thenComparing(Map.Entry.<String, Double>comparingByValue().reversed()))
                .map(entry -> rankedAbilityId(form, entry.getKey()))
                .filter(java.util.Objects::nonNull)
                .findFirst().orElse(null);
    }

    private List<String> rankedMoveOrder(Map<String, Double> rankedMoves,
                                         RankedUsageService.BuildStyle style) {
        if (rankedMoves == null || rankedMoves.isEmpty()) return List.of();
        return rankedMoves.entrySet().stream()
                .sorted(Comparator
                        .<Map.Entry<String, Double>, Boolean>comparing(
                                entry -> RankedUsageService.isStyleMove(entry.getKey(), style)).reversed()
                        .thenComparing(Map.Entry.<String, Double>comparingByValue().reversed()))
                .map(Map.Entry::getKey)
                .toList();
    }

    private List<String> rankedMoveIds(FormData form, List<String> rankedMoves) {
        if (form == null || rankedMoves == null) return List.of();
        Map<String, MoveTemplate> legal = new LinkedHashMap<>();
        CobblemonCatalogueCache.Entry cached = catalogue.snapshot().entry(form);
        Iterable<MoveTemplate> legalMoves = cached == null
                ? form.getMoves().getAllLegalMoves() : cached.legalMoves();
        for (MoveTemplate template : legalMoves) {
            legal.putIfAbsent(RankedUsageService.key(template.getName()), template);
        }
        List<String> result = new ArrayList<>();
        for (String rankedMove : rankedMoves) {
            MoveTemplate template = legal.get(RankedUsageService.key(rankedMove));
            if (template != null && !result.contains(template.getName())) result.add(template.getName());
            if (result.size() == TeamModels.MAX_MOVES) break;
        }
        return List.copyOf(result);
    }

    private Map<String, Integer> rankedEvs(String spread) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (spread == null) return result;
        Map<String, String> aliases = Map.of("hp", "hp", "atk", "atk", "def", "def",
                "spa", "spa", "spd", "spd", "spe", "spe");
        for (String part : spread.split("/")) {
            String[] pieces = part.strip().split("\\s+", 2);
            if (pieces.length != 2) continue;
            try {
                int value = Integer.parseInt(pieces[0]);
                String stat = aliases.get(pieces[1].toLowerCase(Locale.ROOT));
                if (stat != null && value > 0) result.put(stat, Math.min(252, value));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    private void filterPokemonChoices() {
        pickerDetails.clear(); // Also called when the selected Ranked season/data changes.
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
                choice -> pickerNames.computeIfAbsent(choice, this::choiceDisplayName), String.CASE_INSENSITIVE_ORDER);
        Comparator<PokemonChoice> byLevel = Comparator.comparingInt(this::pokemonChoiceLevel).reversed();
        Comparator<PokemonChoice> byEvolutionStage = Comparator.comparingInt(this::evolutionPriority);
        if (pokemonPickerSortStat == null) {
            return pokemonPickerAllSpecies
                    ? Comparator.comparingDouble(this::pokemonChoiceUsage).reversed()
                            .thenComparing(byEvolutionStage).thenComparing(byName)
                    : byLevel.thenComparing(byName);
        }

        Comparator<PokemonChoice> byStat = Comparator.comparingInt(
                choice -> pokemonChoiceStatValue(choice, pokemonPickerSortStat));
        if (pokemonPickerSortDescending) byStat = byStat.reversed();
        return pokemonPickerAllSpecies
                ? byStat.thenComparing(byEvolutionStage).thenComparing(byName)
                : byStat.thenComparing(byLevel).thenComparing(byName);
    }

    private int pokemonChoiceLevel(PokemonChoice choice) {
        return choice.pokemon == null ? -1 : choice.pokemon.getLevel();
    }

    private double pokemonChoiceUsage(PokemonChoice choice) {
        RankedUsageService.UsageEntry usage = rankedUsage.get(rankedKey(choice));
        return usage == null ? 0.0D : usage.usagePercent;
    }

    private int pokemonChoiceStatValue(PokemonChoice choice, Stat stat) {
        if (choice.pokemon != null) return choice.pokemon.getIvs().getOrDefault(stat);
        CobblemonCatalogueCache.Entry entry = catalogue.snapshot().entry(choice.form);
        return (entry == null ? choice.form.getBaseStats() : entry.baseStats()).getOrDefault(stat, 0);
    }

    private int evolutionPriority(PokemonChoice choice) {
        if (choice == null || choice.form == null) return 2;
        if (choice.form.getEvolutions().isEmpty()) return 0;
        return choice.form.getPreEvolution() == null ? 2 : 1;
    }

    private String pokemonSearchText(PokemonChoice choice) {
        return pickerSearchText.computeIfAbsent(choice, this::buildPokemonSearchText);
    }

    private String buildPokemonSearchText(PokemonChoice choice) {
        Pokemon pokemon = choice.pokemon;
        Species species = choice.species;
        FormData form = choice.form;
        String secondary = form.getSecondaryType() == null ? "" : form.getSecondaryType().getDisplayName().getString();
        String ability = choice.abilities.stream()
                .map(entry -> entry.id + " " + abilityName(entry.id).getString())
                .reduce("", (left, right) -> left + " " + right);
        return (pokemonNickname(choice) + " " + choiceDisplayName(choice) + " " + species.getName() + " " + form.getName() + " "
                + form.showdownId() + " " + form.getPrimaryType().getDisplayName().getString() + " " + secondary + " "
                + ability + " " + choice.source).toLowerCase(Locale.ROOT);
    }

    private int pokemonPickerPages() {
        int rows = pokemonPickerRows();
        return Math.max(1, (filteredPokemonChoices.size() + rows - 1) / rows);
    }

    private void closePokemonPicker(boolean rebuild) {
        pokemonPickerOpen = false;
        pokemonPickerAssociationOnly = false;
        pokemonPickerAllSpecies = rememberedPokemonPickerAllSpecies;
        pokemonPickerPage = 0;
        pokemonSearchField = null;
        pokemonPickerModels.clear();
        pendingAbilityPokemon = null;
        abilityEditSlot = -1;
        pokemonPickerExcluded.clear();
        pokemonChoices.clear();
        filteredPokemonChoices.clear();
        if (rebuild) clearAndInit();
    }

    private Unit selectPokemon(PokemonChoice choice) {
        String selectedAbility = choice.pokemon == null ? null : abilityId(choice.pokemon);
        return selectPokemon(choice, selectedAbility);
    }

    private Unit selectPokemon(PokemonChoice choice, String selectedAbility) {
        int target = pickerReplaceIndex;
        Pokemon pokemon = choice.pokemon;
        if (target >= 0 && target < draft.size() && pokemon != null
                && shouldKeepPreset(draft.get(target), choice)) {
            DraftSlot planned = draft.get(target);
            if (!plannedAbilityMatches(planned, pokemon)) {
                status = uiText("error_association_ability", displayName(planned),
                        abilityName(planned.abilityId).getString(), abilityName(pokemon).getString());
                statusError = true;
                notifyLinkError(status);
                pickerReplaceIndex = -1;
                closePokemonPicker(false);
                clearAndInit();
                return Unit.INSTANCE;
            }
            AssociationOutcome outcome = associateCapturedPokemon(target, choice);
            List<String> warnings = associationWarnings(outcome);
            if (!warnings.isEmpty()) {
                status = uiText("status_associated_warnings", displayName(outcome.replacement()),
                        String.join(" · ", warnings));
                statusError = true;
                notifyAssociationWarnings(outcome);
            } else {
                status = uiText("status_associated", displayName(outcome.replacement()));
                statusError = false;
            }
            pickerReplaceIndex = -1;
            closePokemonPicker(false);
            clearAndInit();
            return Unit.INSTANCE;
        }
        String pokemonId = pokemon == null ? null : pokemon.getUuid().toString();
        String itemId = pokemon == null ? "minecraft:air" : itemId(pokemon.getHeldItem$common());
        List<String> moves = pokemon == null ? List.of() : activeMoveIds(pokemon);
        String natureId = pokemon == null ? null : pokemon.getEffectiveNature().getName().getPath();
        Map<String, Integer> evs = pokemon == null ? Map.of() : pokemonEvs(pokemon);
        String selectedForm = formId(choice.form, choice.species);
        DraftSlot replacement = new DraftSlot(pokemon, pokemonId, choice.species.getResourceIdentifier().toString(),
                selectedForm, itemId, selectedAbility, moves, natureId, evs, choice.source);
        if (target >= 0 && target < draft.size()) {
            draft.set(target, replacement);
            if (rankedTeamGenerated) rankedLockedSlots.add(target);
            selectedDraftSlot = target;
            status = uiText("status_replaced", target + 1, displayName(replacement));
            statusError = false;
        } else if (draft.size() < TeamModels.MAX_TEAM_SIZE) {
            draft.add(replacement);
            selectedDraftSlot = draft.size() - 1;
            status = uiText("status_added", displayName(replacement), choice.source);
            statusError = false;
        }
        pickerReplaceIndex = -1;
        closePokemonPicker(false);
        clearAndInit();
        return Unit.INSTANCE;
    }

    private boolean shouldKeepPreset(DraftSlot planned, PokemonChoice replacement) {
        if (planned == null || replacement == null || replacement.pokemon == null) return false;
        if (planned.pokemon == null) return true;
        return rankedKey(planned).equals(rankedKey(replacement));
    }

    private boolean plannedAbilityMatches(DraftSlot target, Pokemon pokemon) {
        String wanted = TeamModels.canonicalAbilityId(target == null ? null : target.abilityId);
        return wanted == null || wanted.equals(abilityId(pokemon));
    }

    private AssociationOutcome associateCapturedPokemon(int target, PokemonChoice choice) {
        DraftSlot previous = draft.get(target);
        Pokemon pokemon = choice.pokemon;
        List<String> activeMoves = activeMoveIds(pokemon);
        List<String> missing = List.of();
        if (!previous.moveIds.isEmpty()) {
            TeamMoveReconciler.Result reconciliation = reconcilePlannedMoves(pokemon, previous.moveIds);
            missing = reconciliation.missing();
        }
        String selectedForm = previous.formId == null ? formId(choice.form, choice.species) : previous.formId;
        String effectiveNature = pokemon.getEffectiveNature().getName().getPath();
        Map<String, Integer> actualEvs = pokemonEvs(pokemon);
        boolean natureMismatch = previous.natureId != null && !previous.natureId.isBlank()
                && !RankedUsageService.key(previous.natureId).equals(
                RankedUsageService.key(effectiveNature));
        boolean evMismatch = hasPlannedEvs(previous.evs) && !plannedEvsMatch(previous.evs, actualEvs);
        SavedSlot planned = new SavedSlot(previous.pokemonId, previous.speciesId, previous.formId,
                previous.itemId, previous.abilityId, previous.moveIds, previous.natureId, previous.evs);
        TeamPresetPolicy.SetValues preset = TeamPresetPolicy.afterAssociation(planned,
                abilityId(pokemon), activeMoves, effectiveNature, actualEvs);
        DraftSlot replacement = new DraftSlot(pokemon, pokemon.getUuid().toString(),
                choice.species.getResourceIdentifier().toString(), selectedForm, previous.itemId,
                preset.abilityId(), preset.moveIds(), preset.natureId(), preset.evs(), choice.source);
        draft.set(target, replacement);
        if (rankedTeamGenerated) rankedLockedSlots.add(target);
        selectedDraftSlot = target;
        return new AssociationOutcome(replacement, List.copyOf(missing), previous.natureId,
                natureMismatch, evMismatch);
    }

    private boolean hasPlannedEvs(Map<String, Integer> evs) {
        return evs != null && evs.values().stream().anyMatch(value -> value != null && value > 0);
    }

    private boolean plannedEvsMatch(Map<String, Integer> planned, Map<String, Integer> actual) {
        for (String key : EV_KEYS) {
            if (evValue(planned, key) != evValue(actual, key)) return false;
        }
        return true;
    }

    private int evValue(Map<String, Integer> evs, String key) {
        Integer value = evs == null ? null : evs.get(key);
        return value == null ? 0 : Math.max(0, value);
    }

    private List<String> associationWarnings(AssociationOutcome outcome) {
        List<String> warnings = new ArrayList<>();
        if (!outcome.missingMoves().isEmpty()) {
            String names = outcome.missingMoves().stream().map(this::moveDisplayName)
                    .reduce((left, right) -> left + ", " + right).orElse("");
            warnings.add(uiText("link_warning_moves", names));
        }
        if (outcome.natureMismatch()) {
            warnings.add(uiText("link_warning_nature",
                    plannedNatureName(outcome.plannedNatureId()).getString(),
                    natureName(outcome.replacement().pokemon).getString()));
        }
        if (outcome.evMismatch()) warnings.add(uiText("link_warning_evs"));
        return warnings;
    }

    private void notifyAssociationWarnings(AssociationOutcome outcome) {
        List<String> warnings = associationWarnings(outcome);
        if (warnings.isEmpty()) return;
        notifyLinkError(uiText("chat_link_warnings", displayName(outcome.replacement()),
                String.join(" · ", warnings)));
    }

    private void notifyLinkError(String message) {
        if (client == null || client.inGameHud == null) return;
        client.inGameHud.getChatHud().addMessage(Text.literal(message).formatted(Formatting.RED));
    }

    private TeamMoveReconciler.Result reconcilePlannedMoves(Pokemon pokemon, List<String> planned) {
        List<String> active = activeMoveIds(pokemon);
        Set<String> learned = new LinkedHashSet<>(active);
        for (BenchedMove move : pokemon.getBenchedMoves()) learned.add(move.getMoveTemplate().getName());
        return TeamMoveReconciler.reconcile(planned, new ArrayList<>(learned), active,
                Math.max(1, pokemon.getMoveSet().getMoves().size()));
    }

    private String moveDisplayName(String moveId) {
        MoveTemplate template = com.cobblemon.mod.common.api.moves.Moves.getByName(moveId);
        return template == null ? moveId : template.getDisplayName().getString();
    }

    private void saveDraft() {
        if (draft.isEmpty()) return;
        ownedPokemon.refreshIfChanged(parent);
        TeamValidationService.Report validation = validate(draftAsTeam());
        if (!validation.readyToSave()) {
            applyFailed(validationSummary(validation));
            return;
        }
        List<SavedSlot> slots = new ArrayList<>();
        for (DraftSlot slot : draft) {
            slots.add(new SavedSlot(slot.pokemonId, slot.speciesId, slot.formId,
                    slot.itemId, slot.abilityId, slot.moveIds, slot.natureId, slot.evs));
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
        rankedGenerationCore.clear();
        rankedRegenerationSnapshot.clear();
        rankedLockedSlots.clear();
        rankedReasons.clear();
        rankedProposal = null;
        rankedTeamGenerated = false;
        draftName = "";
        selectedDraftSlot = -1;
        selectedPreviewSlot = -1;
        page = selectedTeam / PAGE_SIZE;
        statusError = false;
        clearAndInit();
    }

    private void saveCurrentParty() {
        if (ClientTeamApplier.isActive(this)) return;
        if (parent.getParty().isEmpty()) {
            applyFailed(uiText("error_empty"));
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
        if (ClientTeamApplier.isActive(this)) return;
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
        if (ClientTeamApplier.isActive(this)) return;
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
        ownedPokemon.refresh(parent);
        TeamValidationService.Report validation = validate(team);
        if (!validation.readyToEquip()) {
            applyFailed(validationSummary(validation));
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
        lastApplyTeamId = team.id;
        status = uiText("status_applying", analysisSummary(analysis(team)));
        statusError = false;
        if (!ClientTeamApplier.start(this, parent, team, data)) pendingPartyUndo = null;
        else clearAndInit();
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

    void applyFinished(PlayerData updated, int missingItems) {
        ownedPokemon.refresh(parent);
        lastApplyTeamId = null;
        boolean positionsSaved = repository.save(client, updated);
        if (restoringParty) {
            restoringParty = false;
            status = positionsSaved ? uiText("status_restored") : uiText("error_save");
            statusError = !positionsSaved;
            clearAndInit();
            return;
        }
        boolean canUndo = !applyInBackground && installPartyUndo();
        String undoSuffix = canUndo ? uiText("status_undo_available") : ".";
        status = missingItems == 0 ? uiText("status_equipped", undoSuffix) :
                uiText("status_equipped_missing_items", missingItems, undoSuffix);
        statusError = !positionsSaved;
        if (!positionsSaved) status = uiText("error_save");
        if (finishBackgroundApply()) return;
        clearAndInit();
    }

    void applyFailed(String message) {
        pendingPartyUndo = null;
        restoringParty = false;
        status = message;
        statusError = true;
        if (finishBackgroundApply()) return;
        clearAndInit();
    }

    void applyProgress(String key, int current, int total, boolean retry) {
        status = uiText(key, current, total) + (retry ? uiText("progress_retrying") : "");
        statusError = false;
    }

    void applyCancelled(boolean changed) {
        restoringParty = false;
        boolean canUndo = changed && !applyInBackground && installPartyUndo();
        status = canUndo ? uiText("status_apply_cancelled_undo") : uiText("status_apply_cancelled");
        statusError = false;
        if (finishBackgroundApply()) return;
        clearAndInit();
    }

    void applyFailedAfterChange(String message) {
        restoringParty = false;
        boolean canUndo = !applyInBackground && installPartyUndo();
        status = canUndo ? uiText("error_partial_undo", message) : message;
        statusError = true;
        if (finishBackgroundApply()) return;
        clearAndInit();
    }

    private void continueApplyInBackground() {
        if (client == null || !ClientTeamApplier.isActive(this)) return;
        applyInBackground = true;
        cancelRankedHelperRequest();
        rankedUsageRequest++;
        if (rankedUsageFuture != null) rankedUsageFuture.cancel(true);
        if (client.player != null) client.player.sendMessage(ui("status_background_apply"), true);
        client.setScreen(null);
    }

    private boolean finishBackgroundApply() {
        if (!applyInBackground) return false;
        applyInBackground = false;
        pendingPartyUndo = null;
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal(status).formatted(statusError ? Formatting.RED : Formatting.GREEN), false);
        }
        if (!standalone) parent.closeNormally(true);
        return true;
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
        if (ClientTeamApplier.isActive(this)) return;
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
                    formId(pokemon), itemId(pokemon.getHeldItem$common()), abilityId(pokemon), activeMoveIds(pokemon),
                    pokemon.getEffectiveNature().getName().getPath(), pokemonEvs(pokemon)));
        }
        return snapshot;
    }

    private SavedTeam copyTeam(SavedTeam source) {
        SavedTeam copy = new SavedTeam();
        copy.id = source.id;
        copy.name = source.name;
        for (SavedSlot slot : source.slots) copy.slots.add(new SavedSlot(slot.pokemonId, slot.speciesId,
                slot.formId, normalizedItemId(slot.itemId), slot.abilityId, slot.moveIds, slot.natureId, slot.evs));
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
                addPokemonModel(pokemon, x + 5, y + 1, 78, 66, 0.96F);
            } else {
                Species species = findSpecies(slot.speciesId);
                FormData form = findForm(species, slot.formId);
                if (species != null && form != null) {
                    addPokemonModel(species, formAspects(form), x + 5, y + 1, 78, 66, 0.96F);
                }
            }
        }
    }

    private void addDraftModels(int x, int y) {
        for (int i = 0; i < draft.size(); i++) {
            DraftSlot slot = draft.get(i);
            if (slot.pokemon != null) {
                addPokemonModel(slot.pokemon, cardX(x, i) + 5, cardY(y, i) + 1, 78, 66, 0.96F);
            }
            else {
                Species species = findSpecies(slot.speciesId);
                FormData form = findForm(species, slot.formId);
                if (species != null && form != null) {
                    addPokemonModel(species, formAspects(form),
                            cardX(x, i) + 5, cardY(y, i) + 1, 78, 66, 0.96F);
                }
            }
        }
    }

    private void addPokemonModel(Pokemon pokemon, int x, int y, int width, int height, float scale) {
        RenderablePokemon renderable = new RenderablePokemon(pokemon.getSpecies(), pokemon.getAspects(), ItemStack.EMPTY);
        addDrawableChild(createModelWidget(x, y, width, height, renderable,
                scale, 35.0F, 1.0, false, false));
    }

    private void addPokemonModel(Species species, Set<String> aspects, int x, int y,
                                 int width, int height, float scale) {
        RenderablePokemon renderable = new RenderablePokemon(species, aspects, ItemStack.EMPTY);
        addDrawableChild(createModelWidget(x, y, width, height, renderable,
                scale, 35.0F, 1.0, false, false));
    }

    private static ModelWidget createModelWidget(int x, int y, int width, int height,
                                                 RenderablePokemon pokemon, float scale,
                                                 float rotation, double offset,
                                                 boolean playCry, boolean followCursor) {
        try {
            for (var constructor : ModelWidget.class.getConstructors()) {
                if (constructor.getParameterCount() == 11) {
                    return (ModelWidget) constructor.newInstance(x, y, width, height, pokemon,
                            scale, rotation, offset, playCry, followCursor, 15);
                }
                if (constructor.getParameterCount() == 10) {
                    ModelWidget widget = (ModelWidget) constructor.newInstance(
                            x, y, width, height, pokemon, scale, rotation, offset, playCry, followCursor);
                    try {
                        ModelWidget.Companion.getClass().getMethod("setRender", boolean.class)
                                .invoke(ModelWidget.Companion, true);
                    } catch (NoSuchMethodException ignored) {
                        // Cobblemon 1.8 renders each widget independently.
                    }
                    return widget;
                }
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("API ModelWidget Cobblemon non compatible", exception);
        }
        throw new IllegalStateException("Constructeur ModelWidget Cobblemon non reconnu");
    }

    private RenderablePokemon placeholderRenderable() {
        CobblemonCatalogueCache.Entry entry = catalogue.snapshot().entries().get(0);
        return new RenderablePokemon(entry.species(), formAspects(entry.form()), ItemStack.EMPTY);
    }

    private void refreshPokemonPickerModels() {
        if (!pokemonPickerOpen || pokemonPickerModels.isEmpty()) return;
        int start = pokemonPickerPage * pokemonPickerRows();
        for (int row = 0; row < pokemonPickerModels.size(); row++) {
            ModelWidget widget = pokemonPickerModels.get(row);
            int index = start + row;
            widget.visible = index < filteredPokemonChoices.size();
            if (!widget.visible) continue;
            PokemonChoice choice = filteredPokemonChoices.get(index);
            RenderIdentity identity = choice.pokemon != null
                    ? new RenderIdentity(choice.pokemon.getSpecies(), Set.copyOf(choice.pokemon.getAspects()), ClientDataRevision.catalogue())
                    : new RenderIdentity(choice.species, Set.copyOf(formAspects(choice.form)), ClientDataRevision.catalogue());
            if (!identity.equals(pickerModelIdentities.get(widget))) {
                widget.setPokemon(new RenderablePokemon(identity.species, identity.aspects, ItemStack.EMPTY));
                pickerModelIdentities.put(widget, identity);
            }
        }
    }

    private Pokemon findPokemon(String rawId) {
        UUID id = parseUuid(rawId);
        OwnedPokemonIndex.Entry entry = ownedPokemon.byUuid(id);
        return entry == null ? null : entry.pokemon();
    }

    private Species findSpecies(String rawId) {
        return catalogue.snapshot().species(rawId);
    }

    private FormData findForm(Species species, String rawFormId) {
        if (species == null) return null;
        String formId = TeamModels.canonicalFormId(rawFormId);
        if (formId == null) return species.getStandardForm();
        FormData form = species.getFormByName(formId);
        if (form == null) form = species.getFormByShowdownId(formId);
        return form == null ? species.getStandardForm() : form;
    }

    private String formId(Pokemon pokemon) {
        return pokemon == null ? null : formId(pokemon.getForm(), pokemon.getSpecies());
    }

    private String formId(FormData form, Species species) {
        if (form == null || species == null || form == species.getStandardForm()) return null;
        return TeamModels.canonicalFormId(form.getName());
    }

    private Set<String> formAspects(FormData form) {
        return form == null ? Set.of() : new LinkedHashSet<>(form.getAspects());
    }

    private String choiceDisplayName(PokemonChoice choice) {
        if (choice == null) return uiText("not_found");
        String base = speciesName(choice.species);
        String form = formId(choice.form, choice.species);
        return form == null ? base : base + " " + form;
    }

    private String pokemonNickname(PokemonChoice choice) {
        if (choice == null || choice.pokemon == null || choice.pokemon.getNickname() == null) return "";
        return choice.pokemon.getNickname().getString().strip();
    }

    private String pokemonChoiceListName(PokemonChoice choice) {
        String nickname = pokemonNickname(choice);
        return nickname.isBlank() ? choiceDisplayName(choice) : nickname;
    }

    private String plannedSpeciesName(String rawSpeciesId, String rawFormId) {
        Species species = findSpecies(rawSpeciesId);
        if (species == null) return uiText("not_found");
        FormData form = findForm(species, rawFormId);
        String formName = formId(form, species);
        return formName == null ? speciesName(species) : speciesName(species) + " " + formName;
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

    private Map<String, Integer> pokemonEvs(Pokemon pokemon) {
        if (pokemon == null) return Map.of();
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("hp", pokemon.getEvs().getOrDefault(Stats.HP));
        result.put("atk", pokemon.getEvs().getOrDefault(Stats.ATTACK));
        result.put("def", pokemon.getEvs().getOrDefault(Stats.DEFENCE));
        result.put("spa", pokemon.getEvs().getOrDefault(Stats.SPECIAL_ATTACK));
        result.put("spd", pokemon.getEvs().getOrDefault(Stats.SPECIAL_DEFENCE));
        result.put("spe", pokemon.getEvs().getOrDefault(Stats.SPEED));
        result.values().removeIf(value -> value == null || value <= 0);
        return result;
    }

    private MoveCheck moveCheck(DraftSlot slot, String moveId) {
        if (slot == null || moveId == null || moveId.isBlank()) return MoveCheck.UNKNOWN;
        if (slot.pokemon == null) return MoveCheck.UNKNOWN;
        if (activeMoveIds(slot.pokemon).contains(moveId)) return MoveCheck.AVAILABLE;
        for (BenchedMove move : slot.pokemon.getBenchedMoves()) {
            if (move.getMoveTemplate().getName().equals(moveId)) return MoveCheck.AVAILABLE;
        }
        return MoveCheck.MISSING;
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
    public void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
        if (pokemonPickerOpen) {
            int pickerLeft = pickerLeft();
            int pickerTop = pickerTop();
            renderPokemonPickerBase(context, mouseX, mouseY, pickerLeft, pickerTop);
            renderWidgets(context, mouseX, mouseY, delta);
            renderPokemonPickerOverlay(context, mouseX, mouseY, pickerLeft, pickerTop);
            return;
        }
        int left = left();
        int top = top();
        drawMainPcFrame(context, left, top);
        context.fill(left + 4, top + 25, left + 76, top + PANEL_HEIGHT - 19, 0xE52B3539);
        context.fill(left + 5, top + 26, left + 75, top + 27, 0x665F6A6E);
        context.fill(left + 75, top + 27, left + 76, top + PANEL_HEIGHT - 20, 0xAA171C1E);
        context.drawTexture(activePcWallpaper(), left + SCREEN_X, top + 25, SCREEN_WIDTH, SCREEN_HEIGHT,
                0, 0, 174, 155, 174, 155);
        drawMainScreenOverlay(context, left + SCREEN_X, top + 25);
        context.fill(left + PARTY_X + 4, top + 24, left + PARTY_X + 78,
                top + PANEL_HEIGHT - 17, 0xFF343A3D);
        context.fill(left + PARTY_X + 5, top + 25, left + PARTY_X + 77, top + 26, 0xFF626A6D);
        context.fill(left + PARTY_X + 5, top + PANEL_HEIGHT - 18,
                left + PARTY_X + 77, top + PANEL_HEIGHT - 17, 0xFF1C2022);
        if (!movePickerOpen) {
            context.drawCenteredTextWithShadow(textRenderer, title, left + SCREEN_CENTER_X,
                    top + 16, 0xFFFFFF);
        }

        if (movePickerOpen) {
            renderWidgets(context, mouseX, mouseY, delta);
            renderMovePicker(context, mouseX, mouseY, left, top);
            return;
        }

        if (itemPickerOpen) {
            renderWidgets(context, mouseX, mouseY, delta);
            renderItemPicker(context, mouseX, mouseY, left, top);
            return;
        }

        if (setEditorOpen) {
            renderSetEditor(context, mouseX, mouseY, left, top);
            renderWidgets(context, mouseX, mouseY, delta);
            return;
        }

        if (teamDoctorOpen) {
            renderWidgets(context, mouseX, mouseY, delta);
            renderTeamDoctor(context, mouseX, mouseY, left, top);
            return;
        }

        if (creating) renderEditor(context, left, top, mouseX, mouseY);
        else renderBrowser(context, left, top, mouseX, mouseY);
        renderStatus(context, left, top);
        renderWidgets(context, mouseX, mouseY, delta);

        if (creating) renderDraftCardOverlay(context, left, top, mouseX, mouseY);
        else if (!data.teams.isEmpty()) renderBrowserCardOverlay(context, selectedTeam(), left, top, mouseX, mouseY);
        if (draftDragging) renderDraftSlotDrag(context);
        else if (previewDragging) renderPreviewSlotDrag(context);
        else if (teamDrag.team != null) renderTeamDrag(context);
        else renderHoverTooltip(context, mouseX, mouseY);
    }

    private void renderDraftSlotDrag(DrawContext context) {
        if (draggedDraftSlot < 0 || draggedDraftSlot >= draft.size()) return;
        renderSlotDrag(context, displayName(draft.get(draggedDraftSlot)), draftDragX, draftDragY,
                cardIndexAt(draftDragX, draftDragY));
    }

    private void renderPreviewSlotDrag(DrawContext context) {
        if (data.teams.isEmpty() || draggedPreviewSlot < 0
                || draggedPreviewSlot >= selectedTeam().slots.size()) return;
        SavedSlot slot = selectedTeam().slots.get(draggedPreviewSlot);
        renderSlotDrag(context, savedSlotName(slot, findPokemon(slot.pokemonId)), previewDragX, previewDragY,
                previewCardIndexAt(previewDragX, previewDragY));
    }

    private void renderSlotDrag(DrawContext context, String name, double mouseX, double mouseY, int target) {
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 500);
        if (target >= 0) {
            int x = cardX(left() + CARD_START_X, target);
            int y = cardY(top() + CARD_START_Y, target);
            drawOutline(context, x, y, CARD_WIDTH, CARD_HEIGHT, 0xFF9CE8F2);
        }
        int width = 92;
        int x = Math.max(0, Math.min(this.width - width, (int) mouseX + 10));
        int y = Math.max(0, Math.min(this.height - 16, (int) mouseY - 8));
        PcStyleButton.drawFrame(context, x, y, width, 16, PcStyleButton.Style.SELECTED, false, true);
        context.drawTextWithShadow(textRenderer, Text.literal(fitText(name, width - 8)), x + 4, y + 4,
                0xFFFFFFFF);
        context.getMatrices().pop();
    }

    private void renderTeamDrag(DrawContext context) {
        if (!teamDrag.dragging) return;
        int boundary = teamDropBoundary(teamDrag.x, teamDrag.y);
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 500);
        if (boundary >= 0) {
            int y = top() + LIST_START_Y + (boundary - page * PAGE_SIZE) * LIST_ROW_HEIGHT - 2;
            context.fill(left() + LIST_X, y, left() + LIST_X + LEFT_CONTROL_WIDTH, y + 2, 0xFF9CE8F2);
        }
        int x = Math.max(0, Math.min(width - LEFT_CONTROL_WIDTH, (int) teamDrag.x + 10));
        int y = Math.max(0, Math.min(height - 16, (int) teamDrag.y - 8));
        PcStyleButton.drawFrame(context, x, y, LEFT_CONTROL_WIDTH, 16, PcStyleButton.Style.SELECTED, false, true);
        context.drawTextWithShadow(textRenderer, Text.literal(fitText(teamDrag.team.name, LEFT_CONTROL_WIDTH - 8)),
                x + 4, y + 4, 0xFFFFFFFF);
        context.getMatrices().pop();
    }

    private void openTeamDoctor() {
        if (draft.isEmpty()) return;
        teamDoctorOpen = true;
        clearAndInit();
    }

    private void openSetEditor(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= draft.size()) return;
        setEditorSlot = slotIndex;
        setEditorOpen = true;
        stopEvAdjustment();
        clearAndInit();
    }

    private void closeSetEditor() {
        setEditorOpen = false;
        setEditorSlot = -1;
        evInputFields.clear();
        stopEvAdjustment();
        returnToSetEditor = false;
        clearAndInit();
    }

    private void initSetEditorInputs() {
        evInputFields.clear();
        if (setEditorSlot < 0 || setEditorSlot >= draft.size()) return;
        DraftSlot slot = draft.get(setEditorSlot);
        int panelX = left() + (PANEL_WIDTH - 301) / 2;
        int panelY = top() + (PANEL_HEIGHT - 169) / 2;
        for (int index = 0; index < EV_KEYS.size(); index++) {
            String stat = EV_KEYS.get(index);
            int y = panelY + 45 + index * 16;
            TextFieldWidget input = new TextFieldWidget(textRenderer, panelX + 200, y, 40, 14,
                    ui("ev_" + stat));
            input.setMaxLength(3);
            input.setTextPredicate(value -> value.matches("\\d{0,3}"));
            input.setText(String.valueOf(slot.evs.getOrDefault(stat, 0)));
            input.setChangedListener(value -> updateTypedSetEv(stat, value, input));
            input.setTooltip(Tooltip.of(ui("set_ev_input_tooltip")));
            evInputFields.put(stat, input);
            addDrawableChild(input);
        }
    }

    private void renderSetEditor(DrawContext context, int mouseX, int mouseY, int left, int top) {
        if (setEditorSlot < 0 || setEditorSlot >= draft.size()) return;
        DraftSlot slot = draft.get(setEditorSlot);
        int panelX = left + (PANEL_WIDTH - 301) / 2;
        int panelY = top + (PANEL_HEIGHT - 169) / 2;
        int panelWidth = 301;
        int panelHeight = 169;
        context.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xA8000000);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF283238);
        context.fill(panelX + 2, panelY + 2, panelX + panelWidth - 2, panelY + 18, 0xFF3B464B);
        drawOutline(context, panelX, panelY, panelWidth, panelHeight, 0xFF9BA5A9);
        drawOutline(context, panelX + 2, panelY + 2, panelWidth - 4, panelHeight - 4, 0xFF111719);
        context.drawCenteredTextWithShadow(textRenderer, ui("set_editor_title", displayName(slot)),
                panelX + panelWidth / 2, panelY + 6, 0xFFFFFFFF);

        drawSetEditorButton(context, mouseX, mouseY, panelX + 8, panelY + 28, 126, 21,
                uiText("set_item", itemName(slot.itemId)));
        drawSetEditorButton(context, mouseX, mouseY, panelX + 8, panelY + 53, 126, 21,
                uiText("set_ability", abilityName(slot.abilityId).getString()),
                isHiddenAbility(findSpecies(slot.speciesId), slot.formId, slot.abilityId)
                        ? 0xFFFFD15C : 0xFFFFFFFF);
        drawSetEditorButton(context, mouseX, mouseY, panelX + 27, panelY + 78, 88, 21,
                uiText("set_nature", plannedNatureName(slot.natureId).getString()));
        drawSetEditorButton(context, mouseX, mouseY, panelX + 8, panelY + 78, 17, 21, "‹");
        drawSetEditorButton(context, mouseX, mouseY, panelX + 117, panelY + 78, 17, 21, "›");
        drawSetEditorButton(context, mouseX, mouseY, panelX + 8, panelY + 103, 126, 21,
                uiText("set_moves", slot.moveIds.size()));
        drawSetEditorButton(context, mouseX, mouseY, panelX + 8, panelY + 139, 126, 18,
                uiText("set_editor_done"));

        int evTotal = slot.evs.values().stream().mapToInt(Integer::intValue).sum();
        context.drawCenteredTextWithShadow(textRenderer, ui("set_evs", evTotal), panelX + 220, panelY + 28,
                evTotal > 510 ? COLOR_MISSING : 0xFF9CEBFF);
        for (int index = 0; index < EV_KEYS.size(); index++) {
            String stat = EV_KEYS.get(index);
            int y = panelY + 45 + index * 16;
            context.drawTextWithShadow(textRenderer, ui("ev_" + stat), panelX + 151, y + 3, 0xFFB9C9CD);
            drawSetEditorButton(context, mouseX, mouseY, panelX + 180, y, 16, 14, "−");
            drawSetEditorButton(context, mouseX, mouseY, panelX + 244, y, 16, 14, "+");
        }
        drawSetEditorButton(context, mouseX, mouseY, panelX + 180, panelY + 143, 80, 16,
                uiText("set_evs_reset"));
    }

    private void drawSetEditorButton(DrawContext context, int mouseX, int mouseY,
                                     int x, int y, int width, int height, String label) {
        drawSetEditorButton(context, mouseX, mouseY, x, y, width, height, label, 0xFFFFFFFF);
    }

    private void drawSetEditorButton(DrawContext context, int mouseX, int mouseY,
                                     int x, int y, int width, int height, String label, int textColor) {
        boolean hovered = isInside(mouseX, mouseY, x, y, width, height);
        PcStyleButton.drawFrame(context, x, y, width, height, PcStyleButton.Style.NORMAL, hovered, true);
        scissor(context, x + 3, y + 1, x + width - 3, y + height - 1);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(fitText(label, width - 8)),
                x + width / 2, y + Math.max(3, (height - 8) / 2), textColor);
        context.disableScissor();
    }

    private boolean handleSetEditorClick(double mouseX, double mouseY, int button) {
        if (button != 0 || setEditorSlot < 0 || setEditorSlot >= draft.size()) return true;
        int panelX = left() + (PANEL_WIDTH - 301) / 2;
        int panelY = top() + (PANEL_HEIGHT - 169) / 2;
        if (isInside(mouseX, mouseY, panelX + 8, panelY + 28, 126, 21)) {
            int slot = setEditorSlot;
            returnToSetEditor = true;
            setEditorOpen = false;
            openItemPicker(slot);
            return true;
        }
        if (isInside(mouseX, mouseY, panelX + 8, panelY + 53, 126, 21)) {
            cycleSetAbility(1);
            return true;
        }
        if (isInside(mouseX, mouseY, panelX + 8, panelY + 78, 17, 21)) {
            cycleSetNature(-1);
            return true;
        }
        if (isInside(mouseX, mouseY, panelX + 117, panelY + 78, 17, 21)) {
            cycleSetNature(1);
            return true;
        }
        if (isInside(mouseX, mouseY, panelX + 27, panelY + 78, 88, 21)) {
            cycleSetNature(1);
            return true;
        }
        if (isInside(mouseX, mouseY, panelX + 8, panelY + 103, 126, 21)) {
            int slot = setEditorSlot;
            returnToSetEditor = true;
            setEditorOpen = false;
            openMovePicker(slot);
            return true;
        }
        if (isInside(mouseX, mouseY, panelX + 8, panelY + 139, 126, 18)) {
            closeSetEditor();
            return true;
        }
        for (int index = 0; index < EV_KEYS.size(); index++) {
            String stat = EV_KEYS.get(index);
            int y = panelY + 45 + index * 16;
            if (isInside(mouseX, mouseY, panelX + 200, y, 40, 14)) {
                stopEvAdjustment();
                boolean handled = super.clickContent(mouseX, mouseY, button);
                TextFieldWidget input = evInputFields.get(stat);
                if (input != null) {
                    input.setFocused(true);
                    input.setSelectionStart(0);
                    input.setSelectionEnd(input.getText().length());
                }
                return handled || input != null;
            }
            if (isInside(mouseX, mouseY, panelX + 180, y, 16, 14)) {
                beginEvAdjustment(stat, -1);
                return true;
            }
            if (isInside(mouseX, mouseY, panelX + 244, y, 16, 14)) {
                beginEvAdjustment(stat, 1);
                return true;
            }
        }
        if (isInside(mouseX, mouseY, panelX + 180, panelY + 143, 80, 16)) {
            updateSetEvs(Map.of());
            return true;
        }
        if (!isInside(mouseX, mouseY, panelX, panelY, 301, 169)) closeSetEditor();
        return true;
    }

    private void cycleSetAbility(int direction) {
        DraftSlot previous = draft.get(setEditorSlot);
        Species species = findSpecies(previous.speciesId);
        FormData form = findForm(species, previous.formId);
        if (form == null) return;
        List<AbilityChoice> choices = cachedAbilityChoices(form);
        if (choices.isEmpty()) return;
        int current = -1;
        for (int index = 0; index < choices.size(); index++) {
            if (choices.get(index).id.equals(previous.abilityId)) current = index;
        }
        AbilityChoice selected = choices.get(Math.floorMod(current + direction, choices.size()));
        draft.set(setEditorSlot, new DraftSlot(previous.pokemon, previous.pokemonId, previous.speciesId,
                previous.formId, previous.itemId, selected.id, previous.moveIds,
                previous.natureId, previous.evs, previous.source));
        status = uiText("status_ability_selected", abilityName(selected.id).getString());
    }

    private void cycleSetNature(int direction) {
        DraftSlot previous = draft.get(setEditorSlot);
        int current = previous.natureId == null ? -1 : NATURE_IDS.indexOf(previous.natureId);
        String nature = NATURE_IDS.get(Math.floorMod(current + direction, NATURE_IDS.size()));
        draft.set(setEditorSlot, new DraftSlot(previous.pokemon, previous.pokemonId, previous.speciesId,
                previous.formId, previous.itemId, previous.abilityId, previous.moveIds,
                nature, previous.evs, previous.source));
        status = uiText("status_nature_selected", plannedNatureName(nature).getString());
    }

    private void adjustSetEv(String stat, int delta) {
        DraftSlot previous = draft.get(setEditorSlot);
        Map<String, Integer> evs = new LinkedHashMap<>(previous.evs);
        int current = evs.getOrDefault(stat, 0);
        int total = evs.values().stream().mapToInt(Integer::intValue).sum();
        int next = TeamModels.clampEvValue(current, total, current + delta);
        if (next == current) return;
        if (next == 0) evs.remove(stat);
        else evs.put(stat, next);
        updateSetEvs(evs);
    }

    private void updateTypedSetEv(String stat, String rawValue, TextFieldWidget input) {
        if (syncingEvInputFields || setEditorSlot < 0 || setEditorSlot >= draft.size()) return;
        int requested;
        try {
            requested = rawValue.isBlank() ? 0 : Integer.parseInt(rawValue);
        } catch (NumberFormatException ignored) {
            return;
        }
        DraftSlot previous = draft.get(setEditorSlot);
        Map<String, Integer> evs = new LinkedHashMap<>(previous.evs);
        int current = evs.getOrDefault(stat, 0);
        int total = evs.values().stream().mapToInt(Integer::intValue).sum();
        int accepted = TeamModels.clampEvValue(current, total, requested);
        if (accepted == 0) evs.remove(stat);
        else evs.put(stat, accepted);
        updateSetEvs(evs);
        if (accepted != requested || rawValue.isBlank()) {
            syncingEvInputFields = true;
            input.setText(String.valueOf(accepted));
            input.setCursorToEnd(false);
            syncingEvInputFields = false;
        }
    }

    private void beginEvAdjustment(String stat, int direction) {
        clearEvInputFocus();
        heldEvStat = stat;
        heldEvDirection = Integer.signum(direction);
        heldEvTicks = 0;
        adjustSetEv(stat, heldEvDirection * 4);
    }

    private void stopEvAdjustment() {
        heldEvStat = null;
        heldEvDirection = 0;
        heldEvTicks = 0;
    }

    private void clearEvInputFocus() {
        for (TextFieldWidget input : evInputFields.values()) input.setFocused(false);
    }

    private void syncEvInputFields() {
        if (syncingEvInputFields || setEditorSlot < 0 || setEditorSlot >= draft.size()) return;
        syncingEvInputFields = true;
        DraftSlot slot = draft.get(setEditorSlot);
        for (String stat : EV_KEYS) {
            TextFieldWidget input = evInputFields.get(stat);
            if (input == null) continue;
            String value = String.valueOf(slot.evs.getOrDefault(stat, 0));
            if (!input.getText().equals(value)) input.setText(value);
        }
        syncingEvInputFields = false;
    }

    private void updateSetEvs(Map<String, Integer> evs) {
        DraftSlot previous = draft.get(setEditorSlot);
        draft.set(setEditorSlot, new DraftSlot(previous.pokemon, previous.pokemonId, previous.speciesId,
                previous.formId, previous.itemId, previous.abilityId, previous.moveIds,
                previous.natureId, evs, previous.source));
        status = uiText("status_evs_updated");
        statusError = false;
        syncEvInputFields();
    }

    private void closeTeamDoctor() {
        teamDoctorOpen = false;
        clearAndInit();
    }

    private void renderTeamDoctor(DrawContext context, int mouseX, int mouseY, int left, int top) {
        int panelX = left + (PANEL_WIDTH - 385) / 2;
        int panelY = top + (PANEL_HEIGHT - 207) / 2;
        int panelWidth = 385;
        int panelHeight = 207;
        context.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xA8000000);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF283238);
        context.fill(panelX + 2, panelY + 2, panelX + panelWidth - 2, panelY + 18, 0xFF3B464B);
        drawOutline(context, panelX, panelY, panelWidth, panelHeight, 0xFF9BA5A9);
        drawOutline(context, panelX + 2, panelY + 2, panelWidth - 4, panelHeight - 4, 0xFF111719);
        context.drawCenteredTextWithShadow(textRenderer, ui("team_doctor_title"),
                panelX + panelWidth / 2, panelY + 6, 0xFFFFFFFF);

        List<TeamDoctor.Finding> findings = doctorFindings;
        int rowY = panelY + 28;
        for (int index = 0; index < Math.min(9, findings.size()); index++) {
            TeamDoctor.Finding finding = findings.get(index);
            int color = finding.severity() >= 2 ? COLOR_MISSING
                    : finding.severity() == 1 ? COLOR_ITEM : COLOR_OK;
            context.fill(panelX + 10, rowY + 2, panelX + 13, rowY + 11, color);
            String findingType = teamDoctorFindingType(finding);
            int textX = panelX + 19;
            if (!findingType.isBlank()) {
                drawDoctorTypeIcon(context, findingType, textX, rowY - 1, 14);
                textX += 18;
            }
            drawMarqueeText(context, teamDoctorText(finding), textX, rowY + 2,
                    panelX + panelWidth - 10 - textX, color);
            rowY += 17;
        }
        context.drawCenteredTextWithShadow(textRenderer, ui("team_doctor_close"),
                panelX + panelWidth / 2, panelY + panelHeight - 15, 0xFFA7C6CD);
    }

    private List<TeamDoctor.Member> teamDoctorMembers() {
        List<TeamDoctor.Member> result = new ArrayList<>();
        for (DraftSlot slot : draft) {
            Species species = findSpecies(slot.speciesId);
            FormData form = findForm(species, slot.formId);
            if (form == null) continue;
            Set<String> types = new LinkedHashSet<>();
            types.add(form.getPrimaryType().getName());
            if (form.getSecondaryType() != null) types.add(form.getSecondaryType().getName());
            int attack = form.getBaseStats().getOrDefault(Stats.ATTACK, 0);
            int specialAttack = form.getBaseStats().getOrDefault(Stats.SPECIAL_ATTACK, 0);
            String offence = attack >= specialAttack + 15 ? "physical"
                    : specialAttack >= attack + 15 ? "special" : "mixed";
            List<CompetitiveSetCoherence.MoveInfo> moveInfo = new ArrayList<>();
            for (String moveId : slot.moveIds) {
                MoveTemplate move = com.cobblemon.mod.common.api.moves.Moves.getByName(moveId);
                if (move == null) continue;
                moveInfo.add(new CompetitiveSetCoherence.MoveInfo(move.getName(),
                        move.getDamageCategory().getName(), move.getPower() > 0.0D));
            }
            CobblemonCatalogueCache.Entry cached = catalogue.snapshot().entry(form);
            Set<String> legalMoves = cached == null ? Set.of() : cached.legalMoveIds();
            result.add(new TeamDoctor.Member(types, offence,
                    form.getBaseStats().getOrDefault(Stats.SPEED, 0), slot.moveIds,
                    slot.speciesId, slot.abilityId, displayName(slot), slot.itemId,
                    slot.natureId, slot.evs, moveInfo, legalMoves));
        }
        return result;
    }

    private Text teamDoctorText(TeamDoctor.Finding finding) {
        return switch (finding.code()) {
            case INCOMPLETE -> ui("doctor_incomplete", finding.detail());
            case MONOTYPE -> ui("doctor_monotype", Text.translatable("cobblemon.type." + finding.detail()));
            case TYPE_STACK -> {
                String[] parts = finding.detail().split(":", 2);
                yield ui("doctor_type_stack_icon", parts.length > 1 ? parts[1] : "3");
            }
            case SHARED_WEAKNESS -> {
                String[] parts = finding.detail().split(":", 2);
                yield ui("doctor_shared_weakness_icon", parts.length > 1 ? parts[1] : "3");
            }
            case NO_HAZARDS -> ui("doctor_no_hazards");
            case NO_REMOVAL -> ui("doctor_no_removal");
            case HAZARD_SETTERS -> ui("doctor_hazard_setters", finding.detail());
            case HAZARD_CONTROLLERS -> ui("doctor_hazard_controllers", finding.detail());
            case SUGGEST_HAZARD_MOVE -> doctorMoveSuggestion(finding.detail(), "doctor_suggest_hazard");
            case SUGGEST_REMOVAL_MOVE -> doctorMoveSuggestion(finding.detail(), "doctor_suggest_removal");
            case SET_CONFLICT -> doctorSetConflict(finding.detail());
            case STYLE_TRICK_ROOM -> ui("doctor_style_trick_room", finding.detail());
            case STYLE_WEATHER -> doctorWeatherText(finding.detail());
            case STYLE_AURORA_VEIL -> ui("doctor_style_aurora_veil");
            case STYLE_RECOVERY -> {
                String[] parts = finding.detail().split("\\|", 2);
                yield ui("doctor_style_recovery", parts[0], parts.length > 1 ? parts[1] : "1");
            }
            case STYLE_PIVOT -> ui("doctor_style_pivot");
            case STYLE_WIN_CONDITION -> ui("doctor_style_win_condition");
            case LOW_SPEED -> ui("doctor_low_speed", finding.detail());
            case NO_PHYSICAL -> ui("doctor_no_physical");
            case NO_SPECIAL -> ui("doctor_no_special");
            case HEALTHY -> ui("doctor_healthy");
        };
    }

    private Text doctorMoveSuggestion(String detail, String key) {
        String[] parts = detail.split("\\|", 2);
        String pokemon = parts.length > 0 ? parts[0] : "Pokémon";
        String moveId = parts.length > 1 ? parts[1] : "";
        MoveTemplate move = com.cobblemon.mod.common.api.moves.Moves.getByName(moveId);
        Text moveName = move == null ? Text.literal(moveId) : move.getDisplayName();
        return ui(key, pokemon, moveName);
    }

    private Text doctorSetConflict(String detail) {
        String[] parts = detail.split("\\|", 2);
        String pokemon = parts.length > 0 ? parts[0] : "Pokémon";
        String issue = parts.length > 1 ? parts[1].toLowerCase(Locale.ROOT) : "generic";
        String key = switch (issue) {
            case "assault_vest_with_status" -> "doctor_issue_assault_vest";
            case "loaded_dice_without_multi_hit" -> "doctor_issue_loaded_dice";
            case "choice_band_without_physical" -> "doctor_issue_choice_band";
            case "choice_specs_without_special" -> "doctor_issue_choice_specs";
            case "belly_drum_without_physical" -> "doctor_issue_belly_drum";
            case "physical_plan_without_attack" -> "doctor_issue_physical";
            case "special_plan_without_attack" -> "doctor_issue_special";
            default -> "doctor_issue_generic";
        };
        return ui(key, pokemon);
    }

    private Text doctorWeatherText(String detail) {
        String[] parts = detail.split("\\|", 3);
        String style = parts.length > 0 ? parts[0].toLowerCase(Locale.ROOT) : "balanced";
        Text styleName = ui("ranked_style_" + style);
        return ui("doctor_style_weather", styleName,
                parts.length > 1 ? parts[1] : "0", parts.length > 2 ? parts[2] : "0");
    }

    private String teamDoctorFindingType(TeamDoctor.Finding finding) {
        if (finding.code() != TeamDoctor.Code.TYPE_STACK
                && finding.code() != TeamDoctor.Code.SHARED_WEAKNESS) return "";
        return finding.detail().split(":", 2)[0];
    }

    private void drawDoctorTypeIcon(DrawContext context, String typeName, int x, int y, int size) {
        ElementalType type = ElementalTypes.get(typeName);
        if (type == null) return;
        int textureX = type.getTextureXMultiplier() * 36;
        context.drawTexture(TYPE_ICONS, x, y, size, size,
                textureX, 0, 36, 36, 648, 36);
    }

    private void renderMovePicker(DrawContext context, int mouseX, int mouseY, int left, int top) {
        int panelX = left + (PANEL_WIDTH - MOVE_PICKER_WIDTH) / 2;
        int panelY = top + (PANEL_HEIGHT - MOVE_PICKER_HEIGHT) / 2;
        int panelWidth = MOVE_PICKER_WIDTH;
        int panelHeight = MOVE_PICKER_HEIGHT;
        context.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0x98000000);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF283238);
        context.fill(panelX + 2, panelY + 2, panelX + panelWidth - 2, panelY + 21, 0xFF3B464B);
        drawOutline(context, panelX, panelY, panelWidth, panelHeight, 0xFF9BA5A9);
        drawOutline(context, panelX + 2, panelY + 2, panelWidth - 4, panelHeight - 4, 0xFF111719);
        DraftSlot slot = movePickerSlot >= 0 && movePickerSlot < draft.size() ? draft.get(movePickerSlot) : null;
        context.drawCenteredTextWithShadow(textRenderer,
                slot == null ? ui("move_picker_title") : ui("move_picker_title_pokemon", displayName(slot)),
                panelX + panelWidth / 2, panelY + 7, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, ui("move_picker_preset"), panelX + 9, panelY + 26, 0xFFA7D6E2);
        context.drawTextWithShadow(textRenderer, ui("move_picker_legal"),
                panelX + MOVE_PICKER_LIST_X + 2, panelY + 26, 0xFFA7D6E2);

        if (slot != null) {
            for (int i = 0; i < 4; i++) {
                int x = panelX + 8;
                int y = panelY + 36 + i * 25;
                boolean hovered = isInside(mouseX, mouseY, x, y, MOVE_PICKER_PRESET_WIDTH, 22);
                boolean selected = movePickerSelectedIndex == i;
                context.fill(x, y, x + MOVE_PICKER_PRESET_WIDTH, y + 22,
                        selected ? 0xFF356D7A : hovered ? 0xFF3C5158 : 0xFF1A252A);
                drawOutline(context, x, y, MOVE_PICKER_PRESET_WIDTH, 22,
                        selected ? 0xFFA7F1FF : 0xFF607980);
                String moveId = i < slot.moveIds.size() ? slot.moveIds.get(i) : null;
                MoveTemplate move = moveId == null ? null : com.cobblemon.mod.common.api.moves.Moves.getByName(moveId);
                context.drawTextWithShadow(textRenderer, Text.literal(String.valueOf(i + 1)), x + 5, y + 7, 0xFF8ECED9);
                context.drawTextWithShadow(textRenderer,
                        move == null ? ui("move_picker_empty") : move.getDisplayName(), x + 19, y + 7,
                        move == null ? 0xFF87989D : 0xFFFFFFFF);
                if (move != null) {
                    MoveCheck check = moveCheck(slot, moveId);
                    String symbol = check == MoveCheck.UNKNOWN ? "?" : check == MoveCheck.AVAILABLE ? "✓" : "×";
                    int checkColor = check == MoveCheck.UNKNOWN ? 0xFF72C9EA
                            : check == MoveCheck.AVAILABLE ? COLOR_OK : COLOR_MISSING;
                    context.drawTextWithShadow(textRenderer, Text.literal(symbol),
                            x + MOVE_PICKER_PRESET_WIDTH - 12, y + 7, checkColor);
                }
            }
        }

        int start = movePickerPage * MOVE_PICKER_ROWS;
        for (int row = 0; row < MOVE_PICKER_ROWS; row++) {
            int index = start + row;
            if (index >= moveChoices.size()) break;
            MoveChoice choice = moveChoices.get(index);
            int x = panelX + MOVE_PICKER_LIST_X;
            int y = panelY + 36 + row * 20;
            boolean hovered = isInside(mouseX, mouseY, x, y, MOVE_PICKER_LIST_WIDTH, 18);
            context.fill(x, y, x + MOVE_PICKER_LIST_WIDTH, y + 18,
                    hovered ? 0xFF3F6F79 : 0xFF1A252A);
            drawOutline(context, x, y, MOVE_PICKER_LIST_WIDTH, 18,
                    hovered ? 0xFFB7F5FF : 0xFF607980);
            context.fill(x + 2, y + 2, x + 5, y + 16, typeColor(choice.template.getElementalType().getName()));
            context.drawText(textRenderer, Text.literal(fitText(choice.template.getDisplayName().getString(), 136)),
                    x + 9, y + 5, 0xFFFFFFFF, false);
            String info = switch (choice.source) {
                case ACTIVE -> uiText("move_active");
                case RESERVE -> uiText("move_benched");
                case CATALOGUE -> uiText("move_legal");
            };
            int badgeX = x + MOVE_PICKER_LIST_WIDTH - 59;
            int badgeColor = choice.source == MoveSource.ACTIVE ? 0xFF245D46
                    : choice.source == MoveSource.RESERVE ? 0xFF66531D : 0xFF24566A;
            int badgeBorder = choice.source == MoveSource.ACTIVE ? 0xFF57E391
                    : choice.source == MoveSource.RESERVE ? 0xFFFFD15C : 0xFF72C9EA;
            context.fill(badgeX, y + 3, badgeX + 57, y + 15, badgeColor);
            drawOutline(context, badgeX, y + 3, 57, 12, badgeBorder);
            drawCenteredPlainText(context, Text.literal(fitText(info, 53)), badgeX + 28, y + 5,
                    choice.source == MoveSource.ACTIVE ? 0xFFB8FFD4
                            : choice.source == MoveSource.RESERVE ? 0xFFFFE59A : 0xFFBFEFFF);
        }

        context.drawCenteredTextWithShadow(textRenderer,
                ui("move_picker_page", movePickerPage + 1, movePickerPages()),
                panelX + MOVE_PICKER_LIST_X + MOVE_PICKER_LIST_WIDTH / 2,
                panelY + panelHeight - 11, 0xFF91AEB5);
        context.drawCenteredTextWithShadow(textRenderer, ui("move_picker_close"),
                panelX + 8 + MOVE_PICKER_PRESET_WIDTH / 2,
                panelY + panelHeight - 17, 0xFF91AEB5);
    }

    private void renderPokemonPickerBase(DrawContext context, int mouseX, int mouseY, int left, int top) {
        int panelX = left + 5;
        int panelY = top + 24;
        int panelWidth = pickerWidth() - 10;
        int panelHeight = pickerHeight() - 48;
        int extraWidth = pickerWidth() - PCGUI.BASE_WIDTH;
        int statStart = panelX + 231 + extraWidth;
        drawExpandedPcFrame(context, left, top, pickerWidth(), pickerHeight());
        int headerTextY = top + 16;
        drawScaledCenteredTextWithShadow(context, title,
                left + pickerWidth() / 2, headerTextY, 0xFFFFFFFF, 0.82F);
        drawScaledRightAlignedTextWithShadow(context,
                ui("pokemon_picker_page_compact", pokemonPickerPage + 1, pokemonPickerPages()),
                left + pickerWidth() - 6, headerTextY, 0xFFD8F5FA, 0.78F);
        context.fill(panelX, panelY, panelX + panelWidth, top + pickerHeight() - 24, 0xFFE7EDF3);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 2, 0xFF8B969E);
        context.fill(panelX, top + 46, panelX + panelWidth, top + 59, 0xFFD1DAE3);
        int sortedStat = pokemonPickerSortStat == null ? -1 : DISPLAY_STATS.indexOf(pokemonPickerSortStat);
        if (sortedStat >= 0) {
            int statX = statStart + sortedStat * 18;
            context.fill(statX, top + 46, statX + 18, top + 59, 0xFFB8DFE9);
            int markerY = pokemonPickerSortDescending ? top + 57 : top + 46;
            context.fill(statX, markerY, statX + 18, markerY + 2, 0xFF147D96);
        }
        drawOutline(context, panelX, panelY, panelWidth, panelHeight, 0xFF20282D);

        int rows = pokemonPickerRows();
        int start = pokemonPickerPage * rows;
        for (int row = 0; row < rows; row++) {
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

    private void drawMainPcFrame(DrawContext context, int x, int y) {
        int leftWidth = SCREEN_X;
        int sourceMiddleWidth = 177;
        int targetMiddleWidth = PARTY_X - SCREEN_X;
        int rightWidth = 86;
        int topHeight = 25;
        int sourceMiddleHeight = 156;
        int targetMiddleHeight = PANEL_HEIGHT - 49;
        int bottomHeight = 24;

        drawFrameSlice(context, x, y, leftWidth, topHeight,
                0, 0, leftWidth, topHeight);
        drawFrameSlice(context, x + leftWidth, y, targetMiddleWidth, topHeight,
                leftWidth, 0, sourceMiddleWidth, topHeight);
        drawFrameSlice(context, x + PARTY_X, y, rightWidth, topHeight,
                263, 0, rightWidth, topHeight);

        drawFrameSlice(context, x, y + topHeight, leftWidth, targetMiddleHeight,
                0, topHeight, leftWidth, sourceMiddleHeight);
        drawFrameSlice(context, x + leftWidth, y + topHeight, targetMiddleWidth, targetMiddleHeight,
                leftWidth, topHeight, sourceMiddleWidth, sourceMiddleHeight);
        drawFrameSlice(context, x + PARTY_X, y + topHeight, rightWidth, targetMiddleHeight,
                263, topHeight, rightWidth, sourceMiddleHeight);

        drawFrameSlice(context, x, y + PANEL_HEIGHT - bottomHeight, leftWidth, bottomHeight,
                0, 181, leftWidth, bottomHeight);
        drawFrameSlice(context, x + leftWidth, y + PANEL_HEIGHT - bottomHeight,
                targetMiddleWidth, bottomHeight, leftWidth, 181, sourceMiddleWidth, bottomHeight);
        drawFrameSlice(context, x + PARTY_X, y + PANEL_HEIGHT - bottomHeight,
                rightWidth, bottomHeight, 263, 181, rightWidth, bottomHeight);
    }

    private void drawMainScreenOverlay(DrawContext context, int x, int y) {
        int edge = 8;
        int middleWidth = SCREEN_WIDTH - edge * 2;
        int middleHeight = SCREEN_HEIGHT - edge * 2;
        int sourceMiddleWidth = 174 - edge * 2;
        int sourceMiddleHeight = 155 - edge * 2;

        drawTextureSlice(context, SCREEN_OVERLAY, x, y, edge, edge,
                0, 0, edge, edge, 174, 155);
        drawTextureSlice(context, SCREEN_OVERLAY, x + edge, y, middleWidth, edge,
                edge, 0, sourceMiddleWidth, edge, 174, 155);
        drawTextureSlice(context, SCREEN_OVERLAY, x + SCREEN_WIDTH - edge, y, edge, edge,
                174 - edge, 0, edge, edge, 174, 155);
        drawTextureSlice(context, SCREEN_OVERLAY, x, y + edge, edge, middleHeight,
                0, edge, edge, sourceMiddleHeight, 174, 155);
        drawTextureSlice(context, SCREEN_OVERLAY, x + edge, y + edge, middleWidth, middleHeight,
                edge, edge, sourceMiddleWidth, sourceMiddleHeight, 174, 155);
        drawTextureSlice(context, SCREEN_OVERLAY, x + SCREEN_WIDTH - edge, y + edge, edge, middleHeight,
                174 - edge, edge, edge, sourceMiddleHeight, 174, 155);
        drawTextureSlice(context, SCREEN_OVERLAY, x, y + SCREEN_HEIGHT - edge, edge, edge,
                0, 155 - edge, edge, edge, 174, 155);
        drawTextureSlice(context, SCREEN_OVERLAY, x + edge, y + SCREEN_HEIGHT - edge, middleWidth, edge,
                edge, 155 - edge, sourceMiddleWidth, edge, 174, 155);
        drawTextureSlice(context, SCREEN_OVERLAY, x + SCREEN_WIDTH - edge,
                y + SCREEN_HEIGHT - edge, edge, edge,
                174 - edge, 155 - edge, edge, edge, 174, 155);
    }

    private void drawTextureSlice(DrawContext context, Identifier texture, int x, int y, int width, int height,
                                  int sourceX, int sourceY, int sourceWidth, int sourceHeight,
                                  int textureWidth, int textureHeight) {
        context.drawTexture(texture, x, y, width, height, sourceX, sourceY,
                sourceWidth, sourceHeight, textureWidth, textureHeight);
    }

    private void drawExpandedPcFrame(DrawContext context, int x, int y, int width, int height) {
        int sourceX = 76;
        int sourceY = 0;
        int sourceWidth = 191;
        int sourceHeight = 205;
        int leftSlice = 40;
        int rightSlice = 40;
        int topSlice = 28;
        int bottomSlice = 24;
        int middleWidth = width - leftSlice - rightSlice;
        int middleHeight = height - topSlice - bottomSlice;
        int sourceMiddleWidth = sourceWidth - leftSlice - rightSlice;
        int sourceMiddleHeight = sourceHeight - topSlice - bottomSlice;

        drawFrameSlice(context, x, y, leftSlice, topSlice,
                sourceX, sourceY, leftSlice, topSlice);
        drawFrameSlice(context, x + leftSlice, y, middleWidth, topSlice,
                sourceX + leftSlice, sourceY, sourceMiddleWidth, topSlice);
        drawFrameSlice(context, x + width - rightSlice, y, rightSlice, topSlice,
                sourceX + sourceWidth - rightSlice, sourceY, rightSlice, topSlice);

        drawFrameSlice(context, x, y + topSlice, leftSlice, middleHeight,
                sourceX, sourceY + topSlice, leftSlice, sourceMiddleHeight);
        drawFrameSlice(context, x + leftSlice, y + topSlice, middleWidth, middleHeight,
                sourceX + leftSlice, sourceY + topSlice, sourceMiddleWidth, sourceMiddleHeight);
        drawFrameSlice(context, x + width - rightSlice, y + topSlice, rightSlice, middleHeight,
                sourceX + sourceWidth - rightSlice, sourceY + topSlice, rightSlice, sourceMiddleHeight);

        drawFrameSlice(context, x, y + height - bottomSlice, leftSlice, bottomSlice,
                sourceX, sourceY + sourceHeight - bottomSlice, leftSlice, bottomSlice);
        drawFrameSlice(context, x + leftSlice, y + height - bottomSlice, middleWidth, bottomSlice,
                sourceX + leftSlice, sourceY + sourceHeight - bottomSlice, sourceMiddleWidth, bottomSlice);
        drawFrameSlice(context, x + width - rightSlice, y + height - bottomSlice, rightSlice, bottomSlice,
                sourceX + sourceWidth - rightSlice, sourceY + sourceHeight - bottomSlice, rightSlice, bottomSlice);
    }

    private void drawFrameSlice(DrawContext context, int x, int y, int width, int height,
                                int sourceX, int sourceY, int sourceWidth, int sourceHeight) {
        context.drawTexture(PC_BASE, x, y, width, height, sourceX, sourceY,
                sourceWidth, sourceHeight, 349, 205);
    }

    private void renderPokemonPickerOverlay(DrawContext context, int mouseX, int mouseY, int left, int top) {
        int panelX = left + 5;
        int panelWidth = pickerWidth() - 10;
        int extraWidth = pickerWidth() - PCGUI.BASE_WIDTH;
        int nameWidth = 76 + (extraWidth * 55 / 100);
        int typesX = panelX + 112 + (extraWidth * 55 / 100);
        int abilityX = panelX + 159 + (extraWidth * 55 / 100);
        int statStart = panelX + 240 + extraWidth;
        int abilityWidth = Math.max(70, statStart - abilityX - 10);
        context.drawText(textRenderer, ui("pokemon_picker_name"), panelX + 34, top + 48, 0xFF17242B, false);
        context.drawText(textRenderer, ui("pokemon_picker_types"), typesX, top + 48, 0xFF17242B, false);
        context.drawText(textRenderer, ui("pokemon_picker_ability"), abilityX, top + 48, 0xFF17242B, false);
        for (int i = 0; i < DISPLAY_STATS.size(); i++) {
            boolean selected = DISPLAY_STATS.get(i) == pokemonPickerSortStat;
            drawCenteredPlainText(context, compactStatHeader(DISPLAY_STATS.get(i)),
                    statStart + i * 18, top + 48,
                    selected ? 0xFF07566A : 0xFF17242B);
        }

        int rows = pokemonPickerRows();
        int start = pokemonPickerPage * rows;
        for (int row = 0; row < rows; row++) {
            int index = start + row;
            if (index >= filteredPokemonChoices.size()) break;
            PokemonChoice choice = filteredPokemonChoices.get(index);
            Pokemon pokemon = choice.pokemon;
            Species species = choice.species;
            var primaryType = choice.form.getPrimaryType();
            var secondaryType = choice.form.getSecondaryType();
            int y = top + 59 + row * POKEMON_PICKER_ROW_HEIGHT;
            String nickname = pokemonNickname(choice);
            context.drawText(textRenderer, Text.literal(fitText(pokemonChoiceListName(choice), nameWidth)),
                    panelX + 34, y + 2, nickname.isBlank() ? 0xFF172126 : 0xFF176B7E, false);
            String detail = pokemonChoiceCompactDetail(choice);
            drawMarqueeText(context, Text.literal(detail), panelX + 34, y + 13, nameWidth,
                    choice.owned ? 0xFF455760 : 0xFF27758A);
            drawTypeLabel(context, primaryType.getDisplayName(), primaryType.getName(),
                    typesX, y + 1);
            if (secondaryType != null) {
                drawTypeLabel(context, secondaryType.getDisplayName(), secondaryType.getName(),
                        typesX, y + 12);
            }
            Text ability = pokemon == null ? abilitySummary(choice) : abilityName(pokemon);
            drawMarqueeText(context, ability, abilityX, y + 7, abilityWidth,
                    pokemon == null ? 0xFF07566A : isHiddenAbility(pokemon) ? 0xFFD89B24 : 0xFF07566A);
            for (int stat = 0; stat < DISPLAY_STATS.size(); stat++) {
                int value = pokemonChoiceStatValue(choice, DISPLAY_STATS.get(stat));
                drawCenteredPlainText(context, Text.literal(String.valueOf(value)),
                        statStart + stat * 18, y + 7,
                        pokemon == null ? 0xFF17242B : ivValueColor(value));
            }
        }

        if (filteredPokemonChoices.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, ui("pokemon_picker_empty"),
                    left + pickerWidth() / 2, top + pickerHeight() / 2,
                    0xFF596A73);
        }
        int footerY = top + pickerHeight() - 11;
        context.drawText(textRenderer, Text.literal("‹"), panelX + 8, footerY,
                pokemonPickerPage > 0 ? 0xFF1D7187 : 0xFF9AA5AB, false);
        context.drawText(textRenderer, Text.literal("›"), panelX + panelWidth - 14, footerY,
                pokemonPickerPage + 1 < pokemonPickerPages() ? 0xFF1D7187 : 0xFF9AA5AB, false);
        drawScaledCenteredTextWithShadow(context, ui("pokemon_picker_scroll_hint"),
                left + pickerWidth() / 2, footerY, 0xFFD5E3E7, 0.78F);

        if (pendingAbilityPokemon != null) {
            renderAbilityPicker(context, mouseX, mouseY, left, top);
            return;
        }

        if (rankedSeasonMenuOpen) {
            renderRankedSeasonDropdown(context, mouseX, mouseY, left, top);
            return;
        }

        int hovered = pokemonChoiceIndexAt(mouseX, mouseY);
        if (hovered >= 0 && hovered < filteredPokemonChoices.size()) {
            RankedUsageService.UsageEntry usage = rankedUsage.get(rankedKey(filteredPokemonChoices.get(hovered)));
            if (usage != null) {
                String summary = uiText("pokemon_picker_ranked_footer", rankedSeason, usage.rank,
                        formatRankedPercent(usage.usagePercent), formatRankedPercent(usage.winRate), usage.count);
                drawScaledCenteredTextWithShadow(context,
                        Text.literal(fitText(summary, Math.round((panelWidth - 40) / 0.76F))),
                        left + pickerWidth() / 2, top + pickerHeight() - 21, 0xFFBFEFFF, 0.76F);
            }
        }
        if (hovered >= 0 && hovered < filteredPokemonChoices.size()) {
            PokemonChoice choice = filteredPokemonChoices.get(hovered);
            Pokemon pokemon = choice.pokemon;
            List<Text> lines = new ArrayList<>();
            String nickname = pokemonNickname(choice);
            if (!nickname.isBlank()) {
                lines.add(Text.literal(nickname).styled(style -> style.withColor(0x38B8D0)));
            }
            lines.add(Text.literal(choiceDisplayName(choice)));
            lines.add(Text.literal(pokemon == null ? choice.source : choice.source + " · Nv." + pokemon.getLevel()));
            RankedUsageService.UsageEntry usage = rankedUsage.get(rankedKey(choice));
            if (usage != null) {
                lines.add(ui("pokemon_picker_ranked_summary", rankedSeason, usage.rank));
                lines.add(ui("pokemon_picker_ranked_rates", formatRankedPercent(usage.usagePercent),
                        formatRankedPercent(usage.winRate)));
                lines.add(ui("pokemon_picker_ranked_count", usage.count));
            }
            if (pokemon != null) {
                lines.add(coloredAbilityName(new AbilityChoice(abilityId(pokemon), isHiddenAbility(pokemon))));
                lines.add(ui("pokemon_picker_iv_total", pokemonIvScore(choice), DISPLAY_STATS.size() * 31));
                lines.add(pokemonIvLine(pokemon, 0, 3));
                lines.add(pokemonIvLine(pokemon, 3, 6));
            } else {
                for (AbilityChoice ability : choice.abilities) lines.add(coloredAbilityName(ability));
            }
            lines.add(ui(pokemon == null ? "pokemon_picker_choose_ability" : "pokemon_picker_select"));
            queueTooltip(textRenderer, lines, Optional.empty(), mouseX, mouseY);
        } else {
            int hoveredStat = pokemonStatHeaderIndexAt(mouseX, mouseY);
            if (hoveredStat >= 0) {
                Stat stat = DISPLAY_STATS.get(hoveredStat);
                boolean descending = stat != pokemonPickerSortStat || pokemonPickerSortDescending;
                queueTooltip(textRenderer, List.of(
                        ui(descending ? "pokemon_picker_sort_desc" : "pokemon_picker_sort_asc", statHeader(stat)),
                        ui(pokemonPickerAllSpecies ? "pokemon_picker_stat_base_hint" : "pokemon_picker_stat_iv_hint"),
                        ui("pokemon_picker_sort_hint")), Optional.empty(), mouseX, mouseY);
            }
        }

    }

    private void renderRankedSeasonDropdown(DrawContext context, int mouseX, int mouseY, int left, int top) {
        int x = left + 115;
        int y = top + 45;
        int width = 126;
        int rows = Math.max(1, rankedSeasons.size());
        int height = rows * 14 + 2;
        context.fill(x, y, x + width, y + height, 0xFF20282D);
        drawOutline(context, x, y, width, height, 0xFF9BA5A9);
        if (rankedSeasons.isEmpty()) {
            context.drawText(textRenderer, ui("pokemon_picker_ranked_loading"), x + 5, y + 3,
                    0xFFA7D6E2, false);
            return;
        }
        for (int i = 0; i < rankedSeasons.size(); i++) {
            int rowY = y + 1 + i * 14;
            boolean hovered = isInside(mouseX, mouseY, x + 1, rowY, width - 2, 14);
            boolean selected = rankedSeasons.get(i).equals(rankedSeason);
            int color = selected ? 0xFF2E7180 : hovered ? 0xFF46545B : 0xFF303A3F;
            context.fill(x + 1, rowY, x + width - 1, rowY + 14, color);
            if (selected) context.fill(x + 1, rowY, x + 4, rowY + 14, 0xFF57E391);
            context.drawText(textRenderer, Text.literal(fitText(rankedSeasons.get(i), width - 12)),
                    x + 7, rowY + 3, 0xFFFFFFFF, false);
        }
    }

    private void renderAbilityPicker(DrawContext context, int mouseX, int mouseY, int left, int top) {
        PokemonChoice choice = pendingAbilityPokemon;
        if (choice == null) return;
        int panelX = left + (pickerWidth() - 194) / 2;
        int panelY = top + 48;
        int panelWidth = 194;
        int rowHeight = 22;
        int panelHeight = 42 + choice.abilities.size() * rowHeight;

        context.fill(left + 5, top + 24, left + pickerWidth() - 5,
                top + pickerHeight() - 16, 0xF20D1519);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF28343A);
        context.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + 17, 0xFF3A474D);
        drawOutline(context, panelX, panelY, panelWidth, panelHeight, 0xFFC2D7DC);
        drawCenteredPlainText(context,
                ui("pokemon_picker_ability_for", choiceDisplayName(choice)),
                panelX + panelWidth / 2, panelY + 5, 0xFFFFFFFF);

        for (int i = 0; i < choice.abilities.size(); i++) {
            AbilityChoice ability = choice.abilities.get(i);
            int y = panelY + 21 + i * rowHeight;
            boolean hovered = isInside(mouseX, mouseY, panelX + 7, y, panelWidth - 14, 18);
            PcStyleButton.drawFrame(context, panelX + 7, y, panelWidth - 14, 18,
                    PcStyleButton.Style.NORMAL, hovered, true);
            drawCenteredPlainText(context, abilityName(ability.id), panelX + panelWidth / 2, y + 5,
                    ability.hidden ? 0xFFFFD15C : 0xFFFFFFFF);
        }
        drawCenteredPlainText(context, ui("pokemon_picker_ability_back"), panelX + panelWidth / 2,
                panelY + panelHeight - 14, 0xFFA7C6CD);
    }

    private void openAbilityPicker(PokemonChoice choice) {
        if (choice.abilities.isEmpty()) {
            selectPokemon(choice, null);
            return;
        }
        abilityEditSlot = -1;
        pendingAbilityPokemon = choice;
        clearAndInit();
    }

    private boolean handleAbilityPickerClick(double mouseX, double mouseY) {
        PokemonChoice choice = pendingAbilityPokemon;
        if (choice == null) return true;
        int panelX = pickerLeft() + (pickerWidth() - 194) / 2;
        int panelY = pickerTop() + 48;
        int panelWidth = 194;
        int rowHeight = 22;
        for (int i = 0; i < choice.abilities.size(); i++) {
            int y = panelY + 21 + i * rowHeight;
            if (isInside(mouseX, mouseY, panelX + 7, y, panelWidth - 14, 18)) {
                AbilityChoice ability = choice.abilities.get(i);
                pendingAbilityPokemon = null;
                if (abilityEditSlot >= 0 && abilityEditSlot < draft.size()) {
                    DraftSlot previous = draft.get(abilityEditSlot);
                    draft.set(abilityEditSlot, new DraftSlot(previous.pokemon, previous.pokemonId,
                            previous.speciesId, previous.formId, previous.itemId,
                            ability.id, previous.moveIds, previous.natureId, previous.evs, previous.source));
                    status = uiText("status_ability_selected", abilityName(ability.id));
                    statusError = false;
                    abilityEditSlot = -1;
                    closePokemonPicker(false);
                    clearAndInit();
                } else {
                    selectPokemon(choice, ability.id);
                }
                return true;
            }
        }
        if (abilityEditSlot >= 0) {
            closePokemonPicker(false);
            clearAndInit();
        } else {
            pendingAbilityPokemon = null;
            clearAndInit();
        }
        return true;
    }

    private void renderBrowser(DrawContext context, int left, int top, int mouseX, int mouseY) {
        drawCompactLeftHeader(context, ui("my_teams"), left, top);
        if (data.teams.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, ui("empty"), left + SCREEN_CENTER_X, top + 94, 0xE4F7FB);
            context.drawCenteredTextWithShadow(textRenderer, ui("empty_hint"), left + SCREEN_CENTER_X, top + 110, 0xA7D6E2);
            return;
        }
        context.drawCenteredTextWithShadow(textRenderer, ui("team_page", page + 1, teamPages()),
                left + SIDEBAR_CENTER_X, top + 222, 0xFFA7D6E2);

        SavedTeam team = selectedTeam();
        TeamAnalysis analysis = browserAnalysis;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(trim(team.name, 30)),
                left + SCREEN_CENTER_X, top + 29, 0xFFFFFF);
        for (int i = 0; i < TeamModels.MAX_TEAM_SIZE; i++) {
            drawBrowserCardBase(context, team, i, left, top, mouseX, mouseY);
        }
        if (status.isBlank()) {
            drawStatusMessage(context, Text.literal(browserSummary), left, top,
                    analysis.validation.readyToEquip()
                            ? analysis.itemDifferences > 0 ? COLOR_ITEM : COLOR_OK : COLOR_MISSING);
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
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("?"),
                    x + PREVIEW_CARD_WIDTH / 2, y + 23, COLOR_MISSING);
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
            if (rankedTeamGenerated) {
                boolean locked = rankedLockedSlots.contains(i);
                drawRankedLock(context, x + CARD_WIDTH - 11, y + 3, locked);
                if (!locked) {
                    context.drawTextWithShadow(textRenderer, Text.literal("↻"), x + 4,
                            y + CARD_HEIGHT - 14, 0xFF9CEBFF);
                }
            }
        }
    }

    private void drawRankedLock(DrawContext context, int x, int y, boolean locked) {
        int color = locked ? 0xFFFFD15C : 0xFFB7D8DE;
        int shadow = 0xFF263238;
        context.fill(x + 2, y, x + 7, y + 1, color);
        context.fill(x + 1, y + 1, x + 2, y + 5, color);
        if (locked) context.fill(x + 7, y + 1, x + 8, y + 5, color);
        else context.fill(x + 7, y + 1, x + 8, y + 3, color);
        context.fill(x, y + 4, x + 9, y + 10, shadow);
        drawOutline(context, x, y + 4, 9, 6, color);
        context.fill(x + 4, y + 6, x + 5, y + 9, color);
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
                    Text.literal(trim(plannedSpeciesName(slot.speciesId, slot.formId), 12)),
                    left + PARTY_X + 41, top + 43, 0xFFFFFFFF);
            if (slot.natureId != null) {
                context.drawCenteredTextWithShadow(textRenderer, plannedNatureName(slot.natureId),
                        left + PARTY_X + 41, top + 56, 0xFFBFEFFF);
            }
            context.drawCenteredTextWithShadow(textRenderer, abilityName(slot.abilityId),
                    left + PARTY_X + 41, top + 68,
                    isHiddenAbility(species, slot.formId, slot.abilityId) ? 0xFFFFD15C : 0xFFBFEFFF);
            if (slot.evs != null && !slot.evs.isEmpty()) {
                context.drawTextWrapped(textRenderer, Text.literal(evSummary(slot.evs)),
                        left + PARTY_X + 9, top + 80, 64, 0xFF72C9EA);
            }
            drawMarqueeText(context, ui("replace_required"),
                    left + PARTY_X + 9, top + 131, 64, COLOR_MISSING);
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
        drawEditorRail(context, left, top);
        drawStep(context, left + 8, top + 43, "1", uiText("step_name"), !draftName.isBlank());
        drawStep(context, left + 8, top + 71, "2", uiText("step_pokemon"), !draft.isEmpty());
        drawStep(context, left + 8, top + 99, "3", uiText("step_save"), false);
        if (rankedTeamGenerated || rankedHelperRunning) renderRankedDisclaimer(context, left, top);
        drawDraftCards(context, left + CARD_START_X, top + CARD_START_Y, mouseX, mouseY);
    }

    private void renderRankedDisclaimer(DrawContext context, int left, int top) {
        int centerX = left + 40;
        drawScaledCenteredTextWithShadow(context, ui("ranked_disclaimer_title"),
                centerX, top + 141, COLOR_MISSING, 0.58F);
        drawScaledCenteredTextWithShadow(context, ui("ranked_disclaimer_check"),
                centerX, top + 151, 0xFFFFA0A0, 0.58F);
    }

    private void drawEditorRail(DrawContext context, int left, int top) {
        context.fill(left + 7, top + 34, left + 73, top + 126, 0x443B484D);
        context.fill(left + 8, top + 35, left + 72, top + 36, 0x665D6C71);
        context.fill(left + 8, top + 125, left + 72, top + 126, 0x77171D1F);
        context.fill(left + 16, top + 60, left + 18, top + 72, 0xAA397C8C);
        context.fill(left + 16, top + 88, left + 18, top + 100, 0xAA397C8C);
        context.drawTexture(SUMMARY_SIDE_SPACER, left + 6, top + 128, 70, 7,
                0, 0, 144, 14, 144, 14);
    }

    private void drawStep(DrawContext context, int x, int y, String number, String label, boolean complete) {
        int accent = complete ? COLOR_OK : 0xFF4FA6B8;
        context.drawTexture(SUMMARY_PARTY_SLOT, x, y, 20, 22,
                0, 0, 46, 54, 46, 54);
        context.fill(x + 2, y + 2, x + 18, y + 4, accent);
        context.fill(x + 2, y + 18, x + 18, y + 20, complete ? 0xFF2CBF70 : 0xFF285B66);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(complete ? "✓" : number),
                x + 10, y + 7, complete ? 0xFFFFFFFF : 0xFFE8FBFF);
        context.drawTextWithShadow(textRenderer, Text.literal(label), x + 25, y + 7,
                complete ? 0xFFFFFFFF : 0xFFD5E4E7);
    }

    private void drawCompactLeftHeader(DrawContext context, Text label, int left, int top) {
        float scale = 0.78F;
        float centerX = left + 40.0F;
        float y = top + 13.0F;
        scissor(context, left + 4, top + 2, left + 78, top + 20);
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
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("+"), x + CARD_WIDTH / 2, y + 22,
                hovered ? 0xFFFFFFFF : 0xFFD9E5E8);
        context.drawCenteredTextWithShadow(textRenderer, ui("slot", index + 1), x + CARD_WIDTH / 2, y + 45,
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
        drawStatusMessage(context, Text.literal(status), left, top,
                statusError ? 0xFFFF9A9A : 0xFF9DFFC0);
    }

    private void drawStatusMessage(DrawContext context, Text message, int left, int top, int accent) {
        int stripX = left + SCREEN_X + 3;
        int stripY = top + STATUS_Y;
        int stripWidth = SCREEN_WIDTH - 6;
        int textX = stripX + 5;
        int textY = stripY + 3;
        int availableWidth = stripWidth - 10;
        int textWidth = textRenderer.getWidth(message);
        PcStyleButton.drawFrame(context, stripX, stripY, stripWidth, 14,
                PcStyleButton.Style.NORMAL, false, true);
        context.fill(stripX + 3, stripY + 2, stripX + stripWidth - 3, stripY + 12, 0xE51A2529);
        context.fill(stripX + 3, stripY + 11, stripX + stripWidth - 3, stripY + 12, accent);
        scissor(context, textX, stripY + 1, textX + availableWidth, stripY + 13);
        if (textWidth <= availableWidth) {
            context.drawCenteredTextWithShadow(textRenderer, message,
                    textX + availableWidth / 2, textY, 0xFFF0FAFC);
        } else {
            int travel = textWidth - availableWidth;
            long phase = (System.currentTimeMillis() / 45L) % Math.max(1L, (travel + 28L) * 2L);
            int offset = (int) (phase <= travel + 28L ? Math.max(0L, phase - 14L)
                    : Math.max(0L, (travel + 28L) * 2L - phase - 14L));
            context.drawTextWithShadow(textRenderer, message,
                    textX - Math.min(travel, offset), textY, 0xFFF0FAFC);
        }
        context.disableScissor();
    }

    private void renderHoverTooltip(DrawContext context, int mouseX, int mouseY) {
        if (creating) {
            if ((rankedTeamGenerated || rankedHelperRunning)
                    && isInside(mouseX, mouseY, left() + 6, top() + 137, 70, 27)) {
                queueTooltip(textRenderer, ui("tooltip_ranked_disclaimer"), mouseX, mouseY);
                return;
            }
            int index = cardIndexAt(mouseX, mouseY);
            if (index >= 0 && index < draft.size()) {
                DraftSlot slot = draft.get(index);
                List<Text> lines = new ArrayList<>();
                lines.add(Text.literal(displayName(slot)));
                if (slot.abilityId != null) lines.add(ui("tooltip_ability_saved", abilityName(slot.abilityId)));
                lines.add(ui("tooltip_item_saved", itemName(slot.itemId)));
                if (slot.natureId != null) lines.add(ui("tooltip_nature_saved", plannedNatureName(slot.natureId)));
                if (slot.evs != null && !slot.evs.isEmpty()) lines.add(ui("tooltip_evs_saved", evSummary(slot.evs)));
                lines.add(ui("tooltip_origin", slot.source));
                if (slot.pokemon == null) lines.add(ui("replace_required"));
                else lines.add(ui("tooltip_moves_count", slot.moveIds.size()));
                if (rankedTeamGenerated) {
                    lines.add(ui(rankedLockedSlots.contains(index)
                            ? "tooltip_ranked_locked" : "tooltip_ranked_unlocked"));
                    if (!rankedLockedSlots.contains(index)) lines.add(ui("tooltip_ranked_slot_regenerate"));
                }
                RankedReason reason = rankedReasons.get(rankedKey(slot));
                if (reason != null) {
                    lines.add(ui("ranked_reason_usage", formatRankedPercent(reason.usagePercent)));
                    if (!reason.partner.isBlank()) {
                        lines.add(ui("ranked_reason_partner", reason.partner,
                                formatRankedPercent(reason.affinity)));
                    }
                    lines.add(ui("ranked_reason_style", reason.style));
                }
                lines.add(ui("tooltip_slot_controls"));
                queueTooltip(textRenderer, lines, Optional.empty(), mouseX, mouseY);
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
            if (slot.abilityId != null) lines.add(ui("tooltip_ability_saved", abilityName(slot.abilityId)));
            lines.add(ui("tooltip_expected", itemName(slot.itemId)));
            if (slot.natureId != null) lines.add(ui("tooltip_nature_saved", plannedNatureName(slot.natureId)));
            if (slot.evs != null && !slot.evs.isEmpty()) lines.add(ui("tooltip_evs_saved", evSummary(slot.evs)));
            lines.add(ui("tooltip_current", heldItemName(pokemon)));
            lines.add(Text.literal(stateText(state)));
            lines.add(ui("tooltip_action", actionLabel(current, slot, pokemon, state)));
            lines.add(ui("tooltip_preview_controls"));
            queueTooltip(textRenderer, lines, Optional.empty(), mouseX, mouseY);
        }
    }

    private void renderItemPicker(DrawContext context, int mouseX, int mouseY, int left, int top) {
        int panelX = left + (PANEL_WIDTH - 214) / 2;
        int panelY = top + (PANEL_HEIGHT - 160) / 2;
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
            queueTooltip(textRenderer, lines, Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean clickContent(double mouseX, double mouseY, int button) {
        if (teamDrag.team != null) {
            if (button == 1) cancelTeamDrag();
            return true;
        }
        if (pokemonPickerOpen) return handlePokemonPickerClick(mouseX, mouseY, button);
        if (movePickerOpen) return handleMovePickerClick(mouseX, mouseY, button);
        if (itemPickerOpen) return handleItemPickerClick(mouseX, mouseY, button);
        if (setEditorOpen) return handleSetEditorClick(mouseX, mouseY, button);
        if (teamDoctorOpen) {
            if (button == 0) closeTeamDoctor();
            return true;
        }
        if (ClientTeamApplier.isActive(this)) return super.clickContent(mouseX, mouseY, button);
        if (!creating && reorderingTeams && button == 0) {
            int index = teamListIndexAt(mouseX, mouseY);
            if (index >= 0) {
                selectTeam(index);
                teamDrag.begin(data.teams.get(index), mouseX, mouseY);
                teamDragEdgeTicks = 0;
                return true;
            }
        }
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
                draggedPreviewSlot = preview;
                previewPressX = previewDragX = mouseX;
                previewPressY = previewDragY = mouseY;
                previewDragging = false;
                return true;
            }
        }
        if (creating && button == 1) {
            int index = cardIndexAt(mouseX, mouseY);
            if (index >= 0 && index < draft.size()) {
                selectedDraftSlot = selectedDraftSlot == index ? -1 : index;
                status = selectedDraftSlot < 0 ? uiText("status_deselected") : uiText("status_selected", index + 1);
                statusError = false;
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
                    if (rankedTeamGenerated && isInside(mouseX, mouseY,
                            cardX + CARD_WIDTH - 11, cardY + 2, 10, 12)) {
                        toggleRankedLock(index);
                        return true;
                    }
                    if (rankedTeamGenerated && !rankedLockedSlots.contains(index)
                            && isInside(mouseX, mouseY, cardX + 1,
                            cardY + CARD_HEIGHT - 17, 15, 15)) {
                        regenerateRankedSlot(index);
                        return true;
                    }
                    if (isInside(mouseX, mouseY, cardX + CARD_WIDTH - ITEM_SLOT_SIZE - 2,
                            cardY + CARD_HEIGHT - ITEM_SLOT_SIZE - 4, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE)) {
                        openItemPicker(index);
                        return true;
                    }
                    draggedDraftSlot = index;
                    draftPressX = draftDragX = mouseX;
                    draftPressY = draftDragY = mouseY;
                    draftDragging = false;
                }
                else if (draft.size() < TeamModels.MAX_TEAM_SIZE) openPicker(-1);
                return true;
            }
        }
        return super.clickContent(mouseX, mouseY, button);
    }

    private boolean handlePokemonPickerClick(double mouseX, double mouseY, int button) {
        if (button != 0) return true;
        if (pendingAbilityPokemon != null) return handleAbilityPickerClick(mouseX, mouseY);
        if (rankedSeasonMenuOpen) {
            if (isInside(mouseX, mouseY, pickerLeft() + 115, pickerTop() + 27, 86, 17)) {
                return super.clickContent(mouseX, mouseY, button);
            }
            int seasonIndex = rankedSeasonIndexAt(mouseX, mouseY);
            if (seasonIndex >= 0) {
                selectRankedSeason(rankedSeasons.get(seasonIndex));
                return true;
            }
            if (isInside(mouseX, mouseY, pickerLeft() + 115, pickerTop() + 45, 126,
                    Math.max(1, rankedSeasons.size()) * 14 + 2)) {
                return true;
            }
            rankedSeasonMenuOpen = false;
        }
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
            refreshPokemonPickerModels();
            return true;
        }
        int index = pokemonChoiceIndexAt(mouseX, mouseY);
        if (index >= 0 && index < filteredPokemonChoices.size()) {
            PokemonChoice choice = filteredPokemonChoices.get(index);
            if (choice.pokemon == null) openAbilityPicker(choice);
            else selectPokemon(choice);
            return true;
        }
        int panelX = pickerLeft() + 5;
        int footerY = pickerTop() + pickerHeight() - 15;
        if (isInside(mouseX, mouseY, panelX + 2, footerY, 24, 14) && pokemonPickerPage > 0) {
            pokemonPickerPage--;
            refreshPokemonPickerModels();
            return true;
        }
        if (isInside(mouseX, mouseY, panelX + pickerWidth() - 36, footerY, 24, 14)
                && pokemonPickerPage + 1 < pokemonPickerPages()) {
            pokemonPickerPage++;
            refreshPokemonPickerModels();
            return true;
        }
        if (!isInside(mouseX, mouseY, pickerLeft(), pickerTop(), pickerWidth(), pickerHeight())) {
            pickerReplaceIndex = -1;
            closePokemonPicker(true);
            return true;
        }
        return super.clickContent(mouseX, mouseY, button);
    }

    private int rankedSeasonIndexAt(double mouseX, double mouseY) {
        int x = pickerLeft() + 115;
        int y = pickerTop() + 46;
        if (rankedSeasons.isEmpty() || !isInside(mouseX, mouseY, x + 1, y, 124, rankedSeasons.size() * 14)) {
            return -1;
        }
        return Math.min(rankedSeasons.size() - 1, ((int) mouseY - y) / 14);
    }

    private boolean handleMovePickerClick(double mouseX, double mouseY, int button) {
        if (button != 0) return true;
        int panelX = left() + (PANEL_WIDTH - MOVE_PICKER_WIDTH) / 2;
        int panelY = top() + (PANEL_HEIGHT - MOVE_PICKER_HEIGHT) / 2;
        for (int i = 0; i < 4; i++) {
            if (isInside(mouseX, mouseY, panelX + 8, panelY + 36 + i * 25,
                    MOVE_PICKER_PRESET_WIDTH, 22)) {
                movePickerSelectedIndex = i;
                return true;
            }
        }
        for (int row = 0; row < MOVE_PICKER_ROWS; row++) {
            int index = movePickerPage * MOVE_PICKER_ROWS + row;
            if (index >= moveChoices.size()) break;
            if (isInside(mouseX, mouseY, panelX + MOVE_PICKER_LIST_X,
                    panelY + 36 + row * 20, MOVE_PICKER_LIST_WIDTH, 18)) {
                chooseMove(moveChoices.get(index));
                return true;
            }
        }
        if (!isInside(mouseX, mouseY, panelX, panelY, MOVE_PICKER_WIDTH, MOVE_PICKER_HEIGHT)) {
            closeMovePicker(true);
        }
        return true;
    }

    private void openMovePicker(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= draft.size()) return;
        DraftSlot slot = draft.get(slotIndex);
        if (slot.pokemon == null && findSpecies(slot.speciesId) == null) return;
        movePickerSlot = slotIndex;
        movePickerSelectedIndex = 0;
        movePickerPage = 0;
        movePickerOpen = true;
        scanMoveChoices(slot);
        clearAndInit();
        loadRankedMoveUsage(slot);
    }

    private void closeMovePicker(boolean rebuild) {
        int previousSlot = movePickerSlot;
        boolean reopen = returnToSetEditor && previousSlot >= 0 && previousSlot < draft.size();
        movePickerOpen = false;
        movePickerSlot = -1;
        movePickerPage = 0;
        moveChoices.clear();
        rankedMoveUsageRequest++;
        if (rankedMoveUsageFuture != null) rankedMoveUsageFuture.cancel(true);
        rankedMoveUsageFuture = null;
        returnToSetEditor = false;
        if (reopen) {
            setEditorSlot = previousSlot;
            setEditorOpen = true;
        }
        if (rebuild || reopen) clearAndInit();
    }

    private void scanMoveChoices(DraftSlot slot) {
        moveChoices.clear();
        Set<String> seen = new HashSet<>();
        if (slot.pokemon != null) {
            for (Move move : slot.pokemon.getMoveSet().getMoves()) {
                if (seen.add(move.getTemplate().getName())) {
                    moveChoices.add(new MoveChoice(move.getTemplate(), MoveSource.ACTIVE));
                }
            }
            for (BenchedMove move : slot.pokemon.getBenchedMoves()) {
                MoveTemplate template = move.getMoveTemplate();
                if (seen.add(template.getName())) moveChoices.add(new MoveChoice(template, MoveSource.RESERVE));
            }
        }
        Species species = findSpecies(slot.speciesId);
        FormData form = slot.pokemon == null ? findForm(species, slot.formId) : slot.pokemon.getForm();
        if (form != null) {
            CobblemonCatalogueCache.Entry cached = catalogue.snapshot().entry(form);
            Iterable<MoveTemplate> legalMoves = cached == null
                    ? form.getMoves().getAllLegalMoves() : cached.legalMoves();
            for (MoveTemplate template : legalMoves) {
                if (seen.add(template.getName())) moveChoices.add(new MoveChoice(template, MoveSource.CATALOGUE));
            }
        }
        Map<String, Double> moveUsage = rankedMoveUsage.getOrDefault(rankedKey(slot), Map.of());
        moveChoices.sort((first, second) -> {
            if (first.source != second.source) return Integer.compare(first.source.ordinal(), second.source.ordinal());
            double firstUsage = moveUsage.getOrDefault(RankedUsageService.key(first.template.getName()), 0.0D);
            double secondUsage = moveUsage.getOrDefault(RankedUsageService.key(second.template.getName()), 0.0D);
            int usageOrder = Double.compare(secondUsage, firstUsage);
            if (usageOrder != 0) return usageOrder;
            return first.template.getDisplayName().getString()
                    .compareToIgnoreCase(second.template.getDisplayName().getString());
        });
    }

    private void loadRankedMoveUsage(DraftSlot slot) {
        String pokemon = rankedKey(slot);
        if (pokemon.isBlank() || rankedMoveUsage.containsKey(pokemon)) return;
        int request = ++rankedMoveUsageRequest;
        if (rankedMoveUsageFuture != null) rankedMoveUsageFuture.cancel(true);
        CompletableFuture<Map<String, Double>> loading = RankedUsageService.INSTANCE
                .loadMoveUsage(rankedSeason, pokemon);
        rankedMoveUsageFuture = loading;
        loading.whenComplete((usage, error) -> {
            if (client == null) return;
            client.execute(() -> {
                if (request != rankedMoveUsageRequest) return;
                rankedMoveUsageFuture = null;
                rankedMoveUsage.put(pokemon, error == null && usage != null ? usage : Map.of());
                if (error != null) {
                    TropimonTeamSaverClient.LOGGER.debug(
                            "Usages d'attaques indisponibles pour {}", pokemon, error);
                }
                if (movePickerOpen && movePickerSlot >= 0 && movePickerSlot < draft.size()
                        && rankedKey(draft.get(movePickerSlot)).equals(pokemon)) {
                    scanMoveChoices(draft.get(movePickerSlot));
                    movePickerPage = 0;
                }
            });
        });
    }

    private void chooseMove(MoveChoice choice) {
        if (movePickerSlot < 0 || movePickerSlot >= draft.size()) return;
        DraftSlot previous = draft.get(movePickerSlot);
        List<String> moves = new ArrayList<>(previous.moveIds);
        int activeSlots = TeamModels.MAX_MOVES;
        while (moves.size() < activeSlots) moves.add("");
        int target = Math.min(movePickerSelectedIndex, activeSlots - 1);
        int existing = moves.indexOf(choice.template.getName());
        if (existing >= 0 && existing != target) Collections.swap(moves, existing, target);
        else moves.set(target, choice.template.getName());
        moves.removeIf(String::isBlank);
        draft.set(movePickerSlot, new DraftSlot(previous.pokemon, previous.pokemonId, previous.speciesId,
                previous.formId, previous.itemId, previous.abilityId, moves,
                previous.natureId, previous.evs, previous.source));
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
        int panelX = left() + (PANEL_WIDTH - 214) / 2;
        int panelY = top() + (PANEL_HEIGHT - 160) / 2;
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
    public boolean scrollContent(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (teamDrag.team != null) {
            if (!ClientTeamApplier.isActive(this)) {
                int direction = teamDrag.scrollPage(verticalAmount);
                if (direction != 0) {
                    teamDrag.update(mouseX, mouseY);
                    changeTeamDragPage(direction);
                    teamDragEdgeTicks = 0;
                }
            }
            return true;
        }
        if (pokemonPickerOpen) {
            if (pendingAbilityPokemon != null) return true;
            if (verticalAmount < 0 && pokemonPickerPage + 1 < pokemonPickerPages()) {
                pokemonPickerPage++;
                refreshPokemonPickerModels();
            } else if (verticalAmount > 0 && pokemonPickerPage > 0) {
                pokemonPickerPage--;
                refreshPokemonPickerModels();
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
        if (!creating && !data.teams.isEmpty()
                && isInside(mouseX, mouseY, left() + LIST_X, top() + LIST_START_Y,
                LEFT_CONTROL_WIDTH, PAGE_SIZE * LIST_ROW_HEIGHT)) {
            if (ClientTeamApplier.isActive(this)) return true;
            if (verticalAmount < 0) changePage(1);
            else if (verticalAmount > 0) changePage(-1);
            return true;
        }
        return super.scrollContent(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void tick() {
        super.tick();
        tickTeamDrag();
        refreshReadModels();
        if (!setEditorOpen || heldEvStat == null || heldEvDirection == 0) {
            if (!setEditorOpen) stopEvAdjustment();
            return;
        }
        heldEvTicks++;
        if (heldEvTicks < 7) return;
        int multiplier = heldEvTicks >= 30 ? 2 : 1;
        adjustSetEv(heldEvStat, heldEvDirection * 4 * multiplier);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (teamDrag.team != null) {
            if (keyCode == 256) cancelTeamDrag();
            return true;
        }
        if (pokemonPickerOpen && pendingAbilityPokemon != null && keyCode == 256) {
            if (abilityEditSlot >= 0) {
                closePokemonPicker(false);
                clearAndInit();
            } else {
                pendingAbilityPokemon = null;
                clearAndInit();
            }
            return true;
        }
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
        if (setEditorOpen && keyCode == 256) {
            closeSetEditor();
            return true;
        }
        if (teamDoctorOpen && keyCode == 256) {
            closeTeamDoctor();
            return true;
        }
        if (!creating && reorderingTeams && keyCode == 256) {
            toggleTeamReordering();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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
        int previousSlot = itemPickerSlot;
        boolean reopen = returnToSetEditor && previousSlot >= 0 && previousSlot < draft.size();
        itemPickerOpen = false;
        itemPickerSlot = -1;
        itemPickerPage = 0;
        itemChoices.clear();
        returnToSetEditor = false;
        if (reopen) {
            setEditorSlot = previousSlot;
            setEditorOpen = true;
        }
        if (rebuild) clearAndInit();
    }

    private void scanAvailableItems() {
        itemChoices.clear();
        itemChoices.add(new ItemChoice("minecraft:air", ItemStack.EMPTY, 0));
        if (client != null && client.player != null) {
            for (int slot = 0; slot < Math.min(36, client.player.getInventory().size()); slot++) {
                ItemStack stack = client.player.getInventory().getStack(slot);
                if (stack.isEmpty()) continue;
                addItemChoice(stack, stack.getCount(), uiText("item_source_inventory"));
            }
        }

        for (Item item : Registries.ITEM) {
            ItemStack stack = item.getDefaultStack();
            if (!stack.isEmpty()) addItemChoice(stack, 0, uiText("item_source_catalogue"));
        }

        if (itemChoices.size() > 2) {
            CompetitiveItemRanker.Profile profile = selectedItemProfile();
            Map<String, Integer> scores = new LinkedHashMap<>();
            Map<String, String> names = new LinkedHashMap<>();
            for (ItemChoice choice : itemChoices) {
                scores.put(choice.itemId, CompetitiveItemRanker.score(choice.itemId, profile));
                names.put(choice.itemId, choice.stack.getName().getString());
            }
            itemChoices.subList(1, itemChoices.size()).sort(
                    Comparator.comparingInt((ItemChoice choice) -> scores.get(choice.itemId)).reversed()
                            .thenComparing(Comparator.comparingInt((ItemChoice choice) -> choice.count).reversed())
                            .thenComparing(choice -> names.get(choice.itemId), String.CASE_INSENSITIVE_ORDER));
        }
        itemPickerInventory = Map.copyOf(inventoryItemCounts());
    }

    private CompetitiveItemRanker.Profile selectedItemProfile() {
        String speciesId = null;
        String formId = null;
        String ability = null;
        List<String> moves = List.of();
        if (creating && itemPickerSlot >= 0 && itemPickerSlot < draft.size()) {
            DraftSlot slot = draft.get(itemPickerSlot);
            speciesId = slot.speciesId;
            formId = slot.formId;
            ability = slot.abilityId;
            moves = slot.moveIds;
        } else if (!creating && !data.teams.isEmpty()) {
            SavedTeam team = data.teams.get(Math.max(0, Math.min(selectedTeam, data.teams.size() - 1)));
            if (itemPickerSlot >= 0 && itemPickerSlot < team.slots.size()) {
                SavedSlot slot = team.slots.get(itemPickerSlot);
                speciesId = slot.speciesId;
                formId = slot.formId;
                ability = slot.abilityId;
                moves = slot.moveIds == null ? List.of() : slot.moveIds;
            }
        }
        FormData form = findForm(findSpecies(speciesId), formId);
        if (form == null) return CompetitiveItemRanker.Profile.empty();
        Set<String> types = new LinkedHashSet<>();
        types.add(form.getPrimaryType().getName());
        if (form.getSecondaryType() != null) types.add(form.getSecondaryType().getName());
        Map<Stat, Integer> stats = form.getBaseStats();
        return new CompetitiveItemRanker.Profile(types, new LinkedHashSet<>(moves), ability,
                stats.getOrDefault(Stats.HP, 0), stats.getOrDefault(Stats.ATTACK, 0),
                stats.getOrDefault(Stats.DEFENCE, 0), stats.getOrDefault(Stats.SPECIAL_ATTACK, 0),
                stats.getOrDefault(Stats.SPECIAL_DEFENCE, 0), stats.getOrDefault(Stats.SPEED, 0));
    }

    private void addItemChoice(ItemStack stack, int count, String source) {
        if (stack.isEmpty() || (!stack.isIn(PVP_ITEM_TAG) && !stack.isIn(PVP_BERRY_TAG))) return;
        String id = itemId(stack);
        int available = Math.max(0, count);
        for (ItemChoice choice : itemChoices) {
            if (choice.itemId.equals(id)) {
                choice.count += available;
                choice.sources.add(source);
                return;
            }
        }
        ItemStack icon = stack.copy();
        icon.setCount(1);
        ItemChoice choice = new ItemChoice(id, icon, available);
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
                    previous.formId, selectedItemId, previous.abilityId, previous.moveIds,
                    previous.natureId, previous.evs, previous.source));
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
        int gridX = left + (PANEL_WIDTH - 214) / 2 + 11;
        int gridY = top + (PANEL_HEIGHT - 160) / 2 + 34;
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
    public boolean dragContent(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (teamDrag.team != null && button == 0) {
            teamDrag.update(mouseX, mouseY);
            return true;
        }
        if (creating && draggedDraftSlot >= 0 && button == 0) {
            draftDragX = mouseX;
            draftDragY = mouseY;
            double x = mouseX - draftPressX;
            double y = mouseY - draftPressY;
            if (x * x + y * y >= SLOT_DRAG_THRESHOLD_SQUARED) draftDragging = true;
            return true;
        }
        if (!creating && draggedPreviewSlot >= 0 && button == 0) {
            previewDragX = mouseX;
            previewDragY = mouseY;
            double x = mouseX - previewPressX;
            double y = mouseY - previewPressY;
            if (x * x + y * y >= SLOT_DRAG_THRESHOLD_SQUARED) previewDragging = true;
            return true;
        }
        return super.dragContent(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean releaseContent(double mouseX, double mouseY, int button) {
        if (teamDrag.team != null && button == 0) {
            int from = findTeamIndex(teamDrag.team.id);
            int boundary = teamDrag.dragging ? teamDropBoundary(mouseX, mouseY) : -1;
            int to = TeamListDrag.targetIndex(from, boundary, data.teams.size());
            cancelTeamDrag();
            if (to >= 0 && to != from) moveTeamTo(from, to);
            return true;
        }
        if (button == 0 && heldEvStat != null) {
            stopEvAdjustment();
            return true;
        }
        if (!creating && button == 0 && draggedPreviewSlot >= 0) {
            int source = draggedPreviewSlot;
            int target = previewCardIndexAt(mouseX, mouseY);
            boolean dragged = previewDragging;
            draggedPreviewSlot = -1;
            previewDragging = false;
            if (dragged) {
                lastPreviewClickSlot = -1;
                if (target >= 0 && target < selectedTeam().slots.size() && target != source) {
                    swapSavedSlots(source, target);
                }
                return true;
            }
            if (target == source) {
                long now = System.currentTimeMillis();
                boolean doubleClick = lastPreviewClickSlot == source && now - lastPreviewClickTime <= DOUBLE_CLICK_MS;
                lastPreviewClickSlot = source;
                lastPreviewClickTime = now;
                if (doubleClick) {
                    lastPreviewClickSlot = -1;
                    quickReplaceFromBrowser(source);
                } else {
                    selectedPreviewSlot = selectedPreviewSlot == source ? -1 : source;
                    clearAndInit();
                }
            }
            return true;
        }
        if (creating && button == 0 && draggedDraftSlot >= 0) {
            int source = draggedDraftSlot;
            int target = cardIndexAt(mouseX, mouseY);
            boolean dragged = draftDragging;
            draggedDraftSlot = -1;
            draftDragging = false;
            if (target >= 0 && target < draft.size()) {
                if (dragged && target != source) {
                    Collections.swap(draft, source, target);
                    swapRankedLocks(source, target);
                    selectedDraftSlot = target;
                    status = uiText("status_order", source + 1, target + 1);
                    statusError = false;
                    clearAndInit();
                } else if (!dragged && target == source) {
                    selectedDraftSlot = source;
                    replaceOrAssociate(source);
                } else {
                    selectedDraftSlot = source;
                    status = uiText("status_selected", source + 1);
                    statusError = false;
                    clearAndInit();
                }
            }
            return true;
        }
        return super.releaseContent(mouseX, mouseY, button);
    }

    private TeamAnalysis analysis(SavedTeam team) {
        int changes = 0;
        int itemDifferences = 0;
        int moveDifferences = 0;
        for (int i = 0; i < TeamModels.MAX_TEAM_SIZE; i++) {
            Pokemon current = parent.getParty().get(i);
            String currentId = current == null ? null : current.getUuid().toString();
            SavedSlot target = i < team.slots.size() ? team.slots.get(i) : null;
            String targetId = target == null ? null : target.pokemonId;
            if (currentId == null ? targetId != null : !currentId.equals(targetId)) changes++;
            if (target != null) {
                Pokemon pokemon = findPokemon(target.pokemonId);
                SlotState state = slotState(target, pokemon);
                if (state == SlotState.ITEM_MISMATCH) itemDifferences++;
                if (pokemon != null && target.moveIds != null && !target.moveIds.isEmpty()
                        && !target.moveIds.equals(activeMoveIds(pokemon))) moveDifferences++;
            }
        }
        return new TeamAnalysis(changes, itemDifferences, moveDifferences, validate(team));
    }

    /** Presentation caches only. saveDraft/applyTeam still call the original live preflight. */
    private TeamReadKey readKey(SavedTeam team) {
        return TeamReadKey.capture(team, ClientDataRevision.storage(), ClientDataRevision.pokemon(),
                ClientDataRevision.catalogue(), inventoryItemCounts());
    }

    private TeamAnalysis cachedBrowserAnalysis(SavedTeam team) {
        TeamAnalysis next = browserAnalysisCache.get(readKey(team), () -> analysis(team));
        if (browserAnalysis != next || !browserSummaryLanguage.equals(language())) {
            browserAnalysis = next;
            browserSummary = analysisSummary(next);
            browserSummaryLanguage = language();
            if (equipButton != null) equipButton.setTooltip(Tooltip.of(Text.literal(browserSummary)));
        }
        return browserAnalysis;
    }

    private TeamValidationService.Report cachedDraftValidation() {
        SavedTeam team = draftAsTeam();
        return draftValidationCache.get(readKey(team), () -> validate(team));
    }

    private void refreshDoctor() {
        // Members depend on planned sets and display names, NOT on render time or animations.
        TeamDoctorCache.Input key = new TeamDoctorCache.Input(draft.stream().map(slot -> new TeamReadKey.Slot(
                slot.pokemonId, slot.speciesId, slot.formId, slot.itemId, slot.abilityId, slot.natureId,
                slot.moveIds, slot.evs)).toList(), draft.stream().map(this::displayName).toList(),
                ClientDataRevision.catalogue(), language());
        doctorFindings = teamDoctorCache.analyze(key, rankedBuildStyle, this::teamDoctorMembers);
    }

    private String language() {
        return client == null ? "" : client.options.language;
    }

    private void refreshReadModels() {
        boolean storageChanged = ownedPokemon.refreshIfChanged(parent);
        if (storageChanged) {
            for (int i = 0; i < draft.size(); i++) {
                DraftSlot slot = draft.get(i);
                Pokemon current = findPokemon(slot.pokemonId);
                if (slot.pokemon != current) draft.set(i, new DraftSlot(current, slot.pokemonId, slot.speciesId,
                        slot.formId, slot.itemId, slot.abilityId, slot.moveIds, slot.natureId, slot.evs, slot.source));
            }
        }
        long pokemonRevision = ClientDataRevision.pokemon();
        long catalogueRevision = ClientDataRevision.catalogue();
        String currentLanguage = language();
        boolean catalogueChanged = observedCatalogueRevision != catalogueRevision;
        boolean languageChanged = !observedLanguage.equals(currentLanguage);
        boolean pickerChanged = storageChanged || observedPokemonRevision != pokemonRevision
                || catalogueChanged || languageChanged;
        observedPokemonRevision = pokemonRevision;
        observedCatalogueRevision = catalogueRevision;
        observedLanguage = currentLanguage;
        if (catalogueChanged) {
            rankedTemplateCache = null;
            rankedProfileCache = null;
        }
        pickerRefreshPending |= pickerChanged;
        if (pickerRefreshPending && pokemonPickerOpen && pendingAbilityPokemon == null) {
            scanPokemonChoices();
            refreshPokemonPickerModels();
            pickerRefreshPending = false;
        }
        if (pickerChanged && movePickerOpen && movePickerSlot >= 0 && movePickerSlot < draft.size()) {
            scanMoveChoices(draft.get(movePickerSlot));
            movePickerPage = Math.min(movePickerPage, movePickerPages() - 1);
        }
        if (itemPickerOpen && (catalogueChanged || languageChanged || !itemPickerInventory.equals(inventoryItemCounts()))) {
            scanAvailableItems();
            itemPickerPage = Math.min(itemPickerPage, Math.max(0, (itemChoices.size() - 1) / ITEM_PICKER_PAGE_SIZE));
        }
        if (teamDoctorOpen) refreshDoctor();
        if (!creating && !data.teams.isEmpty()) {
            TeamAnalysis value = cachedBrowserAnalysis(selectedTeam());
            if (equipButton != null) {
                equipButton.active = ClientTeamApplier.isActive(this) || value.validation.readyToEquip();
            }
        } else if (creating && saveButton != null) {
            TeamValidationService.Report value = cachedDraftValidation();
            saveButton.active = !draft.isEmpty() && value.readyToSave();
            if (displayedDraftValidation != value || !draftSummaryLanguage.equals(currentLanguage)) {
                saveButton.setTooltip(Tooltip.of(Text.literal(validationSummary(value))));
                displayedDraftValidation = value;
                draftSummaryLanguage = currentLanguage;
            }
        }
    }

    private String analysisSummary(TeamAnalysis analysis) {
        TeamValidationService.Report validation = analysis.validation;
        if (!validation.readyToEquip()) return validationSummary(validation);
        return uiText("validation_ready", validation.missingItems(), validation.movesToLearn(),
                validation.natureMismatches(), validation.evMismatches(), validation.repeatedSpecies()) + " · "
                + uiText("summary_changes_moves", analysis.changes, analysis.itemDifferences,
                analysis.moveDifferences);
    }

    private TeamValidationService.Report validate(SavedTeam team) {
        return TeamValidationService.validate(team, ownedPokemon, catalogue.snapshot(), inventoryItemCounts());
    }

    private Map<String, Integer> inventoryItemCounts() {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (client == null || client.player == null) return result;
        for (int slot = 0; slot < Math.min(36, client.player.getInventory().size()); slot++) {
            ItemStack stack = client.player.getInventory().getStack(slot);
            if (!stack.isEmpty()) result.merge(itemId(stack), stack.getCount(), Integer::sum);
        }
        return result;
    }

    private SavedTeam draftAsTeam() {
        SavedTeam team = new SavedTeam();
        team.id = editingTeamId;
        team.name = draftName;
        for (DraftSlot slot : draft) team.slots.add(new SavedSlot(slot.pokemonId, slot.speciesId,
                slot.formId, slot.itemId, slot.abilityId, slot.moveIds, slot.natureId, slot.evs));
        return team;
    }

    private String validationSummary(TeamValidationService.Report report) {
        if (report.unassociated() > 0) return uiText("validation_unassociated", report.unassociated());
        if (report.missingPokemon() > 0) return uiText("validation_missing_pokemon", report.missingPokemon());
        if (report.duplicates() > 0) return uiText("validation_duplicates", report.duplicates());
        if (report.evOverflow() > 0) return uiText("validation_evs", report.evOverflow());
        if (report.illegalMoves() > 0) return uiText("validation_illegal_moves", report.illegalMoves());
        if (report.invalidAbilities() > 0) return uiText("validation_invalid_abilities", report.invalidAbilities());
        if (report.formMismatches() > 0) return uiText("validation_forms", report.formMismatches());
        if (report.abilityMismatches() > 0) return uiText("validation_ability_mismatch", report.abilityMismatches());
        return uiText("validation_ready", report.missingItems(), report.movesToLearn(),
                report.natureMismatches(), report.evMismatches(), report.repeatedSpecies());
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
        return x + (index % 3) * (PREVIEW_CARD_WIDTH + CARD_GAP_X);
    }

    private int previewCardY(int y, int index) {
        return y + (index / 3) * (PREVIEW_CARD_HEIGHT + CARD_GAP_Y);
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private String currentSource(Pokemon pokemon) {
        if (pokemon == null) return uiText("not_found");
        OwnedPokemonIndex.Entry entry = ownedPokemon.byUuid(pokemon.getUuid());
        if (entry == null) return uiText("source_pc");
        return entry.party() ? uiText("source_party") : uiText("source_box", entry.box() + 1);
    }

    private String pokemonName(Pokemon pokemon) {
        return pokemon == null ? uiText("state_empty") : pokemon.getDisplayName(false).getString();
    }

    private String savedSlotName(SavedSlot slot, Pokemon pokemon) {
        if (pokemon != null) return pokemonName(pokemon);
        return plannedSpeciesName(slot.speciesId, slot.formId);
    }

    private String displayName(DraftSlot slot) {
        return slot.pokemon == null ? plannedSpeciesName(slot.speciesId, slot.formId) : pokemonName(slot.pokemon);
    }

    private String heldItemName(Pokemon pokemon) {
        return pokemon == null || pokemon.getHeldItem$common().isEmpty() ? uiText("no_item") : pokemon.getHeldItem$common().getName().getString();
    }

    private Text natureName(Pokemon pokemon) {
        String path = pokemon.getEffectiveNature().getName().getPath();
        return Text.translatable("cobblemon.nature." + path);
    }

    private Text abilityName(Pokemon pokemon) {
        return abilityName(abilityId(pokemon));
    }

    private Text abilityName(String rawAbilityId) {
        String id = TeamModels.canonicalAbilityId(rawAbilityId);
        return id == null ? ui("ability_unspecified") : Text.translatable("cobblemon.ability." + id);
    }

    private String abilityId(Pokemon pokemon) {
        return pokemon == null ? null : TeamModels.canonicalAbilityId(pokemon.getAbility().getTemplate().getName());
    }

    private List<AbilityChoice> abilityChoices(Iterable<PotentialAbility> potentials) {
        Map<String, Boolean> unique = new LinkedHashMap<>();
        for (PotentialAbility potential : potentials) {
            String id = TeamModels.canonicalAbilityId(potential.getTemplate().getName());
            if (id != null) unique.merge(id, potential instanceof HiddenAbility, Boolean::logicalOr);
        }
        boolean hasSeveralAbilities = unique.size() > 1;
        List<AbilityChoice> result = new ArrayList<>();
        unique.forEach((id, hidden) -> result.add(new AbilityChoice(id, hasSeveralAbilities && hidden)));
        return List.copyOf(result);
    }

    private List<AbilityChoice> cachedAbilityChoices(FormData form) {
        CobblemonCatalogueCache.Entry entry = catalogue.snapshot().entry(form);
        if (entry == null) return form == null ? List.of() : abilityChoices(form.getAbilities());
        return entry.abilities().stream().map(ability -> new AbilityChoice(ability.id(), ability.hidden())).toList();
    }

    private Text abilitySummary(PokemonChoice choice) {
        if (choice == null || choice.abilities.isEmpty()) return ui("ability_unspecified");
        return pickerAbilities.computeIfAbsent(choice, this::buildAbilitySummary);
    }

    private Text buildAbilitySummary(PokemonChoice choice) {
        var summary = Text.empty();
        for (int i = 0; i < choice.abilities.size(); i++) {
            if (i > 0) summary.append(Text.literal(" / ").styled(style -> style.withColor(0x07566A)));
            summary.append(coloredAbilityName(choice.abilities.get(i)));
        }
        return summary;
    }

    private Text coloredAbilityName(AbilityChoice ability) {
        int color = ability.hidden ? 0xFFD89B24 : 0xFF07566A;
        return abilityName(ability.id).copy().styled(style -> style.withColor(color));
    }

    private boolean isHiddenAbility(Pokemon pokemon) {
        String currentAbility = abilityId(pokemon);
        for (AbilityChoice choice : cachedAbilityChoices(pokemon.getForm())) {
            if (choice.id.equals(currentAbility)) return choice.hidden;
        }
        return false;
    }

    private boolean isHiddenAbility(Species species, String rawFormId, String rawAbilityId) {
        String ability = TeamModels.canonicalAbilityId(rawAbilityId);
        if (ability == null) return false;
        FormData form = findForm(species, rawFormId);
        if (form == null) return false;
        for (AbilityChoice choice : cachedAbilityChoices(form)) {
            if (choice.id.equals(ability)) return choice.hidden;
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
        int panelX = pickerLeft() + 5;
        int firstY = pickerTop() + 59;
        int rows = pokemonPickerRows();
        if (!isInside(mouseX, mouseY, panelX + 2, firstY, pickerWidth() - 14,
                rows * POKEMON_PICKER_ROW_HEIGHT)) return -1;
        int row = ((int) mouseY - firstY) / POKEMON_PICKER_ROW_HEIGHT;
        int index = pokemonPickerPage * rows + row;
        return index < filteredPokemonChoices.size() ? index : -1;
    }

    private int pokemonStatHeaderIndexAt(double mouseX, double mouseY) {
        int panelX = pickerLeft() + 5;
        int statX = panelX + 231 + pickerWidth() - PCGUI.BASE_WIDTH;
        if (!isInside(mouseX, mouseY, statX, pickerTop() + 46, 108, 13)) return -1;
        return Math.min(DISPLAY_STATS.size() - 1, ((int) mouseX - statX) / 18);
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

    private Text pokemonIvLine(Pokemon pokemon, int start, int end) {
        var line = Text.empty();
        for (int index = start; index < Math.min(end, DISPLAY_STATS.size()); index++) {
            if (index > start) line.append(Text.literal(" · ").styled(style -> style.withColor(0x6F8189)));
            Stat stat = DISPLAY_STATS.get(index);
            int value = pokemon.getIvs().getOrDefault(stat);
            line.append(compactStatHeader(stat).copy().styled(style -> style.withColor(0xA7C6CD)));
            line.append(Text.literal(" " + value).styled(style -> style.withColor(ivValueColor(value))));
        }
        return line;
    }

    private int ivValueColor(int value) {
        if (value >= 31) return 0xFFFFC83D;
        if (value >= 21) return 0xFF34B86B;
        if (value >= 11) return 0xFF17242B;
        return 0xFFFF5A5F;
    }

    private String pokemonChoiceCompactDetail(PokemonChoice choice) {
        return pickerDetails.computeIfAbsent(choice, this::buildPokemonChoiceCompactDetail);
    }

    private String buildPokemonChoiceCompactDetail(PokemonChoice choice) {
        if (choice.pokemon == null) {
            RankedUsageService.UsageEntry usage = rankedUsage.get(rankedKey(choice));
            if (usage != null) return uiText("pokemon_picker_ranked_usage",
                    formatRankedPercent(usage.usagePercent), formatRankedPercent(usage.winRate));
            return rankedUsageLoading ? uiText("pokemon_picker_ranked_loading") : choice.source;
        }
        String source;
        if (choice.position instanceof PCPosition pcPosition) {
            source = uiText("pokemon_picker_source_box_compact", pcPosition.getBox() + 1);
        } else if (choice.position instanceof PartyPosition) {
            source = uiText("pokemon_picker_source_party_compact");
        } else {
            source = choice.source;
        }
        String level = uiText("pokemon_picker_level_compact", choice.pokemon.getLevel());
        String identityPrefix = pokemonNickname(choice).isBlank() ? "" : choiceDisplayName(choice) + " · ";
        RankedUsageService.UsageEntry usage = rankedUsage.get(rankedKey(choice));
        return usage == null ? identityPrefix + source + " · " + level
                : identityPrefix + uiText("pokemon_picker_owned_ranked", source, level,
                        formatRankedPercent(usage.usagePercent));
    }

    private String formatRankedPercent(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private Text plannedNatureName(String rawNatureId) {
        String key = RankedUsageService.key(rawNatureId);
        return key.isBlank() ? ui("nature_unspecified") : Text.translatable("cobblemon.nature." + key);
    }

    private String evSummary(Map<String, Integer> evs) {
        if (evs == null || evs.isEmpty()) return uiText("evs_unspecified");
        List<String> parts = new ArrayList<>();
        for (String key : List.of("hp", "atk", "def", "spa", "spd", "spe")) {
            int value = evs.getOrDefault(key, 0);
            if (value > 0) parts.add(value + " " + uiText("ev_" + key));
        }
        return String.join(" / ", parts);
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

    private void drawCenteredPlainText(DrawContext context, Text text, int centerX, int y, int color) {
        context.drawText(textRenderer, text, centerX - textRenderer.getWidth(text) / 2, y, color, false);
    }

    private void drawScaledCenteredTextWithShadow(DrawContext context, Text text, int centerX, int y,
                                                   int color, float scale) {
        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, 1.0F);
        context.drawCenteredTextWithShadow(textRenderer, text,
                Math.round(centerX / scale), Math.round(y / scale), color);
        context.getMatrices().pop();
    }

    private void drawScaledRightAlignedTextWithShadow(DrawContext context, Text text, int rightX, int y,
                                                       int color, float scale) {
        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, 1.0F);
        int scaledRight = Math.round(rightX / scale);
        context.drawTextWithShadow(textRenderer, text, scaledRight - textRenderer.getWidth(text),
                Math.round(y / scale), color);
        context.getMatrices().pop();
    }

    private void drawMarqueeText(DrawContext context, Text text, int x, int y, int width, int color) {
        int textWidth = textRenderer.getWidth(text);
        if (textWidth <= width) {
            context.drawText(textRenderer, text, x, y, color, false);
            return;
        }

        int overflow = textWidth - width;
        long cycle = MARQUEE_PAUSE_MS * 2L + MARQUEE_TRAVEL_MS * 2L;
        long phase = net.minecraft.util.Util.getMeasuringTimeMs() % cycle;
        double progress;
        if (phase < MARQUEE_PAUSE_MS) {
            progress = 0.0D;
        } else if (phase < MARQUEE_PAUSE_MS + MARQUEE_TRAVEL_MS) {
            progress = (double) (phase - MARQUEE_PAUSE_MS) / MARQUEE_TRAVEL_MS;
        } else if (phase < MARQUEE_PAUSE_MS * 2L + MARQUEE_TRAVEL_MS) {
            progress = 1.0D;
        } else {
            progress = 1.0D - (double) (phase - MARQUEE_PAUSE_MS * 2L - MARQUEE_TRAVEL_MS)
                    / MARQUEE_TRAVEL_MS;
        }
        int offset = (int) Math.round(overflow * progress);

        scissor(context, x, y - 1, x + width, y + 10);
        context.drawText(textRenderer, text, x - offset, y, color, false);
        context.disableScissor();
    }

    private UUID parseUuid(String raw) {
        try { return UUID.fromString(raw); }
        catch (Exception ignored) { return null; }
    }

    private int cardX(int x, int index) {
        return x + (index % 3) * (CARD_WIDTH + CARD_GAP_X);
    }

    private int cardY(int y, int index) {
        return y + (index / 3) * (CARD_HEIGHT + CARD_GAP_Y);
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

    private int pickerWidth() {
        return Math.max(PANEL_WIDTH, Math.min(POKEMON_PICKER_MAX_WIDTH, width - 12));
    }

    private int pickerHeight() {
        return Math.max(PANEL_HEIGHT, Math.min(POKEMON_PICKER_MAX_HEIGHT, height - 12));
    }

    private int pickerLeft() {
        return (width - pickerWidth()) / 2;
    }

    private int pickerTop() {
        return (height - pickerHeight()) / 2;
    }

    private int pokemonPickerRows() {
        return Math.max(POKEMON_PICKER_MIN_ROWS, (pickerHeight() - 90) / POKEMON_PICKER_ROW_HEIGHT);
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
        if (ClientTeamApplier.isActive(this)) return;
        if (client != null) client.setScreen(standalone ? null : parent);
    }

    @Override
    public void close() {
        teamDrag.clear();
        if (ClientTeamApplier.isActive(this)) {
            continueApplyInBackground();
            return;
        }
        cancelRankedHelperRequest();
        rankedUsageRequest++;
        if (rankedUsageFuture != null) rankedUsageFuture.cancel(true);
        openBoxes();
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        if (teamDrag.team != null) page = selectedTeam / PAGE_SIZE;
        teamDrag.clear();
        teamDragEdgeTicks = 0;
        super.resize(client, width, height);
    }

    @Override
    public void removed() {
        teamDrag.clear();
        teamDragEdgeTicks = 0;
        super.removed();
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

    private record TeamAnalysis(int changes, int itemDifferences, int moveDifferences,
                                TeamValidationService.Report validation) {
    }

    private record DraftSlot(Pokemon pokemon, String pokemonId, String speciesId, String formId,
                             String itemId, String abilityId, List<String> moveIds,
                             String natureId, Map<String, Integer> evs, String source) {
        DraftSlot {
            moveIds = moveIds == null ? List.of() : List.copyOf(moveIds);
            evs = evs == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(evs));
        }
    }
    private record PokemonChoice(Pokemon pokemon, Species species, FormData form,
                                 StorePosition position, String source,
                                 boolean owned, List<AbilityChoice> abilities) {
    }

    private record RenderIdentity(Species species, Set<String> aspects, long catalogue) { }

    private record RankedLocalTemplate(Species species, FormData form) {
    }

    private record RankedReason(String partner, double affinity, double usagePercent, String style) {
    }

    private record AbilityChoice(String id, boolean hidden) {
    }

    private record AssociationOutcome(DraftSlot replacement, List<String> missingMoves,
                                      String plannedNatureId, boolean natureMismatch, boolean evMismatch) {
        int warningCount() {
            return missingMoves.size() + (natureMismatch ? 1 : 0) + (evMismatch ? 1 : 0);
        }
    }

    private enum MoveSource {
        ACTIVE, RESERVE, CATALOGUE
    }

    private enum MoveCheck {
        UNKNOWN, AVAILABLE, MISSING
    }

    private record MoveChoice(MoveTemplate template, MoveSource source) {
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
