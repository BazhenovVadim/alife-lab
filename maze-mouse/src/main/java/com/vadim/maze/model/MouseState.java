package com.vadim.maze.model;

/**
 * Состояние агента для Q-таблицы: позиция и маска уже выпитой воды.
 * Маска делает процесс марковским: одна и та же клетка до и после
 * питья — разные состояния, иначе мышь не могла бы понять, что вода уже выпита.
 */
public record MouseState(Position position, long waterMask) {

    public boolean hasDrunk(int waterIndex) {
        return (waterMask & (1L << waterIndex)) != 0;
    }

    public MouseState drink(int waterIndex) {
        return new MouseState(position, waterMask | (1L << waterIndex));
    }

    public MouseState moveTo(Position next) {
        return new MouseState(next, waterMask);
    }
}
