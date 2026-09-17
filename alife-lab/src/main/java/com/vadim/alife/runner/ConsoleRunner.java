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

    private static final String RESET = "[0m";
    private static final String BOLD = "[1m";
    private static final String GRAY = "[90m";
    private static final String GREEN = "[32m";
    private static final String CYAN = "[96m";
    private static final String RED = "[91m";
    private static final String YELLOW = "[33m";

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
                YELLOW + BOLD + "Итерация %-6d" + RESET
                        + " | занято клеток: %-5d | " + GREEN + "растения: %-4d" + RESET
                        + " | " + CYAN + "травоядные: %-4d" + RESET
                        + " | " + RED + "хищники: %-4d" + RESET + "%n",
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
        System.out.println("Карта (" + GREEN + "Р" + RESET + " — растение, "
                + CYAN + "Т" + RESET + " — травоядное, "
                + RED + "Х" + RESET + " — хищник, "
                + GRAY + "·" + RESET + " — пустая клетка):");
        System.out.print(GRAY + "    ");
        for (int x = 0; x < environment.getWidth(); x++) {
            System.out.print(x % 10);
        }
        System.out.println(RESET);

        for (int y = 0; y < environment.getHeight(); y++) {
            System.out.printf(GRAY + "%3d " + RESET, y);
            for (int x = 0; x < environment.getWidth(); x++) {
                System.out.print(coloredSymbol(environment.getAgent(x, y)));
            }
            System.out.println();
        }
    }

    private String coloredSymbol(Agent agent) {
        if (agent == null) {
            return GRAY + "·" + RESET;
        }
        return switch (agent.getType()) {
            case PLANT -> GREEN + "Р" + RESET;
            case HERBIVORE -> CYAN + "Т" + RESET;
            case PREDATOR -> RED + "Х" + RESET;
        };
    }
}
