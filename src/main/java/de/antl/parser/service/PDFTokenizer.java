package de.antl.parser.service;

import de.antl.parser.enums.TokenType;
import de.antl.parser.model.Token;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Stack;

public class PDFTokenizer {
    private final BufferedReader reader;
    private int currentChar;

    public PDFTokenizer(BufferedReader input) throws IOException {
        this.reader = input;
        this.currentChar = reader.read();
    }

    private void nextChar() throws IOException {
        currentChar = reader.read();
    }

    private void skipWhitespaceAndComments() throws IOException {
        while (currentChar != -1) {
            if (Character.isWhitespace(currentChar)) {
                nextChar();
            } else if (currentChar == '%') {
                while (currentChar != -1 && currentChar != '\n' && currentChar != '\r') {
                    nextChar();
                }
            } else {
                break;
            }
        }
    }

    private int peekAhead() throws IOException {
        reader.mark(1);
        int read = reader.read();
        reader.reset();
        return read;
    }

    private int peekAhead(int length) throws IOException {
        reader.mark(length);
        int read = -1;
        for (int i = 0; i < length; i++) {
            read = reader.read();
        }
        reader.reset();
        return read;
    }

    private String nextString(int length) throws IOException {
        reader.mark(length);
        StringBuilder sb = new StringBuilder();
        sb.append((char) currentChar);
        for (int i = 0; i < length; i++) {
            int read = reader.read();
            sb.append((char) read);
        }
        reader.reset();
        return sb.toString();
    }

    public Token nextToken() throws IOException {
        skipWhitespaceAndComments();

        if (currentChar == -1) {
            return null;
        }

        if (currentChar == '<' && peekAhead() != '<') {
            StringBuilder hex = new StringBuilder();
            hex.append((char) currentChar);
            nextChar();
            while (currentChar != -1 && currentChar != '>') {
                hex.append(Integer.toHexString(currentChar));
                nextChar();
            }
            hex.append((char) currentChar);
            nextChar();
            return new Token(TokenType.HEX_STRING, hex.toString());
        }

        if (currentChar == '(') {
            Stack<Integer> stack = new Stack<>();
            stack.push(currentChar);
            StringBuilder stringObject = new StringBuilder();
            stringObject.append((char) currentChar);
            nextChar();
            while (currentChar != 1 && !stack.isEmpty()) {
                stringObject.append((char) currentChar);
                if (currentChar == ')') {
                    stack.pop();
                }
                if (currentChar == '(') {
                    stack.push(currentChar);
                }
                nextChar();
            }

            return new Token(TokenType.STRING, stringObject.toString());
        }

        if (nextString(5).equals("stream")) {
            StringBuilder stream = new StringBuilder();
            stream.append((char) currentChar);
            nextChar();
            while (currentChar != -1 && !nextString(8).equals("endstream")) {
                stream.append((char) currentChar);
                nextChar();
            }
            for (int i = 0; i < 9; i++) {
                stream.append((char) currentChar);
                nextChar();
            }
            return new Token(TokenType.STREAM, stream.toString());
        }

        if (Character.isDigit(currentChar) || currentChar == '-' || currentChar == '+') {
            StringBuilder number = new StringBuilder();
            while (currentChar != -1 && (Character.isDigit(currentChar) || currentChar == '.' || currentChar == 'e' || currentChar == 'E' || currentChar == '-' || currentChar == '+')) {
                number.append((char) currentChar);
                nextChar();
            }
            return new Token(TokenType.NUMBER, number.toString());
        }

        if (currentChar == '/') {
            StringBuilder name = new StringBuilder();
            name.append((char) currentChar);
            nextChar();
            while (currentChar != -1 && !Character.isWhitespace(currentChar) && "{}[]()/<>".indexOf(currentChar) == -1) {
                name.append((char) currentChar);
                nextChar();
            }
            return new Token(TokenType.NAME, name.toString());
        }

        if (currentChar == '<' && peekAhead() == '<') {
            StringBuilder dict = new StringBuilder();
            dict.append((char) currentChar);
            nextChar();
            while (currentChar != -1) {
                if (currentChar == '>' && peekAhead() == '>') {
                    dict.append((char) currentChar);
                    nextChar();
                    dict.append((char) currentChar);
                    nextChar();
                    break;
                } else {
                    dict.append((char) currentChar);
                    nextChar();
                }
            }

            return new Token(TokenType.DICTIONARY, dict.toString());
        }

        if (currentChar == '[') {
            StringBuilder array = new StringBuilder();
            array.append((char) currentChar);
            nextChar();
            while (currentChar != -1) {
                array.append((char) currentChar);
                nextChar();
                if (currentChar == ']') {
                    array.append((char) currentChar);
                    nextChar();
                    break;
                }
            }
            return new Token(TokenType.ARRAY, array.toString());
        }

        StringBuilder operator = new StringBuilder();
        while (currentChar != -1 && !Character.isWhitespace(currentChar)) {
            operator.append((char) currentChar);
            nextChar();
        }
        return new Token(TokenType.OPERATOR, operator.toString());
    }
}
