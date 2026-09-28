package com.vadim.maze.service;

import com.vadim.maze.model.CellType;
import com.vadim.maze.model.Maze;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Разбор текстовой схемы лабиринта, заданной пользователем.
 * Символы: {@code #} стена, {@code .} или пробел — проход, {@code M} мышь,
 * {@code C} сыр, {@code W} вода, {@code E} электроток.
 * Короткие строки дополняются стенами; клетки за пределами сетки тоже считаются стеной.
 * Строки, начинающиеся с {@code ;}, — комментарии.
 */
@Service
public class MazeParser {

    public Maze parse(String text) {
        List<String> lines = text.lines()
                .map(String::stripTrailing)
                .filter(line -> !line.isBlank() && !line.startsWith(";"))
                .toList();
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Схема лабиринта пуста");
        }
        int width = lines.stream().mapToInt(String::length).max().orElse(0);
        Maze maze = new Maze(Math.max(width, 3), Math.max(lines.size(), 3));
        for (int y = 0; y < lines.size(); y++) {
            String line = lines.get(y);
            for (int x = 0; x < line.length(); x++) {
                char symbol = line.charAt(x);
                int column = x;
                int row = y;
                CellType type = CellType.fromSymbol(symbol).orElseThrow(() -> new IllegalArgumentException(
                        "Неизвестный символ '" + symbol + "' в строке " + (row + 1) + ", столбце " + (column + 1)));
                maze.set(x, y, type);
            }
        }
        return maze;
    }
}
