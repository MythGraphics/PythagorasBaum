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

import java.nio.DoubleBuffer;
import org.lwjgl.BufferUtils;
import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public abstract class AbstractGL2D {

    public final float speed    = 0.05f;

    protected long window;
    protected double time       = glfwGetTime();
    protected double lastTime   = time;
    protected int frameCount    = 0;
    protected float zoom        = 1.0f;
    protected float mainX       = 0.0f;
    protected float mainY       = -1.0f;

    abstract String getTitle();
    abstract void drawMain();

    public void run() {
        init();
        renderLoop();
        glfwTerminate();
    }

    public void init() {
        // Initialisierung von GLFW & Fenster
        if ( !glfwInit() ) {
            throw new IllegalStateException("GLFW konnte nicht geladen werden");
        }
        window = glfwCreateWindow( 800, 600, getTitle(), 0, 0 );
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
        // Callback für Maus setzen
        glfwSetCursorPosCallback(window, (windowHandle, xpos, ypos) -> {
            mouseCallback(xpos, ypos);
        });

        // Initialer Aufruf des Kamera-Setups
        int[] w = new int[1], h = new int[1];
        glfwGetWindowSize(window, w, h);
        setupProjection(w[0], h[0]);

        activateGlow();
    }

    private void activateGlow() {
        // Glow-Effekt
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE); // Additives Blending: Farben addieren sich bei Überlagerung
        glDisable(GL_DEPTH_TEST); // Wichtig für Glow: Objekte verdecken sich nicht
    }

    // Kamera-Setup (Einfaches 2D-Koordinatensystem)
    public void setupProjection(int width, int height) {
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

    public void processKeyBindings() {
        if ( glfwGetKey( window, GLFW_KEY_ESCAPE ) == GLFW_PRESS ) { glfwSetWindowShouldClose(window, true); }
        if ( glfwGetKey( window, GLFW_KEY_W      ) == GLFW_PRESS ) {
            zoom *= 1.01f; // 1% vergrößern pro Frame
        }
        if ( glfwGetKey( window, GLFW_KEY_S      ) == GLFW_PRESS ) {
            zoom /= 1.01f; // 1% verkleinern pro Frame
        }
        if ( glfwGetKey( window, GLFW_KEY_LEFT   ) == GLFW_PRESS ) { mainX -= speed; }
        if ( glfwGetKey( window, GLFW_KEY_RIGHT  ) == GLFW_PRESS ) { mainX += speed; }
        if ( glfwGetKey( window, GLFW_KEY_UP     ) == GLFW_PRESS ) { mainY += speed; }
        if ( glfwGetKey( window, GLFW_KEY_DOWN   ) == GLFW_PRESS ) { mainY -= speed; }
        // Zurücksetzen
        if ( glfwGetKey( window, GLFW_KEY_R      ) == GLFW_PRESS ) { resetPos(); }
    }

    public void resetPos() {
        mainX = 0;
        mainY = -1.0f;
        zoom  = 1.0f;
    }

    // Methode optional, daher nicht abstract
    public void mouseCallback(double xpos, double ypos) {}

    public void renderLoop() {
        while ( !glfwWindowShouldClose( window )) {
            // FPS ermitteln
            time = glfwGetTime();
            frameCount++;
            if (time-lastTime >= 1.0) {
                glfwSetWindowTitle( window, getTitle() + " | FPS: " + frameCount ); // FPS in den Titel schreiben
                frameCount = 0;
                lastTime = time;
            }

            glClear(GL_COLOR_BUFFER_BIT);
            glLoadIdentity();
            processKeyBindings();

            // Gesamte Welt verschieben, bevor gezeichnet wird
            glTranslatef(mainX, mainY, 0);
            // Die Reihenfolge bestimmt, ob wir um das Zentrum oder den Ursprung zoomen
            glScalef(zoom, zoom, 1.0f);

            drawMain();
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

}
