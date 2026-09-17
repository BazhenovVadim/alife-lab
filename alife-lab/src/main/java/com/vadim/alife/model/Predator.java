package com.vadim.alife.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Двигается, обладает зрением 5x5, охотится на травоядных.
 * Не может занять клетку с растением - в клетке может быть только один агент.
 */
@Getter
@SuperBuilder
@NoArgsConstructor
public class Predator extends Agent {

    @Override
    public AgentType getType() {
        return AgentType.PREDATOR;
    }

    @Override
    public char getSymbol() {
        return 'X';
    }

    @Override
    public boolean isMobile() {
        return true;
    }
}
