package fr.tropimon.teamsaver.client;

import java.util.Objects;
import java.util.function.Supplier;

/** One immutable dependency key, one derived value. Never used by transfer preflights. */
final class ReadModelCache<K, V> {
    private K key;
    private V value;
    private boolean populated;

    V get(K dependencies, Supplier<V> compute) {
        if (!populated || !Objects.equals(key, dependencies)) {
            V next = compute.get(); // Failed computations must not poison the cache.
            key = dependencies;
            value = next;
            populated = true;
        }
        return value;
    }

    void clear() {
        populated = false;
        key = null;
        value = null;
    }
}
