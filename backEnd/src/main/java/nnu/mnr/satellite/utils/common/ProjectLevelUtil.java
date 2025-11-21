package nnu.mnr.satellite.utils.common;

/**
 * Utility helpers for handling project level flags (scene/cube).
 */
public final class ProjectLevelUtil {

    public static final String LEVEL_SCENE = "scene";
    public static final String LEVEL_CUBE = "cube";

    private ProjectLevelUtil() {
    }

    /**
     * Normalize level input. Defaults to scene when value is blank/unknown.
     */
    public static String normalize(String level) {
        if (level == null) {
            return LEVEL_SCENE;
        }
        String normalized = level.trim().toLowerCase();
        if (LEVEL_CUBE.equals(normalized)) {
            return LEVEL_CUBE;
        }
        // keep compatibility for uppercase or unexpected inputs
        if (LEVEL_SCENE.equals(normalized)) {
            return LEVEL_SCENE;
        }
        return LEVEL_SCENE;
    }
}
