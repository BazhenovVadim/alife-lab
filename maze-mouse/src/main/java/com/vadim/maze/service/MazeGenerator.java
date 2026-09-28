package com.vadim.maze.service;

import com.vadim.maze.model.CellType;
import com.vadim.maze.model.Maze;
import com.vadim.maze.model.Position;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Генератор лабиринта произвольного размера.
 * Каркас строится поиском в глубину с возвратом (recursive backtracker) —
 * получается «идеальный» лабиринт, где до сыра ровно один путь.
 * Затем часть внутренних стен убирается ({@code loopFactor}), чтобы у мыши
 * появился выбор: длинный безопасный путь или короткий через электроток.
 */
@Service
public class MazeGenerator {

    public Maze generate(int roomsX, int roomsY, int waterCount, int shockCount, double loopFactor, Long seed) {
        if (roomsX < 2 || roomsY < 2) {
            throw new IllegalArgumentException("Размер лабиринта — минимум 2x2 комнаты");
        }
        Random random = seed == null ? new Random() : new Random(seed);
        Maze maze = new Maze(roomsX * 2 + 1, roomsY * 2 + 1);

        carvePassages(maze, roomsX, roomsY, random);
        openLoops(maze, loopFactor, random);

        // Как на рисунке задачи: сыр в левом верхнем углу, мышь — в правом нижнем.
        Position cheese = new Position(1, 1);
        Position start = new Position(maze.getWidth() - 2, maze.getHeight() - 2);
        maze.set(cheese, CellType.CHEESE);
        maze.set(start, CellType.START);

        List<Position> free = maze.findAll(CellType.EMPTY);
        Collections.shuffle(free, random);
        int water = Math.min(Math.min(waterCount, Maze.MAX_WATER), free.size());
        int shocks = Math.min(shockCount, free.size() - water);
        for (int i = 0; i < water; i++) {
            maze.set(free.get(i), CellType.WATER);
        }
        for (int i = water; i < water + shocks; i++) {
            maze.set(free.get(i), CellType.SHOCK);
        }
        return maze;
    }

    private void carvePassages(Maze maze, int roomsX, int roomsY, Random random) {
        boolean[][] visited = new boolean[roomsY][roomsX];
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{0, 0});
        visited[0][0] = true;
        maze.set(1, 1, CellType.EMPTY);
        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

        while (!stack.isEmpty()) {
            int[] room = stack.peek();
            List<int[]> neighbours = new ArrayList<>(4);
            for (int[] d : dirs) {
                int nx = room[0] + d[0];
                int ny = room[1] + d[1];
                if (nx >= 0 && ny >= 0 && nx < roomsX && ny < roomsY && !visited[ny][nx]) {
                    neighbours.add(new int[]{nx, ny});
                }
            }
            if (neighbours.isEmpty()) {
                stack.pop();
                continue;
            }
            int[] next = neighbours.get(random.nextInt(neighbours.size()));
            visited[next[1]][next[0]] = true;
            // Стена между комнатами лежит посередине, комната (i,j) -> клетка (2i+1, 2j+1).
            maze.set(room[0] + next[0] + 1, room[1] + next[1] + 1, CellType.EMPTY);
            maze.set(next[0] * 2 + 1, next[1] * 2 + 1, CellType.EMPTY);
            stack.push(next);
        }
    }

    private void openLoops(Maze maze, double loopFactor, Random random) {
        if (loopFactor <= 0) {
            return;
        }
        List<Position> innerWalls = new ArrayList<>();
        for (int y = 1; y < maze.getHeight() - 1; y++) {
            for (int x = 1; x < maze.getWidth() - 1; x++) {
                boolean separatesRoomsHorizontally = x % 2 == 0 && y % 2 == 1;
                boolean separatesRoomsVertically = x % 2 == 1 && y % 2 == 0;
                if ((separatesRoomsHorizontally || separatesRoomsVertically) && maze.get(x, y) == CellType.WALL) {
                    innerWalls.add(new Position(x, y));
                }
            }
        }
        Collections.shuffle(innerWalls, random);
        int toRemove = (int) Math.round(innerWalls.size() * Math.min(loopFactor, 1.0));
        for (int i = 0; i < toRemove; i++) {
            maze.set(innerWalls.get(i), CellType.EMPTY);
        }
    }
}
