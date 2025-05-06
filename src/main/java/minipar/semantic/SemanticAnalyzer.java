package minipar.semantic;

import minipar.parser.ASTNode;

import java.util.List;

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
            case "send" -> analyzeSend(stmt);
            case "receive" -> analyzeReceive(stmt);
            case "print" -> analyzePrint(stmt);
            case "if", "while" -> analyzeConditional(stmt);
            case "def" -> { /* ignorar por enquanto ou validar nomes únicos */ }
            case "return" -> validateExpression(stmt.getChildren().get(0));
            case "ChamadaFuncao" -> validateExpressionList(stmt.getChildren());
            case "Comentario" -> { /* ignora comentários */ }

            default -> throw new RuntimeException("Tipo de instrucao desconhecido: " + stmt.getType());
        }
    }

    private void analyzeAssignment(ASTNode stmt) {
        String varName = stmt.getChildren().get(0).getValue();
        if (!symbolTable.isDeclared(varName)) {
            symbolTable.declare(varName, "int"); // assumindo tipo int por padrão
        }

        ASTNode expr = stmt.getChildren().get(1);
        validateExpression(expr);
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
    private void analyzeSend(ASTNode stmt) {
        String canal = stmt.getValue();
        if (!symbolTable.isDeclared(canal)) {
            throw new RuntimeException("Canal não declarado: " + canal);
        }
        String valor = stmt.getChildren().get(0).getValue();
        if (!valor.matches("\\d+") && !symbolTable.isDeclared(valor)) {
            throw new RuntimeException("Valor a ser enviado não declarado: " + valor);
        }
    }

    private void analyzeReceive(ASTNode stmt) {
        String canal = stmt.getValue();
        String var = stmt.getChildren().get(0).getValue();
        if (!symbolTable.isDeclared(canal)) {
            throw new RuntimeException("Canal não declarado: " + canal);
        }
        if (!symbolTable.isDeclared(var)) {
            symbolTable.declare(var, "int"); // declaração implícita na recepção
        }
    }

    private void analyzePrint(ASTNode stmt) {
        String arg = stmt.getChildren().get(0).getValue();
        if (!arg.matches("\\d+") && !arg.startsWith("\"") && !symbolTable.isDeclared(arg)) {
            throw new RuntimeException("Variavel não declarada: " + arg);
        }
    }

    private void analyzeConditional(ASTNode stmt) {
        validateExpression(stmt.getChildren().get(0));  // condição
        analyzeBlock(stmt.getChildren().get(1));         // corpo
    }

    private void validateExpression(ASTNode expr) {
        switch (expr.getType()) {
            case "Valor" -> {
                String val = expr.getValue();
                if (val.matches("\\d+")) return; // número literal
                if (val.startsWith("\"") && val.endsWith("\"")) return; // string literal
                if (!symbolTable.isDeclared(val)) throw new RuntimeException("Variável não declarada: " + val);
            }
            case "BinOp" -> {
                validateExpression(expr.getChildren().get(0)); // lado esquerdo
                validateExpression(expr.getChildren().get(1)); // lado direito
            }
            case "ChamadaFuncao" -> {
                for (ASTNode arg : expr.getChildren()) {
                    validateExpression(arg);
                }
            }
            default -> throw new RuntimeException("Expressão inválida: " + expr.getType());
        }
    }
    private void validateExpressionList(List<ASTNode> exprs) {
        for (ASTNode expr : exprs) {
            validateExpression(expr);
        }
    }

    private boolean isLiteralOrDeclared(String value) {
        return value.matches("\\d+") || symbolTable.isDeclared(value);
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }
}
