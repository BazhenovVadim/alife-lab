package com.vadim.alife.environment;

import com.vadim.alife.model.Plant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentTest {

    @Test
    void accessesOppositeEdgeWhenCoordinateLeavesMap() {
        Environment environment = new Environment(5, 4);
        Plant plant = Plant.builder().energy(1).build();

        environment.setAgent(-1, 4, plant);

        assertSame(plant, environment.getAgent(4, 0));
        assertSame(plant, environment.getAgent(-1, -4));
        assertTrue(environment.getNeighbors(0, 0, 1).contains(new com.vadim.alife.model.Position(4, 3)));
    }
}
