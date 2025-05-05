package minipar.interpreter;

import minipar.parser.ASTNode;
import minipar.semantic.SymbolTable;

import java.util.HashMap;
import java.util.Map;

public class Interpreter {

    private final SymbolTable symbolTable = new SymbolTable();
    private final Map<String, Integer> memory = new HashMap<>();

    public void execute(ASTNode root) {
        if (!root.getType().equals("Programa")) {
            throw new RuntimeException("Raiz invalida. Esperado 'Programa'");
        }
        for (ASTNode bloco : root.getChildren()) {
            executeBlock(bloco);
        }
    }

    private void executeBlock(ASTNode block) {
        switch (block.getType()) {
            case "SEQ" -> executeSequential(block);
            case "PAR" -> executeParallel(block);
            default -> throw new RuntimeException("Tipo de bloco desconhecido: " + block.getType());
        }
    }

    private void executeSequential(ASTNode block) {
        for (ASTNode stmt : block.getChildren()) {
            executeStatement(stmt);
        }
    }

    private void executeParallel(ASTNode block) {
        Thread[] threads = new Thread[block.getChildren().size()];
        for (int i = 0; i < block.getChildren().size(); i++) {
            ASTNode stmt = block.getChildren().get(i);
            threads[i] = new Thread(() -> executeStatement(stmt));
            threads[i].start();
        }
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException("Erro em thread paralela", e);
            }
        }
    }

    private void executeStatement(ASTNode stmt) {
        switch (stmt.getType()) {
            case "Atribuicao" -> executeAssignment(stmt);
            case "Comentario" -> {} // Ignora
            case "c_channel" -> executeChannelDeclaration(stmt);
            default -> throw new RuntimeException("Instrucao nao suportada: " + stmt.getType());
        }
    }

    private void executeAssignment(ASTNode stmt) {
        String var = stmt.getChildren().get(0).getValue();
        String expr = stmt.getChildren().get(1).getValue();
        int value;

        if (expr.matches("\\d+")) {
            value = Integer.parseInt(expr);
        } else if (memory.containsKey(expr)) {
            value = memory.get(expr);
        } else {
            throw new RuntimeException("Variavel nao declarada ou valor invalido: " + expr);
        }

        memory.put(var, value);
        symbolTable.declare(var, "int");
        System.out.println(var + " = " + value);
    }

    private void executeChannelDeclaration(ASTNode stmt) {
        String canal = stmt.getValue();
        String comp1 = stmt.getChildren().get(0).getValue();
        String comp2 = stmt.getChildren().get(1).getValue();

        symbolTable.declare(canal, "canal");
        symbolTable.declare(comp1, "computador");
        symbolTable.declare(comp2, "computador");

        System.out.println("Canal criado: " + canal + " entre " + comp1 + " e " + comp2);
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public Map<String, Integer> getMemory() {
        return memory;
    }
}
