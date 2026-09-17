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
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
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

    private static final double MIN_TICK_MS = 30;
    private static final double MAX_TICK_MS = 600;
    private static final double DEFAULT_TICK_MS = 180;
    private static final Duration MOVE_DURATION = Duration.millis(170);
    private static final Duration SPAWN_DURATION = Duration.millis(220);
    private static final Duration DEATH_DURATION = Duration.millis(240);

    private final Map<String, TextField> fields = new LinkedHashMap<>();
    private final Map<Agent, Circle> agentNodes = new IdentityHashMap<>();

    private SimulationEngine engine;
    private SimulationProperties properties;
    private Pane worldPane;
    private Label stepLabel;
    private Label statsLabel;
    private Label statusLabel;
    private Button automaticButton;
    private double tickMillis = DEFAULT_TICK_MS;
    private Timeline timer;
    private boolean initialized;
    private int currentStep;

    @Override
    public void start(Stage stage) {
        engine = AlifeApplication.getBean(SimulationEngine.class);
        properties = AlifeApplication.getBean(SimulationProperties.class);

        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(createWorldTab(), createSettingsTab());

        Label title = new Label("🌍 Искусственная жизнь — экосистема");
        title.getStyleClass().add("app-title");
        VBox header = new VBox(title);
        header.setPadding(new Insets(16, 20, 4, 20));

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        root.setTop(header);
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1280, 860);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setTitle("Искусственная жизнь — экосистема");
        stage.setScene(scene);
        stage.setMinWidth(980);
        stage.setMinHeight(700);
        stage.setOnCloseRequest(event -> {
            stopTimer();
            AlifeApplication.closeContext();
            Platform.exit();
        });
        stage.setMaximized(true);
        stage.show();
        resetSimulation();
    }

    // ---------------- вкладка "Мир" ----------------

    private Tab createWorldTab() {
        stepLabel = new Label("Шаг: 0");
        stepLabel.getStyleClass().add("step-label");
        statsLabel = new Label();
        statsLabel.getStyleClass().add("stats-label");
        HBox infoBar = new HBox(28, stepLabel, statsLabel);
        infoBar.setAlignment(Pos.CENTER_LEFT);

        worldPane = new Pane();
        worldPane.getStyleClass().add("world-canvas-frame");
        worldPane.setOnMouseClicked(event -> stepOnce());
        worldPane.widthProperty().addListener((obs, oldV, newV) -> relayout());
        worldPane.heightProperty().addListener((obs, oldV, newV) -> relayout());
        Tooltip.install(worldPane, new Tooltip("Клик по полю выполняет один шаг симуляции"));

        StackPane frame = new StackPane(worldPane);
        VBox.setVgrow(frame, Priority.ALWAYS);

        statusLabel = new Label("Настройте параметры и запустите симуляцию.");
        statusLabel.getStyleClass().add("status-label");

        HBox legend = createLegend();
        HBox controls = createControls();

        VBox content = new VBox(12, infoBar, frame, statusLabel, legend, controls);
        content.setMaxWidth(Double.MAX_VALUE);
        content.setMaxHeight(Double.MAX_VALUE);
        content.setPadding(new Insets(16, 20, 16, 20));

        Tab tab = new Tab("🌍 Мир", content);
        tab.setClosable(false);
        return tab;
    }

    private HBox createLegend() {
        HBox legend = new HBox(20,
                legendItem(colorFor(AgentType.PLANT), "Растение"),
                legendItem(colorFor(AgentType.HERBIVORE), "Травоядное"),
                legendItem(colorFor(AgentType.PREDATOR), "Хищник"),
                new Label("•  карта замкнута: переход через край ведёт на противоположную сторону"));
        legend.getStyleClass().add("legend-box");
        legend.setAlignment(Pos.CENTER_LEFT);
        return legend;
    }

    private HBox legendItem(Color color, String text) {
        Circle dot = new Circle(6, color);
        Label label = new Label(text);
        label.getStyleClass().add("legend-label");
        HBox item = new HBox(6, dot, label);
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private HBox createControls() {
        automaticButton = new Button("▶  Автоматический режим");
        automaticButton.getStyleClass().addAll("action-button", "button-play");
        automaticButton.setTooltip(new Tooltip("Запустить непрерывный автопрогон симуляции"));
        automaticButton.setOnAction(event -> toggleAutomatic());

        Label speedCaption = new Label("Скорость:");
        speedCaption.getStyleClass().add("field-label");
        Slider speedSlider = new Slider(MIN_TICK_MS, MAX_TICK_MS, DEFAULT_TICK_MS);
        speedSlider.setPrefWidth(120);
        speedSlider.getStyleClass().add("speed-slider");
        // ползунок инвертирован: вправо - быстрее (меньше задержка между шагами)
        speedSlider.setValue(MAX_TICK_MS - DEFAULT_TICK_MS + MIN_TICK_MS);
        speedSlider.setTooltip(new Tooltip("Скорость автоматического режима"));
        speedSlider.valueProperty().addListener((obs, oldV, newV) -> {
            tickMillis = MAX_TICK_MS - newV.doubleValue() + MIN_TICK_MS;
            restartTimerIfRunning();
        });
        HBox speedGroup = new HBox(8, speedCaption, speedSlider);
        speedGroup.getStyleClass().add("steps-group");
        speedGroup.setAlignment(Pos.CENTER);

        TextField stepsField = new TextField("1");
        stepsField.setPrefColumnCount(4);
        stepsField.getStyleClass().add("steps-field");

        Button stepsButton = new Button("⏭  Выполнить");
        stepsButton.getStyleClass().addAll("action-button", "button-step");
        stepsButton.setTooltip(new Tooltip("Выполнить указанное число шагов подряд и остановиться"));
        stepsButton.setOnAction(event -> {
            try {
                int count = Integer.parseInt(stepsField.getText().trim());
                runSteps(count);
            } catch (NumberFormatException exception) {
                statusLabel.setText("Введите целое число шагов.");
            }
        });

        Label stepsCaption = new Label("Шагов:");
        stepsCaption.getStyleClass().add("field-label");
        HBox stepsGroup = new HBox(8, stepsCaption, stepsField, stepsButton);
        stepsGroup.getStyleClass().add("steps-group");
        stepsGroup.setAlignment(Pos.CENTER);

        HBox bar = new HBox(20, automaticButton, speedGroup, stepsGroup);
        bar.getStyleClass().add("control-bar");
        bar.setPadding(new Insets(14, 20, 14, 20));
        bar.setAlignment(Pos.CENTER);
        return bar;
    }

    private void toggleAutomatic() {
        if (timer != null && timer.getStatus() == Timeline.Status.RUNNING) {
            stopTimer();
        } else {
            if (!initialized) {
                resetSimulation();
            }
            startContinuousTimer();
            automaticButton.setText("⏸  Остановить");
            automaticButton.getStyleClass().add("running");
        }
    }

    private void startContinuousTimer() {
        timer = new Timeline(new KeyFrame(Duration.millis(tickMillis), tick -> stepOnce()));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    /** Если сейчас идёт бесконечный автопрогон, перезапускает его с новой скоростью без потери состояния. */
    private void restartTimerIfRunning() {
        boolean continuousRunning = timer != null && timer.getStatus() == Timeline.Status.RUNNING
                && timer.getCycleCount() == Timeline.INDEFINITE;
        if (continuousRunning) {
            timer.stop();
            startContinuousTimer();
        }
    }

    private void runSteps(int count) {
        if (count <= 0) {
            statusLabel.setText("Количество шагов должно быть больше нуля.");
            return;
        }
        stopTimer();
        if (!initialized) {
            resetSimulation();
        }
        timer = new Timeline(new KeyFrame(Duration.millis(tickMillis), tick -> stepOnce()));
        timer.setCycleCount(count);
        timer.play();
    }

    // ---------------- вкладка "Настройки" ----------------

    private Tab createSettingsTab() {
        VBox sections = new VBox(12);
        sections.getChildren().addAll(
                settingsSection("Карта и начальная популяция", grid -> {
                    addInt(grid, "Ширина", properties::getGridWidth, properties::setGridWidth);
                    addInt(grid, "Высота", properties::getGridHeight, properties::setGridHeight);
                    addInt(grid, "Растения", properties::getInitialPlants, properties::setInitialPlants);
                    addInt(grid, "Зайцы", properties::getInitialHerbivores, properties::setInitialHerbivores);
                    addInt(grid, "Волки", properties::getInitialPredators, properties::setInitialPredators);
                }),
                settingsSection("Растения", grid -> {
                    addDouble(grid, "Начальная энергия растения", properties::getPlantInitialEnergy, properties::setPlantInitialEnergy);
                    addDouble(grid, "Прирост энергии за шаг", properties::getPlantEnergyPerStep, properties::setPlantEnergyPerStep);
                    addDouble(grid, "Потеря энергии растения", properties::getPlantEnergyDecayPerStep, properties::setPlantEnergyDecayPerStep);
                    addDouble(grid, "Максимальная энергия", properties::getPlantMaxEnergy, properties::setPlantMaxEnergy);
                    addDouble(grid, "Порог размножения", properties::getPlantReproductionThreshold, properties::setPlantReproductionThreshold);
                    addDouble(grid, "Цена размножения", properties::getPlantReproductionCost, properties::setPlantReproductionCost);
                }),
                settingsSection("Зайцы", grid -> {
                    addDouble(grid, "Начальная энергия зайца", properties::getHerbivoreInitialEnergy, properties::setHerbivoreInitialEnergy);
                    addDouble(grid, "Расход энергии зайца", properties::getHerbivoreEnergyLossPerStep, properties::setHerbivoreEnergyLossPerStep);
                    addInt(grid, "Радиус зрения зайца", properties::getHerbivoreVisionRadius, properties::setHerbivoreVisionRadius);
                    addDouble(grid, "Порог размножения зайца", properties::getHerbivoreReproductionThreshold, properties::setHerbivoreReproductionThreshold);
                    addDouble(grid, "Цена размножения зайца", properties::getHerbivoreReproductionCost, properties::setHerbivoreReproductionCost);
                    addInt(grid, "Пауза размножения зайца", properties::getHerbivoreReproductionCooldownSteps, properties::setHerbivoreReproductionCooldownSteps);
                }),
                settingsSection("Волки", grid -> {
                    addDouble(grid, "Начальная энергия волка", properties::getPredatorInitialEnergy, properties::setPredatorInitialEnergy);
                    addDouble(grid, "Расход энергии волка", properties::getPredatorEnergyLossPerStep, properties::setPredatorEnergyLossPerStep);
                    addInt(grid, "Радиус зрения волка", properties::getPredatorVisionRadius, properties::setPredatorVisionRadius);
                    addDouble(grid, "Порог размножения волка", properties::getPredatorReproductionThreshold, properties::setPredatorReproductionThreshold);
                    addDouble(grid, "Цена размножения волка", properties::getPredatorReproductionCost, properties::setPredatorReproductionCost);
                    addInt(grid, "Пауза размножения волка", properties::getPredatorReproductionCooldownSteps, properties::setPredatorReproductionCooldownSteps);
                })
        );

        ScrollPane scroll = new ScrollPane(sections);
        scroll.getStyleClass().add("settings-pane");
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Button apply = new Button("🔄  Применить настройки и создать новую экосистему");
        apply.getStyleClass().addAll("action-button", "button-reset");
        apply.setTooltip(new Tooltip("Пересоздать экосистему с указанными выше параметрами"));
        apply.setOnAction(event -> resetSimulation());
        HBox applyBar = new HBox(apply);
        applyBar.setAlignment(Pos.CENTER);
        applyBar.setPadding(new Insets(14, 0, 4, 0));

        VBox content = new VBox(8, scroll, applyBar);
        content.setMaxWidth(Double.MAX_VALUE);
        content.setMaxHeight(Double.MAX_VALUE);
        content.setPadding(new Insets(16, 20, 16, 20));

        Tab tab = new Tab("⚙  Настройки", content);
        tab.setClosable(false);
        return tab;
    }

    private TitledPane settingsSection(String title, Consumer<GridPane> fieldBuilder) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(7);
        grid.setPadding(new Insets(10, 4, 4, 4));
        fieldBuilder.accept(grid);

        TitledPane pane = new TitledPane(title, grid);
        pane.setExpanded(true);
        pane.getStyleClass().add("settings-section");
        return pane;
    }

    // ---------------- симуляция ----------------

    private void resetSimulation() {
        stopTimer();
        try {
            applySettings();
            engine.initialize();
            initialized = true;
            currentStep = 0;
            clearWorld();
            statusLabel.setText("Новая экосистема создана. Можно запускать автоматически или выполнять шаги вручную.");
            statusLabel.getStyleClass().removeAll("status-error", "status-collapsed");
            redraw();
        } catch (IllegalArgumentException exception) {
            initialized = false;
            statusLabel.setText("Ошибка настройки: " + exception.getMessage());
            if (!statusLabel.getStyleClass().contains("status-error")) {
                statusLabel.getStyleClass().add("status-error");
            }
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
        currentStep++;
        redraw();
        if (engine.isEcosystemCollapsed()) {
            stopTimer();
            statusLabel.setText("Экосистема вымерла. Измените настройки и создайте новую.");
            if (!statusLabel.getStyleClass().contains("status-collapsed")) {
                statusLabel.getStyleClass().add("status-collapsed");
            }
        }
    }

    private void clearWorld() {
        worldPane.getChildren().clear();
        agentNodes.clear();
    }

    // ---------------- отрисовка ----------------

    private record WorldMetrics(double cell, double offsetX, double offsetY) {
    }

    private WorldMetrics computeMetrics(Environment environment) {
        double paneWidth = Math.max(worldPane.getWidth(), 100);
        double paneHeight = Math.max(worldPane.getHeight(), 100);
        double cell = Math.min(paneWidth / environment.getWidth(), paneHeight / environment.getHeight());
        double usedWidth = cell * environment.getWidth();
        double usedHeight = cell * environment.getHeight();
        return new WorldMetrics(cell, (paneWidth - usedWidth) / 2, (paneHeight - usedHeight) / 2);
    }

    private void redraw() {
        Environment environment = engine.getEnvironment();
        WorldMetrics metrics = computeMetrics(environment);

        Set<Agent> alive = new HashSet<>();
        for (int y = 0; y < environment.getHeight(); y++) {
            for (int x = 0; x < environment.getWidth(); x++) {
                Agent agent = environment.getAgent(x, y);
                if (agent == null) {
                    continue;
                }
                alive.add(agent);
                double targetX = metrics.offsetX() + x * metrics.cell();
                double targetY = metrics.offsetY() + y * metrics.cell();
                Circle node = agentNodes.get(agent);
                if (node == null) {
                    spawnAgentNode(agent, metrics.cell(), targetX, targetY);
                } else {
                    resizeAgentNode(node, metrics.cell());
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

        stepLabel.setText("Шаг: " + currentStep);
        SimulationStats stats = engine.collectStats();
        statsLabel.setText(String.format("Занято: %d  •  Растения: %d  •  Травоядные: %d  •  Хищники: %d",
                stats.getOccupiedCells(), stats.getPlantCount(), stats.getHerbivoreCount(), stats.getPredatorCount()));
    }

    /** Пересчитывает положение уже существующих агентов при изменении размера окна, без анимации. */
    private void relayout() {
        if (!initialized) {
            return;
        }
        Environment environment = engine.getEnvironment();
        WorldMetrics metrics = computeMetrics(environment);
        for (int y = 0; y < environment.getHeight(); y++) {
            for (int x = 0; x < environment.getWidth(); x++) {
                Agent agent = environment.getAgent(x, y);
                if (agent == null) {
                    continue;
                }
                Circle node = agentNodes.get(agent);
                if (node != null) {
                    resizeAgentNode(node, metrics.cell());
                    node.setLayoutX(metrics.offsetX() + x * metrics.cell());
                    node.setLayoutY(metrics.offsetY() + y * metrics.cell());
                }
            }
        }
    }

    private void spawnAgentNode(Agent agent, double cell, double x, double y) {
        Circle node = new Circle(cell * 0.38, colorFor(agent.getType()));
        node.setStroke(Color.web("#33413a", 0.35));
        node.setStrokeWidth(1);
        node.setCenterX(cell / 2.0);
        node.setCenterY(cell / 2.0);
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

    private void animateMove(Node node, double targetX, double targetY) {
        Timeline timeline = new Timeline(new KeyFrame(MOVE_DURATION,
                new KeyValue(node.layoutXProperty(), targetX, Interpolator.EASE_BOTH),
                new KeyValue(node.layoutYProperty(), targetY, Interpolator.EASE_BOTH)));
        timeline.play();
    }

    private void animateDeath(Node node) {
        FadeTransition fade = new FadeTransition(DEATH_DURATION, node);
        fade.setToValue(0);
        ScaleTransition scale = new ScaleTransition(DEATH_DURATION, node);
        scale.setToX(0.2);
        scale.setToY(0.2);
        ParallelTransition death = new ParallelTransition(fade, scale);
        death.setOnFinished(event -> worldPane.getChildren().remove(node));
        death.play();
    }

    private void resizeAgentNode(Circle node, double cell) {
        node.setRadius(cell * 0.38);
        node.setCenterX(cell / 2.0);
        node.setCenterY(cell / 2.0);
    }

    private Color colorFor(AgentType type) {
        return switch (type) {
            case PLANT -> Color.web("#2ecc71");
            case HERBIVORE -> Color.web("#2f8fe0");
            case PREDATOR -> Color.web("#e0473c");
        };
    }

    // ---------------- настройки ----------------

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
            automaticButton.setText("▶  Автоматический режим");
            automaticButton.getStyleClass().remove("running");
        }
    }
}
