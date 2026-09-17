package com.vadim.alife.environment;

import com.vadim.alife.model.Plant;
import com.vadim.alife.model.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnvironmentTest {

    @Test
    void mapIsBoundedAndDoesNotWrapAroundEdges() {
        Environment environment = new Environment(5, 4);
        Plant plant = Plant.builder().energy(1).build();

        assertThrows(IllegalArgumentException.class, () -> environment.setAgent(-1, 4, plant));

        assertNull(environment.getAgent(-1, -4));
        assertNull(environment.getAgent(5, 0));
        assertFalse(environment.isEmpty(-1, 0));
        assertFalse(environment.getNeighbors(0, 0, 1).contains(new Position(4, 3)));
    }

    @Test
    void neighborsNearEdgeOnlyIncludeInBoundsCells() {
        Environment environment = new Environment(5, 4);

        assertFalse(environment.getNeighbors(0, 0, 1).contains(new Position(-1, -1)));
        assertFalse(environment.getNeighbors(4, 3, 1).contains(new Position(5, 4)));
    }

    @Test
    void setAndGetAgentRoundtrip() {
        Environment environment = new Environment(5, 4);
        Plant plant = Plant.builder().energy(1).build();

        environment.setAgent(2, 3, plant);

        assertSame(plant, environment.getAgent(2, 3));
    }
}
