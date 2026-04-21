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
import java.nio.DoubleBuffer;
import org.lwjgl.BufferUtils;
import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class PythagorasBaumOpenGL {

    private final static String TITLE = "Pythagoras-Baum (OpenGL)";

    private final int MAX_REKURSION = 10;
    private final float speed = 0.05f;

    private long window;
    private float animationOffset = 0; // Taktgeber für die Farbe
    private float farbPhase = 0; // Taktgeber für die Farbe der alternativen Farb-Logik
    private int frameCount  = 0;
    private double lastTime = glfwGetTime();
    private float winkel = 45.0f; // Standardwert -> symmetrischer Baum

    private float zoom = 1.0f;
    private float baumX = 0.0f;
    private float baumY = -1.0f;

    // Kamera-Setup (Einfaches 2D-Koordinatensystem)
    private void setupProjection(int width, int height) {
        // Verhindere Division durch Null, falls das Fenster minimiert wird
        if (height == 0) {
            height = 1;
        }

        glViewport(0, 0, width, height);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();

        float aspectRatio = (float) width / height;
        glOrtho(-2.5f * aspectRatio, 2.5f * aspectRatio, -2.5f, 2.5f, -1, 1);

        glMatrixMode(GL_MODELVIEW);
    }

    private void init() {
        // Initialisierung von GLFW & Fenster
        if ( !glfwInit() ) {
            throw new IllegalStateException("GLFW konnte nicht geladen werden");
        }
        window = glfwCreateWindow(800, 600, TITLE, 0, 0);
        if (window == NULL) {
            throw new RuntimeException("Fenster konnte nicht erstellt werden");
        }
        glfwMakeContextCurrent(window);
        GL.createCapabilities(); // Verbindet LWJGL mit dem OpenGL-Kontext
        glfwSwapInterval(1);     // Aktiviert V-Sync
        glShadeModel(GL_SMOOTH); // Erlaubt Farbverläufe

        // Callback für Kamera-Setup setzen
        glfwSetFramebufferSizeCallback(window, (win, width, height) -> {
            setupProjection(width, height);
        });

        // Initialer Aufruf des Kamera-Setups
        int[] w = new int[1], h = new int[1];
        glfwGetWindowSize(window, w, h);
        setupProjection(w[0], h[0]);

        // Glow-Effekt
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE); // Additives Blending: Farben addieren sich bei Überlagerung
        glDisable(GL_DEPTH_TEST); // Wichtig für Glow: Objekte verdecken sich nicht
    }

    public void run() {
        init();
//      DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);

        while ( !glfwWindowShouldClose( window )) {
            // FPS ermitteln
            double currentTime = glfwGetTime();
            frameCount++;
            // Sobald eine Sekunde vergangen ist
            if (currentTime-lastTime >= 1.0) {
                glfwSetWindowTitle(window, TITLE + " | FPS: " + frameCount); // FPS in den Titel schreiben
                frameCount = 0;
                lastTime = currentTime;
            }

            glClear(GL_COLOR_BUFFER_BIT);
            glLoadIdentity();

            // Tastatur-Steuerung
            if ( glfwGetKey( window, GLFW_KEY_W     ) == GLFW_PRESS ) {
                zoom *= 1.01f; // 1% vergrößern pro Frame
            }
            if ( glfwGetKey( window, GLFW_KEY_S     ) == GLFW_PRESS ) {
                zoom /= 1.01f; // 1% verkleinern pro Frame
            }
            if ( glfwGetKey( window, GLFW_KEY_LEFT  ) == GLFW_PRESS ) { baumX -= speed; }
            if ( glfwGetKey( window, GLFW_KEY_RIGHT ) == GLFW_PRESS ) { baumX += speed; }
            if ( glfwGetKey( window, GLFW_KEY_UP    ) == GLFW_PRESS ) { baumY += speed; }
            if ( glfwGetKey( window, GLFW_KEY_DOWN  ) == GLFW_PRESS ) { baumY -= speed; }
            if ( glfwGetKey( window, GLFW_KEY_A     ) == GLFW_PRESS ) { winkel += 0.2f; }
            if ( glfwGetKey( window, GLFW_KEY_D     ) == GLFW_PRESS ) { winkel -= 0.2f; }
            // Zurücksetzen
            if ( glfwGetKey( window, GLFW_KEY_R     ) == GLFW_PRESS ) {
                baumX = 0;
                baumY = -1.0f;
                zoom  = 1.0f;
            }

            // Gesamte Welt verschieben, bevor der Baum gezeichnet wird
            glTranslatef(baumX, baumY, 0);

            // Transformationen anwenden
            // WICHTIG: Die Reihenfolge bestimmt, ob wir um das Zentrum oder den Ursprung zoomen
            glTranslatef(baumX, baumY, 0);
            glScalef(zoom, zoom, 1.0f);
/*
            // Mausposition abfragen und Winkel berechnen
            glfwGetCursorPos(window, xBuffer, null);
            double xPos = xBuffer.get(0);
            int[] width = new int[1];
            glfwGetWindowSize(window, width, null);

            // Mappe X-Position (0 bis Fensterbreite) auf Winkel (10 bis 80 Grad) für MausWinkel
            winkel = (float) (10 + (xPos / width[0]) * 70);
 */
            // Wind-Effekt
            float wind = (float) Math.sin(animationOffset * 1.5f) * 5.0f; // 5° Oszillation

            // Geschwindigkeit des Farbwechsels
            animationOffset += 0.01f;
            farbPhase += 0.01f;

            // Baum zeichnen
            drawTree(0.6f, 0, winkel + wind);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
        glfwTerminate();
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
        // Wir nutzen Sinus-Wellen für R, G und B, die leicht phasenverschoben sind
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
        // float neueSize = size * (float) (Math.sqrt(2) / 2.0);

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
