package com.vadim.maze.service;

import com.vadim.maze.configuration.RewardProperties;
import com.vadim.maze.model.Action;
import com.vadim.maze.model.CellType;
import com.vadim.maze.model.Maze;
import com.vadim.maze.model.MouseState;
import com.vadim.maze.model.Position;
import com.vadim.maze.model.StepEvent;
import com.vadim.maze.model.StepResult;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Окружающая среда: на каждое действие мыши сообщает новое состояние
 * и величину выигрыша/проигрыша — это и есть обратная связь (подкрепление).
 */
public class MazeEnvironment {

    @Getter
    private final Maze maze;
    private final Map<Position, Integer> waterIndex;
    @Getter
    @Setter
    private RewardProperties rewards;
    @Getter
    private final int maxSteps;

    @Getter
    private MouseState state;
    @Getter
    private int steps;
    @Getter
    private double totalReward;

    public MazeEnvironment(Maze maze, RewardProperties rewards, int maxSteps) {
        this.maze = maze;
        this.rewards = rewards;
        this.waterIndex = maze.waterIndex();
        this.maxSteps = maxSteps > 0 ? maxSteps : 4 * countPassable(maze);
        reset();
    }

    public MouseState reset() {
        state = new MouseState(maze.getStart(), 0L);
        steps = 0;
        totalReward = 0;
        return state;
    }

    public StepResult step(Action action) {
        steps++;
        Position target = state.position().move(action);
        double reward;
        StepEvent event;
        boolean terminal = false;

        if (!maze.isPassable(target)) {
            reward = rewards.getWallHit();
            event = StepEvent.WALL_HIT;
        } else {
            state = state.moveTo(target);
            reward = rewards.getStep();
            event = StepEvent.MOVE;
            CellType cell = maze.get(target);
            switch (cell) {
                case CHEESE -> {
                    reward += rewards.getCheese();
                    event = StepEvent.CHEESE;
                    terminal = true;
                }
                case WATER -> {
                    int index = waterIndex.get(target);
                    if (!state.hasDrunk(index)) {
                        state = state.drink(index);
                        reward += rewards.getWater();
                        event = StepEvent.WATER;
                    }
                }
                case SHOCK -> {
                    reward += rewards.getShock();
                    event = StepEvent.SHOCK;
                }
                default -> {
                    // обычный проход
                }
            }
        }
        totalReward += reward;
        boolean truncated = !terminal && steps >= maxSteps;
        if (truncated && event == StepEvent.MOVE) {
            event = StepEvent.TIMEOUT;
        }
        return new StepResult(state, action, reward, event, terminal, truncated);
    }

    private static int countPassable(Maze maze) {
        int count = 0;
        for (int y = 0; y < maze.getHeight(); y++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                if (maze.get(x, y).isPassable()) {
                    count++;
                }
            }
        }
        return count;
    }
}
