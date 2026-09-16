package edu.umg.programacion2.proyecto.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import edu.umg.programacion2.proyecto.modelo.Cita;
import edu.umg.programacion2.proyecto.modelo.EstadoCita;

/**
 * Ventana secundaria con el detalle completo de una cita.
 * Se abre con doble clic sobre una fila de la tabla principal.
 * No contiene lógica de negocio: solo presenta datos y delega la edición.
 */
final class DetalleCitaDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    DetalleCitaDialog(Window propietario, Cita cita, DateTimeFormatter formatoFecha, Consumer<Cita> alEditar) {
        super(propietario, "Detalle de la cita #" + cita.getId(), ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        construirContenido(cita, formatoFecha, alEditar);
        configurarTeclado();
        setResizable(false);
        pack();
        setLocationRelativeTo(propietario);
    }

    private void construirContenido(Cita cita, DateTimeFormatter formatoFecha, Consumer<Cita> alEditar) {
        Color colorEstado = colorParaEstado(cita.getEstado());

        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setBackground(Color.WHITE);
        contenedor.setBorder(BorderFactory.createLineBorder(Estilos.BORDE));
        setContentPane(contenedor);

        JPanel franjaEstado = new JPanel(new BorderLayout());
        franjaEstado.setBackground(colorEstado);
        franjaEstado.setBorder(BorderFactory.createEmptyBorder(20, 26, 20, 26));

        JLabel numero = new JLabel("Cita #" + cita.getId());
        numero.setFont(numero.getFont().deriveFont(Font.BOLD, 20f));
        numero.setForeground(Color.WHITE);
        franjaEstado.add(numero, BorderLayout.WEST);

        JLabel estadoTexto = new JLabel(cita.getEstado().toString().toUpperCase(new Locale("es", "GT")));
        estadoTexto.setFont(estadoTexto.getFont().deriveFont(Font.BOLD, 12.5f));
        estadoTexto.setForeground(Color.WHITE);
        franjaEstado.add(estadoTexto, BorderLayout.EAST);
        contenedor.add(franjaEstado, BorderLayout.NORTH);

        JPanel cuerpo = new JPanel(new GridBagLayout());
        cuerpo.setBackground(Color.WHITE);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(26, 30, 6, 30));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 20, 0);

        c.gridy = 0;
        cuerpo.add(fila("Cliente", cita.getCliente(), IconoVector.Tipo.USUARIO), c);
        c.gridy = 1;
        cuerpo.add(fila("Fecha y hora", formatoFecha.format(cita.getFechaHora()), IconoVector.Tipo.RELOJ), c);
        c.gridy = 2;
        cuerpo.add(fila("Servicio", cita.getServicio(), IconoVector.Tipo.ETIQUETA), c);
        c.gridy = 3;
        c.insets = new Insets(0, 0, 6, 0);
        cuerpo.add(fila("Duración", cita.getDuracionMinutos() + " minutos", IconoVector.Tipo.CALENDARIO), c);

        contenedor.add(cuerpo, BorderLayout.CENTER);

        JPanel pieDialogo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pieDialogo.setBackground(Color.WHITE);
        pieDialogo.setBorder(BorderFactory.createEmptyBorder(6, 24, 22, 24));

        Estilos.Boton cerrar = new Estilos.Boton("Cerrar", IconoVector.Tipo.CERRAR,
                Estilos.SECUNDARIO, Estilos.SECUNDARIO, Estilos.SECUNDARIO, false);
        cerrar.addActionListener(evento -> dispose());

        Estilos.Boton editar = new Estilos.Boton("Editar esta cita", IconoVector.Tipo.LAPIZ,
                Estilos.PRIMARIO, Estilos.PRIMARIO_HOVER, Color.WHITE, true);
        editar.addActionListener(evento -> {
            alEditar.accept(cita);
            dispose();
        });

        pieDialogo.add(cerrar);
        pieDialogo.add(editar);
        contenedor.add(pieDialogo, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(editar);
    }

    private void configurarTeclado() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cerrar");
        getRootPane().getActionMap().put("cerrar", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evento) {
                dispose();
            }
        });
    }

    private JPanel fila(String etiquetaTexto, String valorTexto, IconoVector.Tipo icono) {
        JPanel fila = new JPanel();
        fila.setOpaque(false);
        fila.setLayout(new BoxLayout(fila, BoxLayout.Y_AXIS));

        JLabel etiqueta = new JLabel(etiquetaTexto,
                new Estilos.IconoSwing(icono, 13, Estilos.SECUNDARIO), SwingConstants.LEFT);
        etiqueta.setIconTextGap(7);
        etiqueta.setForeground(Estilos.SECUNDARIO);
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD, 11.5f));

        JLabel valor = new JLabel(valorTexto == null ? "" : valorTexto);
        valor.setForeground(Estilos.TEXTO);
        valor.setFont(valor.getFont().deriveFont(15f));
        valor.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        fila.add(etiqueta);
        fila.add(valor);
        return fila;
    }

    private static Color colorParaEstado(EstadoCita estado) {
        if (estado == EstadoCita.CONFIRMADA) {
            return Estilos.EXITO;
        }
        if (estado == EstadoCita.CANCELADA) {
            return Estilos.PELIGRO;
        }
        return Estilos.ADVERTENCIA;
    }

    /**
     * Muestra la ventana con una animación breve de aparición.
     * Si el sistema no admite transparencia, se muestra normalmente.
     */
    void mostrarConAnimacion() {
        boolean animable = true;
        try {
            setOpacity(0f);
        } catch (RuntimeException ex) {
            animable = false;
        }

        int destinoY = getY();
        if (animable) {
            setLocation(getX(), destinoY - 16);
            final int pasos = 12;
            final int[] paso = { 0 };

            Timer temporizador = new Timer(12, null);
            temporizador.addActionListener(evento -> {
                paso[0]++;
                float progreso = Math.min(1f, paso[0] / (float) pasos);
                float suavizado = 1f - (1f - progreso) * (1f - progreso);

                try {
                    setOpacity(suavizado);
                } catch (RuntimeException ex) {
                    // El entorno no admite opacidad variable.
                }

                setLocation(getX(), destinoY - Math.round(16 * (1 - suavizado)));
                if (progreso >= 1f) {
                    ((Timer) evento.getSource()).stop();
                }
            });
            temporizador.start();
        }

        setVisible(true);
    }
}
