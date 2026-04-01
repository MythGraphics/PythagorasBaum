/*
 *
 */

package pythagorasbaum;

/**
 *
 * @author  Martin Pröhl alias MythGraphics
 * @version 1.0.0
 *
 */

import java.awt.*;
import java.awt.geom.Ellipse2D;
import javax.swing.JPanel;

public class Rings extends JPanel {

    private Graphics2D g2d;
    private Point mp;                                                                                                   // MousePoint
    private double r = 100.0;                                                                                           // Radius Kreis
    private Ellipse2D kreis1, kreis2, kreis3;

    public Rings() {
        this(400, 300);
    }

    public Rings(int width, int hight, double r) {
        this(width, hight);
        this.r = r;
    }

    public Rings(int width, int hight) {
        super.setBackground( Color.DARK_GRAY );
        super.setSize( width, hight );
    }

    public static void setRenderingHints(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g2d = (Graphics2D) g;
        setRenderingHints(g2d);
        mp = super.getMousePosition();
        if ( mp != null ) {
            draw(g2d, mp, r);
        } else {
            System.err.println("Maus außerhalb des Panels - zeichnen nicht möglich!");
        }
    }

    public void draw(Graphics2D g2d, Point mp, double r) {
        // Punkt(0, 0) ist oben links
        // Kreis mit Mittelpunkt mx/my und Radius r
        double mx = mp.getX();
        double my = mp.getY();
        double r2 = r*2/3;
        double r3 = r*1/3;
        kreis1 = new Ellipse2D.Double(mx-r,  my-r,  2*r,  2*r);
        kreis2 = new Ellipse2D.Double(mx-r2, my-r2, 2*r2, 2*r2);
        kreis3 = new Ellipse2D.Double(mx-r3, my-r3, 2*r3, 2*r3);
        g2d.setColor( Color.GREEN );
        g2d.draw(kreis1);
        g2d.draw(kreis2);
        g2d.draw(kreis3);
    }

}
