package com.vadim.maze.ui;

import com.vadim.maze.configuration.LearningProperties;
import com.vadim.maze.configuration.MazeProperties;
import com.vadim.maze.configuration.RewardProperties;
import com.vadim.maze.service.MazeService;
import com.vadim.maze.service.MazeValidator;
import com.vadim.maze.service.TrainingService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ConfigurableApplicationContext;

/** JavaFX-приложение; бины берёт из Spring-контекста, поднятого в {@code main}. */
public class MazeFxApplication extends Application {

    private static ConfigurableApplicationContext context;

    public static void setContext(ConfigurableApplicationContext applicationContext) {
        context = applicationContext;
    }

    @Override
    public void start(Stage stage) {
        MazeWindow window = new MazeWindow(stage,
                context.getBean(MazeService.class),
                context.getBean(MazeValidator.class),
                context.getBean(TrainingService.class),
                context.getBean(MazeProperties.class),
                context.getBean(RewardProperties.class),
                context.getBean(LearningProperties.class));
        stage.setTitle("Мышь в лабиринте — обучение с подкреплением (Q-learning)");
        stage.setScene(new Scene(window.build(), 1400, 860));
        stage.show();
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
        Platform.exit();
    }
}
