package edu.umg.programacion2.proyecto.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

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
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import edu.umg.programacion2.proyecto.excepcion.PersistenciaException;
import edu.umg.programacion2.proyecto.excepcion.ValidacionException;
import edu.umg.programacion2.proyecto.modelo.Cita;
import edu.umg.programacion2.proyecto.modelo.EstadoCita;
import edu.umg.programacion2.proyecto.servicio.CitaServicio;

/** Formulario y listado. Las reglas de negocio y SQL pertenecen al módulo core. */
public final class VentanaPrincipal extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter
            .ofPattern("dd/MM/uuuu HH:mm", new Locale("es", "GT"))
            .withResolverStyle(ResolverStyle.STRICT);

    private final CitaServicio servicio;
    private final JTextField campoCliente = new JTextField(25);
    private final JTextField campoFechaHora = new JTextField(20);
    private final JTextField campoServicio = new JTextField(25);
    private final JTextField campoDuracion = new JTextField(10);
    private final JComboBox<EstadoCita> campoEstado = new JComboBox<>(EstadoCita.values());
    private final JButton botonNuevo = new Estilos.Boton("Nuevo / Limpiar", IconoVector.Tipo.MAS,
            Estilos.SECUNDARIO, Estilos.SECUNDARIO, Estilos.SECUNDARIO, false);
    private final JButton botonGuardar = new Estilos.Boton("Guardar cita", IconoVector.Tipo.CHECK,
            Estilos.PRIMARIO, Estilos.PRIMARIO_HOVER, Color.WHITE, true);
    private final JButton botonActualizar = new Estilos.Boton("Actualizar", IconoVector.Tipo.CHECK,
            Estilos.PRIMARIO, Estilos.PRIMARIO_HOVER, Color.WHITE, true);
    private final JButton botonEliminar = new Estilos.Boton("Eliminar", IconoVector.Tipo.PAPELERA,
            Estilos.PELIGRO, Estilos.PELIGRO_HOVER, Color.WHITE, true);
    private final JButton botonRefrescar = new Estilos.Boton("Refrescar", IconoVector.Tipo.REFRESCAR,
            Estilos.PRIMARIO, Estilos.PRIMARIO, Estilos.PRIMARIO, false);
    private final JLabel tituloFormulario = new JLabel("Nueva cita");
    private final JLabel etiquetaModo = new JLabel();
    private final JLabel etiquetaTotal = new JLabel("0 citas");
    private final JLabel etiquetaEstado = new JLabel("Preparando agenda…");
    private final ModeloTablaCitas modeloTabla = new ModeloTablaCitas();
    private final JTable tabla = new JTable(modeloTabla) {
        private static final long serialVersionUID = 1L;

        @Override
        public Component prepareRenderer(TableCellRenderer renderer, int fila, int columna) {
            Component componente = super.prepareRenderer(renderer, fila, columna);
            if (isRowSelected(fila)) {
                componente.setBackground(Estilos.PRIMARIO_CLARO);
            } else {
                componente.setBackground(fila % 2 == 0 ? Color.WHITE : new Color(248, 250, 253));
            }
            return componente;
        }
    };
    private final ChipEstadistica chipPendientes = new ChipEstadistica("Pendientes", new Color(251, 191, 36));
    private final ChipEstadistica chipConfirmadas = new ChipEstadistica("Confirmadas", new Color(74, 222, 128));
    private final ChipEstadistica chipCanceladas = new ChipEstadistica("Canceladas", new Color(248, 113, 113));

    private Integer idSeleccionado;
    private boolean ocupado;
    private boolean actualizandoSeleccion;
    private Point ubicacionFinal;

    public VentanaPrincipal(CitaServicio servicio) {
        super("Agenda de citas | Servicios");
        this.servicio = Objects.requireNonNull(servicio, "El servicio de citas no puede ser null");
        construirInterfaz();
        conectarEventos();
        limpiarFormulario();
        pack();
        setMinimumSize(new Dimension(940, 660));
        setLocationRelativeTo(null);
        prepararAnimacionApertura();
        refrescarLista("Agenda lista.");
    }

    // ------------------------------------------------------------------
    // Construcción de la interfaz (capa visual)
    // ------------------------------------------------------------------

    private void construirInterfaz() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel contenido = new JPanel(new BorderLayout(0, 16));
        contenido.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        contenido.setBackground(Estilos.FONDO);
        contenido.setPreferredSize(new Dimension(1080, 740));
        setContentPane(contenido);

        JPanel superior = new JPanel();
        superior.setOpaque(false);
        superior.setLayout(new BoxLayout(superior, BoxLayout.Y_AXIS));
        JPanel encabezado = construirEncabezado();
        encabezado.setAlignmentX(Component.LEFT_ALIGNMENT);
        superior.add(encabezado);
        superior.add(Box.createVerticalStrut(16));
        JPanel formulario = construirFormulario();
        formulario.setAlignmentX(Component.LEFT_ALIGNMENT);
        superior.add(formulario);
        contenido.add(superior, BorderLayout.NORTH);
        contenido.add(construirListado(), BorderLayout.CENTER);

        JPanel barraEstado = new JPanel(new BorderLayout());
        barraEstado.setOpaque(false);
        barraEstado.setBorder(BorderFactory.createEmptyBorder(8, 4, 0, 4));
        etiquetaEstado.setForeground(Estilos.SECUNDARIO);
        etiquetaEstado.setFont(etiquetaEstado.getFont().deriveFont(12.5f));
        barraEstado.add(etiquetaEstado, BorderLayout.WEST);
        contenido.add(barraEstado, BorderLayout.SOUTH);
    }

    private JPanel construirEncabezado() {
        JPanel panel = new JPanel(new BorderLayout(20, 0)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint degradado = new GradientPaint(0, 0, Estilos.PRIMARIO, getWidth(), getHeight(),
                        Estilos.ACENTO);
                g2.setPaint(degradado);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));

        JPanel izquierda = new JPanel();
        izquierda.setOpaque(false);
        izquierda.setLayout(new BoxLayout(izquierda, BoxLayout.Y_AXIS));
        JLabel titulo = new JLabel("Agenda de citas", new Estilos.IconoSwing(IconoVector.Tipo.CALENDARIO, 24,
                Color.WHITE), SwingConstants.LEFT);
        titulo.setIconTextGap(12);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 26f));
        titulo.setForeground(Color.WHITE);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel descripcion = new JLabel("Registra servicios, organiza horarios y controla el estado de cada cita.");
        descripcion.setForeground(new Color(232, 232, 250));
        descripcion.setBorder(BorderFactory.createEmptyBorder(8, 2, 0, 0));
        descripcion.setAlignmentX(Component.LEFT_ALIGNMENT);
        izquierda.add(titulo);
        izquierda.add(descripcion);
        panel.add(izquierda, BorderLayout.WEST);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        derecha.setOpaque(false);
        derecha.add(chipPendientes);
        derecha.add(chipConfirmadas);
        derecha.add(chipCanceladas);
        panel.add(derecha, BorderLayout.EAST);

        return panel;
    }

    private JPanel construirFormulario() {
        JPanel tarjeta = crearTarjeta();
        tarjeta.setLayout(new BorderLayout(0, 14));
        tituloFormulario.setFont(tituloFormulario.getFont().deriveFont(Font.BOLD, 18f));
        tituloFormulario.setForeground(Estilos.TEXTO);
        tarjeta.add(tituloFormulario, BorderLayout.NORTH);

        JPanel campos = new JPanel(new GridBagLayout());
        campos.setOpaque(false);
        Estilos.campo(campoCliente);
        Estilos.campo(campoFechaHora);
        Estilos.campo(campoServicio);
        Estilos.campo(campoDuracion);
        Estilos.combo(campoEstado);
        agregarCampo(campos, "Cliente", IconoVector.Tipo.USUARIO, campoCliente, 0, 0);
        agregarCampo(campos, "Fecha y hora", IconoVector.Tipo.RELOJ, campoFechaHora, 2, 0);
        agregarCampo(campos, "Servicio", IconoVector.Tipo.ETIQUETA, campoServicio, 0, 1);
        agregarCampo(campos, "Duración (min)", null, campoDuracion, 2, 1);
        agregarCampo(campos, "Estado", null, campoEstado, 0, 2);

        GridBagConstraints ayuda = new GridBagConstraints();
        ayuda.gridx = 2;
        ayuda.gridy = 2;
        ayuda.gridwidth = 2;
        ayuda.anchor = GridBagConstraints.WEST;
        ayuda.insets = new Insets(7, 20, 7, 0);
        JLabel formato = new JLabel("Formato: día/mes/año hora:minuto en 24 horas");
        formato.setForeground(Estilos.SECUNDARIO);
        formato.setFont(formato.getFont().deriveFont(12f));
        campos.add(formato, ayuda);
        tarjeta.add(campos, BorderLayout.CENTER);

        campoFechaHora.setToolTipText("Ejemplo: 19/09/2026 14:30. Las citas nuevas deben ser futuras.");
        campoDuracion.setToolTipText("Número entero de minutos, mayor que cero.");
        campoEstado.setToolTipText("Las citas nuevas comienzan pendientes. Selecciona una cita para cambiar su estado.");
        campoCliente.getAccessibleContext().setAccessibleName("Cliente");
        campoFechaHora.getAccessibleContext().setAccessibleName("Fecha y hora, día mes año y hora de 24 horas");
        campoServicio.getAccessibleContext().setAccessibleName("Servicio");
        campoDuracion.getAccessibleContext().setAccessibleName("Duración en minutos");
        campoEstado.getAccessibleContext().setAccessibleName("Estado de la cita");

        JPanel inferior = new JPanel(new BorderLayout(0, 10));
        inferior.setOpaque(false);
        etiquetaModo.setForeground(Estilos.SECUNDARIO);
        etiquetaModo.setFont(etiquetaModo.getFont().deriveFont(12.5f));
        inferior.add(etiquetaModo, BorderLayout.NORTH);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        acciones.setOpaque(false);
        acciones.add(botonNuevo);
        acciones.add(botonGuardar);
        acciones.add(botonActualizar);
        acciones.add(botonEliminar);
        inferior.add(acciones, BorderLayout.SOUTH);
        tarjeta.add(inferior, BorderLayout.SOUTH);
        return tarjeta;
    }

    private void agregarCampo(JPanel panel, String texto, IconoVector.Tipo icono, JComponent campo, int columna,
            int fila) {
        JLabel etiqueta = icono == null ? new JLabel(texto)
                : new JLabel(texto, new Estilos.IconoSwing(icono, 13, Estilos.SECUNDARIO), SwingConstants.LEFT);
        if (icono != null) {
            etiqueta.setIconTextGap(6);
        }
        etiqueta.setForeground(Estilos.TEXTO);
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD, 12.5f));
        etiqueta.setLabelFor(campo);
        GridBagConstraints restriccion = new GridBagConstraints();
        restriccion.gridx = columna;
        restriccion.gridy = fila;
        restriccion.anchor = GridBagConstraints.WEST;
        restriccion.insets = new Insets(9, columna == 0 ? 0 : 24, 6, 12);
        panel.add(etiqueta, restriccion);

        restriccion.gridx = columna + 1;
        restriccion.weightx = 1;
        restriccion.fill = GridBagConstraints.HORIZONTAL;
        restriccion.insets = new Insets(9, 0, 6, 0);
        campo.setPreferredSize(new Dimension(campo.getPreferredSize().width, 32));
        panel.add(campo, restriccion);
    }

    private JPanel construirListado() {
        JPanel tarjeta = crearTarjeta();
        tarjeta.setLayout(new BorderLayout(0, 12));
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);
        JLabel titulo = new JLabel("Citas registradas");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));
        titulo.setForeground(Estilos.TEXTO);
        cabecera.add(titulo, BorderLayout.WEST);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        acciones.setOpaque(false);
        etiquetaTotal.setForeground(Estilos.SECUNDARIO);
        etiquetaTotal.setFont(etiquetaTotal.getFont().deriveFont(12.5f));
        acciones.add(etiquetaTotal);
        acciones.add(botonRefrescar);
        cabecera.add(acciones, BorderLayout.EAST);
        tarjeta.add(cabecera, BorderLayout.NORTH);

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(true);
        tabla.setRowHeight(34);
        tabla.setFillsViewportHeight(true);
        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(false);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.setSelectionBackground(Estilos.PRIMARIO_CLARO);
        tabla.setSelectionForeground(Estilos.TEXTO);
        tabla.setFont(tabla.getFont().deriveFont(13.5f));
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setFont(tabla.getFont().deriveFont(Font.BOLD, 12.5f));
        tabla.getTableHeader().setForeground(Estilos.SECUNDARIO);
        tabla.getTableHeader().setBackground(new Color(247, 249, 252));
        tabla.getTableHeader().setPreferredSize(new Dimension(0, 38));
        tabla.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Estilos.BORDE));
        tabla.setToolTipText("Un clic para editar. Doble clic para ver el detalle completo.");
        tabla.getAccessibleContext().setAccessibleName("Listado de citas registradas");
        int[] anchos = { 48, 205, 165, 220, 90, 130 };
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        tabla.getColumnModel().getColumn(0).setMaxWidth(75);
        tabla.setDefaultRenderer(LocalDateTime.class, new FechaRenderer());
        tabla.getColumnModel().getColumn(4).setCellRenderer(new DuracionRenderer());
        tabla.setDefaultRenderer(EstadoCita.class, new EstadoRenderer());
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Estilos.BORDE));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        tarjeta.add(scroll, BorderLayout.CENTER);
        JLabel ayuda = new JLabel(
                "Un clic para editar · doble clic para ver el detalle · Cancelar conserva la cita, Eliminar la borra.");
        ayuda.setForeground(Estilos.SECUNDARIO);
        ayuda.setFont(ayuda.getFont().deriveFont(11.5f));
        ayuda.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 0));
        tarjeta.add(ayuda, BorderLayout.SOUTH);
        return tarjeta;
    }

    private JPanel crearTarjeta() {
        Estilos.Tarjeta tarjeta = new Estilos.Tarjeta();
        tarjeta.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 24));
        return tarjeta;
    }

    // ------------------------------------------------------------------
    // Eventos
    // ------------------------------------------------------------------

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
            public void mouseClicked(MouseEvent evento) {
                if (evento.getClickCount() == 2 && !ocupado) {
                    int filaVista = tabla.getSelectedRow();
                    if (filaVista >= 0) {
                        int filaModelo = tabla.convertRowIndexToModel(filaVista);
                        mostrarDetalle(modeloTabla.obtenerCita(filaModelo));
                    }
                }
            }
        });
    }

    private void mostrarDetalle(Cita cita) {
        DetalleCitaDialog dialogo = new DetalleCitaDialog(this, cita, FORMATO_FECHA, this::cargarCitaEnFormulario);
        dialogo.mostrarConAnimacion();
    }

    private void cargarSeleccion() {
        int filaVista = tabla.getSelectedRow();
        if (filaVista < 0) {
            limpiarFormulario();
            return;
        }
        int filaModelo = tabla.convertRowIndexToModel(filaVista);
        cargarCitaEnFormulario(modeloTabla.obtenerCita(filaModelo));
    }

    private void cargarCitaEnFormulario(Cita cita) {
        actualizandoSeleccion = true;
        try {
            for (int fila = 0; fila < tabla.getRowCount(); fila++) {
                int filaModelo = tabla.convertRowIndexToModel(fila);
                if (Objects.equals(modeloTabla.obtenerCita(filaModelo).getId(), cita.getId())) {
                    tabla.setRowSelectionInterval(fila, fila);
                    tabla.scrollRectToVisible(tabla.getCellRect(fila, 0, true));
                    break;
                }
            }
        } finally {
            actualizandoSeleccion = false;
        }
        idSeleccionado = cita.getId();
        campoCliente.setText(cita.getCliente());
        campoFechaHora.setText(FORMATO_FECHA.format(cita.getFechaHora()));
        campoServicio.setText(cita.getServicio());
        campoDuracion.setText(Integer.toString(cita.getDuracionMinutos()));
        campoEstado.setSelectedItem(cita.getEstado());
        tituloFormulario.setText("Editar cita #" + cita.getId());
        etiquetaModo.setText("Modifica los datos y pulsa Actualizar. Para cancelar, elige el estado Cancelada.");
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
        etiquetaModo.setText("Completa todos los campos. Las citas nuevas se guardan en estado Pendiente.");
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

        EstadoCita estado = nueva ? EstadoCita.PENDIENTE : (EstadoCita) campoEstado.getSelectedItem();
        if (estado == null) {
            estado = EstadoCita.PENDIENTE;
        }

        return new Cita(nueva ? 0 : idSeleccionado,
                campoCliente.getText().trim(), fecha, campoServicio.getText().trim(), duracion, estado);
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

    private void mostrarRegistroAusente() {
        JOptionPane.showMessageDialog(this,
                "La cita seleccionada ya no existe. Se volverá a consultar el listado.",
                "Cita no disponible", JOptionPane.INFORMATION_MESSAGE);
    }

    private void limpiarTrasCambio() {
        limpiarFormulario();
        modeloTabla.reemplazar(Collections.emptyList());
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
            etiquetaTotal.setText(citas.size() + (citas.size() == 1 ? " cita" : " citas"));
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
        chipPendientes.setValor(pendientes);
        chipConfirmadas.setValor(confirmadas);
        chipCanceladas.setValor(canceladas);
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
        tabla.setEnabled(!ocupado);
        tabla.getTableHeader().setEnabled(!ocupado);
        botonNuevo.setEnabled(!ocupado);
        botonGuardar.setEnabled(!ocupado && !editando);
        botonActualizar.setEnabled(!ocupado && editando);
        botonEliminar.setEnabled(!ocupado && editando);
        botonRefrescar.setEnabled(!ocupado);
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

    // ------------------------------------------------------------------
    // Animación de apertura (solo estético; se degrada sin errores si el
    // entorno gráfico no admite ventanas translúcidas).
    // ------------------------------------------------------------------

    private void prepararAnimacionApertura() {
        try {
            ubicacionFinal = getLocation();
            setOpacity(0f);
            setLocation(ubicacionFinal.x, ubicacionFinal.y + 24);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent evento) {
                    animarAparicion();
                }
            });
        } catch (RuntimeException ex) {
            // El entorno gráfico no admite ventanas translúcidas; se muestra sin animación.
        }
    }

    private void animarAparicion() {
        final int pasos = 18;
        final int[] paso = { 0 };
        Timer temporizador = new Timer(14, null);
        temporizador.addActionListener(evento -> {
            paso[0]++;
            float progreso = Math.min(1f, paso[0] / (float) pasos);
            float suavizado = 1f - (1f - progreso) * (1f - progreso);
            try {
                setOpacity(suavizado);
            } catch (RuntimeException ex) {
                // se ignora si el entorno no admite opacidad variable
            }
            setLocation(ubicacionFinal.x, ubicacionFinal.y + Math.round(24 * (1 - suavizado)));
            if (progreso >= 1f) {
                ((Timer) evento.getSource()).stop();
            }
        });
        temporizador.start();
    }

    // ------------------------------------------------------------------
    // Renderizadores de la tabla
    // ------------------------------------------------------------------

    private static final class ChipEstadistica extends JPanel {
        private static final long serialVersionUID = 1L;
        private final JLabel valor;

        ChipEstadistica(String etiquetaTexto, Color colorPunto) {
            setOpaque(false);
            setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));
            setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 14));

            JPanel punto = new JPanel() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(colorPunto);
                    g2.fillOval(0, 0, 10, 10);
                    g2.dispose();
                }
            };
            punto.setOpaque(false);
            punto.setPreferredSize(new Dimension(10, 10));

            JPanel textos = new JPanel();
            textos.setOpaque(false);
            textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
            valor = new JLabel("0");
            valor.setForeground(Color.WHITE);
            valor.setFont(valor.getFont().deriveFont(Font.BOLD, 18f));
            JLabel etiqueta = new JLabel(etiquetaTexto);
            etiqueta.setForeground(new Color(255, 255, 255, 205));
            etiqueta.setFont(etiqueta.getFont().deriveFont(11f));
            textos.add(valor);
            textos.add(etiqueta);

            add(punto);
            add(textos);
        }

        void setValor(int numero) {
            valor.setText(Integer.toString(numero));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(255, 255, 255, 30));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class FechaRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        FechaRenderer() {
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        }

        @Override
        protected void setValue(Object valor) {
            setText(valor == null ? "" : FORMATO_FECHA.format((LocalDateTime) valor));
        }
    }

    private static final class DuracionRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        DuracionRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        protected void setValue(Object valor) {
            setText(valor == null ? "" : valor + " min");
        }
    }

    /** Dibuja el estado como una insignia (píldora) de color en vez de solo texto. */
    private static final class EstadoRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private Color colorPildora = Estilos.SECUNDARIO;

        @Override
        public Component getTableCellRendererComponent(JTable tablaOrigen, Object valor, boolean seleccionada,
                boolean enfocada, int fila, int columna) {
            super.getTableCellRendererComponent(tablaOrigen, valor, seleccionada, enfocada, fila, columna);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(getFont().deriveFont(Font.BOLD, 11.5f));
            if (valor == EstadoCita.CONFIRMADA) {
                colorPildora = Estilos.EXITO;
            } else if (valor == EstadoCita.CANCELADA) {
                colorPildora = Estilos.PELIGRO;
            } else {
                colorPildora = Estilos.ADVERTENCIA;
            }
            setText(valor == null ? "" : valor.toString());
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            String texto = getText();
            if (texto.isEmpty()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            FontMetrics fm = g2.getFontMetrics(getFont());
            int anchoTexto = fm.stringWidth(texto);
            int anchoPildora = anchoTexto + 24;
            int altoPildora = Math.min(getHeight() - 8, 24);
            int x = (getWidth() - anchoPildora) / 2;
            int y = (getHeight() - altoPildora) / 2;
            g2.setColor(colorPildora);
            g2.fillRoundRect(x, y, anchoPildora, altoPildora, altoPildora, altoPildora);
            g2.setColor(Color.WHITE);
            g2.setFont(getFont());
            int textoX = x + (anchoPildora - anchoTexto) / 2;
            int textoY = y + (altoPildora - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(texto, textoX, textoY);
            g2.dispose();
        }
    }
}