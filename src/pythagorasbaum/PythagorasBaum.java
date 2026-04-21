/*
 *
 */

package pythagorasbaum;

/**
 *
 * @author  Martin Pröhl alias MythGraphics
 * @version 1.1.2
 *
 */

import java.awt.*;
import javax.swing.JPanel;

public class PythagorasBaum extends JPanel {

    private final static String ERR1 = " Zeichnen nicht möglich.";

    private Point a1, b1, c1, d1; // Punkte für linkes Quadrat
    private Point a2, b2, c2, d2; // Punkte für rechtes Quadrat
    private Point mp; // MousePoint
    private Color color1 = new Color(118, 67, 38);  // Farbe des "Stamms" (braun)
    private Color color2 = new Color(0, 255, 0);    // Farbe der "Blätter" (grün)
    private Graphics2D graphics;
    private int depth = 8; // Rekursionstiefe des Baums
    private double tanphi = Math.tan( Math.toRadians(45) ); // Tangens des Winkels Phi (ϕ) zwischen Hypothenuse und rechter Kathete

    public PythagorasBaum() {
        this(400, 300);
    }

    public PythagorasBaum(int width, int hight) {
        setBGColor(Color.DARK_GRAY);
        setSize(width, hight);
    }

    /**
     *
     * @param phi Winkel Phi in Grad (Winkel zwischen Hypothenuse und rechter Kathete)
     */
    public void setPhi(double phi) {
        if ( (phi < 3) || (phi > 64) ) {
            System.err.println("Winkel ungültig." + ERR1);
            return;
        }
        tanphi = Math.tan( Math.toRadians(phi) );
    }

    /**
     *
     * @return phi Winkel Phi in Grad (Winkel zwischen Hypothenuse und rechter Kathete)
     */
    public double getPhi() {
        return Math.toDegrees( Math.atan( tanphi ));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        graphics = (Graphics2D) g;
        graphics.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );
        mp = getMousePosition();
        if ( mp != null ) {
            drawTree(
                graphics,
                mp,
                new Point(mp.x+50, mp.y   ),
                new Point(mp.x+50, mp.y-50),
                new Point(mp.x   , mp.y-50),
                depth
            );
        } else {
            System.err.println("Maus außerhalb des Panels." + ERR1);
        }
    }


    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int getDepth() {
        return depth;
    }

    public void setTreeColors(Color color1, Color color2) {
        this.color1 = color1;
        this.color2 = color2;
    }

    public final void setBGColor(Color bg) {
        setBackground(bg);
    }

    public void drawTree(Point[] firstRect, int depth) {
        drawTree(graphics, firstRect[0], firstRect[1], firstRect[2], firstRect[3], depth);
    }

    public void drawTree(Graphics2D g, Point[] firstRect, int depth) {
        drawTree(g, firstRect[0], firstRect[1], firstRect[2], firstRect[3], depth);
    }

    public void drawTree(Graphics2D g, Point a, Point b, Point c, Point d, int depth) {
        // Punkt(0, 0) ist oben links
        // a,b,c,d sind die Punkte des jeweiligen Quadrats (beginnend von unten links im Uhrzeigersinn)
        if ( depth == 0 ) { return; } // Ast-Ende erreicht
        --depth;
        // Quadrat-Koordinaten bestimmen und zeichnen
        int[] xcoords = {a.x, b.x, c.x, d.x};
        int[] ycoords = {a.y, b.y, c.y, d.y};
        Polygon p = new Polygon(xcoords, ycoords, 4);
        // Farbe variieren mittels gewichteter arithmetischer Mittelung
        double w2 = 1.0 / (depth + 1); // Gewichtung für Color2 (Blätter)
        double w1 = 1.0 - w2;          // Gewichtung für Color1 (Stamm)
        g.setColor( new Color(
            (int) ( color1.getRed()   * w1 + color2.getRed()   * w2 ),
            (int) ( color1.getGreen() * w1 + color2.getGreen() * w2 ),
            (int) ( color1.getBlue()  * w1 + color2.getBlue()  * w2 )
        ));
/*    * oder
        g.setColor( new Color(
            color1.getRed()   * depth / (depth+1) + color2.getRed()   / (depth+1) / (depth+1),
            color1.getGreen() * depth / (depth+1) + color2.getGreen() / (depth+1) / (depth+1),
            color1.getBlue()  * depth / (depth+1) + color2.getBlue()  / (depth+1) / (depth+1)
        ));
      * mathematisch vereinfacht
        g.setColor( new Color(
            ( color1.getRed()   * depth + color2.getRed()   / (depth+1) ) / (depth+1),
            ( color1.getGreen() * depth + color2.getGreen() / (depth+1) ) / (depth+1),
            ( color1.getBlue()  * depth + color2.getBlue()  / (depth+1) ) / (depth+1)
        ));
 */
        g.fillPolygon(p);
        /* neuen Basispunkt e für a oder b des nächsten Quadrats bestimmen
         * r1 als den relativen Abstand des Punktes e von d entlang der Kante dc, wobei r1​ näherungsweise sin(ϕ)
         * darstellt, die das Teilungsverhältnis bestimmt */
        double r1 = tanphi/2.0; // normiertes Längenverhältnis der beiden Katheten zueinander
        // Dreieckshöhe; r2 basiert auf dem Satz des Pythagoras und bestimmt die Höhe des Punktes e senkrecht zur Kante dc
        double r2 = Math.sqrt( 0.5*0.5 - (0.5-r1)*(0.5-r1) ); // Satz des Pythagoras, normierte Darstellung: Hypothenuse mit Länge 1
        // Scheitelpunkt e über Kante dc
        Point e = new Point(
            (int) (d.x + r1*(c.x-d.x) + r2*(c.y-d.y) ),
            (int) (d.y + r1*(c.y-d.y) - r2*(b.x-a.x) )
        );
        /* e wird durch die Vektoraddition des Startpunkts d, der horizontalen Verschiebung r1​ entlang der Kante und der
         * vertikalen Verschiebung r2​ (Höhe) senkrecht zur Kante bestimmt. Der Winkel ϕ (über r1​) und die Höhe r2​ sind
         * also untrennbar miteinander verbunden und definieren die exakte Position des Verzweigungspunkts e. */
        // Neues Quadrat links
        a1 = d;
        b1 = e;
        c1 = new Point(b1.x - a1.y + b1.y , b1.y - b1.x + a1.x); // b1 + (90-Grad-gedrehter Vektor von a1 nach b1)
        d1 = new Point(a1.x - a1.y + b1.y , a1.y - b1.x + a1.x); // a1 + (90-Grad-gedrehter Vektor von a1 nach b1)
        drawTree(g, a1, b1, c1, d1, depth);
        // Neues Quadrat rechts
        a2 = e;
        b2 = c;
        c2 = new Point(b2.x - a2.y + b2.y , b2.y - b2.x + a2.x); // b2 + (90-Grad-gedrehter Vektor von a2 nach b2)
        d2 = new Point(a2.x - a2.y + b2.y , a2.y - b2.x + a2.x); // a2 + (90-Grad-gedrehter Vektor von a2 nach b2)
        drawTree(g, a2, b2, c2, d2, depth);
    }

}
