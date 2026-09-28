package com.vadim.maze.service;

import com.vadim.maze.model.CellType;
import com.vadim.maze.model.Maze;
import com.vadim.maze.model.Position;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MazeParserTest {

    private final MazeParser parser = new MazeParser();
    private final MazeValidator validator = new MazeValidator();

    @Test
    void parsesAllCellTypesAndRoundTrips() {
        String text = """
                ; комментарий
                #######
                #C.W.E#
                #.###.#
                #....M#
                #######
                """;
        Maze maze = parser.parse(text);

        assertThat(maze.getWidth()).isEqualTo(7);
        assertThat(maze.getHeight()).isEqualTo(5);
        assertThat(maze.getCheese()).isEqualTo(new Position(1, 1));
        assertThat(maze.getStart()).isEqualTo(new Position(5, 3));
        assertThat(maze.get(3, 1)).isEqualTo(CellType.WATER);
        assertThat(maze.get(5, 1)).isEqualTo(CellType.SHOCK);
        assertThat(validator.validate(maze)).isEmpty();
        assertThat(parser.parse(maze.toText()).toText()).isEqualTo(maze.toText());
    }

    @Test
    void shortLinesArePaddedWithWalls() {
        Maze maze = parser.parse("#####\n#C.M\n#####");
        assertThat(maze.get(4, 1)).isEqualTo(CellType.WALL);
    }

    @Test
    void rejectsUnknownSymbol() {
        assertThatThrownBy(() -> parser.parse("###\n#X#\n###"))
                .hasMessageContaining("'X'").hasMessageContaining("строке 2");
    }

    @Test
    void validatorFindsProblems() {
        Maze unreachable = parser.parse("#######\n#C#..M#\n#######");
        assertThat(validator.validate(unreachable)).containsExactly("Сыр недостижим из стартовой клетки мыши");

        Maze twoMice = parser.parse("#####\n#CMM#\n#####");
        assertThat(validator.validate(twoMice)).anyMatch(e -> e.contains("одна мышь"));
    }
}
