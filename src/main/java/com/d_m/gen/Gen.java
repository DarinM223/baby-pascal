package com.d_m.gen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Gen {
    public static void generateFile(String filename) throws IOException {
        Path path = Paths.get(filename);
        String content = Files.readString(path);
        System.out.println("Content: " + content);
        Scanner scanner = new Scanner(content);
        List<Token> tokens = scanner.scanTokens();
        System.out.println("Tokens: " + tokens);
        Parser parser = new Parser(tokens);
        List<Rule> rules = new ArrayList<>();
        while (parser.peek().type() != TokenType.EOF) {
            rules.add(parser.parseRule());
        }

        for (Rule rule : rules) {
            System.out.println("Rule: " + rule);
        }

        Automata automata = new Automata(rules);
        String name = com.google.common.io.Files.getNameWithoutExtension(path.getFileName().toString()).toUpperCase();
        System.out.println("Generating class: " + name);
        File automataJava = new File("src/main/java/com/d_m/gen/rules/" + name + ".java");
        AutomataWriter writer = new AutomataWriter(automata, new FileWriter(automataJava));
        writer.write(name);
    }

    public static void main(String[] args) throws IOException {
        for (String filename : args) {
            System.out.println("Generating for file: " + filename);
            generateFile(filename);
        }
    }
}

