package minipar.interpreter;

import minipar.parser.ASTNode;
import minipar.semantic.SymbolTable;

import java.util.Map;

public class ChannelExecutor {

    private final Map<String, Canal> canais;
    private final SymbolTable symbolTable;
    private final Map<String, Object> memory;
    private static int portaAtual = 5000;

    public ChannelExecutor(Map<String, Canal> canais, Map<String, Object> memory, SymbolTable symbolTable) {
        this.canais = canais;
        this.memory = memory;
        this.symbolTable = symbolTable;
    }

    public void declareChannel(ASTNode stmt) {
        String canal = stmt.getValue();
        String comp1 = stmt.getChildren().get(0).getValue();
        String comp2 = stmt.getChildren().get(1).getValue();

        symbolTable.declare(canal, "canal");
        symbolTable.declare(comp1, "computador");
        symbolTable.declare(comp2, "computador");

        Canal c = new Canal(canal, portaAtual++);
        canais.put(canal, c);

        System.out.println("Canal criado: " + canal + " entre " + comp1 + " e " + comp2 + " na porta " + c.getPorta());
    }

    public void send(ASTNode stmt) {
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

    public void receive(ASTNode stmt) {
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
}
