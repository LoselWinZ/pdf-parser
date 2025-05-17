package de.antl.parser.model;

import de.antl.parser.enums.TokenType;

public record Token(
        TokenType type,
        Object value
) {
    public String getStringValue() {
        return (String) value;
    }

    public byte[] getByteArrayValue() {
        return (byte[]) value;
    }

    @Override
    public String toString() {
        String displayValue;
        if (value instanceof byte[] bytes) {
            displayValue = "byte[" + bytes.length + "]";
        } else {
            displayValue = String.valueOf(value);
        }
        return "Token{" +
                "type=" + type +
                ", value='" + displayValue + '\'' +
                '}';
    }
}
