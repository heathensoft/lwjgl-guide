package io.github.heathensoft.jagfw.core;

import io.github.heathensoft.jagfw.core.gfx.ShaderProgram;
import org.lwjgl.Version;
import org.tinylog.Logger;

import static io.github.heathensoft.jagfw.core.Game.State.*;
import static java.lang.System.nanoTime;

/**
 * The Engine is responsible for running the Game Object.
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
    private Game game;
    private Time time;

    public void run(Game game, String[] args) {
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
            try {
                window.initialize(boot_configuration);
                game.state = STARTING;
            } catch (Exception e) {
                Logger.error(e);
                Logger.debug("Game State: {}",game.state);
                game.state = TERMINATED;
                return;
            }
            info = new GLInfo(window.handle());
            Logger.debug("initialized window, starting game");
            /*
             *  Game start
             */
            try {
                game.start(window.gameResolution());
            } catch (Exception e) {
                Logger.error(e);
                Logger.debug("Game State: {}",game.state);
                game.state = EXITING;
                game.exit();
                window.terminate();
                game.state = TERMINATED;
                return;
            }
            try {
                time.start();
                double alpha;
                double fixed_time_step;
                double time_accumulator = 0.0;
                while (!window.shouldClose()) {
                    /*
                     *  Main Loop. Synced up with monitor refresh rate if v-sync is enabled.
                     */
                    fixed_time_step = 1.0 / window.targetUps();
                    time.tick();
                    time_accumulator += time.frameTimeSeconds();
                    //boolean process_input = true;
                    while (time_accumulator >= fixed_time_step) {
                        /*
                         *  Game update happens at a fixed interval of (window.targetUps()) / second
                         */
                        game.state = UPDATING;
                        if (!window.isMinimized()) {
                            window.processInput((float)fixed_time_step);
                        }
                        game.update((float)fixed_time_step);
                        time.incrementUpsCounter();
                        time_accumulator -= fixed_time_step;
                    }
                    // alpha: how close we were to the next game update
                    // We can use alpha when rendering, by projecting positions
                    // into the future based on current velocity
                    // As far as I know, this is not a very common technique
                    // But it can optionally be applied to help smoothen rendering
                    alpha = time_accumulator / fixed_time_step;
                    game.state = RENDERING;
                    if (!window.isMinimized()) {
                        if (window.shouldChangeGameResolution()) {
                            /*
                             *  Window found a better suited Game resolution.
                             *  Can only be one of the provided resolutions
                             *  (From BootConfiguration)
                             */
                            game.resize(window.gameResolution());
                        }
                        /*
                         *  Game render
                         * Todo: "alpha" as argument to game.render()
                         */
                        game.render((float)alpha);

                        /*
                         *  Swap the back and the front buffers in order to display
                         *  what has been rendered and begin rendering a new frame.
                         */
                        window.swapRenderBuffers();
                    }
                    /*
                     *  GLFW polls for any user events, triggering callbacks
                     */
                    window.pollUserEvents();
                    time.incrementFpsCounter();
                }
            } catch (Exception e) {
                Logger.error(e);
                Logger.debug("Game State: {}",game.state);
            } finally {
                Logger.debug("exiting game");
                game.state = EXITING;
                game.exit();
                Logger.debug("deleting shaders");
                ShaderProgram.deleteAllPrograms();
                Logger.debug("terminating window");
                window.terminate();
                game.state = TERMINATED;
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
    public Game game() { return game; }
    public <T extends Game> T game(Class<T> clazz) {
        if (game.getClass() != clazz) {
            throw new ClassCastException("");
        } return clazz.cast(game);
    }

    /** Engine Time Details */
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
        private long frame;

        Time() { /* */ }

        void start() {
            init_time_seconds = systemTimeSeconds();
            last_frame_seconds = init_time_seconds;
            frame = -1L;
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
            } frame++;
        }
        void incrementFpsCounter() { fps_counter++; }
        void incrementUpsCounter() { ups_counter++; }
        /** @return Average FPS over a 1-second time span */
        public int framesPerSecond() { return fps > 0 ? fps : fps_counter; }
        /** @return Average UPS over a 1-second time span */
        public int updatesPerSecond() { return ups > 0 ? ups : ups_counter; }
        /** @return duration of the last frame in seconds */
        public double frameTimeSeconds() { return frame_time_seconds; }
        public double systemTimeSeconds() { return nanoTime() / 1_000_000_000.0; }
        /** @return time spent in the main loop in seconds (game run time) */
        public double runTimeSeconds() { return systemTimeSeconds() - init_time_seconds; }
        /** @return the current frame (frame increments each iteration of the main loop) */
        public long frame() { return frame; }
    }
}
