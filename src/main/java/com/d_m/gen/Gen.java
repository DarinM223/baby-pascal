package com.d_m.gen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Gen {
    public static void generateFile(String filename) throws IOException {
        String content = Files.readString(Paths.get(filename));
        System.out.println("Content: " + content);
        Scanner scanner = new Scanner(content);
        List<Token> tokens = scanner.scanTokens();
        System.out.println("Tokens: " + tokens);
        Parser parser = new Parser(tokens);
        List<Rule> rules = new ArrayList<>();
        while (parser.peek().type != TokenType.EOF) {
            rules.add(parser.parseRule());
        }

        for (Rule rule : rules) {
            System.out.println("Rule: " + rule);
        }
    }

    public static void main(String[] args) throws IOException {
        for (String filename : args) {
            System.out.println("Generating for file: " + filename);
            generateFile(filename);
        }
    }
}
