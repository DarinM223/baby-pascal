package com.d_m.gen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Gen {
    public static void generateFile(String filename) throws IOException {
        String content = Files.readString(Paths.get(filename));
        System.out.println("Content: " + content);
        Scanner scanner = new Scanner(content);
        List<Token> tokens = scanner.scanTokens();
        System.out.println("Tokens: " + tokens);
    }

    public static void main(String[] args) throws IOException {
        for (String filename : args) {
            System.out.println("Generating for file: " + filename);
            generateFile(filename);
        }
    }
}
