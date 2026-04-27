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

import org.joml.Matrix4f;

public class TreePool {

    public final Matrix4f[] matrices;
    public final float[] distances;
    public final int[] depths;
    public final Integer[] indices; // Für die Sortierung

    private int count = 0;

    public TreePool(int maxNodes) {
        matrices    = new Matrix4f[maxNodes];
        distances   = new float[maxNodes];
        depths      = new int[maxNodes];
        indices     = new Integer[maxNodes];

        for (int i = 0; i < maxNodes; i++) {
            matrices[i] = new Matrix4f();
            indices[i] = i; // Initialer Index
        }
    }

    public void reset() {
        count = 0;
    }

    public int size() {
        return count;
    }

    public void add(Matrix4f m, float dist, int d) {
        if (count < matrices.length) {
            matrices[count].set(m);
            distances[count] = dist;
            depths[count] = d;
            count++;
        }
    }

}
