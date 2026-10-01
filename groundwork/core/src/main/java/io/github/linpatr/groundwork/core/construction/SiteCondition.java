package io.github.linpatr.groundwork.core.construction;

/** Whether a single position can receive a building block. */
public enum SiteCondition {
    /** Empty or trivially replaceable (air, grass, water...). */
    CLEAR,
    /** Occupied by something that would have to be removed first. */
    OBSTRUCTED,
    /** Outside the world's build height or border. */
    OUT_OF_BOUNDS,
    /** In a chunk that is not loaded, so its contents are unknown. */
    UNLOADED
}
