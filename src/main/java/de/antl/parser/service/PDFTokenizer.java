package de.antl.parser.service;

import de.antl.parser.enums.TokenType;
import de.antl.parser.model.Token;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.Stack;

public class PDFTokenizer {
    private final BufferedReader reader;
    private int currentChar;
    private Token peekedToken;

    public PDFTokenizer(Reader input) throws IOException {
        this.reader = new BufferedReader(input);
        this.currentChar = reader.read();
        this.peekedToken = null;
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

    public String peekString(int length) throws IOException {
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

    private String nextString(int length) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append((char) currentChar);
        for (int i = 0; i < length; i++) {
            int read = reader.read();
            sb.append((char) read);
        }
        return sb.toString();
    }

    private void nextChar(int length) throws IOException {
        for (int i = 0; i < length; i++) {
            currentChar = reader.read();
        }
    }

    public Token peekToken() throws IOException {
        if (peekedToken == null) {
            // Generate the next token and cache it.
            peekedToken = nextToken();
        }
        return peekedToken;
    }

    public Token nextToken() throws IOException {
        if (peekedToken != null) {
            Token token = peekedToken;
            peekedToken = null;
            return token;
        }

        if (currentChar == -1) {
            return null;
        }

        if (currentChar == '%' && peekAhead(1) == 'P') {
            StringBuilder sb = new StringBuilder();
            nextChar();
            while (currentChar != -1 && !Character.isWhitespace(currentChar)) {
                sb.append((char) currentChar);
                nextChar();
            }
            return new Token(TokenType.VERSION, sb.toString());
        }

        skipWhitespaceAndComments();

        if (currentChar == '<' && peekAhead() != '<') {
            StringBuilder hex = new StringBuilder();
            nextChar();
            while (currentChar != -1 && currentChar != '>') {
                hex.append((char) currentChar);
                nextChar();
            }
            nextChar();
            return new Token(TokenType.HEX_STRING, hex.toString());
        }

        if (currentChar == '(') {
            Stack<Integer> stack = new Stack<>();
            stack.push(currentChar);
            StringBuilder stringObject = new StringBuilder();
            nextChar();
            while (currentChar != 1 && !stack.isEmpty()) {
                if (currentChar == '(') {
                    stack.push(currentChar);
                }
                if (currentChar == ')') {
                    stack.pop();
                    nextChar();
                    if (stack.isEmpty()) {
                        break;
                    }
                }
                stringObject.append((char) currentChar);
                nextChar();
            }

            return new Token(TokenType.STRING, stringObject.toString());
        }

        if (peekString(5).equals("stream")) {
            nextChar(6);
            skipWhitespaceAndComments();
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while (currentChar != -1) {
                // Here, you need a way to peek ahead to see if the next 8 bytes equal "endstream".
                // One strategy is to read into a buffer and check if "endstream" appears at the current position.
                // This example assumes you have a method `peekBytes(int n)` that returns the next n bytes as a string.
                if (peekString(8).equals("endstream")) {
                    break;
                }
                // Append the current byte to the output stream.
                byteStream.write(currentChar);
                nextChar();
            }
            nextChar(9);
            return new Token(TokenType.STREAM_DATA, byteStream.toByteArray());
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
            dict.append((char) currentChar);
            nextChar();
            return new Token(TokenType.DICT_START, dict.toString());
        }

        if (currentChar == '>' && peekAhead() == '>') {
            StringBuilder dict = new StringBuilder();
            dict.append((char) currentChar);
            nextChar();
            dict.append((char) currentChar);
            nextChar();
            return new Token(TokenType.DICT_END, dict.toString());
        }

        if (currentChar == '[') {
            String array = String.valueOf((char) currentChar);
            nextChar();
            return new Token(TokenType.ARRAY_START, array);
        }
        if (currentChar == ']') {
            String array = String.valueOf((char) currentChar);
            nextChar();
            return new Token(TokenType.ARRAY_END, array);
        }

        StringBuilder operator = new StringBuilder();
        while (currentChar != -1 && !Character.isWhitespace(currentChar) && "{}[]()/<>".indexOf(currentChar) == -1) {
            operator.append((char) currentChar);
            nextChar();
        }
        if(operator.isEmpty()) return null;
        return new Token(TokenType.OPERATOR, operator.toString());
    }
}
