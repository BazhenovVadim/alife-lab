package com.vadim.alife.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;


@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class Agent {

    private int x;
    private int y;
    private double energy;

    @Builder.Default
    private int age = 0;

    public abstract AgentType getType();

    public abstract char getSymbol();

    public abstract boolean isMobile();
}
