package de.antl;

import de.antl.parser.model.Token;
import de.antl.parser.service.PDFTokenizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        try (BufferedReader br = Files.newBufferedReader(Path.of("src/main/java/de/antl/resources/test.pdf"), StandardCharsets.ISO_8859_1)) {
            PDFTokenizer tokenizer = new PDFTokenizer(br);
            Token token = tokenizer.nextToken();
            while (token != null) {
                token = tokenizer.nextToken();
                System.out.println(token);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}