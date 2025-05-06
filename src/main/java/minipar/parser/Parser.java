package minipar.parser;

import minipar.lexer.*;

import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int position = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public ASTNode parseProgram() {
        expect(TokenType.KEYWORD, "programa_minipar");
        ASTNode root = new ASTNode("Programa", "programa_minipar");
        while (!isAtEnd()) {
            Token token = current();
            if (token.getType() == TokenType.KEYWORD &&
                    (token.getValue().equals("SEQ") || token.getValue().equals("PAR"))) {
                root.addChild(parseBlock());
            } else {
                throw error("Esperado bloco SEQ ou PAR após programa-minipar");
            }
        }
        return root;
    }

    private ASTNode parseBlock() {
        Token token = current();

        if (token.getType() == TokenType.KEYWORD && token.getValue().equals("SEQ")) {
            consume(); // consome SEQ
            ASTNode seqNode = new ASTNode("SEQ", "");
            while (!isAtEnd()) {
                if (current().getType() == TokenType.KEYWORD &&
                        (current().getValue().equals("SEQ") || current().getValue().equals("PAR"))) {
                    break; // para o bloco atual
                }
                seqNode.addChild(parseStatement());
            }
            return seqNode;

        } else if (token.getType() == TokenType.KEYWORD && token.getValue().equals("PAR")) {
            consume(); // consome PAR
            ASTNode parNode = new ASTNode("PAR", "");
            while (!isAtEnd()) {
                if (current().getType() == TokenType.KEYWORD &&
                        (current().getValue().equals("SEQ") || current().getValue().equals("PAR"))) {
                    parNode.addChild(parseBlock()); // permite blocos aninhados dentro do PAR
                } else {
                    parNode.addChild(parseStatement());
                }
            }
            return parNode;

        } else {
            throw error("Esperado SEQ ou PAR");
        }
    }


    private ASTNode parseStatement() {
        Token token = current();
        if (token.getType() == TokenType.IDENTIFIER) {
            Token next = peek();
            if (next != null && next.getValue().equals("=")) {
                return parseAssignment();
            } else if (next != null && next.getValue().equals(".")) {
                return parseChannelOperation();
            } else {
                throw error("Atribuiçao invalida ou comando esperado ou desconhecido após '" + token.getValue() + "'");
            }
        } else if (token.getType() == TokenType.KEYWORD) {
            if (token.getValue().equals("c_channel")) return parseChannelDeclaration();
            else if (token.getValue().equals("print")) return parsePrint();
            else if (token.getValue().equals("if")) return parseIf();
            else if (token.getValue().equals("while")) return parseWhile();

        } else if (token.getType() == TokenType.COMMENT) {
            return new ASTNode("Comentario", consume().getValue());
        }
        throw error("Instrucao invalida: " + token.getValue());
    }

    private ASTNode parseAssignment() {
        Token var = expect(TokenType.IDENTIFIER);
        expect(TokenType.OPERATOR, "=");
        ASTNode expr = parseExpression();
        ASTNode node = new ASTNode("Atribuicao", "");
        node.addChild(new ASTNode("Variavel", var.getValue()));
        node.addChild(expr);
        return node;
    }

    private ASTNode parseChannelDeclaration() {
        expect(TokenType.KEYWORD, "c_channel");
        Token canal = expect(TokenType.IDENTIFIER);
        Token comp1 = expect(TokenType.IDENTIFIER);
        Token comp2 = expect(TokenType.IDENTIFIER);
        ASTNode node = new ASTNode("c_channel", canal.getValue());
        node.addChild(new ASTNode("Comp1", comp1.getValue()));
        node.addChild(new ASTNode("Comp2", comp2.getValue()));
        return node;
    }
    private ASTNode parseChannelOperation() {
        Token canal = expect(TokenType.IDENTIFIER); // ex: "canal1"
        expect(TokenType.OPERATOR, ".");           // ex: "."
        Token operacao = expect(TokenType.IDENTIFIER); // ex: "send" ou "receive"
        expect(TokenType.DELIMITER, "(");          // "("
        Token argumento = expect(TokenType.IDENTIFIER, TokenType.NUMBER);
        expect(TokenType.DELIMITER, ")");          // ")"

        ASTNode node = new ASTNode(operacao.getValue(), canal.getValue());

        if (operacao.getValue().equals("send")) {
            node.addChild(new ASTNode("Valor", argumento.getValue()));
        } else if (operacao.getValue().equals("receive")) {
            node.addChild(new ASTNode("Variavel", argumento.getValue()));
        } else {
            throw error("Operação desconhecida: " + operacao.getValue());
        }

        return node;
    }
    private ASTNode parsePrint() {
        expect(TokenType.KEYWORD, "print");
        expect(TokenType.DELIMITER, "(");
        Token argumento = expect(TokenType.IDENTIFIER, TokenType.NUMBER, TokenType.STRING);
        expect(TokenType.DELIMITER, ")");

        ASTNode node = new ASTNode("print", "");
        node.addChild(new ASTNode("Valor", argumento.getValue()));
        return node;
    }
    private ASTNode parseExpression() {
        ASTNode left = parseTerm();
        while (!isAtEnd() && isComparisonOperator(current().getValue())) {
            String op = consume().getValue(); // operador relacional
            ASTNode right = parseTerm();
            ASTNode node = new ASTNode("BinOp", op);
            node.addChild(left);
            node.addChild(right);
            left = node;
        }
        return left;
    }

    private ASTNode parseTerm() {
        ASTNode node = parseFactor();

        while (!isAtEnd() && (match("+") || match("-"))) {
            String op = previous().getValue();
            ASTNode operador = new ASTNode("BinOp", op);
            operador.addChild(node); // filho esquerdo
            operador.addChild(parseFactor()); // filho direito
            node = operador;
        }
        return node;
    }

    private ASTNode parseFactor() {
        ASTNode node = parsePrimary();

        while (!isAtEnd() && (match("*") || match("/"))) {
            String op = previous().getValue();
            ASTNode operador = new ASTNode("BinOp", op);
            operador.addChild(node); // filho esquerdo
            operador.addChild(parsePrimary()); // filho direito
            node = operador;
        }

        return node;
    }

    private ASTNode parsePrimary() {
        Token token = current();

        if (match("(")) {
            ASTNode expr = parseExpression();
            expect(TokenType.DELIMITER, ")");
            return expr;
        }

        if (token.getType() == TokenType.NUMBER || token.getType() == TokenType.IDENTIFIER) {
            consume();
            return new ASTNode("Valor", token.getValue());
        }

        throw error("Expressão inválida");
    }
    private ASTNode parseIf() {
        return parseConditional("if");
    }

    private ASTNode parseWhile() {
        return parseConditional("while");
    }

    // === Utilitários ===
    private ASTNode parseConditional(String type) {
        expect(TokenType.KEYWORD, type);
        ASTNode condition = parseExpression(); // funciona com operadores relacionais
        expect(TokenType.DELIMITER, "{");

        ASTNode block = new ASTNode("Bloco", "");
        while (!peekIs("}")) {
            block.addChild(parseStatement());
        }
        expect(TokenType.DELIMITER, "}");

        ASTNode node = new ASTNode(type, "");
        node.addChild(condition);
        node.addChild(block);
        return node;
    }

    private boolean match(String value) {
        if (!isAtEnd() && current().getValue().equals(value)) {
            consume();
            return true;
        }
        return false;
    }

    private Token previous() {
        return tokens.get(position - 1);
    }

    private Token current() {
        return tokens.get(position);
    }

    private Token peek() {
        return position + 1 < tokens.size() ? tokens.get(position + 1) : null;
    }

    private boolean peekIs(String value) {
        return current().getValue().equals(value);
    }

    private Token consume() {
        return tokens.get(position++);
    }

    private Token expect(TokenType... types) {
        Token token = current();
        for (TokenType type : types) {
            if (token.getType() == type) {
                return consume();
            }
        }
        throw error("Esperado tipo: " + List.of(types) + ", encontrado: " + token.getType());
    }

    private Token expect(TokenType type, String value) {
        Token token = current();
        if (token.getType() == type && token.getValue().equals(value)) {
            return consume();
        }
        throw error("Esperado: " + value + ", encontrado: " + token.getValue());
    }

    private boolean isAtEnd() {
        return current().getType() == TokenType.EOF;
    }

    private RuntimeException error(String message) {
        return new RuntimeException("Erro sintatico na linha " + current().getLine() + ": " + message);
    }
    private boolean isComparisonOperator(String op) {
        return switch (op) {
            case "==", "!=", "<", ">", "<=", ">=" -> true;
            default -> false;
        };
    }
}
