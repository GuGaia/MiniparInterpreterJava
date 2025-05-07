package minipar.semantic;

import minipar.parser.ASTNode;
import java.util.List;

public class ExpressionValidator {
    private final SymbolTable symbolTable;

    public ExpressionValidator(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public void validateExpression(ASTNode expr) {
        switch (expr.getType()) {
            case "Valor" -> validateLiteralOrVariable(expr.getValue());
            case "BinOp" -> expr.getChildren().forEach(this::validateExpression);
            case "ChamadaFuncao", "Lista" -> validateExpressionList(expr.getChildren());
            case "Indexacao" -> {
                if (!symbolTable.isDeclared(expr.getValue())) {
                    throw new RuntimeException("Lista não declarada: " + expr.getValue());
                }
                validateExpression(expr.getChildren().get(0));
            }
            default -> throw new RuntimeException("Expressão inválida: " + expr.getType());
        }
    }

    public void validateExpressionList(List<ASTNode> exprs) {
        exprs.forEach(this::validateExpression);
    }

    private void validateLiteralOrVariable(String val) {
        if (isLiteral(val)) return;
        if (!symbolTable.isDeclared(val)) {
            throw new RuntimeException("Variável não declarada: " + val);
        }
    }

    private boolean isLiteral(String val) {
        return val.matches("\\d+") || (val.startsWith("\"") && val.endsWith("\""));
    }
}
