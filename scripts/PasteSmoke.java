package fr.tropimon.teamsaver.client;

import java.util.concurrent.TimeUnit;

/** Optional live HTTP smoke test. Not packaged in the mod and not run by the unit suite. */
public final class PasteSmoke {
    public static void main(String[] args) throws Exception {
        var paste = PokePasteService.load("https://pokepast.es/df00cbb92bd55558").get(25, TimeUnit.SECONDS);
        if (paste.members().size() != 6) throw new AssertionError("Expected six Pokémon");
        if (!paste.members().get(4).species().equals("Samurott-Hisui")) throw new AssertionError("Form lost");
        for (var member : paste.members()) {
            if (member.moves().size() != 4 || member.ability().isBlank() || member.item().isBlank()) {
                throw new AssertionError("Incomplete imported set: " + member.species());
            }
        }
        System.out.println("Live Poképaste import: six complete sets, including Hisuian form; OK.");
    }
}
