package edu.umg.programacion2.proyecto.ui;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import edu.umg.programacion2.proyecto.modelo.Cita;
import edu.umg.programacion2.proyecto.modelo.EstadoCita;

/** Conserva los tipos de los datos para ordenar correctamente fechas y números. */
public final class ModeloTablaCitas extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNAS = {
        "ID",
        "Cliente",
        "Fecha y hora",
        "Servicio",
        "Duración",
        "Estado",
        "Confirmación"
    };

    private static final Class<?>[] TIPOS = {
        Integer.class,
        String.class,
        LocalDateTime.class,
        String.class,
        Integer.class,
        EstadoCita.class,
        Boolean.class
    };

    private final List<Cita> citas = new ArrayList<>();

    public void reemplazar(List<Cita> nuevasCitas) {
        citas.clear();
        citas.addAll(nuevasCitas);
        fireTableDataChanged();
    }

    public Cita obtenerCita(int filaModelo) {
        return citas.get(filaModelo);
    }

    @Override
    public int getRowCount() {
        return citas.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNAS.length;
    }

    @Override
    public String getColumnName(int columna) {
        return COLUMNAS[columna];
    }

    @Override
    public Class<?> getColumnClass(int columna) {
        return TIPOS[columna];
    }

    @Override
    public boolean isCellEditable(int fila, int columna) {
        return false;
    }

    @Override
    public Object getValueAt(int fila, int columna) {

        Cita cita = citas.get(fila);

        switch (columna) {
            case 0:
                return cita.getId();

            case 1:
                return cita.getCliente();

            case 2:
                return cita.getFechaHora();

            case 3:
                return cita.getServicio();

            case 4:
                return cita.getDuracionMinutos();

            case 5:
                return cita.getEstado();

            case 6:
                return cita.isRequiereConfirmacionLlamada();

            default:
                throw new IndexOutOfBoundsException(
                        "Columna inexistente: " + columna);
        }
    }
}