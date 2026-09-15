package edu.umg.programacion2.proyecto.excepcion;

/** Mensaje seguro para la interfaz, conservando la causa para diagnóstico. */
public class PersistenciaException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public PersistenciaException(String mensaje) {
        super(mensaje);
    }

    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
