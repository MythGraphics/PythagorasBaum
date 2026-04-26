/*
 *
 */

package graphic.io;

/**
 *
 * @author  Martin Pröhl alias MythGraphics
 * @version 1.0.0
 *
 */

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class PathFinder {

    private PathFinder() {}

    public static void main(String[] args) {
        String jar = getJarPath();
        System.out.println("executing JAR file or class folder:\n" + jar);

        File jarFile = new File(jar);

        String jarName = jarFile.getName();
        System.out.println("executing JAR file name or folder name:\n" + jarName);

        String jarDir = jarFile.getParentFile().getAbsolutePath();
        System.out.println("active JAR directory:\n" + jarDir);
    }

    public static String getJarDir() {
        return getJarFile().getParentFile().getAbsolutePath();
    }

    public static String getJarName() {
        return getJarFile().getName();
    }

    public static File getJarFile() {
        return new File( getJarPath() );
    }

    public static String getJarPath() {
        try {
            // Holt die URL des Speicherorts der aktuellen Klasse
            URL url = PathFinder.class.getProtectionDomain().getCodeSource().getLocation();

            // Dekodiert die URL, um Leerzeichen oder Sonderzeichen (%20) korrekt zu behandeln
            String decodedPath = URLDecoder.decode( url.getFile(), StandardCharsets.UTF_8.name() );

            // Entfernt eventuelle "file:" Präfixe, falls vorhanden (obwohl .getFile() dies normalerweise handhabt)
            if ( decodedPath.startsWith( "file:" )) {
                decodedPath = decodedPath.substring(5);
            }

            // Bei Windows-Pfaden, die mit einem Schrägstrich beginnen, diesen entfernen
            if ( decodedPath.startsWith( "/" ) && System.getProperty( "os.name" ).toLowerCase().contains( "win" )) {
                 decodedPath = decodedPath.substring(1);
            }

            return decodedPath;
        } catch (UnsupportedEncodingException e) {
            System.err.println( e.getMessage() );
            return null;
        }
    }

}
