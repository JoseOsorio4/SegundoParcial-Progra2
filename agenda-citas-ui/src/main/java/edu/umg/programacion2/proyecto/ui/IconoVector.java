package edu.umg.programacion2.proyecto.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Iconos vectoriales sencillos dibujados con líneas y arcos.
 * Así la interfaz no depende de archivos de imagen externos.
 */
final class IconoVector {

    enum Tipo {
        MAS, CHECK, REFRESCAR, PAPELERA, LAPIZ, CERRAR,
        CALENDARIO, USUARIO, RELOJ, ETIQUETA
    }

    private IconoVector() {
    }

    static void dibujar(Graphics2D destino, Tipo tipo, int cx, int cy, int tam, Color color) {
        Graphics2D g = (Graphics2D) destino.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int r = tam / 2;
        int x = cx - r;
        int y = cy - r;

        switch (tipo) {
            case MAS:
                g.drawLine(cx - r, cy, cx + r, cy);
                g.drawLine(cx, cy - r, cx, cy + r);
                break;
            case CHECK:
                g.drawLine(cx - r, cy, cx - r / 4, cy + r - 2);
                g.drawLine(cx - r / 4, cy + r - 2, cx + r, cy - r + 2);
                break;
            case REFRESCAR:
                g.drawArc(x, y, tam, tam, -40, 280);
                int[] px = { cx + r - 1, cx + r + 4, cx + r - 5 };
                int[] py = { cy - r + 1, cy - r - 5, cy - r - 4 };
                g.fillPolygon(px, py, 3);
                break;
            case PAPELERA:
                g.drawLine(x, y + 3, cx + r, y + 3);
                g.drawLine(cx - r / 2, y + 3, cx - r / 2, y);
                g.drawLine(cx + r / 2, y + 3, cx + r / 2, y);
                g.drawRect(x + 1, y + 4, tam - 2, tam - 5);
                g.drawLine(cx - r / 2, y + 6, cx - r / 2, y + tam - 3);
                g.drawLine(cx + r / 2, y + 6, cx + r / 2, y + tam - 3);
                break;
            case LAPIZ:
                g.drawLine(x, y + tam, x + tam - 4, y + 4);
                g.drawLine(x + tam - 4, y + 4, x + tam, y + 8);
                g.drawLine(x + tam, y + 8, x + 4, y + tam);
                g.drawLine(x, y + tam, x + 4, y + tam);
                break;
            case CERRAR:
                g.drawLine(x, y, x + tam, y + tam);
                g.drawLine(x + tam, y, x, y + tam);
                break;
            case CALENDARIO:
                g.drawRoundRect(x, y + 2, tam, tam - 2, 3, 3);
                g.drawLine(x + tam / 3, y, x + tam / 3, y + 4);
                g.drawLine(x + tam - tam / 3, y, x + tam - tam / 3, y + 4);
                g.drawLine(x, y + 6, x + tam, y + 6);
                break;
            case USUARIO:
                g.drawOval(cx - r / 2, y, r, r);
                g.drawArc(x, cy, tam, tam, 0, 180);
                break;
            case RELOJ:
                g.drawOval(x, y, tam, tam);
                g.drawLine(cx, cy, cx, cy - r + 3);
                g.drawLine(cx, cy, cx + r - 4, cy + 2);
                break;
            case ETIQUETA:
                int[] ex = { x, x + tam - 4, x + tam, x + tam - 4, x };
                int[] ey = { y + 2, y, y + r, y + tam, y + tam - 2 };
                g.drawPolygon(ex, ey, 5);
                g.drawOval(x + 2, y + 3, 3, 3);
                break;
            default:
                break;
        }

        g.dispose();
    }
}
