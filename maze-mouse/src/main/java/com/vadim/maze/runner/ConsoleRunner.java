package com.vadim.maze.runner;

import com.vadim.maze.configuration.LearningProperties;
import com.vadim.maze.model.CellType;
import com.vadim.maze.model.Maze;
import com.vadim.maze.model.Position;
import com.vadim.maze.model.StepResult;
import com.vadim.maze.model.TrainingSummary;
import com.vadim.maze.service.MazeService;
import com.vadim.maze.service.TrainingService;
import com.vadim.maze.service.TrainingSession;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Консольный режим: обучение без GUI и печать найденного пути. {@code --app.mode=console} */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app", name = "mode", havingValue = "console")
public class ConsoleRunner implements CommandLineRunner {

    private final MazeService mazeService;
    private final TrainingService trainingService;
    private final LearningProperties learning;

    @Override
    public void run(String... args) {
        Maze maze = mazeService.initialMaze();
        System.out.println("Лабиринт " + maze.getWidth() + "x" + maze.getHeight() + ":");
        System.out.println(maze.toText());

        TrainingSession session = trainingService.createSession(maze);
        int reportEvery = Math.max(1, learning.getEpisodes() / 10);
        trainingService.train(session, learning.getEpisodes(), stats -> {
            if (stats.getEpisode() % reportEvery == 0) {
                TrainingSummary s = trainingService.summarize(session, reportEvery);
                System.out.printf("Эпизод %5d | ε=%.3f | награда %8.1f | шагов %6.1f | сыр %3.0f%% | ток %.2f | вода %.2f%n",
                        stats.getEpisode(), stats.getEpsilon(), s.averageReward(), s.averageSteps(),
                        s.successRate() * 100, s.averageShocks(), s.averageWater());
            }
        }, () -> false);

        List<StepResult> path = trainingService.greedyRun(session);
        double total = 0;
        System.out.println();
        System.out.println("Выученный маршрут (обратная связь по шагам):");
        for (int i = 0; i < path.size(); i++) {
            StepResult step = path.get(i);
            total += step.reward();
            if (step.event() != com.vadim.maze.model.StepEvent.MOVE || i == path.size() - 1) {
                System.out.printf("  шаг %3d %s -> %s %-8s награда %+6.1f, сумма %+7.1f%n", i + 1,
                        step.action().getArrow(), step.state().position(), step.event(), step.reward(), total);
            }
        }
        StepResult last = path.get(path.size() - 1);
        System.out.printf("Итог: %s за %d шагов, суммарный выигрыш %.1f%n%n",
                last.terminal() ? "сыр найден" : "сыр НЕ найден", path.size(), total);
        System.out.println(render(maze, path));
    }

    private String render(Maze maze, List<StepResult> path) {
        Set<Position> visited = new HashSet<>();
        path.forEach(step -> visited.add(step.state().position()));
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < maze.getHeight(); y++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                CellType cell = maze.get(x, y);
                boolean onPath = visited.contains(new Position(x, y));
                sb.append(cell == CellType.EMPTY && onPath ? '*' : cell.getSymbol());
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
