package io.github.linpatr.groundwork.core.building;

/** Thrown when a building definition is malformed. The message is meant for content authors. */
public class DefinitionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DefinitionException(String message) {
        super(message);
    }

    public DefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
