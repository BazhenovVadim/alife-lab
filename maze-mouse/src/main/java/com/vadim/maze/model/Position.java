package com.vadim.maze.model;

/** Координаты клетки: x — столбец, y — строка. */
public record Position(int x, int y) {

    public Position move(Action action) {
        return new Position(x + action.getDx(), y + action.getDy());
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
