/*
 *
 */

package pythagorasbaum;

/**
 *
 * @author  Martin Pröhl alias MythGraphics
 * @version 0.0.0
 *
 */

public class Main {

    public final static String NAME    = "MythGraphics Pythagoras Baum";
    public final static String VERSION = "0.0.0";

    public static void main(String[] args) {
        new Main(args);
    }

    public Main(String[] args) {
        if ( args == null || args.length == 0 || args[0].equalsIgnoreCase( "--help" )) {
            printHelp();
        }
        else if ( args[0].equalsIgnoreCase( "--version" )) {
            System.out.println(NAME + " v" + VERSION);
        }
        else if ( args[0].equalsIgnoreCase( "--j2d" )) {
            PythagorasFrame.main(null);
        }
        else if ( args[0].equalsIgnoreCase( "--opengl" )) {
            PythagorasBaumOpenGL.main(null);
        }
        else {
            System.err.println("Parameter \"" + args[0] + "\" unbekannt");
            System.err.println();
            printHelp();
        }
    }

    private static void printHelp() {
        System.out.println("Parameter:");
        System.out.println("  --j2d         nutzt die Java2D API");
        System.out.println("  --opengl      nutzt die OpenGL API");
        System.out.println("  --help        zeigt diese Hilfe an");
        System.out.println("  --version     zeigt Programm-Version an");
        System.out.println();
    }

}
