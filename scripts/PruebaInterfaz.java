import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.function.BooleanSupplier;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import edu.umg.programacion2.proyecto.config.ConexionBD;
import edu.umg.programacion2.proyecto.dao.CitaDAO;
import edu.umg.programacion2.proyecto.modelo.Cita;
import edu.umg.programacion2.proyecto.modelo.EstadoCita;
import edu.umg.programacion2.proyecto.servicio.CitaServicio;
import edu.umg.programacion2.proyecto.ui.ModeloTablaCitas;
import edu.umg.programacion2.proyecto.ui.VentanaPrincipal;

/**
 * Prueba optativa de nuestros componentes Swing con MySQL real.
 * No muestra el JFrame, no captura el escritorio ni envía entrada a otras apps.
 * Los JOptionPane propios se responden dentro del proceso y del EDT.
 */
public final class PruebaInterfaz {
    private static final long ESPERA_MAXIMA_MS = 25000;
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm");
    private static int comprobaciones;

    private PruebaInterfaz() {
    }

    public static void main(String[] args) throws Exception {
        CitaServicio servicio = new CitaServicio(new CitaDAO(ConexionBD.desdeConfiguracion()));
        String token = UUID.randomUUID().toString().substring(0, 12);
        String clienteA = "Prueba UI Alfa " + token;
        String clienteB = "Prueba UI Zeta Á " + token;
        List<String> clientesPrueba = Arrays.asList(clienteA, clienteB);
        Set<Integer> idsCreados = new LinkedHashSet<>();
        VentanaPrincipal[] referenciaVentana = new VentanaPrincipal[1];
        ControlDialogos[] referenciaDialogos = new ControlDialogos[1];
        boolean terminada = false;
        try {
            enEdt(() -> {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                VentanaPrincipal ventana = new VentanaPrincipal(servicio);
                referenciaVentana[0] = ventana;
                referenciaDialogos[0] = new ControlDialogos(ventana);
                referenciaDialogos[0].iniciar();
                return null;
            });
            VentanaPrincipal ventana = referenciaVentana[0];
            ControlDialogos dialogos = referenciaDialogos[0];
            esperarLista(ventana, dialogos);
            verificar(enEdt(() -> !ventana.isVisible()), "El JFrame de prueba permanece oculto");
            verificar(enEdt(() -> combo(ventana).getItemCount() == 3 && !combo(ventana).isEnabled()
                    && combo(ventana).getSelectedItem() == EstadoCita.PENDIENTE),
                    "Formulario nuevo tiene tres estados y fija Pendiente");

            LocalDateTime fechaA = LocalDateTime.now().plusDays(2).withSecond(0).withNano(0);
            LocalDateTime fechaB = fechaA.plusDays(1);
            crearDesdeFormulario(ventana, dialogos, clienteA, fechaA, "Consulta de prueba", "30");
            Cita citaA = localizarPrueba(servicio, clienteA);
            idsCreados.add(citaA.getId());
            verificar(citaA.getEstado() == EstadoCita.PENDIENTE && citaA.getDuracionMinutos() == 30,
                    "Alta desde Swing persiste con estado inicial Pendiente");
            verificar(enEdt(() -> filaModelo(ventana, citaA.getId()) >= 0),
                    "La tabla recarga la cita creada");

            crearDesdeFormulario(ventana, dialogos, clienteB, fechaB, "Servicio por actualizar", "45");
            Cita citaB = localizarPrueba(servicio, clienteB);
            idsCreados.add(citaB.getId());
            verificar(citaB.getFechaHora().equals(fechaB) && citaB.getCliente().equals(clienteB),
                    "Fecha y nombre con tilde persisten desde formulario");

            enEdt(() -> {
                JTable tabla = tabla(ventana);
                tabla.getRowSorter().setSortKeys(Collections.singletonList(
                        new RowSorter.SortKey(0, SortOrder.DESCENDING)));
                int modelo = filaModelo(ventana, citaB.getId());
                int vista = tabla.convertRowIndexToView(modelo);
                int vistaA = tabla.convertRowIndexToView(filaModelo(ventana, citaA.getId()));
                verificar(citaB.getId() > citaA.getId() && vista < vistaA,
                        "Orden descendente coloca el ID mayor antes del menor");
                tabla.setRowSelectionInterval(vista, vista);
                verificar(campo(ventana, "campoCliente").getText().equals(clienteB)
                        && campo(ventana, "campoServicio").getText().equals("Servicio por actualizar"),
                        "Seleccionar tabla ordenada carga la cita correcta");
                verificar(boton(ventana, "botonActualizar").isEnabled()
                        && boton(ventana, "botonEliminar").isEnabled()
                        && !boton(ventana, "botonGuardar").isEnabled() && combo(ventana).isEnabled(),
                        "Selección activa edición y bloquea creación duplicada");
                campo(ventana, "campoServicio").setText("Servicio actualizado desde Swing");
                campo(ventana, "campoDuracion").setText("60");
                combo(ventana).setSelectedItem(EstadoCita.CONFIRMADA);
                boton(ventana, "botonActualizar").doClick();
                return null;
            });
            esperarLista(ventana, dialogos);
            Cita actualizada = servicio.buscarPorId(citaB.getId()).orElseThrow(
                    () -> new AssertionError("La actualización perdió el registro"));
            verificar(actualizada.getServicio().equals("Servicio actualizado desde Swing")
                    && actualizada.getDuracionMinutos() == 60
                    && actualizada.getEstado() == EstadoCita.CONFIRMADA,
                    "Actualizar desde Swing conserva ID y modifica servicio, duración y estado");
            verificar(enEdt(() -> campo(ventana, "campoCliente").getText().isEmpty()
                    && !boton(ventana, "botonActualizar").isEnabled()
                    && boton(ventana, "botonGuardar").isEnabled()),
                    "La actualización limpia el formulario y vuelve al modo nuevo");

            enEdt(() -> {
                seleccionar(ventana, citaB.getId());
                campo(ventana, "campoFechaHora").setText("31/02/" + fechaB.getYear() + " 14:30");
                dialogos.esperar("Datos de la cita", "fecha y hora válidas", JOptionPane.OK_OPTION);
                boton(ventana, "botonActualizar").doClick();
                dialogos.verificarRespuesta();
                campo(ventana, "campoFechaHora").setText(FORMATO.format(fechaB));
                return null;
            });
            verificar(servicio.buscarPorId(citaB.getId()).get().getFechaHora().equals(fechaB),
                    "31 de febrero muestra validación y no modifica la base de datos");

            enEdt(() -> {
                campo(ventana, "campoDuracion").setText("no-es-un-numero");
                dialogos.esperar("Datos de la cita", "duración", JOptionPane.OK_OPTION);
                boton(ventana, "botonActualizar").doClick();
                dialogos.verificarRespuesta();
                campo(ventana, "campoDuracion").setText("60");
                combo(ventana).setSelectedItem(EstadoCita.CANCELADA);
                boton(ventana, "botonActualizar").doClick();
                return null;
            });
            esperarLista(ventana, dialogos);
            verificar(servicio.buscarPorId(citaB.getId()).get().getEstado() == EstadoCita.CANCELADA,
                    "Cancelar desde el formulario conserva la fila con estado Cancelada");
            enEdt(() -> {
                seleccionar(ventana, citaB.getId());
                ventana.validate();
                return null;
            });

            Path vistaPrevia = Paths.get(".tools", "agenda-preview.png").toAbsolutePath().normalize();
            Files.createDirectories(vistaPrevia.getParent());
            BufferedImage imagen = enEdt(() -> {
                Container contenido = ventana.getContentPane();
                ventana.validate();
                BufferedImage resultado = new BufferedImage(contenido.getWidth(), contenido.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D grafico = resultado.createGraphics();
                try {
                    contenido.printAll(grafico);
                } finally {
                    grafico.dispose();
                }
                return resultado;
            });
            verificar(ImageIO.write(imagen, "png", vistaPrevia.toFile()),
                    "Render interno del formulario y tabla generado");

            BufferedImage imagenMinima = enEdt(() -> {
                Dimension original = ventana.getSize();
                ventana.setSize(ventana.getMinimumSize());
                ventana.validate();
                JComboBox<?> estado = combo(ventana);
                estado.scrollRectToVisible(new Rectangle(0, 0, estado.getWidth(), estado.getHeight()));
                verificar(estado.getHeight() >= 30 && estado.getVisibleRect().height == estado.getHeight(),
                        "Estado es accesible completo al tamaño mínimo de la ventana");
                JButton guardar = boton(ventana, "botonActualizar");
                verificar(guardar.getVisibleRect().height == guardar.getHeight()
                        && boton(ventana, "botonEliminar").getVisibleRect().height >= 30,
                        "Los botones de guardar y eliminar permanecen accesibles al reducir la ventana");
                Container contenido = ventana.getContentPane();
                BufferedImage resultado = new BufferedImage(contenido.getWidth(), contenido.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D grafico = resultado.createGraphics();
                try { contenido.printAll(grafico); } finally { grafico.dispose(); }
                ventana.setSize(original);
                ventana.validate();
                return resultado;
            });
            ImageIO.write(imagenMinima, "png", Paths.get(".tools", "agenda-preview-minima.png").toFile());

            enEdt(() -> {
                dialogos.esperar("Confirmar eliminación", "Eliminar definitivamente", JOptionPane.NO_OPTION);
                boton(ventana, "botonEliminar").doClick();
                dialogos.verificarRespuesta();
                return null;
            });
            verificar(servicio.buscarPorId(citaB.getId()).isPresent(),
                    "Rechazar confirmación conserva la cita en MySQL");
            verificar(enEdt(() -> filaModelo(ventana, citaB.getId()) >= 0),
                    "Rechazar confirmación conserva la cita en la tabla");

            enEdt(() -> {
                dialogos.esperar("Confirmar eliminación", "Eliminar definitivamente", JOptionPane.YES_OPTION);
                boton(ventana, "botonEliminar").doClick();
                dialogos.verificarRespuesta();
                return null;
            });
            esperarLista(ventana, dialogos);
            verificar(!servicio.buscarPorId(citaB.getId()).isPresent(),
                    "Aceptar confirmación elimina físicamente la cita");
            verificar(enEdt(() -> filaModelo(ventana, citaB.getId()) < 0),
                    "El listado se recarga tras eliminar");
            verificar(servicio.buscarPorId(citaA.getId()).isPresent(),
                    "La eliminación no afecta otra cita de prueba");

            comprobarErrorConexion();
            terminada = true;
        } finally {
            Exception errorLimpieza = null;
            try {
                // Rescata el ID si un alta llegó a confirmarse antes de una falla del listado.
                for (Cita cita : servicio.listarTodos()) {
                    if (clientesPrueba.contains(cita.getCliente())) {
                        idsCreados.add(cita.getId());
                    }
                }
                for (int id : idsCreados) {
                    servicio.eliminar(id);
                }
            } catch (Exception ex) {
                errorLimpieza = ex;
            } finally {
                enEdt(() -> {
                    if (referenciaDialogos[0] != null) {
                        referenciaDialogos[0].detener();
                    }
                    VentanaPrincipal ventana = referenciaVentana[0];
                    if (ventana != null) {
                        for (Window propia : ventana.getOwnedWindows()) {
                            propia.dispose();
                        }
                        ventana.dispose();
                    }
                    return null;
                });
            }
            if (errorLimpieza != null) {
                throw new IllegalStateException("No se pudo completar la limpieza de los IDs de prueba: "
                        + idsCreados, errorLimpieza);
            }
        }
        if (terminada) {
            System.out.println("OK: " + comprobaciones + " comprobaciones Swing + MySQL. Datos de prueba eliminados.");
            System.out.println("Vista previa interna: .tools/agenda-preview.png");
        }
    }

    private static void comprobarErrorConexion() throws Exception {
        VentanaPrincipal[] ventanaError = new VentanaPrincipal[1];
        ControlDialogos[] dialogosError = new ControlDialogos[1];
        try {
            enEdt(() -> {
                CitaServicio desconectado = new CitaServicio(new CitaDAO(
                        new ConexionBD("127.0.0.1", 1, "segundo_parcial_progra2", "prueba", "")));
                ventanaError[0] = new VentanaPrincipal(desconectado);
                dialogosError[0] = new ControlDialogos(ventanaError[0]);
                dialogosError[0].esperar("No se pudo completar", "conexión", JOptionPane.OK_OPTION);
                dialogosError[0].iniciar();
                return null;
            });
            esperar(() -> {
                dialogosError[0].verificarSinError();
                return dialogosError[0].respondido
                        && boton(ventanaError[0], "botonRefrescar").isEnabled();
            });
            enEdt(() -> {
                dialogosError[0].verificarRespuesta();
                verificar(ventanaError[0].isDisplayable()
                        && boton(ventanaError[0], "botonRefrescar").isEnabled(),
                        "El fallo de conexión muestra un mensaje y permite reintentar sin cerrar la ventana");
                return null;
            });
        } finally {
            enEdt(() -> {
                if (dialogosError[0] != null) { dialogosError[0].detener(); }
                if (ventanaError[0] != null) {
                    for (Window propia : ventanaError[0].getOwnedWindows()) { propia.dispose(); }
                    ventanaError[0].dispose();
                }
                return null;
            });
        }
    }

    private static void crearDesdeFormulario(VentanaPrincipal ventana, ControlDialogos dialogos,
            String cliente, LocalDateTime fecha, String servicio, String duracion) throws Exception {
        enEdt(() -> {
            boton(ventana, "botonNuevo").doClick();
            campo(ventana, "campoCliente").setText(cliente);
            campo(ventana, "campoFechaHora").setText(FORMATO.format(fecha));
            campo(ventana, "campoServicio").setText(servicio);
            campo(ventana, "campoDuracion").setText(duracion);
            boton(ventana, "botonGuardar").doClick();
            verificar(!boton(ventana, "botonGuardar").isEnabled() && !tabla(ventana).isEnabled(),
                    "La escritura en segundo plano bloquea acciones duplicadas");
            return null;
        });
        esperarLista(ventana, dialogos);
    }

    private static Cita localizarPrueba(CitaServicio servicio, String nombre) {
        List<Cita> coincidencias = new ArrayList<>();
        for (Cita cita : servicio.listarTodos()) {
            if (nombre.equals(cita.getCliente())) {
                coincidencias.add(cita);
            }
        }
        verificar(coincidencias.size() == 1, "El alta genera exactamente una cita para el cliente de prueba");
        return coincidencias.get(0);
    }

    private static void esperarLista(VentanaPrincipal ventana, ControlDialogos dialogos) throws Exception {
        esperar(() -> {
            dialogos.verificarSinError();
            return boton(ventana, "botonNuevo").isEnabled();
        });
        enEdt(() -> {
            dialogos.verificarSinError();
            return null;
        });
    }

    private static void esperar(BooleanSupplier condicionEnEdt) throws Exception {
        long limite = System.nanoTime() + ESPERA_MAXIMA_MS * 1000000;
        while (System.nanoTime() < limite) {
            if (enEdt(condicionEnEdt::getAsBoolean)) {
                return;
            }
            Thread.sleep(40);
        }
        throw new AssertionError("Se agotó la espera de " + ESPERA_MAXIMA_MS + " ms del SwingWorker");
    }

    private static <T> T enEdt(Callable<T> tarea) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return tarea.call();
        }
        FutureTask<T> pendiente = new FutureTask<>(tarea);
        SwingUtilities.invokeAndWait(pendiente);
        return pendiente.get();
    }

    private static void seleccionar(VentanaPrincipal ventana, int id) {
        JTable tabla = tabla(ventana);
        int modelo = filaModelo(ventana, id);
        if (modelo < 0) {
            throw new AssertionError("No existe la fila de prueba #" + id);
        }
        int vista = tabla.convertRowIndexToView(modelo);
        tabla.setRowSelectionInterval(vista, vista);
    }

    private static int filaModelo(VentanaPrincipal ventana, int id) {
        ModeloTablaCitas modelo = (ModeloTablaCitas) tabla(ventana).getModel();
        for (int i = 0; i < modelo.getRowCount(); i++) {
            if (modelo.obtenerCita(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }

    private static JTextField campo(VentanaPrincipal ventana, String nombre) {
        return buscar(ventana.getContentPane(), nombre, JTextField.class);
    }

    private static JButton boton(VentanaPrincipal ventana, String nombre) {
        return buscar(ventana.getContentPane(), nombre, JButton.class);
    }

    private static JTable tabla(VentanaPrincipal ventana) {
        return buscar(ventana.getContentPane(), "tablaCitas", JTable.class);
    }

    private static JComboBox<?> combo(VentanaPrincipal ventana) {
        return buscar(ventana.getContentPane(), "comboEstado", JComboBox.class);
    }

    private static <T extends Component> T buscar(Container contenedor, String nombre, Class<T> tipo) {
        for (Component componente : contenedor.getComponents()) {
            if (nombre.equals(componente.getName()) && tipo.isInstance(componente)) {
                return tipo.cast(componente);
            }
            if (componente instanceof Container) {
                T encontrado = buscarOpcional((Container) componente, nombre, tipo);
                if (encontrado != null) {
                    return encontrado;
                }
            }
        }
        throw new AssertionError("No se encontró el componente " + nombre);
    }

    private static <T extends Component> T buscarOpcional(Container contenedor, String nombre, Class<T> tipo) {
        for (Component componente : contenedor.getComponents()) {
            if (nombre.equals(componente.getName()) && tipo.isInstance(componente)) {
                return tipo.cast(componente);
            }
            if (componente instanceof Container) {
                T encontrado = buscarOpcional((Container) componente, nombre, tipo);
                if (encontrado != null) {
                    return encontrado;
                }
            }
        }
        return null;
    }

    private static void verificar(boolean condicion, String detalle) {
        if (!condicion) {
            throw new AssertionError(detalle);
        }
        comprobaciones++;
    }

    /** Responde exclusivamente los diálogos pertenecientes al JFrame de esta prueba. */
    private static final class ControlDialogos {
        private final VentanaPrincipal ventana;
        private final Timer temporizador;
        private String tituloEsperado;
        private String textoEsperado;
        private int respuesta;
        private boolean respondido;
        private AssertionError error;

        ControlDialogos(VentanaPrincipal ventana) {
            this.ventana = ventana;
            this.temporizador = new Timer(35, evento -> revisar());
        }

        void iniciar() {
            temporizador.start();
        }

        void detener() {
            temporizador.stop();
        }

        void esperar(String titulo, String texto, int valor) {
            verificarSinError();
            tituloEsperado = titulo;
            textoEsperado = texto;
            respuesta = valor;
            respondido = false;
        }

        void verificarRespuesta() {
            verificarSinError();
            verificar(respondido, "Se recibió y respondió el diálogo esperado");
        }

        void verificarSinError() {
            if (error != null) {
                throw error;
            }
        }

        private void revisar() {
            for (Window posible : Window.getWindows()) {
                if (!(posible instanceof JDialog) || !posible.isShowing() || !pertenece(posible)) {
                    continue;
                }
                JDialog dialogo = (JDialog) posible;
                JOptionPane panel = encontrarPanel(dialogo);
                if (panel == null) {
                    error = new AssertionError("La app de prueba mostró un diálogo sin JOptionPane");
                    dialogo.dispose();
                    continue;
                }
                String texto = String.valueOf(panel.getMessage());
                boolean esperado = tituloEsperado != null && tituloEsperado.equals(dialogo.getTitle())
                        && texto.contains(textoEsperado);
                if (!esperado) {
                    error = new AssertionError("Diálogo inesperado de la app de prueba: "
                            + dialogo.getTitle() + " — " + texto);
                    panel.setValue(JOptionPane.CLOSED_OPTION);
                } else {
                    respondido = true;
                    tituloEsperado = null;
                    panel.setValue(respuesta);
                }
            }
        }

        private boolean pertenece(Window candidata) {
            Window propietaria = candidata.getOwner();
            while (propietaria != null) {
                if (propietaria == ventana) {
                    return true;
                }
                propietaria = propietaria.getOwner();
            }
            return false;
        }

        private JOptionPane encontrarPanel(Container contenedor) {
            for (Component componente : contenedor.getComponents()) {
                if (componente instanceof JOptionPane) {
                    return (JOptionPane) componente;
                }
                if (componente instanceof Container) {
                    JOptionPane encontrado = encontrarPanel((Container) componente);
                    if (encontrado != null) {
                        return encontrado;
                    }
                }
            }
            return null;
        }
    }
}
