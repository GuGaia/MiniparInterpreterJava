package minipar.interpreter;

import minipar.parser.ASTNode;
import minipar.semantic.SymbolTable;

import java.util.HashMap;
import java.util.Map;
public class Interpreter {

    private final SymbolTable symbolTable = new SymbolTable();
    private final Map<String, Integer> memory = new HashMap<>();
    private final Map<String, Canal> canais = new HashMap<>();
    private static int portaAtual = 5000;

    public void execute(ASTNode root) {
        if (!root.getType().equals("Programa")) {
            throw new RuntimeException("Raiz invalida. Esperado 'Programa'");
        }
        for (ASTNode bloco : root.getChildren()) {
            executeBlock(bloco);
        }
    }

    private void executeSend(ASTNode stmt) {
        String canal = stmt.getValue();
        Canal c = canais.get(canal);
        if (c == null) throw new RuntimeException("Canal '" + canal + "' não existe");

        String mensagem = stmt.getChildren().get(0).getValue();
        if (memory.containsKey(mensagem)) {
            mensagem = String.valueOf(memory.get(mensagem));
        }
        System.out.println("[DEBUG] Enviando para canal " + canal + " valor: " + mensagem);
        c.send(mensagem);
    }

    private void executeReceive(ASTNode stmt) {
        String canal = stmt.getValue();
        String variavel = stmt.getChildren().get(0).getValue();
        Canal c = canais.get(canal);
        if (c == null) throw new RuntimeException("Canal '" + canal + "' não existe");

        System.out.println("[DEBUG] Recebendo de canal " + canal);

        String recebido = c.receive();

        int valor = Integer.parseInt(recebido);
        memory.put(variavel, valor);
        symbolTable.declare(variavel, "int");
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
            case "send" -> executeSend(stmt);
            case "receive" -> executeReceive(stmt);
            case "print" -> executePrint(stmt);
            default -> throw new RuntimeException("Instrucao nao suportada: " + stmt.getType());
        }
    }

    private void executeAssignment(ASTNode stmt) {
        String var = stmt.getChildren().get(0).getValue();
        ASTNode expr = stmt.getChildren().get(1);
        int value = avaliarExpressao(expr);

        memory.put(var, value);
        symbolTable.declare(var, "int");
        System.out.println(var + " = " + value);
    }

    private void executePrint(ASTNode stmt) {
        String valor = stmt.getChildren().get(0).getValue();
        if (memory.containsKey(valor)) {
            System.out.println(memory.get(valor));
        } else {
            System.out.println(valor.replaceAll("^\"|\"$", "")); // remove aspas se for string
        }
    }

    private void executeChannelDeclaration(ASTNode stmt) {
        String canal = stmt.getValue();
        String comp1 = stmt.getChildren().get(0).getValue();
        String comp2 = stmt.getChildren().get(1).getValue();

        symbolTable.declare(canal, "canal");
        symbolTable.declare(comp1, "computador");
        symbolTable.declare(comp2, "computador");

        Canal c = new Canal(canal, portaAtual++);
        canais.put(canal, c);

        System.out.println("Canal criado: " + canal + " entre " + comp1 + " e " + comp2 + " na porta " + c.getPorta());}

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public Map<String, Integer> getMemory() {
        return memory;
    }

    private int avaliarExpressao(ASTNode node) {
        return switch (node.getType()) {
            case "Valor" -> {
                String val = node.getValue();
                if (val.matches("\\d+")) yield Integer.parseInt(val);
                else if (memory.containsKey(val)) yield memory.get(val);
                else throw new RuntimeException("Variável não declarada: " + val);
            }
            case "BinOp" -> {
                int left = avaliarExpressao(node.getChildren().get(0));
                int right = avaliarExpressao(node.getChildren().get(1));
                yield switch (node.getValue()) {
                    case "+" -> left + right;
                    case "-" -> left - right;
                    case "*" -> left * right;
                    case "/" -> right == 0 ? 0 : left / right;
                    default -> throw new RuntimeException("Operador inválido: " + node.getValue());
                };
            }
            default -> throw new RuntimeException("Expressão inválida: " + node.getType());
        };
    }

}
