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

import static GLUtil.GLUtil.CUBE_INDICES;
import static GLUtil.GLUtil.CUBE_VERTICES;
import GLUtil.Material;
import GLUtil.Shader;
import static GLUtil.Shader.FRAGMENT;
import static GLUtil.Shader.VERTEX;
import java.nio.FloatBuffer;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL31.glDrawElementsInstanced;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;

public class PythagorasBaum3D extends AbstractGL3D {

    private final static String TITLE = "3D Pythagoras-Baum (OpenGL)";

    private final int[] matAmbLoc = new int[2], matDiffLoc = new int[2], matSpecLoc = new int[2], matShinLoc = new int[2];
    private final int maxInstances;
    private final float[] instanceDepths;
    private final Matrix4f[] instanceMatrices;
    private final Matrix4f rootMatrix = new Matrix4f();
    private final Matrix4f[] matrixStack;

    private int vao; // Vertex Array Object (Positionen der Eckpunkte)
    private int instanceVbo;
    private FloatBuffer matrixBuffer;
    private int matrixIndex = 0;

    public PythagorasBaum3D(int maxDepth) {
        super(maxDepth);
        maxInstances = (int)(( Math.pow( 4, maxDepth ) - 1 ) / 3 );
        instanceMatrices = new Matrix4f[maxInstances];
        instanceDepths = new float[maxInstances];
        for (int i = 0; i < maxInstances; ++i) {
            instanceMatrices[i] = new Matrix4f();
            instanceDepths[i]   = 0.0f;
        }
        matrixStack = new Matrix4f[maxDepth+1];
        for (int i = 0; i < matrixStack.length; i++) {
            matrixStack[i] = new Matrix4f();
        }
    }

    public static void main(String[] args) {
        if ( args == null || args.length == 0 ) {
            new PythagorasBaum3D(8).run();
        } else {
            new PythagorasBaum3D( Integer.parseInt( args[0] )).run();
        }
    }


    @Override
    public void loadMaterial() {
        Material.WOOD.setMaterial(matAmbLoc[0], matDiffLoc[0], matSpecLoc[0], matShinLoc[0]);
        Material.METALL.setMaterial(matAmbLoc[1], matDiffLoc[1], matSpecLoc[1], matShinLoc[1]);
    }

    @Override
    public String getMainShaderFile(Shader shader) {
        switch (shader) {
            case VERTEX:
                return "tree-vertex.shader";
            case FRAGMENT:
                return "tree-fragment.shader";
        }
        return "";
    }

    @Override
    public String getBGShaderFile(Shader shader) {
        switch (shader) {
            case VERTEX:
                return "bg-vertex.shader";
            case FRAGMENT:
                return "bg-fragment.shader";
        }
        return "";
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public void drawMain() {
        matrixIndex = 0; // Pool-Nutzung von vorn beginnen
        matrixBuffer.clear(); // Buffer leeren und zurücksetzen (Position auf 0)
        rootMatrix.identity().translate(mainX, mainY, mainZ); // Wurzel-Matrix mit der Verschiebung berechnen
        generateTree2(rootMatrix, maxDepth, time); // Baum-Matrizen (instanceMatrices) berechnen

        // Buffer für OpenGL "umklappen".
        // Da wir mit absoluten Indizes geschrieben haben, müssen wir das Limit manuell setzen
        matrixBuffer.limit(matrixIndex * 17);
        matrixBuffer.position(0);

        // GPU-Upload: Daten in das Instanz-VBO schieben
        glBindBuffer(GL_ARRAY_BUFFER, instanceVbo);
        // glBufferSubData, da die Größe des Buffers auf der GPU (glBufferData in init) bereits feststeht.
        glBufferSubData(GL_ARRAY_BUFFER, 0, matrixBuffer);

        // rendern
        glBindVertexArray(vao); // VAO (Vertex Array Object - Positionen der Eckpunkte) binden
        glEnableVertexAttribArray(6); // Lesen aus dem VBO für Location 6 (wieder) aktivieren
        glDrawElementsInstanced( GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0, instanceMatrices.length ); // Instanced Draw Call
    }

    @Override
    public void loadLocations() {
        super.loadLocations();
        matAmbLoc [0] = glGetUniformLocation(mainShaderProgram, "material_base.ambient");
        matDiffLoc[0] = glGetUniformLocation(mainShaderProgram, "material_base.diffuse");
        matSpecLoc[0] = glGetUniformLocation(mainShaderProgram, "material_base.specular");
        matShinLoc[0] = glGetUniformLocation(mainShaderProgram, "material_base.shininess");
        matAmbLoc [1] = glGetUniformLocation(mainShaderProgram, "material_top.ambient");
        matDiffLoc[1] = glGetUniformLocation(mainShaderProgram, "material_top.diffuse");
        matSpecLoc[1] = glGetUniformLocation(mainShaderProgram, "material_top.specular");
        matShinLoc[1] = glGetUniformLocation(mainShaderProgram, "material_top.shininess");
    }

    @Override
    public void setupMain() {
        matrixBuffer = BufferUtils.createFloatBuffer(maxInstances * 17);

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
        glBufferData(GL_ARRAY_BUFFER, maxInstances * 17 * Float.BYTES, GL_DYNAMIC_DRAW);

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

    private void generateTree(Matrix4f parentMatrix, int depth, double time) {
        if (depth == 0) {
            return;
        }

        // Matrix direkt in den Buffer schreiben (an der aktuellen Position)
        parentMatrix.get(matrixIndex * 17, matrixBuffer); // .get(index, buffer) kopiert die 16 Floats ohne Umwege
        // Tiefe (aDepth) direkt dahinter schreiben
        matrixBuffer.put(matrixIndex * 17 + 16, depth);
        matrixIndex++;

        // Abbruchbedingung für die Zweige/Kinder
        if (depth <= 1) {
            return;
        }

        // Animation: sanftes Schwingen: Sinus der Zeit: Basisneigung 45° + 5° Oszillation
        float angle = (float) Math.toRadians( 45 + Math.sin( time + depth ) * 5.0 );

        Matrix4f childMatrix = matrixStack[depth];
        // Wir erstellen 4 Kinder an den 4 Oberkanten
        for (int i = 0; i < 4; i++) {
            childMatrix.set(parentMatrix);

            // zur jeweiligen Oberkante des Elternwürfels gehen
            switch (i) {
                case 0: // rechts
                    childMatrix.translate(0.5f, 0.5f, 0.0f)
                               .rotateZ(-angle); // Nach rechts außen knicken
                    break;
                case 1: // links
                    childMatrix.translate(-0.5f, 0.5f, 0.0f)
                               .rotateZ(angle); // Nach links außen knicken
                    break;
                case 2: // vorne
                    childMatrix.translate(0.0f, 0.5f, 0.5f)
                               .rotateX(angle); // Nach vorne außen knicken
                    break;
                case 3: // hinten
                    childMatrix.translate(0.0f, 0.5f, -0.5f)
                               .rotateX(-angle); // Nach hinten außen knicken
                    break;
            }
            childMatrix.scale(0.5f); // Skalierung

            /* Kind-Würfel so verschieben, dass er mit seiner Unterkante
             * am Pivot-Punkt ansetzt (da er von -0.5 bis 0.5 geht, muss er um 0.5 hoch) */
            childMatrix.translate(0.0f, 0.5f, 0.0f);

            generateTree(childMatrix, depth-1, time); // Rekursion
        }
    }

    private void generateTree2(Matrix4f parentMatrix, int depth, double time) {
        if (depth <= 0) {
            return;
        }

        // Matrix direkt in den Buffer schreiben (an der aktuellen Position)
        parentMatrix.get(matrixIndex * 17, matrixBuffer); // .get(index, buffer) kopiert die 16 Floats
        // Tiefe (aDepth) direkt dahinter schreiben
        matrixBuffer.put(matrixIndex * 17 + 16, depth);
        matrixIndex++;

        // Abbruchbedingung für die Blätter
        if (depth <= 1) {
            return;
        }

        // Animation: sanftes Schwingen: Sinus der Zeit: Basisneigung 45° + 5° Oszillation
        float angle = (float) Math.toRadians( 45 + Math.sin( time + depth * 1.5f ) * 5.0f );
        Matrix4f childMatrix = matrixStack[depth];

        for (int i = 0; i < 4; i++) {
            childMatrix.set(parentMatrix);
            switch (i) {
                case 0: // rechts
                    childMatrix.translate(0.5f, 0.5f, 0.0f)     // Zur rechten Oberkante des Vaters
                               .rotateZ(-angle)                 // Scharnier-Rotation
                               .translate(-0.25f, 0.25f, 0.0f); // eigene rechte Kante zum Pivot
                    break;
                case 1: // links
                    childMatrix.translate(-0.5f, 0.5f, 0.0f).rotateZ(angle).translate(0.25f, 0.25f, 0.0f);
                    break;
                case 2: // vorne
                    childMatrix.translate(0.0f, 0.5f, 0.5f).rotateX(angle).translate(0.0f, 0.25f, -0.25f);
                    break;
                case 3: // hinten
                    childMatrix.translate(0.0f, 0.5f, -0.5f).rotateX(-angle).translate(0.0f, 0.25f, 0.25f);
                    break;
            }
            childMatrix.scale(0.5f); // Skalierung
            generateTree2(childMatrix, depth-1, time); // Rekursion
        }
    }

}
