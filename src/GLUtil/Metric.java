/*
 *
 */

package GLUtil;

/**
 *
 * @author  Martin Pröhl alias MythGraphics
 * @version 1.0.0
 *
 */

public class Metric {

    /**
     * OpenGL: Punkt(0,0) ist in der Mitte des Frames
     *         Punkt(1.0f,1.0f) ist oben rechts, Punkt(-1.0f,-1.0f) unten links
     * Java2D: Punkt(0,0) ist oben links
     *         Punkt(frame_width, frame_height) ist unten rechts
     */

    private final int frame_width, frame_height;

    public Metric(int frame_width, int frame_height) {
        this.frame_width  = frame_width;
        this.frame_height = frame_height;
    }

    public float[] getGLPoint(int x, int y) {
        float[] point = new float[2];
        point[0] = x / (frame_width/2)  - 1.0f;
        point[1] = y / (frame_height/2) - 1.0f;
        return point;
    }

}
