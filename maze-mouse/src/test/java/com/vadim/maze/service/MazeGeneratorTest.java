package com.vadim.maze.service;

import com.vadim.maze.model.CellType;
import com.vadim.maze.model.Maze;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MazeGeneratorTest {

    private final MazeGenerator generator = new MazeGenerator();
    private final MazeValidator validator = new MazeValidator();

    @ParameterizedTest
    @CsvSource({"2,2,0,0,0", "5,3,2,2,0.1", "10,10,6,6,0.15", "30,20,20,40,0.3"})
    void generatesValidMazeOfRequestedSize(int w, int h, int water, int shock, double loops) {
        Maze maze = generator.generate(w, h, water, shock, loops, 1L);

        assertThat(maze.getWidth()).isEqualTo(2 * w + 1);
        assertThat(maze.getHeight()).isEqualTo(2 * h + 1);
        assertThat(validator.validate(maze)).isEmpty();
        assertThat(maze.findAll(CellType.WATER)).hasSize(water);
        assertThat(maze.findAll(CellType.SHOCK)).hasSize(shock);
    }

    @Test
    void perfectMazeConnectsEveryRoom() {
        Maze maze = generator.generate(12, 9, 0, 0, 0, 5L);
        for (int y = 1; y < maze.getHeight(); y += 2) {
            for (int x = 1; x < maze.getWidth(); x += 2) {
                assertThat(validator.isReachable(maze, maze.getStart(), new com.vadim.maze.model.Position(x, y)))
                        .as("комната (%d,%d)", x, y).isTrue();
            }
        }
        assertThat(maze.get(0, 0)).isEqualTo(CellType.WALL);
    }

    @Test
    void sameSeedGivesSameMaze() {
        assertThat(generator.generate(8, 8, 5, 5, 0.2, 42L).toText())
                .isEqualTo(generator.generate(8, 8, 5, 5, 0.2, 42L).toText());
    }

    @Test
    void rejectsTooSmallMaze() {
        assertThatThrownBy(() -> generator.generate(1, 5, 0, 0, 0, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
