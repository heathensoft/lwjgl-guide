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
            Logger.debug("running on: {} version {}, {} platform", os_name,os_version,os_arch);
            Logger.debug("java version: {}", System.getProperty("java.version"));
            Logger.debug("lwjgl version: {}", Version.getVersion());
            Logger.debug("reserved memory: {}MB", memory);
            Logger.debug("available processors: {}", processors);
            /*
             *  Initialize Window
             */
            try { window.initialize(boot_configuration);
            } catch (Exception e) {
                Logger.error(e);
                return;
            }
            info = new GLInfo(window.handle());
            Logger.debug("initialized window, starting game");
            /*
             *  Game start
             */
            try { game.start(window.gameResolution());
            } catch (Exception e) {
                Logger.error(e);
                game.exit();
                window.terminate();
                return;
            }
            try {
                time.start();
                double fixed_time_step;
                double time_accumulator = 0.0;
                while (!window.shouldClose()) {
                    /*
                     *  Main Loop. Synced up with monitor refresh rate if v-sync is enabled.
                     */
                    fixed_time_step = 1.0 / window.targetUps();
                    time.tick();
                    time_accumulator += time.frameTimeSeconds();
                    while (time_accumulator >= fixed_time_step) {
                        /*
                         *  Game update happens at a fixed interval of (window.targetUps()) / second
                         */
                        // Todo: process input

                        game.update((float) fixed_time_step);
                        time.incrementUpsCounter();
                        time_accumulator -= fixed_time_step;
                    } if (!window.isMinimized()) {
                        if (window.shouldChangeGameResolution()) {
                            /*
                             *  Window found a better suited Game resolution.
                             *  Can only be one of the provided resolutions (BootConfiguration)
                             */
                            game.resize(window.gameResolution());
                        }
                        /*
                         *  Game render
                         */
                        game.render();

                        /*
                         *  Swap the back and the front buffers in order to display
                         *  what has been rendered and begin rendering a new frame.
                         */
                        window.swapRenderBuffers();
                    }
                    /*
                     *  GLFW polls for any user events, triggering callbacks
                     */
                    window.processUserEvents();
                    time.incrementFpsCounter();
                }
            } catch (Exception e) {
                Logger.error(e);
            } finally {
                Logger.debug("exiting game");
                game.exit();
                Logger.debug("terminating window");
                window.terminate();
            }
        }
    }

    public void exitMainLoop() {
        if (window != null) {
            window.signalToClose();
        }
    }

    public Time time() { return time; }
    public GLInfo glInfo() { return info; }
    public GLFWWindow window() { return window; }
    public <T extends IGame> T game(Class<T> clazz) {
        if (game.getClass() != clazz) {
            throw new ClassCastException("");
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
            frame_time_seconds = time_seconds - last_frame_seconds;
            frame_time_seconds = Math.min(frame_time_seconds,FRAME_TIME_MAX_SECONDS);
            last_frame_seconds = time_seconds;
            counter_time_accumulator += frame_time_seconds;
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
