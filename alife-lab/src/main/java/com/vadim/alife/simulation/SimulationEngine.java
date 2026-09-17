package com.vadim.alife.simulation;

import com.vadim.alife.config.SimulationProperties;
import com.vadim.alife.environment.Environment;
import com.vadim.alife.model.Agent;
import com.vadim.alife.model.AgentType;
import com.vadim.alife.model.Herbivore;
import com.vadim.alife.model.Plant;
import com.vadim.alife.model.Position;
import com.vadim.alife.model.Predator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class SimulationEngine {

    private final SimulationProperties props;
    private final Random random = new Random();

    @Getter
    private Environment environment;

    public void initialize() {
        environment = new Environment(props.getGridWidth(), props.getGridHeight());
        spawnInitial(props.getInitialPlants(), () -> Plant.builder().energy(props.getPlantInitialEnergy()).build());
        spawnInitial(props.getInitialHerbivores(), () -> Herbivore.builder().energy(props.getHerbivoreInitialEnergy()).build());
        spawnInitial(props.getInitialPredators(), () -> Predator.builder().energy(props.getPredatorInitialEnergy()).build());
    }

    private void spawnInitial(int count, Supplier<Agent> factory) {
        int placed = 0;
        int attempts = 0;
        int maxAttempts = count * 50 + 100;
        while (placed < count && attempts < maxAttempts) {
            int x = random.nextInt(environment.getWidth());
            int y = random.nextInt(environment.getHeight());
            attempts++;
            if (environment.isEmpty(x, y)) {
                environment.setAgent(x, y, factory.get());
                placed++;
            }
        }
    }

    public void step() {
        List<Agent> snapshot = collectAliveAgents();
        Collections.shuffle(snapshot, random);
        // хищники ходят первыми - логичный порядок "охотник действует активно, жертва реагирует"
        snapshot.sort(Comparator.comparingInt(a -> a.getType() == AgentType.PREDATOR ? 0 : 1));

        for (Agent agent : snapshot) {
            // агент мог быть съеден другим агентом раньше в этом же шаге
            if (environment.getAgent(agent.getX(), agent.getY()) != agent) {
                continue;
            }
            agent.setAge(agent.getAge() + 1);
            switch (agent.getType()) {
                case PLANT -> processPlant((Plant) agent);
                case HERBIVORE -> processHerbivore((Herbivore) agent);
                case PREDATOR -> processPredator((Predator) agent);
            }
        }
    }

    private void processPlant(Plant plant) {
        double newEnergy = plant.getEnergy() + props.getPlantEnergyPerStep() - props.getPlantEnergyDecayPerStep();
        plant.setEnergy(Math.min(props.getPlantMaxEnergy(), newEnergy));

        if (plant.getEnergy() <= 0) {
            environment.removeAgent(plant.getX(), plant.getY());
            return;
        }

        if (plant.getEnergy() >= props.getPlantReproductionThreshold()) {
            findRandomEmptyNeighbor(plant.getX(), plant.getY()).ifPresent(pos -> {
                plant.setEnergy(plant.getEnergy() - props.getPlantReproductionCost());
                Plant child = Plant.builder().energy(props.getPlantReproductionCost()).build();
                environment.setAgent(pos.getX(), pos.getY(), child);
            });
        }
    }

    private void processHerbivore(Herbivore herbivore) {
        herbivore.setEnergy(herbivore.getEnergy() - props.getHerbivoreEnergyLossPerStep());
        if (herbivore.getEnergy() <= 0) {
            environment.removeAgent(herbivore.getX(), herbivore.getY());
            return;
        }

        int radius = props.getHerbivoreVisionRadius();
        List<Position> predators = findAll(herbivore.getX(), herbivore.getY(), radius, AgentType.PREDATOR);

        if (!predators.isEmpty()) {
            fleeFromAll(herbivore, predators);
        } else {
            Optional<Position> plant = findNearest(herbivore.getX(), herbivore.getY(), radius, AgentType.PLANT);
            if (plant.isPresent()) {
                moveTowardAndConsume(herbivore, plant.get(), props.getHerbivoreEnergyFromPlant());
            } else {
                randomWalk(herbivore);
            }
        }

        tryReproduce(herbivore, props.getHerbivoreReproductionThreshold(), props.getHerbivoreReproductionCost(),
                props.getHerbivoreReproductionCooldownSteps(), () -> Herbivore.builder().build());
    }

    private void processPredator(Predator predator) {
        predator.setEnergy(predator.getEnergy() - props.getPredatorEnergyLossPerStep());
        if (predator.getEnergy() <= 0) {
            environment.removeAgent(predator.getX(), predator.getY());
            return;
        }

        int radius = props.getPredatorVisionRadius();
        // ищем только травоядных: на клетку с растением хищник встать не может
        Optional<Position> prey = findNearest(predator.getX(), predator.getY(), radius, AgentType.HERBIVORE);

        if (prey.isPresent()) {
            moveTowardAndConsume(predator, prey.get(), props.getPredatorEnergyFromHerbivore());
        } else {
            randomWalk(predator);
        }

        tryReproduce(predator, props.getPredatorReproductionThreshold(), props.getPredatorReproductionCost(),
                props.getPredatorReproductionCooldownSteps(), () -> Predator.builder().build());
    }

    // ---------------- перемещения ----------------

    /** Двигаться на 1 клетку к цели (кратчайший путь по тору); если цель вплотную - съесть без задержки. */
    private void moveTowardAndConsume(Agent agent, Position target, double defaultEnergyGain) {
        int stepX = getTorusStep(agent.getX(), target.getX(), environment.getWidth());
        int stepY = getTorusStep(agent.getY(), target.getY(), environment.getHeight());

        int nx = wrapX(agent.getX() + stepX);
        int ny = wrapY(agent.getY() + stepY);

        boolean isTargetCell = (nx == target.getX() && ny == target.getY());

        if (isTargetCell) {
            Agent prey = environment.getAgent(nx, ny);

            if (prey == null) {
                // жертву уже съел кто-то другой в этот же шаг - просто занимаем освободившуюся клетку
                environment.removeAgent(agent.getX(), agent.getY());
                environment.setAgent(nx, ny, agent);
            } else {
                double gainedEnergy = defaultEnergyGain;
                // травоядное, поедая растение, забирает всю накопленную им энергию
                if (agent.getType() == AgentType.HERBIVORE && prey.getType() == AgentType.PLANT) {
                    gainedEnergy = prey.getEnergy();
                }

                environment.removeAgent(nx, ny);
                environment.removeAgent(agent.getX(), agent.getY());
                environment.setAgent(nx, ny, agent);
                agent.setEnergy(agent.getEnergy() + gainedEnergy);
            }
        } else if (environment.isEmpty(nx, ny)) {
            environment.removeAgent(agent.getX(), agent.getY());
            environment.setAgent(nx, ny, agent);
        }
    }

    /** Кратчайший шаг с учётом тороидальности поля (через край может быть ближе). */
    private int getTorusStep(int from, int to, int maxSide) {
        int dist = to - from;
        if (Math.abs(dist) > maxSide / 2) {
            dist = dist > 0 ? dist - maxSide : dist + maxSide;
        }
        return Integer.signum(dist);
    }

    /**
     * Побег от ВСЕХ видимых хищников сразу, а не только от ближайшего.
     * Перебирает 8 соседних клеток (без варианта "остаться на месте" - двигаться
     * обязан, стоять на месте могут только растения) и выбирает ту, что
     * максимизирует минимальное расстояние до любого из хищников.
     */
    private void fleeFromAll(Agent agent, List<Position> threats) {
        int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1};

        int bestScore = Integer.MIN_VALUE;
        List<Integer> ties = new ArrayList<>();

        for (int i = 0; i < dx.length; i++) {
            int nx = wrapX(agent.getX() + dx[i]);
            int ny = wrapY(agent.getY() + dy[i]);
            if (!environment.isEmpty(nx, ny)) {
                continue; // клетка занята - вариант недоступен
            }

            int minDist = Integer.MAX_VALUE;
            for (Position threat : threats) {
                minDist = Math.min(minDist, manhattan(nx, ny, threat.getX(), threat.getY()));
            }
            if (minDist > bestScore) {
                bestScore = minDist;
                ties.clear();
                ties.add(i);
            } else if (minDist == bestScore) {
                ties.add(i);
            }
        }

        if (ties.isEmpty()) {
            return; // все 8 клеток заняты - физически некуда шагнуть
        }
        int choice = ties.get(random.nextInt(ties.size()));
        int nx = wrapX(agent.getX() + dx[choice]);
        int ny = wrapY(agent.getY() + dy[choice]);
        environment.removeAgent(agent.getX(), agent.getY());
        environment.setAgent(nx, ny, agent);
    }

    private void randomWalk(Agent agent) {
        findRandomEmptyNeighbor(agent.getX(), agent.getY()).ifPresent(pos -> {
            environment.removeAgent(agent.getX(), agent.getY());
            environment.setAgent(pos.getX(), pos.getY(), agent);
        });
    }

    private void tryReproduce(Agent parent, double threshold, double childEnergy, int cooldownSteps, Supplier<Agent> factory) {
        if (parent.getEnergy() > threshold && parent.getAge() >= cooldownSteps) {
            findRandomEmptyNeighbor(parent.getX(), parent.getY()).ifPresent(pos -> {
                parent.setEnergy(parent.getEnergy() - childEnergy);
                parent.setAge(0); // родитель "восстанавливается" после размножения
                Agent child = factory.get();
                child.setEnergy(childEnergy);
                environment.setAgent(pos.getX(), pos.getY(), child);
            });
        }
    }


    private List<Position> findAll(int x, int y, int radius, AgentType type) {
        List<Position> result = new ArrayList<>();
        for (Position pos : environment.getNeighbors(x, y, radius)) {
            Agent a = environment.getAgent(pos.getX(), pos.getY());
            if (a != null && a.getType() == type) {
                result.add(pos);
            }
        }
        return result;
    }

    private Optional<Position> findNearest(int x, int y, int radius, AgentType type) {
        return environment.getNeighbors(x, y, radius).stream()
                .filter(pos -> {
                    Agent a = environment.getAgent(pos.getX(), pos.getY());
                    return a != null && a.getType() == type;
                })
                .min(Comparator.comparingInt(pos -> manhattan(x, y, pos.getX(), pos.getY())));
    }

    private Optional<Position> findRandomEmptyNeighbor(int x, int y) {
        List<Position> empty = new ArrayList<>();
        for (Position pos : environment.getNeighbors(x, y, 1)) {
            if (environment.isEmpty(pos.getX(), pos.getY())) {
                empty.add(pos);
            }
        }
        if (empty.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(empty.get(random.nextInt(empty.size())));
    }

    private int wrapX(int x) {
        return ((x % environment.getWidth()) + environment.getWidth()) % environment.getWidth();
    }

    private int wrapY(int y) {
        return ((y % environment.getHeight()) + environment.getHeight()) % environment.getHeight();
    }

    private int manhattan(int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x1 - x2);
        int dy = Math.abs(y1 - y2);
        dx = Math.min(dx, environment.getWidth() - dx);
        dy = Math.min(dy, environment.getHeight() - dy);
        return dx + dy;
    }

    private List<Agent> collectAliveAgents() {
        List<Agent> agents = new ArrayList<>();
        for (int y = 0; y < environment.getHeight(); y++) {
            for (int x = 0; x < environment.getWidth(); x++) {
                Agent a = environment.getAgent(x, y);
                if (a != null) {
                    agents.add(a);
                }
            }
        }
        return agents;
    }

    public SimulationStats collectStats() {
        int plants = 0;
        int herbivores = 0;
        int predators = 0;
        for (Agent a : collectAliveAgents()) {
            switch (a.getType()) {
                case PLANT -> plants++;
                case HERBIVORE -> herbivores++;
                case PREDATOR -> predators++;
            }
        }
        return SimulationStats.builder()
                .occupiedCells(plants + herbivores + predators)
                .plantCount(plants)
                .herbivoreCount(herbivores)
                .predatorCount(predators)
                .build();
    }

    public boolean isEcosystemCollapsed() {
        SimulationStats stats = collectStats();
        return (stats.getHerbivoreCount() == 0 && stats.getPredatorCount() == 0)
                || (stats.getPlantCount() == 0 && stats.getHerbivoreCount() == 0);
    }
}
