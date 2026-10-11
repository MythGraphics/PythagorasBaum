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

import GLUtil.Metric;

public class MetricTest {

    public static void main(String[] args) {
        Metric m = new Metric(400, 300);
        float[] point = m.getGLPoint(200, 150);
        System.out.println( "x = " + point[0] );
        System.out.println( "y = " + point[1] );
    }

}
