package io.github.heathensoft.guide.core;

import org.lwjgl.Version;
import org.tinylog.Logger;

import static java.lang.System.nanoTime;

/**
 * Frederik Dahl 12/5/2024
 */
public class Engine {

    // Singleton class
    private static Engine instance;
    private Engine() { /* */ }
    public static Engine get() {
        if (instance == null) {
            instance = new Engine();
        } return instance;
    }

    private GLFWWindow window;
    private GLInfo info;
    private IGame game;
    private Time time;


    public void run(IGame game, String[] args) {
        if (this.game == null && game != null) {
            this.game = game;
            this.time = new Time();
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
            try { game.start(window.gameResolution());
            } catch (Exception e) {
                game.exit();
                window.terminate();
                return;
            }

            time.start();
            double alpha; // remainder of frame [0 -> 1)
            double fixed_time_step;
            double time_accumulator = 0.0;

            while (!window.shouldClose()) {
                fixed_time_step = 1.0 / window.targetUps();
                time.tick(); // 0nce every frame
                time_accumulator += time.frameTimeSeconds();
                while (time_accumulator >= fixed_time_step) {
                    if (!window.isMinimized()) {
                        // Todo: process input
                    }
                    game.update((float) fixed_time_step);
                    time.incrementUpsCounter();
                    time_accumulator -= fixed_time_step;
                } alpha = time_accumulator / fixed_time_step;

                if (!window.isMinimized()) {
                    if (window.shouldChangeGameResolution()) {
                        game.resize(window.gameResolution());
                    }

                }


            }

        }
    }

    public void exitMainLoop() {
        if (window != null) {
            window.signalToClose();
            Logger.info("window signaled to close");
        }
    }

    public Time time() { return time; }
    public GLInfo glInfo() { return info; }
    public GLFWWindow window() { return window; }
    public <T extends IGame> T game(Class<T> clazz) {
        if (game.getClass() != clazz) {
            throw new ClassCastException("wrong cast of application");
        } return clazz.cast(game);
    }

    public static final class Time {

        private static final double FRAME_TIME_MAX_SECONDS = 1 / 4.0; // 250 ms (15 frames of 60 fps)
        private double counter_time_accumulator;
        private double init_time_seconds;
        private double last_frame_seconds;
        private double frame_time_seconds;
        private int fps_counter;
        private int ups_counter;
        private int fps;
        private int ups;

        Time() { /* */ }

        void start() {
            init_time_seconds = systemTimeSeconds();
            last_frame_seconds = init_time_seconds;
        }

        void tick() {
            double time_seconds = systemTimeSeconds();
            double frame_time = time_seconds - last_frame_seconds;
            frame_time = Math.min(frame_time, FRAME_TIME_MAX_SECONDS);
            last_frame_seconds = time_seconds;
            counter_time_accumulator += frame_time;
            if (counter_time_accumulator > 1.0) {
                fps = fps_counter;
                ups = ups_counter;
                fps_counter = 0;
                ups_counter = 0;
                counter_time_accumulator -= 1.0;
            }
        }

        void incrementFpsCounter() { fps_counter++; }
        void incrementUpsCounter() { ups_counter++; }
        public int framesPerSecond() { return fps > 0 ? fps : fps_counter; }
        public int updatesPerSecond() { return ups > 0 ? ups : ups_counter; }
        public double frameTimeSeconds() { return frame_time_seconds; }
        public double systemTimeSeconds() { return nanoTime() / 1_000_000_000.0; }
        public double lastFrameSeconds() { return last_frame_seconds; }
        public double runTimeSeconds() { return systemTimeSeconds() - init_time_seconds; }

    }
}
