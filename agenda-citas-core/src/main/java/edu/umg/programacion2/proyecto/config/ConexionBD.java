package edu.umg.programacion2.proyecto.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import edu.umg.programacion2.proyecto.excepcion.PersistenciaException;

/** Abre conexiones JDBC con credenciales externas al código y al repositorio. */
public final class ConexionBD {
    private final String url;
    private final String usuario;
    private final String contrasena;

    public ConexionBD(String host, int puerto, String nombreBD, String usuario, String contrasena) {
        if (host == null || !host.matches("[A-Za-z0-9._:\\[\\]-]+")
                || puerto < 1 || puerto > 65535
                || nombreBD == null || !nombreBD.matches("[A-Za-z0-9_]+")
                || usuario == null || usuario.trim().isEmpty() || contrasena == null) {
            throw new PersistenciaException("La configuración de la base de datos no es válida.");
        }
        this.url = "jdbc:mysql://" + host + ":" + puerto + "/" + nombreBD
                + "?connectTimeout=5000&socketTimeout=10000"
                + "&useUnicode=true&characterEncoding=UTF-8"
                + "&allowPublicKeyRetrieval=true&useSSL=false";
        this.usuario = usuario.trim();
        this.contrasena = contrasena;
    }

    public static ConexionBD desdeConfiguracion() {
        Path archivo = localizarConfiguracion();
        Properties propiedades = new Properties();
        try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            propiedades.load(lector);
        } catch (IOException | IllegalArgumentException e) {
            throw new PersistenciaException("No se pudo leer db-local.properties. Revisa su ubicación y formato.", e);
        }

        String host = propiedades.getProperty("db.host", "127.0.0.1").trim();
        String nombreBD = propiedades.getProperty("db.name", "segundo_parcial_progra2").trim();
        String usuario = propiedades.getProperty("db.user");
        String contrasena = propiedades.getProperty("db.password");
        int puerto;
        try {
            puerto = Integer.parseInt(propiedades.getProperty("db.port", "3306").trim());
        } catch (NumberFormatException e) {
            throw new PersistenciaException("El puerto de la base de datos debe ser un número válido.", e);
        }
        return new ConexionBD(host, puerto, nombreBD, usuario, contrasena);
    }

    private static Path localizarConfiguracion() {
        String ruta = System.getProperty("agenda.db.config");
        if (ruta == null || ruta.trim().isEmpty()) {
            ruta = System.getenv("AGENDA_DB_CONFIG");
        }
        if (ruta != null && !ruta.trim().isEmpty()) {
            try {
                Path archivo = Paths.get(ruta).toAbsolutePath().normalize();
                if (Files.isRegularFile(archivo)) {
                    return archivo;
                }
            } catch (InvalidPathException e) {
                throw new PersistenciaException("La ruta configurada para db-local.properties no es válida.", e);
            }
            throw new PersistenciaException("No se encontró el archivo indicado por agenda.db.config o AGENDA_DB_CONFIG.");
        }

        Path directorio = Paths.get("").toAbsolutePath().normalize();
        while (directorio != null) {
            Path candidato = directorio.resolve("db-local.properties");
            if (Files.isRegularFile(candidato)) {
                return candidato;
            }
            directorio = directorio.getParent();
        }
        throw new PersistenciaException("No se encontró db-local.properties. Crea ese archivo con la configuración de MySQL.");
    }

    public Connection abrir() throws SQLException {
        Properties credenciales = new Properties();
        credenciales.setProperty("user", usuario);
        credenciales.setProperty("password", contrasena);
        return DriverManager.getConnection(url, credenciales);
    }
}
