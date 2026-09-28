package com.vadim.maze.service;

import com.vadim.maze.model.EpisodeStats;
import com.vadim.maze.model.Maze;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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

    public int episodesDone() {
        return history.size();
    }

    public List<EpisodeStats> historySnapshot() {
        synchronized (history) {
            return List.copyOf(history);
        }
    }
}
