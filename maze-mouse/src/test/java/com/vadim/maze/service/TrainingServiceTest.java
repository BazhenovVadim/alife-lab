package com.vadim.maze.service;

import com.vadim.maze.configuration.LearningProperties;
import com.vadim.maze.configuration.RewardProperties;
import com.vadim.maze.model.Maze;
import com.vadim.maze.model.StepEvent;
import com.vadim.maze.model.StepResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingServiceTest {

    private TrainingService service = new TrainingService(new RewardProperties(), new LearningProperties(),
            new Random(7));

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
    void mouseLearnsToAvoidShockWhenDetourIsCheap(int seed) {
        // Прямой путь (4 шага) через ток, обход — 8 шагов: обход выгоднее на 30 − 4 = 26.
        Maze maze = new MazeParser().parse("""
                #######
                #C.E.M#
                #.###.#
                #.....#
                #######
                """);
        service = new TrainingService(new RewardProperties(), new LearningProperties(), new Random(seed));
        TrainingSession session = service.createSession(maze);
        service.train(session, 1500, s -> { }, () -> false);

        List<StepResult> path = service.greedyRun(session);
        assertThat(path.get(path.size() - 1).event()).isEqualTo(StepEvent.CHEESE);
        assertThat(path).noneMatch(s -> s.event() == StepEvent.SHOCK);
        assertThat(path).hasSize(8);
    }

    @Test
    void mouseDetoursForWaterAndReachesCheeseInGeneratedMaze() {
        Maze maze = new MazeGenerator().generate(6, 6, 4, 4, 0.15, 3L);
        TrainingSession session = service.createSession(maze);
        service.train(session, 3000, s -> { }, () -> false);

        assertThat(service.summarize(session, 100).successRate()).isGreaterThan(0.9);
        List<StepResult> path = service.greedyRun(session);
        assertThat(path.get(path.size() - 1).terminal()).isTrue();
        assertThat(session.getAgent().getEpsilon()).isLessThan(0.1);
    }
}
