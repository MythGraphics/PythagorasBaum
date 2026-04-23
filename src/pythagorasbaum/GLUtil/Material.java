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

import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform3f;

public enum Material {

    METALL (
        new float[] {0.25f, 0.25f, 0.25f}, // Anteil r, g, b (1.0f == 100%)
        new float[] {0.3f, 0.3f, 0.3f},
        new float[] {1.0f, 1.0f, 0.55f},
        128.0f
    ),
    EMERALD (
        new float[] {0.0215f, 0.1745f, 0.0215f},
        new float[] {0.07568f, 0.61424f, 0.07568f},
        new float[] {0.633f, 0.7278f, 0.633f},
        0.6f*128.0f
    ),
    OBSIDIAN (
        new float[] {0.05375f, 0.05f, 0.06625f},
        new float[] {0.18275f, 0.17f, 0.22525f},
        new float[] {0.332741f, 0.328634f, 0.346435f},
        0.3f*128.0f
    ),
    WOOD (
        new float[] {0.2f, 0.2f, 0.2f},
        new float[] {0.6f, 0.6f, 0.6f},
        new float[] {0.05f, 0.05f, 0.05f},
        2.0f
    ),
    PLASTIC (
        new float[] {0.2f, 0.2f, 0.2f},
        new float[] {1.0f, 1.0f, 1.0f},
        new float[] {0.8f, 0.8f, 0.8f},
        16.0f
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
