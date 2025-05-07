package minipar.interpreter;

import minipar.parser.ASTNode;

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
            for (ASTNode child : block.getChildren()) {
                interpreter.executeStatement(child);
            }
        }
    }

    public void executeWhile(ASTNode stmt) {
        ASTNode condition = stmt.getChildren().get(0);
        ASTNode block = stmt.getChildren().get(1);

        while (evaluateCondition(condition)) {
            for (ASTNode child : block.getChildren()) {
                interpreter.executeStatement(child);
            }
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
}
