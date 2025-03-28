package de.antl.parser.model;

import de.antl.parser.enums.TokenType;

public record Token(
        TokenType type,
        String value
) {

    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                ", value='" + value + '\'' +
                '}';
    }
}
