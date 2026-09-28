package com.vadim.maze.service;

import com.vadim.maze.configuration.LearningProperties;
import com.vadim.maze.configuration.RewardProperties;
import com.vadim.maze.model.Action;
import com.vadim.maze.model.EpisodeStats;
import com.vadim.maze.model.Maze;
import com.vadim.maze.model.MouseState;
import com.vadim.maze.model.StepResult;
import com.vadim.maze.model.TrainingSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

/** Цикл обучения с подкреплением: эпизоды «мышь — среда — награда — обновление Q». */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingService {

    private final RewardProperties rewardProperties;
    private final LearningProperties learningProperties;
    private final RandomGenerator explorationRandom;

    /** Новая мышь без опыта в заданном лабиринте. Настройки берутся живыми ссылками — правки в GUI сразу действуют. */
    public TrainingSession createSession(Maze maze) {
        MazeEnvironment environment = new MazeEnvironment(maze, rewardProperties, learningProperties.getMaxSteps());
        QLearningAgent agent = new QLearningAgent(learningProperties, explorationRandom);
        return new TrainingSession(maze, environment, agent);
    }

    /** Один обучающий эпизод: мышь действует ε-жадно и после каждого шага обновляет Q-таблицу. */
    public EpisodeStats trainEpisode(TrainingSession session, Consumer<StepResult> onStep) {
        EpisodeStats stats = runEpisode(session, true, onStep);
        session.getAgent().decayEpsilon();
        session.getHistory().add(stats);
        return stats;
    }

    public List<EpisodeStats> train(TrainingSession session, int episodes,
                                    Consumer<EpisodeStats> onEpisode, BooleanSupplier cancelled) {
        List<EpisodeStats> result = new ArrayList<>(episodes);
        for (int i = 0; i < episodes && !cancelled.getAsBoolean(); i++) {
            EpisodeStats stats = trainEpisode(session, step -> { });
            result.add(stats);
            onEpisode.accept(stats);
        }
        TrainingSummary summary = summarize(session, 100);
        log.info("Обучено эпизодов: {}, успех (последние 100): {}%, средняя награда: {}",
                session.episodesDone(), Math.round(summary.successRate() * 100),
                Math.round(summary.averageReward()));
        return result;
    }

    /** Прогон выученной политики без исследования и без обучения — демонстрация результата. */
    public List<StepResult> greedyRun(TrainingSession session) {
        List<StepResult> path = new ArrayList<>();
        runEpisode(session, false, path::add);
        return path;
    }

    public TrainingSummary summarize(TrainingSession session, int window) {
        List<EpisodeStats> history = session.historySnapshot();
        if (history.isEmpty()) {
            return TrainingSummary.EMPTY;
        }
        List<EpisodeStats> last = history.subList(Math.max(0, history.size() - window), history.size());
        return new TrainingSummary(history.size(),
                last.stream().mapToDouble(EpisodeStats::getTotalReward).average().orElse(0),
                last.stream().filter(EpisodeStats::isCheeseFound).count() / (double) last.size(),
                last.stream().mapToInt(EpisodeStats::getSteps).average().orElse(0),
                last.stream().mapToInt(EpisodeStats::getShocks).average().orElse(0),
                last.stream().mapToInt(EpisodeStats::getWaterDrunk).average().orElse(0));
    }

    private EpisodeStats runEpisode(TrainingSession session, boolean learn, Consumer<StepResult> onStep) {
        MazeEnvironment environment = session.getEnvironment();
        QLearningAgent agent = session.getAgent();
        MouseState state = environment.reset();
        double epsilon = agent.getEpsilon();
        int water = 0;
        int shocks = 0;
        int wallHits = 0;
        StepResult result;
        do {
            Action action = agent.chooseAction(state, learn);
            result = environment.step(action);
            if (learn) {
                agent.update(state, action, result.reward(), result.state(), result.terminal());
            }
            switch (result.event()) {
                case WATER -> water++;
                case SHOCK -> shocks++;
                case WALL_HIT -> wallHits++;
                default -> { }
            }
            onStep.accept(result);
            state = result.state();
        } while (!result.done());

        return EpisodeStats.builder()
                .episode(session.episodesDone() + 1)
                .totalReward(environment.getTotalReward())
                .steps(environment.getSteps())
                .cheeseFound(result.terminal())
                .waterDrunk(water)
                .shocks(shocks)
                .wallHits(wallHits)
                .epsilon(epsilon)
                .build();
    }
}
