package com.vadim.maze.configuration;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Гиперпараметры Q-learning (секция {@code learning}). */
@Data
@Validated
@ConfigurationProperties(prefix = "learning")
public class LearningProperties {

    /** α — скорость обучения. */
    @DecimalMin("0.0") @DecimalMax("1.0")
    private double alpha = 0.5;

    /** γ — коэффициент дисконтирования будущей награды. */
    @DecimalMin("0.0") @DecimalMax("1.0")
    private double gamma = 0.97;

    /** Начальное ε для ε-жадной стратегии. */
    @DecimalMin("0.0") @DecimalMax("1.0")
    private double epsilonStart = 1.0;

    @DecimalMin("0.0") @DecimalMax("1.0")
    private double epsilonMin = 0.02;

    /** Множитель ε после каждого эпизода. */
    @DecimalMin("0.0") @DecimalMax("1.0")
    private double epsilonDecay = 0.998;

    /**
     * Начальное значение Q(s,a). Оптимистичная оценка (не ниже реальной ценности) заставляет
     * мышь систематически пробовать ещё не испытанные действия, а не застревать на первом найденном пути.
     */
    private double initialQ = 100;

    @Min(1)
    private int episodes = 3000;

    /** Лимит шагов в эпизоде; 0 — автоматически (4 × число проходимых клеток). */
    @Min(0)
    private int maxSteps = 0;
}
