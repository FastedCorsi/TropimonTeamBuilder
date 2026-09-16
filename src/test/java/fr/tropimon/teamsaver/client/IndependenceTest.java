package fr.tropimon.teamsaver.client;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Architectural guard: this module must keep working with other Tropimon mods absent. */
final class IndependenceTest {
    @Test void noOtherTropimonPackageOrReflectiveBridgeInProduction() throws Exception {
        Pattern otherPackage = Pattern.compile("fr[./]tropimon[./](?!teamsaver[./;])");
        try (var files = Files.walk(Path.of("src/main"))) {
            for (Path file : files.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".java")).toList()) {
                String code = Files.readString(file);
                assertFalse(otherPackage.matcher(code).find(), file.toString());
                assertFalse(code.contains("Class.forName"), file.toString());
                assertFalse(code.contains("getDeclaredField"), file.toString());
                assertFalse(code.contains("getAllMods()"), file.toString());
            }
        }
    }

    @Test void dependenciesAreOfficialAndTheLearnsetResourceIsOwnedByThisMod() throws Exception {
        var manifest = JsonParser.parseString(Files.readString(Path.of("src/main/resources/fabric.mod.json"))).getAsJsonObject();
        assertEquals(Set.of("fabricloader", "minecraft", "fabric-api", "cobblemon"), manifest.getAsJsonObject("depends").keySet());
        try (var stream = getClass().getResourceAsStream("/assets/tropimon_team_saver/data/learnset_supplements.json")) {
            assertNotNull(stream);
            var supplements = JsonParser.parseString(new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals(9, supplements.size());
            for (var entry : supplements.entrySet()) {
                assertTrue(entry.getKey().startsWith("cobblemon:"));
                assertEquals(Set.of("forms", "moves"), entry.getValue().getAsJsonObject().keySet());
            }
        }
    }

    @Test void renderingNeverAnalyzesOrSavesTeamDoctorData() throws Exception {
        String code = Files.readString(Path.of("src/main/java/fr/tropimon/teamsaver/client/TeamManagerScreen.java"));
        String doctorRender = code.substring(code.indexOf("private void renderTeamDoctor("), code.indexOf("private List<TeamDoctor.Member> teamDoctorMembers()"));
        assertFalse(doctorRender.contains("TeamDoctor.analyze"));
        assertFalse(doctorRender.contains("teamDoctorMembers()"));
        assertFalse(doctorRender.contains("repository."));
        String browserRender = code.substring(code.indexOf("private void renderBrowser("), code.indexOf("private void drawBrowserCardBase("));
        assertFalse(browserRender.contains("analysis(team)"));
        assertFalse(browserRender.contains("validate("));
    }
}
