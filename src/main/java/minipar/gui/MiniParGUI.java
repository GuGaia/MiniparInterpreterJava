package minipar.gui;

import minipar.lexer.*;
import minipar.parser.*;
import minipar.semantic.SemanticAnalyzer;
import minipar.interpreter.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.util.List;

public class MiniParGUI extends JFrame {

    private JTextArea astArea;
    private JTextArea outputArea;
    private String codigoFonte = "";

    public MiniParGUI() {
        setTitle("MiniPar - Interpretador Visual");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JButton btnAbrir = new JButton("Abrir Código");
        JButton btnExecutar = new JButton("Executar");

        astArea = new JTextArea();
        astArea.setEditable(false);
        outputArea = new JTextArea();
        outputArea.setEditable(false);

        JScrollPane astScroll = new JScrollPane(astArea);
        JScrollPane outputScroll = new JScrollPane(outputArea);

        JPanel botoes = new JPanel();
        botoes.add(btnAbrir);
        botoes.add(btnExecutar);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, astScroll, outputScroll);
        split.setDividerLocation(300);

        add(botoes, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);

        btnAbrir.addActionListener(e -> carregarCodigo());
        btnExecutar.addActionListener(e -> executarCodigo());
    }

    private void carregarCodigo() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                codigoFonte = Files.readString(file.toPath());
                JOptionPane.showMessageDialog(this, "Código carregado com sucesso!");
            } catch (IOException ex) {
                mostrarErro("Erro ao ler o arquivo: " + ex.getMessage());
            }
        }
    }

    private void executarCodigo() {
        astArea.setText("");
        outputArea.setText("");
        try {
            // Etapas de compilação e execução
            Lexer lexer = new Lexer(codigoFonte);
            List<Token> tokens = lexer.tokenize();

            Parser parser = new Parser(tokens);
            ASTNode ast = parser.parseProgram();
            astArea.setText(astToString(ast, ""));

            SemanticAnalyzer sem = new SemanticAnalyzer();
            sem.analyze(ast);

            // Redirecionar saída para capturar o print
            PrintStream originalOut = System.out;
            ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outputCapture));

            // Criar e configurar interpretador
            Interpreter interpreter = new Interpreter();
            interpreter.setupFunctionEvaluation();  // importante para chamadas de função e `import`
            interpreter.execute(ast);

            System.setOut(originalOut);
            outputArea.setText(outputCapture.toString());

        } catch (Exception e) {
            mostrarErro("Erro: " + e.getMessage());
        }
    }

    private void mostrarErro(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    private String astToString(ASTNode node, String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append(node.getType());
        if (!node.getValue().isEmpty()) {
            sb.append(" (").append(node.getValue()).append(")");
        }
        sb.append("\n");
        for (ASTNode child : node.getChildren()) {
            sb.append(astToString(child, indent + "  "));
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MiniParGUI().setVisible(true));
    }
}
