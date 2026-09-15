package edu.umg.programacion2.proyecto.excepcion;

/** Error de entrada que la interfaz puede explicar al usuario. */
public class ValidacionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
