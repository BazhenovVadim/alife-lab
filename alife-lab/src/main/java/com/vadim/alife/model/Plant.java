package com.vadim.alife.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Не двигается, генерирует энергию из "солнца" каждый шаг,
 * размножается вегетативно в соседние клетки.
 */
@Getter
@SuperBuilder
@NoArgsConstructor
public class Plant extends Agent {

    @Override
    public AgentType getType() {
        return AgentType.PLANT;
    }

    @Override
    public char getSymbol() {
        return 'P';
    }

    @Override
    public boolean isMobile() {
        return false;
    }
}
