package minipar.interpreter;

import minipar.parser.ASTNode;
import minipar.semantic.SymbolTable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssignmentExecutor {

    private final Map<String, Object> memory;
    private final SymbolTable symbolTable;
    private final ExpressionEvaluator evaluator;

    public AssignmentExecutor(Map<String, Object> memory, SymbolTable symbolTable, ExpressionEvaluator evaluator) {
        this.memory = memory;
        this.symbolTable = symbolTable;
        this.evaluator = evaluator;
    }

    public void executeAssignment(ASTNode stmt) {
        String var = stmt.getChildren().get(0).getValue();
        ASTNode expr = stmt.getChildren().get(1);

        Object value;

        if (expr.getType().equals("Lista")) {
            List<Integer> lista = evaluator.getLastEvaluatedList(); // já foi avaliada como 0
            evaluator.evaluate(expr); // reavalia para preencher `lastEvaluatedList`
            value = evaluator.getLastEvaluatedList();
        } else {
            value = evaluator.evaluate(expr);
        }

        memory.put(var, value);

        if (!symbolTable.isDeclared(var)) {
            symbolTable.declare(var, "int"); // poderia usar "lista" se quiser ser mais preciso
        }
    }

    public void executeIndexAssignment(ASTNode stmt) {
        String nome = stmt.getChildren().get(0).getValue();
        int index = evaluator.evaluate(stmt.getChildren().get(1));
        int valor = evaluator.evaluate(stmt.getChildren().get(2));

        if (!memory.containsKey(nome)) throw new RuntimeException("Lista não declarada: " + nome);

        Object objeto = memory.get(nome);
        if (!(objeto instanceof List<?>)) {
            throw new RuntimeException("Variável '" + nome + "' não é uma lista");
        }
        @SuppressWarnings("unchecked")
        List<Integer> lista = (List<Integer>) objeto;

        if (index < 0 || index >= lista.size()) {
            throw new RuntimeException("Índice fora dos limites da lista");
        }

        lista.set(index, valor);
    }
}
