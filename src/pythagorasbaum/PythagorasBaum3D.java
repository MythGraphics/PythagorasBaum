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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
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

public class PythagorasBaum3D {

    public enum Material {
        METALL (
            new float[] {0.3f, 0.3f, 0.3f}, // Anteil r, g, b (1.0f == 100%)
            new float[] {0.4f, 0.4f, 0.4f},
            new float[] {1.0f, 1.0f, 1.0f},
            64.0f
        ),
        WOOD (
            new float[] {0.1f, 0.1f, 0.1f},
            new float[] {0.6f, 0.6f, 0.6f},
            new float[] {0.05f, 0.05f, 0.05f},
            2.0f
        ),
        PLASTIC (
            new float[] {0.2f, 0.2f, 0.2f},
            new float[] {1.0f, 1.0f, 1.0f},
            new float[] {0.8f, 0.8f, 0.8f},
            32.0f
        );

        final float[] ambient;
        final float[] diffuse;
        final float[] specular;
        final float shininess;

        Material(float[] ambient, float[] diffuse, float[] specular, float shininess) {
            this.ambient   = ambient;
            this.diffuse   = diffuse;
            this.specular  = specular;
            this.shininess = shininess;
        }

        public void setMaterial(int matAmbLoc, int matDiffLoc, int matSpecLoc, int matShinLoc) {
            glUniform3f(matAmbLoc,  ambient[0],  ambient[1],  ambient[2]);
            glUniform3f(matDiffLoc, diffuse[0],  diffuse[1],  diffuse[2]);
            glUniform3f(matSpecLoc, specular[0], specular[1], specular[2]);
            glUniform1f(matShinLoc, shininess);
        }
    }

    private final static String TITLE = "Pythagoras-Baum 3D (OpenGL)";

    private final List<Matrix4f> instanceMatrices = new ArrayList<>();
    private final List<Float> instanceDepths = new ArrayList<>();
    private final int MAX_INSTANCES = 17*1000; // Kapazität Tiefe max. 7
    private final float moveSpeed = 0.05f;
    private final int maxDepth;

    private long window;
    private int vao; // Vertex Array Object (Positionen der Eckpunkte)
    private int instanceVbo, shaderProgram;
    private int projLoc, viewLoc, viewPosLoc, maxDepthLoc;
    private int matAmbLoc, matDiffLoc, matSpecLoc, matShinLoc;
    private int frameCount = 0;
    private double lastTime = glfwGetTime();
    private FloatBuffer matrixBuffer;
    private float camYrot   = 0.0f;  // Drehung um die Y-Achse
    private float pitch     = 20.0f; // Oben/Unten
    private float distance  = 7.0f;  // Abstand zum Baum
    private float treeX     = 0.0f, treeY = 0.0f, treeZ = 0.0f;

    public PythagorasBaum3D(int maxDepth) {
        this.maxDepth = maxDepth;
        if (maxDepth < 1) {
            throw new IllegalArgumentException("maxDepth <1 unsinnig.");
        }
    }

    public static void main(String[] args) {
        new PythagorasBaum3D(3).run();
    }

    private static String readFile(String filename) throws IOException {
        return Files.readString( Path.of( "src/pythagorasbaum/"+filename ));
    }

    private void dispose() {
        // Ressourcen freigeben
        Callbacks.glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    public void run() {
        init();
        loop();
        dispose();
    }

    private void setupMesh() {
        float[] vertices = {
            // Positionen            // Normalen
            // Vorne (Z+)
            -0.5f, -0.5f,  0.5f,     0.0f,  0.0f,  1.0f,
             0.5f, -0.5f,  0.5f,     0.0f,  0.0f,  1.0f,
             0.5f,  0.5f,  0.5f,     0.0f,  0.0f,  1.0f,
            -0.5f,  0.5f,  0.5f,     0.0f,  0.0f,  1.0f,
            // Hinten (Z-)
            -0.5f, -0.5f, -0.5f,     0.0f,  0.0f, -1.0f,
            -0.5f,  0.5f, -0.5f,     0.0f,  0.0f, -1.0f,
             0.5f,  0.5f, -0.5f,     0.0f,  0.0f, -1.0f,
             0.5f, -0.5f, -0.5f,     0.0f,  0.0f, -1.0f,
            // Oben (Y+)
            -0.5f,  0.5f, -0.5f,     0.0f,  1.0f,  0.0f,
            -0.5f,  0.5f,  0.5f,     0.0f,  1.0f,  0.0f,
             0.5f,  0.5f,  0.5f,     0.0f,  1.0f,  0.0f,
             0.5f,  0.5f, -0.5f,     0.0f,  1.0f,  0.0f,
            // Unten (Y-)
            -0.5f, -0.5f, -0.5f,     0.0f, -1.0f,  0.0f,
             0.5f, -0.5f, -0.5f,     0.0f, -1.0f,  0.0f,
             0.5f, -0.5f,  0.5f,     0.0f, -1.0f,  0.0f,
            -0.5f, -0.5f,  0.5f,     0.0f, -1.0f,  0.0f,
            // Rechts (X+)
             0.5f, -0.5f, -0.5f,     1.0f,  0.0f,  0.0f,
             0.5f,  0.5f, -0.5f,     1.0f,  0.0f,  0.0f,
             0.5f,  0.5f,  0.5f,     1.0f,  0.0f,  0.0f,
             0.5f, -0.5f,  0.5f,     1.0f,  0.0f,  0.0f,
            // Links (X-)
            -0.5f, -0.5f, -0.5f,    -1.0f,  0.0f,  0.0f,
            -0.5f, -0.5f,  0.5f,    -1.0f,  0.0f,  0.0f,
            -0.5f,  0.5f,  0.5f,    -1.0f,  0.0f,  0.0f,
            -0.5f,  0.5f, -0.5f,    -1.0f,  0.0f,  0.0f
        };
        // 12 Dreiecke (2 pro Würfelseite)
        int[] indices = {
            0,  1,  2,   2,  3,  0, // Vorne
            4,  5,  6,   6,  7,  4, // Hinten
            8,  9, 10,  10, 11,  8, // Oben
           12, 13, 14,  14, 15, 12, // Unten
           16, 17, 18,  18, 19, 16, // Rechts
           20, 21, 22,  22, 23, 20  // Links
       };

        // OpenGL sagen, dass die Matrix-Daten im zweiten Buffer pro Instanz (also pro Ast) gelesen werden sollen, nicht pro Eckpunkt
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        // VBO (Vertex Buffer Object) für die Vertices (Positionen) -> Daten-Container
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        int stride = 6 * Float.BYTES; // 3 Pos + 3 Normale
        // Position: Location 0: im Shader: vec3 aPos
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        // Normale: Location 5
        glEnableVertexAttribArray(5);
        glVertexAttribPointer(5, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);

        // EBO für die Indices (Element Buffer Object): Die Reihenfolge, in der die Ecken zu Dreiecken verbunden werden (Indices) -> Verbindungs-Logik
        int eboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        // VBO (Vertex Buffer Object) für die Instanz-Matrizen (Location 1-4)
        instanceVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, instanceVbo);

        // Platz reservieren für MAX_INSTANCES Matrizen mit 17 floats pro Matrix
        glBufferData(GL_ARRAY_BUFFER, MAX_INSTANCES * 17 * Float.BYTES, GL_DYNAMIC_DRAW);

        setupInstanceAttributes();

        // alles entbinden (Sicherheitshalber)
        glBindVertexArray(0);
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
        camYrot  = 0.0f;
        pitch    = 20.0f;
        distance = 7.0f;
        treeX    = 0.0f;
        treeY    = 0.0f;
        treeZ    = 0.0f;
    }

    private void init() {
        // Initialisierung von GLFW & Fenster
        if ( !glfwInit() ) {
            throw new IllegalStateException("GLFW konnte nicht geladen werden");
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
            glViewport(0, 0, width, height);
            // Hier müsste die projection Matrix idealerweise neu berechnet werden
        });

        glfwMakeContextCurrent(window);
        GL.createCapabilities(); // Verbindet LWJGL mit dem OpenGL-Kontext
        glEnable(GL_DEPTH_TEST); // Damit vordere Würfel hintere verdeckt
        glfwSwapInterval(1); // Aktiviert V-Sync
//      glPolygonMode(GL_FRONT_AND_BACK, GL_LINE); // Wireframe aktivieren
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        try {
            shaderProgram = createShaderProgram( readFile( "vertex.shader" ), readFile( "fragment.shader" ));
        } catch (IOException e) {
            throw new RuntimeException( "Lesen der Shader-Dateien fehlgeschlagen: " + e.getMessage() );
        }

        matrixBuffer = BufferUtils.createFloatBuffer(MAX_INSTANCES * 16);
        setupMesh();

        // Locations abfragen
        viewPosLoc  = glGetUniformLocation(shaderProgram, "viewPos");
        projLoc     = glGetUniformLocation(shaderProgram, "projection");
        viewLoc     = glGetUniformLocation(shaderProgram, "view");
        maxDepthLoc = glGetUniformLocation(shaderProgram, "maxDepth");
        matAmbLoc   = glGetUniformLocation(shaderProgram, "material.ambient");
        matDiffLoc  = glGetUniformLocation(shaderProgram, "material.diffuse");
        matSpecLoc  = glGetUniformLocation(shaderProgram, "material.specular");
        matShinLoc  = glGetUniformLocation(shaderProgram, "material.shininess");

        // Einmalig das Programm aktivieren
        glUseProgram(shaderProgram);
    }

    private void checkShaderError(int shaderId) {
        if ( glGetShaderi( shaderId, GL_COMPILE_STATUS ) == GL_FALSE ) {
            System.err.println( "Shader-Fehler: " + glGetShaderInfoLog( shaderId ));
        }
    }

    private int createShaderProgram(String vertexCode, String fragmentCode) {
        // Vertex Shader kompilieren
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexCode);
        glCompileShader(vertexShader);
        checkShaderError(vertexShader);

        // Fragment Shader kompilieren
        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentCode);
        glCompileShader(fragmentShader);
        checkShaderError(fragmentShader);

        // Programm verknüpfen (Linken)
        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);

        // Shader-Objekte können nach dem Linken gelöscht werden
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        return program;
    }

    private void loop() {
        // Projektion (Sichtfeld 45°, Aspect Ratio 800/600)
        Matrix4f projection = new Matrix4f().perspective( org.joml.Math.toRadians(45.0f), 800.0f/600.0f, 0.1f, 100.0f );
        float[] singleMatrixBuffer = new float[16];
        while ( !glfwWindowShouldClose( window )) {
            // FPS ermitteln
            double time = (float) glfwGetTime();
            frameCount++;
            // Sobald eine Sekunde vergangen ist
            if (time-lastTime >= 1.0) {
                glfwSetWindowTitle(window, TITLE + " | FPS: " + frameCount); // FPS in den Titel schreiben
                frameCount = 0;
                lastTime = time;
            }

            // --- Tastatur abfragen ---
            // Rotationen (Winkel) & Zoom (Abstand) der Kamera
            if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) { camYrot  -= 1.5f; }
            if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) { camYrot  += 1.5f; }
            if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) { pitch    -= 1.5f; }
            if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) { pitch    += 1.5f; }
            if (glfwGetKey(window, GLFW_KEY_Q) == GLFW_PRESS) { distance -= 0.1f; }
            if (glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS) { distance += 0.1f; }
            // Pitch begrenzen (Vermeidet "Gimbal Lock" und Kopfstand)
            if (pitch >  89.0f) { pitch =  89.0f; }
            if (pitch < -89.0f) { pitch = -89.0f; }
            // Verschiebung X und Y (Pfeiltasten)
            if (glfwGetKey(window, GLFW_KEY_UP)    == GLFW_PRESS) { treeY += moveSpeed; }
            if (glfwGetKey(window, GLFW_KEY_DOWN)  == GLFW_PRESS) { treeY -= moveSpeed; }
            if (glfwGetKey(window, GLFW_KEY_LEFT)  == GLFW_PRESS) { treeX -= moveSpeed; }
            if (glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS) { treeX += moveSpeed; }
            // Verschiebung Z (plus/minus)
            if (glfwGetKey(window, GLFW_KEY_KP_ADD)      == GLFW_PRESS ||
                glfwGetKey(window, GLFW_KEY_EQUAL)       == GLFW_PRESS) { treeZ += moveSpeed; }
            if (glfwGetKey(window, GLFW_KEY_KP_SUBTRACT) == GLFW_PRESS ||
                glfwGetKey(window, GLFW_KEY_MINUS)       == GLFW_PRESS) { treeZ -= moveSpeed; }
            // Reset
            if (glfwGetKey(window, GLFW_KEY_HOME) == GLFW_PRESS) { resetPos(); }
            // Translation begrenzen
            treeY = Math.clamp(treeY, -10.0f, 10.0f);
            treeX = Math.clamp(treeX, -10.0f, 10.0f);
            treeZ = Math.clamp(treeZ, -10.0f, 10.0f);

            instanceMatrices.clear();
            Material.METALL.setMaterial(matAmbLoc, matDiffLoc, matSpecLoc, matShinLoc);

            // Boden-Matrix erstellen und hinzufügen: flacher, großer Würfel
            Matrix4f groundMatrix = new Matrix4f()
                .translate(0, -0.05f, 0) // knapp unter dem Nullpunkt
                .scale(20.0f, 0.05f, 20.0f); // sehr breit und flach
            instanceMatrices.add(groundMatrix);
            instanceDepths.add(-1.0f); // spezieller Tiefenwert für den Boden

            // Wurzel-Matrix mit der Verschiebung erstellen
            Matrix4f rootMatrix = new Matrix4f()
                .translate(treeX, treeY, treeZ);
            // Baum-Matrizen (instanceMatrices) berechnen
            generateTree(rootMatrix, maxDepth, time);

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
                    System.err.println("matrixBuffer überschreitet Kapazitätsgrenze!");
                    break;
                }
            }
            // Buffer für OpenGL vorbereiten (Position auf 0, Limit auf aktuelle Position)
            matrixBuffer.flip();

            // GPU-Upload: Daten in das Instanz-VBO schieben
            glBindBuffer(GL_ARRAY_BUFFER, instanceVbo);
            // glBufferSubData, da die Größe des Buffers auf der GPU (glBufferData in init) bereits feststeht.
            glBufferSubData(GL_ARRAY_BUFFER, 0, matrixBuffer);

            // rendern
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glUseProgram(shaderProgram); // Shader starten

            // View (Orbit-Kamera)
            // Umrechnung in Radianten
            float radYrot  = (float) Math.toRadians(camYrot);
            float radPitch = (float) Math.toRadians(pitch);
            // Sphärische Koordinaten Berechnung
            float camX = (float) (distance * Math.cos(radPitch) * Math.sin(radYrot));
            float camY = (float) (distance * Math.sin(radPitch)) + 2.0f; // +2.0f um auf Baummitte zu schauen
            float camZ = (float) (distance * Math.cos(radPitch) * Math.cos(radYrot));
            // View-Matrix
            Matrix4f view = new Matrix4f().lookAt(camX, camY, camZ, 0, 2, 0, 0, 1, 0);

            // Uniforms senden
            glUniform1f(maxDepthLoc, maxDepth);
            glUniform3f(viewPosLoc, camX, camY, camZ);
            glUniformMatrix4fv(projLoc, false, projection.get( singleMatrixBuffer ));
            glUniformMatrix4fv(viewLoc, false, view.get( singleMatrixBuffer ));

            // VAO binden und Instanced Draw Call
            glBindVertexArray(vao);
            glDrawElementsInstanced( GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0, instanceMatrices.size() );  // Instanced Rendering

            // Abschluss
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    public void generateTree(Matrix4f parentMatrix, int depth, double time) {
        if (depth == 0) {
            return;
        }

        // aktuelle Matrix zur Liste hinzufügen
        instanceMatrices.add( new Matrix4f( parentMatrix ));
        instanceDepths.add( (float) depth );

        // Parameter für die Kinder
        float scaleFactor = 0.5f; // bei 4 Kindern füllen sie exakt die Fläche aus
        // Animation: sanftes Schwingen: Sinus der Zeit: Basisneigung 45° + 5° Oszillation
        float windAngle = (float) Math.toRadians( 45 + Math.sin( time + depth ) * 5.0 );

        // Wir erstellen 4 Kinder, die nach "außen" geneigt sind
        for (int i = 0; i < 4; i++) {
            // Starte mit der Matrix des Elternteils
            Matrix4f childMatrix = new Matrix4f(parentMatrix);

            // Zum oberen Ende des aktuellen Würfels schieben
            childMatrix.translate(0, 0.5f, 0);

            // Rotation um die Y-Achse (0, 90, 180, 270 Grad), um die 4 Seiten zu erreichen
            childMatrix.rotateY( (float) Math.toRadians( i * 90 ));
            childMatrix.rotateX(windAngle);

            // Skalieren und den neuen Würfel so verschieben, dass er auf der Kante aufsitzt
//          childMatrix.scale(scaleFactor);
            childMatrix.scale(scaleFactor, 1.0f, scaleFactor); // nur X und Z skalieren

            // den neuen Würfel so verschieben, dass seine Unterseite (y=-0.5)
            // exakt auf dem Drehpunkt liegt. Da er skaliert ist, ist das wieder 0.5
            childMatrix.translate(0, 0.5f, 0);

            // Rekursion
            generateTree(childMatrix, depth-1, time);
        }
    }

}
