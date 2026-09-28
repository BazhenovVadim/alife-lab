package com.vadim.maze.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/** Тип клетки лабиринта и её символ в текстовой схеме. */
@Getter
@RequiredArgsConstructor
public enum CellType {
    WALL('#', "Стена"),
    EMPTY('.', "Проход"),
    START('M', "Старт мыши"),
    CHEESE('C', "Сыр (+Z)"),
    WATER('W', "Вода (+x)"),
    SHOCK('E', "Электроток (-y)");

    private final char symbol;
    private final String title;

    public boolean isPassable() {
        return this != WALL;
    }

    public static Optional<CellType> fromSymbol(char symbol) {
        char normalized = symbol == ' ' ? '.' : Character.toUpperCase(symbol);
        return Arrays.stream(values()).filter(t -> t.symbol == normalized).findFirst();
    }
}
