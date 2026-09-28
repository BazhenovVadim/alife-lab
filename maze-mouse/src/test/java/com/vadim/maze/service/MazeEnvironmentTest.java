package com.vadim.maze.service;

import com.vadim.maze.configuration.RewardProperties;
import com.vadim.maze.model.Action;
import com.vadim.maze.model.Maze;
import com.vadim.maze.model.StepEvent;
import com.vadim.maze.model.StepResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MazeEnvironmentTest {

    // Коридор: сыр — ток — вода — мышь
    private final Maze maze = new MazeParser().parse("""
            ######
            #CEWM#
            ######
            """);
    private final RewardProperties rewards = new RewardProperties();
    private MazeEnvironment env;

    @BeforeEach
    void setUp() {
        env = new MazeEnvironment(maze, rewards, 0);
    }

    @Test
    void wallHitKeepsMouseInPlace() {
        StepResult r = env.step(Action.UP);
        assertThat(r.event()).isEqualTo(StepEvent.WALL_HIT);
        assertThat(r.reward()).isEqualTo(rewards.getWallHit());
        assertThat(r.state().position()).isEqualTo(maze.getStart());
        assertThat(r.done()).isFalse();
    }

    @Test
    void waterIsRewardedOnlyOncePerEpisode() {
        StepResult drink = env.step(Action.LEFT);
        assertThat(drink.event()).isEqualTo(StepEvent.WATER);
        assertThat(drink.reward()).isEqualTo(rewards.getStep() + rewards.getWater());

        env.step(Action.RIGHT);
        StepResult again = env.step(Action.LEFT);
        assertThat(again.event()).isEqualTo(StepEvent.MOVE);
        assertThat(again.reward()).isEqualTo(rewards.getStep());

        env.reset();
        assertThat(env.step(Action.LEFT).event()).isEqualTo(StepEvent.WATER);
    }

    @Test
    void shockAndCheeseRewardsAndTotal() {
        env.step(Action.LEFT);
        StepResult shock = env.step(Action.LEFT);
        StepResult cheese = env.step(Action.LEFT);

        assertThat(shock.event()).isEqualTo(StepEvent.SHOCK);
        assertThat(shock.reward()).isEqualTo(rewards.getStep() + rewards.getShock());
        assertThat(cheese.event()).isEqualTo(StepEvent.CHEESE);
        assertThat(cheese.terminal()).isTrue();
        assertThat(env.getTotalReward()).isEqualTo(
                3 * rewards.getStep() + rewards.getWater() + rewards.getShock() + rewards.getCheese());
    }

    @Test
    void episodeIsTruncatedByStepLimit() {
        MazeEnvironment limited = new MazeEnvironment(maze, rewards, 2);
        assertThat(limited.step(Action.UP).done()).isFalse();
        StepResult last = limited.step(Action.UP);
        assertThat(last.truncated()).isTrue();
        assertThat(last.terminal()).isFalse();
    }
}
