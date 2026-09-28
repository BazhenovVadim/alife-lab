package com.vadim.maze.ui;

import com.vadim.maze.model.Action;
import com.vadim.maze.model.CellType;
import com.vadim.maze.model.Maze;
import com.vadim.maze.model.MouseState;
import com.vadim.maze.model.Position;
import com.vadim.maze.service.QLearningAgent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Отрисовка лабиринта, мыши, её следа и выученной политики. */
public class MazeCanvas extends Pane {

    private static final Color BACKGROUND = Color.web("#1b1340");
    private static final Color FLOOR = Color.web("#241a55");
    private static final Color WALL = Color.web("#7a45e6");
    private static final Color WALL_EDGE = Color.web("#9b6cff");
    private static final Color CHEESE = Color.web("#ffc83d");
    private static final Color WATER = Color.web("#4fc3f7");
    private static final Color SHOCK = Color.web("#ffe14d");
    private static final Color MOUSE = Color.web("#d7d7e0");
    private static final Color TRAIL = Color.web("#ff8a65", 0.55);
    private static final Color ARROW = Color.web("#ffffff", 0.55);

    private final Canvas canvas = new Canvas();
    private final List<Position> trail = new ArrayList<>();

    private Maze maze;
    private Map<Position, Integer> waterIndex = Map.of();
    @Setter
    private QLearningAgent agent;
    @Setter
    private Position mouse;
    @Setter
    private long waterMask;
    @Setter
    private boolean showPolicy = true;
    @Setter
    private boolean showValues;
    @Setter
    private boolean showTrail = true;

    public MazeCanvas() {
        getChildren().add(canvas);
        setMinSize(200, 200);
    }

    public void setMaze(Maze maze) {
        this.maze = maze;
        this.waterIndex = maze.waterIndex();
        this.mouse = safeStart(maze);
        this.waterMask = 0;
        trail.clear();
    }

    public void clearTrail() {
        trail.clear();
    }

    public void addTrail(Position position) {
        trail.add(position);
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        redraw();
    }

    /** Клетка лабиринта под точкой экрана — для редактора схемы. */
    public Optional<Position> cellAt(double px, double py) {
        if (maze == null) {
            return Optional.empty();
        }
        double cell = cellSize();
        Position p = new Position((int) Math.floor((px - offsetX(cell)) / cell),
                (int) Math.floor((py - offsetY(cell)) / cell));
        return maze.inBounds(p) ? Optional.of(p) : Optional.empty();
    }

    public void redraw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(BACKGROUND);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (maze == null) {
            return;
        }
        double cell = cellSize();
        double ox = offsetX(cell);
        double oy = offsetY(cell);

        double[] valueRange = showValues ? valueRange() : null;
        for (int y = 0; y < maze.getHeight(); y++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                Position p = new Position(x, y);
                double cx = ox + x * cell;
                double cy = oy + y * cell;
                CellType type = maze.get(p);
                if (type == CellType.WALL) {
                    g.setFill(WALL);
                    g.fillRect(cx, cy, cell, cell);
                    g.setStroke(WALL_EDGE);
                    g.setLineWidth(1);
                    g.strokeRect(cx + 0.5, cy + 0.5, cell - 1, cell - 1);
                    continue;
                }
                g.setFill(FLOOR);
                g.fillRect(cx, cy, cell, cell);
                if (valueRange != null) {
                    drawValue(g, p, cx, cy, cell, valueRange);
                }
            }
        }

        if (showTrail && trail.size() > 1) {
            g.setStroke(TRAIL);
            g.setLineWidth(Math.max(2, cell * 0.18));
            g.setLineCap(StrokeLineCap.ROUND);
            for (int i = 1; i < trail.size(); i++) {
                Position a = trail.get(i - 1);
                Position b = trail.get(i);
                g.strokeLine(ox + (a.x() + 0.5) * cell, oy + (a.y() + 0.5) * cell,
                        ox + (b.x() + 0.5) * cell, oy + (b.y() + 0.5) * cell);
            }
        }

        for (int y = 0; y < maze.getHeight(); y++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                Position p = new Position(x, y);
                double cx = ox + x * cell;
                double cy = oy + y * cell;
                switch (maze.get(p)) {
                    case CHEESE -> drawCheese(g, cx, cy, cell);
                    case WATER -> drawWater(g, cx, cy, cell, isDrunk(p) ? 0.25 : 1.0);
                    case SHOCK -> drawShock(g, cx, cy, cell);
                    default -> { }
                }
                if (showPolicy && maze.get(p).isPassable() && maze.get(p) != CellType.CHEESE) {
                    drawPolicyArrow(g, p, cx, cy, cell);
                }
            }
        }

        if (mouse != null) {
            drawMouse(g, ox + mouse.x() * cell, oy + mouse.y() * cell, cell);
        }
    }

    private boolean isDrunk(Position p) {
        Integer index = waterIndex.get(p);
        return index != null && (waterMask & (1L << index)) != 0;
    }

    private void drawPolicyArrow(GraphicsContext g, Position p, double cx, double cy, double cell) {
        if (agent == null || cell < 10) {
            return;
        }
        MouseState state = new MouseState(p, waterMask);
        if (agent.value(state) == null) {
            return;
        }
        Action a = agent.bestAction(state, false);
        double mx = cx + cell / 2;
        double my = cy + cell / 2;
        double len = cell * 0.28;
        double ex = mx + a.getDx() * len;
        double ey = my + a.getDy() * len;
        g.setStroke(ARROW);
        g.setLineWidth(Math.max(1, cell * 0.06));
        g.setLineCap(StrokeLineCap.ROUND);
        g.strokeLine(mx - a.getDx() * len * 0.6, my - a.getDy() * len * 0.6, ex, ey);
        double head = cell * 0.12;
        double px = -a.getDy();
        double py = a.getDx();
        g.strokeLine(ex, ey, ex - a.getDx() * head + px * head, ey - a.getDy() * head + py * head);
        g.strokeLine(ex, ey, ex - a.getDx() * head - px * head, ey - a.getDy() * head - py * head);
    }

    private double[] valueRange() {
        if (agent == null) {
            return null;
        }
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int y = 0; y < maze.getHeight(); y++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                Double v = agent.value(new MouseState(new Position(x, y), waterMask));
                if (v != null) {
                    min = Math.min(min, v);
                    max = Math.max(max, v);
                }
            }
        }
        return min <= max ? new double[]{min, max} : null;
    }

    private void drawValue(GraphicsContext g, Position p, double cx, double cy, double cell, double[] range) {
        Double v = agent.value(new MouseState(p, waterMask));
        if (v == null) {
            return;
        }
        double t = range[1] > range[0] ? (v - range[0]) / (range[1] - range[0]) : 1;
        g.setFill(Color.hsb(t * 120, 0.85, 0.85, 0.45));
        g.fillRect(cx, cy, cell, cell);
    }

    private void drawCheese(GraphicsContext g, double x, double y, double s) {
        double[] xs = {x + s * 0.12, x + s * 0.88, x + s * 0.88, x + s * 0.12};
        double[] ys = {y + s * 0.62, y + s * 0.30, y + s * 0.82, y + s * 0.82};
        g.setFill(CHEESE);
        g.fillPolygon(xs, ys, 4);
        g.setFill(Color.web("#e0a21a"));
        g.fillOval(x + s * 0.30, y + s * 0.62, s * 0.12, s * 0.12);
        g.fillOval(x + s * 0.58, y + s * 0.50, s * 0.14, s * 0.14);
        g.fillOval(x + s * 0.66, y + s * 0.70, s * 0.08, s * 0.08);
    }

    private void drawWater(GraphicsContext g, double x, double y, double s, double opacity) {
        g.setGlobalAlpha(opacity);
        g.setFill(WATER);
        g.fillPolygon(new double[]{x + s * 0.5, x + s * 0.30, x + s * 0.70},
                new double[]{y + s * 0.14, y + s * 0.55, y + s * 0.55}, 3);
        g.fillOval(x + s * 0.28, y + s * 0.40, s * 0.44, s * 0.44);
        g.setFill(Color.web("#ffffff", 0.6));
        g.fillOval(x + s * 0.38, y + s * 0.52, s * 0.09, s * 0.14);
        g.setGlobalAlpha(1);
    }

    private void drawShock(GraphicsContext g, double x, double y, double s) {
        double[] xs = {0.58, 0.28, 0.48, 0.38, 0.72, 0.52, 0.62};
        double[] ys = {0.10, 0.55, 0.55, 0.90, 0.42, 0.42, 0.10};
        double[] px = new double[xs.length];
        double[] py = new double[ys.length];
        for (int i = 0; i < xs.length; i++) {
            px[i] = x + xs[i] * s;
            py[i] = y + ys[i] * s;
        }
        g.setFill(SHOCK);
        g.fillPolygon(px, py, xs.length);
    }

    private void drawMouse(GraphicsContext g, double x, double y, double s) {
        g.setFill(MOUSE);
        g.fillOval(x + s * 0.14, y + s * 0.12, s * 0.30, s * 0.30);
        g.fillOval(x + s * 0.56, y + s * 0.12, s * 0.30, s * 0.30);
        g.setFill(Color.web("#f4a7b9"));
        g.fillOval(x + s * 0.20, y + s * 0.18, s * 0.18, s * 0.18);
        g.fillOval(x + s * 0.62, y + s * 0.18, s * 0.18, s * 0.18);
        g.setFill(MOUSE);
        g.fillOval(x + s * 0.24, y + s * 0.30, s * 0.52, s * 0.52);
        g.setFill(Color.web("#222222"));
        g.fillOval(x + s * 0.36, y + s * 0.48, s * 0.07, s * 0.07);
        g.fillOval(x + s * 0.57, y + s * 0.48, s * 0.07, s * 0.07);
        g.setFill(Color.web("#f06292"));
        g.fillOval(x + s * 0.46, y + s * 0.62, s * 0.08, s * 0.07);
    }

    private double cellSize() {
        return Math.max(1, Math.floor(Math.min(canvas.getWidth() / maze.getWidth(),
                canvas.getHeight() / maze.getHeight())));
    }

    private double offsetX(double cell) {
        return Math.floor((canvas.getWidth() - cell * maze.getWidth()) / 2);
    }

    private double offsetY(double cell) {
        return Math.floor((canvas.getHeight() - cell * maze.getHeight()) / 2);
    }

    private static Position safeStart(Maze maze) {
        List<Position> starts = maze.findAll(CellType.START);
        return starts.size() == 1 ? starts.get(0) : null;
    }
}
