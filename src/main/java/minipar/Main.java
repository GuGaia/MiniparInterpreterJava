package minipar;

import minipar.lexer.*;
import minipar.parser.*;
import minipar.semantic.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        String code = Files.readString(Paths.get("programs/test1.mpr"));
        Lexer lexer = new Lexer(code);
        List<Token> tokens = lexer.tokenize();

        Parser parser = new Parser(tokens);
        ASTNode ast = parser.parseProgram();

        System.out.println("== AST ==");
        ast.print("");

        SemanticAnalyzer sem = new SemanticAnalyzer();
        sem.analyze(ast);

        sem.getSymbolTable().print();
    }
}