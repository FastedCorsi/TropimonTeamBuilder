package fr.tropimon.teamsaver.client;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.Test;

final class ShowdownPasteTest {
    @Test void parsesNicknamesGenderFormsMovesAndZeroEvs() {
        var parsed = ShowdownPaste.parse("""
                === [gen9ou] Test import ===
                Sam (Samurott-Hisui) (M) @ Assault Vest
                Ability: Sharpness
                EVs: 252 HP / 252 Atk / 0 SpA / 4 SpD
                Adamant Nature
                IVs: 0 SpA
                Tera Type: Water
                - Ceaseless Edge
                - Aqua Jet
                - Knock Off
                - Sacred Sword
                """);
        var pokemon = parsed.members().getFirst();
        assertEquals("Test import", parsed.name());
        assertEquals("Samurott-Hisui", pokemon.species());
        assertEquals("Assault Vest", pokemon.item());
        assertEquals("Sharpness", pokemon.ability());
        assertEquals("adamant", pokemon.nature());
        assertEquals(252, pokemon.evs().get("atk"));
        assertEquals(0, pokemon.evs().get("spa"));
        assertEquals("ceaselessedge", pokemon.moves().getFirst());
        assertTrue(parsed.ignoredFields().containsAll(List.of("Nickname", "IVs", "Tera Type")));
    }

    @Test void parsesUserReferenceSixPokemon() throws Exception {
        try (var stream = getClass().getResourceAsStream("/pastes/df00cbb92bd55558.txt")) {
            assertNotNull(stream);
            var parsed = ShowdownPaste.parse(new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            assertEquals(6, parsed.members().size());
            assertEquals("Samurott-Hisui", parsed.members().get(4).species());
            assertEquals(0, parsed.members().getLast().evs().getOrDefault("atk", 0));
            assertTrue(parsed.ignoredFields().contains("IVs"));
            assertTrue(parsed.ignoredFields().contains("Tera Type"));
        }
    }

    @Test void rejectsIncompleteOrOversizedMalformedTeams() {
        for (String text : List.of("", "<html>error</html>", "Glimmora\nEVs: 253 SpA", "Glimmora\nEVs: 252 HP / 252 Atk / 252 Spe",
                "Glimmora\n- Protect\n- Protect", "Glimmora\n- A\n- B\n- C\n- D\n- E",
                "Glimmora\nEVs: -1 HP", "Glimmora\nEVs: 4 HP / 4 HP", "Glimmora\n\n".repeat(7),
                "x".repeat(ShowdownPaste.MAX_LENGTH + 1))) {
            assertThrows(IllegalArgumentException.class, () -> ShowdownPaste.parse(text), text.substring(0, Math.min(50, text.length())));
        }
    }

    @Test void onlyExplicitHttpsPokepasteLinksAreFetched() {
        assertEquals(URI.create("https://pokepast.es/df00cbb92bd55558/raw"),
                PokePasteService.rawUri("https://pokepast.es/df00cbb92bd55558"));
        assertEquals(URI.create("https://pokepast.es/df00cbb92bd55558/raw"),
                PokePasteService.rawUri("https://pokepast.es/df00cbb92bd55558/raw"));
        for (String url : List.of("http://pokepast.es/df00cbb92bd55558", "https://localhost/df00cbb92bd55558",
                "https://pokepast.es.evil/df00cbb92bd55558", "https://user@pokepast.es/df00cbb92bd55558",
                "https://pokepast.es:8443/df00cbb92bd55558", "file:///etc/passwd", "https://pokepast.es/../secret",
                "https://pokepast.es/df00cbb92bd55558?redirect=other")) {
            assertThrows(IllegalArgumentException.class, () -> PokePasteService.rawUri(url));
        }
    }

    @Test void bodyLimitCancelsStreamAndCancellationBeforeSubscribeIsSafe() {
        PokePasteService.BoundedBody body = new PokePasteService.BoundedBody();
        boolean[] cancelled = {false};
        Flow.Subscription subscription = new Flow.Subscription() {
            @Override public void request(long count) { }
            @Override public void cancel() { cancelled[0] = true; }
        };
        body.onSubscribe(subscription);
        body.onNext(List.of(ByteBuffer.wrap(new byte[ShowdownPaste.MAX_LENGTH + 1])));
        assertTrue(cancelled[0]);
        assertTrue(body.getBody().toCompletableFuture().isCompletedExceptionally());
        cancelled[0] = false;
        PokePasteService.BoundedBody cancelledBody = new PokePasteService.BoundedBody();
        cancelledBody.cancel();
        cancelledBody.onSubscribe(subscription);
        assertTrue(cancelled[0]);
    }
}
