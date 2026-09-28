package com.vadim.maze.model;

import lombok.Builder;
import lombok.Value;

/** Итог одного эпизода обучения. */
@Value
@Builder
public class EpisodeStats {
    int episode;
    double totalReward;
    int steps;
    boolean cheeseFound;
    int waterDrunk;
    int shocks;
    int wallHits;
    double epsilon;
}
