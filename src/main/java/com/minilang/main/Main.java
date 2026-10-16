// Notes:
// - I used M's Notes: to help determine who the comments came from and the new changed things.


package com.minilang.main;

import com.minilang.lexer.Lexer;
import com.minilang.lexer.Token;
import com.minilang.parser.Parser;
import com.minilang.ast.Stmt;
import com.minilang.ast.AstPrinter;
import com.minilang.semantic.SemanticAnalyzer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // M's Note: Read the MiniScript code from the file path I pass in.
        if (args.length == 0) {
            System.out.println(
                    "Usage: mvn exec:java -Dexec.mainClass=com.minilang.main.Main -Dexec.args=\"path/to/program.mini\"");
            return;
        }

        Path sourcePath = Path.of(args[0]);

        try {
            String source = Files.readString(sourcePath, StandardCharsets.UTF_8);

            // M's Note: Turn the source code into tokens first.
            List<Token> tokens = new Lexer(source).tokenize();

            // M's Note: Display a bullet-point lexical analysis summary.
            System.out.println("\n========== LEXER: ANALYSIS REPORT ==========");
            System.out.println("Summary:");
            System.out.println("1.) Total tokens: " + tokens.size());
            System.out.println("2.) Lexical errors: 0");
            System.out.println("3.) Comments: Ignored by the lexer");

            // M's Note: Display the tokens produced by the lexer.
            System.out.println("\n========== TOKEN DETAILS ==========");
            for (Token token : tokens) {
                System.out.println("- " + token);
            }

            // M's Note: Parse only after the lexer succeeds.
            Parser parser = new Parser(tokens);
            List<Stmt> program = parser.parse();

            if (parser.hasErrors()) {
                System.out.println("\n========== PARSER: SYNTAX ERRORS ==========");
                for (Parser.ParseException error : parser.getErrors()) {
                    System.out.println(error.getMessage());
                }
                System.out.println("Semantic analysis was skipped.");
                System.exit(1);
                return;
            }

            // M's Note: Display the AST created by the parser.
            System.out.println("\n========== PARSER: ABSTRACT SYNTAX TREE ==========");
            AstPrinter astPrinter = new AstPrinter();
            astPrinter.print(program);

            // M's Note: Check types and variable usage.
            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            analyzer.analyze(program);

            // M's Note: Display the semantic analysis report.
            System.out.println("\n========== SEMANTIC ANALYSIS ==========");
            System.out.println(analyzer.report());

            if (analyzer.hasErrors()) {
                // M's Note: Tell the operating system that semantic analysis failed.
                System.exit(1);
                return;
            }

            // M's Note: Display the final message after all analysis stages.
            System.out.println("\nMiniScript analysis completed.\n");

        } catch (Lexer.LexerException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.out.println("Could not read source file: " + sourcePath);
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }
}