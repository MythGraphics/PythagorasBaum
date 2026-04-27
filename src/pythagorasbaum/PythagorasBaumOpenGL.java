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

import java.awt.Color;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class PythagorasBaumOpenGL extends AbstractGL2D {

    private final static String TITLE = "Pythagoras-Baum (OpenGL)";
    private final int MAX_REKURSION   = 10;

    private float animationOffset = 0.0f; // Taktgeber für die Farbe
    private float farbPhase = 0.0f; // Taktgeber für die Farbe der alternativen Farb-Logik
    private float winkel = 45.0f; // Standardwert -> symmetrischer Baum

    public PythagorasBaumOpenGL() {}

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public void processKeyBindings() {
        super.processKeyBindings();
        if ( glfwGetKey( window, GLFW_KEY_A ) == GLFW_PRESS ) { winkel += 0.2f; }
        if ( glfwGetKey( window, GLFW_KEY_D ) == GLFW_PRESS ) { winkel -= 0.2f; }
    }

    @Override
    public void drawMain() {
        float wind = (float) Math.sin(animationOffset * 1.5f) * 5.0f; // Wind-Effekt: 5° Oszillation

        // Geschwindigkeit des Farbwechsels
        animationOffset += 0.01f;
        farbPhase += 0.01f;

        drawTree(0.6f, 0, winkel+wind); // Baum zeichnen
    }

    private void drawTree(float size, int ebene, float winkelAlpha) {
        if (ebene > MAX_REKURSION) {
            return;
        }

        /* Dynamische Farbberechnung (Regenbogen)
         * HSB-Modus: Der Farbton (Hue) wandert mit der Zeit und der Ebene
         * Berechne den Farbton basierend auf Zeit (offset) und der Baum-Tiefe (ebene)
         */
        float hue = ( animationOffset + ( ebene * 0.05f )) % 1.0f;
        int rgb = Color.HSBtoRGB(hue, 0.8f, 1.0f);

        // RGB-Werte aus dem Integer extrahieren
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >>  8) & 0xFF) / 255.0f;
        float b = ( rgb & 0xFF)        / 255.0f;
 /*
        // alternative Farb-Logik (manuell ohne java.awt)
        // Sinus-Wellen für RGB, die leicht phasenverschoben sind
        float r = (float) Math.sin(farbPhase + ebene * 0.3f) * 0.5f + 0.5f;
        float g = (float) Math.sin(farbPhase + ebene * 0.3f + 2.0f) * 0.5f + 0.5f;
        float b = (float) Math.sin(farbPhase + ebene * 0.3f + 4.0f) * 0.5f + 0.5f;
 */
        // Farbe setzen
        glColor4f(r, g, b, 0.4f); // 40% Opazität

        // Zeichen des aktuellen Quadrats
        glBegin(GL_QUADS);
            glVertex2f(-size/2, 0);
            glVertex2f( size/2, 0);
            glVertex2f( size/2, size);
            glVertex2f(-size/2, size);
        glEnd();

        // Geometrie berechnen (Trigonometrie am rechtwinkligen Dreieck)
        float winkelBeta     = 90 - winkelAlpha;
        float neueSizeLinks  = size * (float) Math.cos( Math.toRadians( winkelAlpha ));
        float neueSizeRechts = size * (float) Math.sin( Math.toRadians( winkelAlpha ));

        // Verschiebe den Ursprung auf die Oberkante des Quadrats
        glTranslatef(0, size, 0);

        // Die Seitenlänge der neuen Quadrate bei 45° ist size*cos(45°)
        // float neuSize = size * (float) (Math.sqrt(2) / 2.0);

        // Linker Zweig
        glPushMatrix();
            glTranslatef(-size / 2, 0, 0);                  // Gehe zur linken Ecke
            glRotatef(winkelAlpha, 0, 0, 1);                // Rotiere um winkelAlpha
            glTranslatef(neueSizeLinks / 2, 0, 0);          // Ursprung schieben, dass neues Quadrat auf der Hypotenuse sitzt
            drawTree(neueSizeLinks, ebene+1, winkelAlpha);  // Rekursion
        glPopMatrix();

        // Rechter Zweig
        glPushMatrix();
            glTranslatef(size / 2, 0, 0);                   // Gehe zur rechten Ecke
            glRotatef(-winkelBeta, 0, 0, 1);                // Rotiere um -winkelBeta
            glTranslatef(-neueSizeRechts / 2, 0, 0);        // Ursprung schieben, dass neues Quadrat auf der Hypotenuse sitzt
            drawTree(neueSizeRechts, ebene+1, winkelAlpha); // Rekursion
        glPopMatrix();
    }

    public static void main(String[] args) {
        new PythagorasBaumOpenGL().run();
    }

}
