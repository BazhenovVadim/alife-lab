package com.vadim.maze.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.random.RandomGenerator;

/** Источник случайности для ε-жадной стратегии агента. */
@Configuration
public class RandomConfiguration {

    @Bean
    public RandomGenerator explorationRandom() {
        return RandomGenerator.of("L64X128MixRandom");
    }
}
