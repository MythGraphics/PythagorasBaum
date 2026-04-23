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
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.Callbacks;
import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL31.glDrawElementsInstanced;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;
import static org.lwjgl.system.MemoryUtil.NULL;
import static pythagorasbaum.GLUtil.GLUtil.*;
import pythagorasbaum.GLUtil.Material;

public class PythagorasBaum3D {

    private final static String TITLE = "Pythagoras-Baum 3D (OpenGL)";
    private final static String SHADERPATH = "src/pythagorasbaum/";

    private final List<Matrix4f> instanceMatrices = new ArrayList<>();
    private final List<Float> instanceDepths = new ArrayList<>();
    private final int[] matAmbLoc = new int[2], matDiffLoc = new int[2], matSpecLoc = new int[2], matShinLoc = new int[2];
    private final int MAX_INSTANCES = 17*1000; // Kapazität für max. Tiefe 7
    private final float moveSpeed = 0.05f;
    private final float[] singleMatrixBuffer = new float[16];
    private final int maxDepth;

    private long window;
    private int vao, bgVao, groundVao; // Vertex Array Object (Positionen der Eckpunkte)
    private int instanceVbo;
    private int treeShaderProgram, bgShaderProgram;
    private int projLoc, viewLoc, viewPosLoc, maxDepthLoc, groundLoc, normalGroundLoc;
    private int frameCount = 0;
    private double lastTime = glfwGetTime();
    private FloatBuffer matrixBuffer;
    private Matrix4f projection;

    private float camYrot       = 0.0f;  // Drehung um die Y-Achse
    private float pitch         = 20.0f; // Oben/Unten
    private float distance      = 7.0f;  // Abstand zum Baum
    private float camYOffset    = 2.0f;  // Baummitte
    private float treeX         = 0.0f, treeY = 0.0f, treeZ = 0.0f;

    public PythagorasBaum3D(int maxDepth) {
        this.maxDepth = maxDepth;
        if (maxDepth < 1) {
            throw new IllegalArgumentException("maxDepth <1 unsinnig.");
        }
    }

    public static void main(String[] args) {
        if ( args == null || args.length == 0 ) {
            new PythagorasBaum3D(7).run();
        } else {
            new PythagorasBaum3D( Integer.parseInt( args[0] )).run();
        }
    }

    private void dispose() {
        // Ressourcen freigeben
        Callbacks.glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    public void run() {
        init();
        renderloop();
        dispose();
    }

    private void setupMesh() {
        // --- Setup Hintergrund ---
        bgVao = glGenVertexArrays();
        int bgVbo = glGenBuffers();

        glBindVertexArray(bgVao);
        glBindBuffer(GL_ARRAY_BUFFER, bgVbo);
        glBufferData(GL_ARRAY_BUFFER, BG_QUAD_VERTICES, GL_STATIC_DRAW);

        // BG: nur ein Attribut: Position (Location 0 im Shader)
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
        glBindVertexArray(0); // entbinden

        // --- Setup Baum ---
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        // VBO (Vertex Buffer Object) für die Vertices (Positionen) -> Daten-Container
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, CUBE_VERTICES, GL_STATIC_DRAW);

        glEnableVertexAttribArray(0);
        int stride = 6 * Float.BYTES; // 3 Pos + 3 Normale
        // Position: Location 0: im Shader: vec3 aPos
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        // Normale: Location 5
        glEnableVertexAttribArray(5);
        glVertexAttribPointer(5, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);

        // EBO für die Indices (Element Buffer Object): Die Reihenfolge, in der die Ecken zu Dreiecken verbunden werden (Indices) -> Verbindungs-Logik
        int ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, CUBE_INDICES, GL_STATIC_DRAW);

        // VBO (Vertex Buffer Object) für die Instanz-Matrizen (Location 1-4)
        instanceVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, instanceVbo);

        // Platz reservieren für MAX_INSTANCES Matrizen mit 17 Floats pro Matrix
        // Matrix belegt 16 Floats (4x4), die Tiefe 1 Float -> 17 Floats
        glBufferData(GL_ARRAY_BUFFER, MAX_INSTANCES * 17 * Float.BYTES, GL_DYNAMIC_DRAW);

        setupInstanceAttributes();
        glBindVertexArray(0); // entbinden

        // --- Setup Bodenplatte ---
        groundVao = glGenVertexArrays();
        glBindVertexArray(groundVao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo); // das existierende Baum-VBO nutzen
        glBufferData(GL_ARRAY_BUFFER, CUBE_VERTICES, GL_STATIC_DRAW);
        // Das EBO (Indices) vom Baum auch hier binden. Es definiert ja nur, welche Punkte zu Dreiecken verbunden werden.
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        // Position
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        // Normale
        glEnableVertexAttribArray(5);
        glVertexAttribPointer(5, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glBindVertexArray(0); // entbinden
    }

    private void setupInstanceAttributes() {
        // Matrix belegt 16 Floats (4x4), die Tiefe 1 Float = 17 Floats Gesamt-Schrittweite (stride)
        int stride = 17 * Float.BYTES;
        int offset = 0;
        for (int i = 0; i < 4; i++) {
            int location = 1+i; // nutzt Location 1, 2, 3, 4
            glEnableVertexAttribArray(location);
            // 4 Floats pro Location
            glVertexAttribPointer(location, 4, GL_FLOAT, false, stride, offset);
            // erhöht den Attribut-Index pro INSTANZ, nicht pro VERTEX
            glVertexAttribDivisor(location, 1);
            // Offset um eine Spalte (4 Floats) verschieben
            offset += 4 * Float.BYTES;
        }
        int depthLocation = 6;
        glEnableVertexAttribArray(depthLocation);
        glVertexAttribPointer(depthLocation, 1, GL_FLOAT, false, stride, 16*Float.BYTES); // Offset nach der Matrix
        glVertexAttribDivisor(depthLocation, 1); // Update pro Instanz
    }

    private void resetPos() {
        camYrot     = 0.0f;
        camYOffset  = 2.0f;
        pitch       = 20.0f;
        distance    = 7.0f;
        treeX       = 0.0f;
        treeY       = 0.0f;
        treeZ       = 0.0f;
    }

    private void init() {
        // Initialisierung von GLFW & Fenster
        if ( !glfwInit() ) {
            throw new IllegalStateException("GLFW konnte nicht geladen werden.");
        }

        // Konfiguriere das Fenster: OpenGL 3.3 Core Profile
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        window = glfwCreateWindow(800, 600, TITLE, NULL, NULL);
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
        glfwSwapInterval(1); // Aktiviert V-Sync
//      glPolygonMode(GL_FRONT_AND_BACK, GL_LINE); // Wireframe aktivieren
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        try {
            treeShaderProgram = createShaderProgram(
                readFile( SHADERPATH+"tree-vertex.shader" ), readFile( SHADERPATH+"tree-fragment.shader" )
            );
            bgShaderProgram = createShaderProgram(
                readFile( SHADERPATH+"bg-vertex.shader" ), readFile( SHADERPATH+"bg-fragment.shader" )
            );
        } catch (IOException e) {
            throw new RuntimeException( "Lesen der Shader-Dateien fehlgeschlagen: " + e.getMessage() );
        }

        matrixBuffer = BufferUtils.createFloatBuffer(MAX_INSTANCES * 16);
        setupMesh();

        // Locations abfragen
        viewPosLoc    = glGetUniformLocation(treeShaderProgram, "viewPos");
        projLoc       = glGetUniformLocation(treeShaderProgram, "projection");
        viewLoc       = glGetUniformLocation(treeShaderProgram, "view");
        maxDepthLoc   = glGetUniformLocation(treeShaderProgram, "maxDepth");
        matAmbLoc [0] = glGetUniformLocation(treeShaderProgram, "material_base.ambient");
        matDiffLoc[0] = glGetUniformLocation(treeShaderProgram, "material_base.diffuse");
        matSpecLoc[0] = glGetUniformLocation(treeShaderProgram, "material_base.specular");
        matShinLoc[0] = glGetUniformLocation(treeShaderProgram, "material_base.shininess");
        matAmbLoc [1] = glGetUniformLocation(treeShaderProgram, "material_top.ambient");
        matDiffLoc[1] = glGetUniformLocation(treeShaderProgram, "material_top.diffuse");
        matSpecLoc[1] = glGetUniformLocation(treeShaderProgram, "material_top.specular");
        matShinLoc[1] = glGetUniformLocation(treeShaderProgram, "material_top.shininess");

        // Einmalig das Programm aktivieren
        glUseProgram(bgShaderProgram);
        glUseProgram(treeShaderProgram);
    }

    private void drawBG() {
        glUseProgram(bgShaderProgram);
        glDisable(GL_DEPTH_TEST); // Tiefentest aus, damit es immer im Hintergrund ist
        glBindVertexArray(bgVao);
        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
        glEnable(GL_DEPTH_TEST);  // Tiefentest wieder an für den Rest
    }

    private void drawGroundPlate() {
        /* Deaktiviere das Lesen aus dem VBO für Location 6, damit glVertexAttrib1f überhaupt Priorität bekommt:
         * Wenn ein Vertex-Attribut-Array aktiviert ist (glEnableVertexAttribArray),
         * zieht OpenGL die Daten immer aus dem Puffer (deinem matrixBuffer mit der Tiefe).
         * Der Befehl glVertexAttrib1f wird dann schlicht ignoriert.
         * Indem man es für den Boden ausschaltest, "hört" der Shader auf den festen Wert -1.0f.
         */
        glDisableVertexAttribArray(6); // schalten das Attribut-Array aus, damit glVertexAttrib1f Priorität hat
        glEnable(GL_BLEND); // Blending einschalten
        glDepthMask(false); // Baum durch den Boden sichtbar
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); // Misch-Formel festlegen (Standard für Transparenz)

        glBindVertexArray(groundVao); // kennt nur Positionen & Normalen

        // Matrizen vorbereiten
        Matrix4f groundMatrix = new Matrix4f()
            .translate(0, -0.5f, 0) // unter dem Nullpunkt, so dass der erste Würfel direkt aufsitzt
            .scale(20.0f, 0.05f, 20.0f); // sehr breit und flach
        // Normalenmatrix berechnen: Inverse -> Transponierte -> zu mat3 konvertieren
        Matrix3f normalMatrix = getNormalMatrix( groundMatrix, new Matrix3f() );

        // Matrix als Uniform an den Shader senden
        groundLoc = glGetUniformLocation(treeShaderProgram, "uGroundMatrix");
        float[] buffer = new float[16];
        glUniformMatrix4fv( groundLoc, false, groundMatrix.get( buffer ));

        // Normalen-Matrix als Uniform an den Shader senden
        normalGroundLoc = glGetUniformLocation(treeShaderProgram, "uGroundNormalMatrix");
        float[] normalBuffer = new float[9];
        glUniformMatrix3fv(normalGroundLoc, false, normalMatrix.get( normalBuffer ));

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

    private void renderloop() {
        // Projektion (Sichtfeld 45°, Aspect Ratio 800/600)
        projection = new Matrix4f().perspective( org.joml.Math.toRadians(45.0f), 800.0f/600.0f, 0.1f, 100.0f );
        while ( !glfwWindowShouldClose( window )) {
            // --- FPS ermitteln ---
            double time = glfwGetTime();
            frameCount++;
            // sobald eine Sekunde vergangen ist
            if (time-lastTime >= 1.0) {
                glfwSetWindowTitle(window, TITLE + " | FPS: " + frameCount); // FPS in den Titel schreiben
                frameCount = 0;
                lastTime = time;
            }

            // --- View/Kamera (Orbit-Kamera) ---
            // Umrechnung in Radianten
            float radYrot  = (float) Math.toRadians(camYrot);
            float radPitch = (float) Math.toRadians(pitch);
            // Sphärische Koordinaten Berechnung
            float camX = (float) (distance * Math.cos(radPitch) * Math.sin(radYrot));
            float camY = (float) (distance * Math.sin(radPitch)) + camYOffset;
            float camZ = (float) (distance * Math.cos(radPitch) * Math.cos(radYrot));
            // View-Matrix
            Matrix4f view = new Matrix4f().lookAt(
                camX, camY, camZ,       // Position der Kamera
                0.0f, camYOffset, 0.0f, // Punkt, auf den die Kamera schaut (Mitte des Baums)
                0.0f, 1.0f, 0.0f        // Up-Vektor (Oben ist Y)
            );

            // --- Tastatur abfragen ---
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
            if (glfwGetKey(window, GLFW_KEY_UP)    == GLFW_PRESS) { treeY += moveSpeed; }
            if (glfwGetKey(window, GLFW_KEY_DOWN)  == GLFW_PRESS) { treeY -= moveSpeed; }
            if (glfwGetKey(window, GLFW_KEY_LEFT)  == GLFW_PRESS) { treeX -= moveSpeed; }
            if (glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS) { treeX += moveSpeed; }
            // Verschiebung Z (plus/minus Numpad)
            if (glfwGetKey(window, GLFW_KEY_KP_ADD)      == GLFW_PRESS) { treeZ += moveSpeed; }
            if (glfwGetKey(window, GLFW_KEY_KP_SUBTRACT) == GLFW_PRESS) { treeZ -= moveSpeed; }
            // Zurücksetzen
            if (glfwGetKey(window, GLFW_KEY_HOME) == GLFW_PRESS) { resetPos(); }
            if (glfwGetKey(window, GLFW_KEY_R)    == GLFW_PRESS) { resetPos(); }
            // Pitch begrenzen (Vermeidet "Gimbal Lock" und Kopfstand)
            if (pitch >  89.0f) { pitch =  89.0f; }
            if (pitch < -89.0f) { pitch = -89.0f; }
            // Translation begrenzen
            treeY = Math.clamp(treeY, -10.0f, 10.0f);
            treeX = Math.clamp(treeX, -10.0f, 10.0f);
            treeZ = Math.clamp(treeZ, -10.0f, 10.0f);

            instanceMatrices.clear();

            // Wurzel-Matrix mit der Verschiebung erstellen
            Matrix4f rootMatrix = new Matrix4f()
                .translate(treeX, treeY, treeZ);
            // Baum-Matrizen (instanceMatrices) berechnen
            generateTree3(rootMatrix, maxDepth, time);

            // Buffer leeren und zurücksetzen (Position auf 0)
            matrixBuffer.clear();

            // Matrix in den FloatBuffer (matrixBuffer) für die GPU laden
            for (int i = 0; i < instanceMatrices.size(); i++) {
                Matrix4f mat = instanceMatrices.get(i);
                float depthValue = instanceDepths.get(i);
                mat.get( matrixBuffer.position(), matrixBuffer );
                matrixBuffer.position( matrixBuffer.position() + 16 ); // 16 Werte schreiben
                matrixBuffer.put(depthValue); // 17. Wert: Tiefe
                // sicherstellen, dass wir nicht über die Kapazität hinausschießen
                if ( matrixBuffer.position() >= matrixBuffer.capacity() ) {
                    System.err.println("MatrixBuffer überschreitet Kapazitätsgrenze!");
                    break;
                }
            }
            // Buffer für OpenGL vorbereiten (Position auf 0, Limit auf aktuelle Position)
            matrixBuffer.flip();

            // GPU-Upload: Daten in das Instanz-VBO schieben
            glBindBuffer(GL_ARRAY_BUFFER, instanceVbo);
            // glBufferSubData, da die Größe des Buffers auf der GPU (glBufferData in init) bereits feststeht.
            glBufferSubData(GL_ARRAY_BUFFER, 0, matrixBuffer);

            // rendern vorbereiten
            glBindVertexArray(vao); // VAO (Vertex Array Object - Positionen der Eckpunkte) binden
            glEnableVertexAttribArray(6); // Lesen aus dem VBO für Location 6 (wieder) aktivieren

            // --- rendern ---
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // FrameBuffer leeren

            // Hintergrund rendern
            drawBG();

            // Shader starten (Baum)
            glUseProgram(treeShaderProgram);

            // Uniforms senden
            glUniform1f(maxDepthLoc, maxDepth);
            glUniform3f(viewPosLoc, camX, camY, camZ);
            glUniformMatrix4fv(projLoc, false, projection.get( singleMatrixBuffer ));
            glUniformMatrix4fv(viewLoc, false, view.get( singleMatrixBuffer ));

            // Material-Daten senden
            Material.WOOD.setMaterial(matAmbLoc[0], matDiffLoc[0], matSpecLoc[0], matShinLoc[0]);
            Material.METALL.setMaterial(matAmbLoc[1], matDiffLoc[1], matSpecLoc[1], matShinLoc[1]);

            // Instanced Rendering
            glBindVertexArray(vao); // VAO (Vertex Array Object - Positionen der Eckpunkte) binden
            glEnableVertexAttribArray(6); // Lesen aus dem VBO für Location 6 (wieder) aktivieren
            glDrawElementsInstanced( GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0, instanceMatrices.size() ); // Instanced Draw Call

            // Bodenplatte rendern
            drawGroundPlate();

            // Abschluss
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void generateTree(Matrix4f parentMatrix, int depth, double time) {
        if (depth == 0) {
            return;
        }

        // aktuelle Matrix zur Liste hinzufügen
        instanceMatrices.add( new Matrix4f( parentMatrix ));
        instanceDepths.add( (float) depth );

        float scaleFactor = 0.5f; // Die Kinder sind nur halb so groß

        // Animation: sanftes Schwingen: Sinus der Zeit: Basisneigung 45° + 5° Oszillation
        float windAngle = (float) Math.toRadians( 45 + Math.sin( time + depth ) * 5.0 );

        // 4 Kinder-Würfel erstellen, die nach außen geneigt sind
        for (int i = 0; i < 4; i++) {
            // Matrix des Elternwürfels
            Matrix4f childMatrix = new Matrix4f(parentMatrix);

            // zum oberen Ende des aktuellen Würfels schieben
            childMatrix.translate(0.0f, 0.5f, 0.0f);

            // Rotation um die Y-Achse (0, 90, 180, 270 Grad), um die 4 Seiten zu erreichen
            childMatrix.rotateY( (float) Math.toRadians( i * 90 ));
            childMatrix.rotateX(windAngle);

            // skalieren
            childMatrix.scale(scaleFactor);
//          childMatrix.scale(scaleFactor, scaleFactor * 1.5f, scaleFactor); // Länge der Würfel (y) um 50% erhöhen

            /* den neuen Würfel so verschieben, dass seine Unterseite (y=-0.5)
             * exakt auf dem Drehpunkt liegt. Da er skaliert ist, ist das wieder 0.5 */
            childMatrix.translate(0.0f, 0.5f, 0.0f);

            // Rekursion
            generateTree(childMatrix, depth-1, time);
        }
    }

    private void generateTree2(Matrix4f parentMatrix, int depth, double time) {
        if (depth == 0) {
            return;
        }

        // aktuelle Matrix zur Liste hinzufügen
        instanceMatrices.add( new Matrix4f( parentMatrix ));
        instanceDepths.add( (float) depth );

        // Abbruchbedingung für die Zweige
        if (depth <= 1) {
            return;
        }

        float scale = 0.5f; // Die Kinder sind nur halb so groß
        // Animation: sanftes Schwingen: Sinus der Zeit: Basisneigung 45° + 5° Oszillation
        float angle = (float) Math.toRadians( 45 + Math.sin( time + depth ) * 5.0 );

        // Wir erstellen 4 Kinder an den 4 Oberkanten
        for (int i = 0; i < 4; i++) {
            Matrix4f childMatrix = new Matrix4f(parentMatrix);

            // zur jeweiligen Oberkante des Elternwürfels gehen
            switch (i) {
                case 0: // rechts
                    childMatrix.translate(0.5f, 0.5f, 0.0f);
                    childMatrix.rotateZ(-angle); // Nach rechts außen knicken
                    break;
                case 1: // links
                    childMatrix.translate(-0.5f, 0.5f, 0.0f);
                    childMatrix.rotateZ(angle); // Nach links außen knicken
                    break;
                case 2: // vorne
                    childMatrix.translate(0.0f, 0.5f, 0.5f);
                    childMatrix.rotateX(angle); // Nach vorne außen knicken
                    break;
                case 3: // hinten
                    childMatrix.translate(0.0f, 0.5f, -0.5f);
                    childMatrix.rotateX(-angle); // Nach hinten außen knicken
                    break;
            }

            // Skalierung
            childMatrix.scale(scale);

            /* Kind-Würfel so verschieben, dass er mit seiner Unterkante
             * am Pivot-Punkt ansetzt (da er von -0.5 bis 0.5 geht, muss er um 0.5 hoch) */
            childMatrix.translate(0.0f, 0.5f, 0.0f);

            // Rekursion
            generateTree2(childMatrix, depth-1, time);
        }
    }

    private void generateTree3(Matrix4f parentMatrix, int depth, double time) {
        if (depth == 0) {
            return;
        }

        // aktuelle Matrix zur Liste hinzufügen
        instanceMatrices.add( new Matrix4f( parentMatrix ));
        instanceDepths.add( (float) depth );

        if (depth <= 1) {
            return;
        }

        float scale = 0.5f;
        // Animation: sanftes Schwingen: Sinus der Zeit: Basisneigung 45° + 5° Oszillation
        float angle = (float) Math.toRadians( 45 + Math.sin( time + depth * 1.5f ) * 5.0f );

        for (int i = 0; i < 4; i++) {
            Matrix4f childMatrix = new Matrix4f(parentMatrix);

            switch (i) {
                case 0: // rechts
                    childMatrix.translate(0.5f, 0.5f, 0.0f);    // Zur rechten Oberkante des Vaters
                    childMatrix.rotateZ(-angle);                // Scharnier-Rotation
                    childMatrix.translate(-0.25f, 0.25f, 0.0f); // eigene rechte Kante zum Pivot
                    break;
                case 1: // links
                    childMatrix.translate(-0.5f, 0.5f, 0.0f);   // zur linken Oberkante des Vaters
                    childMatrix.rotateZ(angle);
                    childMatrix.translate(0.25f, 0.25f, 0.0f);  // eigene linke Kante zum Pivot
                    break;
                case 2: // vorne
                    childMatrix.translate(0.0f, 0.5f, 0.5f);    // zur vorderen Oberkante
                    childMatrix.rotateX(angle);
                    childMatrix.translate(0.0f, 0.25f, -0.25f); // eigene vordere Kante zum Pivot
                    break;
                case 3: // hinten
                    childMatrix.translate(0.0f, 0.5f, -0.5f);   // zur hinteren Oberkante
                    childMatrix.rotateX(-angle);
                    childMatrix.translate(0.0f, 0.25f, 0.25f);  // eigene hintere Kante zum Pivot
                    break;
            }

            // Skalierung
            childMatrix.scale(scale);

            // Rekursion
            generateTree3(childMatrix, depth-1, time);
        }
    }

}
