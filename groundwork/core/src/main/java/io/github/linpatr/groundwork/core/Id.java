package io.github.linpatr.groundwork.core;

import java.util.Objects;

/**
 * A namespaced identifier such as {@code minecraft:iron_ingot} or {@code groundwork:smelter}.
 *
 * <p>Follows the same character rules as Minecraft resource locations so that values can be
 * converted to and from the game's identifiers without loss, while keeping the core free of any
 * Minecraft dependency.
 */
public record Id(String namespace, String path) implements Comparable<Id> {
    public static final String DEFAULT_NAMESPACE = "minecraft";

    public Id {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (namespace.isEmpty() || !namespace.chars().allMatch(Id::isNamespaceChar)) {
            throw new IllegalArgumentException("Invalid namespace in identifier: " + namespace + ":" + path);
        }
        if (path.isEmpty() || !path.chars().allMatch(Id::isPathChar)) {
            throw new IllegalArgumentException("Invalid path in identifier: " + namespace + ":" + path);
        }
    }

    /** Parses {@code namespace:path}; a missing namespace defaults to {@code minecraft}. */
    public static Id parse(String value) {
        Objects.requireNonNull(value, "value");
        int colon = value.indexOf(':');
        return colon < 0
                ? new Id(DEFAULT_NAMESPACE, value)
                : new Id(value.substring(0, colon), value.substring(colon + 1));
    }

    public static Id of(String namespace, String path) {
        return new Id(namespace, path);
    }

    private static boolean isNamespaceChar(int c) {
        return c == '_' || c == '-' || c == '.' || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
    }

    private static boolean isPathChar(int c) {
        return isNamespaceChar(c) || c == '/';
    }

    @Override
    public int compareTo(Id other) {
        int byNamespace = namespace.compareTo(other.namespace);
        return byNamespace != 0 ? byNamespace : path.compareTo(other.path);
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
