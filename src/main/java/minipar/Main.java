package minipar;

import minipar.lexer.*;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        String codigo = Files.readString(Paths.get("programs/test1.mpr"));

        Lexer lexer = new Lexer(codigo);
        for (Token token : lexer.tokenize()) {
            System.out.println(token);
        }
    }
}
