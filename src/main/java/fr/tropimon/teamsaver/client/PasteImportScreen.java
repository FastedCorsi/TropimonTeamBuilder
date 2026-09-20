package fr.tropimon.teamsaver.client;

import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.text.Text;

/** Dedicated modal keeps paste input and asynchronous responses out of the underlying PC UI. */
final class PasteImportScreen extends Screen {
    private final TeamManagerScreen parent;
    private EditBoxWidget input;
    private CompletableFuture<ShowdownPaste.Parsed> pending;
    private Text message = Text.empty();
    private boolean error;
    private int request;

    PasteImportScreen(TeamManagerScreen parent) {
        super(tr("paste_title"));
        this.parent = parent;
    }

    @Override protected void init() {
        String previous = input == null ? "" : input.getText();
        int w = panelWidth();
        int x = (width - w) / 2;
        int top = 22;
        int hintBottom = 46 + textRenderer.wrapLines(tr("paste_hint"), w - 20).size() * 9;
        int inputTop = Math.max(top + 43, hintBottom + 5);
        input = new EditBoxWidget(textRenderer, x + 10, inputTop, w - 20, Math.max(35, height - 89 - inputTop),
                tr("paste_hint"), tr("paste_title"));
        input.setMaxLength(ShowdownPaste.MAX_LENGTH);
        input.setText(previous);
        addDrawableChild(input);
        setInitialFocus(input);
        addDrawableChild(new PcStyleButton(x + 10, height - 40, (w - 28) / 3, 18,
                tr("paste_clipboard"), button -> {
                    String clipboard = client.keyboard.getClipboard();
                    if (clipboard.length() > ShowdownPaste.MAX_LENGTH) showError(new IllegalArgumentException("too_large"));
                    else input.setText(clipboard);
                }));
        addDrawableChild(new PcStyleButton(x + 14 + (w - 28) / 3, height - 40, (w - 28) / 3, 18,
                tr("paste_import"), button -> importPaste()));
        addDrawableChild(new PcStyleButton(x + 18 + 2 * ((w - 28) / 3), height - 40, (w - 28) / 3, 18,
                tr("cancel"), button -> close()));
    }

    private void importPaste() {
        if (pending != null && !pending.isDone()) return;
        String value = input.getText().strip();
        if (value.startsWith("https://") || value.startsWith("http://")) {
            try {
                String source = PokePasteService.rawUri(value).toString();
                boolean french = client.options.language.startsWith("fr");
                client.setScreen(new net.minecraft.client.gui.screen.ConfirmScreen(accepted -> {
                    client.setScreen(this);
                    if (accepted) importApprovedPaste(value);
                }, Text.literal(french ? "Télécharger ce Poképaste ?" : "Download this Poképaste?"),
                        Text.literal((french ? "Le texte de l'équipe sera téléchargé depuis " : "Team text will be downloaded from ") + source),
                        Text.literal(french ? "Télécharger" : "Download"), Text.literal(french ? "Annuler" : "Cancel")));
            } catch (IllegalArgumentException exception) { showError(exception); }
            return;
        }
        importApprovedPaste(value);
    }

    private void importApprovedPaste(String value) {
        int token = ++request;
        message = tr("paste_loading");
        error = false;
        try {
            pending = PokePasteService.load(value);
            pending.whenComplete((parsed, failure) -> client.execute(() -> {
                if (token != request || client.currentScreen != this) return;
                if (failure != null) { showError(failure); return; }
                try {
                    parent.importPaste(parsed);
                    client.setScreen(parent);
                } catch (IllegalArgumentException exception) { showError(exception); }
            }));
        } catch (IllegalArgumentException exception) { showError(exception); }
    }

    private void showError(Throwable failure) {
        while (failure.getCause() != null) failure = failure.getCause();
        error = true;
        String code = failure.getMessage();
        if (!java.util.Set.of("empty", "too_large", "multiple_teams", "too_many", "invalid_text",
                "duplicate_move", "too_many_moves", "invalid_evs", "invalid_url", "network",
                "unknown_species", "unknown_item", "unknown_ability", "unknown_nature", "unknown_move").contains(code == null ? "" : code)) {
            code = "network";
        }
        message = tr("paste_error_" + code);
    }

    @Override public void close() {
        request++;
        if (pending != null) pending.cancel(true);
        client.setScreen(parent);
    }

    @Override public void removed() {
        request++;
        if (pending != null) pending.cancel(true);
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int w = panelWidth();
        int x = (width - w) / 2;
        context.fill(x, 22, x + w, height - 16, 0xFF283238);
        context.fill(x + 2, 24, x + w - 2, 42, 0xFF3B464B);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 28, 0xFFFFFFFF);
        context.drawTextWrapped(textRenderer, tr("paste_hint"), x + 10, 46, w - 20, 0xFFBFEFFF);
        context.drawTextWrapped(textRenderer, message, x + 10, height - 79, w - 20,
                error ? 0xFFFF6B6B : 0xFFBFEFFF);
        for (var child : children()) {
            if (child instanceof net.minecraft.client.gui.Drawable drawable)
                drawable.render(context, mouseX, mouseY, delta);
        }
    }

    private int panelWidth() { return Math.min(430, width - 16); }
    @Override public boolean shouldPause() { return false; }
    private static Text tr(String key) { return Text.translatable("screen.tropimon_team_saver." + key); }
}
