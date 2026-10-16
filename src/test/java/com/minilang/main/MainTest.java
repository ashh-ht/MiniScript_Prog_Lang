package com.minilang.main;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import com.minilang.ast.Stmt;
import com.minilang.lexer.Lexer;
import com.minilang.lexer.Token;
import com.minilang.parser.Parser;
import com.minilang.semantic.SemanticAnalyzer;
import org.junit.jupiter.api.Test;

public class MainTest {

    private SemanticAnalyzer analyzeSource(String source) {
        List<Token> tokens = new Lexer(source).tokenize();
        List<Stmt> program = new Parser(tokens).parse();

        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        analyzer.analyze(program);
        return analyzer;
    }

    @Test
    public void validProgramShouldHaveNoErrors() {
        SemanticAnalyzer analyzer = analyzeSource(
                "int count = 10;\n" +
                "double price = 2.5;\n" +
                "bool valid = count > price;\n"
        );

        assertFalse(analyzer.hasErrors(), analyzer.report());
    }

    @Test
    public void invalidInitializerShouldNotInitializeVariable() {
        SemanticAnalyzer analyzer = analyzeSource(
                "int date = 16.14;\n" +
                "date + 1;\n"
        );

        assertTrue(analyzer.hasErrors());
        assertTrue(analyzer.report().contains("Can't assign a double"));
        assertTrue(analyzer.report().contains("used before it is assigned"));
    }

    @Test
    public void invalidAssignmentShouldNotInitializeVariable() {
        SemanticAnalyzer analyzer = analyzeSource(
                "int date;\n" +
                "date = 16.14;\n" +
                "date + 1;\n"
        );

        assertTrue(analyzer.hasErrors());
        assertTrue(analyzer.report().contains("trying to put a double"));
        assertTrue(analyzer.report().contains("used before it is assigned"));
    }

    @Test
    public void intValueShouldBeAssignableToDouble() {
        SemanticAnalyzer analyzer = analyzeSource(
                "double price = 5;\n"
        );

        assertFalse(analyzer.hasErrors(), analyzer.report());
    }

    @Test
    public void doubleValueShouldNotBeAssignableToInt() {
        SemanticAnalyzer analyzer = analyzeSource(
                "int count = 5.5;\n"
        );

        assertTrue(analyzer.hasErrors());
        assertTrue(analyzer.report().contains("Can't assign a double"));
    }

    @Test
    public void numericArithmeticAndComparisonShouldBeValid() {
        SemanticAnalyzer analyzer = analyzeSource(
                "int count = 5 + 2;\n" +
                "double total = 5 + 2.5;\n" +
                "bool result = 5 < 2.5;\n"
        );

        assertFalse(analyzer.hasErrors(), analyzer.report());
    }

    @Test
    public void invalidArithmeticOperandsShouldProduceError() {
        SemanticAnalyzer analyzer = analyzeSource(
                "int result = true + 5;\n"
        );

        assertTrue(analyzer.hasErrors());
    }

    @Test
    public void logicalOperatorsShouldRequireBooleans() {
        SemanticAnalyzer analyzer = analyzeSource(
                "bool valid = true && false;\n" +
                "bool invalid = true && 5;\n"
        );

        assertTrue(analyzer.hasErrors());
    }

    @Test
    public void undeclaredVariableShouldProduceError() {
        SemanticAnalyzer analyzer = analyzeSource(
                "count = 10;\n"
        );

        assertTrue(analyzer.hasErrors());
        assertTrue(analyzer.report().contains("Undeclared variable 'count'"));
    }

    @Test
    public void duplicateDeclarationInSameScopeShouldProduceError() {
        SemanticAnalyzer analyzer = analyzeSource(
                "int count = 10;\n" +
                "int count = 20;\n"
        );

        assertTrue(analyzer.hasErrors());
        assertTrue(analyzer.report().contains("already declared"));
    }

    @Test
    public void variableDeclaredInsideBlockShouldNotBeAccessibleOutside() {
        SemanticAnalyzer analyzer = analyzeSource(
                "{\n" +
                "    int count = 10;\n" +
                "}\n" +
                "count = 20;\n"
        );

        assertTrue(analyzer.hasErrors());
        assertTrue(analyzer.report().contains("Undeclared variable 'count'"));
    }

    @Test
    public void parserShouldReportMissingExpression() {
        List<Token> tokens = new Lexer(
                "int first = ;\n" +
                "int second = 10;\n"
        ).tokenize();

        Parser parser = new Parser(tokens);
        parser.parse();

        assertTrue(parser.hasErrors());
        assertTrue(
                parser.getErrors().stream()
                        .anyMatch(error -> error.getMessage().contains("Expected an expression"))
        );
    }

    @Test
    public void parserShouldContinueAfterSyntaxError() {
        List<Token> tokens = new Lexer(
                "int first = ;\n" +
                "int second = 10;\n"
        ).tokenize();

        Parser parser = new Parser(tokens);
        List<Stmt> program = parser.parse();

        assertTrue(parser.hasErrors());
        assertTrue(
                program.stream()
                        .anyMatch(stmt -> stmt instanceof com.minilang.ast.Vardecl
                                && ((com.minilang.ast.Vardecl) stmt).name.getLexeme().equals("second"))
        );
    }

    @Test
    public void lexerShouldRejectUnexpectedCharacter() {
        boolean threwError = false;

        try {
            new Lexer("int count = 10; @").tokenize();
        } catch (Lexer.LexerException e) {
            threwError = e.getMessage().contains("Unexpected character '@'");
        }

        assertTrue(threwError, "The lexer should reject unsupported characters.");
    }
}
