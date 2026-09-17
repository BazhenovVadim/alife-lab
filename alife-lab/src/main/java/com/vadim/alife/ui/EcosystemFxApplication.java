package com.vadim.alife.ui;

import com.vadim.alife.AlifeApplication;
import com.vadim.alife.config.SimulationProperties;
import com.vadim.alife.environment.Environment;
import com.vadim.alife.model.Agent;
import com.vadim.alife.model.AgentType;
import com.vadim.alife.simulation.SimulationEngine;
import com.vadim.alife.simulation.SimulationStats;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** JavaFX интерфейс для настройки, пошагового и автоматического запуска экосистемы. */
public class EcosystemFxApplication extends Application {

    private static final int CANVAS_SIZE = 760;
    private static final Duration MOVE_DURATION = Duration.millis(170);
    private static final Duration SPAWN_DURATION = Duration.millis(220);
    private static final Duration DEATH_DURATION = Duration.millis(240);

    private final Map<String, TextField> fields = new LinkedHashMap<>();
    private final Map<Agent, Label> agentNodes = new IdentityHashMap<>();

    private SimulationEngine engine;
    private SimulationProperties properties;
    private Pane worldPane;
    private Label statsLabel;
    private Label statusLabel;
    private Button automaticButton;
    private Timeline timer;
    private boolean initialized;

    @Override
    public void start(Stage stage) {
        engine = AlifeApplication.getBean(SimulationEngine.class);
        properties = AlifeApplication.getBean(SimulationProperties.class);

        worldPane = new Pane();
        worldPane.setPrefSize(CANVAS_SIZE, CANVAS_SIZE);
        worldPane.setMinSize(CANVAS_SIZE, CANVAS_SIZE);
        worldPane.setMaxSize(CANVAS_SIZE, CANVAS_SIZE);
        worldPane.getStyleClass().add("world-canvas-frame");
        worldPane.setOnMouseClicked(event -> stepOnce());

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        root.setTop(createHeader());
        root.setLeft(createSettingsPane());
        root.setCenter(createWorldPane());
        root.setBottom(createControls());

        Scene scene = new Scene(root, 1160, 900);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setTitle("Искусственная жизнь — экосистема");
        stage.setScene(scene);
        stage.setMinWidth(940);
        stage.setMinHeight(740);
        stage.setOnCloseRequest(event -> {
            stopTimer();
            AlifeApplication.closeContext();
            Platform.exit();
        });
        stage.show();
        resetSimulation();
    }

    private VBox createHeader() {
        Label title = new Label("🌍 Искусственная жизнь — экосистема");
        title.getStyleClass().add("app-title");
        VBox box = new VBox(title);
        box.setPadding(new Insets(16, 20, 4, 20));
        return box;
    }

    private ScrollPane createSettingsPane() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(7);
        grid.setPadding(new Insets(14));
        grid.getStyleClass().add("settings-card");
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
        pane.getStyleClass().add("settings-pane");
        pane.setFitToWidth(true);
        pane.setPrefWidth(320);
        pane.setMinWidth(270);
        pane.setPadding(new Insets(12, 0, 12, 12));
        return pane;
    }

    private VBox createWorldPane() {
        statsLabel = new Label();
        statsLabel.getStyleClass().add("stats-label");
        statusLabel = new Label("Настройте параметры и запустите симуляцию.");
        statusLabel.getStyleClass().add("status-label");
        Label legend = new Label("🌸 растение   🐇 травоядное   🐺 хищник   •   карта ограничена: выйти за край нельзя");
        legend.getStyleClass().add("legend-label");

        StackPane frame = new StackPane(worldPane);
        frame.setPadding(new Insets(4));

        VBox box = new VBox(10, statsLabel, frame, statusLabel, legend);
        box.getStyleClass().add("world-card");
        box.setPadding(new Insets(18));
        box.setAlignment(Pos.TOP_CENTER);

        VBox wrapper = new VBox(box);
        wrapper.setPadding(new Insets(12, 18, 12, 12));
        return wrapper;
    }

    private HBox createControls() {
        Button reset = new Button("🔄  Применить настройки / Сбросить");
        reset.getStyleClass().addAll("action-button", "button-reset");
        reset.setOnAction(event -> resetSimulation());

        Button step = new Button("⏭  Шаг симуляции");
        step.getStyleClass().addAll("action-button", "button-step");
        step.setOnAction(event -> stepOnce());

        automaticButton = new Button("▶  Запустить приложение");
        automaticButton.getStyleClass().addAll("action-button", "button-play");
        automaticButton.setOnAction(event -> {
            if (timer != null && timer.getStatus() == Timeline.Status.RUNNING) {
                stopTimer();
            } else {
                if (!initialized) {
                    resetSimulation();
                }
                timer = new Timeline(new KeyFrame(Duration.millis(180), tick -> stepOnce()));
                timer.setCycleCount(Timeline.INDEFINITE);
                timer.play();
                automaticButton.setText("⏸  Остановить");
                automaticButton.getStyleClass().add("running");
            }
        });

        HBox bar = new HBox(14, reset, step, automaticButton);
        bar.getStyleClass().add("control-bar");
        bar.setPadding(new Insets(14, 20, 18, 20));
        bar.setAlignment(Pos.CENTER);
        return bar;
    }

    private void resetSimulation() {
        stopTimer();
        try {
            applySettings();
            engine.initialize();
            initialized = true;
            clearWorld();
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

    private void clearWorld() {
        worldPane.getChildren().clear();
        agentNodes.clear();
    }

    private void redraw() {
        Environment environment = engine.getEnvironment();
        double cell = Math.min(CANVAS_SIZE / (double) environment.getWidth(), CANVAS_SIZE / (double) environment.getHeight());
        double usedWidth = cell * environment.getWidth();
        double usedHeight = cell * environment.getHeight();
        double offsetX = (CANVAS_SIZE - usedWidth) / 2;
        double offsetY = (CANVAS_SIZE - usedHeight) / 2;

        Set<Agent> alive = new HashSet<>();
        for (int y = 0; y < environment.getHeight(); y++) {
            for (int x = 0; x < environment.getWidth(); x++) {
                Agent agent = environment.getAgent(x, y);
                if (agent == null) {
                    continue;
                }
                alive.add(agent);
                double targetX = offsetX + x * cell;
                double targetY = offsetY + y * cell;
                Label node = agentNodes.get(agent);
                if (node == null) {
                    spawnAgentNode(agent, cell, targetX, targetY);
                } else {
                    resizeAgentNode(node, cell);
                    animateMove(node, targetX, targetY);
                }
            }
        }

        agentNodes.entrySet().removeIf(entry -> {
            if (alive.contains(entry.getKey())) {
                return false;
            }
            animateDeath(entry.getValue());
            return true;
        });

        SimulationStats stats = engine.collectStats();
        statsLabel.setText(String.format("Занято: %d | 🌸 %d | 🐇 %d | 🐺 %d",
                stats.getOccupiedCells(), stats.getPlantCount(), stats.getHerbivoreCount(), stats.getPredatorCount()));
    }

    private void spawnAgentNode(Agent agent, double cell, double x, double y) {
        Label node = new Label(symbol(agent.getType()));
        node.getStyleClass().add("agent-symbol");
        resizeAgentNode(node, cell);
        node.setLayoutX(x);
        node.setLayoutY(y);
        node.setOpacity(0);
        node.setScaleX(0.3);
        node.setScaleY(0.3);
        worldPane.getChildren().add(node);
        agentNodes.put(agent, node);

        FadeTransition fade = new FadeTransition(SPAWN_DURATION, node);
        fade.setToValue(1);
        ScaleTransition scale = new ScaleTransition(SPAWN_DURATION, node);
        scale.setToX(1);
        scale.setToY(1);
        new ParallelTransition(fade, scale).play();
    }

    private void animateMove(Label node, double targetX, double targetY) {
        Timeline timeline = new Timeline(new KeyFrame(MOVE_DURATION,
                new KeyValue(node.layoutXProperty(), targetX, Interpolator.EASE_BOTH),
                new KeyValue(node.layoutYProperty(), targetY, Interpolator.EASE_BOTH)));
        timeline.play();
    }

    private void animateDeath(Label node) {
        FadeTransition fade = new FadeTransition(DEATH_DURATION, node);
        fade.setToValue(0);
        ScaleTransition scale = new ScaleTransition(DEATH_DURATION, node);
        scale.setToX(0.2);
        scale.setToY(0.2);
        ParallelTransition death = new ParallelTransition(fade, scale);
        death.setOnFinished(event -> worldPane.getChildren().remove(node));
        death.play();
    }

    private void resizeAgentNode(Label node, double cell) {
        node.setPrefWidth(cell);
        node.setPrefHeight(cell);
        node.setAlignment(Pos.CENTER);
        node.setFont(Font.font(Math.max(6, Math.min(22, cell * 0.95))));
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
        label.getStyleClass().add("section-label");
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
        Label fieldLabel = new Label(label);
        fieldLabel.getStyleClass().add("field-label");
        grid.add(fieldLabel, 0, row);
        grid.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
        }
        if (automaticButton != null) {
            automaticButton.setText("▶  Запустить приложение");
            automaticButton.getStyleClass().remove("running");
        }
    }
}
