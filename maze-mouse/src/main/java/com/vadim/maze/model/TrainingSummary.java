package com.vadim.maze.model;

/** Сводка по последним эпизодам обучения. */
public record TrainingSummary(int episodes, double averageReward, double successRate, double averageSteps,
                              double averageShocks, double averageWater) {

    public static final TrainingSummary EMPTY = new TrainingSummary(0, 0, 0, 0, 0, 0);
}
