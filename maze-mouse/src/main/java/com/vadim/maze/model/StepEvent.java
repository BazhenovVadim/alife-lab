package com.vadim.maze.model;

/** Что произошло с мышью на шаге — основа сигнала подкрепления. */
public enum StepEvent {
    MOVE,
    WALL_HIT,
    WATER,
    SHOCK,
    CHEESE,
    TIMEOUT
}
