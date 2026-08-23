package fr.tropimon.teamsaver.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

final class LocalCobblemonLearnsetIndexTest {
    @Test
    void readsEveryObtainableLearnMethodIncludingTms() {
        assertEquals("earthpower", LocalCobblemonLearnsetIndex.parseMoveSpec("39:earthpower"));
        assertEquals("icebeam", LocalCobblemonLearnsetIndex.parseMoveSpec("tm:icebeam"));
        assertEquals("yawn", LocalCobblemonLearnsetIndex.parseMoveSpec("egg:yawn"));
        assertEquals("dracometeor", LocalCobblemonLearnsetIndex.parseMoveSpec("tutor:dracometeor"));
    }

    @Test
    void excludesMovesThatCannotNormallyBeObtainedInGame() {
        assertNull(LocalCobblemonLearnsetIndex.parseMoveSpec("legacy:return"));
        assertNull(LocalCobblemonLearnsetIndex.parseMoveSpec("special:celebrate"));
        assertNull(LocalCobblemonLearnsetIndex.parseMoveSpec("invalid"));
    }
}
