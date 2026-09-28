package com.vadim.maze.service;

import com.vadim.maze.configuration.LearningProperties;
import com.vadim.maze.model.Action;
import com.vadim.maze.model.MouseState;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.random.RandomGenerator;

/**
 * Агент-мышь, обучаемый методом Q-learning (Watkins, 1989).
 * <pre>
 *   Q(s,a) ← Q(s,a) + α · [ r + γ · max_a' Q(s',a') − Q(s,a) ]
 * </pre>
 * Действия выбираются ε-жадно: с вероятностью ε — случайное (исследование),
 * иначе — лучшее по Q (использование опыта). ε убывает от эпизода к эпизоду.
 */
public class QLearningAgent {

    private static final Action[] ACTIONS = Action.values();

    private final Map<MouseState, double[]> qTable = new ConcurrentHashMap<>();
    private final RandomGenerator random;
    @Getter
    private final LearningProperties learning;
    @Getter
    private volatile double epsilon;

    public QLearningAgent(LearningProperties learning, RandomGenerator random) {
        this.learning = learning;
        this.random = random;
        this.epsilon = learning.getEpsilonStart();
    }

    public Action chooseAction(MouseState state, boolean explore) {
        if (explore && random.nextDouble() < epsilon) {
            return ACTIONS[random.nextInt(ACTIONS.length)];
        }
        return bestAction(state, explore);
    }

    /** Лучшее действие; при равенстве — случайное среди лучших (в режиме обучения) или первое. */
    public Action bestAction(MouseState state, boolean randomTieBreak) {
        double[] q = qValues(state);
        int best = 0;
        int ties = 1;
        for (int a = 1; a < q.length; a++) {
            if (q[a] > q[best]) {
                best = a;
                ties = 1;
            } else if (q[a] == q[best] && randomTieBreak && random.nextInt(++ties) == 0) {
                best = a;
            }
        }
        return ACTIONS[best];
    }

    public void update(MouseState state, Action action, double reward, MouseState next, boolean terminal) {
        double[] q = qTable.computeIfAbsent(state, s -> initialRow());
        double target = terminal ? reward : reward + learning.getGamma() * maxQ(next);
        q[action.ordinal()] += learning.getAlpha() * (target - q[action.ordinal()]);
    }

    public void decayEpsilon() {
        epsilon = Math.max(learning.getEpsilonMin(), epsilon * learning.getEpsilonDecay());
    }

    public void resetEpsilon() {
        epsilon = learning.getEpsilonStart();
    }

    public double[] qValues(MouseState state) {
        double[] q = qTable.get(state);
        return q == null ? initialRow() : q.clone();
    }

    /** V(s) = max_a Q(s,a); null — состояние ещё не посещалось. */
    public Double value(MouseState state) {
        double[] q = qTable.get(state);
        return q == null ? null : maxOf(q);
    }

    public int knownStates() {
        return qTable.size();
    }

    private double maxQ(MouseState state) {
        double[] q = qTable.get(state);
        return q == null ? learning.getInitialQ() : maxOf(q);
    }

    private double[] initialRow() {
        double[] row = new double[ACTIONS.length];
        java.util.Arrays.fill(row, learning.getInitialQ());
        return row;
    }

    private static double maxOf(double[] q) {
        double max = q[0];
        for (int i = 1; i < q.length; i++) {
            max = Math.max(max, q[i]);
        }
        return max;
    }
}
