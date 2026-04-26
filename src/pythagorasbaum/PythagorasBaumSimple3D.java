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
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL31.glDrawElementsInstanced;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;
import static pythagorasbaum.GLUtil.GLUtil.CUBE_INDICES;
import static pythagorasbaum.GLUtil.GLUtil.CUBE_VERTICES;
import pythagorasbaum.GLUtil.Material;
import pythagorasbaum.GLUtil.Shader;
import static pythagorasbaum.GLUtil.Shader.FRAGMENT;
import static pythagorasbaum.GLUtil.Shader.VERTEX;

public class PythagorasBaumSimple3D extends AbstractGL3D {

    private final static String TITLE = "3D Pythagoras-Baum aus Würfeln (OpenGL)";

    private final int maxInstances;
    private final Matrix4f rootMatrix = new Matrix4f();
    private final Matrix4f[] matrixStack;
    private final TreePool treePool;
    private final Vector3f vec = new Vector3f();
    private final Vector3f camPos = new Vector3f();

    private int vao; // Vertex Array Object (Positionen der Eckpunkte)
    private int instanceVbo;
    private int matAmbLoc, matDiffLoc, matSpecLoc, matShinLoc;
    private FloatBuffer matrixBuffer;

    public PythagorasBaumSimple3D(int maxDepth) {
        super(maxDepth);
        maxInstances = (int)(( Math.pow( 2, maxDepth+1 )));
        treePool = new TreePool(maxInstances);
        matrixStack = new Matrix4f[maxDepth+1];
        for (int i = 0; i < matrixStack.length; i++) {
            matrixStack[i] = new Matrix4f();
        }
    }

    public static void main(String[] args) {
        if ( args == null || args.length == 0 ) {
            new PythagorasBaumSimple3D(10).run();
        } else {
            new PythagorasBaumSimple3D( Integer.parseInt( args[0] )).run();
        }
    }

    @Override
    public void loadMaterial() {
        Material.METALL.setMaterial(matAmbLoc, matDiffLoc, matSpecLoc, matShinLoc);
    }

    @Override
    public String getMainShaderFile(Shader shader) {
        switch (shader) {
            case VERTEX:
                return "simple-tree-vertex.shader";
            case FRAGMENT:
                return "simple-tree-fragment.shader";
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
        glEnable(GL_BLEND); // Blending einschalten
        glBlendFunc(GL_SRC_ALPHA, GL_ONE); // Misch-Formel für Transparenz festlegen
//      glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_DST_COLOR); // Misch-Formel für Transparenz festlegen

        view.origin(camPos); // Kamera-Position aus der View-Matrix berechnen
        matrixBuffer.clear(); // Buffer leeren und zurücksetzen (Position auf 0)
        for (int i = 0; i < treePool.size(); i++) {
            int sortedIdx = treePool.indices[i];
            treePool.matrices[sortedIdx].get(matrixBuffer); // 16 Floats
            matrixBuffer.position(matrixBuffer.position() + 16);
            matrixBuffer.put( treePool.depths[sortedIdx] ); // 17. Float
        }
        matrixBuffer.flip();
        treePool.reset(); // Pool zurücksetzen

        rootMatrix.identity().translate(mainX, mainY, mainZ); // Wurzel-Matrix mit der Verschiebung berechnen
        generateTree(rootMatrix, maxDepth, time); // Baum-Matrizen (instanceMatrices) berechnen

        // GPU-Upload: Daten in das Instanz-VBO schieben
        glBindBuffer(GL_ARRAY_BUFFER, instanceVbo);
        // glBufferSubData, da die Größe des Buffers auf der GPU (glBufferData in init) bereits feststeht.
        glBufferSubData(GL_ARRAY_BUFFER, 0, matrixBuffer);

        // rendern
        glBindVertexArray(vao); // VAO (Vertex Array Object - Positionen der Eckpunkte) binden
        glEnableVertexAttribArray(6); // Lesen aus dem VBO für Location 6 (wieder) aktivieren
        glDrawElementsInstanced( GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0, treePool.size() ); // Instanced Draw Call

        // Aufräumen
        glDisable(GL_BLEND);
    }

    @Override
    public void loadLocations() {
        super.loadLocations();
        matAmbLoc  = glGetUniformLocation(mainShaderProgram, "material_base.ambient");
        matDiffLoc = glGetUniformLocation(mainShaderProgram, "material_base.diffuse");
        matSpecLoc = glGetUniformLocation(mainShaderProgram, "material_base.specular");
        matShinLoc = glGetUniformLocation(mainShaderProgram, "material_base.shininess");
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

    private void setupInstanceAttributes() {
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
        if (depth <= 0) {
            return;
        }

        // Distanz berechnen (Zero-Allocation mit JOML)
        float distSq = parentMatrix.getTranslation(vec).distanceSquared(camPos);
        treePool.add(parentMatrix, distSq, depth);

        // Abbruchbedingung für die Blätter
        if (depth <= 1) {
            return;
        }

        // Animation: sanftes Schwingen: Sinus der Zeit: Basisneigung 45° + 5° Oszillation
//      float alpha = (float) Math.toRadians( 45 + Math.sin( time + depth * 1.5f ) * 5.0f );
        float alpha = (float) Math.toRadians( 45 + Math.sin( time ) * 5.0f );
        float beta  = (float) (Math.PI / 2.0 - alpha);
        float scaleLeft  = (float) Math.cos(alpha);
        float scaleRight = (float) Math.sin(alpha);
        Matrix4f childMatrix = matrixStack[depth];

        for (int i = 0; i < 2; i++) {
            childMatrix.set(parentMatrix);
            switch (i) {
                case 0: // rechts
                    childMatrix.translate(0.5f, 0.5f, 0.5f)     // Zur rechten Oberkante des Vaters
                               .rotateZ(-beta)                  // Scharnier-Rotation
                               .scale(scaleRight)               // Skalierung mit sin(alpha)
                               .translate(-0.5f, 0.5f, -0.5f);  // Pivot-Korrektur (Mitte des neuen Würfels)
                    generateTree(childMatrix, depth-1, time);   // Rekursion
                    break;
                case 1: // links
                    childMatrix.translate(-0.5f, 0.5f, -0.5f)   // Linke obere Ecke Vater
                               .rotateZ(alpha)                  // Rotation nach links
                               .scale(scaleLeft)                // Skalierung mit cos(alpha)
                               .translate(0.5f, 0.5f, 0.5f);    // Pivot-Korrektur (Mitte des neuen Würfels)
                    generateTree(childMatrix, depth-1, time);   // Rekursion
                    break;
            }
        }
    }

}
