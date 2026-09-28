package com.vadim.maze.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Лабиринт: прямоугольная сетка клеток с одной мышью и одним сыром. */
public class Maze {

    /** Ограничение маски воды в {@link MouseState}. */
    public static final int MAX_WATER = 63;

    @Getter
    private final int width;
    @Getter
    private final int height;
    private final CellType[][] cells;

    public Maze(int width, int height) {
        if (width < 3 || height < 3) {
            throw new IllegalArgumentException("Лабиринт должен быть не меньше 3x3");
        }
        this.width = width;
        this.height = height;
        this.cells = new CellType[height][width];
        for (CellType[] row : cells) {
            java.util.Arrays.fill(row, CellType.WALL);
        }
    }

    public CellType get(Position p) {
        return inBounds(p) ? cells[p.y()][p.x()] : CellType.WALL;
    }

    public CellType get(int x, int y) {
        return get(new Position(x, y));
    }

    public void set(Position p, CellType type) {
        cells[p.y()][p.x()] = type;
    }

    public void set(int x, int y, CellType type) {
        set(new Position(x, y), type);
    }

    public boolean inBounds(Position p) {
        return p.x() >= 0 && p.y() >= 0 && p.x() < width && p.y() < height;
    }

    public boolean isPassable(Position p) {
        return get(p).isPassable();
    }

    public List<Position> findAll(CellType type) {
        List<Position> result = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (cells[y][x] == type) {
                    result.add(new Position(x, y));
                }
            }
        }
        return result;
    }

    public Position getStart() {
        return single(CellType.START);
    }

    public Position getCheese() {
        return single(CellType.CHEESE);
    }

    /** Индексы клеток с водой — номера битов в маске состояния. */
    public Map<Position, Integer> waterIndex() {
        Map<Position, Integer> index = new HashMap<>();
        List<Position> water = findAll(CellType.WATER);
        for (int i = 0; i < water.size(); i++) {
            index.put(water.get(i), i);
        }
        return Collections.unmodifiableMap(index);
    }

    public Maze copy() {
        Maze copy = new Maze(width, height);
        for (int y = 0; y < height; y++) {
            System.arraycopy(cells[y], 0, copy.cells[y], 0, width);
        }
        return copy;
    }

    /** Текстовая схема в формате {@link CellType#getSymbol()}. */
    public String toText() {
        StringBuilder sb = new StringBuilder();
        for (CellType[] row : cells) {
            for (CellType cell : row) {
                sb.append(cell.getSymbol());
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    private Position single(CellType type) {
        List<Position> found = findAll(type);
        if (found.size() != 1) {
            throw new IllegalStateException("В лабиринте должна быть ровно одна клетка «" + type.getTitle()
                    + "», найдено: " + found.size());
        }
        return found.get(0);
    }
}
