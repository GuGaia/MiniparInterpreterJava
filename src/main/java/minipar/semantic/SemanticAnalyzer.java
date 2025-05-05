package minipar.semantic;

import minipar.parser.ASTNode;

public class SemanticAnalyzer {

    private final SymbolTable symbolTable = new SymbolTable();

    public void analyze(ASTNode root) {
        if (!root.getType().equals("Programa")) {
            throw new RuntimeException("AST invalida: no raiz não e 'Programa'");
        }

        for (ASTNode bloco : root.getChildren()) {
            analyzeBlock(bloco);
        }
    }

    private void analyzeBlock(ASTNode bloco) {
        for (ASTNode stmt : bloco.getChildren()) {
            analyzeStatement(stmt);
        }
    }

    private void analyzeStatement(ASTNode stmt) {
        switch (stmt.getType()) {
            case "Atribuicao" -> analyzeAssignment(stmt);
            case "c_channel" -> analyzeChannel(stmt);
            case "Comentario" -> {
                // ignora comentários
            }
            default -> throw new RuntimeException("Tipo de instrucao desconhecido: " + stmt.getType());
        }
    }

    private void analyzeAssignment(ASTNode stmt) {
        String varName = stmt.getChildren().get(0).getValue();
        if (!symbolTable.isDeclared(varName)) {
            symbolTable.declare(varName, "int"); // assumindo tipo int por padrão
        }

        ASTNode expr = stmt.getChildren().get(1);
        if (!isLiteralOrDeclared(expr.getValue())) {
            throw new RuntimeException("Expressao invalida ou variavel não declarada: " + expr.getValue());
        }
    }

    private void analyzeChannel(ASTNode stmt) {
        String channelName = stmt.getValue();
        if (symbolTable.isDeclared(channelName)) {
            throw new RuntimeException("Canal '" + channelName + "' ja declarado.");
        }
        symbolTable.declare(channelName, "canal");

        String comp1 = stmt.getChildren().get(0).getValue();
        String comp2 = stmt.getChildren().get(1).getValue();

        if (!symbolTable.isDeclared(comp1)) symbolTable.declare(comp1, "computador");
        if (!symbolTable.isDeclared(comp2)) symbolTable.declare(comp2, "computador");
    }

    private boolean isLiteralOrDeclared(String value) {
        return value.matches("\\d+") || symbolTable.isDeclared(value);
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }
}
