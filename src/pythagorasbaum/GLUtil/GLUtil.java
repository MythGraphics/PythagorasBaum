/*
 *
 */

package pythagorasbaum.GLUtil;

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
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;

public class GLUtil {

    public final static float[] CUBE_VERTICES = {
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
    public final static int[] CUBE_INDICES = {
         0,  1,  2,   2,  3,  0, // Vorne
         4,  5,  6,   6,  7,  4, // Hinten
         8,  9, 10,  10, 11,  8, // Oben
        12, 13, 14,  14, 15, 12, // Unten
        16, 17, 18,  18, 19, 16, // Rechts
        20, 21, 22,  22, 23, 20  // Links
    };
    public final static float[] BG_QUAD_VERTICES = {
        -1.0f,  1.0f,
        -1.0f, -1.0f,
         1.0f, -1.0f,
         1.0f,  1.0f
    };

    private GLUtil() {}

    public static String readFile(String filename) throws IOException {
        return Files.readString( Path.of( filename ));
    }

    public static boolean isBufferSizeSufficient(FloatBuffer matrixBuffer) {
        if ( matrixBuffer.position() >= matrixBuffer.capacity() ) {
            System.err.println("MatrixBuffer überschreitet Kapazitätsgrenze!");
            return false;
        } else {
            return true;
        }
    }

    public static void checkShaderError(int shaderId) {
        if ( glGetShaderi( shaderId, GL_COMPILE_STATUS ) == GL_FALSE ) {
            throw new RuntimeException( "Kompilieren des Shaders fehlgeschlagen: " + glGetShaderInfoLog( shaderId ));
        }
    }

    public static void checkProgramError(int programId) {
        if ( glGetProgrami( programId, GL_LINK_STATUS ) == GL_FALSE) {
            throw new RuntimeException("Linken des Shaders fehlgeschlagen: " + glGetProgramInfoLog( programId ));
        }
    }

    public static int createShaderProgram(String vertexCode, String fragmentCode) {
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

    public final static Matrix3f getNormalMatrix(Matrix4f modelMatrix, Matrix3f dest) {
        Matrix4f nMatrix = new Matrix4f(modelMatrix);
        nMatrix.invert().transpose();
        return nMatrix.get3x3(dest); // Extrahiert den Rotations/Skalierungs-Teil
    }

    public static void updateProjectionMatrix(int width, int height, Matrix4f projection) {
        float aspectRatio = width / (float) Math.max(height, 1); // Division durch 0 verhindern
        float fov = (float) Math.toRadians(45.0f); // 45° Sichtfeld
        float near = 0.1f;
        float far = 100.0f;
        projection.setPerspective(fov, aspectRatio, near, far); // globale Projektions-Matrix überschreiben (JOML)
    }

}
