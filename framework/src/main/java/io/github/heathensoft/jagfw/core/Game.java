package io.github.heathensoft.jagfw.core;

/**
 * Frederik Dahl 12/5/2024
 */
public abstract class Game {


    protected State state = State.UNINITIALIZED;

    /** Engine Game State */
    public enum State {
        /** Default state (prior to starting)*/
        UNINITIALIZED,
        /** Window class is initialized and the game is starting */
        STARTING,
        /** Currently processing input and updating the game (fixed time step)*/
        UPDATING,
        /** Currently rendering the game.
         * Either at monitor refresh-rate (vsync)
         * or uncapped FPS. If the window is minimized,
         * The game will not render, but the state is still
         * set to RENDERING*/
        RENDERING,
        /** Main loop is exited. Game about to exit */
        EXITING,
        /** Engine finished executing */
        TERMINATED
    }

    /**
     * @return current game state.
     * @see State
     */
    public State getState() { return state; }

    protected abstract void configure(BootConfiguration boot_config, String[] args);

    protected abstract void start(Resolution resolution) throws Exception;

    protected abstract void resize(Resolution resolution);

    protected abstract void update(float delta_time);

    protected abstract void render();

    protected abstract void exit();
}
