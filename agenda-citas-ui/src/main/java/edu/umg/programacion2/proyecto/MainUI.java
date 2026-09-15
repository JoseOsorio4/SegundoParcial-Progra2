package edu.umg.programacion2.proyecto;

import java.awt.EventQueue;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import edu.umg.programacion2.proyecto.config.ConexionBD;
import edu.umg.programacion2.proyecto.dao.CitaDAO;
import edu.umg.programacion2.proyecto.excepcion.PersistenciaException;
import edu.umg.programacion2.proyecto.servicio.CitaServicio;
import edu.umg.programacion2.proyecto.ui.VentanaPrincipal;

/** Punto de entrada de la aplicación. La interfaz se crea en el hilo de Swing. */
public final class MainUI {

    private MainUI() {
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                Logger.getLogger(MainUI.class.getName()).warning(
                        "No se pudo aplicar el aspecto del sistema; se usará el aspecto predeterminado de Swing.");
            }

            try {
                ConexionBD conexion = ConexionBD.desdeConfiguracion();
                CitaServicio servicio = new CitaServicio(new CitaDAO(conexion));
                new VentanaPrincipal(servicio).setVisible(true);
            } catch (PersistenciaException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(),
                        "Configuración de la base de datos", JOptionPane.ERROR_MESSAGE);
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(null,
                        "No se pudo iniciar la agenda. Revisa la configuración local de la base de datos.",
                        "No se pudo iniciar", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
