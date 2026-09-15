package edu.umg.programacion2.proyecto.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import edu.umg.programacion2.proyecto.config.ConexionBD;
import edu.umg.programacion2.proyecto.modelo.Cita;
import edu.umg.programacion2.proyecto.modelo.EstadoCita;

/** Traduce entre objetos Cita y filas SQL; no conoce Swing. */
public class CitaDAO {
    private static final String COLUMNAS = "id, cliente, fecha_hora, servicio, duracion_minutos, estado";
    private final ConexionBD conexionBD;

    public CitaDAO(ConexionBD conexionBD) {
        this.conexionBD = Objects.requireNonNull(conexionBD, "Se requiere una configuración de conexión.");
    }

    public Cita crear(Cita cita) throws SQLException {
        String sql = "INSERT INTO citas (cliente, fecha_hora, servicio, duracion_minutos, estado) VALUES (?, ?, ?, ?, ?)";
        try (Connection conexion = conexionBD.abrir();
                PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            asignarDatos(sentencia, cita);
            if (sentencia.executeUpdate() != 1) {
                throw new SQLException("No se insertó la cita solicitada.");
            }
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException("No se obtuvo el identificador de la cita.");
                }
                return new Cita(claves.getInt(1), cita.getCliente(), cita.getFechaHora(),
                        cita.getServicio(), cita.getDuracionMinutos(), cita.getEstado());
            }
        }
    }

    public List<Cita> listarTodos() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM citas ORDER BY fecha_hora, id";
        List<Cita> citas = new ArrayList<>();
        try (Connection conexion = conexionBD.abrir();
                PreparedStatement sentencia = conexion.prepareStatement(sql);
                ResultSet resultados = sentencia.executeQuery()) {
            while (resultados.next()) {
                citas.add(leerCita(resultados));
            }
        }
        return citas;
    }

    public Optional<Cita> buscarPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM citas WHERE id = ?";
        try (Connection conexion = conexionBD.abrir();
                PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, id);
            try (ResultSet resultados = sentencia.executeQuery()) {
                return resultados.next() ? Optional.of(leerCita(resultados)) : Optional.empty();
            }
        }
    }

    public boolean actualizar(Cita cita) throws SQLException {
        String sql = "UPDATE citas SET cliente = ?, fecha_hora = ?, servicio = ?, duracion_minutos = ?, estado = ? WHERE id = ?";
        try (Connection conexion = conexionBD.abrir();
                PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            asignarDatos(sentencia, cita);
            sentencia.setInt(6, cita.getId());
            return sentencia.executeUpdate() > 0;
        }
    }

    public boolean eliminar(int id) throws SQLException {
        try (Connection conexion = conexionBD.abrir();
                PreparedStatement sentencia = conexion.prepareStatement("DELETE FROM citas WHERE id = ?")) {
            sentencia.setInt(1, id);
            return sentencia.executeUpdate() > 0;
        }
    }

    private static void asignarDatos(PreparedStatement sentencia, Cita cita) throws SQLException {
        sentencia.setString(1, cita.getCliente());
        sentencia.setTimestamp(2, Timestamp.valueOf(cita.getFechaHora()));
        sentencia.setString(3, cita.getServicio());
        sentencia.setInt(4, cita.getDuracionMinutos());
        sentencia.setString(5, cita.getEstado().getValorBD());
    }

    private static Cita leerCita(ResultSet fila) throws SQLException {
        Timestamp fechaHora = fila.getTimestamp("fecha_hora");
        if (fechaHora == null) {
            throw new SQLException("Una cita almacenada no tiene fecha y hora.");
        }
        EstadoCita estado;
        try {
            estado = EstadoCita.desdeBD(fila.getString("estado"));
        } catch (IllegalArgumentException e) {
            throw new SQLException("Una cita almacenada tiene un estado inválido.", e);
        }
        return new Cita(fila.getInt("id"), fila.getString("cliente"), fechaHora.toLocalDateTime(),
                fila.getString("servicio"), fila.getInt("duracion_minutos"), estado);
    }
}
