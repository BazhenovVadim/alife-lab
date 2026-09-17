package com.vadim.alife.runner;

import com.vadim.alife.config.SimulationProperties;
import com.vadim.alife.environment.Environment;
import com.vadim.alife.model.Agent;
import com.vadim.alife.simulation.SimulationEngine;
import com.vadim.alife.simulation.SimulationStats;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "simulation.console", havingValue = "true")
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

            boolean reportDue = iteration % properties.getPrintEveryNSteps() == 0
                    || iteration == properties.getTotalIterations();
            if (reportDue) {
                printSnapshot(iteration);
            }

            if (engine.isEcosystemCollapsed()) {
                if (!reportDue) {
                    printSnapshot(iteration);
                }
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

    /**
     * Печатает актуальное состояние карты в момент отчёта. Символы намеренно
     * однобуквенные, чтобы ширина каждой строки совпадала с шириной поля.
     */
    private void printSnapshot(int iteration) {
        printStats(iteration, engine.collectStats());
        printMap(engine.getEnvironment());
    }

    private void printMap(Environment environment) {
        System.out.println("Карта (Р — растение, Т — травоядное, Х — хищник, · — пустая клетка):");
        System.out.print("    ");
        for (int x = 0; x < environment.getWidth(); x++) {
            System.out.print(x % 10);
        }
        System.out.println();

        for (int y = 0; y < environment.getHeight(); y++) {
            System.out.printf("%3d ", y);
            for (int x = 0; x < environment.getWidth(); x++) {
                System.out.print(symbol(environment.getAgent(x, y)));
            }
            System.out.println();
        }
    }

    private char symbol(Agent agent) {
        if (agent == null) {
            return '·';
        }
        return switch (agent.getType()) {
            case PLANT -> 'Р';
            case HERBIVORE -> 'Т';
            case PREDATOR -> 'Х';
        };
    }
}
