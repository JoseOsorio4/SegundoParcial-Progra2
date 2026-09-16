package edu.umg.programacion2.proyecto.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

/**
 * Paleta de colores y componentes visuales compartidos entre las ventanas.
 * Mantener los estilos aquí evita repetir colores, bordes y comportamiento visual.
 */
final class Estilos {

    static final Color FONDO = new Color(238, 241, 247);
    static final Color TEXTO = new Color(30, 34, 51);
    static final Color SECUNDARIO = new Color(108, 117, 136);
    static final Color PRIMARIO = new Color(79, 70, 229);
    static final Color PRIMARIO_HOVER = new Color(67, 56, 202);
    static final Color PRIMARIO_CLARO = new Color(237, 236, 253);
    static final Color ACENTO = new Color(124, 58, 237);
    static final Color EXITO = new Color(22, 163, 74);
    static final Color EXITO_HOVER = new Color(21, 128, 61);
    static final Color ADVERTENCIA = new Color(217, 119, 6);
    static final Color PELIGRO = new Color(220, 38, 38);
    static final Color PELIGRO_HOVER = new Color(185, 28, 28);
    static final Color BORDE = new Color(224, 228, 236);

    private Estilos() {
    }

    /** Aplica un borde redondeado que cambia de color al enfocar el campo. */
    static void campo(JTextField campo) {
        campo.setFont(campo.getFont().deriveFont(14f));
        campo.setForeground(TEXTO);
        campo.setBackground(Color.WHITE);
        campo.setCaretColor(PRIMARIO);

        Border normal = BorderFactory.createCompoundBorder(
                new LineBorder(BORDE, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10));
        Border enfocado = BorderFactory.createCompoundBorder(
                new LineBorder(PRIMARIO, 2, true),
                BorderFactory.createEmptyBorder(5, 9, 5, 9));

        campo.setBorder(normal);
        campo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent evento) {
                if (campo.isEnabled()) {
                    campo.setBorder(enfocado);
                }
            }

            @Override
            public void focusLost(FocusEvent evento) {
                campo.setBorder(normal);
            }
        });
    }

    /** Aplica un borde consistente con el resto del formulario a un combo. */
    static void combo(JComboBox<?> combo) {
        combo.setFont(combo.getFont().deriveFont(14f));
        combo.setBackground(Color.WHITE);
        combo.setForeground(TEXTO);
        combo.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDE, 1, true),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));
    }

    /** Tarjeta blanca con esquinas redondeadas y una sombra suave. */
    static class Tarjeta extends JPanel {
        private static final long serialVersionUID = 1L;

        Tarjeta() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int arco = 16;
            int sombra = 5;
            for (int i = sombra; i > 0; i--) {
                g2.setColor(new Color(30, 41, 59, 6));
                g2.fillRoundRect(i, i + 2,
                        Math.max(getWidth() - i * 2, 0),
                        Math.max(getHeight() - i * 2, 0), arco, arco);
            }

            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0,
                    Math.max(getWidth() - sombra, 0),
                    Math.max(getHeight() - sombra - 2, 0), arco, arco);

            g2.setColor(BORDE);
            g2.drawRoundRect(0, 0,
                    Math.max(getWidth() - sombra - 1, 0),
                    Math.max(getHeight() - sombra - 3, 0), arco, arco);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Botón redondeado con variante rellena o de contorno e icono opcional. */
    static class Boton extends JButton {
        private static final long serialVersionUID = 1L;

        private final IconoVector.Tipo icono;
        private final Color base;
        private final Color hover;
        private final Color textoColor;
        private final boolean relleno;
        private boolean sobre;

        Boton(String texto, IconoVector.Tipo icono, Color base, Color hover,
                Color textoColor, boolean relleno) {
            super(texto);
            this.icono = icono;
            this.base = base;
            this.hover = hover;
            this.textoColor = textoColor;
            this.relleno = relleno;

            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setFont(getFont().deriveFont(Font.BOLD, 13f));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent evento) {
                    sobre = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent evento) {
                    sobre = false;
                    repaint();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension base2 = super.getPreferredSize();
            int extra = icono == null ? 24 : 40;
            return new Dimension(base2.width + extra, Math.max(base2.height + 12, 38));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int ancho = getWidth();
            int alto = getHeight();
            int arco = 10;
            Color colorTexto;

            if (!isEnabled()) {
                g2.setColor(new Color(228, 231, 237));
                g2.fillRoundRect(0, 0, ancho - 1, alto - 1, arco, arco);
                colorTexto = new Color(160, 167, 178);
            } else if (relleno) {
                Color fondo = sobre ? hover : base;
                if (getModel().isPressed()) {
                    fondo = hover.darker();
                }
                g2.setColor(fondo);
                g2.fillRoundRect(0, 0, ancho - 1, alto - 1, arco, arco);
                colorTexto = textoColor;
            } else {
                if (sobre || getModel().isPressed()) {
                    int alfa = getModel().isPressed() ? 40 : 26;
                    g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), alfa));
                    g2.fillRoundRect(0, 0, ancho - 1, alto - 1, arco, arco);
                }
                g2.setColor(base);
                g2.setStroke(new BasicStroke(1.3f));
                g2.drawRoundRect(0, 0, ancho - 2, alto - 2, arco, arco);
                colorTexto = textoColor;
            }

            int x = 14;
            int centroY = alto / 2;
            if (icono != null) {
                IconoVector.dibujar(g2, icono, x + 7, centroY, 14, colorTexto);
                x += 20;
            }

            g2.setColor(colorTexto);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int textoY = centroY - fm.getHeight() / 2 + fm.getAscent();
            g2.drawString(getText(), x, textoY);
            g2.dispose();
        }
    }

    /** Adaptador para usar un IconoVector como Icon de Swing. */
    static final class IconoSwing implements Icon {
        private final IconoVector.Tipo tipo;
        private final int tam;
        private final Color color;

        IconoSwing(IconoVector.Tipo tipo, int tam, Color color) {
            this.tipo = tipo;
            this.tam = tam;
            this.color = color;
        }

        @Override
        public int getIconWidth() {
            return tam;
        }

        @Override
        public int getIconHeight() {
            return tam;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            IconoVector.dibujar(g2, tipo, x + tam / 2, y + tam / 2, tam, color);
            g2.dispose();
        }
    }
}
