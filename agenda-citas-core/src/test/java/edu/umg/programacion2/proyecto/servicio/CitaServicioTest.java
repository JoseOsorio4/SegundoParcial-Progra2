package edu.umg.programacion2.proyecto.servicio;

import static org.junit.Assert.*;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import edu.umg.programacion2.proyecto.config.ConexionBD;
import edu.umg.programacion2.proyecto.dao.CitaDAO;
import edu.umg.programacion2.proyecto.excepcion.PersistenciaException;
import edu.umg.programacion2.proyecto.excepcion.ValidacionException;
import edu.umg.programacion2.proyecto.modelo.Cita;
import edu.umg.programacion2.proyecto.modelo.EstadoCita;

/** Pruebas de negocio aisladas: el doble del DAO nunca abre una conexión. */
public class CitaServicioTest {
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-09-13T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.now(RELOJ);
    private DAODoble dao;
    private CitaServicio servicio;

    @Before
    public void preparar() {
        dao = new DAODoble();
        servicio = new CitaServicio(dao, RELOJ);
    }

    @Test
    public void crearNormalizaLosTextosYFuerzaPendiente() {
        Cita original = new Cita(999, "  María López  ", AHORA.plusDays(1), "  Corte  ", 45, EstadoCita.CONFIRMADA);
        Cita creada = servicio.crear(original);

        assertEquals(1, dao.llamadas);
        assertEquals(0, dao.ultimaCita.getId());
        assertEquals(42, creada.getId());
        assertEquals("María López", creada.getCliente());
        assertEquals("Corte", creada.getServicio());
        assertEquals(EstadoCita.PENDIENTE, creada.getEstado());
        assertEquals(EstadoCita.CONFIRMADA, original.getEstado());
        assertEquals("  María López  ", original.getCliente());
    }

    @Test
    public void rechazaClienteVacioAntesDelDAO() {
        for (String cliente : new String[] {null, "", "  \t\n"}) {
            verificarRechazo(() -> servicio.crear(cita(0, cliente, AHORA.plusDays(1), "Servicio", 30, EstadoCita.PENDIENTE)));
        }
    }

    @Test
    public void rechazaServicioVacioAntesDelDAO() {
        for (String descripcion : new String[] {null, "", "  \t\n"}) {
            verificarRechazo(() -> servicio.crear(cita(0, "Cliente", AHORA.plusDays(1), descripcion, 30, EstadoCita.PENDIENTE)));
        }
    }

    @Test
    public void limitaLasLongitudesSegunElEsquema() {
        verificarRechazo(() -> servicio.crear(cita(0, "x".repeat(101), AHORA.plusDays(1), "Servicio", 30, EstadoCita.PENDIENTE)));
        verificarRechazo(() -> servicio.crear(cita(0, "Cliente", AHORA.plusDays(1), "x".repeat(256), 30, EstadoCita.PENDIENTE)));
        Cita creada = servicio.crear(cita(0, "x".repeat(100), AHORA.plusDays(1), "x".repeat(255), 30, EstadoCita.PENDIENTE));
        assertEquals(100, creada.getCliente().length());
        assertEquals(255, creada.getServicio().length());
    }

    @Test
    public void rechazaDuracionesNoPositivas() {
        for (int duracion : new int[] {0, -1, Integer.MIN_VALUE}) {
            verificarRechazo(() -> servicio.crear(cita(0, "Cliente", AHORA.plusDays(1), "Servicio", duracion, EstadoCita.PENDIENTE)));
        }
        assertEquals(1, servicio.crear(cita(0, "Cliente", AHORA.plusDays(1), "Servicio", 1, EstadoCita.PENDIENTE)).getDuracionMinutos());
    }

    @Test
    public void rechazaCitaFechaOEstadoNulos() {
        verificarRechazo(() -> servicio.crear(null));
        verificarRechazo(() -> servicio.crear(cita(0, "Cliente", null, "Servicio", 30, EstadoCita.PENDIENTE)));
        verificarRechazo(() -> servicio.crear(cita(0, "Cliente", AHORA.plusDays(1), "Servicio", 30, null)));
    }

    @Test
    public void rechazaElPasadoAlCrearPeroAdmiteElInstanteActual() {
        verificarRechazo(() -> servicio.crear(cita(0, "Cliente", AHORA.minusSeconds(1), "Servicio", 30, EstadoCita.PENDIENTE)));
        assertNotNull(servicio.crear(cita(0, "Cliente", AHORA, "Servicio", 30, EstadoCita.PENDIENTE)));
    }

    @Test
    public void validaElRangoDeFechaQuePuedeGuardarMySQL() {
        verificarRechazo(() -> servicio.crear(cita(0, "Cliente", AHORA.withYear(10000), "Servicio", 30, EstadoCita.PENDIENTE)));
        verificarRechazo(() -> servicio.actualizar(cita(7, "Cliente", AHORA.withYear(0), "Servicio", 30, EstadoCita.PENDIENTE)));
    }

    @Test
    public void permiteCancelarUnaCitaHistoricaSinEliminarla() {
        Cita historica = cita(7, " Cliente ", AHORA.minusDays(1), " Servicio ", 30, EstadoCita.CANCELADA);
        assertTrue(servicio.actualizar(historica));
        assertEquals("actualizar", dao.ultimaOperacion);
        assertEquals(7, dao.ultimaCita.getId());
        assertEquals(EstadoCita.CANCELADA, dao.ultimaCita.getEstado());
        assertEquals("Cliente", dao.ultimaCita.getCliente());
    }

    @Test
    public void editarTambienValidaLosCamposAntesDelDAO() {
        verificarRechazo(() -> servicio.actualizar(cita(7, " ", AHORA.minusDays(1), "Servicio", 30, EstadoCita.CONFIRMADA)));
        verificarRechazo(() -> servicio.actualizar(cita(7, "Cliente", AHORA.minusDays(1), "Servicio", 0, EstadoCita.CONFIRMADA)));
    }

    @Test
    public void rechazaIdentificadoresInvalidosAntesDelDAO() {
        for (int id : new int[] {0, -1}) {
            verificarRechazo(() -> servicio.eliminar(id));
            verificarRechazo(() -> servicio.buscarPorId(id));
            verificarRechazo(() -> servicio.actualizar(cita(id, "Cliente", AHORA, "Servicio", 30, EstadoCita.PENDIENTE)));
        }
    }

    @Test
    public void conservaLaRespuestaParaFilasQueYaNoExisten() {
        dao.resultadoCambio = false;
        assertFalse(servicio.actualizar(cita(7, "Cliente", AHORA, "Servicio", 30, EstadoCita.PENDIENTE)));
        assertFalse(servicio.eliminar(7));
        assertEquals("eliminar", dao.ultimaOperacion);
        assertEquals(7, dao.ultimoId);
        assertFalse(servicio.buscarPorId(7).isPresent());
        assertTrue(servicio.listarTodos().isEmpty());
    }

    @Test
    public void traduceLosErroresDeTodasLasOperacionesSinMostrarSusDetalles() {
        dao.fallo = new SQLException("Contraseña privada y detalles internos de conexión");
        Cita valida = cita(7, "Cliente", AHORA.plusDays(1), "Servicio", 30, EstadoCita.PENDIENTE);
        Runnable[] operaciones = {
            () -> servicio.crear(valida), () -> servicio.listarTodos(),
            () -> servicio.buscarPorId(7), () -> servicio.actualizar(valida), () -> servicio.eliminar(7)
        };
        for (Runnable operacion : operaciones) {
            try {
                operacion.run();
                fail("Se esperaba un error de persistencia.");
            } catch (PersistenciaException e) {
                assertSame(dao.fallo, e.getCause());
                assertFalse(e.getMessage().contains("Contraseña privada"));
                assertFalse(e.getMessage().contains("detalles internos"));
                assertFalse(e.getMessage().isEmpty());
            }
        }
        assertEquals(5, dao.llamadas);
    }

    @Test
    public void estadosUsanSoloLosTresValoresDelContrato() {
        assertEquals(3, EstadoCita.values().length);
        assertEquals("Pendiente", EstadoCita.PENDIENTE.toString());
        assertEquals("confirmada", EstadoCita.CONFIRMADA.getValorBD());
        assertEquals(EstadoCita.CANCELADA, EstadoCita.desdeBD("cancelada"));
        try {
            EstadoCita.desdeBD("completada");
            fail("Un estado desconocido no puede ingresar a la aplicación.");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    private void verificarRechazo(Runnable accion) {
        int llamadasAntes = dao.llamadas;
        try {
            accion.run();
            fail("Debió fallar la validación.");
        } catch (ValidacionException e) {
            assertNotNull(e.getMessage());
        }
        assertEquals("Una entrada inválida no debe llegar al DAO.", llamadasAntes, dao.llamadas);
    }

    private static Cita cita(int id, String cliente, LocalDateTime fecha, String descripcion, int minutos, EstadoCita estado) {
        return new Cita(id, cliente, fecha, descripcion, minutos, estado);
    }

    private static class DAODoble extends CitaDAO {
        private int llamadas;
        private Cita ultimaCita;
        private String ultimaOperacion;
        private int ultimoId;
        private boolean resultadoCambio = true;
        private SQLException fallo;

        DAODoble() {
            super(new ConexionBD("127.0.0.1", 3306, "pruebas_sin_conexion", "sin_conexion", ""));
        }

        private void registrar(String operacion) throws SQLException {
            llamadas++;
            ultimaOperacion = operacion;
            if (fallo != null) {
                throw fallo;
            }
        }

        @Override
        public Cita crear(Cita cita) throws SQLException {
            registrar("crear");
            ultimaCita = cita;
            return new Cita(42, cita.getCliente(), cita.getFechaHora(), cita.getServicio(), cita.getDuracionMinutos(), cita.getEstado());
        }

        @Override
        public List<Cita> listarTodos() throws SQLException {
            registrar("listar");
            return Collections.emptyList();
        }

        @Override
        public Optional<Cita> buscarPorId(int id) throws SQLException {
            registrar("buscar");
            ultimoId = id;
            return Optional.empty();
        }

        @Override
        public boolean actualizar(Cita cita) throws SQLException {
            registrar("actualizar");
            ultimaCita = cita;
            return resultadoCambio;
        }

        @Override
        public boolean eliminar(int id) throws SQLException {
            registrar("eliminar");
            ultimoId = id;
            return resultadoCambio;
        }
    }
}
