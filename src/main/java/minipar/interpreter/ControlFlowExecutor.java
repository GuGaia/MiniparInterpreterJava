package minipar.interpreter;

import minipar.parser.ASTNode;

import java.util.List;

public class ControlFlowExecutor {

    private final Interpreter interpreter;
    private final ExpressionEvaluator evaluator;

    public ControlFlowExecutor(Interpreter interpreter, ExpressionEvaluator evaluator) {
        this.interpreter = interpreter;
        this.evaluator = evaluator;
    }

    public void executeIf(ASTNode stmt) {
        ASTNode condition = stmt.getChildren().get(0);
        ASTNode block = stmt.getChildren().get(1);

        if (evaluateCondition(condition)) {
            executeBlock(block);
        } else if (stmt.getChildren().size() > 2) {
                ASTNode elseBlock = stmt.getChildren().get(2);
                executeBlock(elseBlock);
        }
    }

    public void executeWhile(ASTNode stmt) {
        ASTNode condition = stmt.getChildren().get(0);
        ASTNode block = stmt.getChildren().get(1);

        while (evaluateCondition(condition)) {
            executeBlock(block);
        }
    }

    private boolean evaluateCondition(ASTNode node) {
        if (!node.getType().equals("BinOp")) {
            throw new RuntimeException("Condição inválida");
        }

        int left = evaluator.evaluate(node.getChildren().get(0));
        int right = evaluator.evaluate(node.getChildren().get(1));

        return switch (node.getValue()) {
            case "==" -> left == right;
            case "!=" -> left != right;
            case "<"  -> left < right;
            case ">"  -> left > right;
            case "<=" -> left <= right;
            case ">=" -> left >= right;
            default -> throw new RuntimeException("Operador inválido em condição: " + node.getValue());
        };
    }
    private void executeBlock(ASTNode block) {
        for (ASTNode child : block.getChildren()) {
            interpreter.executeStatement(child);
        }
    }
    /// /////////////////////////////////////parei aqui/////////////////////////////////////////////////////////
    public void executeFor(ASTNode stmt) {
        String var = stmt.getValue();
        ASTNode iterableExpr = stmt.getChildren().get(0);
        ASTNode block = stmt.getChildren().get(1);

        Object iterable = evaluator.evaluateRaw(iterableExpr);

        if (!(iterable instanceof List<?> lista)) {
            throw new RuntimeException("A expressão em 'for' não é uma lista.");
        }

        for (Object elem : lista) {
            if (!(elem instanceof Integer i)) {
                throw new RuntimeException("Elemento na lista não é inteiro.");
            }

            memory.put(var, i);
            if (!symbolTable.isDeclared(var)) {
                symbolTable.declare(var, "int");
            }

            for (ASTNode child : block.getChildren()) {
                interpreter.executeStatement(child);
            }
        }
    }
}
