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
        return runEpisode(session, true, onStep);
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

    /**
     * Один шаг мыши. Если эпизод не начат (или предыдущий завершён), он начинается со старта.
     * При {@code learn=true} шаг ε-жадный с обновлением Q, а завершённый эпизод попадает в историю.
     * При {@code learn=false} шаг жадный, без обучения.
     */
    public StepResult step(TrainingSession session, boolean learn) {
        EpisodeProgress progress = session.getProgress();
        if (progress == null || progress.isLearning() != learn) {
            progress = beginEpisode(session, learn);
        }
        QLearningAgent agent = session.getAgent();
        MouseState state = progress.getState();
        Action action = agent.chooseAction(state, learn);
        StepResult result = session.getEnvironment().step(action);
        if (learn) {
            agent.update(state, action, result.reward(), result.state(), result.terminal());
        }
        progress.record(result);
        if (result.done()) {
            finishEpisode(session, progress, result);
        }
        return result;
    }

    /** Прерывает незавершённый пошаговый эпизод: следующий шаг начнётся со старта. */
    public void abandonEpisode(TrainingSession session) {
        session.setProgress(null);
    }

    private EpisodeProgress beginEpisode(TrainingSession session, boolean learn) {
        MouseState start = session.getEnvironment().reset();
        EpisodeProgress progress = new EpisodeProgress(start, learn, session.getAgent().getEpsilon());
        session.setProgress(progress);
        return progress;
    }

    private void finishEpisode(TrainingSession session, EpisodeProgress progress, StepResult last) {
        MazeEnvironment environment = session.getEnvironment();
        EpisodeStats stats = EpisodeStats.builder()
                .episode(session.episodesDone() + 1)
                .totalReward(environment.getTotalReward())
                .steps(environment.getSteps())
                .cheeseFound(last.terminal())
                .waterDrunk(progress.getWaterDrunk())
                .shocks(progress.getShocks())
                .wallHits(progress.getWallHits())
                .epsilon(progress.getEpsilonAtStart())
                .build();
        if (progress.isLearning()) {
            session.getAgent().decayEpsilon();
            session.getHistory().add(stats);
        }
        session.setLastEpisode(stats);
        session.setProgress(null);
    }

    private EpisodeStats runEpisode(TrainingSession session, boolean learn, Consumer<StepResult> onStep) {
        beginEpisode(session, learn);
        StepResult result;
        do {
            result = step(session, learn);
            onStep.accept(result);
        } while (!result.done());
        return session.getLastEpisode();
    }
}
