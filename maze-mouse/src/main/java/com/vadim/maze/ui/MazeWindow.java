package com.vadim.maze.ui;

import com.vadim.maze.configuration.LearningProperties;
import com.vadim.maze.configuration.MazeProperties;
import com.vadim.maze.configuration.RewardProperties;
import com.vadim.maze.model.Action;
import com.vadim.maze.model.CellType;
import com.vadim.maze.model.EpisodeStats;
import com.vadim.maze.model.Maze;
import com.vadim.maze.model.MouseState;
import com.vadim.maze.model.Position;
import com.vadim.maze.model.StepEvent;
import com.vadim.maze.model.StepResult;
import com.vadim.maze.model.TrainingSummary;
import com.vadim.maze.service.MazeEnvironment;
import com.vadim.maze.service.MazeService;
import com.vadim.maze.service.MazeValidator;
import com.vadim.maze.service.TrainingService;
import com.vadim.maze.service.TrainingSession;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/** Главное окно: настройка и редактирование лабиринта, обучение мыши и визуализация. */
public class MazeWindow {

    private static final int LOG_LIMIT = 400;

    private final Stage stage;
    private final MazeService mazeService;
    private final MazeValidator validator;
    private final TrainingService trainingService;
    private final MazeProperties mazeProps;
    private final RewardProperties rewards;
    private final LearningProperties learning;

    private final MazeCanvas canvas = new MazeCanvas();
    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private final BooleanProperty noSession = new SimpleBooleanProperty(true);

    private Maze maze;
    private TrainingSession session;
    private volatile boolean stopRequested;

    private final XYChart.Series<Number, Number> rewardSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> successSeries = new XYChart.Series<>();
    private final ListView<String> log = new ListView<>();
    private final Label statusLabel = new Label();
    private final Label episodesLabel = new Label("0");
    private final Label epsilonLabel = new Label("—");
    private final Label statesLabel = new Label("0");
    private final Label successLabel = new Label("—");
    private final Label avgRewardLabel = new Label("—");
    private final Label avgStepsLabel = new Label("—");
    private final Label stepLabel = new Label("0");
    private final Label eventLabel = new Label("—");
    private final Label lastRewardLabel = new Label("—");
    private final Label totalLabel = new Label("0");
    private final Label qLabel = new Label("—");

    private final ToggleButton editToggle = new ToggleButton("Рисовать");
    private final ComboBox<CellType> brush = new ComboBox<>();
    private final Slider speed = new Slider(1, 120, 20);
    private final TextField seedField = new TextField();

    private Playback playback;

    public MazeWindow(Stage stage, MazeService mazeService, MazeValidator validator, TrainingService trainingService,
                      MazeProperties mazeProps, RewardProperties rewards, LearningProperties learning) {
        this.stage = stage;
        this.mazeService = mazeService;
        this.validator = validator;
        this.trainingService = trainingService;
        this.mazeProps = mazeProps;
        this.rewards = rewards;
        this.learning = learning;
    }

    public Parent build() {
        BorderPane root = new BorderPane();
        root.setLeft(settingsPane());
        root.setCenter(centerPane());
        root.setRight(statsPane());
        root.setBottom(controlBar());

        try {
            setMaze(mazeService.initialMaze());
        } catch (RuntimeException e) {
            showError("Не удалось загрузить лабиринт из настроек", e);
            setMaze(mazeService.generate(mazeProps));
        }
        return root;
    }

    /* ---------------------------------------------------------------- layout */

    private Node settingsPane() {
        GridPane mazeGrid = grid();
        int row = 0;
        mazeGrid.addRow(row++, new Label("Ширина, комнат"), intSpinner(2, 60, mazeProps.getWidth(), mazeProps::setWidth));
        mazeGrid.addRow(row++, new Label("Высота, комнат"), intSpinner(2, 60, mazeProps.getHeight(), mazeProps::setHeight));
        mazeGrid.addRow(row++, new Label("Вода, клеток"), intSpinner(0, Maze.MAX_WATER, mazeProps.getWaterCount(), mazeProps::setWaterCount));
        mazeGrid.addRow(row++, new Label("Ток, клеток"), intSpinner(0, 999, mazeProps.getShockCount(), mazeProps::setShockCount));
        mazeGrid.addRow(row++, new Label("Доля петель"), doubleSpinner(0, 1, mazeProps.getLoopFactor(), 0.05, mazeProps::setLoopFactor));
        seedField.setPromptText("случайно");
        seedField.setText(mazeProps.getSeed() == null ? "" : mazeProps.getSeed().toString());
        seedField.setTooltip(new Tooltip("Одинаковое зерно — одинаковый лабиринт"));
        mazeGrid.addRow(row++, new Label("Зерно (seed)"), seedField);

        Button generate = button("Сгенерировать", this::generateMaze);
        Button schema = button("Схема…", this::editSchemaText);
        Button open = button("Открыть…", this::openSchema);
        Button save = button("Сохранить…", this::saveSchema);
        generate.setMaxWidth(Double.MAX_VALUE);
        brush.getItems().addAll(CellType.values());
        brush.setValue(CellType.WALL);
        brush.setConverter(new StringConverter<>() {
            @Override
            public String toString(CellType type) {
                return type == null ? "" : type.getSymbol() + "  " + type.getTitle();
            }

            @Override
            public CellType fromString(String s) {
                return null;
            }
        });
        editToggle.setTooltip(new Tooltip("Рисуйте схему мышкой по полю лабиринта"));
        VBox mazeBox = new VBox(8, mazeGrid, generate, new HBox(6, schema, open, save),
                new HBox(6, editToggle, brush));
        mazeBox.disableProperty().bind(busy);

        GridPane rewardGrid = grid();
        row = 0;
        rewardGrid.addRow(row++, new Label("Сыр, +Z"), doubleSpinner(0, 10_000, rewards.getCheese(), 10, rewards::setCheese));
        rewardGrid.addRow(row++, new Label("Вода, +x"), doubleSpinner(0, 10_000, rewards.getWater(), 1, rewards::setWater));
        rewardGrid.addRow(row++, new Label("Ток, −y"), doubleSpinner(-10_000, 0, rewards.getShock(), 5, rewards::setShock));
        rewardGrid.addRow(row++, new Label("Шаг"), doubleSpinner(-100, 0, rewards.getStep(), 0.5, rewards::setStep));
        rewardGrid.addRow(row++, new Label("Удар о стену"), doubleSpinner(-100, 0, rewards.getWallHit(), 1, rewards::setWallHit));

        GridPane learnGrid = grid();
        row = 0;
        learnGrid.addRow(row++, new Label("α (скорость)"), doubleSpinner(0.01, 1, learning.getAlpha(), 0.05, learning::setAlpha));
        learnGrid.addRow(row++, new Label("γ (дисконт)"), doubleSpinner(0, 1, learning.getGamma(), 0.01, learning::setGamma));
        learnGrid.addRow(row++, new Label("ε начальное"), doubleSpinner(0, 1, learning.getEpsilonStart(), 0.05, learning::setEpsilonStart));
        learnGrid.addRow(row++, new Label("ε минимум"), doubleSpinner(0, 1, learning.getEpsilonMin(), 0.01, learning::setEpsilonMin));
        learnGrid.addRow(row++, new Label("Затухание ε"), doubleSpinner(0.9, 1, learning.getEpsilonDecay(), 0.001, learning::setEpsilonDecay));
        Spinner<Double> initialQ = doubleSpinner(-10_000, 10_000, learning.getInitialQ(), 10, learning::setInitialQ);
        initialQ.setTooltip(new Tooltip("Оптимистичная оценка ≈ +Z заставляет мышь пробовать неиспытанные ходы"));
        learnGrid.addRow(row++, new Label("Начальное Q"), initialQ);
        learnGrid.addRow(row++, new Label("Эпизодов"), intSpinner(1, 1_000_000, learning.getEpisodes(), learning::setEpisodes));
        Spinner<Integer> maxSteps = intSpinner(0, 1_000_000, learning.getMaxSteps(), learning::setMaxSteps);
        maxSteps.setTooltip(new Tooltip("0 — авто; применяется после «Сбросить опыт»"));
        learnGrid.addRow(row++, new Label("Лимит шагов"), maxSteps);
        learnGrid.disableProperty().bind(busy);

        VBox box = new VBox(
                titled("Лабиринт", mazeBox),
                titled("Подкрепление (награды)", rewardGrid),
                titled("Q-learning", learnGrid));
        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(330);
        scroll.setMinWidth(300);
        return scroll;
    }

    private Node centerPane() {
        canvas.setOnMousePressed(this::paintCell);
        canvas.setOnMouseDragged(this::paintCell);
        statusLabel.setPadding(new Insets(4, 8, 4, 8));
        statusLabel.setWrapText(true);
        VBox.setVgrow(canvas, Priority.ALWAYS);
        return new VBox(statusLabel, canvas);
    }

    private Node statsPane() {
        GridPane learn = grid();
        int row = 0;
        learn.addRow(row++, new Label("Эпизодов"), episodesLabel);
        learn.addRow(row++, new Label("ε сейчас"), epsilonLabel);
        learn.addRow(row++, new Label("Состояний в Q"), statesLabel);
        learn.addRow(row++, new Label("Сыр найден (100)"), successLabel);
        learn.addRow(row++, new Label("Ср. награда (100)"), avgRewardLabel);
        learn.addRow(row++, new Label("Ср. шагов (100)"), avgStepsLabel);

        GridPane walk = grid();
        row = 0;
        totalLabel.setFont(Font.font(null, javafx.scene.text.FontWeight.BOLD, 22));
        walk.addRow(row++, new Label("Шаг"), stepLabel);
        walk.addRow(row++, new Label("Событие"), eventLabel);
        walk.addRow(row++, new Label("Награда за шаг"), lastRewardLabel);
        walk.addRow(row++, new Label("Сумма выигрыша"), totalLabel);
        qLabel.setStyle("-fx-font-family: monospace;");
        qLabel.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        qLabel.setTooltip(new Tooltip("Q-значения клетки до шага «Шаг»; * — выбранное действие"));
        walk.addRow(row++, new Label("Q(s,·) до шага"), qLabel);

        rewardSeries.setName("Средняя награда за эпизод");
        successSeries.setName("Сыр найден, %");
        LineChart<Number, Number> rewardChart = chart(rewardSeries, "Эпизод", "Награда");
        LineChart<Number, Number> successChart = chart(successSeries, "Эпизод", "%");
        log.setPrefHeight(180);
        log.setStyle("-fx-font-family: monospace;");

        VBox box = new VBox(6,
                titled("Обучение", learn),
                titled("Текущий проход (обратная связь)", walk),
                rewardChart, successChart,
                new Label("Журнал подкреплений"), log);
        box.setPadding(new Insets(6));
        box.setPrefWidth(380);
        box.setMinWidth(340);
        VBox.setVgrow(log, Priority.ALWAYS);
        return box;
    }

    private Node controlBar() {
        Button train = button("▶ Обучить", this::startTraining);
        Button stop = button("■ Стоп", this::stopAll);
        Button step = button("Шаг ▸", this::manualStep);
        Button episode = button("Эпизод с обучением", this::animateTrainingEpisode);
        Button walk = button("Пройти лабиринт", this::animateGreedyRun);
        Button reset = button("Сбросить опыт", this::resetSession);
        train.setTooltip(new Tooltip("Фоновое обучение на заданное число эпизодов"));
        episode.setTooltip(new Tooltip("Один ε-жадный эпизод с обновлением Q-таблицы, показывается анимацией"));
        walk.setTooltip(new Tooltip("Проход по выученной политике без случайных шагов и без обучения"));
        step.setTooltip(new Tooltip("Мышь делает один ε-жадный ход с обновлением Q; эпизод продолжается с того же места"));
        stop.setTooltip(new Tooltip("Остановить обучение или анимацию прохода"));

        train.disableProperty().bind(busy.or(noSession));
        episode.disableProperty().bind(busy.or(noSession));
        walk.disableProperty().bind(busy.or(noSession));
        step.disableProperty().bind(busy.or(noSession));
        reset.disableProperty().bind(busy.or(noSession));
        stop.disableProperty().bind(busy.not());

        CheckBox policy = new CheckBox("Политика (стрелки)");
        policy.setSelected(true);
        policy.selectedProperty().addListener((o, a, b) -> { canvas.setShowPolicy(b); canvas.redraw(); });
        CheckBox values = new CheckBox("Ценность V(s)");
        values.selectedProperty().addListener((o, a, b) -> { canvas.setShowValues(b); canvas.redraw(); });
        CheckBox trail = new CheckBox("След");
        trail.setSelected(true);
        trail.selectedProperty().addListener((o, a, b) -> { canvas.setShowTrail(b); canvas.redraw(); });
        speed.setPrefWidth(140);

        FlowPane bar = new FlowPane(8, 6, train, stop, new Separator(), step, episode, walk, reset, new Separator(),
                new Label("Скорость, шаг/с"), speed, policy, values, trail);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8));
        return bar;
    }

    /* ---------------------------------------------------------------- maze */

    private void generateMaze() {
        try {
            String seed = seedField.getText().trim();
            mazeProps.setSeed(seed.isEmpty() ? null : Long.parseLong(seed));
            setMaze(mazeService.generate(mazeProps));
        } catch (NumberFormatException e) {
            showError("Зерно должно быть целым числом", e);
        } catch (RuntimeException e) {
            showError("Не удалось сгенерировать лабиринт", e);
        }
    }

    private void setMaze(Maze newMaze) {
        stopPlayback();
        maze = newMaze;
        canvas.setMaze(maze);
        List<String> errors = validator.validate(maze);
        if (errors.isEmpty()) {
            resetSession();
            setStatus("Лабиринт " + maze.getWidth() + "×" + maze.getHeight()
                    + ": вода — " + maze.findAll(CellType.WATER).size()
                    + ", ток — " + maze.findAll(CellType.SHOCK).size() + ". Мышь ничего не знает — нажмите «Обучить».",
                    false);
        } else {
            session = null;
            noSession.set(true);
            canvas.setAgent(null);
            clearStats();
            setStatus("Схема некорректна: " + String.join("; ", errors), true);
        }
        canvas.redraw();
    }

    private void paintCell(MouseEvent event) {
        if (!editToggle.isSelected() || busy.get()) {
            return;
        }
        canvas.cellAt(event.getX(), event.getY()).ifPresent(p -> {
            CellType type = brush.getValue();
            if (maze.get(p) == type) {
                return;
            }
            Maze edited = maze.copy();
            if (type == CellType.START || type == CellType.CHEESE) {
                edited.findAll(type).forEach(old -> edited.set(old, CellType.EMPTY));
            }
            edited.set(p, type);
            setMaze(edited);
        });
    }

    private void editSchemaText() {
        TextArea area = new TextArea(maze.toText());
        area.setStyle("-fx-font-family: monospace; -fx-font-size: 14px;");
        area.setPrefSize(520, 420);
        Label legend = new Label("# стена   . проход   M мышь   C сыр   W вода (+x)   E электроток (−y)\n"
                + "Строки, начинающиеся с «;», — комментарии.");
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(stage);
        dialog.setTitle("Схема лабиринта");
        dialog.getDialogPane().setContent(new VBox(8, legend, area));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(b -> {
            try {
                setMaze(mazeService.parse(area.getText()));
            } catch (RuntimeException e) {
                showError("Схема не принята", e);
            }
        });
    }

    private void openSchema() {
        File file = fileChooser("Открыть схему").showOpenDialog(stage);
        if (file != null) {
            try {
                setMaze(mazeService.load(file.getAbsolutePath()));
            } catch (RuntimeException e) {
                showError("Не удалось открыть схему", e);
            }
        }
    }

    private void saveSchema() {
        File file = fileChooser("Сохранить схему").showSaveDialog(stage);
        if (file != null) {
            try {
                mazeService.save(maze, file.toPath());
            } catch (RuntimeException e) {
                showError("Не удалось сохранить схему", e);
            }
        }
    }

    /* ---------------------------------------------------------------- training */

    private void resetSession() {
        stopPlayback();
        session = trainingService.createSession(maze);
        noSession.set(false);
        canvas.setAgent(session.getAgent());
        canvas.setMouse(maze.getStart());
        canvas.setWaterMask(0);
        canvas.clearTrail();
        clearStats();
        canvas.redraw();
    }

    private void startTraining() {
        stopPlayback();
        int episodes = learning.getEpisodes();
        int chunk = Math.max(1, episodes / 150);
        TrainingSession current = session;
        stopRequested = false;
        busy.set(true);
        setStatus("Обучение: " + episodes + " эпизодов…", false);

        Thread worker = new Thread(() -> {
            int[] done = {0};
            try {
                trainingService.train(current, episodes, stats -> {
                    done[0]++;
                    if (done[0] % chunk == 0 || done[0] == episodes) {
                        TrainingSummary window = trainingService.summarize(current, chunk);
                        int episode = stats.getEpisode();
                        Platform.runLater(() -> {
                            addChartPoint(episode, window);
                            refreshStats();
                            canvas.redraw();
                        });
                    }
                }, () -> stopRequested);
            } catch (RuntimeException e) {
                Platform.runLater(() -> showError("Ошибка обучения", e));
            } finally {
                Platform.runLater(() -> {
                    busy.set(false);
                    refreshStats();
                    canvas.redraw();
                    setStatus((stopRequested ? "Обучение остановлено. " : "Обучение завершено. ")
                            + "Нажмите «Пройти лабиринт», чтобы увидеть выученный маршрут.", false);
                });
            }
        }, "maze-training");
        worker.setDaemon(true);
        worker.start();
    }

    /** «Стоп»: прерывает и фоновое обучение, и анимацию прохода. */
    private void stopAll() {
        stopRequested = true;
        if (playback != null) {
            int shown = playback.index;
            int total = playback.steps.size();
            stopPlayback();
            canvas.redraw();
            setStatus("Показ остановлен на шаге " + shown + " из " + total + ".", false);
        }
    }

    /** Один шаг мыши с обучением — видно, что выбрано, что ответила среда и как изменилось Q. */
    private void manualStep() {
        stopPlayback();
        boolean newEpisode = !session.isEpisodeInProgress();
        MouseState before = newEpisode ? new MouseState(maze.getStart(), 0L) : session.getProgress().getState();
        if (newEpisode) {
            log.getItems().clear();
            log.getItems().add("— Пошаговый эпизод " + (session.episodesDone() + 1)
                    + " (ε=" + fmt(session.getAgent().getEpsilon(), 3) + ") —");
            canvas.clearTrail();
            canvas.addTrail(maze.getStart());
        }
        double[] qBefore = session.getAgent().qValues(before);
        StepResult result = trainingService.step(session, true);
        double[] qAfter = session.getAgent().qValues(before);
        int a = result.action().ordinal();
        boolean greedy = qBefore[a] >= java.util.Arrays.stream(qBefore).max().orElse(0);

        MazeEnvironment environment = session.getEnvironment();
        canvas.setMouse(result.state().position());
        canvas.setWaterMask(result.state().waterMask());
        canvas.addTrail(result.state().position());
        showStep(environment.getSteps(), result, environment.getTotalReward(), true);
        qLabel.setText(formatQ(qBefore, a));
        log.getItems().add(String.format("    %s: Q %s → %s", greedy ? "жадный ход" : "случайный ход (ε)",
                fmt(qBefore[a], 1), fmt(qAfter[a], 1)));
        log.scrollTo(log.getItems().size() - 1);

        refreshStats();
        if (result.done()) {
            EpisodeStats stats = session.getLastEpisode();
            addChartPoint(stats.getEpisode(), trainingService.summarize(session, 1));
            setStatus("Пошаговый эпизод " + stats.getEpisode() + " завершён: "
                    + (result.terminal() ? "сыр найден" : "лимит шагов") + " за " + stats.getSteps()
                    + " шагов, сумма выигрыша " + fmt(stats.getTotalReward(), 1)
                    + ". Следующий «Шаг» начнёт новый эпизод.", !result.terminal());
        } else {
            setStatus("Пошаговый режим: " + (greedy ? "жадный" : "случайный") + " ход "
                    + result.action().getArrow() + ", " + eventTitle(result.event()) + " "
                    + signed(result.reward()) + ", сумма " + fmt(environment.getTotalReward(), 1), false);
        }
        canvas.redraw();
    }

    private static String formatQ(double[] q, int chosen) {
        StringBuilder sb = new StringBuilder();
        for (Action action : Action.values()) {
            int i = action.ordinal();
            if (i == 2) {
                sb.append('\n');
            } else if (i > 0) {
                sb.append("  ");
            }
            sb.append(action.getArrow()).append(String.format("%6.1f", q[i])).append(i == chosen ? '*' : ' ');
        }
        return sb.toString();
    }

    private void animateTrainingEpisode() {
        stopPlayback();
        List<StepResult> steps = new java.util.ArrayList<>();
        EpisodeStats stats = trainingService.trainEpisode(session, steps::add);
        addChartPoint(stats.getEpisode(), trainingService.summarize(session, 1));
        refreshStats();
        play(steps, "Эпизод " + stats.getEpisode() + " (ε=" + fmt(stats.getEpsilon(), 3) + ")");
    }

    private void animateGreedyRun() {
        stopPlayback();
        play(trainingService.greedyRun(session), "Проход по выученной политике");
    }

    private void play(List<StepResult> steps, String title) {
        log.getItems().clear();
        log.getItems().add("— " + title + " —");
        canvas.clearTrail();
        canvas.setMouse(maze.getStart());
        canvas.setWaterMask(0);
        canvas.addTrail(maze.getStart());
        showStep(0, null, 0, false);
        qLabel.setText("—");
        busy.set(true);
        playback = new Playback(steps, title);
        playback.start();
    }

    private void stopPlayback() {
        if (playback != null) {
            playback.stop();
            playback = null;
            busy.set(false);
        }
    }

    /** Покадровое воспроизведение шагов с учётом ползунка скорости. */
    private final class Playback extends AnimationTimer {
        private final List<StepResult> steps;
        private final String title;
        private int index;
        private double total;
        private long last;

        Playback(List<StepResult> steps, String title) {
            this.steps = steps;
            this.title = title;
        }

        @Override
        public void handle(long now) {
            if (last == 0) {
                last = now;
                return;
            }
            double interval = 1_000_000_000.0 / speed.getValue();
            while (now - last >= interval && index < steps.size()) {
                last += (long) interval;
                StepResult step = steps.get(index++);
                total += step.reward();
                canvas.setMouse(step.state().position());
                canvas.setWaterMask(step.state().waterMask());
                canvas.addTrail(step.state().position());
                showStep(index, step, total, false);
            }
            canvas.redraw();
            if (index >= steps.size()) {
                stop();
                playback = null;
                busy.set(false);
                StepResult end = steps.isEmpty() ? null : steps.get(steps.size() - 1);
                boolean found = end != null && end.terminal();
                setStatus(title + ": " + (found ? "сыр найден" : "сыр не найден") + " за " + steps.size()
                        + " шагов, сумма выигрыша " + fmt(total, 1), !found);
            }
        }
    }

    private void showStep(int number, StepResult step, double total, boolean logEveryStep) {
        stepLabel.setText(String.valueOf(number));
        totalLabel.setText(fmt(total, 1));
        if (step == null) {
            eventLabel.setText("старт");
            lastRewardLabel.setText("—");
            return;
        }
        eventLabel.setText(eventTitle(step.event()));
        lastRewardLabel.setText(signed(step.reward()));
        if (logEveryStep || step.event() != StepEvent.MOVE || step.done()) {
            log.getItems().add(String.format("%3d %s %-8s %-6s %6s Σ%7s", number, step.action().getArrow(),
                    step.state().position(), eventTitle(step.event()), signed(step.reward()), fmt(total, 1)));
            if (log.getItems().size() > LOG_LIMIT) {
                log.getItems().remove(1);
            }
            log.scrollTo(log.getItems().size() - 1);
        }
    }

    /* ---------------------------------------------------------------- stats */

    private void addChartPoint(int episode, TrainingSummary window) {
        rewardSeries.getData().add(new XYChart.Data<>(episode, window.averageReward()));
        successSeries.getData().add(new XYChart.Data<>(episode, window.successRate() * 100));
    }

    private void refreshStats() {
        if (session == null) {
            return;
        }
        TrainingSummary summary = trainingService.summarize(session, 100);
        episodesLabel.setText(String.valueOf(summary.episodes()));
        epsilonLabel.setText(fmt(session.getAgent().getEpsilon(), 3));
        statesLabel.setText(String.valueOf(session.getAgent().knownStates()));
        if (summary.episodes() > 0) {
            successLabel.setText(fmt(summary.successRate() * 100, 0) + " %");
            avgRewardLabel.setText(fmt(summary.averageReward(), 1));
            avgStepsLabel.setText(fmt(summary.averageSteps(), 1));
        }
    }

    private void clearStats() {
        rewardSeries.getData().clear();
        successSeries.getData().clear();
        log.getItems().clear();
        for (Label label : List.of(successLabel, avgRewardLabel, avgStepsLabel, eventLabel, lastRewardLabel)) {
            label.setText("—");
        }
        stepLabel.setText("0");
        totalLabel.setText("0");
        qLabel.setText("—");
        episodesLabel.setText("0");
        statesLabel.setText("0");
        epsilonLabel.setText(session == null ? "—" : fmt(session.getAgent().getEpsilon(), 3));
    }

    /* ---------------------------------------------------------------- helpers */

    private static String eventTitle(StepEvent event) {
        return switch (event) {
            case MOVE -> "шаг";
            case WALL_HIT -> "стена";
            case WATER -> "вода";
            case SHOCK -> "ТОК";
            case CHEESE -> "СЫР";
            case TIMEOUT -> "лимит";
        };
    }

    private void setStatus(String text, boolean error) {
        statusLabel.setText(text);
        statusLabel.setStyle(error ? "-fx-text-fill: #c62828;" : "");
    }

    private void showError(String header, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
        alert.initOwner(stage);
        alert.setHeaderText(header);
        alert.showAndWait();
    }

    private static FileChooser fileChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Схема лабиринта (*.txt)", "*.txt"));
        return chooser;
    }

    private static Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.setOnAction(e -> action.run());
        return button;
    }

    private static GridPane grid() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        return grid;
    }

    private static TitledPane titled(String title, Node content) {
        TitledPane pane = new TitledPane(title, content);
        pane.setCollapsible(true);
        return pane;
    }

    private static LineChart<Number, Number> chart(XYChart.Series<Number, Number> series, String x, String y) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel(x);
        xAxis.setForceZeroInRange(false);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(y);
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.getData().add(series);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setPrefHeight(190);
        chart.setLegendVisible(true);
        return chart;
    }

    private static Spinner<Integer> intSpinner(int min, int max, int value, IntConsumer onChange) {
        Spinner<Integer> spinner = new Spinner<>(min, max, value);
        spinner.setEditable(true);
        spinner.setPrefWidth(105);
        spinner.valueProperty().addListener((o, a, b) -> onChange.accept(b));
        commitOnFocusLost(spinner);
        return spinner;
    }

    private static Spinner<Double> doubleSpinner(double min, double max, double value, double step,
                                                 DoubleConsumer onChange) {
        Spinner<Double> spinner = new Spinner<>();
        SpinnerValueFactory.DoubleSpinnerValueFactory factory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, value, step);
        // Стандартный конвертер округляет до 2 знаков — 0.998 превратилось бы в 1.
        factory.setConverter(new StringConverter<>() {
            @Override
            public String toString(Double v) {
                return v == null ? "" : BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP)
                        .stripTrailingZeros().toPlainString();
            }

            @Override
            public Double fromString(String text) {
                return Double.parseDouble(text.trim().replace(',', '.'));
            }
        });
        spinner.setValueFactory(factory);
        spinner.setEditable(true);
        spinner.setPrefWidth(105);
        spinner.valueProperty().addListener((o, a, b) -> onChange.accept(b));
        commitOnFocusLost(spinner);
        return spinner;
    }

    private static <T> void commitOnFocusLost(Spinner<T> spinner) {
        spinner.focusedProperty().addListener((o, was, focused) -> {
            if (!focused) {
                try {
                    spinner.increment(0);
                } catch (RuntimeException e) {
                    spinner.getEditor().setText(String.valueOf(spinner.getValue()));
                }
            }
        });
    }

    private static String fmt(double value, int digits) {
        return String.format("%." + digits + "f", value);
    }

    private static String signed(double value) {
        return String.format("%+.1f", value);
    }
}
