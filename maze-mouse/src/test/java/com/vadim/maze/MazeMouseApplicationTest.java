package com.vadim.maze;

import com.vadim.maze.configuration.LearningProperties;
import com.vadim.maze.configuration.RewardProperties;
import com.vadim.maze.service.MazeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"maze.schema-file=classpath:mazes/example.txt", "reward.shock=-40"})
class MazeMouseApplicationTest {

    @Autowired
    private MazeService mazeService;
    @Autowired
    private RewardProperties rewards;
    @Autowired
    private LearningProperties learning;

    @Test
    void contextLoadsConfigurationAndExampleSchema() {
        assertThat(rewards.getShock()).isEqualTo(-40);
        assertThat(learning.getEpisodes()).isPositive();
        assertThat(mazeService.initialMaze().getWidth()).isEqualTo(17);
    }
}
