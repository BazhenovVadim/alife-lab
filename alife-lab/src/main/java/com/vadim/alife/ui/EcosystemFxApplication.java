package com.vadim.alife.ui;

import com.vadim.alife.AlifeApplication;
import com.vadim.alife.config.SimulationProperties;
import com.vadim.alife.environment.Environment;
import com.vadim.alife.model.Agent;
import com.vadim.alife.model.AgentType;
import com.vadim.alife.simulation.SimulationEngine;
import com.vadim.alife.simulation.SimulationStats;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** JavaFX interface for configuring, stepping and observing the ecosystem. */
public class EcosystemFxApplication extends Application {

    private static final int CANVAS_SIZE = 760;
    private final Map<String, TextField> fields = new LinkedHashMap<>();
    private SimulationEngine engine;
    private SimulationProperties properties;
    private Canvas canvas;
    private Label statsLabel;
    private Label statusLabel;
    private Timeline timer;
    private boolean initialized;

    @Override
    public void start(Stage stage) {
        engine = AlifeApplication.getBean(SimulationEngine.class);
        properties = AlifeApplication.getBean(SimulationProperties.class);
        canvas = new Canvas(CANVAS_SIZE, CANVAS_SIZE);
        canvas.setOnMouseClicked(event -> stepOnce());

        BorderPane root = new BorderPane();
        root.setLeft(createSettingsPane());
        root.setCenter(createWorldPane());
        root.setBottom(createControls());

        stage.setTitle("Искусственная жизнь — экосистема");
        stage.setScene(new Scene(root, 1120, 860));
        stage.setMinWidth(900);
        stage.setMinHeight(700);
        stage.setOnCloseRequest(event -> {
            stopTimer();
            AlifeApplication.closeContext();
            Platform.exit();
        });
        stage.show();
        resetSimulation();
    }

    private ScrollPane createSettingsPane() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(7);
        grid.setPadding(new Insets(12));
        addSection(grid, "Карта и начальная популяция");
        addInt(grid, "Ширина", properties::getGridWidth, properties::setGridWidth);
        addInt(grid, "Высота", properties::getGridHeight, properties::setGridHeight);
        addInt(grid, "Растения", properties::getInitialPlants, properties::setInitialPlants);
        addInt(grid, "Зайцы", properties::getInitialHerbivores, properties::setInitialHerbivores);
        addInt(grid, "Волки", properties::getInitialPredators, properties::setInitialPredators);
        addSection(grid, "Растения");
        addDouble(grid, "Начальная энергия растения", properties::getPlantInitialEnergy, properties::setPlantInitialEnergy);
        addDouble(grid, "Прирост энергии за шаг", properties::getPlantEnergyPerStep, properties::setPlantEnergyPerStep);
        addDouble(grid, "Потеря энергии растения", properties::getPlantEnergyDecayPerStep, properties::setPlantEnergyDecayPerStep);
        addDouble(grid, "Максимальная энергия", properties::getPlantMaxEnergy, properties::setPlantMaxEnergy);
        addDouble(grid, "Порог размножения", properties::getPlantReproductionThreshold, properties::setPlantReproductionThreshold);
        addDouble(grid, "Цена размножения", properties::getPlantReproductionCost, properties::setPlantReproductionCost);
        addSection(grid, "Зайцы");
        addDouble(grid, "Начальная энергия зайца", properties::getHerbivoreInitialEnergy, properties::setHerbivoreInitialEnergy);
        addDouble(grid, "Расход энергии зайца", properties::getHerbivoreEnergyLossPerStep, properties::setHerbivoreEnergyLossPerStep);
        addInt(grid, "Радиус зрения зайца", properties::getHerbivoreVisionRadius, properties::setHerbivoreVisionRadius);
        addDouble(grid, "Порог размножения зайца", properties::getHerbivoreReproductionThreshold, properties::setHerbivoreReproductionThreshold);
        addDouble(grid, "Цена размножения зайца", properties::getHerbivoreReproductionCost, properties::setHerbivoreReproductionCost);
        addInt(grid, "Пауза размножения зайца", properties::getHerbivoreReproductionCooldownSteps, properties::setHerbivoreReproductionCooldownSteps);
        addSection(grid, "Волки");
        addDouble(grid, "Начальная энергия волка", properties::getPredatorInitialEnergy, properties::setPredatorInitialEnergy);
        addDouble(grid, "Расход энергии волка", properties::getPredatorEnergyLossPerStep, properties::setPredatorEnergyLossPerStep);
        addInt(grid, "Радиус зрения волка", properties::getPredatorVisionRadius, properties::setPredatorVisionRadius);
        addDouble(grid, "Порог размножения волка", properties::getPredatorReproductionThreshold, properties::setPredatorReproductionThreshold);
        addDouble(grid, "Цена размножения волка", properties::getPredatorReproductionCost, properties::setPredatorReproductionCost);
        addInt(grid, "Пауза размножения волка", properties::getPredatorReproductionCooldownSteps, properties::setPredatorReproductionCooldownSteps);

        ScrollPane pane = new ScrollPane(grid);
        pane.setFitToWidth(true);
        pane.setPrefWidth(310);
        pane.setMinWidth(260);
        return pane;
    }

    private VBox createWorldPane() {
        statsLabel = new Label();
        statsLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        statusLabel = new Label("Настройте параметры и запустите симуляцию.");
        VBox box = new VBox(8, statsLabel, canvas, statusLabel,
                new Label("🌸 растение   🐇 травоядное   🐺 хищник   •   карта замкнута: переход через край ведёт на противоположную сторону"));
        box.setPadding(new Insets(14));
        box.setAlignment(Pos.TOP_CENTER);
        return box;
    }

    private HBox createControls() {
        Button reset = new Button("Применить настройки / Сбросить");
        reset.setOnAction(event -> resetSimulation());
        Button step = new Button("Один шаг");
        step.setOnAction(event -> stepOnce());
        Button automatic = new Button("Запустить автоматически");
        automatic.setOnAction(event -> {
            if (timer != null && timer.getStatus() == Timeline.Status.RUNNING) {
                stopTimer();
                automatic.setText("Запустить автоматически");
            } else {
                if (!initialized) {
                    resetSimulation();
                }
                timer = new Timeline(new KeyFrame(Duration.millis(180), tick -> stepOnce()));
                timer.setCycleCount(Timeline.INDEFINITE);
                timer.play();
                automatic.setText("Остановить");
            }
        });
        HBox bar = new HBox(10, reset, step, automatic);
        bar.setPadding(new Insets(12));
        bar.setAlignment(Pos.CENTER);
        return bar;
    }

    private void resetSimulation() {
        stopTimer();
        try {
            applySettings();
            engine.initialize();
            initialized = true;
            statusLabel.setText("Новая экосистема создана. Можно запускать автоматически или выполнять шаги вручную.");
            redraw();
        } catch (IllegalArgumentException exception) {
            initialized = false;
            statusLabel.setText("Ошибка настройки: " + exception.getMessage());
        }
    }

    private void stepOnce() {
        if (!initialized) {
            resetSimulation();
        }
        if (!initialized) {
            return;
        }
        engine.step();
        redraw();
        if (engine.isEcosystemCollapsed()) {
            stopTimer();
            statusLabel.setText("Экосистема вымерла. Измените настройки и создайте новую.");
        }
    }

    private void redraw() {
        Environment environment = engine.getEnvironment();
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.setFill(Color.web("#e9f5e5"));
        graphics.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
        double cell = Math.min(CANVAS_SIZE / (double) environment.getWidth(), CANVAS_SIZE / (double) environment.getHeight());
        double usedWidth = cell * environment.getWidth();
        double usedHeight = cell * environment.getHeight();
        double offsetX = (CANVAS_SIZE - usedWidth) / 2;
        double offsetY = (CANVAS_SIZE - usedHeight) / 2;
        graphics.setFont(Font.font(Math.max(4, Math.min(18, cell * 1.05))));
        graphics.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        graphics.setTextBaseline(javafx.geometry.VPos.CENTER);
        for (int y = 0; y < environment.getHeight(); y++) {
            for (int x = 0; x < environment.getWidth(); x++) {
                Agent agent = environment.getAgent(x, y);
                if (agent != null) {
                    graphics.fillText(symbol(agent.getType()), offsetX + (x + .5) * cell, offsetY + (y + .5) * cell);
                }
            }
        }
        graphics.setStroke(Color.web("#9ac898"));
        graphics.strokeRect(offsetX, offsetY, usedWidth, usedHeight);
        SimulationStats stats = engine.collectStats();
        statsLabel.setText(String.format("Занято: %d | 🌸 %d | 🐇 %d | 🐺 %d", stats.getOccupiedCells(), stats.getPlantCount(), stats.getHerbivoreCount(), stats.getPredatorCount()));
    }

    private String symbol(AgentType type) {
        return switch (type) {
            case PLANT -> "🌸";
            case HERBIVORE -> "🐇";
            case PREDATOR -> "🐺";
        };
    }

    private void applySettings() {
        fields.forEach((name, field) -> {
            try {
                if (name.startsWith("int:")) {
                    int value = Integer.parseInt(field.getText().trim());
                    if (value < 0) throw new IllegalArgumentException("значение «" + name.substring(4) + "» не может быть отрицательным");
                    ((Consumer<Integer>) field.getUserData()).accept(value);
                } else {
                    double value = Double.parseDouble(field.getText().trim());
                    if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("некорректное значение «" + name.substring(7) + "»");
                    ((Consumer<Double>) field.getUserData()).accept(value);
                }
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("введите число для «" + name.substring(name.indexOf(':') + 1) + "»");
            }
        });
        if (properties.getGridWidth() == 0 || properties.getGridHeight() == 0) {
            throw new IllegalArgumentException("ширина и высота должны быть больше нуля");
        }
    }

    private void addSection(GridPane grid, String title) {
        int row = grid.getRowCount();
        Label label = new Label(title);
        label.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 2 0;");
        grid.add(label, 0, row, 2, 1);
    }

    private void addInt(GridPane grid, String label, Supplier<Integer> getter, Consumer<Integer> setter) {
        addField(grid, label, Integer.toString(getter.get()), "int:" + label, setter);
    }

    private void addDouble(GridPane grid, String label, Supplier<Double> getter, Consumer<Double> setter) {
        addField(grid, label, Double.toString(getter.get()), "double:" + label, setter);
    }

    private void addField(GridPane grid, String label, String value, String key, Object setter) {
        int row = grid.getRowCount();
        TextField field = new TextField(value);
        field.setPrefColumnCount(8);
        field.setUserData(setter);
        fields.put(key, field);
        grid.add(new Label(label), 0, row);
        grid.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
        }
    }
}
