package minipar.interpreter;

import minipar.exceptions.ReturnException;
import minipar.lexer.*;
import minipar.parser.*;
import minipar.semantic.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Interpreter {

    private final SymbolTable symbolTable = new SymbolTable();
    private final Map<String, Object> memory = new HashMap<>();
    private final Map<String, Canal> canais = new HashMap<>();
    private final Map<String, ASTNode> functions = new HashMap<>();

    // Módulos especializados
    private final ExpressionEvaluator evaluator = new ExpressionEvaluator(memory, null);
    private final AssignmentExecutor assignmentExecutor = new AssignmentExecutor(memory, symbolTable, evaluator);
    private final ChannelExecutor channelExecutor = new ChannelExecutor(canais, memory, symbolTable, evaluator);
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
        System.out.println("[DEBUG] Executando bloco tipo: " + block.getType() + " na thread " + Thread.currentThread().getName());
        switch (block.getType()) {
            case "SEQ", "Bloco" -> executeSequential(block);
            case "PAR" -> executeParallel(block);
            default -> throw new RuntimeException("Tipo de bloco desconhecido: " + block.getType());
        }
    }

    private void executeSequential(ASTNode block) {
        System.out.println("[THREAD] Iniciando bloco em thread: " + Thread.currentThread().getName());
        for (ASTNode stmt : block.getChildren()) {
            executeStatement(stmt);
        }
    }

    private void executeParallel(ASTNode block) {
        List<Thread> threads = new ArrayList<>();
        for (ASTNode child : block.getChildren()) {
            Thread t = new Thread(() -> executeBlock(child));
            t.start();
            threads.add(t);
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
            case "import"           -> executeImport(stmt);
            default                 -> throw new RuntimeException("Instrução não suportada: " + stmt.getType());
        }
    }

    private void executePrint(ASTNode stmt) {
        ASTNode valorNode = stmt.getChildren().get(0);
        String tipo = valorNode.getType();
        String raw = valorNode.getValue();

        // Se for string literal explícita
        if (tipo.equals("Valor") && raw.startsWith("\"") && raw.endsWith("\"")) {
            System.out.println(raw.substring(1, raw.length() - 1)); // remove aspas
            return;
        }

        try {
            int valor = evaluator.evaluate(valorNode);
            System.out.println(valor);
        } catch (RuntimeException e) {
            // Fallback: imprime o valor literal bruto
            System.out.println(raw);
        }
    }
    private void executeImport(ASTNode stmt) {
        String path = stmt.getValue(); // já sem aspas, vindo do parser
        try {
            String importedSource = Files.readString(Path.of(path));

            Lexer lexer = new Lexer(importedSource);
            List<minipar.lexer.Token> importedTokens = lexer.tokenize();

            Parser parser = new Parser(importedTokens);
            ASTNode importedAst = parser.parseProgram();

            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
            semanticAnalyzer.analyze(importedAst);

            this.execute(importedAst);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao importar arquivo '" + path + "': " + e.getMessage(), e);
        }
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public Map<String, Object> getMemory() {
        return memory;
    }

}
