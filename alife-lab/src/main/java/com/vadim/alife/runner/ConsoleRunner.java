package com.vadim.alife.runner;

import com.vadim.alife.config.SimulationProperties;
import com.vadim.alife.simulation.SimulationEngine;
import com.vadim.alife.simulation.SimulationStats;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConsoleRunner implements CommandLineRunner {

    private final SimulationEngine engine;
    private final SimulationProperties properties;

    @Override
    public void run(String... args) {
        engine.initialize();
        System.out.printf(
                "Старт симуляции: поле %dx%d, растения=%d, травоядные=%d, хищники=%d, итераций=%d%n",
                properties.getGridWidth(), properties.getGridHeight(),
                properties.getInitialPlants(), properties.getInitialHerbivores(), properties.getInitialPredators(),
                properties.getTotalIterations());

        int lastIteration = 0;
        for (int iteration = 1; iteration <= properties.getTotalIterations(); iteration++) {
            engine.step();
            lastIteration = iteration;

            if (iteration % properties.getPrintEveryNSteps() == 0 || iteration == properties.getTotalIterations()) {
                printStats(iteration, engine.collectStats());
            }

            if (engine.isEcosystemCollapsed()) {
                printStats(iteration, engine.collectStats());
                System.out.printf("Экосистема вымерла на итерации %d%n", iteration);
                return;
            }
        }
        System.out.printf("Симуляция завершена: экосистема пережила %d итераций.%n", lastIteration);
    }

    private void printStats(int iteration, SimulationStats stats) {
        System.out.printf(
                "Итерация %-6d | занято клеток: %-5d | растения: %-4d | травоядные: %-4d | хищники: %-4d%n",
                iteration, stats.getOccupiedCells(), stats.getPlantCount(),
                stats.getHerbivoreCount(), stats.getPredatorCount());
    }
}
