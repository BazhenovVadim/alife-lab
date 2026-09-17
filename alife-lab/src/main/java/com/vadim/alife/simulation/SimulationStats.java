package com.vadim.alife.simulation;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SimulationStats {
    private final int occupiedCells;
    private final int plantCount;
    private final int herbivoreCount;
    private final int predatorCount;
}
