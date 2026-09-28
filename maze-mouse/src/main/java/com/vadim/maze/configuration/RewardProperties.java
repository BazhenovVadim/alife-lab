package com.vadim.maze.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Величины подкрепления, которые сообщает среда (секция {@code reward}). */
@Data
@Validated
@ConfigurationProperties(prefix = "reward")
public class RewardProperties {

    /** +Z — сыр в конце лабиринта, эпизод завершается. */
    private double cheese = 100;

    /** +x — вода, выдаётся один раз за эпизод для каждой клетки воды. */
    private double water = 10;

    /** -y — электротравма, выдаётся каждый раз при входе на клетку. */
    private double shock = -30;

    /** Цена шага: подталкивает искать короткий путь. */
    private double step = -1;

    /** Удар о стену: мышь остаётся на месте. */
    private double wallHit = -5;
}
