package minipar.interpreter;

import minipar.exceptions.ReturnException;
import minipar.parser.ASTNode;
import minipar.semantic.SymbolTable;

import java.util.*;

public class Interpreter {

    private final SymbolTable symbolTable = new SymbolTable();
    private final Map<String, Object> memory = new HashMap<>();
    private final Map<String, Canal> canais = new HashMap<>();
    private final Map<String, ASTNode> functions = new HashMap<>();

    // Módulos especializados
    private final ExpressionEvaluator evaluator = new ExpressionEvaluator(memory, null);
    private final AssignmentExecutor assignmentExecutor = new AssignmentExecutor(memory, symbolTable, evaluator);
    private final ChannelExecutor channelExecutor = new ChannelExecutor(canais, memory, symbolTable);
    private final ControlFlowExecutor controlFlowExecutor = new ControlFlowExecutor(this, evaluator);
    private final FunctionExecutor functionExecutor = new FunctionExecutor(functions, memory, symbolTable, this, evaluator);

    public Interpreter() {
        setupFunctionEvaluation();
    }

    public void setupFunctionEvaluation() {
        evaluator.setFunctionExecutor(functionExecutor);
    }

    public void execute(ASTNode root) {
        if (!root.getType().equals("Programa")) {
            throw new RuntimeException("Raiz inválida. Esperado 'Programa'");
        }
        for (ASTNode bloco : root.getChildren()) {
            executeBlock(bloco);
        }
    }

    public void executeBlock(ASTNode block) {
        switch (block.getType()) {
            case "SEQ", "Bloco" -> executeSequential(block);
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
        List<Thread> threads = new ArrayList<>();
        for (ASTNode stmt : block.getChildren()) {
            Thread thread = new Thread(() -> executeStatement(stmt));
            thread.start();
            threads.add(thread);
        }
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException("Erro em thread paralela", e);
            }
        }
    }

    public void executeStatement(ASTNode stmt) {
        switch (stmt.getType()) {
            case "Atribuicao"       -> assignmentExecutor.executeAssignment(stmt);
            case "AtribuicaoIndice" -> assignmentExecutor.executeIndexAssignment(stmt);
            case "Comentario"       -> {} // Ignora
            case "c_channel"        -> channelExecutor.declareChannel(stmt);
            case "send"             -> channelExecutor.send(stmt);
            case "receive"          -> channelExecutor.receive(stmt);
            case "print"            -> executePrint(stmt);
            case "if"               -> controlFlowExecutor.executeIf(stmt);
            case "while"            -> controlFlowExecutor.executeWhile(stmt);
            case "for"              -> controlFlowExecutor.executeFor(stmt);
            case "def"              -> functionExecutor.register(stmt);
            case "return"           -> throw new ReturnException(evaluator.evaluate(stmt.getChildren().get(0)));
            case "ChamadaFuncao"    -> functionExecutor.call(stmt);
            default                 -> throw new RuntimeException("Instrução não suportada: " + stmt.getType());
        }
    }

    private void executePrint(ASTNode stmt) {
        String valor = stmt.getChildren().get(0).getValue();
        if (memory.containsKey(valor)) {
            System.out.println(memory.get(valor));
        } else {
            System.out.println(valor.replaceAll("^\"|\"$", "")); // remove aspas se for string
        }
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public Map<String, Object> getMemory() {
        return memory;
    }
}
