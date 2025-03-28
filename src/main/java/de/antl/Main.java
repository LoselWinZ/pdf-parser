package de.antl;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");

        try (BufferedReader br = Files.newBufferedReader(Path.of("src/main/resources/tokens.txt"))) {

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}