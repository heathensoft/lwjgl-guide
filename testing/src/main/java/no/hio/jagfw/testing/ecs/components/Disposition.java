package no.hio.jagfw.testing.ecs.components;

/**
 * Frederik Dahl 2/27/2025
 */
public enum Disposition {
    FRIENDLY,   // Can be hit by Neutral and Hostile
    NEUTRAL,    // Can be hit by All
    HOSTILE;     // Can be hit by Neutral and Friendly
    public boolean isValidTarget(Disposition source) {
        switch (source) {
            case FRIENDLY -> { return this == HOSTILE || this == NEUTRAL; }
            case HOSTILE -> { return this == FRIENDLY || this == NEUTRAL; }
            case NEUTRAL -> { return true; }
        } return false;
    }
}
