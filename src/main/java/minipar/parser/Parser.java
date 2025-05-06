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
            consume();
            ASTNode seqNode = new ASTNode("SEQ", "");
            while (!isAtEnd() && !peekIs("PAR") && !peekIs("EOF")) {
                ASTNode stmt = parseStatement();
                seqNode.addChild(stmt);
            }
            return seqNode;
        } else if (token.getType() == TokenType.KEYWORD && token.getValue().equals("PAR")) {
            consume();
            ASTNode parNode = new ASTNode("PAR", "");
            while (!isAtEnd() && !peekIs("SEQ") && !peekIs("EOF")) {
                try {
                    parNode.addChild(parseStatement());
                } catch (RuntimeException e) {
                    throw new RuntimeException("Erro dentro do bloco PAR: " + e.getMessage());
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
        } else if (token.getType() == TokenType.KEYWORD && token.getValue().equals("c_channel")) {
            return parseChannelDeclaration();
        } else if (token.getType() == TokenType.COMMENT) {
            return new ASTNode("Comentario", consume().getValue());
        }
        throw error("Instrucao invalida: " + token.getValue());
    }

    private ASTNode parseAssignment() {
        Token id = expect(TokenType.IDENTIFIER);
        expect(TokenType.OPERATOR, "=");
        Token expr = expect(TokenType.IDENTIFIER, TokenType.NUMBER);
        ASTNode node = new ASTNode("Atribuicao", "=");
        node.addChild(new ASTNode("ID", id.getValue()));
        node.addChild(new ASTNode("Expr", expr.getValue()));
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


    // === Utilitários ===

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
}
