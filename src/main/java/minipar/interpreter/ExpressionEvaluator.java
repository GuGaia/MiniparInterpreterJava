package minipar.interpreter;

import minipar.parser.ASTNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExpressionEvaluator {

    private final Map<String, Object> memory;
    private List<Integer> lastEvaluatedList;
    private FunctionExecutor functionExecutor;

    public ExpressionEvaluator(Map<String, Object> memory, FunctionExecutor functionExecutor) {
        this.memory = memory;
        this.functionExecutor = functionExecutor;
    }

    public int evaluate(ASTNode node) {
        return switch (node.getType()) {
            case "Valor" -> evaluateLiteral(node.getValue());
            case "Lista" -> {
                lastEvaluatedList = new ArrayList<>();
                for (ASTNode item : node.getChildren()) {
                    lastEvaluatedList.add(evaluate(item));
                }
                yield 0;
            }
            case "BinOp" -> evaluateBinary(node);
            case "ChamadaFuncao" -> functionExecutor.call(node);
            default -> throw new RuntimeException("Expressão inválida: " + node.getType());
        };
    }

    private int evaluateLiteral(String val) {
        if (val.matches("\\d+")) {
            return Integer.parseInt(val);
        }
        if (val.startsWith("\"") && val.endsWith("\"")) {
            System.out.println(val.substring(1, val.length() - 1));
            return 0;
        }
        if (memory.containsKey(val)) {
            Object value = memory.get(val);
            if (value instanceof Integer i) return i;
            throw new RuntimeException("Valor da variável '" + val + "' não é inteiro");
        }
        throw new RuntimeException("Variável não declarada: " + val);
    }

    private int evaluateBinary(ASTNode node) {
        int left = evaluate(node.getChildren().get(0));
        int right = evaluate(node.getChildren().get(1));
        return switch (node.getValue()) {
            case "+" -> left + right;
            case "-" -> left - right;
            case "*" -> left * right;
            case "/" -> right == 0 ? 0 : left / right;
            case "==" -> left == right ? 1 : 0;
            case "!=" -> left != right ? 1 : 0;
            case ">"  -> left > right ? 1 : 0;
            case "<"  -> left < right ? 1 : 0;
            case ">=" -> left >= right ? 1 : 0;
            case "<=" -> left <= right ? 1 : 0;
            default -> throw new RuntimeException("Operador inválido: " + node.getValue());
        };
    }

    private int evaluateIndex(ASTNode node) {
        String nome = node.getChildren().get(0).getValue();
        int index = evaluate(node.getChildren().get(1));

        if (!memory.containsKey(nome)) throw new RuntimeException("Variável não declarada: " + nome);

        Object valor = memory.get(nome);
        if (!(valor instanceof List<?> lista)) {
            throw new RuntimeException("Variável '" + nome + "' não é uma lista");
        }

        if (index < 0 || index >= lista.size()) {
            throw new RuntimeException("Índice fora dos limites: " + index);
        }

        Object elemento = lista.get(index);
        if (!(elemento instanceof Integer)) {
            throw new RuntimeException("Elemento não é inteiro");
        }

        return (Integer) elemento;
    }

    public List<Integer> getLastEvaluatedList() {
        return lastEvaluatedList;
    }

    public void setFunctionExecutor(FunctionExecutor functionExecutor) {
        this.functionExecutor = functionExecutor;
    }
}
