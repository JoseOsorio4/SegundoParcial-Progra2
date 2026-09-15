package edu.umg.programacion2.proyecto.ui;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;

import edu.umg.programacion2.proyecto.excepcion.PersistenciaException;
import edu.umg.programacion2.proyecto.excepcion.ValidacionException;
import edu.umg.programacion2.proyecto.modelo.Cita;
import edu.umg.programacion2.proyecto.modelo.EstadoCita;
import edu.umg.programacion2.proyecto.servicio.CitaServicio;

/**
 * Interfaz principal de la agenda.
 * La ventana solo se encarga de presentación y eventos; las reglas de negocio y
 * el acceso JDBC permanecen en el módulo core.
 */
public final class VentanaPrincipal extends JFrame {
    private static final long serialVersionUID = 1L;

    // Paleta visual
    private static final Color FONDO = new Color(245, 247, 251);
    private static final Color SUPERFICIE = Color.WHITE;
    private static final Color TEXTO = new Color(24, 39, 66);
    private static final Color TEXTO_SUAVE = new Color(103, 116, 137);
    private static final Color BORDE = new Color(222, 229, 239);
    private static final Color PRIMARIO = new Color(45, 91, 213);
    private static final Color PRIMARIO_OSCURO = new Color(31, 67, 165);
    private static final Color AZUL_SUAVE = new Color(235, 241, 255);
    private static final Color VERDE = new Color(29, 132, 96);
    private static final Color VERDE_SUAVE = new Color(232, 248, 241);
    private static final Color AMBAR = new Color(175, 117, 28);
    private static final Color AMBAR_SUAVE = new Color(255, 247, 226);
    private static final Color ROJO = new Color(183, 61, 72);
    private static final Color ROJO_SUAVE = new Color(253, 237, 239);

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter
            .ofPattern("dd/MM/uuuu HH:mm", new Locale("es", "GT"))
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FORMATO_DIA = DateTimeFormatter
            .ofPattern("EEEE d 'de' MMMM 'de' uuuu", new Locale("es", "GT"));

    private final CitaServicio servicio;

    // Los nombres de estos componentes se conservan porque también los usa la prueba Swing.
    private final JTextField campoCliente = new JTextField(25);
    private final JTextField campoFechaHora = new JTextField(20);
    private final JTextField campoServicio = new JTextField(25);
    private final JTextField campoDuracion = new JTextField(10);
    private final JComboBox<EstadoCita> campoEstado = new JComboBox<>(EstadoCita.values());
    private final JButton botonNuevo = new BotonModerno("Nueva cita", TipoBoton.SECUNDARIO);
    private final JButton botonGuardar = new BotonModerno("Guardar cita", TipoBoton.PRIMARIO);
    private final JButton botonActualizar = new BotonModerno("Guardar cambios", TipoBoton.PRIMARIO);
    private final JButton botonEliminar = new BotonModerno("Eliminar", TipoBoton.PELIGRO);
    private final JButton botonRefrescar = new BotonModerno("Refrescar", TipoBoton.SECUNDARIO);

    private final JLabel tituloFormulario = new JLabel("Nueva cita");
    private final JLabel etiquetaModo = new JLabel();
    private final JLabel etiquetaTotal = new JLabel("0 citas");
    private final JLabel etiquetaEstado = new JLabel("Preparando agenda…");

    private final JLabel valorTotal = new JLabel("0");
    private final JLabel valorPendientes = new JLabel("0");
    private final JLabel valorConfirmadas = new JLabel("0");
    private final JLabel valorCanceladas = new JLabel("0");

    private final CampoBusqueda campoBusqueda = new CampoBusqueda("Buscar por cliente, servicio o estado…");
    private final ModeloTablaCitas modeloTabla = new ModeloTablaCitas();
    private final JTable tabla = new JTable(modeloTabla);
    private final TableRowSorter<ModeloTablaCitas> ordenador = new TableRowSorter<>(modeloTabla);

    private Integer idSeleccionado;
    private boolean ocupado;
    private boolean actualizandoSeleccion;

    public VentanaPrincipal(CitaServicio servicio) {
        super("Agenda de citas | Servicios");
        this.servicio = servicio;
        construirInterfaz();
        conectarEventos();
        limpiarFormulario();
        pack();
        setMinimumSize(new Dimension(1040, 680));
        setLocationRelativeTo(null);
        refrescarLista("Agenda lista.");
    }

    private void construirInterfaz() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel contenido = new JPanel(new BorderLayout(0, 16));
        contenido.setBorder(BorderFactory.createEmptyBorder(18, 20, 14, 20));
        contenido.setBackground(FONDO);
        contenido.setPreferredSize(new Dimension(1220, 760));
        setContentPane(contenido);

        contenido.add(construirEncabezado(), BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 16));
        centro.setOpaque(false);
        centro.add(construirResumen(), BorderLayout.NORTH);

        JPanel zonaTrabajo = new JPanel(new BorderLayout(16, 0));
        zonaTrabajo.setOpaque(false);
        JPanel formulario = construirFormulario();
        formulario.setPreferredSize(new Dimension(355, 560));
        zonaTrabajo.add(formulario, BorderLayout.WEST);
        zonaTrabajo.add(construirListado(), BorderLayout.CENTER);
        centro.add(zonaTrabajo, BorderLayout.CENTER);

        contenido.add(centro, BorderLayout.CENTER);
        contenido.add(construirBarraEstado(), BorderLayout.SOUTH);
    }

    private JComponent construirEncabezado() {
        PanelGradiente encabezado = new PanelGradiente();
        encabezado.setLayout(new BorderLayout(20, 0));
        encabezado.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        encabezado.setPreferredSize(new Dimension(0, 92));

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Agenda de citas");
        titulo.setForeground(Color.WHITE);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 27f));
        textos.add(titulo);
        textos.add(Box.createVerticalStrut(5));

        JLabel descripcion = new JLabel("Organiza clientes, servicios y horarios desde un solo lugar.");
        descripcion.setForeground(new Color(225, 234, 255));
        descripcion.setFont(descripcion.getFont().deriveFont(13f));
        textos.add(descripcion);
        encabezado.add(textos, BorderLayout.WEST);

        JPanel derecha = new JPanel();
        derecha.setOpaque(false);
        derecha.setLayout(new BoxLayout(derecha, BoxLayout.Y_AXIS));

        JLabel fecha = new JLabel(capitalizar(FORMATO_DIA.format(LocalDate.now())));
        fecha.setForeground(Color.WHITE);
        fecha.setFont(fecha.getFont().deriveFont(Font.BOLD, 13f));
        fecha.setAlignmentX(Component.RIGHT_ALIGNMENT);
        derecha.add(fecha);
        derecha.add(Box.createVerticalStrut(8));

        JLabel etiqueta = new JLabel("  GESTIÓN DE SERVICIOS  ");
        etiqueta.setOpaque(true);
        etiqueta.setBackground(new Color(255, 255, 255, 40));
        etiqueta.setForeground(Color.WHITE);
        etiqueta.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD, 11f));
        etiqueta.setAlignmentX(Component.RIGHT_ALIGNMENT);
        derecha.add(etiqueta);

        encabezado.add(derecha, BorderLayout.EAST);
        return encabezado;
    }

    private JComponent construirResumen() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 0, 10);

        g.gridx = 0;
        panel.add(crearTarjetaResumen("TOTAL", valorTotal, PRIMARIO, AZUL_SUAVE), g);
        g.gridx = 1;
        panel.add(crearTarjetaResumen("PENDIENTES", valorPendientes, AMBAR, AMBAR_SUAVE), g);
        g.gridx = 2;
        panel.add(crearTarjetaResumen("CONFIRMADAS", valorConfirmadas, VERDE, VERDE_SUAVE), g);
        g.gridx = 3;
        g.insets = new Insets(0, 0, 0, 0);
        panel.add(crearTarjetaResumen("CANCELADAS", valorCanceladas, ROJO, ROJO_SUAVE), g);

        return panel;
    }

    private JPanel crearTarjetaResumen(String titulo, JLabel valor, Color acento, Color fondoIcono) {
        PanelRedondeado tarjeta = new PanelRedondeado(18, SUPERFICIE, true);
        tarjeta.setLayout(new BorderLayout(12, 0));
        tarjeta.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        tarjeta.setPreferredSize(new Dimension(0, 66));

        JPanel icono = new PanelRedondeado(14, fondoIcono, false);
        icono.setPreferredSize(new Dimension(40, 40));
        icono.setLayout(new BorderLayout());
        JLabel punto = new JLabel("●", SwingConstants.CENTER);
        punto.setForeground(acento);
        punto.setFont(punto.getFont().deriveFont(Font.BOLD, 17f));
        icono.add(punto, BorderLayout.CENTER);
        tarjeta.add(icono, BorderLayout.WEST);

        JPanel datos = new JPanel();
        datos.setOpaque(false);
        datos.setLayout(new BoxLayout(datos, BoxLayout.Y_AXIS));
        JLabel nombre = new JLabel(titulo);
        nombre.setForeground(TEXTO_SUAVE);
        nombre.setFont(nombre.getFont().deriveFont(Font.BOLD, 11f));
        alinearEtiqueta(nombre);
        valor.setForeground(TEXTO);
        valor.setFont(valor.getFont().deriveFont(Font.BOLD, 20f));
        alinearEtiqueta(valor);
        datos.add(nombre);
        datos.add(Box.createVerticalStrut(2));
        datos.add(valor);
        tarjeta.add(datos, BorderLayout.CENTER);
        return tarjeta;
    }

    private JPanel construirFormulario() {
        PanelRedondeado tarjeta = new PanelRedondeado(20, SUPERFICIE, true);
        tarjeta.setLayout(new BorderLayout(0, 8));
        tarjeta.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JPanel cabecera = new JPanel();
        cabecera.setOpaque(false);
        cabecera.setLayout(new BoxLayout(cabecera, BoxLayout.Y_AXIS));
        tituloFormulario.setFont(tituloFormulario.getFont().deriveFont(Font.BOLD, 19f));
        tituloFormulario.setForeground(TEXTO);
        alinearEtiqueta(tituloFormulario);
        cabecera.add(tituloFormulario);
        cabecera.add(Box.createVerticalStrut(3));

        JLabel subtitulo = new JLabel("Datos de la cita");
        subtitulo.setForeground(TEXTO_SUAVE);
        subtitulo.setFont(subtitulo.getFont().deriveFont(12f));
        alinearEtiqueta(subtitulo);
        cabecera.add(subtitulo);
        tarjeta.add(cabecera, BorderLayout.NORTH);

        JPanel campos = new PanelCampos();
        campos.setOpaque(false);
        campos.setLayout(new BoxLayout(campos, BoxLayout.Y_AXIS));
        agregarCampoVertical(campos, "Cliente", campoCliente, null);
        agregarCampoVertical(campos, "Servicio", campoServicio, null);
        agregarCampoVertical(campos, "Fecha y hora", campoFechaHora, "Formato: dd/mm/aaaa hh:mm");
        agregarCampoVertical(campos, "Duración (min)", campoDuracion, null);
        agregarCampoVertical(campos, "Estado", campoEstado, null);
        // Al reducir la ventana, los campos conservan su altura y siguen accesibles
        // mediante desplazamiento; los botones permanecen visibles en el pie.
        JScrollPane desplazamiento = new JScrollPane(campos,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        desplazamiento.setBorder(BorderFactory.createEmptyBorder());
        desplazamiento.setOpaque(false);
        desplazamiento.getViewport().setBackground(SUPERFICIE);
        desplazamiento.getVerticalScrollBar().setUnitIncrement(16);
        tarjeta.add(desplazamiento, BorderLayout.CENTER);

        campoFechaHora.setToolTipText("Ejemplo: 19/09/2026 14:30. Las citas nuevas deben ser futuras.");
        campoDuracion.setToolTipText("Número entero de minutos, mayor que cero.");
        campoEstado.setToolTipText("Las citas nuevas comienzan pendientes. Selecciona una cita para cambiar su estado.");
        campoCliente.getAccessibleContext().setAccessibleName("Cliente");
        campoFechaHora.getAccessibleContext().setAccessibleName("Fecha y hora, día mes año y hora de 24 horas");
        campoServicio.getAccessibleContext().setAccessibleName("Servicio");
        campoDuracion.getAccessibleContext().setAccessibleName("Duración en minutos");

        JPanel inferior = new JPanel();
        inferior.setOpaque(false);
        inferior.setLayout(new BoxLayout(inferior, BoxLayout.Y_AXIS));

        etiquetaModo.setForeground(TEXTO_SUAVE);
        etiquetaModo.setFont(etiquetaModo.getFont().deriveFont(11f));
        alinearEtiqueta(etiquetaModo);
        inferior.add(etiquetaModo);
        inferior.add(Box.createVerticalStrut(8));

        for (JButton boton : new JButton[] { botonGuardar, botonActualizar }) {
            boton.setAlignmentX(Component.LEFT_ALIGNMENT);
            boton.setMinimumSize(new Dimension(0, 36));
            boton.setPreferredSize(new Dimension(0, 36));
            boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        }
        inferior.add(botonGuardar);
        inferior.add(botonActualizar);
        inferior.add(Box.createVerticalStrut(7));

        JPanel accionesSecundarias = new JPanel(new GridBagLayout());
        accionesSecundarias.setOpaque(false);
        accionesSecundarias.setAlignmentX(Component.LEFT_ALIGNMENT);
        accionesSecundarias.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 0, 8);
        g.gridx = 0;
        accionesSecundarias.add(botonNuevo, g);
        g.gridx = 1;
        g.insets = new Insets(0, 0, 0, 0);
        accionesSecundarias.add(botonEliminar, g);
        inferior.add(accionesSecundarias);

        tarjeta.add(inferior, BorderLayout.SOUTH);
        return tarjeta;
    }

    private void agregarCampoVertical(JPanel panel, String texto, JComponent campo, String ayuda) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setForeground(TEXTO);
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD, 12f));
        etiqueta.setLabelFor(campo);
        alinearEtiqueta(etiqueta);
        panel.add(etiqueta);
        panel.add(Box.createVerticalStrut(3));

        estilizarCampo(campo);
        campo.setAlignmentX(Component.LEFT_ALIGNMENT);
        campo.setMinimumSize(new Dimension(0, 32));
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        campo.setPreferredSize(new Dimension(0, 32));
        panel.add(campo);

        if (ayuda != null) {
            JLabel ayudaCampo = new JLabel(ayuda);
            ayudaCampo.setForeground(new Color(137, 147, 164));
            ayudaCampo.setFont(ayudaCampo.getFont().deriveFont(11f));
            alinearEtiqueta(ayudaCampo);
            panel.add(Box.createVerticalStrut(2));
            panel.add(ayudaCampo);
        }
        panel.add(Box.createVerticalStrut(5));
    }

    private static void alinearEtiqueta(JLabel etiqueta) {
        etiqueta.setAlignmentX(Component.LEFT_ALIGNMENT);
        int altura = Math.max(etiqueta.getPreferredSize().height,
                etiqueta.getFontMetrics(etiqueta.getFont()).getHeight());
        etiqueta.setMinimumSize(new Dimension(0, altura));
        etiqueta.setMaximumSize(new Dimension(Integer.MAX_VALUE, altura));
    }

    private void estilizarCampo(JComponent campo) {
        campo.setFont(campo.getFont().deriveFont(13f));
        campo.setBackground(Color.WHITE);
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));

        campo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                campo.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(PRIMARIO, 1, true),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)));
                campo.scrollRectToVisible(new Rectangle(0, 0, campo.getWidth(), campo.getHeight()));
            }

            @Override
            public void focusLost(FocusEvent e) {
                campo.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDE, 1, true),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            }
        });
    }

    private JPanel construirListado() {
        PanelRedondeado tarjeta = new PanelRedondeado(20, SUPERFICIE, true);
        tarjeta.setLayout(new BorderLayout(0, 14));
        tarjeta.setBorder(BorderFactory.createEmptyBorder(18, 18, 15, 18));

        JPanel superior = new JPanel(new BorderLayout(12, 10));
        superior.setOpaque(false);

        JPanel tituloPanel = new JPanel();
        tituloPanel.setOpaque(false);
        tituloPanel.setLayout(new BoxLayout(tituloPanel, BoxLayout.Y_AXIS));
        JLabel titulo = new JLabel("Citas registradas");
        titulo.setForeground(TEXTO);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 20f));
        alinearEtiqueta(titulo);
        tituloPanel.add(titulo);
        tituloPanel.add(Box.createVerticalStrut(3));
        etiquetaTotal.setForeground(TEXTO_SUAVE);
        etiquetaTotal.setFont(etiquetaTotal.getFont().deriveFont(12f));
        alinearEtiqueta(etiquetaTotal);
        tituloPanel.add(etiquetaTotal);
        superior.add(tituloPanel, BorderLayout.WEST);

        JPanel herramientas = new JPanel(new BorderLayout(8, 0));
        herramientas.setOpaque(false);
        campoBusqueda.setPreferredSize(new Dimension(265, 36));
        campoBusqueda.setMinimumSize(new Dimension(100, 36));
        campoBusqueda.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE, 1, true),
                BorderFactory.createEmptyBorder(7, 11, 7, 11)));
        herramientas.add(campoBusqueda, BorderLayout.CENTER);
        botonRefrescar.setPreferredSize(new Dimension(105, 36));
        herramientas.add(botonRefrescar, BorderLayout.EAST);
        superior.add(herramientas, BorderLayout.CENTER);
        tarjeta.add(superior, BorderLayout.NORTH);

        configurarTabla();
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(BORDE));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setOpaque(false);
        tarjeta.add(scroll, BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout(0, 4));
        pie.setOpaque(false);
        JLabel ayuda = new JLabel("Selecciona una fila para editarla · Doble clic para ver el detalle completo");
        ayuda.setForeground(TEXTO_SUAVE);
        ayuda.setFont(ayuda.getFont().deriveFont(11f));
        pie.add(ayuda, BorderLayout.CENTER);

        JLabel leyenda = new JLabel("Pendiente  •  Confirmada  •  Cancelada");
        leyenda.setForeground(new Color(126, 136, 152));
        leyenda.setFont(leyenda.getFont().deriveFont(11f));
        pie.add(leyenda, BorderLayout.SOUTH);
        tarjeta.add(pie, BorderLayout.SOUTH);
        return tarjeta;
    }

    private void configurarTabla() {
        tabla.setName("tablaCitas");
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowSorter(ordenador);
        tabla.setRowHeight(38);
        tabla.setFillsViewportHeight(true);
        tabla.setShowHorizontalLines(true);
        tabla.setShowVerticalLines(false);
        tabla.setGridColor(new Color(237, 241, 247));
        tabla.setIntercellSpacing(new Dimension(0, 1));
        tabla.setSelectionBackground(AZUL_SUAVE);
        tabla.setSelectionForeground(TEXTO);
        tabla.setFont(tabla.getFont().deriveFont(13f));
        tabla.setToolTipText("Selecciona una fila para editarla. Haz doble clic para ver el detalle.");

        JTableHeader cabecera = tabla.getTableHeader();
        cabecera.setReorderingAllowed(false);
        cabecera.setBackground(new Color(248, 250, 253));
        cabecera.setForeground(new Color(69, 82, 103));
        cabecera.setFont(cabecera.getFont().deriveFont(Font.BOLD, 12f));
        cabecera.setPreferredSize(new Dimension(0, 39));
        cabecera.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDE));

        int[] anchos = { 55, 190, 155, 220, 90, 115 };
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        tabla.getColumnModel().getColumn(0).setMaxWidth(75);
        tabla.setDefaultRenderer(Object.class, new CeldaRenderer());
        tabla.setDefaultRenderer(String.class, new CeldaRenderer());
        tabla.setDefaultRenderer(Integer.class, new CeldaRenderer());
        tabla.setDefaultRenderer(LocalDateTime.class, new FechaRenderer());
        tabla.getColumnModel().getColumn(4).setCellRenderer(new DuracionRenderer());
        tabla.setDefaultRenderer(EstadoCita.class, new EstadoRenderer());
    }

    private JComponent construirBarraEstado() {
        PanelRedondeado barra = new PanelRedondeado(14, new Color(238, 242, 248), false);
        barra.setLayout(new BorderLayout(8, 0));
        barra.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        barra.setPreferredSize(new Dimension(0, 34));

        JLabel punto = new JLabel("●");
        punto.setForeground(VERDE);
        punto.setFont(punto.getFont().deriveFont(10f));
        barra.add(punto, BorderLayout.WEST);

        etiquetaEstado.setForeground(TEXTO_SUAVE);
        etiquetaEstado.setFont(etiquetaEstado.getFont().deriveFont(12f));
        barra.add(etiquetaEstado, BorderLayout.CENTER);
        return barra;
    }

    private void conectarEventos() {
        campoCliente.setName("campoCliente");
        campoFechaHora.setName("campoFechaHora");
        campoServicio.setName("campoServicio");
        campoDuracion.setName("campoDuracion");
        campoEstado.setName("comboEstado");
        tabla.setName("tablaCitas");
        botonNuevo.setName("botonNuevo");
        botonGuardar.setName("botonGuardar");
        botonActualizar.setName("botonActualizar");
        botonEliminar.setName("botonEliminar");
        botonRefrescar.setName("botonRefrescar");

        botonNuevo.addActionListener(evento -> {
            limpiarFormulario();
            etiquetaEstado.setText("Formulario listo para registrar una cita nueva.");
            campoCliente.requestFocusInWindow();
        });
        botonGuardar.addActionListener(evento -> guardar());
        botonActualizar.addActionListener(evento -> actualizar());
        botonEliminar.addActionListener(evento -> eliminar());
        botonRefrescar.addActionListener(evento -> refrescarLista("Listado actualizado."));

        tabla.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting() && !actualizandoSeleccion && !ocupado) {
                cargarSeleccion();
            }
        });

        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabla.getSelectedRow() >= 0 && !ocupado) {
                    mostrarDetalleSeleccionado();
                }
            }
        });

        campoBusqueda.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { aplicarFiltro(); }
            @Override public void removeUpdate(DocumentEvent e) { aplicarFiltro(); }
            @Override public void changedUpdate(DocumentEvent e) { aplicarFiltro(); }
        });
    }

    private void aplicarFiltro() {
        String texto = campoBusqueda.getText().trim();
        if (texto.isEmpty()) {
            ordenador.setRowFilter(null);
        } else {
            ordenador.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(texto)));
        }
    }

    private void cargarSeleccion() {
        int filaVista = tabla.getSelectedRow();
        if (filaVista < 0) {
            limpiarFormulario();
            return;
        }
        int filaModelo = tabla.convertRowIndexToModel(filaVista);
        Cita cita = modeloTabla.obtenerCita(filaModelo);
        idSeleccionado = cita.getId();
        campoCliente.setText(cita.getCliente());
        campoFechaHora.setText(FORMATO_FECHA.format(cita.getFechaHora()));
        campoServicio.setText(cita.getServicio());
        campoDuracion.setText(Integer.toString(cita.getDuracionMinutos()));
        campoEstado.setSelectedItem(cita.getEstado());
        tituloFormulario.setText("Editar cita #" + cita.getId());
        etiquetaModo.setText("Editando cita #" + cita.getId() + " · Puedes cambiar su estado.");
        etiquetaEstado.setText("Cita #" + cita.getId() + " seleccionada.");
        actualizarControles();
    }

    private void limpiarFormulario() {
        actualizandoSeleccion = true;
        try {
            tabla.clearSelection();
        } finally {
            actualizandoSeleccion = false;
        }
        idSeleccionado = null;
        campoCliente.setText("");
        campoFechaHora.setText(FORMATO_FECHA.format(LocalDateTime.now().plusDays(1).withSecond(0).withNano(0)));
        campoServicio.setText("");
        campoDuracion.setText("30");
        campoEstado.setSelectedItem(EstadoCita.PENDIENTE);
        tituloFormulario.setText("Nueva cita");
        etiquetaModo.setText("Nueva cita · Estado inicial: Pendiente");
        actualizarControles();
    }

    private Cita leerFormulario(boolean nueva) {
        if (campoCliente.getText().trim().isEmpty()) {
            campoCliente.requestFocusInWindow();
            throw new IllegalArgumentException("Escribe el nombre del cliente.");
        }
        if (campoServicio.getText().trim().isEmpty()) {
            campoServicio.requestFocusInWindow();
            throw new IllegalArgumentException("Escribe el servicio de la cita.");
        }

        LocalDateTime fecha;
        try {
            fecha = LocalDateTime.parse(campoFechaHora.getText().trim(), FORMATO_FECHA);
        } catch (DateTimeParseException ex) {
            campoFechaHora.requestFocusInWindow();
            throw new IllegalArgumentException("Escribe una fecha y hora válidas con el formato dd/MM/aaaa HH:mm.\n"
                    + "Ejemplo: 19/09/2026 14:30 (hora de 24 horas).");
        }

        int duracion;
        try {
            duracion = Integer.parseInt(campoDuracion.getText().trim());
            if (duracion <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            campoDuracion.requestFocusInWindow();
            throw new IllegalArgumentException("La duración debe ser un número entero de minutos mayor que cero.");
        }

        return new Cita(nueva ? 0 : idSeleccionado,
                campoCliente.getText().trim(), fecha, campoServicio.getText().trim(), duracion,
                nueva ? EstadoCita.PENDIENTE : (EstadoCita) campoEstado.getSelectedItem());
    }

    private void guardar() {
        if (ocupado || idSeleccionado != null) {
            return;
        }
        final Cita cita;
        try {
            cita = leerFormulario(true);
        } catch (IllegalArgumentException ex) {
            mostrarValidacion(ex.getMessage());
            return;
        }
        ejecutar("Guardando cita…", () -> servicio.crear(cita), creada -> {
            limpiarTrasCambio();
            String mensaje = "Cita #" + creada.getId() + " guardada correctamente.";
            refrescarLista(mensaje, mensaje);
        });
    }

    private void actualizar() {
        if (ocupado || idSeleccionado == null) {
            return;
        }
        final Cita cita;
        try {
            cita = leerFormulario(false);
        } catch (IllegalArgumentException ex) {
            mostrarValidacion(ex.getMessage());
            return;
        }
        ejecutar("Actualizando cita…", () -> servicio.actualizar(cita), actualizada -> {
            limpiarTrasCambio();
            if (actualizada) {
                String mensaje = "Cita #" + cita.getId() + " actualizada correctamente.";
                refrescarLista(mensaje, mensaje);
            } else {
                mostrarRegistroAusente();
                refrescarLista("La cita ya no estaba disponible. Listado actualizado.");
            }
        });
    }

    private void eliminar() {
        if (ocupado || idSeleccionado == null) {
            return;
        }
        final int id = idSeleccionado;
        int respuesta = JOptionPane.showConfirmDialog(this,
                "¿Eliminar definitivamente la cita #" + id + "?\n"
                        + "Esta acción borra el registro. Si solo deseas cancelar, cambia su estado a Cancelada.",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }
        ejecutar("Eliminando cita…", () -> servicio.eliminar(id), eliminada -> {
            limpiarTrasCambio();
            if (eliminada) {
                String mensaje = "Cita #" + id + " eliminada correctamente.";
                refrescarLista(mensaje, mensaje);
            } else {
                mostrarRegistroAusente();
                refrescarLista("La cita ya no estaba disponible. Listado actualizado.");
            }
        });
    }

    private void mostrarDetalleSeleccionado() {
        int filaVista = tabla.getSelectedRow();
        if (filaVista < 0) {
            return;
        }
        Cita cita = modeloTabla.obtenerCita(tabla.convertRowIndexToModel(filaVista));
        String detalle = "<html><div style='width:330px;padding:6px'>"
                + "<h2 style='color:#18304e;margin-bottom:10px'>Cita #" + cita.getId() + "</h2>"
                + "<b>Cliente:</b> " + escaparHtml(cita.getCliente()) + "<br><br>"
                + "<b>Servicio:</b> " + escaparHtml(cita.getServicio()) + "<br><br>"
                + "<b>Fecha y hora:</b> " + FORMATO_FECHA.format(cita.getFechaHora()) + "<br><br>"
                + "<b>Duración:</b> " + cita.getDuracionMinutos() + " minutos<br><br>"
                + "<b>Estado:</b> " + cita.getEstado() + "</div></html>";
        JOptionPane.showMessageDialog(this, detalle, "Detalle de la cita", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarRegistroAusente() {
        JOptionPane.showMessageDialog(this,
                "La cita seleccionada ya no existe. Se volverá a consultar el listado.",
                "Cita no disponible", JOptionPane.INFORMATION_MESSAGE);
    }

    private void limpiarTrasCambio() {
        limpiarFormulario();
        modeloTabla.reemplazar(Collections.emptyList());
        actualizarResumen(Collections.emptyList());
        etiquetaTotal.setText("Actualizando…");
    }

    private void refrescarLista(String mensajeExito) {
        refrescarLista(mensajeExito, null);
    }

    private void refrescarLista(String mensajeExito, String cambioConfirmado) {
        ejecutar("Consultando citas…", servicio::listarTodos, citas -> {
            limpiarFormulario();
            modeloTabla.reemplazar(citas);
            actualizarResumen(citas);
            etiquetaTotal.setText(citas.size() + (citas.size() == 1 ? " cita registrada" : " citas registradas"));
            etiquetaEstado.setText(mensajeExito + (citas.isEmpty() ? " No hay citas registradas." : ""));
        }, (cambioConfirmado == null ? "" : cambioConfirmado + "\n\n") + "No se pudo actualizar el listado."
                + "\nComprueba la conexión y pulsa Refrescar para volver a consultar.");
    }

    private void actualizarResumen(List<Cita> citas) {
        int pendientes = 0;
        int confirmadas = 0;
        int canceladas = 0;
        for (Cita cita : citas) {
            if (cita.getEstado() == EstadoCita.CONFIRMADA) {
                confirmadas++;
            } else if (cita.getEstado() == EstadoCita.CANCELADA) {
                canceladas++;
            } else {
                pendientes++;
            }
        }
        valorTotal.setText(Integer.toString(citas.size()));
        valorPendientes.setText(Integer.toString(pendientes));
        valorConfirmadas.setText(Integer.toString(confirmadas));
        valorCanceladas.setText(Integer.toString(canceladas));
    }

    /** Ejecuta I/O fuera del hilo gráfico y aplica los resultados únicamente en el EDT. */
    private <T> void ejecutar(String mensaje, Callable<T> operacion, Consumer<T> alCompletar) {
        ejecutar(mensaje, operacion, alCompletar, null);
    }

    private <T> void ejecutar(String mensaje, Callable<T> operacion,
            Consumer<T> alCompletar, String contextoError) {
        if (ocupado) {
            return;
        }
        establecerOcupado(true);
        etiquetaEstado.setText(mensaje);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return operacion.call();
            }

            @Override
            protected void done() {
                T resultado;
                try {
                    resultado = get();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    establecerOcupado(false);
                    mostrarError("La operación fue interrumpida. Pulsa Refrescar antes de intentarlo de nuevo.");
                    return;
                } catch (ExecutionException ex) {
                    establecerOcupado(false);
                    Throwable causa = ex.getCause();
                    if (causa instanceof ValidacionException) {
                        mostrarValidacion(causa.getMessage());
                    } else {
                        String detalle = causa instanceof PersistenciaException
                                ? causa.getMessage() : "No se pudo completar la operación. Inténtalo de nuevo.";
                        mostrarError(contextoError == null ? detalle : contextoError + "\n\n" + detalle);
                    }
                    return;
                }
                establecerOcupado(false);
                alCompletar.accept(resultado);
            }
        }.execute();
    }

    private void establecerOcupado(boolean nuevoEstado) {
        ocupado = nuevoEstado;
        setCursor(Cursor.getPredefinedCursor(ocupado ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
        actualizarControles();
    }

    private void actualizarControles() {
        boolean editando = idSeleccionado != null;
        for (JTextField campo : new JTextField[] { campoCliente, campoFechaHora, campoServicio, campoDuracion }) {
            campo.setEnabled(!ocupado);
        }
        campoEstado.setEnabled(!ocupado && editando);
        campoBusqueda.setEnabled(!ocupado);
        tabla.setEnabled(!ocupado);
        tabla.getTableHeader().setEnabled(!ocupado);
        botonNuevo.setEnabled(!ocupado);
        botonGuardar.setEnabled(!ocupado && !editando);
        botonActualizar.setEnabled(!ocupado && editando);
        botonEliminar.setEnabled(!ocupado && editando);
        botonRefrescar.setEnabled(!ocupado);

        // Solo se muestra la acción principal correspondiente al modo actual.
        botonGuardar.setVisible(!editando);
        botonActualizar.setVisible(editando);
        getRootPane().setDefaultButton(editando ? botonActualizar : botonGuardar);
    }

    private void mostrarValidacion(String mensaje) {
        etiquetaEstado.setText("Revisa los datos del formulario.");
        JOptionPane.showMessageDialog(this, mensaje, "Datos de la cita", JOptionPane.WARNING_MESSAGE);
    }

    private void mostrarError(String mensaje) {
        etiquetaEstado.setText("No se pudo completar la operación. Revisa el mensaje e intenta nuevamente.");
        if (modeloTabla.getRowCount() == 0) {
            etiquetaTotal.setText("Sin cargar");
        }
        JOptionPane.showMessageDialog(this, mensaje, "No se pudo completar", JOptionPane.ERROR_MESSAGE);
    }

    private static String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private static String escaparHtml(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static final class CeldaRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            if (!isSelected) {
                setBackground(row % 2 == 0 ? Color.WHITE : new Color(251, 252, 254));
                setForeground(TEXTO);
            }
            return this;
        }
    }

    private static final class FechaRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setText(value == null ? "" : FORMATO_FECHA.format((LocalDateTime) value));
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            if (!isSelected) {
                setBackground(row % 2 == 0 ? Color.WHITE : new Color(251, 252, 254));
                setForeground(TEXTO);
            }
            return this;
        }
    }

    private static final class DuracionRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);
            setText(value == null ? "" : value + " min");
            setBorder(BorderFactory.createEmptyBorder(0, 7, 0, 7));
            if (!isSelected) {
                setBackground(row % 2 == 0 ? Color.WHITE : new Color(251, 252, 254));
                setForeground(TEXTO);
            }
            return this;
        }
    }

    private static final class EstadoRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(getFont().deriveFont(Font.BOLD, 11f));
            setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
            if (!isSelected) {
                if (value == EstadoCita.CONFIRMADA) {
                    setBackground(VERDE_SUAVE);
                    setForeground(VERDE);
                } else if (value == EstadoCita.CANCELADA) {
                    setBackground(ROJO_SUAVE);
                    setForeground(ROJO);
                } else {
                    setBackground(AMBAR_SUAVE);
                    setForeground(AMBAR);
                }
            }
            return this;
        }
    }

    private enum TipoBoton { PRIMARIO, SECUNDARIO, PELIGRO }

    /** Mantiene el ancho del formulario dentro del viewport y permite desplazar su altura. */
    private static final class PanelCampos extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(16, visibleRect.height - 16);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getParent().getHeight() > getPreferredSize().height;
        }
    }

    private static final class BotonModerno extends JButton {
        private static final long serialVersionUID = 1L;
        private final TipoBoton tipo;

        BotonModerno(String texto, TipoBoton tipo) {
            super(texto);
            this.tipo = tipo;
            setFont(getFont().deriveFont(Font.BOLD, 12f));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMargin(new Insets(9, 14, 9, 14));
            setPreferredSize(new Dimension(120, 38));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fondo;
            Color texto;
            if (!isEnabled()) {
                fondo = new Color(236, 239, 244);
                texto = new Color(158, 167, 181);
            } else if (tipo == TipoBoton.PRIMARIO) {
                fondo = getModel().isPressed() ? PRIMARIO_OSCURO
                        : (getModel().isRollover() ? new Color(57, 103, 224) : PRIMARIO);
                texto = Color.WHITE;
            } else if (tipo == TipoBoton.PELIGRO) {
                fondo = getModel().isRollover() ? new Color(249, 226, 229) : ROJO_SUAVE;
                texto = ROJO;
            } else {
                fondo = getModel().isRollover() ? new Color(238, 243, 252) : new Color(247, 249, 252);
                texto = TEXTO;
            }
            g2.setColor(fondo);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            if (tipo != TipoBoton.PRIMARIO && isEnabled()) {
                g2.setColor(BORDE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            }
            g2.dispose();
            setForeground(texto);
            super.paintComponent(g);
        }
    }

    private static final class PanelGradiente extends JPanel {
        private static final long serialVersionUID = 1L;

        PanelGradiente() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, new Color(27, 55, 109), getWidth(), getHeight(), PRIMARIO));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class PanelRedondeado extends JPanel {
        private static final long serialVersionUID = 1L;
        private final int radio;
        private final Color fondo;
        private final boolean sombra;

        PanelRedondeado(int radio, Color fondo, boolean sombra) {
            this.radio = radio;
            this.fondo = fondo;
            this.sombra = sombra;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (sombra) {
                g2.setComposite(AlphaComposite.SrcOver.derive(0.07f));
                g2.setColor(new Color(24, 39, 66));
                g2.fillRoundRect(1, 3, Math.max(0, getWidth() - 2), Math.max(0, getHeight() - 3), radio, radio);
                g2.setComposite(AlphaComposite.SrcOver);
            }
            g2.setColor(fondo);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - (sombra ? 3 : 1), radio, radio);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class CampoBusqueda extends JTextField {
        private static final long serialVersionUID = 1L;
        private final String placeholder;

        CampoBusqueda(String placeholder) {
            this.placeholder = placeholder;
            setFont(getFont().deriveFont(12f));
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(146, 156, 171));
                g2.setFont(getFont());
                g2.drawString(placeholder, getInsets().left, getHeight() / 2 + getFontMetrics(getFont()).getAscent() / 2 - 2);
                g2.dispose();
            }
        }
    }
}
