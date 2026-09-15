import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

import edu.umg.programacion2.proyecto.config.ConexionBD;
import edu.umg.programacion2.proyecto.dao.CitaDAO;
import edu.umg.programacion2.proyecto.excepcion.PersistenciaException;
import edu.umg.programacion2.proyecto.excepcion.ValidacionException;
import edu.umg.programacion2.proyecto.modelo.Cita;
import edu.umg.programacion2.proyecto.modelo.EstadoCita;
import edu.umg.programacion2.proyecto.servicio.CitaServicio;

/** Prueba optativa contra MySQL local. Solo elimina la cita creada por esta prueba. */
public class PruebaPersistencia {
    private static int comprobaciones;

    public static void main(String[] args) throws Exception {
        ConexionBD conexion = ConexionBD.desdeConfiguracion();
        CitaServicio servicio = new CitaServicio(new CitaDAO(conexion));
        LocalDateTime fecha = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);
        int id = 0;
        try {
            Cita nueva = servicio.crear(new Cita(0, "  Prueba O'Connor Á  ", fecha,
                    "  Servicio de prueba  ", 45, EstadoCita.CANCELADA));
            id = nueva.getId();
            verificar(id > 0, "Identificador generado");
            verificar(nueva.getEstado() == EstadoCita.PENDIENTE, "Estado inicial pendiente");
            CitaServicio otraConexion = new CitaServicio(new CitaDAO(ConexionBD.desdeConfiguracion()));
            Cita leida = otraConexion.buscarPorId(id).orElseThrow(() -> new AssertionError("No persistió la cita"));
            verificar(leida.getCliente().equals("Prueba O'Connor Á"), "Tildes, apóstrofe y trim");
            verificar(leida.getFechaHora().equals(fecha), "Fecha y hora sin desplazamiento");
            final int idCreado = id;
            verificar(otraConexion.listarTodos().stream().anyMatch(c -> c.getId() == idCreado), "Listado persistente");
            Cita editada = new Cita(id, leida.getCliente(), fecha.plusHours(1), "Servicio actualizado", 60,
                    EstadoCita.CONFIRMADA);
            verificar(servicio.actualizar(editada), "Actualización del mismo ID");
            leida = otraConexion.buscarPorId(id).get();
            verificar(leida.getDuracionMinutos() == 60 && leida.getServicio().equals("Servicio actualizado")
                    && leida.getEstado() == EstadoCita.CONFIRMADA
                    && leida.getFechaHora().equals(fecha.plusHours(1)), "Campos actualizados");
            verificar(servicio.actualizar(editada), "Actualizar sin cambiar datos conserva registro");
            Cita cancelada = new Cita(id, leida.getCliente(), LocalDateTime.now().minusDays(1).withNano(0),
                    leida.getServicio(), 60, EstadoCita.CANCELADA);
            verificar(servicio.actualizar(cancelada), "Permite editar estado de cita histórica");
            verificar(otraConexion.buscarPorId(id).get().getEstado() == EstadoCita.CANCELADA,
                    "Cancelar conserva la fila");

            rechazar(() -> servicio.crear(new Cita(0, " ", fecha, "Servicio", 1, EstadoCita.PENDIENTE)),
                    "Cliente vacío");
            rechazar(() -> servicio.crear(new Cita(0, "Cliente", fecha, " ", 1, EstadoCita.PENDIENTE)),
                    "Servicio vacío");
            rechazar(() -> servicio.crear(new Cita(0, "Cliente", fecha, "Servicio", 0, EstadoCita.PENDIENTE)),
                    "Duración cero");
            rechazar(() -> servicio.crear(new Cita(0, "Cliente", fecha.minusDays(2), "Servicio", 1,
                    EstadoCita.PENDIENTE)), "Fecha pasada al crear");

            try (Connection c = conexion.abrir(); PreparedStatement p = c.prepareStatement(
                    "UPDATE citas SET duracion_minutos = 0 WHERE id = ?")) {
                p.setInt(1, id);
                try { p.executeUpdate(); throw new AssertionError("La BD aceptó duración cero"); }
                catch (SQLException esperada) { comprobaciones++; }
            }
            verificar(servicio.eliminar(id), "Borrado físico");
            verificar(!otraConexion.buscarPorId(id).isPresent(), "Borrado visible desde otra conexión");
            verificar(!servicio.eliminar(id), "Eliminar registro ausente devuelve false");
            verificar(!servicio.actualizar(editada), "Actualizar registro ausente devuelve false");
            id = 0;

            CitaServicio sinConexion = new CitaServicio(new CitaDAO(
                    new ConexionBD("127.0.0.1", 1, "segundo_parcial_progra2", "prueba", "")));
            try { sinConexion.listarTodos(); throw new AssertionError("Debía fallar la conexión de prueba"); }
            catch (PersistenciaException esperada) {
                verificar(!esperada.getMessage().contains("java.sql") && !esperada.getMessage().contains("jdbc:"),
                        "Error de conexión seguro");
            }
            System.out.println("OK: " + comprobaciones + " comprobaciones contra MySQL. Datos de prueba eliminados.");
        } finally {
            if (id > 0) { servicio.eliminar(id); }
        }
    }

    private static void verificar(boolean condicion, String detalle) {
        if (!condicion) { throw new AssertionError(detalle); }
        comprobaciones++;
    }

    private static void rechazar(Runnable operacion, String detalle) {
        try { operacion.run(); throw new AssertionError("Se aceptó: " + detalle); }
        catch (ValidacionException esperada) { comprobaciones++; }
    }
}
