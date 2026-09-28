package com.vadim.maze.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Общие параметры приложения (секция {@code app}). */
@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    public enum Mode { GUI, CONSOLE }

    /** GUI — окно JavaFX, CONSOLE — обучение и вывод в терминал. */
    private Mode mode = Mode.GUI;
}
