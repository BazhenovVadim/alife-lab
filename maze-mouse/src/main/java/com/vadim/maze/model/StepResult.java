package com.vadim.maze.model;

/**
 * Отклик среды на действие — «обратная связь» (подкрепление).
 *
 * @param terminal  эпизод завершён по смыслу задачи (сыр найден)
 * @param truncated эпизод прерван по лимиту шагов — для Q-learning это не конечное состояние
 */
public record StepResult(MouseState state, Action action, double reward, StepEvent event,
                         boolean terminal, boolean truncated) {

    public boolean done() {
        return terminal || truncated;
    }
}
