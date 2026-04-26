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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class RessourceLoader {

    public final static String JAR          = "MythGraphics_PythagorasBaum.jar";
    public final static String ZIP_PATH     = "/pythagorasbaum/";
    public final static String LOCAL_PATH   = "src/"+ZIP_PATH;

    private final static String EMPTY       = "fileName null oder leer.";

    private RessourceLoader() {}

    public static String loadTextFileFromJar(String fileName, Class clazz) throws IOException {
        if ( fileName == null || fileName.isBlank() ) {
            throw new IOException(EMPTY);
        }
        try ( InputStream in = clazz.getClassLoader().getResourceAsStream( fileName )) {
            if (in == null) {
                throw new IOException("Datei in JAR nicht gefunden: " + fileName);
            }
            // via Stream API auslesen
            try ( BufferedReader reader = new BufferedReader( new InputStreamReader( in, StandardCharsets.UTF_8 ))) {
                return reader.lines().collect( Collectors.joining( "\n" ));
            }
        }
    }

    public static String loadTextFileFromFS(String fileName) throws IOException {
        if ( fileName == null || fileName.isBlank() ) {
            throw new IOException(EMPTY);
        }
        try ( BufferedReader reader = new BufferedReader( new FileReader( new File( fileName ), StandardCharsets.UTF_8 ))) {
            return reader.lines().collect( Collectors.joining( "\n" ));
        }
    }

    public static String loadTextFile(String fileName, Class clazz) {
        if ( fileName == null || fileName.isBlank() ) { return null; }
        // versuchen, von FS laden
        try { return loadTextFileFromFS(fileName); }
        catch (IOException e) { System.err.println( e.getMessage() ); }
        try { return loadTextFileFromFS(LOCAL_PATH+fileName); }
        catch (IOException e) { System.err.println( e.getMessage() ); }
        // versuchen, von JAR zu laden
        try { return loadTextFileFromJar(fileName, clazz); }
        catch (IOException e) { System.err.println( e.getMessage() ); }
        try { return loadTextFileFromJar(ZIP_PATH+fileName, clazz); }
        catch (IOException e) { System.err.println( e.getMessage() ); }
        return null;
    }

}
