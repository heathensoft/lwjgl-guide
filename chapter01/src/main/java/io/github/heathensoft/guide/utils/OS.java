package io.github.heathensoft.guide.utils;

/**
 * Frederik Dahl 12/1/2024
 */
public final class OS {
    public enum NAME { WINDOWS, LINUX, MAC, SOLARIS, UNDEFINED }
    public static final NAME name;
    static { String system = System.getProperty("os.name").toLowerCase();
        if (system.contains("win")) { name = NAME.WINDOWS;
        } else if (system.contains("nix") || system.contains("nux") || system.contains("aix")) { name = NAME.LINUX;
        } else if (system.contains("mac")) { name = NAME.MAC;
        } else if (system.contains("sunos")) { name = NAME.SOLARIS;
        } else name = NAME.UNDEFINED;
    }
}
