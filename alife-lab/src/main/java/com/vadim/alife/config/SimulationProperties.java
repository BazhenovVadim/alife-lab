package com.vadim.alife.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "simulation")
public class SimulationProperties {

    /** When true, execute {@code ConsoleRunner} and do not open the JavaFX window. */
    private boolean console;

    private int gridWidth;
    private int gridHeight;
    private int totalIterations;
    private int printEveryNSteps;

    private int initialPlants;
    private int initialHerbivores;
    private int initialPredators;

    private double plantInitialEnergy;
    private double plantEnergyPerStep;
    private double plantEnergyDecayPerStep;
    private double plantMaxEnergy;
    private double plantReproductionThreshold;
    private double plantReproductionCost;

    private double herbivoreInitialEnergy;
    private double herbivoreEnergyLossPerStep;
    private double herbivoreReproductionThreshold;
    private double herbivoreReproductionCost;
    private int herbivoreVisionRadius;
    private int herbivoreReproductionCooldownSteps;

    private double predatorInitialEnergy;
    private double predatorEnergyLossPerStep;
    private double predatorEnergyFromHerbivore;
    private double predatorReproductionThreshold;
    private double predatorReproductionCost;
    private int predatorVisionRadius;
    private int predatorReproductionCooldownSteps;
}
