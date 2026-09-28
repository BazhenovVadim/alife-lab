package com.vadim.maze.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Действие мыши — направление движения на одну клетку. */
@Getter
@RequiredArgsConstructor
public enum Action {
    UP(0, -1, "↑"),
    DOWN(0, 1, "↓"),
    LEFT(-1, 0, "←"),
    RIGHT(1, 0, "→");

    private final int dx;
    private final int dy;
    private final String arrow;
}
