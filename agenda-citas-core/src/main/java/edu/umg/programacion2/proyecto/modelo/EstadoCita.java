package edu.umg.programacion2.proyecto.modelo;

/** Los únicos estados admitidos por la agenda y la base de datos. */
public enum EstadoCita {
    PENDIENTE("pendiente", "Pendiente"),
    CONFIRMADA("confirmada", "Confirmada"),
    CANCELADA("cancelada", "Cancelada");

    private final String valorBD;
    private final String etiqueta;

    EstadoCita(String valorBD, String etiqueta) {
        this.valorBD = valorBD;
        this.etiqueta = etiqueta;
    }

    public String getValorBD() {
        return valorBD;
    }

    public static EstadoCita desdeBD(String valor) {
        for (EstadoCita estado : values()) {
            if (estado.valorBD.equals(valor)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("El estado almacenado no es válido.");
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
