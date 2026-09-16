package fr.tropimon.teamsaver.client;

import java.util.concurrent.atomic.AtomicLong;

/** Passive observation of official Cobblemon updates. No packets, delays or automation state. */
public final class ClientDataRevision {
    private static final AtomicLong STORAGE = new AtomicLong();
    private static final AtomicLong POKEMON = new AtomicLong();
    private static final AtomicLong CATALOGUE = new AtomicLong();

    private ClientDataRevision() { }

    public static void storageChanged() { STORAGE.incrementAndGet(); }
    public static void pokemonChanged() { POKEMON.incrementAndGet(); }
    public static void catalogueChanged() { CATALOGUE.incrementAndGet(); }
    static long storage() { return STORAGE.get(); }
    static long pokemon() { return POKEMON.get(); }
    static long catalogue() { return CATALOGUE.get(); }
}
