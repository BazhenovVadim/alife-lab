package com.vadim.alife.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Двигается, обладает зрением 5x5, ест растения, убегает от хищников.
 */
@Getter
@SuperBuilder
@NoArgsConstructor
public class Herbivore extends Agent {

    @Override
    public AgentType getType() {
        return AgentType.HERBIVORE;
    }

    @Override
    public char getSymbol() {
        return 'H';
    }

    @Override
    public boolean isMobile() {
        return true;
    }
}
