package com.vadim.alife.simulation;

import com.vadim.alife.config.SimulationProperties;
import com.vadim.alife.environment.Environment;
import com.vadim.alife.model.Herbivore;
import com.vadim.alife.model.Plant;
import com.vadim.alife.model.Predator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationEngineTest {

    @Test
    void herbivoreEatsPlantNextToPredatorFullyBoxedIn() {
        SimulationProperties props = new SimulationProperties();
        props.setHerbivoreVisionRadius(3);
        props.setHerbivoreEnergyLossPerStep(0);
        props.setHerbivoreReproductionThreshold(1000);
        props.setPredatorVisionRadius(3);
        props.setPredatorEnergyLossPerStep(0);
        props.setPredatorReproductionThreshold(1000);
        props.setPlantEnergyPerStep(0);
        props.setPlantEnergyDecayPerStep(0);
        props.setPlantMaxEnergy(100);
        props.setPlantReproductionThreshold(1000);

        SimulationEngine engine = new SimulationEngine(props);
        Environment environment = new Environment(5, 5);
        ReflectionTestUtils.setField(engine, "environment", environment);

        Predator predator = Predator.builder().energy(50).build();
        environment.setAgent(2, 2, predator);
        // Хищник полностью окружён растениями (кольцо в один слой) - ему некуда шагнуть.
        int[][] ring = {{1, 1}, {1, 2}, {1, 3}, {2, 1}, {2, 3}, {3, 1}, {3, 2}, {3, 3}};
        for (int[] pos : ring) {
            environment.setAgent(pos[0], pos[1], Plant.builder().energy(1).build());
        }

        Herbivore herbivore = Herbivore.builder().energy(50).build();
        environment.setAgent(0, 1, herbivore); // видит хищника (радиус зрения 3), но тот не может напасть

        engine.step();

        // Заблокированный хищник не должен был сдвинуться с места.
        assertSame(predator, environment.getAgent(2, 2));

        // Заяц не должен был убегать от безопасного хищника - он подошёл и съел ближайшее растение.
        assertSame(herbivore, environment.getAgent(1, 1));
        assertTrue(herbivore.getEnergy() > 50, "заяц должен был получить энергию съеденного растения");
        assertEquals(50 + 1, herbivore.getEnergy());
    }
}
