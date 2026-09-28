package com.vadim.maze.service;

import com.vadim.maze.model.Action;
import com.vadim.maze.model.CellType;
import com.vadim.maze.model.Maze;
import com.vadim.maze.model.Position;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Проверка пользовательской схемы: одна мышь, один сыр, сыр достижим. */
@Service
public class MazeValidator {

    /** @return список проблем; пустой — схема корректна. */
    public List<String> validate(Maze maze) {
        List<String> errors = new ArrayList<>();
        int starts = maze.findAll(CellType.START).size();
        int cheeses = maze.findAll(CellType.CHEESE).size();
        int water = maze.findAll(CellType.WATER).size();
        if (starts != 1) {
            errors.add("Нужна ровно одна мышь (M), сейчас: " + starts);
        }
        if (cheeses != 1) {
            errors.add("Нужен ровно один сыр (C), сейчас: " + cheeses);
        }
        if (water > Maze.MAX_WATER) {
            errors.add("Клеток воды не больше " + Maze.MAX_WATER + ", сейчас: " + water);
        }
        if (errors.isEmpty() && !isReachable(maze, maze.getStart(), maze.getCheese())) {
            errors.add("Сыр недостижим из стартовой клетки мыши");
        }
        return errors;
    }

    public void requireValid(Maze maze) {
        List<String> errors = validate(maze);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
    }

    public boolean isReachable(Maze maze, Position from, Position to) {
        Set<Position> seen = new HashSet<>();
        Deque<Position> queue = new ArrayDeque<>();
        queue.add(from);
        seen.add(from);
        while (!queue.isEmpty()) {
            Position current = queue.poll();
            if (current.equals(to)) {
                return true;
            }
            for (Action action : Action.values()) {
                Position next = current.move(action);
                if (maze.isPassable(next) && seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return false;
    }
}
