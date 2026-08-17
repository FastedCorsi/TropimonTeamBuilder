package fr.tropimon.teamsaver.client;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.gui.pc.PCGUI;
import com.cobblemon.mod.common.client.gui.pc.PCGUIConfiguration;
import com.cobblemon.mod.common.client.storage.ClientPC;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;

public final class TropimonTeamSaverClient implements ClientModInitializer {
    static final Logger LOGGER = LoggerFactory.getLogger("tropimon_team_saver");
    private static boolean openTeamsAfterRemotePc;
    private static int remotePcTimeout;
    private static String autoApplyTeamId;
    private static final KeyBinding OPEN_TEAMS = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.tropimon_team_saver.open_teams",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "category.tropimon_team_saver"
    ));

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof PCGUI pcGui) || pcGui.getConfiguration().getSelectOverride() != null) return;
            if (openTeamsAfterRemotePc) {
                openTeamsAfterRemotePc = false;
                remotePcTimeout = 0;
                String teamToApply = autoApplyTeamId;
                autoApplyTeamId = null;
                client.execute(() -> {
                    if (client.currentScreen == pcGui) {
                        client.setScreen(new TeamManagerScreen(pcGui, false, teamToApply));
                    }
                });
                return;
            }
            int left = (scaledWidth - PCGUI.BASE_WIDTH) / 2;
            int top = (scaledHeight - PCGUI.BASE_HEIGHT) / 2;
            int y = Math.max(1, top - 12);
            PcStyleButton boxes = new PcStyleButton(left + 263, y, 40, 16,
                    Text.translatable("screen.tropimon_team_saver.boxes"), ignored -> {}, PcStyleButton.Style.SELECTED);
            PcStyleButton teams = new PcStyleButton(left + 305, y, 40, 16,
                    Text.translatable("text.tropimon_team_saver.open"), ignored -> {
                        TeamManagerScreen manager = new TeamManagerScreen(pcGui);
                        client.setScreen(manager);
            });
            Screens.getButtons(screen).add(boxes);
            Screens.getButtons(screen).add(teams);
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientTeamApplier.tick();
            if (openTeamsAfterRemotePc && remotePcTimeout > 0 && --remotePcTimeout == 0) {
                openTeamsAfterRemotePc = false;
                autoApplyTeamId = null;
                if (client.player != null) {
                    client.player.sendMessage(Text.translatable(
                            "screen.tropimon_team_saver.error_remote_pc_failed"), true);
                }
                if (client.currentScreen == null) openOutsidePc(client);
            }
            while (OPEN_TEAMS.wasPressed()) {
                if (client.currentScreen == null && !requestRemotePc(null)) {
                    openOutsidePc(client);
                }
            }
        });
        LOGGER.info("Tropimon Team Manager client-only initialized");
    }

    static boolean requestRemotePcForApply(String teamId) {
        MinecraftClient client = MinecraftClient.getInstance();
        return requestRemotePc(teamId);
    }

    /**
     * Opens Cobblemon's real server-authorized PC first. Screen replacement does
     * not call PCGUI.closeNormally(), so the PCLink remains alive while Teams is
     * displayed and subsequent storage packets are accepted by the server.
     */
    private static boolean requestRemotePc(String teamId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.getNetworkHandler() == null
                || openTeamsAfterRemotePc || !hasRemotePcPermission(client)) return false;
        openTeamsAfterRemotePc = true;
        remotePcTimeout = 120;
        autoApplyTeamId = teamId;
        if (teamId != null) client.setScreen(null);
        client.player.sendMessage(Text.translatable(
                "screen.tropimon_team_saver.status_opening_remote_pc"), true);
        client.getNetworkHandler().sendChatCommand("pc");
        return true;
    }

    private static boolean hasRemotePcPermission(MinecraftClient client) {
        return client.getNetworkHandler() != null
                && client.getNetworkHandler().getCommandDispatcher().getRoot().getChild("pc") != null;
    }

    private static void openOutsidePc(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        var storage = CobblemonClient.INSTANCE.getStorage();
        var party = storage.getParty();
        ClientPC pc = storage.getPcStores().get(party.getUuid());
        if (pc == null) pc = storage.getPcStores().get(client.player.getUuid());
        if (pc == null && storage.getPcStores().size() == 1) {
            pc = storage.getPcStores().values().iterator().next();
        }
        if (pc == null) {
            client.player.sendMessage(Text.translatable("screen.tropimon_team_saver.error_pc_not_loaded"), true);
            return;
        }
        PCGUI virtualParent = new PCGUI(pc, party, new PCGUIConfiguration(), 0, new HashSet<>());
        client.setScreen(new TeamManagerScreen(virtualParent, true));
    }
}
