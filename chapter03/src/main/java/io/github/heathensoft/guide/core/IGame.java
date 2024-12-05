package io.github.heathensoft.guide.core;

/**
 * Frederik Dahl 12/5/2024
 */
public interface IGame {

    void configure(BootConfiguration boot_config, String[] args);

    void start(Resolution resolution) throws Exception;

    void resize(Resolution resolution);

    void update(float delta_time);

    void render();

    void exit();
}
