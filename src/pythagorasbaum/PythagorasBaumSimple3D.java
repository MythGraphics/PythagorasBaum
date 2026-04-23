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
import static pythagorasbaum.GLUtil.Shader.*;
import pythagorasbaum.GLUtil.*;

public class PythagorasBaumSimple3D extends AbstractGL3D {

    private final static String TITLE = "3D-Pythagoras-Baum, simple (OpenGL)";
    private final static String SHADERPATH = "src/pythagorasbaum/";

    private final int MAX_INSTANCES = 10*1000;
    private final List<Matrix4f> instanceMatrices = new ArrayList<>();
    private final List<Float> instanceDepths = new ArrayList<>();

    private int vao; // Vertex Array Object (Positionen der Eckpunkte)
    private int instanceVbo;
    private int matAmbLoc, matDiffLoc, matSpecLoc, matShinLoc;
    private int treeShaderProgram;
    private FloatBuffer matrixBuffer;

    public PythagorasBaumSimple3D(int maxDepth) {
        super(maxDepth);
    }

    public static void main(String[] args) {
        if ( args == null || args.length == 0 ) {
            new PythagorasBaumSimple3D(7).run();
        } else {
            new PythagorasBaumSimple3D( Integer.parseInt( args[0] )).run();
        }
    }

    @Override
    public void loadMaterial() {
        Material.METALL.setMaterial(matAmbLoc, matDiffLoc, matSpecLoc, matShinLoc);
    }

    @Override
    public String getMainShaderFilePath(Shader shader) {
        switch (shader) {
            case VERTEX:
                return SHADERPATH + "tree-vertex.shader";
            case FRAGMENT:
                return SHADERPATH + "tree-fragment.shader";
        }
        return "";
    }

    @Override
    public String getBGShaderFilePath(Shader shader) {
        switch (shader) {
            case VERTEX:
                return SHADERPATH + "bg-vertex.shader";
            case FRAGMENT:
                return SHADERPATH + "bg-fragment.shader";
        }
        return "";
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public void drawMain() {
        instanceMatrices.clear();

        // Wurzel-Matrix mit der Verschiebung erstellen
        Matrix4f rootMatrix = new Matrix4f()
            .translate(mainX, mainY, mainZ);
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

        glBindVertexArray(vao); // VAO (Vertex Array Object - Positionen der Eckpunkte) binden
        glEnableVertexAttribArray(6); // Lesen aus dem VBO für Location 6 (wieder) aktivieren
        glDrawElementsInstanced( GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0, instanceMatrices.size() ); // Instanced Draw Call
    }

    private void generateTree(Matrix4f parentMatrix, int depth, double time) {
        if (depth == 0) {
            return;
        }
        // ToDo hier weiter
    }

    @Override
    public void loadLocations() {
        super.loadLocations();
        matAmbLoc  = glGetUniformLocation(treeShaderProgram, "material_base.ambient");
        matDiffLoc = glGetUniformLocation(treeShaderProgram, "material_base.diffuse");
        matSpecLoc = glGetUniformLocation(treeShaderProgram, "material_base.specular");
        matShinLoc = glGetUniformLocation(treeShaderProgram, "material_base.shininess");
    }

    @Override
    public void setupMain() {
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

        setupInstanceAttributes();
    }

    public void setupInstanceAttributes() {
        // VBO (Vertex Buffer Object) für die Instanz-Matrizen (Location 1-4)
        instanceVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, instanceVbo);

        // Platz reservieren für MAX_INSTANCES Matrizen mit 17 Floats pro Matrix
        // Matrix belegt 16 Floats (4x4), Tiefe 1 Float -> 17 Floats
        glBufferData(GL_ARRAY_BUFFER, MAX_INSTANCES * 17 * Float.BYTES, GL_DYNAMIC_DRAW);

        // Matrix belegt 16 Floats (4x4), die Tiefe 1 Float = 17 Floats Gesamt-Schrittweite (stride)
        int instanceStride = 17 * Float.BYTES;
        int offset = 0;
        for (int i = 0; i < 4; i++) {
            int location = 1+i; // nutzt Location 1, 2, 3, 4
            glEnableVertexAttribArray(location);
            // 4 Floats pro Location
            glVertexAttribPointer(location, 4, GL_FLOAT, false, instanceStride, offset);
            // erhöht den Attribut-Index pro INSTANZ, nicht pro VERTEX
            glVertexAttribDivisor(location, 1);
            // Offset um eine Spalte (4 Floats) verschieben
            offset += 4 * Float.BYTES;
        }
        int depthLocation = 6;
        glEnableVertexAttribArray(depthLocation);
        glVertexAttribPointer(depthLocation, 1, GL_FLOAT, false, instanceStride, 16*Float.BYTES); // Offset nach der Matrix
        glVertexAttribDivisor(depthLocation, 1); // Update pro Instanz
        glBindVertexArray(0); // entbinden
    }

}
