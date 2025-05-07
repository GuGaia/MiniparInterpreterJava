package minipar.semantic;

import minipar.parser.ASTNode;

public class ControlFlowValidator {
    private final ExpressionValidator expressionValidator;
    private final SemanticAnalyzer analyzer;

    public ControlFlowValidator(ExpressionValidator expressionValidator, SemanticAnalyzer analyzer) {
        this.expressionValidator = expressionValidator;
        this.analyzer = analyzer;
    }

    public void analyzeConditional(ASTNode stmt) {
        ASTNode condition = stmt.getChildren().get(0);
        ASTNode body = stmt.getChildren().get(1);
        expressionValidator.validateExpression(condition);
        analyzer.analyzeBlock(body);
    }
}
