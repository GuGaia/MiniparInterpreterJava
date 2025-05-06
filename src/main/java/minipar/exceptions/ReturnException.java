package minipar.exceptions;

public class ReturnException extends RuntimeException {
    public final int valor;
    public ReturnException(int valor) {
        this.valor = valor;
    }
}
