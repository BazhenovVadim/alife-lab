package com.vadim.maze.configuration;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Параметры генерации лабиринта (секция {@code maze} в application.yml). */
@Data
@Validated
@ConfigurationProperties(prefix = "maze")
public class MazeProperties {

    /** Ширина в «комнатах»; итоговая сетка — 2*width+1 клеток. */
    @Min(2) @Max(60)
    private int width = 8;

    /** Высота в «комнатах»; итоговая сетка — 2*height+1 клеток. */
    @Min(2) @Max(60)
    private int height = 8;

    @Min(0) @Max(63)
    private int waterCount = 6;

    @Min(0)
    private int shockCount = 6;

    /** Доля лишних стен, которые убираются, чтобы появились альтернативные пути (0 — идеальный лабиринт). */
    @DecimalMin("0.0") @DecimalMax("1.0")
    private double loopFactor = 0.15;

    /** Зерно генератора; пусто — каждый раз новый лабиринт. */
    private Long seed;

    /** Путь к текстовой схеме лабиринта; если задан — схема загружается вместо генерации. */
    private String schemaFile;
}
