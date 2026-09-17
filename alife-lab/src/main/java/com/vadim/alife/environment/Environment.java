package com.vadim.alife.environment;

import com.vadim.alife.model.Agent;
import com.vadim.alife.model.Position;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Пространственная модель: двумерная дискретная сетка (матрица).
 * В одной клетке может находиться только один агент.
 * Сетка тороидальная (края замкнуты) - это уменьшает вымирание
 * популяций из-за "мёртвых" углов и продлевает жизнь экосистемы.
 */
@Getter
public class Environment {

    private final int width;
    private final int height;
    private final Agent[][] grid;

    public Environment(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new Agent[height][width];
    }

    public Agent getAgent(int x, int y) {
        return grid[wrapY(y)][wrapX(x)];
    }

    public void setAgent(int x, int y, Agent agent) {
        int wx = wrapX(x);
        int wy = wrapY(y);
        grid[wy][wx] = agent;
        if (agent != null) {
            agent.setX(wx);
            agent.setY(wy);
        }
    }

    public void removeAgent(int x, int y) {
        grid[wrapY(y)][wrapX(x)] = null;
    }

    public boolean isEmpty(int x, int y) {
        return getAgent(x, y) == null;
    }

    public List<Position> getNeighbors(int x, int y, int radius) {
        List<Position> result = new ArrayList<>();
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                result.add(new Position(wrapX(x + dx), wrapY(y + dy)));
            }
        }
        return result;
    }

    private int wrapX(int x) {
        return ((x % width) + width) % width;
    }

    private int wrapY(int y) {
        return ((y % height) + height) % height;
    }
}
