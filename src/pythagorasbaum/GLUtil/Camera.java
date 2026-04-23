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

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {

    public Vector3f position = new Vector3f(0, 2, 5); // Position der Kamera
    public Vector3f target   = new Vector3f(0, 2, 0); // Punkt, auf den die Kamera schaut
    public Vector3f up       = new Vector3f(0, 1, 0); // Up-Vektor (Oben ist Y)

    public Camera() {}

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(position, target, up);
    }

}
