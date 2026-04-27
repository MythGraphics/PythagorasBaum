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

import static GLUtil.GLUtil.*;
import GLUtil.Shader;
import static GLUtil.Shader.FRAGMENT;
import static GLUtil.Shader.VERTEX;
import java.io.IOException;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public abstract class AbstractGL3D extends AbstractGL3DBasic {

    private int bgVao; // Vertex Array Object (Positionen der Eckpunkte)
    private int bgShaderProgram;

    public AbstractGL3D(int maxDepth) {
        super(maxDepth);
    }

    abstract String getBGShaderFile(Shader shader);

    @Override
    public void init() {
        super.init();
        glUseProgram(bgShaderProgram);
    }

    @Override
    public void loadAdditionalShader() throws IOException {
        bgShaderProgram = createShaderProgram(
            readShaderFile( getBGShaderFile( VERTEX ), getClass() ),
            readShaderFile( getBGShaderFile( FRAGMENT ), getClass() )
        );
    }

    @Override
    public void setupBackground() {
        bgVao = glGenVertexArrays();
        int bgVbo = glGenBuffers();

        glBindVertexArray(bgVao);
        glBindBuffer(GL_ARRAY_BUFFER, bgVbo);
        glBufferData(GL_ARRAY_BUFFER, BG_QUAD_VERTICES, GL_STATIC_DRAW);

        // BG: nur ein Attribut: Position (Location 0 im Shader)
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
        glBindVertexArray(0); // entbinden
    }

    @Override
    public void drawBackground() {
        glUseProgram(bgShaderProgram);
        glDisable(GL_DEPTH_TEST); // Tiefentest aus, damit es immer im Hintergrund ist
        glBindVertexArray(bgVao);
        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
        glEnable(GL_DEPTH_TEST);  // Tiefentest wieder an
    }

}
