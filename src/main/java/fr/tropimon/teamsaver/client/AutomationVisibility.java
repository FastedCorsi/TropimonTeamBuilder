package fr.tropimon.teamsaver.client;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AutomationVisibility {
    private static final Set<UUID> HIDDEN_ENTITIES = ConcurrentHashMap.newKeySet();

    private AutomationVisibility() {
    }

    static void hide(UUID entityId) {
        if (entityId != null) HIDDEN_ENTITIES.add(entityId);
    }

    static void show(UUID entityId) {
        if (entityId != null) HIDDEN_ENTITIES.remove(entityId);
    }

    public static boolean isHidden(UUID entityId) {
        return entityId != null && HIDDEN_ENTITIES.contains(entityId);
    }

    public static boolean isAutomationActive() {
        return !HIDDEN_ENTITIES.isEmpty();
    }
}
