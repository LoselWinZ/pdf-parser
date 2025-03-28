package de.antl.parser.service;

import java.io.BufferedReader;
import java.io.Reader;

public class PDFTokenizer {
    private BufferedReader reader;
    private int currentChar;

    public PDFTokenizer(Reader input) {
        this.reader = new BufferedReader(input);
        this.currentChar = reader.read();
    }
}
