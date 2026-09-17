package com.vadim.alife.environment;

import com.vadim.alife.model.Agent;
import com.vadim.alife.model.Position;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Пространственная модель: двумерная дискретная сетка (матрица).
 * В одной клетке может находиться только один агент.
 * Карта ограничена по краям (не тороидальная): выйти за границу
 * или "телепортироваться" на противоположную сторону нельзя.
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

    public boolean isInBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public Agent getAgent(int x, int y) {
        if (!isInBounds(x, y)) {
            return null;
        }
        return grid[y][x];
    }

    public void setAgent(int x, int y, Agent agent) {
        if (!isInBounds(x, y)) {
            throw new IllegalArgumentException("Координаты (" + x + ", " + y + ") вне границ карты");
        }
        grid[y][x] = agent;
        if (agent != null) {
            agent.setX(x);
            agent.setY(y);
        }
    }

    public void removeAgent(int x, int y) {
        if (isInBounds(x, y)) {
            grid[y][x] = null;
        }
    }

    public boolean isEmpty(int x, int y) {
        return isInBounds(x, y) && grid[y][x] == null;
    }

    public List<Position> getNeighbors(int x, int y, int radius) {
        List<Position> result = new ArrayList<>();
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                int nx = x + dx;
                int ny = y + dy;
                if (isInBounds(nx, ny)) {
                    result.add(new Position(nx, ny));
                }
            }
        }
        return result;
    }
}
