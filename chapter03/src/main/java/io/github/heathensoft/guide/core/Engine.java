package io.github.heathensoft.guide.core;

import org.lwjgl.Version;
import org.tinylog.Logger;

/**
 * Frederik Dahl 12/5/2024
 */
public class Engine {


    private GLFWWindow window;
    private GLInfo info;
    private IGame game;


    public void run(IGame game, String[] args) {
        if (this.game == null && game != null) {
            this.game = game;
            this.window = new GLFWWindow();
            BootConfiguration boot_configuration = new BootConfiguration();
            org.tinylog.configuration.Configuration.set("writer","console");
            org.tinylog.configuration.Configuration.set("writer.format","{date: HH:mm:ss.SS} {level}: {message}");
            game.configure(boot_configuration,args);
            int memory = (int)(Runtime.getRuntime().maxMemory() / 1000000L);
            int processors = Runtime.getRuntime().availableProcessors();
            String os_name = System.getProperty("os.name");
            String os_arch = System.getProperty("os.arch");
            String os_version = System.getProperty("os.version");
            Logger.info("welcome");
            Logger.info("running on: {} version {}, {} platform", os_name,os_version,os_arch);
            Logger.info("java version: {}", System.getProperty("java.version"));
            Logger.info("lwjgl version: {}", Version.getVersion());
            Logger.info("reserved memory: {}MB", memory);
            Logger.info("available processors: {}", processors);
            try {
                Logger.info("initializing GLFWWindow");
                window.initialize(boot_configuration);
            } catch (Exception e) {
                Logger.error(e);
                return;
            }
            info = new GLInfo(window.handle());


        }
    }
}
