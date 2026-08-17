package fr.tropimon.teamsaver.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

final class PcStyleButton extends ButtonWidget {
    private static final Identifier RELEASE_TEXTURE = Identifier.of("cobblemon", "textures/gui/pc/pc_release_button.png");
    private static final int TEXTURE_WIDTH = 58;
    private static final int TEXTURE_HEIGHT = 32;
    private static final int SOURCE_HEIGHT = 16;
    private static final int CAP_WIDTH = 4;

    enum Style { NORMAL, SELECTED, DANGER }

    private final Style style;
    private boolean scrollingText;

    PcStyleButton(int x, int y, int width, int height, Text message, PressAction action) {
        this(x, y, width, height, message, action, Style.NORMAL);
    }

    PcStyleButton(int x, int y, int width, int height, Text message, PressAction action, Style style) {
        super(x, y, width, height, message, action, DEFAULT_NARRATION_SUPPLIER);
        this.style = style;
    }

    PcStyleButton withScrollingText() {
        this.scrollingText = true;
        return this;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        drawFrame(context, getX(), getY(), width, height, style, isHovered(), active);

        int color = !active ? 0xFF7D8588
                : style == Style.DANGER && isHovered() ? 0xFFFFC5C5 : 0xFFFFFFFF;
        var renderer = MinecraftClient.getInstance().textRenderer;
        int textWidth = renderer.getWidth(getMessage());
        int availableWidth = width - 8;
        int textY = getY() + (height - 8) / 2;
        if (scrollingText && textWidth > availableWidth) {
            int travel = textWidth - availableWidth;
            long phase = (System.currentTimeMillis() / 45L) % Math.max(1, (travel + 18L) * 2L);
            int offset = (int) (phase <= travel + 18L ? Math.max(0L, phase - 9L) :
                    Math.max(0L, (travel + 18L) * 2L - phase - 9L));
            offset = Math.min(travel, offset);
            context.enableScissor(getX() + 4, getY() + 1, getX() + width - 4, getY() + height - 1);
            context.drawTextWithShadow(renderer, getMessage(), getX() + 4 - offset, textY, color);
            context.disableScissor();
        } else {
            context.drawCenteredTextWithShadow(renderer, getMessage(), getX() + width / 2, textY, color);
        }
    }

    static void drawFrame(DrawContext context, int x, int y, int width, int height,
                          Style style, boolean hovered, boolean active) {
        int sourceY = hovered && style != Style.SELECTED ? SOURCE_HEIGHT : 0;
        int cap = Math.min(CAP_WIDTH, width / 2);
        int middleWidth = Math.max(0, width - cap * 2);
        int sourceMiddleWidth = TEXTURE_WIDTH - CAP_WIDTH * 2;

        context.drawTexture(RELEASE_TEXTURE, x, y, cap, height,
                0, sourceY, CAP_WIDTH, SOURCE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        if (middleWidth > 0) {
            context.drawTexture(RELEASE_TEXTURE, x + cap, y, middleWidth, height,
                    CAP_WIDTH, sourceY, sourceMiddleWidth, SOURCE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
        context.drawTexture(RELEASE_TEXTURE, x + width - cap, y, cap, height,
                TEXTURE_WIDTH - CAP_WIDTH, sourceY, CAP_WIDTH, SOURCE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        if (style == Style.SELECTED) {
            context.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0x99266376);
            context.fill(x + 2, y + 2, x + width - 2, y + 3, 0xFF9CE8F2);
            context.fill(x + 2, y + height - 3, x + width - 2, y + height - 2, 0xFF163D47);
        }
        if (!active) {
            context.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0x880C1113);
        }
    }
}
