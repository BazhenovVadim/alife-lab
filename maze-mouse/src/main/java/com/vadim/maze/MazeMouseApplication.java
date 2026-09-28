package com.vadim.maze;

import com.vadim.maze.configuration.AppProperties;
import com.vadim.maze.ui.MazeFxApplication;
import javafx.application.Application;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Задача 1.11 «Мышь в лабиринте».
 * Поднимает Spring-контекст, затем — окно JavaFX (или консольный прогон при {@code --app.mode=console}).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MazeMouseApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(MazeMouseApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
        if (context.getBean(AppProperties.class).getMode() == AppProperties.Mode.GUI) {
            MazeFxApplication.setContext(context);
            Application.launch(MazeFxApplication.class, args);
        } else {
            context.close();
        }
    }
}
