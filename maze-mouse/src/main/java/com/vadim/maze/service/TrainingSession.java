package com.vadim.maze.service;

import com.vadim.maze.model.EpisodeStats;
import com.vadim.maze.model.Maze;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Связка «лабиринт + среда + обучаемая мышь» и история её эпизодов. */
@Getter
@RequiredArgsConstructor
public class TrainingSession {

    private final Maze maze;
    private final MazeEnvironment environment;
    private final QLearningAgent agent;
    private final List<EpisodeStats> history = Collections.synchronizedList(new ArrayList<>());

    /** Текущий незавершённый эпизод; null — следующий шаг начнёт новый эпизод со старта. */
    @Setter(AccessLevel.PACKAGE)
    private EpisodeProgress progress;

    /** Итог последнего завершённого эпизода (с обучением или без). */
    @Setter(AccessLevel.PACKAGE)
    private EpisodeStats lastEpisode;

    public boolean isEpisodeInProgress() {
        return progress != null;
    }

    public int episodesDone() {
        return history.size();
    }

    public List<EpisodeStats> historySnapshot() {
        synchronized (history) {
            return List.copyOf(history);
        }
    }
}
