package com.vadim.maze.service;

import com.vadim.maze.configuration.MazeProperties;
import com.vadim.maze.model.Maze;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Фасад для получения лабиринта: генерация по параметрам или загрузка схемы пользователя. */
@Service
@RequiredArgsConstructor
public class MazeService {

    private final MazeProperties properties;
    private final MazeGenerator generator;
    private final MazeParser parser;
    private final MazeValidator validator;
    private final ResourceLoader resourceLoader;

    /** Лабиринт по настройкам application.yml: схема из файла, если указана, иначе генерация. */
    public Maze initialMaze() {
        String schema = properties.getSchemaFile();
        return schema == null || schema.isBlank() ? generate(properties) : load(schema);
    }

    public Maze generate(MazeProperties p) {
        Maze maze = generator.generate(p.getWidth(), p.getHeight(), p.getWaterCount(), p.getShockCount(),
                p.getLoopFactor(), p.getSeed());
        validator.requireValid(maze);
        return maze;
    }

    public Maze parse(String text) {
        Maze maze = parser.parse(text);
        validator.requireValid(maze);
        return maze;
    }

    /** @param location путь к файлу или ресурс Spring ({@code classpath:mazes/example.txt}). */
    public Maze load(String location) {
        try {
            String text = location.startsWith("classpath:") || location.startsWith("file:")
                    ? resourceLoader.getResource(location).getContentAsString(StandardCharsets.UTF_8)
                    : Files.readString(Path.of(location), StandardCharsets.UTF_8);
            return parse(text);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать схему " + location, e);
        }
    }

    public void save(Maze maze, Path file) {
        try {
            Files.writeString(file, maze.toText(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось сохранить схему " + file, e);
        }
    }
}
