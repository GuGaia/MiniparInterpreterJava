package minipar.gui;

import minipar.lexer.*;
import minipar.parser.*;
import minipar.semantic.*;
import minipar.interpreter.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.util.List;

public class MiniParGUI extends JFrame {

    private JTextArea codeArea;
    private JTextArea astArea;
    private JTextArea outputArea;
    private File currentFile = null;

    public MiniParGUI() {
        setTitle("MiniPar - Interpretador Visual");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Criação das áreas
        codeArea = criarTextArea(true);
        astArea = criarTextArea(false);
        outputArea = criarTextArea(false);

        // Painéis organizados em abas
        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Código Fonte", new JScrollPane(codeArea));
        abas.addTab("AST (Árvore Sintática)", new JScrollPane(astArea));
        abas.addTab("Saída do Programa", new JScrollPane(outputArea));
        add(abas, BorderLayout.CENTER);

        // Menus
        setJMenuBar(criarMenuBar());

        setVisible(true);
    }

    private JTextArea criarTextArea(boolean editavel) {
        JTextArea area = new JTextArea();
        area.setFont(new Font("Consolas", Font.PLAIN, 14));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(editavel);
        return area;
    }

    private JMenuBar criarMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu menuArquivo = new JMenu("Arquivo");
        JMenuItem abrir = new JMenuItem("Abrir");
        JMenuItem salvar = new JMenuItem("Salvar como");
        JMenuItem sair = new JMenuItem("Sair");

        abrir.addActionListener(e -> carregarCodigo());
        salvar.addActionListener(e -> salvarCodigo());
        sair.addActionListener(e -> System.exit(0));

        menuArquivo.add(abrir);
        menuArquivo.add(salvar);
        menuArquivo.addSeparator();
        menuArquivo.add(sair);

        JMenu menuExecutar = new JMenu("Executar");
        JMenuItem executar = new JMenuItem("Executar Código");
        executar.addActionListener(e -> executarCodigo());
        menuExecutar.add(executar);

        JMenu menuAjuda = new JMenu("Ajuda");
        JMenuItem sobre = new JMenuItem("Sobre");
        sobre.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "MiniPar IDE\nDesenvolvido em Java\nVersão 1.0", "Sobre", JOptionPane.INFORMATION_MESSAGE));
        menuAjuda.add(sobre);

        menuBar.add(menuArquivo);
        menuBar.add(menuExecutar);
        menuBar.add(menuAjuda);

        return menuBar;
    }

    private void carregarCodigo() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            currentFile = chooser.getSelectedFile();
            try {
                String codigoFonte = Files.readString(currentFile.toPath());
                codeArea.setText(codigoFonte);
                setTitle("MiniPar - " + currentFile.getName());
            } catch (IOException ex) {
                mostrarErro("Erro ao ler o arquivo: " + ex.getMessage());
            }
        }
    }

    private void salvarCodigo() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                Files.writeString(file.toPath(), codeArea.getText());
                JOptionPane.showMessageDialog(this, "Arquivo salvo com sucesso.");
            } catch (IOException ex) {
                mostrarErro("Erro ao salvar o arquivo: " + ex.getMessage());
            }
        }
    }

    private void executarCodigo() {
        astArea.setText("");
        outputArea.setText("");
        String codigoFonte = codeArea.getText();

        if (codigoFonte.isBlank()) {
            mostrarErro("Nenhum código fornecido.");
            return;
        }

        try {
            Lexer lexer = new Lexer(codigoFonte);
            List<Token> tokens = lexer.tokenize();

            Parser parser = new Parser(tokens);
            ASTNode ast = parser.parseProgram();
            astArea.setText(astToString(ast, ""));

            SemanticAnalyzer sem = new SemanticAnalyzer();
            sem.analyze(ast);

            PrintStream originalOut = System.out;
            ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outputCapture));

            Interpreter interpreter = new Interpreter();
            interpreter.execute(ast);

            System.setOut(originalOut);
            outputArea.setText(outputCapture.toString());

        } catch (Exception e) {
            mostrarErro("Erro ao executar: " + e.getMessage());
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
        SwingUtilities.invokeLater(MiniParGUI::new);
    }
}
