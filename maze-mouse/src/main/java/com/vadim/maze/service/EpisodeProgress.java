package com.vadim.maze.service;

import com.vadim.maze.model.MouseState;
import com.vadim.maze.model.StepResult;
import lombok.Getter;

/** Незавершённый эпизод: где мышь сейчас и что с ней уже случилось. Позволяет идти по одному шагу. */
@Getter
public class EpisodeProgress {

    private final boolean learning;
    private final double epsilonAtStart;
    private MouseState state;
    private int waterDrunk;
    private int shocks;
    private int wallHits;

    EpisodeProgress(MouseState start, boolean learning, double epsilonAtStart) {
        this.state = start;
        this.learning = learning;
        this.epsilonAtStart = epsilonAtStart;
    }

    void record(StepResult result) {
        state = result.state();
        switch (result.event()) {
            case WATER -> waterDrunk++;
            case SHOCK -> shocks++;
            case WALL_HIT -> wallHits++;
            default -> { }
        }
    }
}
