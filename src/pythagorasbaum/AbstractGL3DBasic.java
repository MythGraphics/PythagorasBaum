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

import java.io.IOException;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.glfw.Callbacks;
import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.system.MemoryUtil.NULL;
import static pythagorasbaum.GLUtil.GLUtil.*;
import pythagorasbaum.GLUtil.Shader;
import static pythagorasbaum.GLUtil.Shader.FRAGMENT;
import static pythagorasbaum.GLUtil.Shader.VERTEX;

public abstract class AbstractGL3DBasic {

    public final int maxDepth;
    public final float moveSpeed = 0.05f;
    public final float[] singleMatrixBuffer = new float[16];

    public float mainX      = 0.0f, mainY = 0.0f, mainZ = 0.0f;
    public double time      = glfwGetTime();
    public double lastTime  = time;
    public int frameCount   = 0;

    protected long window;
    protected int mainShaderProgram;
    protected Matrix4f projection, view;

    private int groundVao; // Vertex Array Object (Positionen der Eckpunkte)
    private int viewPosLoc, projLoc, viewLoc, maxDepthLoc, groundLoc, normalGroundLoc;
    private float camX, camY, camZ;
    private Matrix4f groundMatrix;
    private Matrix3f normalGroundMatrix;

    private float camYrot    = 0.0f;  // Drehung um die Y-Achse
    private float pitch      = 20.0f; // Oben/Unten
    private float distance   = 7.0f;  // Abstand zum Objekt
    private float camYOffset = 1.0f;  // Betrachtungshöhe

    public AbstractGL3DBasic(int maxDepth) {
        this.maxDepth = maxDepth;
        if (maxDepth < 1) {
            throw new IllegalArgumentException("maxDepth <1 unsinnig.");
        }
    }
abstract String getTitle();
    abstract String getMainShaderFilePath(Shader shader);
    abstract void setupMain();
    abstract void drawMain(); // instanced rendering

    public void setObjectPosition(float x, float y, float z) {
        mainX = x;
        mainY = y;
        mainZ = z;
    }

    public void run() {
        init();
        renderloop();
        dispose();
    }

    public void init() {
        // Initialisierung von GLFW & Fenster
        if ( !glfwInit() ) {
            throw new IllegalStateException("GLFW konnte nicht geladen werden.");
        }

        // Konfiguriere das Fenster: OpenGL 3.3 Core Profile
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        window = glfwCreateWindow(800, 600, getTitle(), NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Fenster konnte nicht erstellt werden.");
        }
        glfwSetFramebufferSizeCallback(window, (win, width, height) -> {
            glViewport(0, 0, width, height); // OpenGL sagen, dass die Zeichenfläche nun größer ist
            updateProjectionMatrix(width, height, projection); // Projection-Matrix neu berechnen
        });
        glfwMakeContextCurrent(window);
        GL.createCapabilities(); // Verbindet LWJGL mit dem OpenGL-Kontext
        glEnable(GL_DEPTH_TEST); // Damit vordere Würfel hintere verdecken
        glfwSwapInterval(1);     // Aktiviert V-Sync

        // Backface Culling: nur Seiten zeichnen, die zur Kamera schauen
//      glEnable(GL_CULL_FACE);
//      glCullFace(GL_BACK);

//      glPolygonMode(GL_FRONT_AND_BACK, GL_LINE); // Wireframe aktivieren
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        loadShader(); // Shader laden und kompilieren
        setupBackground();
        setupMain();
        setupBasePlate();
        loadLocations(); // Locations abfragen

        // Einmalig das Programm aktivieren
        glUseProgram(mainShaderProgram);
    }

    public final void loadShader() {
        try {
            mainShaderProgram = createShaderProgram(
                readFile( getMainShaderFilePath( VERTEX )), readFile( getMainShaderFilePath( FRAGMENT ))
            );
            loadAdditionalShader();
        } catch (IOException e) {
            throw new RuntimeException( "Lesen der Shader-Dateien fehlgeschlagen: " + e.getMessage() );
        }
    }

    public void loadLocations() {
        viewPosLoc      = glGetUniformLocation(mainShaderProgram, "viewPos");
        projLoc         = glGetUniformLocation(mainShaderProgram, "projection");
        viewLoc         = glGetUniformLocation(mainShaderProgram, "view");
        maxDepthLoc     = glGetUniformLocation(mainShaderProgram, "maxDepth");
        groundLoc       = glGetUniformLocation(mainShaderProgram, "uGroundMatrix");
        normalGroundLoc = glGetUniformLocation(mainShaderProgram, "uGroundNormalMatrix");
    }

    public void setupBasePlate() {
        groundVao = glGenVertexArrays();
        glBindVertexArray(groundVao);
        // VBO (Vertex Buffer Object) für die Vertices (Positionen) -> Daten-Container
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, CUBE_VERTICES, GL_STATIC_DRAW);
        // EBO für die Indices (Element Buffer Object): Die Reihenfolge, in der die Ecken zu Dreiecken verbunden werden (Indices) -> Verbindungs-Logik
        int ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, CUBE_INDICES, GL_STATIC_DRAW);
        // Position
        glEnableVertexAttribArray(0);
        int stride = 6 * Float.BYTES; // 3 Pos + 3 Normale
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        // Normale
        glEnableVertexAttribArray(5);
        glVertexAttribPointer(5, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glBindVertexArray(0); // entbinden
        // Matrizen vorbereiten
        groundMatrix = new Matrix4f()
            .translate(0, -0.5f, 0) // unter dem Nullpunkt, so dass der erste Würfel direkt aufsitzt
            .scale(20.0f, 0.1f, 20.0f); // sehr breit und flach
        // Normalenmatrix berechnen: Inverse -> Transponierte -> zu mat3 konvertieren
        normalGroundMatrix = getNormalMatrix( groundMatrix, new Matrix3f() );
    }

    public void processKeyBindings() {
        if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS) { glfwSetWindowShouldClose(window, true); }
        // Rotationen (Winkel) & Zoom (Abstand) der Kamera
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) { camYrot     -= 1.5f; }
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) { camYrot     += 1.5f; }
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) { pitch       -= 1.5f; }
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) { pitch       += 1.5f; }
        if (glfwGetKey(window, GLFW_KEY_Q) == GLFW_PRESS) { distance    -= 0.1f; }
        if (glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS) { distance    += 0.1f; }
        // Verschiebung Y der Kamera (X/Y) - CAVE: US-Tastaturlayout
        if (glfwGetKey(window, GLFW_KEY_Z) == GLFW_PRESS) { camYOffset  -= 0.1f; }
        if (glfwGetKey(window, GLFW_KEY_X) == GLFW_PRESS) { camYOffset  += 0.1f; }
        // Verschiebung X und Y (Pfeiltasten)
        if (glfwGetKey(window, GLFW_KEY_UP)    == GLFW_PRESS) { mainY += moveSpeed; }
        if (glfwGetKey(window, GLFW_KEY_DOWN)  == GLFW_PRESS) { mainY -= moveSpeed; }
        if (glfwGetKey(window, GLFW_KEY_LEFT)  == GLFW_PRESS) { mainX -= moveSpeed; }
        if (glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS) { mainX += moveSpeed; }
        // Verschiebung Z (plus/minus Numpad)
        if (glfwGetKey(window, GLFW_KEY_KP_ADD)      == GLFW_PRESS) { mainZ += moveSpeed; }
        if (glfwGetKey(window, GLFW_KEY_KP_SUBTRACT) == GLFW_PRESS) { mainZ -= moveSpeed; }
        // Zurücksetzen
        if (glfwGetKey(window, GLFW_KEY_HOME) == GLFW_PRESS) { resetPos(); }
        if (glfwGetKey(window, GLFW_KEY_R)    == GLFW_PRESS) { resetPos(); }
        // Pitch begrenzen (Vermeidet "Gimbal Lock" und Kopfstand)
        if (pitch >  89.0f) { pitch =  89.0f; }
        if (pitch < -89.0f) { pitch = -89.0f; }
        // Translation begrenzen
        mainY = Math.clamp(mainY, -10.0f, 10.0f);
        mainX = Math.clamp(mainX, -10.0f, 10.0f);
        mainZ = Math.clamp(mainZ, -10.0f, 10.0f);
    }

    // Methode optional, daher nicht abstract
    public void processMouseBindings() {}

    public void resetPos() {
        camYrot     = 0.0f;
        camYOffset  = 2.0f;
        pitch       = 20.0f;
        distance    = 7.0f;
        mainX       = 0.0f;
        mainY       = 0.0f;
        mainZ       = 0.0f;
    }

    // Methode optional, daher nicht abstract
    public void loadAdditionalShader() throws IOException {}

    // Methode optional, daher nicht abstract
    public void setupBackground() {}

    // Methode optional, daher nicht abstract
    public void drawBackground() {}

    public void renderloop() {
        // Projektions-Matrix (Sichtfeld 45°, Aspect Ratio 800/600)
        projection = new Matrix4f().perspective( org.joml.Math.toRadians(45.0f), 800.0f/600.0f, 0.1f, 100.0f );
        while ( !glfwWindowShouldClose( window )) {
            // --- FPS ermitteln ---
            time = glfwGetTime();
            frameCount++;
            // sobald eine Sekunde vergangen ist
            if (time-lastTime >= 1.0) {
                glfwSetWindowTitle( window, getTitle() + " | FPS: " + frameCount ); // FPS in den Titel schreiben
                frameCount = 0;
                lastTime = time;
            }

            setCamera(); // View/Kamera (Orbit-Kamera)
            processKeyBindings(); // Tastatur abfragen
            processMouseBindings(); // Maus abfragen

            // --- rendern ---
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // FrameBuffer leeren
            drawBackground(); // Hintergrund rendern (optional)
            glUseProgram(mainShaderProgram); // MainShader starten
            loadUniforms(); // Uniforms senden
            loadMaterial(); // Material-Daten senden
            drawMain(); // instanced rendering
            drawGroundPlate(); // Boden-Platte rendern

            // Abschluss
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    public void setCamera() {
        // Umrechnung in Radianten
        float radYrot  = (float) Math.toRadians(camYrot);
        float radPitch = (float) Math.toRadians(pitch);
        // Sphärische Koordinaten Berechnung
        camX = (float) (distance * Math.cos(radPitch) * Math.sin(radYrot));
        camY = (float) (distance * Math.sin(radPitch)) + camYOffset;
        camZ = (float) (distance * Math.cos(radPitch) * Math.cos(radYrot));
        view = new Matrix4f().lookAt(
            camX, camY, camZ,       // Position der Kamera
            0.0f, camYOffset, 0.0f, // Punkt, auf den die Kamera schaut
            0.0f, 1.0f, 0.0f        // Up-Vektor (Oben ist Y)
        );
    }

    // Methode optional, daher nicht abstract
    public void loadMaterial() {}

    public void loadUniforms() {
        glUniform1f(maxDepthLoc, maxDepth);
        glUniform3f(viewPosLoc, camX, camY, camZ);
        glUniformMatrix4fv(projLoc, false, projection.get( singleMatrixBuffer ));
        glUniformMatrix4fv(viewLoc, false, view.get( singleMatrixBuffer ));
    }

    public void drawGroundPlate() {
        /* Deaktiviere das Lesen aus dem VBO für Location 6, damit glVertexAttrib1f überhaupt Priorität bekommt:
         * Wenn ein Vertex-Attribut-Array aktiviert ist (glEnableVertexAttribArray),
         * zieht OpenGL die Daten immer aus dem Puffer (deinem matrixBuffer mit der Tiefe).
         * Der Befehl glVertexAttrib1f wird dann schlicht ignoriert.
         * Indem man es für den Boden ausschaltest, "hört" der Shader auf den festen Wert -1.0f.
         */
        glDisableVertexAttribArray(6); // schalten das Attribut-Array aus, damit glVertexAttrib1f Priorität hat
        glEnable(GL_BLEND); // Blending einschalten
        glDepthMask(false); // Objekte durch den Boden sichtbar
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); // Misch-Formel für Transparenz festlegen
        glBindVertexArray(groundVao); // kennt nur Positionen & Normalen

        // Matrix als Uniform an den Shader senden
        glUniformMatrix4fv( groundLoc, false, groundMatrix.get( new float[16] ));
        // Normalen-Matrix als Uniform an den Shader senden
        glUniformMatrix3fv( normalGroundLoc, false, normalGroundMatrix.get( new float[9] ));

        // Shader mitteilen, dass dies der Boden ist
        // Da wir keine Instanz-Daten nutzen, setzen wir ein generisches Attribut für aDepth (-1.0f)
        glVertexAttrib1f(6, -1.0f);

        // rendern (da wir das EBO im groundVao gebunden haben, glDrawElements)
        glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0);

        // aufräumen (Zustand für den nächsten Frame zurücksetzen)
        glDepthMask(true);
        glDisable(GL_BLEND); // Blending wieder ausschalten
        glBindVertexArray(0);
    }

    private void dispose() {
        // Ressourcen freigeben
        Callbacks.glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

}
