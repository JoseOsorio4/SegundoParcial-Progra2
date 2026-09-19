package edu.umg.programacion2.proyecto.servicio;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import edu.umg.programacion2.proyecto.dao.CitaDAO;
import edu.umg.programacion2.proyecto.excepcion.PersistenciaException;
import edu.umg.programacion2.proyecto.excepcion.ValidacionException;
import edu.umg.programacion2.proyecto.modelo.Cita;
import edu.umg.programacion2.proyecto.modelo.EstadoCita;

/** Aplica reglas antes del DAO y transforma errores JDBC en mensajes seguros. */
public class CitaServicio {

    private final CitaDAO dao;
    private final Clock reloj;

    public CitaServicio(CitaDAO dao) {
        this(dao, Clock.systemDefaultZone());
    }

    public CitaServicio(CitaDAO dao, Clock reloj) {
        this.dao = Objects.requireNonNull(dao, "Se requiere un DAO.");
        this.reloj = Objects.requireNonNull(reloj, "Se requiere un reloj.");
    }

    public Cita crear(Cita cita) {
        validarDatos(cita);

        if (cita.getFechaHora().isBefore(LocalDateTime.now(reloj))) {
            throw new ValidacionException(
                    "La fecha y hora de una cita nueva no pueden estar en el pasado.");
        }

        Cita nueva = normalizar(cita, 0, EstadoCita.PENDIENTE);

        return ejecutar(
                () -> dao.crear(nueva),
                "No se pudo crear la cita.");
    }

    public List<Cita> listarTodos() {
        return ejecutar(
                dao::listarTodos,
                "No se pudo cargar el listado de citas.");
    }

    public Optional<Cita> buscarPorId(int id) {
        validarId(id);

        return ejecutar(
                () -> dao.buscarPorId(id),
                "No se pudo consultar la cita.");
    }

    public boolean actualizar(Cita cita) {
        validarDatos(cita);
        validarId(cita.getId());

        // La regla de fecha futura se exige al crear;
        // una cita histórica puede cambiar de estado.
        Cita actualizada = normalizar(
                cita,
                cita.getId(),
                cita.getEstado());

        return ejecutar(
                () -> dao.actualizar(actualizada),
                "No se pudo actualizar la cita.");
    }

    public boolean eliminar(int id) {
        validarId(id);

        return ejecutar(
                () -> dao.eliminar(id),
                "No se pudo eliminar la cita.");
    }

    private static void validarDatos(Cita cita) {

        if (cita == null) {
            throw new ValidacionException(
                    "Debes ingresar los datos de la cita.");
        }

        validarTexto(
                cita.getCliente(),
                "El nombre del cliente",
                100);

        validarTexto(
                cita.getServicio(),
                "La descripción del servicio",
                255);

        if (cita.getFechaHora() == null) {
            throw new ValidacionException(
                    "Debes ingresar una fecha y hora válidas.");
        }

        if (cita.getFechaHora().getYear() < 1000
                || cita.getFechaHora().getYear() > 9999) {

            throw new ValidacionException(
                    "El año de la cita debe estar entre 1000 y 9999.");
        }

        if (cita.getDuracionMinutos() <= 0) {
            throw new ValidacionException(
                    "La duración debe ser mayor a cero minutos.");
        }

        if (cita.getEstado() == null) {
            throw new ValidacionException(
                    "Debes seleccionar un estado válido.");
        }
    }

    private static void validarTexto(
            String texto,
            String nombre,
            int maximo) {

        if (texto == null || texto.trim().isEmpty()) {
            throw new ValidacionException(
                    nombre + " no puede quedar vacío.");
        }

        if (texto.trim().length() > maximo) {
            throw new ValidacionException(
                    nombre + " admite como máximo "
                            + maximo
                            + " caracteres.");
        }
    }

    private static void validarId(int id) {

        if (id <= 0) {
            throw new ValidacionException(
                    "Selecciona una cita existente.");
        }
    }

    private static Cita normalizar(
            Cita cita,
            int id,
            EstadoCita estado) {

        return new Cita(
                id,
                cita.getCliente().trim(),
                cita.getFechaHora(),
                cita.getServicio().trim(),
                cita.getDuracionMinutos(),
                estado,
                cita.isRequiereConfirmacionLlamada());
    }

    private static <T> T ejecutar(
            OperacionBD<T> operacion,
            String mensaje) {

        try {
            return operacion.ejecutar();
        } catch (SQLException e) {
            throw new PersistenciaException(
                    mensaje
                            + " Revisa la conexión y la configuración de MySQL.",
                    e);
        }
    }

    @FunctionalInterface
    private interface OperacionBD<T> {
        T ejecutar() throws SQLException;
    }
}