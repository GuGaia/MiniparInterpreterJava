package minipar.interpreter;

import java.io.*;
import java.net.*;

public class Canal {
    private final String nome;
    private final int porta;

    public Canal(String nome, int porta) {
        this.nome = nome;
        this.porta = porta;
    }

    public void send(String mensagem) {
        try (Socket socket = new Socket("localhost", porta);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(mensagem);
            System.out.println("Enviado via canal " + nome + ": " + mensagem);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao enviar no canal '" + nome + "': " + e.getMessage(), e);
        }
    }

    public String receive() {
        try (ServerSocket serverSocket = new ServerSocket(porta);
             Socket socket = serverSocket.accept();
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String recebido = in.readLine();
            System.out.println("Recebido no canal " + nome + ": " + recebido);
            return recebido;
        } catch (IOException e) {
            throw new RuntimeException("Erro ao receber no canal '" + nome + "': " + e.getMessage(), e);
        }
    }

    public int getPorta() {
        return porta;
    }
}
